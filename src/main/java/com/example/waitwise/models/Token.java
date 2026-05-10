package com.example.waitwise.models;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "Tokens")
public class Token {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TokenID")
    private int tokenId;
    @Column(name = "TokenNumber")
    private String tokenNumber;
    @Column(name = "PriorityType")
    private String priorityType;
    @Column(name = "Status")
    private String status = "Waiting";
    @Column(name = "IssueTime")
    private LocalDateTime issueTime = LocalDateTime.now();
    @ManyToOne
    @JoinColumn(name = "CitizenID")
    private Citizen citizen;
    @ManyToOne
    @JoinColumn(name = "ServiceID")
    private Service service;
    public int getTokenId() { return tokenId; }
    public void setTokenId(int tokenId) { this.tokenId = tokenId; }
    public String getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(String tokenNumber) { this.tokenNumber = tokenNumber; }
    public String getPriorityType() { return priorityType; }
    public void setPriorityType(String priorityType) { this.priorityType = priorityType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getIssueTime() { return issueTime; }
    public void setIssueTime(LocalDateTime issueTime) { this.issueTime = issueTime; }
    public Citizen getCitizen() { return citizen; }
    public void setCitizen(Citizen citizen) { this.citizen = citizen; }
    public Service getService() { return service; }
    public void setService(Service service) { this.service = service; }

    @Column(name = "ExpiryTime")
    private LocalDateTime expiryTime;
    
    @Column(name = "StatusUpdateTime")
    private LocalDateTime statusUpdateTime = LocalDateTime.now();

    @Column(name = "MissedCallCount")
    private int missedCallCount = 0;

    @Column(name = "PriorityValue")
    private int priorityValue;

    @Column(name = "ServiceStartTime")
    private LocalDateTime serviceStartTime;

    public LocalDateTime getExpiryTime() { return expiryTime; }
    public void setExpiryTime(LocalDateTime expiryTime) { this.expiryTime = expiryTime; }
    public LocalDateTime getStatusUpdateTime() { return statusUpdateTime; }
    public void setStatusUpdateTime(LocalDateTime statusUpdateTime) { this.statusUpdateTime = statusUpdateTime; }
    public int getMissedCallCount() { return missedCallCount; }
    public void setMissedCallCount(int missedCallCount) { this.missedCallCount = missedCallCount; }
    public int getPriorityValue() { return priorityValue; }
    public void setPriorityValue(int priorityValue) { this.priorityValue = priorityValue; }
    public LocalDateTime getServiceStartTime() { return serviceStartTime; }
    public void setServiceStartTime(LocalDateTime serviceStartTime) { this.serviceStartTime = serviceStartTime; }

    @Transient
    private int estimatedWaitTime;

    public int getEstimatedWaitTime() { return estimatedWaitTime; }
    public void setEstimatedWaitTime(int estimatedWaitTime) { this.estimatedWaitTime = estimatedWaitTime; }

    /**
     * Returns the citizen's full name for display on token cards and reports.
     */
    @Transient
    public String getCitizenName() {
        if (citizen != null) {
            return citizen.getFullName();
        }
        return "Unknown";
    }

    /**
     * Returns a masked version of the citizen's CNIC for privacy.
     * e.g., "35202-*****-3" from "3520212345673"
     */
    @Transient
    public String getCitizenCnic() {
        if (citizen != null && citizen.getCnic() != null) {
            String cnic = citizen.getCnic();
            if ("WALKIN".equals(cnic)) return "Walk-In";
            if (cnic.length() >= 13) {
                return cnic.substring(0, 5) + "-*****-" + cnic.substring(cnic.length() - 1);
            }
            return cnic;
        }
        return "N/A";
    }
}