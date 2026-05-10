package com.example.waitwise.services;

import com.example.waitwise.models.Citizen;
import com.example.waitwise.models.Token;
import com.example.waitwise.models.Revenue;
import com.example.waitwise.repositories.CitizenRepository;
import com.example.waitwise.repositories.ServiceRepository;
import com.example.waitwise.repositories.TokenRepository;
import com.example.waitwise.repositories.RevenueRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;

@Service
public class TokenService {
    @Autowired private TokenRepository tokenRepo;
    @Autowired private CitizenRepository citizenRepo;
    @Autowired private ServiceRepository serviceRepo;
    @Autowired private RevenueRepository revenueRepo;
    @Autowired private EmailService emailService;

    public Token generateNewTokenByName(String cnic, String serviceName, String priorityRequest, String emergencyDescription) {
        Citizen citizen = citizenRepo.findByCnic(cnic).orElseGet(() -> {
            Citizen c = new Citizen();
            c.setCnic(cnic);
            c.setFullName("Guest Citizen");
            c.setPhoneNumber("N/A");
            c.setDateOfBirth("1990-01-01"); // Default date
            return citizenRepo.save(c);
        });
        
        com.example.waitwise.models.Service service = serviceRepo.findByServiceName(serviceName)
            .orElseGet(() -> {
                com.example.waitwise.models.Service s = new com.example.waitwise.models.Service();
                s.setServiceName(serviceName);
                s.setAverageWaitTimeMinutes(2); // default
                return serviceRepo.save(s);
            });

        return createToken(citizen, service, priorityRequest, emergencyDescription, false);
    }

    public Token generateNewToken(String cnic, int serviceId, String priorityRequest, String emergencyDescription, boolean isStaff) {
        Citizen citizen = citizenRepo.findByCnic(cnic).orElseGet(() -> {
            Citizen c = new Citizen();
            c.setCnic(cnic);
            c.setFullName("Guest Citizen");
            c.setPhoneNumber("N/A");
            c.setDateOfBirth("1990-01-01"); // Default date
            return citizenRepo.save(c);
        });
        com.example.waitwise.models.Service service = serviceRepo.findById(serviceId).orElseThrow(() -> new RuntimeException("Service not found!"));
        return createToken(citizen, service, priorityRequest, emergencyDescription, isStaff);
    }

    public Token generateWalkInToken(int serviceId, String priorityRequest) {
        Citizen walkIn = citizenRepo.findByCnic("WALKIN").orElseGet(() -> {
            Citizen c = new Citizen();
            c.setCnic("WALKIN");
            c.setFullName("Walk-In Citizen");
            c.setPhoneNumber("00000000000");
            return citizenRepo.save(c);
        });
        com.example.waitwise.models.Service service = serviceRepo.findById(serviceId).orElseThrow(() -> new RuntimeException("Service not found!"));
        return createToken(walkIn, service, priorityRequest, "", true);
    }

    // --- Token Cancellation ---

    /**
     * Cancels a token if it belongs to the requesting citizen and is still in Waiting status.
     * If it was a Golden priority token, the associated revenue entry is also removed.
     */
    public Token cancelToken(int tokenId, String cnic) {
        Token token = tokenRepo.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found!"));

        // Verify the token belongs to this citizen
        if (!token.getCitizen().getCnic().equals(cnic)) {
            throw new RuntimeException("You can only cancel your own tokens!");
        }

        // Only Waiting tokens can be cancelled
        if (!"Waiting".equals(token.getStatus())) {
            throw new RuntimeException("Only tokens with 'Waiting' status can be cancelled!");
        }

        // Check if the token has expired
        if (token.getExpiryTime() != null && LocalDateTime.now().isAfter(token.getExpiryTime())) {
            throw new RuntimeException("This token has already expired!");
        }

        // If Golden, remove the revenue entry
        if ("Golden".equals(token.getPriorityType())) {
            try {
                List<Revenue> revenues = revenueRepo.findByCitizenAndServiceName(
                        token.getCitizen(), token.getService().getServiceName());
                if (!revenues.isEmpty()) {
                    // Delete the most recent matching revenue entry (in case of multiple)
                    revenueRepo.delete(revenues.get(revenues.size() - 1));
                }
            } catch (Exception e) {
                // Log but don't fail the cancellation if revenue cleanup fails
                System.err.println("Warning: Could not remove revenue entry for cancelled Golden token: " + e.getMessage());
            }
        }

        token.setStatus("Cancelled");
        token.setStatusUpdateTime(LocalDateTime.now());
        return tokenRepo.save(token);
    }

