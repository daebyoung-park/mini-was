package com.my.was.http;

import java.util.Map;

public class HttpRequest {
    private final String method;
    private final String path;
    private final String host;
    private final Map<String, String> parameters;

    public HttpRequest(String method, String path, String host, Map<String, String> parameters) {
        this.method = method;
        this.path = path;
        this.host = host;
        this.parameters = parameters;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getHost() {
        return host;
    }

    public String getParameter(String name) {
        return parameters.get(name);
    }
}
