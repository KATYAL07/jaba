package com.unityeats;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Unity Eats - Food Redistribution Platform
 *
 * Main entry point for the Spring Boot application.
 *
 * Architecture Overview:
 * ┌─────────────┐    ┌─────────────┐    ┌──────────────┐    ┌────────────┐
 * │  Controller │ -> │   Service   │ -> │  Repository  │ -> │  Database  │
 * │  (HTTP/REST)│    │(Biz Logic)  │    │  (JPA/ORM)   │    │  (H2/SQL)  │
 * └─────────────┘    └─────────────┘    └──────────────┘    └────────────┘
 *
 * Security Flow:
 * Request -> JwtAuthFilter -> SecurityContext -> Controller (if valid JWT)
 */
@SpringBootApplication
public class UnityEatsApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnityEatsApplication.class, args);
        System.out.println("""
                ╔══════════════════════════════════════════════════╗
                ║         🍃 UNITY EATS BACKEND STARTED 🍃         ║
                ║                                                  ║
                ║  API:       http://localhost:8080/api            ║
                ║  H2 Console: http://localhost:8080/h2-console    ║
                ║  Frontend:  Open index.html with Live Server     ║
                ╚══════════════════════════════════════════════════╝
                """);
    }
}