    // --- Counter Operations ---

    public Token callNextToken(int serviceId) {
        List<Token> waitingTokens = tokenRepo.findAll().stream()
                .filter(t -> "Waiting".equals(t.getStatus()) && (serviceId == -1 || t.getService().getServiceId() == serviceId))
                .sorted(Comparator.comparing(Token::getPriorityValue)
                        .thenComparing(Token::getIssueTime))
                .collect(Collectors.toList());

        if (waitingTokens.isEmpty()) {
            throw new RuntimeException("No tokens waiting for this service.");
        }

        Token nextToken = waitingTokens.get(0);
        nextToken.setStatus("Called");
        nextToken.setStatusUpdateTime(LocalDateTime.now());
        Token saved = tokenRepo.save(nextToken);

        // 1. Notify the person whose turn it is
        new Thread(() -> {
            try {
                if (saved.getCitizen().getEmail() != null) {
                    emailService.sendTurnUpNotification(
                        saved.getCitizen().getEmail(), 
                        saved.getCitizen().getFullName(), 
                        saved.getTokenNumber(), 
                        String.valueOf(saved.getService().getServiceId())
                    );
                }
            } catch (Exception e) {}
        }).start();

        // 2. Notify the person at position 5 (index 4 in remaining waiting list)
        if (waitingTokens.size() > 5) {
            Token approachToken = waitingTokens.get(5); // The person who is now at position 5
            new Thread(() -> {
                try {
                    if (approachToken.getCitizen().getEmail() != null) {
                        emailService.sendApproachNotification(
                            approachToken.getCitizen().getEmail(),
                            approachToken.getCitizen().getFullName(),
                            approachToken.getTokenNumber(),
                            5
                        );
                    }
                } catch (Exception e) {}
            }).start();
        }

        return saved;
    }

    public Token startServing(int tokenId) {
        Token token = tokenRepo.findById(tokenId).orElseThrow(() -> new RuntimeException("Token not found"));
        if (!"Called".equals(token.getStatus())) {
            throw new RuntimeException("Token is not in Called state.");
        }
        token.setStatus("Serving");
        token.setStatusUpdateTime(LocalDateTime.now());
        return tokenRepo.save(token);
    }

    public Token markAsServed(int tokenId) {
        Token token = tokenRepo.findById(tokenId).orElseThrow(() -> new RuntimeException("Token not found"));
        token.setStatus("Served");
        token.setStatusUpdateTime(LocalDateTime.now());
        return tokenRepo.save(token);
    }

    public Token skipToken(int tokenId) {
        Token token = tokenRepo.findById(tokenId).orElseThrow(() -> new RuntimeException("Token not found"));
        return handleNoShow(token);
    }

    @Scheduled(fixedRate = 10000)
    public void checkNoShows() {
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        List<Token> calledTokens = tokenRepo.findAll().stream()
                .filter(t -> "Called".equals(t.getStatus()) && t.getStatusUpdateTime().isBefore(oneMinuteAgo))
                .collect(Collectors.toList());

        for (Token token : calledTokens) {
            handleNoShow(token);
        }
    }

    private Token handleNoShow(Token token) {
        int missed = token.getMissedCallCount() + 1;
        token.setMissedCallCount(missed);
        
        if (missed >= 3) {
            token.setStatus("Expired");
        } else {
            token.setStatus("Waiting");
            // Update issue time so it goes to the back of its own priority queue
            token.setIssueTime(LocalDateTime.now());
        }
        token.setStatusUpdateTime(LocalDateTime.now());
        return tokenRepo.save(token);
    }

    // --- Emergency Escalation (Reception Staff) ---

    /**
     * Escalates a token to the absolute front of the queue.
     * Sets priorityValue to 0 (higher than Emergency=1) and issues a very early timestamp.
     */
    public Token escalateToFront(int tokenId) {
        Token token = tokenRepo.findById(tokenId)
                .orElseThrow(() -> new RuntimeException("Token not found!"));

        if (!"Waiting".equals(token.getStatus())) {
            throw new RuntimeException("Only tokens with 'Waiting' status can be escalated!");
        }

        token.setPriorityType("Emergency");
        token.setPriorityValue(0); // Highest priority (above normal Emergency=1)
        token.setIssueTime(LocalDateTime.of(2000, 1, 1, 0, 0)); // Ensures it's first in queue
        token.setStatusUpdateTime(LocalDateTime.now());
        return tokenRepo.save(token);
    }

