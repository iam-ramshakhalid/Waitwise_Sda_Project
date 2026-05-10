package com.example.waitwise.controllers;

import com.example.waitwise.models.Token;
import com.example.waitwise.repositories.ServiceRepository;
import com.example.waitwise.repositories.TokenRepository;
import com.example.waitwise.services.TokenService;
import com.example.waitwise.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reception")
@CrossOrigin(origins = "*") // Frontend se connect karne k liye lazmi hai
public class ReceptionController {

    @Autowired
    private TokenService tokenService;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private UserService userService;

    @ExceptionHandler(RuntimeException.class)
    public org.springframework.http.ResponseEntity<Map<String, String>> handleException(RuntimeException e) {
        Map<String, String> response = new HashMap<>();
        response.put("message", e.getMessage());
        return org.springframework.http.ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/ping")
    public String ping() {
        return "Reception Controller is LIVE Boss!";
    }

    @PostMapping("/issue-token")
    public Token issueToken(@RequestParam(required = false) String cnic, @RequestParam int serviceId, @RequestParam(required = false, defaultValue = "Normal") String priority) {
        if (cnic == null || cnic.trim().isEmpty()) {
            return tokenService.generateWalkInToken(serviceId, priority);
        }
        return tokenService.generateNewToken(cnic, serviceId, priority, "", true);
    }

    // Get tokens for escalation list
    @GetMapping("/active-tokens")
    public List<Map<String, Object>> getActiveTokens(@RequestParam int serviceId) {
        List<Token> tokens = tokenService.getActiveTokensForService(serviceId);
        return tokens.stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("tokenId", t.getTokenId());
            map.put("tokenNumber", t.getTokenNumber());
            map.put("priorityType", t.getPriorityType());
            map.put("citizenName", t.getCitizenName());
            map.put("citizenCnic", t.getCitizenCnic());
            map.put("issueTime", t.getIssueTime().toString());
            return map;
        }).collect(Collectors.toList());
    }

    // Push token to front
    @PostMapping("/escalate")
    public Map<String, Object> escalateToken(@RequestParam int tokenId) {
        Token escalated = tokenService.escalateToFront(tokenId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Token " + escalated.getTokenNumber() + " has been escalated to the front of the queue.");
        response.put("token", escalated);
        return response;
    }

    @GetMapping("/queue-summary")
    public List<Map<String, Object>> getQueueSummary() {
        return serviceRepository.findAll().stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("serviceId", s.getServiceId());
            map.put("serviceName", s.getServiceName());
            map.put("waitingCount", tokenRepository.countByServiceAndStatus(s, "Waiting"));
            map.put("avgWait", s.getAverageWaitTimeMinutes());
            return map;
        }).collect(Collectors.toList());
    }
}