# Gemini Project Context: Cramer

## Project Overview

Cramer is a full-stack web application designed as an English practice platform, specifically for IELTS-style tests. The application features a modern user interface for learners to take tests, review their answers, and track their progress.

## Architecture

The project follows a classic client-server architecture, containerized for consistent deployment and development.

*   **Frontend (Client):**
    *   **Framework:** React.js (v18) with Vite for a fast development experience.
    *   **Language:** JavaScript (JSX).
    *   **Styling:** A combination of Tailwind CSS and Bootstrap, with custom CSS modules.
    *   **Routing:** `react-router-dom` is used for all client-side routing.
    *   **Authentication:** A custom `AuthContext` manages user sessions. It appears to use the `supabase-js` client, likely for handling user authentication (sign-up, login) against a Supabase-managed identity service.
    *   **API Communication:** `axios` is used to communicate with the backend API for business logic and data fetching.

*   **Backend (Server):**
    *   **Framework:** Spring Boot (v3.3)
    *   **Language:** Java (v21)
    *   **Build Tool:** Apache Maven
    *   **API:** Exposes a RESTful API with OpenAPI (Swagger) documentation available.
    *   **Database:** Uses Spring Data JPA to interact with a PostgreSQL database.
    *   **Authentication:** Secured with Spring Security, using JSON Web Tokens (JWTs) for authorizing API requests.

*   **Orchestration:**
    *   **Docker:** The entire application is containerized using Docker. A `docker-compose.yml` file is provided to orchestrate the `frontend` and `backend` services.

## Building and Running the Application

The recommended method for running the application locally is using Docker Compose, as it handles the setup for both services.

### Using Docker Compose (Recommended)

1.  **Prerequisites:**
    *   Docker and Docker Compose installed.
    *   Create a `.env` file in the project root directory.

2.  **Environment Configuration:**
    *   Populate the `.env` file with the necessary environment variables for the backend to connect to the database and Supabase. Refer to `docker-compose.yml` for the required variables:
        ```env
        # .env
        SPRING_DATASOURCE_URL=jdbc:postgresql://<your_db_host>:<port>/<db_name>
        SPRING_DATASOURCE_USERNAME=<your_db_username>
        SPRING_DATASOURCE_PASSWORD=<your_db_password>
        SUPABASE_URL=<your_supabase_project_url>
        SUPABASE_SERVICE_ROLE_KEY=<your_supabase_service_key>
        ```

3.  **Run:**
    *   Execute the following command from the project root:
        ```shell
        docker-compose up --build
        ```
    *   **Frontend** will be available at `http://localhost:5173`.
    *   **Backend** will be available at `http://localhost:8080`.
    *   **API Docs** (Swagger UI) will be at `http://localhost:8080/swagger-ui.html`.

### Running Services Manually

#### Frontend

```shell
# Navigate to the frontend directory
cd frontend

# Install dependencies
npm install

# Start the development server
npm run dev
```

#### Backend

```shell
# Navigate to the backend directory
cd backend

# Set environment variables for the database connection (in your shell)
export SPRING_DATASOURCE_URL=...
export SPRING_DATASOURCE_USERNAME=...
export SPRING_DATASOURCE_PASSWORD=...

# Run the Spring Boot application using the Maven wrapper
./mvnw spring-boot:run
```

## Development Conventions

*   **Frontend:** The code is structured into `pages`, `components`, `contexts`, `hooks`, and `api` directories, promoting a clean separation of concerns. Components have their own CSS files for styling.
*   **Backend:** Follows standard Spring Boot project structure with `controller`, `service`, `repository`, `entity`, and `dto` packages.
*   **API:** The backend exposes a REST API. Use the Swagger UI for exploration and testing.
*   **Note:** The root `README.md` is partially outdated. It describes a Supabase-centric architecture, but the project has evolved to include a dedicated Java backend for core application logic. The frontend still leverages Supabase, likely for its authentication and possibly storage services.
