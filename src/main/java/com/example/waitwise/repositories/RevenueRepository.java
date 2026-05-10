package com.example.waitwise.repositories;

import com.example.waitwise.models.Citizen;
import com.example.waitwise.models.Revenue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RevenueRepository extends JpaRepository<Revenue, Integer> {
    @Query("SELECT COALESCE(SUM(r.amount), 0.0) FROM Revenue r")
    Double getTotalRevenue();

    List<Revenue> findByCitizenAndServiceName(Citizen citizen, String serviceName);

    @Transactional
    void deleteByCitizenAndServiceName(Citizen citizen, String serviceName);

    // M6: Financial audit — date range queries
    List<Revenue> findByTransactionTimeBetween(LocalDateTime start, LocalDateTime end);

    long countByTransactionTimeBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(r.amount), 0.0) FROM Revenue r WHERE r.transactionTime BETWEEN :start AND :end")
    Double getRevenueByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
