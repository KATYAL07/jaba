# Unity Eats 🍃

> A food redistribution platform connecting restaurants with surplus food to NGOs, volunteers, and communities.

## Tech Stack
- **Frontend**: Vanilla HTML5 / CSS3 / ES6+ JavaScript
- **Backend**: Java 17 + Spring Boot 3.2
- **Security**: Spring Security + JWT (jjwt)
- **Database**: H2 In-Memory + Spring Data JPA
- **Validation**: `jakarta.validation.constraints`

## Quick Start

### 1. Run the Backend
```bash
cd unity-eats-backend
mvn spring-boot:run
```
Starts at **http://localhost:8080**  
H2 Console: **http://localhost:8080/h2-console** (JDBC URL: `jdbc:h2:mem:unityeatsdb`)

### 2. Open the Frontend
Open `unity-eats-frontend/index.html` with **VS Code Live Server** (port 5500) or any static server.

## Demo Credentials
All passwords: `Demo@1234`

| Role        | Email                   |
|-------------|-------------------------|
| Restaurant  | restaurant@demo.com     |
| NGO         | ngo@demo.com            |
| Volunteer   | volunteer@demo.com      |
| Beneficiary | beneficiary@demo.com    |

## Architecture

```
Controller → Service → Repository → H2 Database
     ↑
JwtAuthFilter (validates Bearer token on every request)
     ↑
SecurityConfig (stateless sessions, CORS, @PreAuthorize)
```

## Donation Lifecycle
```
AVAILABLE → (NGO accepts) → ACCEPTED → (Volunteer assigns) → ASSIGNED
         → (Volunteer picks up) → PICKED_UP → (Delivered) → DELIVERED
```

## Key Validation Rules
- Food names / categories: letters and spaces only (`^[A-Za-z ]{2,100}$`)
- Quantities: positive integers 1–10,000
- Passwords: min 8 chars, uppercase + lowercase + digit + special char
- Emails: RFC 5322 format via `@Email`
- Phone: 10–15 digits, optional `+` prefix

## API Endpoints

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | Public | Register |
| POST | `/api/auth/login` | Public | Login → JWT |
| POST | `/api/food` | RESTAURANT | Post listing |
| GET | `/api/food/my-listings` | RESTAURANT | My listings |
| GET | `/api/food/available` | NGO | Browse available |
| PATCH | `/api/food/{id}/accept` | NGO | Accept listing |
| GET | `/api/food/active-deliveries` | VOLUNTEER | Active deliveries |
| PATCH | `/api/food/{id}/assign` | VOLUNTEER | Assign self |
| PATCH | `/api/food/{id}/status` | VOLUNTEER | Update status |
| GET | `/api/public/listings` | Public | Public listings |
| GET | `/api/public/stats` | Public | Platform stats |
