# Cramer - Backend

This is a starter Spring Boot (Maven) backend for the Cramer IELTS site.

Prerequisites

- Java 17+
- Maven

Run locally

1. Build:

   mvn -f pom.xml clean package

2. Run:

   mvn -f pom.xml spring-boot:run

The app will be available at http://localhost:8080. Test endpoint: http://localhost:8080/api/ping

Connecting to Supabase (Postgres)

Supabase exposes a Postgres database. Set the following environment variables before running the app to connect to Supabase:

Windows CMD example:

```
set SPRING_DATASOURCE_URL=jdbc:postgresql://db.<region>.supabase.co:5432/postgres
set SPRING_DATASOURCE_USERNAME=postgres
set SPRING_DATASOURCE_PASSWORD=your_password
```

Note: For production, prefer using connection pooling and secure secret management.
