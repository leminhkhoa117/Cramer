# Cramer CMS - GEMINI.md

## System rules
- Always report back your "Remaining context window" like the following in response, right at the beginning of every response:
```markdown
## Remaining context window: <exact number of unused tokens>/<context window token count> (<percentage used>)
```
- Never uses emojis in codes.
- Never put your thoughts as comments in codes.
- NEVER START MAKING CHANGES UNLESS APPROVED.

## Anti-Hallucination Protocol
1. **Verify, Don't Assume**: Never predict file paths. Always use `list_dir` or `find_by_name` to confirm existence before reading/editing.
2. **Evidence-Based Claims**: Never explain code behavior without reading the actual file first (`view_file` or `grep_search`).
3. **No Invented Paths**: If a file is missing, state it clearly. Do not hallucinate replacement paths.
4. **Strict Truth**: If a claim cannot be backed by a tool output in the current context, do not make it.

## Project Overview

Cramer CMS is an IELTS test management system with:
- **Backend**: Java Spring Boot 3.3 (Supabase PostgreSQL database)
- **Frontend**: React 18 (Vite), Zustand for state management
- **AI System**: Hybrid approach
    - **Admin (ABTS)**: OpenRouter API (Access to 400+ models) for test generation
    - **User Features**: DeepSeek V3.2 (Chat, Vocabulary, Writing Grading)

## Key Directories

| Directory | Purpose |
|-----------|---------|
| `backend/src/main/java/com/cramer/` | Java source code |
| `frontend/src/admin/` | Admin React components |
| `docs/backend/` | Backend documentation & schemas |
| `docs/library/` | Technical library & specifications |

## ABTS (AI-Based Test Generation System)

### Core Files
- `backend/src/main/java/com/cramer/service/abts/ABTSService.java` - Main orchestration
- `backend/src/main/java/com/cramer/service/abts/OpenRouterClient.java` - AI Client
- `backend/src/main/java/com/cramer/controller/admin/ABTSController.java` - REST API
- `frontend/src/admin/stores/useABTSStore.js` - Frontend state

### AI Architecture
- **Test Generation**: Uses OpenRouter API to access various models.
- **Grading & Chat**: Uses DeepSeek API directly.

### Database Schema
- **Tables**: `test_sets` → `tests` → `sections` → `questions`
- **User Data**: `profiles`, `test_attempts`, `user_answers`, `subscription_tiers`
- **Supabase Project**: `jpocdgkrvohmjkejclpl`

### Question Number Ranges
| Skill | Part 1 | Part 2 | Part 3 | Part 4 |
|-------|--------|--------|--------|--------|
| Reading | Q1-13 | Q14-26 | Q27-40 | - |
| Listening | Q1-10 | Q11-20 | Q21-30 | Q31-40 |
| Writing | Task 1 | Task 2 | - | - |

## Running the Project

### Backend
```bash
cd backend
./run-app.sh      # Linux/macOS
./run-app.ps1     # Windows
```

### Frontend
```bash
cd frontend
npm run dev
```

### API Documentation
- **Swagger UI**: `http://localhost:8080/swagger-ui.html` (Requires JWT)

## Environment Variables
- **Root `.env`**: Spring Boot DB & API keys (Supabase, PayOS, OpenRouter, DeepSeek)
- **Frontend `.env`**: `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`

## Key Documentation
- `docs/backend/DATABASE_SCHEMA.md` - Database schema verification
- `docs/library/README.md` - Technical library index
