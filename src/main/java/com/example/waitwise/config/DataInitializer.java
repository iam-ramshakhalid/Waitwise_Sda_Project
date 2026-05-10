package com.example.waitwise.config;

import com.example.waitwise.models.Service;
import com.example.waitwise.models.User;
import com.example.waitwise.repositories.ServiceRepository;
import com.example.waitwise.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(ServiceRepository serviceRepo, UserRepository userRepo) {
        return args -> {
            // 1. Agar Services khali hain toh default services dalo
            if (serviceRepo.count() == 0) {
                Service s1 = new Service(); s1.setServiceName("New CNIC"); s1.setAverageWaitTimeMinutes(20);
                Service s2 = new Service(); s2.setServiceName("Renewal"); s2.setAverageWaitTimeMinutes(15);
                serviceRepo.save(s1);
                serviceRepo.save(s2);
                System.out.println("Default Services added!");
            }

            // Admin is now hardcoded in UserService, no DB insertion needed.
        };
    }
}