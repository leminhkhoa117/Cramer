# Cramer - Backend

Spring Boot (Maven) backend for the Cramer IELTS platform — vertical-slice modules under `com.cramer.*`. See `docs/specs/backend/` for the architecture (SPEC-00…25) and `BUILD_INSTRUCTIONS.md` for detailed build help.

Prerequisites

- Java 25
- Maven (or use the bundled `./mvnw` wrapper)

Run locally

1. Build:

   mvn -f pom.xml clean package

2. Run:

   mvn -f pom.xml spring-boot:run

The app will be available at http://localhost:8080. Health check: http://localhost:8080/api/health · Swagger UI: http://localhost:8080/swagger-ui.html

Connecting to Supabase (Postgres)

Supabase exposes a Postgres database. Set the following environment variables before running the app to connect to Supabase:

Windows CMD example:

```
set SPRING_DATASOURCE_URL=jdbc:postgresql://db.<region>.supabase.co:5432/postgres
set SPRING_DATASOURCE_USERNAME=postgres
set SPRING_DATASOURCE_PASSWORD=your_password
```

Note: For production, prefer using connection pooling and secure secret management.
