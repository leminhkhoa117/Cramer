# Cramer - English Practice Platform

## Project Overview

This project is a full-stack web application called **Cramer**, designed as an English practice platform delivering IELTS-style tests.

- **Frontend:** The frontend is a modern web application built with **React (Vite)** and styled with **Tailwind CSS**. It uses the **Supabase client** for authentication.

- **Backend:** The backend is a **Spring Boot** application written in **Java 21**. It provides a REST API for the frontend and connects to a **PostgreSQL** database. **Maven** is used for dependency management and building the project.

- **Database:** The project uses a **PostgreSQL** database managed by **Supabase**. The database schema is designed to store user profiles, test sections, questions, and user answers.

- **Authentication:** Authentication is handled by **Supabase Auth**. The frontend authenticates with Supabase, which issues a JWT. This JWT is then sent to the Spring Boot backend for authorizing API requests.

- **API Documentation:** The backend includes **Swagger/OpenAPI** for interactive API documentation, which is available at `/swagger-ui.html`.

- **Containerization:** The application is containerized using **Docker**. A `docker-compose.yml` file is provided to run both the frontend and backend services.

## Building and Running

### Environment Setup

Before running the application, you need to set up the environment variables.

**Backend (`.env` file at the root or environment variables):**
```
SPRING_DATASOURCE_URL=jdbc:postgresql://db.jpocdgkrvohmjkejclpl.supabase.co:5432/postgres?sslmode=require
SPRING_DATASource_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=<your-database-password>
SUPABASE_JWT_SECRET=<your-supabase-jwt-secret>
```

**Frontend (`frontend/.env`):**
```
VITE_SUPABASE_URL=https://jpocdgkrvohmjkejclpl.supabase.co
VITE_SUPABASE_ANON_KEY=<your-supabase-anon-key>
VITE_API_BASE_URL=http://localhost:8080/api
```

### Running the Application

**Using Docker Compose (Recommended):**
```bash
docker-compose up --build
```

**Running Frontend and Backend Separately:**

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```
The frontend will be available at `http://localhost:5173`.

**Backend:**
```bash
cd backend
./mvnw spring-boot:run
```
The backend will be available at `http://localhost:8080`.

## Development Conventions

- **Living Documentation:** The `DEVELOPMENT_NOTES.md` file is the primary source of information for developers. It contains detailed setup instructions, the current project state, known issues, and a log of recent changes. Please keep it updated.

- **Commit Messages:** Follow the conventional commit message format (e.g., `feat:`, `fix:`, `docs:`, `refactor:`).

- **Branching:** Create a feature branch for any new development (`git checkout -b feature/your-feature-name`).

- **Code Style:** Follow the existing code style in the frontend and backend.

- **CSS Styling:** For React components, prefer creating a dedicated CSS file (e.g., `MyComponent.css`) in the `src/css` directory and importing it directly into the corresponding JSX file (e.g., `MyComponent.jsx`). This approach is favored over appending styles to a global stylesheet to ensure encapsulation and prevent unforeseen conflicts.

- **API:** The backend provides a REST API. Refer to the Swagger documentation at `http://localhost:8080/swagger-ui.html` for details on the available endpoints.

## Project Status & Recent Changes

**Date:** 2025-11-08

**Status:** BLOCKED - Unresolved Authentication Error

**Summary of Changes:**

*   **Feature Implementation (Test-Taking Interface):**
    *   **Backend:** Implemented a new, efficient API endpoint (`GET /api/sections/{id}/full`) to serve all data required for a test section (passage, questions) in a single request.
    *   **Frontend:** Developed the main test-taking page (`TestPage.jsx`) featuring a two-column layout (passage on the left, questions on the right).
    *   **Dynamic Rendering:** Created a `QuestionRenderer.jsx` component capable of parsing question data and dynamically rendering various question types (e.g., fill-in-the-blank, multiple choice, T/F/NG).
    *   **UX Improvements:** Added logic to group related questions and display contextual instructions (e.g., "Complete the notes below..."), closely mimicking a real exam paper.

*   **System & Security Overhaul (Debugging Journey):**
    *   **Build Process:** Identified and fixed a critical issue where the backend was not being rebuilt, causing code changes to have no effect.
    *   **Dependency Management:** Corrected the `pom.xml` to include the necessary `spring-boot-starter-security` and `jjwt` dependencies, which were previously disabled.
    *   **Security Configuration:** Implemented a modern `SecurityConfig` for Spring Boot from scratch, establishing a full JWT authentication filter chain (`JwtAuthFilter`) to process Supabase tokens.
    *   **CORS Resolution:** Resolved persistent CORS `403 Forbidden` errors by correctly integrating the CORS configuration into the new Spring Security filter chain.
    *   **JWT Validation:** Debugged and corrected the JWT validation logic to properly handle Base64-encoded secrets.

*   **BLOCKER - `SignatureException`:**
    *   Despite a fully implemented feature and a correct security configuration, all authenticated API calls are blocked by a `403 Forbidden` error.
    *   Backend logs show a persistent `io.jsonwebtoken.security.SignatureException`, indicating the JWT signature from the browser does not match the signature computed by the backend.
    *   This points to a fundamental mismatch between the `SUPABASE_JWT_SECRET` being used and the token being issued, even after extensive verification steps (including `jwt.io` confirmation and bypassing environment variable loading). The root cause remains unknown.

**Date:** 2025-11-07

**Status:** Frontend Debugging & Refinement Complete

**Summary of Changes:**

*   **Authentication Debugging:** Investigated and identified the root cause of critical frontend authentication errors. Provided concrete steps to resolve configuration issues with Supabase (environment variables, CORS, email confirmation settings).
*   **Code Refinement:**
    *   Refactored `backendApi.js` to improve API error logging for more precise debugging.
    *   Resolved multiple React warnings in `Home.jsx` and `Login.jsx` by applying best practices for controlled components (`defaultValue`) and form attributes (`autocomplete`).
    *   Updated `App.jsx` to include React Router's `future` flags, ensuring forward compatibility with v7.
*   **Database Management:** Assisted with database maintenance by providing SQL commands to correctly identify and drop database `VIEW`s, resolving user confusion.
*   **Version Control:** Successfully committed all fixes and improvements to the `main` branch on GitHub, including updates to this documentation and removal of cached build artifacts from Git tracking.
