package servlet.service;

import com.my.was.http.HttpRequest;
import com.my.was.http.HttpResponse;
import com.my.was.servlet.SimpleServlet;

import java.io.IOException;

public class Hello implements SimpleServlet {
    @Override
    public void service(HttpRequest req, HttpResponse res) throws IOException {
        res.sendHeaders(200, "text/html");
        res.getWriter().write("Service Hello, " + req.getParameter("name"));
        res.getWriter().flush();
    }
}