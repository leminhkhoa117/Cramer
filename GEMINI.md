# Gemini Project Context: Cramer

## Project Overview

Cramer is a full-stack web application designed as an English practice platform, specifically for IELTS-style tests. The application features a modern user interface for learners to take Reading and Listening tests, review their answers with detailed explanations, and track their progress through a personalized dashboard.

## Current Status (as of 2025-12-05)

- ✅ **Authentication**: JWT-based auth with Supabase tokens is fully functional.
- ✅ **Test Taking**: Users can start, save progress, resume, submit, and cancel tests (Reading/Listening).
- ✅ **Test Review**: Completed tests show correct answers with explanations.
- ✅ **Dashboard**: Shows course progress, skill summaries, and test history.
- ✅ **Profile Management**: Users can update profile info, avatar, and Gemini API key.
- ✅ **Writing Test**: Full implementation with async AI grading using Gemini 2.0 Flash.
- ✅ **Writing Review**: Comprehensive review UI with resizable panels, highlighted essay, and detailed feedback.

## Architecture

The project follows a classic client-server architecture, containerized for consistent deployment.

*   **Frontend (Client):**
    *   **Framework:** React.js (v18) with Vite for fast development.
    *   **Language:** JavaScript (JSX).
    *   **Styling:** Tailwind CSS + Bootstrap with custom CSS modules.
    *   **Routing:** `react-router-dom` for client-side routing.
    *   **Authentication:** `AuthContext` manages sessions using `supabase-js` client.
    *   **API Communication:** `axios` with automatic JWT token injection.

*   **Backend (Server):**
    *   **Framework:** Spring Boot (v3.3)
    *   **Language:** Java (v21)
    *   **Build Tool:** Apache Maven
    *   **API:** RESTful API with OpenAPI (Swagger) documentation.
    *   **Database:** Spring Data JPA with PostgreSQL (Supabase).
    *   **Authentication:** Spring Security with JWT validation.

*   **Database:**
    *   **Provider:** Supabase (PostgreSQL)
    *   **RLS:** Row Level Security enabled on `test_attempts` and `user_answers` tables.
    *   **Key Tables:** `profiles`, `sections`, `questions`, `test_attempts`, `user_answers`, `target`.

## Building and Running the Application

### Using Docker Compose (Recommended)

1.  **Prerequisites:** Docker and Docker Compose installed.

2.  **Environment Configuration:** Create a `.env` file in the project root:
    ```env
    SPRING_DATASOURCE_URL=jdbc:postgresql://<your_db_host>:<port>/<db_name>
    SPRING_DATASOURCE_USERNAME=<your_db_username>
    SPRING_DATASOURCE_PASSWORD=<your_db_password>
    SUPABASE_JWT_SECRET=<your_supabase_jwt_secret>
    ```

3.  **Run:**
    ```shell
    docker-compose up --build
    ```
    *   **Frontend:** http://localhost:5173
    *   **Backend:** http://localhost:8080
    *   **API Docs:** http://localhost:8080/swagger-ui.html

### Running Services Manually

#### Frontend
```shell
cd frontend
npm install
npm run dev
```

#### Backend
```shell
cd backend
.\mvnw.cmd clean package -DskipTests
.\run-app.ps1
```

## Key API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/profiles/{id}` | GET/PUT | User profile management |
| `/api/dashboard/summary/{userId}` | GET | Dashboard data aggregation |
| `/api/test-attempts/start` | POST | Start or resume a test attempt |
| `/api/test-attempts/{id}/progress` | POST | Save test progress |
| `/api/test-attempts/{id}/submit` | POST | Submit and score test |
| `/api/test-attempts/{id}/cancel` | POST | Cancel and delete attempt |
| `/api/tests/data` | GET | Get full test content |
| `/api/writing/attempts/start` | POST | Start a writing test attempt |
| `/api/writing/attempts/{id}/submit` | POST | Submit writing for AI grading |
| `/api/writing/attempts/{id}/status` | GET | Check async grading status |
| `/api/writing/attempts/{id}/review` | GET | Get grading results |

## Development Conventions

*   **Frontend:** Components in `pages/`, `components/`, with contexts in `contexts/` and API calls in `api/`.
*   **Backend:** Standard Spring Boot structure with `controller/`, `service/`, `repository/`, `entity/`, `dto/` packages.
*   **Repository Methods:** Use explicit `@Query` annotations with `@Modifying(clearAutomatically = true, flushAutomatically = true)` for DELETE operations.

## Writing Test Attempt Flow

The Writing test has a special attempt lifecycle to prevent ghost attempts:

1. **Start Attempt**: `POST /api/test-attempts/start?forceNew=false`
   - If latest attempt is `IN_PROGRESS` → return it (resume)
   - If latest attempt is `COMPLETED` and `forceNew=false` → return COMPLETED (frontend shows choice modal)
   - If latest attempt is `COMPLETED` and `forceNew=true` → create new attempt
   - If latest attempt is `CANCELLED` → create new attempt

2. **Choice Modal** (frontend `ResumeConfirmationModal`):
   - When backend returns COMPLETED attempt, frontend shows modal asking:
     - "Xem kết quả" → redirect to `/test/writing/review/:attemptId`
     - "Làm bài mới" → call `startAttempt` with `forceNew=true`

3. **Submit**: `POST /api/writing/attempts/{id}/submit` → marks as COMPLETED, starts async AI grading

4. **forceNew Parameter**:
   - Dashboard "Làm lại" button passes `state={{ forceNew: true }}`
   - CourseDetailPage can also pass `forceNew` for explicit new test
   - Direct URL access or browser back has `forceNew=false` → shows choice modal if COMPLETED exists

## Recent Fixes

- **2025-12-05:** Fixed ghost IN_PROGRESS attempts appearing after completing Writing test:
  - Backend now returns COMPLETED attempt when `forceNew=false` instead of auto-creating new
  - Frontend shows choice modal ("Xem kết quả" / "Làm bài mới") when COMPLETED attempt exists
  - `ResumeConfirmationModal` updated to handle both IN_PROGRESS and COMPLETED states
- **2025-12-05:** Implemented comprehensive Writing Test with AI grading — includes async grading with Gemini 2.0 Flash, resizable review panels, essay highlighting, and click-to-scroll analysis.
- **2025-12-04:** Fixed test cancellation not deleting attempts — required explicit JPQL queries and RLS policy updates.
- **2025-12-04:** Fixed duplicate API calls in React StrictMode causing orphan test attempts.
