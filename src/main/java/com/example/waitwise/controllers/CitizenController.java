package com.example.waitwise.controllers;

import com.example.waitwise.models.Token;
import com.example.waitwise.models.Citizen;
import com.example.waitwise.repositories.TokenRepository;
import com.example.waitwise.repositories.CitizenRepository;
import com.example.waitwise.services.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/citizen")
@CrossOrigin(origins = "*")
public class CitizenController {

    @ExceptionHandler(RuntimeException.class)
    public org.springframework.http.ResponseEntity<Map<String, String>> handleException(RuntimeException e) {
        Map<String, String> response = new HashMap<>();
        response.put("message", e.getMessage());
        return org.springframework.http.ResponseEntity.badRequest().body(response);
    }

    @Autowired private TokenRepository tokenRepo;
    @Autowired private CitizenRepository citizenRepo;
    @Autowired private TokenService tokenService;

    @GetMapping("/dashboard-metrics")
    public Map<String, Object> getDashboardMetrics(@RequestParam String cnic) {
        Citizen citizen = citizenRepo.findByCnic(cnic).orElseThrow(() -> new RuntimeException("Citizen not found"));
        
        List<Token> allTokens = tokenRepo.findAll().stream()
                .filter(t -> t.getCitizen().getCitizenId() == citizen.getCitizenId())
                .collect(Collectors.toList());

        long active = allTokens.stream().filter(t -> t.getStatus().equals("Waiting")).count();
        long served = allTokens.stream().filter(t -> t.getStatus().equals("Served")).count();
        long cancelled = allTokens.stream().filter(t -> t.getStatus().equals("Cancelled") || t.getStatus().equals("Expired")).count();

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("citizenName", citizen.getFullName());
        metrics.put("active", active);
        metrics.put("served", served);
        metrics.put("cancelled", cancelled);
        
        allTokens.sort((t1, t2) -> t2.getIssueTime().compareTo(t1.getIssueTime()));
        List<Token> recentTokens = allTokens.stream().limit(5).collect(Collectors.toList());

        for (Token t : recentTokens) {
            if ("Waiting".equals(t.getStatus())) {
                int position = calculateQueuePosition(t);
                t.setEstimatedWaitTime(position * t.getService().getAverageWaitTimeMinutes());
            } else {
                t.setEstimatedWaitTime(0);
            }
        }

        metrics.put("recentTokens", recentTokens);

        return metrics;
    }

    @PostMapping("/issue-token")
    public Map<String, Object> issueToken(@RequestParam String cnic, @RequestParam(required = false, defaultValue = "0") int serviceId, @RequestParam(required = false) String serviceName, @RequestParam String priorityRequest, @RequestParam(required = false) String emergencyDescription) {
        Token token;
        if (serviceId > 0) {
            token = tokenService.generateNewToken(cnic, serviceId, priorityRequest, emergencyDescription);
        } else if (serviceName != null && !serviceName.isEmpty()) {
            token = tokenService.generateNewTokenByName(cnic, serviceName, priorityRequest, emergencyDescription);
        } else {
            throw new RuntimeException("Must provide serviceId or serviceName");
        }
        
        int position = calculateQueuePosition(token);
        int waitTime = position * token.getService().getAverageWaitTimeMinutes();
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("queuePosition", position);
        response.put("estimatedWaitTime", waitTime);
        return response;
    }

    @GetMapping("/token-history")
    public List<Token> getTokenHistory(@RequestParam String cnic) {
        Citizen citizen = citizenRepo.findByCnic(cnic).orElse(null);
        if (citizen == null) return new ArrayList<>();
        List<Token> history = tokenRepo.findAll().stream()
                .filter(t -> t.getCitizen().getCitizenId() == citizen.getCitizenId())
                .sorted((t1, t2) -> t2.getIssueTime().compareTo(t1.getIssueTime()))
                .collect(Collectors.toList());
                
        for (Token t : history) {
            if ("Waiting".equals(t.getStatus())) {
                int position = calculateQueuePosition(t);
                t.setEstimatedWaitTime(position * t.getService().getAverageWaitTimeMinutes());
            } else {
                t.setEstimatedWaitTime(0);
            }
        }
        return history;
    }

    @PostMapping("/cancel-token")
    public Map<String, Object> cancelToken(@RequestParam int tokenId, @RequestParam String cnic) {
        Token cancelledToken = tokenService.cancelToken(tokenId, cnic);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Token " + cancelledToken.getTokenNumber() + " has been cancelled successfully.");
        response.put("token", cancelledToken);
        return response;
    }
    
    @GetMapping("/queue-status")
    public Map<String, Object> getQueueStatus(@RequestParam int tokenId) {
        Token token = tokenRepo.findById(tokenId).orElse(null);
        if (token == null) throw new RuntimeException("Token not found");
        
        int position = calculateQueuePosition(token);
        int waitTime = position * token.getService().getAverageWaitTimeMinutes();
        
        Token currentlyServing = tokenRepo.findAll().stream()
            .filter(t -> (t.getStatus().equals("Serving") || t.getStatus().equals("Called")) && t.getService().getServiceId() == token.getService().getServiceId())
            .findFirst().orElse(null);

        Map<String, Object> res = new HashMap<>();
        res.put("position", position);
        res.put("estimatedWaitTime", waitTime);
        res.put("currentlyServing", currentlyServing != null ? currentlyServing.getTokenNumber() : "None");
        res.put("status", token.getStatus());
        res.put("serviceId", token.getService().getServiceId());
        return res;
    }

    private int calculateQueuePosition(Token currentToken) {
        List<Token> activeQueue = tokenRepo.findAll().stream()
            .filter(t -> t.getStatus().equals("Waiting") && t.getService().getServiceId() == currentToken.getService().getServiceId())
            .sorted((t1, t2) -> {
                if (t1.getPriorityValue() != t2.getPriorityValue()) {
                    return Integer.compare(t1.getPriorityValue(), t2.getPriorityValue());
                }
                return t1.getIssueTime().compareTo(t2.getIssueTime());
            })
            .collect(Collectors.toList());
            
        for (int i = 0; i < activeQueue.size(); i++) {
            if (activeQueue.get(i).getTokenId() == currentToken.getTokenId()) {
                return i;
            }
        }
        return 0;
    }
}