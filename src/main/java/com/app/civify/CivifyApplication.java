package com.app.civify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class CivifyApplication {

    public static void main(String[] args) {
        SpringApplication.run(CivifyApplication.class, args);
    }

}
