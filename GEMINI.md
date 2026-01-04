# Cramer CMS - GEMINI.md

## System rules
Always report back your "Remaining context window" like the following in response, right at the beginning of every response:
```markdown
## Remaining context window: <exact number of unused tokens>/<context window token count> (<percentage used>)
```

## Project Overview

Cramer CMS is an IELTS test management system with:
- **Backend**: Java Spring Boot (Supabase PostgreSQL database)
- **Frontend**: React (Vite) admin panel
- **AI System**: ABTS (AI-Based Test Generation System) using OpenRouter API

## Key Directories

| Directory | Purpose |
|-----------|---------|
| `backend/src/main/java/com/cramer/` | Java source code |
| `frontend/src/admin/` | Admin React components |
| `docs/` | Technical specifications |

## ABTS (AI-Based Test Generation System)

### Core Files
- `backend/src/main/java/com/cramer/service/abts/ABTSService.java` - Main orchestration
- `backend/src/main/java/com/cramer/service/abts/PromptBuilderService.java` - AI prompts
- `backend/src/main/java/com/cramer/controller/admin/ABTSController.java` - REST API
- `frontend/src/admin/stores/useABTSStore.js` - Frontend state
- `frontend/src/admin/components/abts/` - UI components

### Skills Supported
| Skill | Parts | Questions |
|-------|-------|-----------|
| Reading | 3 | 40 (13+13+14) |
| Listening | 4 | 40 (10×4) |
| Writing | 2 | Tasks 1 & 2 |

### Database Schema
- `test_sets` → `tests` → `sections` → `questions`
- Supabase project ID: `jpocdgkrvohmjkejclpl`

### Question Number Ranges
| Skill | Part 1 | Part 2 | Part 3 | Part 4 |
|-------|--------|--------|--------|--------|
| Reading | Q1-13 | Q14-26 | Q27-40 | - |
| Listening | Q1-10 | Q11-20 | Q21-30 | Q31-40 |
| Writing | Task 1 | Task 2 | - | - |

## Running the Project

```bash
# Backend
cd backend
./run-app.sh

# Frontend
cd frontend
npm run dev
```

## Spec Documents
- `docs/CRAMER_ABTS_SPECS.md` - Full ABTS specification
- `docs/CRAMER_CMS_ADMIN_SPECS.md` - Admin panel specs
