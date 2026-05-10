package com.example.waitwise.services;
import com.example.waitwise.models.User;
import com.example.waitwise.models.Citizen;
import com.example.waitwise.repositories.UserRepository;
import com.example.waitwise.repositories.CitizenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepo;

    @Autowired
    private CitizenRepository citizenRepo;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    // M1: Failed-attempt tracking
    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lockedAccounts = new ConcurrentHashMap<>();
    private static final int MAX_FAILED_ATTEMPTS = 3;
    private static final int LOCK_DURATION_MINUTES = 15;

    public User login(String username, String password) {
        // M1: Check if account is locked
        if (lockedAccounts.containsKey(username)) {
            LocalDateTime lockTime = lockedAccounts.get(username);
            if (LocalDateTime.now().isBefore(lockTime.plusMinutes(LOCK_DURATION_MINUTES))) {
                throw new RuntimeException("Account locked due to 3 failed attempts. Try again in 15 mins.");
            } else {
                lockedAccounts.remove(username);
                failedAttempts.remove(username);
            }
        }

        try {
            // Hardcoded Admin
            if ("admin".equals(username)) {
                if ("admin123".equals(password)) {
                    failedAttempts.remove(username);
                    User admin = new User();
                    admin.setUsername("admin");
                    admin.setRole("Admin");
                    admin.setFullName("System Admin");
                    return admin;
                } else {
                    recordFailedAttempt(username);
                    throw new RuntimeException("Invalid credentials.");
                }
            }

            // Find by Username or CNIC
            Optional<User> userOpt = userRepo.findByUsername(username);
            if (userOpt.isEmpty()) {
                userOpt = userRepo.findByCnic(username);
            }

            User user = userOpt.orElseThrow(() -> {
                recordFailedAttempt(username);
                return new RuntimeException("User not found.");
            });

            if (user.getRole() != null && !user.getRole().equals("Citizen") && !user.getActive()) {
                throw new RuntimeException("Account deactivated. Contact admin.");
            }

            // Enhanced password matching for both plain and BCrypt
            boolean passwordMatches = false;
            String dbPassword = user.getPassword();

            if (dbPassword != null && (dbPassword.startsWith("$2a$") || dbPassword.startsWith("$2b$") || dbPassword.startsWith("$2y$"))) {
                passwordMatches = passwordEncoder.matches(password.trim(), dbPassword);
            } else {
                passwordMatches = (dbPassword != null && dbPassword.equals(password.trim()));
            }

            if (passwordMatches) {
                failedAttempts.remove(username);
                return user;
            } else {
                recordFailedAttempt(username);
                throw new RuntimeException("Incorrect password.");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("System error. Please try again later.");
        }
    }

    /**
     * Records a failed login attempt and locks the account after MAX_FAILED_ATTEMPTS.
     */
    private void recordFailedAttempt(String username) {
        int attempts = failedAttempts.getOrDefault(username, 0) + 1;
        failedAttempts.put(username, attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            lockedAccounts.put(username, LocalDateTime.now());
        }
    }

    public User registerCitizen(String cnic, String password, String fullName, String phoneNumber, String dateOfBirth, String email) {
        if (userRepo.findByCnic(cnic).isPresent()) {
            throw new RuntimeException("CNIC already exists");
        }

        // Backend Validation
        if (cnic == null || cnic.length() != 13) {
            throw new RuntimeException("CNIC must be exactly 13 digits.");
        }
        if (phoneNumber != null && phoneNumber.length() > 11) {
            throw new RuntimeException("Phone number cannot exceed 11 digits.");
        }
        if (dateOfBirth != null) {
            try {
                LocalDate dob = LocalDate.parse(dateOfBirth);
                if (dob.getYear() > 2015) {
                    throw new RuntimeException("Date of Birth must be 2015 or earlier.");
                }
            } catch (Exception e) {
                throw new RuntimeException("Invalid Date of Birth format.");
            }
        }

        User user = new User();
        user.setUsername(null); // Citizens have no usernames
        user.setCnic(cnic);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("Citizen");
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setCreatedAt(LocalDateTime.now());
        userRepo.save(user);

        if (citizenRepo.findByCnic(cnic).isEmpty()) {
            Citizen citizen = new Citizen();
            citizen.setCnic(cnic);
            citizen.setFullName(fullName);
            citizen.setPhoneNumber(phoneNumber);
            citizen.setDateOfBirth(dateOfBirth);
            citizen.setEmail(email);
            citizenRepo.save(citizen);
        }

        // Send welcome email asynchronously (don't block registration)
        new Thread(() -> {
            try {
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    emailService.sendRegistrationSuccess(user.getEmail(), user.getFullName());
                }
            } catch (Exception e) {
                System.err.println("Async registration email failed: " + e.getMessage());
            }
        }).start();

        return user;
    }
}