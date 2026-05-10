package com.example.waitwise.controllers;

import com.example.waitwise.models.Citizen;
import com.example.waitwise.models.Service;
import com.example.waitwise.models.Token;
import com.example.waitwise.models.User;
import com.example.waitwise.repositories.CitizenRepository;
import com.example.waitwise.repositories.RevenueRepository;
import com.example.waitwise.repositories.ServiceRepository;
import com.example.waitwise.repositories.TokenRepository;
import com.example.waitwise.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private RevenueRepository revenueRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private CitizenRepository citizenRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        long generatedToday = tokenRepository.countByIssueTimeBetween(startOfDay, endOfDay);
        long servedToday = tokenRepository.countByStatusAndStatusUpdateTimeBetween("Served", startOfDay, endOfDay);
        long expiredToday = tokenRepository.countByStatusAndStatusUpdateTimeBetween("Expired", startOfDay, endOfDay);
        long cancelledToday = tokenRepository.countByStatusAndStatusUpdateTimeBetween("Cancelled", startOfDay, endOfDay);
        Double totalRevenue = revenueRepository.getTotalRevenue();

        Map<String, Object> stats = new HashMap<>();
        stats.put("generatedToday", generatedToday);
        stats.put("servedToday", servedToday);
        stats.put("expiredToday", expiredToday);
        stats.put("cancelledToday", cancelledToday);
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/reports/tokens")
    public ResponseEntity<List<Map<String, Object>>> getTokenReports(@RequestParam String type) {
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        List<Token> tokens;
        
        if ("generated".equals(type)) {
            tokens = tokenRepository.findByIssueTimeBetween(startOfDay, endOfDay);
        } else if ("served".equals(type)) {
            tokens = tokenRepository.findByStatusAndStatusUpdateTimeBetween("Served", startOfDay, endOfDay);
        } else if ("expired".equals(type)) {
            tokens = tokenRepository.findByStatusAndStatusUpdateTimeBetween("Expired", startOfDay, endOfDay);
        } else if ("cancelled".equals(type)) {
            tokens = tokenRepository.findByStatusAndStatusUpdateTimeBetween("Cancelled", startOfDay, endOfDay);
        } else {
            return ResponseEntity.badRequest().build();
        }

        List<Map<String, Object>> response = tokens.stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("tokenNumber", t.getTokenNumber());
            map.put("serviceName", t.getService().getServiceName());
            map.put("priority", t.getPriorityType());
            map.put("citizenName", t.getCitizen().getFullName());
            map.put("time", t.getIssueTime().toString());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/reports/revenue")
    public ResponseEntity<List<Map<String, Object>>> getRevenueReport() {
        List<com.example.waitwise.models.Revenue> revenues = revenueRepository.findAll();
        List<Map<String, Object>> response = revenues.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("amount", r.getAmount());
            map.put("serviceName", r.getServiceName());
            map.put("citizenName", r.getCitizen().getFullName());
            map.put("time", r.getTransactionTime().toString());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/service-usage")
    public ResponseEntity<List<Map<String, Object>>> getServiceUsage() {
        List<Service> services = serviceRepository.findAll();
        List<Map<String, Object>> usage = services.stream().map(service -> {
            Map<String, Object> map = new HashMap<>();
            map.put("serviceName", service.getServiceName());
            
            // Get LIVE all-time count from repository instead of the static field
            long totalServed = tokenRepository.countByServiceAndStatus(service, "Served");
            map.put("totalServed", totalServed);
            
            long tokensGenerated = tokenRepository.countByService(service);
            map.put("tokensGenerated", tokensGenerated);
            map.put("averageWaitTime", service.getAverageWaitTimeMinutes());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(usage);
    }

    @GetMapping("/blacklisted")
    public ResponseEntity<List<Map<String, Object>>> getBlacklistedCitizens() {
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        List<Citizen> blacklisted = tokenRepository.findBlacklistedCitizens(oneMonthAgo);

        List<Map<String, Object>> response = blacklisted.stream().map(citizen -> {
            Map<String, Object> map = new HashMap<>();
            map.put("cnic", citizen.getCnic());
            map.put("fullName", citizen.getFullName());
            map.put("phoneNumber", citizen.getPhoneNumber());
            map.put("noShowCount", citizen.getNoShowCount());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // ─── Staff Management (M5) ──────────────────────────────────────────

    @GetMapping("/staff/stats")
    public ResponseEntity<Map<String, Object>> getStaffStats() {
        long totalStaff = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && (
                    u.getRole().startsWith("CounterStaff") || 
                    "ReceptionStaff".equals(u.getRole())
                )).count();

        long counterStaff = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && u.getRole().startsWith("CounterStaff"))
                .count();

        long receptionStaff = userRepository.countByRole("ReceptionStaff");

        long activeStaff = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && (
                    u.getRole().startsWith("CounterStaff") || 
                    "ReceptionStaff".equals(u.getRole())
                ))
                .filter(u -> u.getActive())
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStaff", totalStaff);
        stats.put("counterStaff", counterStaff);
        stats.put("receptionStaff", receptionStaff);
        stats.put("activeStaff", activeStaff);
        stats.put("inactiveStaff", totalStaff - activeStaff);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/staff")
    public ResponseEntity<List<Map<String, Object>>> getAllStaff() {
        List<User> staff = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && (
                    u.getRole().startsWith("CounterStaff") || 
                    "ReceptionStaff".equals(u.getRole())
                ))
                .collect(Collectors.toList());

        List<Map<String, Object>> response = staff.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("userId", s.getUserId());
            map.put("fullName", s.getFullName());
            map.put("username", s.getUsername());
            map.put("password", s.getPassword());
            map.put("role", s.getRole());
            map.put("phoneNumber", s.getPhoneNumber());
            map.put("email", s.getEmail());
            map.put("createdAt", s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
            map.put("active", s.getActive());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/staff")
    public ResponseEntity<?> createStaff(@RequestBody Map<String, String> payload) {
        String fullName = payload.get("fullName");
        String username = payload.get("username");
        String password = payload.get("password");
        String role = payload.get("role");
        String phoneNumber = payload.get("phoneNumber");
        String email = payload.get("email");

        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username is required"));
        }
        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password is required"));
        }
        if (role == null || role.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Role is required"));
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Full name is required"));
        }

        if (email != null && !email.toLowerCase().endsWith("@gmail.com")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Only @gmail.com addresses are allowed"));
        }

        if (phoneNumber != null && phoneNumber.length() != 11) {
            return ResponseEntity.badRequest().body(Map.of("message", "Phone number must be exactly 11 digits"));
        }

        // M5: Check for duplicate staff ID
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Staff ID already exists"));
        }

        boolean validRole = "ReceptionStaff".equals(role) || role.startsWith("CounterStaff");
        if (!validRole) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid role. Must be ReceptionStaff or CounterStaff"));
        }

        User user = new User();
        user.setFullName(fullName);
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        user.setPhoneNumber(phoneNumber);
        user.setEmail(email);
        user.setCreatedAt(LocalDateTime.now());
        user.setActive(true);

        User saved = userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", saved.getUserId());
        response.put("fullName", saved.getFullName());
        response.put("username", saved.getUsername());
        response.put("password", saved.getPassword());
        response.put("role", saved.getRole());
        response.put("phoneNumber", saved.getPhoneNumber());
        response.put("email", saved.getEmail());
        response.put("createdAt", saved.getCreatedAt().toString());
        response.put("active", saved.getActive());
        response.put("message", "Staff account created successfully!");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/staff/{id}")
    public ResponseEntity<?> updateStaff(@PathVariable int id, @RequestBody Map<String, String> payload) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Staff member not found"));
        }

        if (payload.containsKey("fullName") && payload.get("fullName") != null) {
            user.setFullName(payload.get("fullName"));
        }
        if (payload.containsKey("password") && payload.get("password") != null && !payload.get("password").isEmpty()) {
            user.setPassword(payload.get("password"));
        }
        if (payload.containsKey("role") && payload.get("role") != null) {
            String role = payload.get("role");
            boolean validRole = "ReceptionStaff".equals(role) || role.startsWith("CounterStaff");
            if (!validRole) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid role"));
            }
            user.setRole(role);
        }
        if (payload.containsKey("phoneNumber")) {
            String phone = payload.get("phoneNumber");
            if (phone != null && phone.length() != 11) {
                return ResponseEntity.badRequest().body(Map.of("message", "Phone number must be exactly 11 digits"));
            }
            user.setPhoneNumber(phone);
        }
        if (payload.containsKey("email")) {
            String email = payload.get("email");
            if (email != null && !email.toLowerCase().endsWith("@gmail.com")) {
                return ResponseEntity.badRequest().body(Map.of("message", "Only @gmail.com addresses are allowed"));
            }
            user.setEmail(email);
        }
        if (payload.containsKey("active")) {
            user.setActive("true".equals(payload.get("active")));
        }

        User saved = userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", saved.getUserId());
        response.put("fullName", saved.getFullName());
        response.put("username", saved.getUsername());
        response.put("password", saved.getPassword());
        response.put("role", saved.getRole());
        response.put("phoneNumber", saved.getPhoneNumber());
        response.put("email", saved.getEmail());
        response.put("createdAt", saved.getCreatedAt() != null ? saved.getCreatedAt().toString() : null);
        response.put("active", saved.getActive());
        response.put("message", "Staff updated successfully!");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/staff/{id}")
    public ResponseEntity<?> deleteStaff(@PathVariable int id) {
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Staff account deleted successfully"));
    }

    // ═══════════════════════════════════════════════════════════════
    // MODULE 2 — Generate Daily Service Reports (GUI based)
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/reports/daily")
    public ResponseEntity<Map<String, Object>> getDailyReport(
            @RequestParam(required = false) String date) {
        
        LocalDate reportDate;
        try {
            reportDate = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid date format. Use YYYY-MM-DD"));
        }

        LocalDateTime startOfDay = LocalDateTime.of(reportDate, LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(reportDate, LocalTime.MAX);

        long totalTokens = tokenRepository.countByIssueTimeBetween(startOfDay, endOfDay);
        long servedTokens = tokenRepository.countByStatusAndStatusUpdateTimeBetween("Served", startOfDay, endOfDay);
        long expiredTokens = tokenRepository.countByStatusAndStatusUpdateTimeBetween("Expired", startOfDay, endOfDay);
        long cancelledTokens = tokenRepository.countByStatusAndStatusUpdateTimeBetween("Cancelled", startOfDay, endOfDay);

        Map<String, Object> report = new HashMap<>();
        report.put("date", reportDate.toString());
        report.put("totalTokens", totalTokens);
        report.put("servedTokens", servedTokens);
        report.put("expiredTokens", expiredTokens);
        report.put("cancelledTokens", cancelledTokens);
        report.put("hasData", totalTokens > 0);

        return ResponseEntity.ok(report);
    }

    // ═══════════════════════════════════════════════════════════════
    // MODULE 3 — Monitor Service Usage
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/service-usage/period")
    public ResponseEntity<Map<String, Object>> getServiceUsageByPeriod(
            @RequestParam(defaultValue = "month") String period) {
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate;

        switch (period) {
            case "week":   startDate = now.minusWeeks(1); break;
            case "month":  startDate = now.minusMonths(1); break;
            case "3months": startDate = now.minusMonths(3); break;
            case "6months": startDate = now.minusMonths(6); break;
            case "year":   startDate = now.minusYears(1); break;
            default:       startDate = now.minusMonths(1);
        }

        List<Service> services = serviceRepository.findAll();
        String busiestService = "";
        long maxTokens = 0;

        List<Map<String, Object>> serviceStats = services.stream().map(service -> {
            Map<String, Object> map = new HashMap<>();
            map.put("serviceName", service.getServiceName());
            long generated = tokenRepository.countByServiceAndIssueTimeBetween(service, startDate, now);
            long served = tokenRepository.countByServiceAndStatusAndStatusUpdateTimeBetween(service, "Served", startDate, now);
            long expired = tokenRepository.countByServiceAndStatusAndStatusUpdateTimeBetween(service, "Expired", startDate, now);
            map.put("tokensGenerated", generated);
            map.put("tokensServed", served);
            map.put("tokensExpired", expired);
            return map;
        }).collect(Collectors.toList());

        for (Map<String, Object> stat : serviceStats) {
            long gen = (long) stat.get("tokensGenerated");
            if (gen > maxTokens) {
                maxTokens = gen;
                busiestService = (String) stat.get("serviceName");
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("services", serviceStats);
        response.put("busiestService", busiestService);
        response.put("busiestServiceTokens", maxTokens);
        response.put("period", period);

        return ResponseEntity.ok(response);
    }

    // ═══════════════════════════════════════════════════════════════
    // MODULE 4 — Evaluate Staff Performance
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/staff/{id}/performance")
    public ResponseEntity<Map<String, Object>> getStaffPerformance(@PathVariable int id) {
        User staff = userRepository.findById(id).orElse(null);
        if (staff == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Staff member not found"));
        }

        Map<String, Object> performance = new HashMap<>();
        performance.put("staffId", staff.getUserId());
        performance.put("staffName", staff.getFullName());
        performance.put("role", staff.getRole());

        String role = staff.getRole();
        if (role == null || (!role.startsWith("CounterStaff") && !"ReceptionStaff".equals(role))) {
            return ResponseEntity.badRequest().body(Map.of("message", "Not a staff member"));
        }

        if (role.startsWith("CounterStaff")) {
            // Since counters are now unified, we track overall performance
            // For now, we return a general staff overview
            performance.put("message", "General performance metrics (Global Pool enabled)");
            performance.put("tokensServed", "N/A"); // Will need servedBy field in Token for detailed stats
        } else {
            performance.put("tokensServed", 0);
            performance.put("avgServiceTime", 0);
            performance.put("noShowCount", 0);
            performance.put("message", "Reception staff performance metrics are not applicable.");
        }

        return ResponseEntity.ok(performance);
    }

    // ═══════════════════════════════════════════════════════════════
    // MODULE 6 — Audit Financial Throughput
    // ═══════════════════════════════════════════════════════════════

    @GetMapping("/reports/financial")
    public ResponseEntity<Map<String, Object>> getFinancialReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LocalDateTime start, end;
        try {
            start = (startDate != null && !startDate.isEmpty())
                    ? LocalDateTime.of(LocalDate.parse(startDate), LocalTime.MIN)
                    : LocalDateTime.of(LocalDate.now().minusMonths(1), LocalTime.MIN);
            end = (endDate != null && !endDate.isEmpty())
                    ? LocalDateTime.of(LocalDate.parse(endDate), LocalTime.MAX)
                    : LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid date format. Use YYYY-MM-DD"));
        }

        List<com.example.waitwise.models.Revenue> transactions = revenueRepository.findByTransactionTimeBetween(start, end);
        long totalSold = transactions.size();
        Double totalRevenue = revenueRepository.getRevenueByDateRange(start, end);

        List<Map<String, Object>> transactionList = transactions.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("revenueId", r.getRevenueId());
            map.put("amount", r.getAmount());
            map.put("serviceName", r.getServiceName());
            map.put("citizenName", r.getCitizen().getFullName());
            map.put("transactionTime", r.getTransactionTime().toString());
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> report = new HashMap<>();
        report.put("startDate", start.toLocalDate().toString());
        report.put("endDate", end.toLocalDate().toString());
        report.put("totalSold", totalSold);
        report.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
        report.put("transactions", transactionList);
        report.put("hasData", totalSold > 0);

        return ResponseEntity.ok(report);
    }
}