    /**
     * Gets all active (Waiting) tokens for a given service, sorted by queue position.
     */
    public List<Token> getActiveTokensForService(int serviceId) {
        return tokenRepo.findAll().stream()
                .filter(t -> "Waiting".equals(t.getStatus()) && (serviceId == -1 || t.getService().getServiceId() == serviceId))
                .sorted(Comparator.comparing(Token::getPriorityValue)
                        .thenComparing(Token::getIssueTime))
                .collect(Collectors.toList());
    }

    private Token createToken(Citizen citizen, com.example.waitwise.models.Service service, String priorityRequest, String emergencyDescription, boolean isStaff) {
        String priority = determinePriority(citizen, priorityRequest, emergencyDescription, isStaff);
        int priorityValue = getPriorityValue(priority);

        String prefix = getServicePrefix(service.getServiceName());
        long count = tokenRepo.countByService(service) + 1;
        String tokenNumber = prefix + String.format("%03d", count);

        Token token = new Token();
        token.setCitizen(citizen);
        token.setService(service);
        token.setPriorityType(priority);
        token.setPriorityValue(priorityValue);
        token.setTokenNumber(tokenNumber);
        token.setIssueTime(LocalDateTime.now());
        token.setExpiryTime(LocalDateTime.now().plusHours(24));
        token.setStatus("Waiting");

        if (priority.equals("Golden")) {
            Revenue revenue = new Revenue();
            revenue.setAmount(500.0);
            revenue.setServiceName(service.getServiceName());
            revenue.setCitizen(citizen);
            revenueRepo.save(revenue);
        }

        Token saved = tokenRepo.save(token);

        // Notify citizen about token generation / payment
        new Thread(() -> {
            try {
                if (saved.getCitizen().getEmail() != null) {
                    if ("Golden".equals(saved.getPriorityType())) {
                        emailService.sendPaymentConfirmation(
                            saved.getCitizen().getEmail(),
                            saved.getCitizen().getFullName(),
                            saved.getTokenNumber(),
                            saved.getService().getServiceName()
                        );
                    } else {
                        emailService.sendEmail(
                            saved.getCitizen().getEmail(),
                            "WaitWise - Token Generated",
                            "Dear " + saved.getCitizen().getFullName() + ",\n\n" +
                            "Your token has been successfully generated.\n" +
                            "Token Number: " + saved.getTokenNumber() + "\n" +
                            "Priority: " + saved.getPriorityType() + "\n\n" +
                            "You can track your status on your dashboard."
                        );
                    }
                }
            } catch (Exception e) {}
        }).start();

        return saved;
    }

    private String determinePriority(Citizen citizen, String requestedPriority, String emergencyDescription, boolean isStaff) {
        if ("Golden".equalsIgnoreCase(requestedPriority)) return "Golden";
        if ("Emergency".equalsIgnoreCase(requestedPriority)) {
            // Staff can override emergency without description
            if (isStaff) return "Emergency";
            
            // Use NLP-based emergency detection for citizens
            if (EmergencyDetector.isEmergency(emergencyDescription)) return "Emergency";
            
            // If verification fails, do NOT auto-assign. Throw error so user can re-choose.
            throw new RuntimeException("Emergency claim not verified. Please provide a more detailed description or choose a different priority.");
        }
        
        // Auto assign based on age (for 'Auto' or failed emergency if we didn't throw)
        try {
            LocalDate dob = LocalDate.parse(citizen.getDateOfBirth());
            int age = Period.between(dob, LocalDate.now()).getYears();
            if (age >= 40) return "Senior";
        } catch (Exception e) {
            // Default if parsing fails
        }
        return "Normal";
    }

    /**
     * Priority values - UPDATED ORDER:
     * Emergency = 1 (highest - real emergencies come first)
     * Golden = 2 (paid priority)
     * Senior = 3
     * Normal = 4 (lowest)
     */
    private int getPriorityValue(String priority) {
        switch(priority) {
            case "Emergency": return 1;
            case "Golden": return 2;
            case "Senior": return 3;
            default: return 4;
        }
    }

    private String getServicePrefix(String serviceName) {
        if (serviceName.contains("CNIC")) return "C";
        if (serviceName.contains("Passport")) return "P";
        if (serviceName.contains("FRC")) return "F";
        if (serviceName.contains("Domicile")) return "D";
        if (serviceName.contains("Birth")) return "B";
        return "T";
    }
}