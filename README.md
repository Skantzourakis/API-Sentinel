# API Sentinel

API Sentinel is a Spring Boot backend for static API security analysis.

The goal is to learn and demonstrate real backend engineering through a practical AppSec project:

- Spring Boot REST APIs
- DTOs, validation, services, repositories, and entities
- PostgreSQL persistence
- Clean milestone-based development
- Later: OpenAPI parsing and deterministic security rules

## Current Milestone

Milestone 1 implements Project CRUD.

Milestone 2 adds the initial ApiScan model connected to Project. This does not
parse OpenAPI files yet. It only stores scan records so the database structure
is ready for the next milestone.

Milestone 3 adds OpenAPI file upload validation for `.json`, `.yaml`, and `.yml`
files.

Milestone 4 parses uploaded OpenAPI files and stores how many API operations
were found in the scan record.

Milestone 5 creates initial security findings from deterministic rules and
stores them per scan.

Milestone 6 fixes project deletion so scans and findings are removed with the
project, respects operation-level `security: []` overrides when parsing, and
records scans of invalid OpenAPI files as `FAILED`.

Milestone 7 manages the database schema with Flyway migrations
(`src/main/resources/db/migration`). Hibernate only validates the schema.

Milestone 8 extends the rule set to eight OWASP-inspired security rules.

## Security Rules

| Rule | Severity | Checks | OWASP API Top 10 |
|------|----------|--------|------------------|
| `ADMIN_WITHOUT_AUTH` | CRITICAL | Admin path without declared security | API2 Broken Authentication |
| `UNAUTHENTICATED_WRITE` | HIGH | POST/PUT/PATCH/DELETE without declared security | API2 Broken Authentication |
| `POTENTIAL_BOLA` | HIGH | Object identifier in path without declared security | API1 Broken Object Level Authorization |
| `SENSITIVE_DATA_IN_QUERY` | HIGH | Passwords, tokens or keys in query parameters | API3 Broken Object Property Level Authorization |
| `INSECURE_SERVER_URL` | HIGH | Server declared over plain HTTP | API8 Security Misconfiguration |
| `MISSING_PAGINATION` | MEDIUM | List endpoint without limit/page/cursor parameters | API4 Unrestricted Resource Consumption |
| `MISSING_REQUEST_BODY_SCHEMA` | MEDIUM | Write endpoint without request body schema | API8 Security Misconfiguration |
| `MISSING_ERROR_RESPONSES` | LOW | Operation documents no 4xx or default response | API8 Security Misconfiguration |

`sample-openapi.yaml` triggers every rule and also contains one endpoint
(`GET /transactions`) that passes all of them.

Implemented endpoints:

```http
POST   /api/projects
GET    /api/projects
GET    /api/projects/{id}
PUT    /api/projects/{id}
DELETE /api/projects/{id}

POST   /api/projects/{projectId}/scans/upload
GET    /api/projects/{projectId}/scans
GET    /api/scans/{scanId}
GET    /api/scans/{scanId}/findings
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
