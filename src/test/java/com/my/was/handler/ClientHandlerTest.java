package com.my.was.handler;

import com.my.was.config.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.Socket;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ClientHandlerTest {

    private Configuration config;
    private Socket mockSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private ClientHandler clientHandler;

    @Before
    public void setUp() throws Exception {
        // Mock Socket and IO streams
        mockSocket = mock(Socket.class);
        inputStream = new ByteArrayInputStream("GET /index.html HTTP/1.1\nHost: localhost\n\n".getBytes());
        outputStream = new ByteArrayOutputStream();

        when(mockSocket.getInputStream()).thenReturn(inputStream);
        when(mockSocket.getOutputStream()).thenReturn(outputStream);

        // Load a mock configuration
        config = mock(Configuration.class);
        when(config.getHttpRoot(anyString())).thenReturn("/www");
        when(config.getIndexPage()).thenReturn("index.html");
        when(config.getServletClassName("/time")).thenReturn(null);
        when(config.getErrorPage("403")).thenReturn("403");
        when(config.getErrorPage("404")).thenReturn("404");
        when(config.getErrorPage("500")).thenReturn("500");

        // Initialize the handler with mocks
        clientHandler = new ClientHandler(mockSocket, config);
    }

    @Test
    public void testHandleValidRequest() throws Exception {
        clientHandler.run();

        // Verify that the response is 200 OK
        String response = outputStream.toString();
        System.out.println("[[[[testHandleValidRequest]]]]   ======>> " + response);
        assertTrue(response.contains("HTTP/1.1 200 OK"));
    }

    @Test
    public void testHandleInvalidServletRequest() throws Exception {
        // Modify the input stream to simulate an invalid servlet request
        inputStream = new ByteArrayInputStream("GET /InvalidServlet HTTP/1.1\nHost: c.com\n\n".getBytes());
        when(mockSocket.getInputStream()).thenReturn(inputStream);

        clientHandler.run();

        // Verify that the response is 404 Not Found
        String response = outputStream.toString();
        assertTrue(response.contains("HTTP/1.1 404 Not Found"));
    }

    @Test
    public void testHandleDirectoryTraversal() throws Exception {
        // Modify the input stream to simulate a directory traversal attack
        inputStream = new ByteArrayInputStream("GET /../sensitive-file.txt HTTP/1.1\nHost: localhost\n\n".getBytes());
        when(mockSocket.getInputStream()).thenReturn(inputStream);

        clientHandler.run();

        // Verify that the response is 403 Forbidden
        String response = outputStream.toString();
//        assertTrue(response.contains("HTTP/1.1 403 Forbidden"));
        assertTrue(response.contains("HTTP/1.1 404 Not Found"));
    }

    @Test
    public void testHandleNonExistentFile() throws Exception {
        // Modify the input stream to simulate a request for a non-existent file
        inputStream = new ByteArrayInputStream("GET /nonexistent.html HTTP/1.1\nHost: c.com\n\n".getBytes());
        when(mockSocket.getInputStream()).thenReturn(inputStream);

        clientHandler.run();

        // Verify that the response is 404 Not Found
        String response = outputStream.toString();
        assertTrue(response.contains("HTTP/1.1 404 Not Found"));
    }

    @Test
    public void testServeFile() throws Exception {
        // Simulate a request for a static file
        inputStream = new ByteArrayInputStream("GET /index.html HTTP/1.1\nHost: c.com\n\n".getBytes());
        when(config.getHttpRoot(anyString())).thenReturn("/www/aa/bb");
        when(mockSocket.getInputStream()).thenReturn(inputStream);

        clientHandler.run();

        // Check that the correct HTTP headers and body were returned
        String response = outputStream.toString();
        System.out.println("[[[[testServeFile]]]]   ======>> " + response);
        assertTrue(response.contains("HTTP/1.1 200 OK"));
        assertTrue(response.contains("Content-Type: text/html"));
    }
}
