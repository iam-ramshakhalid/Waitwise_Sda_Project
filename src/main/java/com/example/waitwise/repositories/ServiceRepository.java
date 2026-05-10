package com.example.waitwise.repositories;
import com.example.waitwise.models.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Integer> {
    java.util.Optional<Service> findByServiceName(String serviceName);
}