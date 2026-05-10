package com.example.waitwise.repositories;
import com.example.waitwise.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    Optional<User> findByCnic(String cnic);
    Optional<User> findByEmail(String email);
    List<User> findByRoleStartingWith(String rolePrefix);
    long countByRoleStartingWith(String rolePrefix);
    long countByRole(String role);
}