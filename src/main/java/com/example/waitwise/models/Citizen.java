package com.example.waitwise.models;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Citizens")
public class Citizen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CitizenID")
    private int citizenId;

    @Column(name = "CNIC", unique = true, nullable = false)
    private String cnic;

    @Column(name = "FullName")
    private String fullName;

    @Column(name = "PhoneNumber")
    private String phoneNumber;

    @Column(name = "NoShowCount")
    private int noShowCount = 0;

    @Column(name = "DateOfBirth")
    private String dateOfBirth;

    @Column(name = "Email")
    private String email;

    @Column(name = "IsBlacklisted")
    private Boolean isBlacklisted = false;

    @Column(name = "BlacklistReleaseDate")
    private LocalDateTime blacklistReleaseDate;

    public int getCitizenId() { return citizenId; }
    public void setCitizenId(int citizenId) { this.citizenId = citizenId; }
    public String getCnic() { return cnic; }
    public void setCnic(String cnic) { this.cnic = cnic; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public int getNoShowCount() { return noShowCount; }
    public void setNoShowCount(int noShowCount) { this.noShowCount = noShowCount; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public Boolean isBlacklisted() { 
        return isBlacklisted == null ? false : isBlacklisted; 
    }
    public void setBlacklisted(Boolean blacklisted) { this.isBlacklisted = blacklisted; }
    
    public LocalDateTime getBlacklistReleaseDate() { return blacklistReleaseDate; }
    public void setBlacklistReleaseDate(LocalDateTime blacklistReleaseDate) { this.blacklistReleaseDate = blacklistReleaseDate; }
}