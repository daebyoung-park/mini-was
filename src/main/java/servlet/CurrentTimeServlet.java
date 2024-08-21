package servlet;

import com.my.was.http.HttpRequest;
import com.my.was.http.HttpResponse;
import com.my.was.servlet.SimpleServlet;

import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;

public class CurrentTimeServlet implements SimpleServlet {
    @Override
    public void service(HttpRequest req, HttpResponse res) throws IOException {
        res.sendHeaders(200, "text/html");
        Writer writer = res.getWriter();
        writer.write("Current time: " + LocalDateTime.now());
        writer.flush();
    }
}
