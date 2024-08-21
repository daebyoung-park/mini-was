package com.my.was.servlet;

import com.my.was.http.HttpRequest;
import com.my.was.http.HttpResponse;

import java.io.IOException;

public interface SimpleServlet {
    void service(HttpRequest req, HttpResponse res) throws IOException;


}
