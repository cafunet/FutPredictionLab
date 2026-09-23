package com.futprediction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FutPredictionApplication {

    public static void main(String[] args) {
        SpringApplication.run(FutPredictionApplication.class, args);
    }
}
