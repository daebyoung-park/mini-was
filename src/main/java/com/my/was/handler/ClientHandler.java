package com.my.was.handler;

import com.my.was.config.Configuration;
import com.my.was.http.HttpRequest;
import com.my.was.http.HttpResponse;
import com.my.was.servlet.SimpleServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.net.URLDecoder.decode;

public class ClientHandler implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private static final String ERROR_403 = "403";
    private static final String ERROR_404 = "404";
    private static final String ERROR_500 = "500";

    // 캐시된 FileSystem을 저장하기 위한 맵
    private static final Map<String, FileSystem> fileSystemCache = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> fileSystemState = new ConcurrentHashMap<>();

    private final Socket clientSocket;
    private final Configuration config;

    public ClientHandler(Socket clientSocket, Configuration config) {
        this.clientSocket = clientSocket;
        this.config = config;
    }

    @Override
    public void run() {

        try (InputStream inputStream = clientSocket.getInputStream();
             OutputStream outputStream = clientSocket.getOutputStream()) {

            HttpRequest request = parseRequest(inputStream);
            HttpResponse response = new HttpResponse(outputStream);

            String requestedPath = request.getPath();
            logger.debug("[RUN requestedPath] requestedPath => {}", requestedPath);
            if (requestedPath.equals("/")) {
                requestedPath = "/" + config.getIndexPage();
            }

            if (isServletRequest(requestedPath)) {
                handleServletRequest(requestedPath, request, response);
            } else {
                handleStaticFileRequest(requestedPath, request, response);
            }

        } catch (IOException | URISyntaxException e) {
            logger.error("Error handling client request", e);
        } finally {
            logger.info("==========================================");
            logger.info("============== PROCESS DONE ==============");
            logger.info("==========================================");
        }
    }

    /**
     * config.json 에 등록된 servlet 인지 확인
     * @param path
     * @return
     */
    private boolean isServletRequest(String path) {
        return config.getServletClassName(path) != null;
    }

    /**
     * 서블릿 호출 - SimpleServlet 상속으로 구현된 servlet class 호출
     * @param path
     * @param request
     * @param response
     */
    private void handleServletRequest(String path, HttpRequest request, HttpResponse response) {
        try {
            String servletClassName = config.getServletClassName(path);
            Class<?> servletClass = Class.forName("servlet." + servletClassName);
            SimpleServlet servlet = (SimpleServlet) servletClass.getDeclaredConstructor().newInstance();
            servlet.service(request, response);
        } catch (Exception e) {
            logger.error("Error processing servlet request", e);
            response.sendErrorResponse(500, config.getErrorPage(ERROR_500));
        }
    }

    /**
     * 정적파일처리
     * @param path
     * @param request
     * @param response
     * @throws URISyntaxException
     */
    private void handleStaticFileRequest(String path, HttpRequest request, HttpResponse response) throws URISyntaxException {

        boolean isJar = false;
        String host = request.getHost();
        try {
            String docRoot = config.getHttpRoot(host).replaceFirst("^/", ""); // '/' 제거
            URL rootUrl = getClass().getClassLoader().getResource(docRoot);

            if (rootUrl == null) {
                response.sendErrorResponse(404, config.getErrorPage(ERROR_404));
                return;
            }

            logger.debug("### rootUrl: {}", rootUrl.getPath());
            // JAR 환경인지, 파일 시스템 환경인지 구분.
            if ("jar".equals(rootUrl.getProtocol()))
                isJar = true;
            logger.debug("[handleStaticFileRequest] isJar => [{}]", isJar);

            // 리소스경로
            Path httpRootDir = getHttpRootDir(rootUrl, docRoot, isJar);
            logger.debug("[DOCROOT::getHttpRootDir] httpRootDir: [{}], path: [{}]", httpRootDir, path);

            // 리소스경로( 파일 or 디렉토리 )
            Path requestedFile = resolveRequestedFile(rootUrl, httpRootDir, path, isJar);
            logger.debug("[REQPATH::resolveRequestedFile] requestedFile: {}", requestedFile);

            if (requestedFile == null || Files.notExists(requestedFile)) {
                response.sendErrorResponse(404, config.getErrorPage(ERROR_404));
                return;
            }

            if (isForbidden(requestedFile, httpRootDir) || Files.isDirectory(requestedFile)) {
                response.sendErrorResponse(403, config.getErrorPage(ERROR_403));
            } else {
                serveFile(response, requestedFile);
            }

        } catch (Exception e) {
            logger.error("Error processing static file request", e);
            response.sendErrorResponse(500, config.getErrorPage(ERROR_500));
        }
    }

    /**
     * JAR 파일 시스템을 가져옵니다. URI가 캐시되어 있지 않으면 새로 생성합니다.
     * @param jarUri JAR 파일 URI
     * @return JAR 파일 시스템
     * @throws IOException IO 예외
     */
    private FileSystem getJarFileSystem(URI jarUri) throws IOException {
        /*
        1.	캐시 키를 URI 기반으로만 관리:
        캐시 키에서 host를 제거하고, JAR 파일의 URI만을 사용하여 캐시 키를 생성하면
        동일한 JAR 파일에 대해서는 어떤 호스트로 접근하더라도 동일한 FileSystem을 재사용할 수 있습니다.
         */
        String cacheKey = jarUri.toString();
        FileSystem fileSystem = fileSystemCache.get(cacheKey);
        logger.debug("[getJarFileSystem] :: jarUri=[{}]", jarUri);
        logger.debug(">>> FsysCache.size=[{}]", fileSystemCache.size());

        if (fileSystem != null && fileSystem.isOpen()) {
            return fileSystem;
        }

        /*
        2.	기존 FileSystem 재사용 처리:
        FileSystems.newFileSystem()은 이미 동일한 JAR URI에 대해
        FileSystem이 존재할 때 예외를 던지므로, 예외가 발생하면 기존 FileSystem을 찾아서 사용하도록 처리합니다.
         */
        try {
            // 새 FileSystem 생성
            fileSystem = FileSystems.newFileSystem(jarUri, Collections.emptyMap());
        } catch (FileSystemAlreadyExistsException e) {
            logger.info("[getJarFileSystem] Using existing FileSystem for cacheKey:\n {}", cacheKey);
            fileSystem = FileSystems.getFileSystem(jarUri);  // 기존 FileSystem 사용
        }

        fileSystemCache.put(cacheKey, fileSystem);
        return fileSystem;
    }

    private void serveOrCheck(HttpResponse response, Path requestedFile, Path httpRootDir) {
        logger.debug("* Final requested path after resolving: {}", requestedFile);
        logger.debug("* File exists: {}", Files.exists(requestedFile, new LinkOption[]{LinkOption.NOFOLLOW_LINKS}));
        logger.debug("* File notExists: {}", Files.notExists(requestedFile, new LinkOption[]{LinkOption.NOFOLLOW_LINKS}));
        logger.debug("* is directory: {}", Files.isDirectory(requestedFile, new LinkOption[]{LinkOption.NOFOLLOW_LINKS}));
        logger.debug("* isForbidden: {}", isForbidden(requestedFile, httpRootDir));

        if (isForbidden(requestedFile, httpRootDir)) response.sendErrorResponse(403, config.getErrorPage(ERROR_403));
        else if (Files.isDirectory(requestedFile)) response.sendErrorResponse(403, config.getErrorPage(ERROR_403));
        else if (Files.notExists(Paths.get(requestedFile.toUri()))) response.sendErrorResponse(404, config.getErrorPage(ERROR_404));
        else serveFile(response, requestedFile);
    }

    /**
     * 1) DocRoot 에 실행가능 JAR 환경
     * 2) 파일 시스템 접근인지
     * 3) config 설정에 없는 파일 시스템 접근인지
     *
     * @param docRoot
     * @param isJar
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    public Path getHttpRootDir(URL resourceUrl, String docRoot, boolean isJar) throws IOException, URISyntaxException {
        if (isJar) {
            logger.debug("[1.1 (JAR) getHttpRootDir]");
            URI jarUri = resourceUrl.toURI();
            FileSystem fileSystem = getJarFileSystem(jarUri);
            Path jarRootPath = fileSystem.getPath(docRoot).toRealPath();
            return jarRootPath;
        } else {
            logger.debug("[1.2 (FILE) getHttpRootDir]");
            return Paths.get(resourceUrl.toURI()).toRealPath();
        }
    }


    /**
     * DocRoot 다음의 Path (Directory 접근 or File 접근)
     * @param requestUrl
     * @param path
     * @param isJar
     * @return
     * @throws IOException
     */
    private Path resolveRequestedFile(URL requestUrl, Path httpRootDir, String path, boolean isJar) {
        Path sanitizedPath = sanitizePath(path);
        Path resolvedPath = null;

        logger.debug("[resolveRequestedFile.3.1] requestUrl={}", requestUrl);
        logger.debug("[resolveRequestedFile.3.2] sanitizedPath={}", sanitizedPath);

        try {
            if (isJar) {
                URI jarUri = requestUrl.toURI();
                FileSystem jarFileSystem = getJarFileSystem(jarUri);
                Path jarRootPath = jarFileSystem.getPath(httpRootDir.toString(), sanitizedPath.toString()).normalize();
                logger.debug("[resolveRequestedFile.3.3] JAR jarRootPath={}", jarRootPath);
                resolvedPath = jarRootPath;
            } else {
                resolvedPath = httpRootDir.resolve(sanitizedPath).normalize();
                logger.debug("[resolveRequestedFile.3.5] File system resolvedPath={}", resolvedPath);
            }
        } catch (Exception e) {
            logger.error("Error resolving file path: ", e);
        }

        return resolvedPath;
    }


    // 리소스 경로에서 선행 슬래시 제거
    private Path sanitizePath(String path) {
        return Paths.get(path.startsWith("/") ? path.substring(1) : path);
    }

    private boolean isForbidden(Path requestedFile, Path httpRootDir) {
        return requestedFile == null
                || !requestedFile.startsWith(httpRootDir)
                || requestedFile.toString().contains("..")
                || requestedFile.toString().endsWith(".exe");
    }

    /**
     * 정적파일 처리
     * @param response
     * @param filePath
     */
    private void serveFile(HttpResponse response, Path filePath) {
        try {
            // 응답 Content-Type 설정에 UTF-8 추가
            String contentType = Files.probeContentType(filePath);
            response.setContentType(contentType + "; charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.sendHeaders(200, contentType);

            Files.copy(filePath, response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            logger.error("Error serving file: {} ", filePath, e);
            response.sendErrorResponse(500, config.getErrorPage(ERROR_500));
        }
    }

    /**
     * HttpRequest 세팅
     * @param inputStream
     * @return
     */
    private HttpRequest parseRequest(InputStream inputStream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String method = "";
        String path = "";
        String host = "";
        Map<String, String> parameters = new HashMap<>();

        try {
            String line = reader.readLine();
            logger.debug("[parseRequest] [Header] [{}]", line);
            if (null != line && !line.isEmpty()) {
                String[] requestLine = line.split(" ");
                method = requestLine[0];
                String fullPath = requestLine[1];

                if (fullPath.contains("?")) {
                    String[] pathAndParams = fullPath.split("\\?");
                    path = pathAndParams[0];
                    parameters = parseParameters(pathAndParams[1]);
                } else {
                    path = fullPath;
                }
            }

            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                if (headerLine.startsWith("Host:")) {
                    host = headerLine.split(": ")[1].split(":")[0];
                    logger.debug("[parseRequest] [HOST] [{}]", host);
                }
            }

        } catch (Exception e) {
            logger.error("Error parsing request", e);
        }

        return new HttpRequest(method, path, host, parameters);
    }

    /**
     * Query String Parameter to Map<K,V>
     * @param query
     * @return
     */
    private Map<String, String> parseParameters(String query) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            String key = decode(keyValue[0]);
            String value = keyValue.length > 1 ? decode(keyValue[1]) : "";
            logger.debug("[parseParameters] ket=[{}], value=[{}]", key, value);
            params.put(key, value);
        }
        return params;
    }
}
