# WAIT_WISE_Official

Section#1
WaitWise — Setup & Deployment Guide

1. Prerequisites
-Before you begin, ensure the following are installed on your machine:

-Java Development Kit (JDK) 17 or higher
-Microsoft SQL Server and SQL Server Management Studio (SSMS)
-Maven (optional — the project includes the mvnw wrapper)


2. Database Configuration

-Open SQL Server Management Studio (SSMS) and connect to your local server.
-Create the database by running:

-sql   CREATE DATABASE Wait_Wise;

-Open src/main/resources/application.properties and confirm the spring.datasource username and password match your SQL Server instance.


3. Compiling and Building
-From the project root directory, run:
-bash./mvnw clean package

On Windows Command Prompt, use mvnw.cmd instead of ./mvnw. This generates a .jar file inside the target/ directory.


4. Running the Application
Method A — Maven (Recommended for Development)
bash./mvnw spring-boot:run
Method B — Compiled JAR
bashjava -jar target/WaitWise-0.0.1-SNAPSHOT.jar
Once started, the backend will be live at: http://localhost:8081

5. Accessing the Interface
Page                 URLMain 
Landing Page        http://localhost:8081/index.html
Admin Portal        http://localhost:8081/admin-login.html
Citizen Dashboard   http://localhost:8081/citizen-dashboard.html
Staff Login         http://localhost:8081/staff-login.html

6. Troubleshooting

Port conflict — If port 8081 is in use, change server.port in application.properties.
Database connection error — Make sure TCP/IP is enabled in SQL Server Configuration Manager.
Emails not sending — Verify your internet connection or check that the Gmail App Password in application.properties is correct.


Section#2
Design Patterns Used in WaitWise

1-Creational

-Singleton — All @Service and @RestController classes are Spring Boot singletons, ensuring one shared instance handles database and queue logic.
-Factory Method — TokenService.createToken() acts as a factory, producing the correct token type (Normal, Senior, Emergency, Golden) based on input, keeping that logic out of the controllers.

2-Structural

-Adapter — EmergencyDetector converts free-text descriptions into a numerical priority score, bridging human language and the rigid database model.
-Composite — A Service (e.g. CNIC) groups multiple tokens into a queue, letting the system treat a single token and an entire service queue the same way for wait time calculations and reports.

3-Behavioral

-Template Method — Token generation always follows the same fixed steps: Validate → Blacklist Check → Assign Priority → Generate Number → Save → Email. Only the details change, not the sequence.
-Observer — The citizen dashboard polls the backend every 15 seconds and reacts to queue changes. The EmailService also acts as an observer, firing a confirmation email whenever a token is issued.

Section#3
-SMS feature
(Already handled in app notifications and email notification)



Section#4
1:Emergency detector(Rule-based NLP engine)
-Emergency Detection: Uses a weighted keyword-matching algorithm to score citizen descriptions based on predefined categories (Medical, Life-Threatening, etc.).
-Accuracy Logic: The engine includes negation handling (e.g., "no emergency") and typo-tolerance (fuzzy algo) to ensure genuine requests are prioritized correctly.
