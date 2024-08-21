package com.my.was.config;

import com.my.was.server.SimpleWASServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class ResourceLoader {

    private static final Logger logger = LoggerFactory.getLogger(ResourceLoader.class.getCanonicalName());
    private static ResourceLoader INSTANCE;
    private ResourceLoader() {
    }

    public static ResourceLoader getINSTANCE() {
        if(INSTANCE == null) {
            INSTANCE = new ResourceLoader();
        }
        return INSTANCE;
    }

    public InputStream getResourceAsStream(String resourcePath) {
        // 리소스 경로에서 시작 '/' 제거
        logger.debug("[getResourceAsStream] {}", resourcePath);
        String sanitizedPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        return getClass().getClassLoader().getResourceAsStream(Arrays.toString(sanitizedPath.getBytes(StandardCharsets.UTF_8)));
    }

    // 리소스에서 파일을 읽는 메서드
    public String readResource(String resourcePath) throws IOException {
        try (InputStream inputStream = getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes());
        }
    }
}
