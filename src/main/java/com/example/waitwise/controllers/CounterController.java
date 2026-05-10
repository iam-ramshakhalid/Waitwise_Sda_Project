package com.example.waitwise.controllers;

import com.example.waitwise.models.Token;
import com.example.waitwise.repositories.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/counter")
@CrossOrigin(origins = "*")
public class CounterController {

    @Autowired
    private com.example.waitwise.services.TokenService tokenService;

    @PostMapping("/call-next")
    public Token callNextToken(@RequestParam(defaultValue = "-1") int serviceId) {
        return tokenService.callNextToken(serviceId);
    }

    @PostMapping("/start-serving")
    public Token startServing(@RequestParam int tokenId) {
        return tokenService.startServing(tokenId);
    }

    @PostMapping("/mark-served")
    public Token markAsServed(@RequestParam int tokenId) {
        return tokenService.markAsServed(tokenId);
    }

    @PostMapping("/skip")
    public Token skipToken(@RequestParam int tokenId) {
        return tokenService.skipToken(tokenId);
    }

    @GetMapping("/queue")
    public List<Token> getQueue(@RequestParam(defaultValue = "-1") int serviceId) {
        return tokenService.getActiveTokensForService(serviceId);
    }
}