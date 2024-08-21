package com.my.was.http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {

    private static final Logger logger = LoggerFactory.getLogger(HttpResponse.class);

    private final OutputStream outputStream;
    private final Writer writer;
    private final Map<String, String> headers = new HashMap<>();
    private String characterEncoding = StandardCharsets.UTF_8.name(); // 기본 인코딩 설정

    public HttpResponse(OutputStream outputStream) {
        this.outputStream = outputStream;
        this.writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
    }

    public Writer getWriter() {
        return writer;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void sendHeaders(int statusCode, String contentType) throws IOException {
        HttpStatus status = HttpStatus.fromStatusCode(statusCode);
        String statusText = (status != null) ? status.getStatusText() : "Unknown Error";
        writer.write("HTTP/1.1 " + statusCode + " " + statusText + "\r\n");
        writer.write("Content-Type: " + contentType + "; charset=" + characterEncoding + "\r\n");
        for (Map.Entry<String, String> header : headers.entrySet()) {
            writer.write(header.getKey() + ": " + header.getValue() + "\r\n");
        }
        writer.write("\r\n");
        writer.flush();
    }

    public void sendErrorResponse(int statusCode, String errorPagePath) {
        try {
            sendHeaders(statusCode, "text/html");

            // Load error page content using ClassLoader
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(errorPagePath);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.write("\r\n");
                }
                reader.close();
            } else {
                // Fallback error message if the error page is not found
                writer.write("<html><body><h1>Error " + statusCode + "</h1></body></html>");
            }

            writer.flush();
        } catch (IOException e) {
            logger.error("sendErrorResponse IOException: ", e);
        }
    }

    public void setContentType(String contentType) {
        headers.put("Content-Type", contentType + "; charset=" + characterEncoding);
    }

    public void setCharacterEncoding(String encoding) {
        this.characterEncoding = encoding;
        // Update the Content-Type header to reflect the new encoding
        if (headers.containsKey("Content-Type")) {
            headers.put("Content-Type", headers.get("Content-Type").split(";")[0] + "; charset=" + encoding);
        }
    }
}