# API Sentinel

API Sentinel is a Spring Boot backend for static API security analysis.

The goal is to learn and demonstrate real backend engineering through a practical AppSec project:

- Spring Boot REST APIs
- DTOs, validation, services, repositories, and entities
- PostgreSQL persistence
- Clean milestone-based development
- Later: OpenAPI parsing and deterministic security rules

## Current Milestone

Milestone 1 implements only Project CRUD.

Implemented endpoints:

```http
POST   /api/projects
GET    /api/projects
GET    /api/projects/{id}
PUT    /api/projects/{id}
DELETE /api/projects/{id}
```

## Planned Direction

Future milestones will add:

1. ApiScan model connected to Project
2. OpenAPI JSON/YAML upload
3. OpenAPI parser
4. OWASP-inspired deterministic security rules
5. Findings and risk scoring
6. Scan history and reports
7. Optional AI explanations after the scanner works without AI

AI will not decide whether something is vulnerable. Rules create findings; AI may later explain findings in a developer-friendly way.

## Local Database

Create a PostgreSQL database named `api_sentinel`, or override the connection with environment variables.

Default configuration:

```text
DB_URL=jdbc:postgresql://localhost:5432/api_sentinel
DB_USERNAME=postgres
DB_PASSWORD=postgres
```

## Run

Maven is required to run from the terminal:

```powershell
mvn spring-boot:run
```

The API will start on:

```text
http://localhost:8080
```
