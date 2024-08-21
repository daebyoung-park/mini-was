package test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Test {
    public static void main(String [] args) {

// [RUN requestedPath] requestedPath => /
// [After::getHttpRootDir] HTTP_ROOT Dir: [/www], path: [/index.html]
//  [resolveRequestedFile] httpRootDir=/www
//  [resolveRequestedFile] sanitizedPath=index.html
        String idx = "/index.html";
        Path path = Paths.get("/www").normalize();
        Path newP = Paths.get(path.startsWith("/") ? idx.substring(1) : idx);
        Path requestedPath = path.resolve(newP).normalize();
        System.out.println("requestedPath >>"+requestedPath);
    }
}

