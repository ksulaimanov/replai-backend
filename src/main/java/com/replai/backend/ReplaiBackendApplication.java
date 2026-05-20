package com.replai.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReplaiBackendApplication {

    static {
        if (java.nio.file.Files.exists(java.nio.file.Paths.get(".env"))) {
            io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure().load();
            dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(ReplaiBackendApplication.class, args);
    }

}
