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
SPRING_DATASOURCE_USERNAME=postgres
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
