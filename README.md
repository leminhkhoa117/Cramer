# Cramer

## Overview

Cramer is an English practice platform that delivers curated IELTS-style tests in a modern web experience. The project focuses on letting learners sign up, log in, and work through materials the team extracts from trusted sources. The development begins with IELTS Cambridge 17. The immediate goal of this codebase is to shape a dependable backend foundation before building rich application features.

## Current Status

- Supabase project provisioned with managed PostgreSQL, authentication, and storage.
- Initial `public` schema designed around profiles, sections, questions, and user answers.
- Documentation for the backend stack lives in `docs/backend/supabase-backend.md`.
- Frontend work is forthcoming once the data layer is stable and verified.

## Key Features (planned)

- Secure email-based authentication powered by Supabase Auth.
- Structured repository of IELTS practice content, starting with Cambridge 17 Test 1.
- Question and answer tracking so learners can monitor progress over time.
- Scalable architecture ready to ingest additional materials and skills.
- Forthcoming expansion into automated scoring experiences powered by LLMs.

## Architecture Snapshot

- **Backend:** Supabase (PostgreSQL, Auth, Functions).
- **Database Schema:**
	- `profiles` links Supabase auth users to public profile data.
	- `sections` organizes materials by exam source, test, skill, and part.
	- `questions` stores question metadata and payloads using `jsonb` for flexibility.
	- `user_answers` records every attempt for analytics and review.
- **Docs:** See `docs/backend/supabase-backend.md` for detailed schema notes and operational guidance.

## Getting Started

1. Clone the repository and ensure you have access to the Supabase project credentials.
2. Review `docs/backend/supabase-backend.md` to align your local environment or migrations with the managed schema.
3. Set the required Supabase environment variables (`SUPABASE_URL`, `SUPABASE_ANON_KEY`, etc.) when beginning frontend or integration work.
4. Follow future instructions (to be documented) for running the application locally once the client is scaffolded.

### Browser Compatibility

**Recommended browsers:**
- ✅ Google Chrome
- ✅ Mozilla Firefox  
- ✅ Zen Browser
- ⚠️ Microsoft Edge (requires cookie settings adjustment)



## Roadmap

- Finalize database constraints, indexes, and Row Level Security policies.
- Build the first version of the reading practice frontend experience.
- Add analytics dashboards for learner performance.
- Expand content coverage to additional IELTS materials and other English skills.
- Introduce new exams (IELTS Academic/General, TOEIC, and more) plus skills such as Writing and Speaking, leveraging LLM-based evaluation for high-accuracy automated marking.

## Contributing

Contributions are welcome as the project matures. Open an issue or submit a pull request with a clear description of the problem or enhancement. Please coordinate schema changes with the team to keep the Supabase environment in sync.
