package com.example.waitwise.config;

import com.example.waitwise.models.Citizen;
import com.example.waitwise.models.Service;
import com.example.waitwise.models.User;
import com.example.waitwise.repositories.CitizenRepository;
import com.example.waitwise.repositories.ServiceRepository;
import com.example.waitwise.repositories.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DataSeeder {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private CitizenRepository citizenRepository;

    @PostConstruct
    public void seedData() {
        // Seed Walk-In Citizen
        if (citizenRepository.findByCnic("WALKIN").isEmpty()) {
            Citizen walkin = new Citizen();
            walkin.setCnic("WALKIN");
            walkin.setFullName("Walk-In Customer");
            walkin.setPhoneNumber("00000000000");
            walkin.setDateOfBirth("1900-01-01");
            citizenRepository.save(walkin);
        }

        // Seed Services
        List<String> serviceNames = Arrays.asList(
                "CNIC Renewal",
                "New Passport",
                "FRC Document",
                "Domicile",
                "Birth Certificate"
        );

        for (String name : serviceNames) {
            if (serviceRepository.findByServiceName(name).isEmpty()) {
                Service service = new Service();
                service.setServiceName(name);
                service.setAverageWaitTimeMinutes(2); // default
                serviceRepository.save(service);
            }
        }
       
    }
}
