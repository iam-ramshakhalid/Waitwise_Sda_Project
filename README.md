# WaitWise - Smart Queue Management System 🚀

**WaitWise** is a robust, role-based Smart Queue Management System designed to eliminate physical waiting in lines at heavy-footfall organizations (like NADRA offices). By providing an intelligent digital queue, WaitWise lets citizens skip the physical wait without skipping the service.

Developed as a Semester 4 Software Design & Analysis (SDA) course project, WaitWise features real-time queue tracking, role-based dashboards, and a custom Rule-Based NLP engine to dynamically detect and prioritize emergency cases.

---

## 🌟 Features by Role

### 👤 Citizens
- **Digital Registration:** Create accounts and request digital tokens online from anywhere.
- **Live Tracking Dashboard:** Monitor exact queue position and estimated wait time in real-time.
- **Form Memory:** Preserves form state locally when navigating pages for better UX.

### 🏢 Reception Staff
- **Walk-in Portal:** Manually issue and print tokens for citizens without the app.
- **Emergency Bar:** Immediately escalate critical or urgent cases to the front of the queue.

### 👨‍💻 Counter Staff
- **Streamlined Workflow:** Easily call the next token, process services, and mark citizens as "Served".
- **No-Show Tracking:** Ability to mark users as "No-Show", triggering automated strikes to prevent abuse.

### ⚙️ Administrators
- **Comprehensive Analytics:** Track daily service metrics, financial throughput, and tokens served/expired.
- **Staff Management:** Dynamically hire staff, assign counter roles, and evaluate individual staff performance.
- **Blacklist Management:** Automatically flags and temporarily blacklists citizens with too many no-shows.

---

## 🧠 Under The Hood: Technical Highlights

We engineered complex business logic using core Software Design & Analysis principles:

- **Rule-Based NLP Emergency Detector:** A custom algorithm featuring fuzzy typo-tolerance and negation handling to read user descriptions and automatically prioritize medical or life-threatening emergencies.
- **Automated Notifications:** Triggers email confirmations and notifications when tokens are issued.
- **Design Patterns in Action (Gang of Four):**
  - **Singleton:** Shared instances for database and queue logic across controllers.
  - **Factory Method:** Produces distinct token tiers (Normal, Senior, Emergency, Golden).
  - **Adapter:** Bridges our NLP text-analysis engine with rigid database priority scoring.
  - **Composite:** Groups tokens into overarching service queues for wait-time calculations.
  - **Template Method:** Fixes the token generation lifecycle (Validate → Blacklist Check → Assign Priority → Generate Number → Save → Email).
  - **Observer:** Empowers the Citizen Dashboard to automatically poll the backend for live updates and triggers automated emails.

---

## 💻 Tech Stack

- **Backend:** Java, Spring Boot, Hibernate ORM
- **Database:** Microsoft SQL Server (MS SQL)
- **Frontend:** HTML5, CSS3, JavaScript (Vanilla), Chart.js for analytics
- **Build Tool:** Maven

---

## 🛠️ Setup & Deployment Guide

### 1. Prerequisites
- **Java Development Kit (JDK)** 17 or higher
- **Microsoft SQL Server** and SQL Server Management Studio (SSMS)
- **Maven** (optional — the project includes the `mvnw` wrapper)

### 2. Database Configuration
1. Open SQL Server Management Studio (SSMS) and connect to your local server.
2. Create the database by running:
   ```sql
   CREATE DATABASE Wait_Wise;
   ```
3. Open `src/main/resources/application.properties` and confirm that `spring.datasource.username` and `password` match your SQL Server instance credentials. *(Note: Ensure TCP/IP is enabled in SQL Server Configuration Manager).*

### 3. Compiling and Building
From the project root directory, run:
```bash
# On Windows
.\mvnw.cmd clean package

# On Mac/Linux
./mvnw clean package
```
*This generates a `.jar` file inside the `target/` directory.*

### 4. Running the Application
**Method A — Maven (Recommended for Development)**
```bash
.\mvnw.cmd spring-boot:run
```

**Method B — Compiled JAR**
```bash
java -jar target/WaitWise-0.0.1-SNAPSHOT.jar
```
*Once started, the backend and frontend will be live on `http://localhost:8082` (or whatever port is configured in application.properties).*

### 5. Accessing the Interface

| Page | Local URL |
| :--- | :--- |
| **Landing Page** | [http://localhost:8082/index.html](http://localhost:8082/index.html) |
| **Admin Portal** | [http://localhost:8082/admin-login.html](http://localhost:8082/admin-login.html) |
| **Citizen Dashboard** | [http://localhost:8082/citizen-dashboard.html](http://localhost:8082/citizen-dashboard.html) |
| **Staff Login** | [http://localhost:8082/staff-login.html](http://localhost:8082/staff-login.html) |

---

## ⚠️ Troubleshooting

- **Port Conflict:** If port 8082 (or 8081) is in use, simply change `server.port` in `application.properties`.
- **Database Connection Error:** Double-check your SQL Server TCP/IP settings and authentication credentials.
- **Emails Not Sending:** Verify your internet connection or check that the Gmail App Password in `application.properties` is valid.
