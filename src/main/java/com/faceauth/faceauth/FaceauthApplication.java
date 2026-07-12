package com.faceauth.faceauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FaceauthApplication {

    public static void main(String[] args) {
        SpringApplication.run(FaceauthApplication.class, args);
    }
}
