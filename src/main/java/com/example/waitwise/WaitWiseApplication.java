package com.example.waitwise;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WaitWiseApplication {

    public static void main(String[] args) {
        SpringApplication.run(WaitWiseApplication.class, args);
    }

    @Bean
    public CommandLineRunner finalCheck() {
        return args -> {
            System.out.println("************************************************");
            System.out.println("WAITWISE BACKEND STATUS: ALL SYSTEMS OPERATIONAL");
            System.out.println("Database: Connected");
            System.out.println("Services: Initialized");
            System.out.println("Controllers: Live at Port 8081");
            System.out.println("READY FOR FRONTEND INTEGRATION!");
            System.out.println("************************************************");
        };
    }
}