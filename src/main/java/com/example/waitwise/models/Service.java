package com.example.waitwise.models;
import jakarta.persistence.*;

@Entity
@Table(name = "Services")
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ServiceID")
    private int serviceId;

    @Column(name = "ServiceName")
    private String serviceName;

    @Column(name = "AverageWaitTimeMinutes")
    private Integer averageWaitTimeMinutes = 5; // Default 5 mins

    @Column(name = "TotalPeopleServed")
    private Integer totalPeopleServed = 0;

    @Column(name = "TotalServiceTimeMinutes")
    private Integer totalServiceTimeMinutes = 0;

    public int getServiceId() { return serviceId; }
    public void setServiceId(int serviceId) { this.serviceId = serviceId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    
    public Integer getAverageWaitTimeMinutes() { 
        return averageWaitTimeMinutes == null ? 5 : averageWaitTimeMinutes; 
    }
    public void setAverageWaitTimeMinutes(Integer averageWaitTimeMinutes) { this.averageWaitTimeMinutes = averageWaitTimeMinutes; }

    public Integer getTotalPeopleServed() { return totalPeopleServed == null ? 0 : totalPeopleServed; }
    public void setTotalPeopleServed(Integer totalPeopleServed) { this.totalPeopleServed = totalPeopleServed; }
    public Integer getTotalServiceTimeMinutes() { return totalServiceTimeMinutes == null ? 0 : totalServiceTimeMinutes; }
    public void setTotalServiceTimeMinutes(Integer totalServiceTimeMinutes) { this.totalServiceTimeMinutes = totalServiceTimeMinutes; }
}