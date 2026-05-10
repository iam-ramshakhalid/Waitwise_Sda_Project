package com.example.waitwise.repositories;
import com.example.waitwise.models.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TokenRepository extends JpaRepository<Token, Integer> {
    long countByService(com.example.waitwise.models.Service service);
    long countByServiceAndStatus(com.example.waitwise.models.Service service, String status);

    long countByIssueTimeBetween(LocalDateTime start, LocalDateTime end);
    long countByStatusAndStatusUpdateTimeBetween(String status, LocalDateTime start, LocalDateTime end);

    List<Token> findByIssueTimeBetween(LocalDateTime start, LocalDateTime end);
    List<Token> findByStatusAndStatusUpdateTimeBetween(String status, LocalDateTime start, LocalDateTime end);

    @Query("SELECT t.citizen FROM Token t WHERE t.status = 'Expired' AND t.issueTime >= :since GROUP BY t.citizen HAVING COUNT(t) >= 3")
    List<com.example.waitwise.models.Citizen> findBlacklistedCitizens(@Param("since") LocalDateTime since);

    // M2/M3: Count tokens per service within a date range
    long countByServiceAndIssueTimeBetween(com.example.waitwise.models.Service service, LocalDateTime start, LocalDateTime end);

    // M3: Count served tokens per service within a date range
    long countByServiceAndStatusAndStatusUpdateTimeBetween(com.example.waitwise.models.Service service, String status, LocalDateTime start, LocalDateTime end);

    // M4: Find tokens for a specific service (for staff performance)
    List<Token> findByServiceAndStatusAndStatusUpdateTimeBetween(com.example.waitwise.models.Service service, String status, LocalDateTime start, LocalDateTime end);

    // M4: Find all tokens for a service within a date range
    List<Token> findByServiceAndIssueTimeBetween(com.example.waitwise.models.Service service, LocalDateTime start, LocalDateTime end);
}