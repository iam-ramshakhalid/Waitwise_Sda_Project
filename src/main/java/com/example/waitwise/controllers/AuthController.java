package com.example.waitwise.controllers;

import com.example.waitwise.models.User;
import com.example.waitwise.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Frontend connectivity k liye
public class AuthController {

    @Autowired
    private UserService userService;

    @ExceptionHandler({RuntimeException.class, Exception.class})
    public ResponseEntity<java.util.Map<String, String>> handleException(Exception e) {
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/login") // Wapis Post kar diya (Ab ye browser link se direct test nahi hoga)
    public User login(@RequestParam String username, @RequestParam String password) {
        return userService.login(username, password);
    }

    @PostMapping("/register")
    public User register(@RequestBody java.util.Map<String, String> params) {
        String cnic = params.get("cnic");
        String password = params.get("password");
        String fullName = params.get("fullName");
        String phoneNumber = params.get("phoneNumber");
        String dateOfBirth = params.get("dateOfBirth");
        String email = params.get("email");

        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email is required but not found in the JSON body.");
        }

        return userService.registerCitizen(cnic, password, fullName, phoneNumber, dateOfBirth, email);
    }
}