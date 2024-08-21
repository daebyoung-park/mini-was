package com.my.was.server;

import com.my.was.config.Configuration;
import com.my.was.config.ConfigurationLoader;
import com.my.was.handler.ClientHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleWASServer {

    private static final String bannerFile = "banner.txt";
    private static final Logger logger = LoggerFactory.getLogger(SimpleWASServer.class.getCanonicalName());
    private static final String configFileName = "config.json";
    private static final int NUM_THREADS = 16;
    private final Configuration config;

    public SimpleWASServer(Configuration config) {
        this.config = config;
    }

    public void start() throws IOException {
        ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);
        try (ServerSocket server = new ServerSocket(config.getPort())) {

            // Server property setup
            server.setSoTimeout(60 * 60 * 1000); // 1 Hour

            // bannerFile
            this.printBanner();
            logger.info("Accepting connections on port {}", server.getLocalPort());

            while (true) {
                try {
                    Socket request = server.accept();
                    // 클라이언트 요청을 처리할 스레드를 생성하고, 요청을 처리하도록 함
                    Runnable r = new ClientHandler(request, config);
                    pool.submit(r);
                } catch (IOException ex) {
                    logger.error("Error accepting connection", ex);
                    throw ex;
                }
            }
        }
    }

    public static void main(String[] args) {
        // Configuration 인스턴스를 설정하는 코드 필요 (예: 파일로부터 읽어오는 등)
        Configuration config = ConfigurationLoader.loadConfig(configFileName);


        try {
            SimpleWASServer server = new SimpleWASServer(config);
            server.start();
        } catch (IOException e) {
            logger.error("Server could not start", e);
            throw new RuntimeException(e);
        }
    }

    private void printBanner() {
        logger.info("\n\r" +
                "   _____  _                    __     _       __ ___    _____\n" +
                "  / ___/ (_)____ ___   ____   / /___ | |     / //   |  / ___/\n" +
                "  \\__ \\ / // __ `__ \\ / __ \\ / // _ \\| | /| / // /| |  \\__ \\\n" +
                " ___/ // // / / / / // /_/ // //  __/| |/ |/ // ___ | ___/ /\n" +
                "/____//_//_/ /_/ /_// .___//_/ \\___/ |__/|__//_/  |_|/____/\n" +
                "                   /_/");
    }
}
