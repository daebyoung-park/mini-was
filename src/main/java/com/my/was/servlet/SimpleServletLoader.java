package com.my.was.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class SimpleServletLoader {

    private static final Logger logger = LoggerFactory.getLogger(SimpleServletLoader.class);

    public static SimpleServlet loadServlet(String path) throws ClassNotFoundException, IllegalAccessException, InstantiationException {

        logger.debug("[SimpleServletLoader] path >> {}", path);
        // URL 매핑 규칙에 따라 클래스 이름을 변환
        String className;
        if (path.startsWith("/")) {
            path = path.substring(1); // Remove leading slash
        }
        if (path.contains(".")) {
            className = path.replace('.', '/').replace("/", ".");
        } else {
            className = path;
        }

        // Class.forName을 통해 클래스 동적 로드
        try {
            Class<?> clazz = Class.forName("servlet." + className);
            return (SimpleServlet) clazz.getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException e) {
            logger.error("Class not found: {}", className, e);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            logger.error("Failed to instantiate: {}", className, e);
        }
        return null;
    }
}
