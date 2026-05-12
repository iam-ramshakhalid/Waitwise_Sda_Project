package com.example.waitwise.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            System.out.println("Email sent successfully to: " + to);
        } catch (Exception e) {
            System.err.println("Failed to send email to: " + to + " Error: " + e.getMessage());
        }
    }

    public void sendRegistrationSuccess(String to, String name) {
        String subject = "Welcome to WaitWise - Registration Confirmed";
        String body = "Dear " + name + ",\n\n" +
                "Welcome to WaitWise! Your registration as a citizen has been successful.\n" +
                "You can now login to your dashboard to generate tokens and track your queue status in real-time.\n\n" +
                "Thank you for choosing WaitWise.\n" +
                "Best Regards,\n" +
                "WaitWise Team";
        sendEmail(to, subject, body);
    }

    public void sendPaymentConfirmation(String to, String name, String tokenNumber, String serviceName) {
        String subject = "WaitWise - Priority Token Generated";
        String body = "Dear " + name + ",\n\n" +
                "Your payment was successful and your Golden Priority token has been generated.\n\n" +
                "Token Number: " + tokenNumber + "\n" +
                "Service: " + serviceName + "\n\n" +
                "Please keep an eye on your dashboard for live updates.\n\n" +
                "Best Regards,\n" +
                "WaitWise Team";
        sendEmail(to, subject, body);
    }

    public void sendApproachNotification(String to, String name, String tokenNumber, int peopleAhead) {
        String subject = "WaitWise - Your Turn is Approaching!";
        String body = "Dear " + name + ",\n\n" +
                "This is a friendly reminder that your turn is approaching for Token #" + tokenNumber + ".\n" +
                "There are only " + peopleAhead + " people ahead of you in the queue.\n\n" +
                "Please stay nearby or proceed towards the service counter.\n\n" +
                "Best Regards,\n" +
                "WaitWise Team";
        sendEmail(to, subject, body);
    }

    public void sendTurnUpNotification(String to, String name, String tokenNumber, String counterId) {
        String subject = "WaitWise - IT'S YOUR TURN!";
        String body = "Dear " + name + ",\n\n" +
                "It is now your turn for Token #" + tokenNumber + "!\n" +
                "Please proceed immediately to Counter " + counterId + ".\n\n" +
                "Thank you for your patience.\n" +
                "Best Regards,\n" +
                "WaitWise Team";
        sendEmail(to, subject, body);
    }

    public void sendRefundNotification(String to, String name, String tokenNumber, double amount) {
        String subject = "WaitWise - Refund Processed";
        String body = "Dear " + name + ",\n\n" +
                "Your Golden Priority token (#" + tokenNumber + ") has been cancelled.\n" +
                "As per our policy, your payment of Rs. " + amount + " has been initiated for refund to your original payment method.\n\n" +
                "It may take 3-5 business days to reflect in your account.\n\n" +
                "Best Regards,\n" +
                "WaitWise Team";
        sendEmail(to, subject, body);
    }
}
