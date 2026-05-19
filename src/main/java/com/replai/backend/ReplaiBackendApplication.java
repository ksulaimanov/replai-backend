package com.replai.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReplaiBackendApplication {

    public static void main(String[] args) {
        long count = java.nio.file.Files.exists(java.nio.file.Paths.get(".env")) ? 1 : 0;
        if (count > 0) {
            io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure().load();
            dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
        }
        SpringApplication.run(ReplaiBackendApplication.class, args);
    }

}
