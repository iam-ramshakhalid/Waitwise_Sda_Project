package com.example.waitwise.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Revenue")
public class Revenue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RevenueID")
    private int revenueId;

    @Column(name = "Amount")
    private double amount;

    @Column(name = "ServiceName")
    private String serviceName;

    @Column(name = "TransactionTime")
    private LocalDateTime transactionTime = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "CitizenID")
    private Citizen citizen;

    public int getRevenueId() { return revenueId; }
    public void setRevenueId(int revenueId) { this.revenueId = revenueId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public LocalDateTime getTransactionTime() { return transactionTime; }
    public void setTransactionTime(LocalDateTime transactionTime) { this.transactionTime = transactionTime; }

    public Citizen getCitizen() { return citizen; }
    public void setCitizen(Citizen citizen) { this.citizen = citizen; }
}
