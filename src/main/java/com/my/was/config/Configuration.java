package com.my.was.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Map;

public class Configuration {
    private static Logger logger = LoggerFactory.getLogger(Configuration.class);

    private int port;
    private String httpRoot;
    private String indexPage;
    private Map<String, String> errorPages;
    private Map<String, Map<String, String>> hosts;
    private Map<String, String> servlet;

    // Root directory for resources (used as base path) classes??
//    private static final String BASE_PATH = "src/main/resources/";

    // Getter and Setter for port
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    // Getter and Setter for httpRoot
    public String getHttpRoot() {
        return httpRoot;
    }

    public void setHttpRoot(String httpRoot) {
        this.httpRoot = httpRoot;
    }

    // Getter and Setter for indexPage
    public String getIndexPage() {
        return indexPage;
    }

    public void setIndexPage(String indexPage) {
        this.indexPage = indexPage;
    }

    // Getter and Setter for errorPages
    public Map<String, String> getErrorPages() {
        return errorPages;
    }

    public void setErrorPages(Map<String, String> errorPages) {
        this.errorPages = errorPages;
    }

    // Getter and Setter for hosts
    public Map<String, Map<String, String>> getHosts() {
        return hosts;
    }

    public void setHosts(Map<String, Map<String, String>> hosts) {
        this.hosts = hosts;
    }

    // Getter and Setter for servlet
    public Map<String, String> getServlet() {
        return servlet;
    }

    public void setServlet(Map<String, String> servlet) {
        this.servlet = servlet;
    }

    // Method to get HTTP_ROOT based on host
    public String getHttpRoot(String host) {
        return hosts.getOrDefault(host, Map.of()).getOrDefault("HTTP_ROOT", httpRoot);
    }

    public String getErrorPage(String errorCode) {
        String errorPagePath = errorPages.get(errorCode);
        if (errorPagePath != null) {
            return loadErrorPage(errorPagePath);
        }
        return "<html><body><h1>Error " + errorCode + "</h1></body></html>"; // Default fallback content
    }
    private String loadErrorPage(String path) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                return null;
            }
            return new String(inputStream.readAllBytes());
        } catch (Exception e) {
            logger.error("loadErrorPage error", e);
            return null;
        }
    }

    // Method to get servlet class name based on path
    public String getServletClassName(String path) {
        return servlet.get(path);
    }
}
