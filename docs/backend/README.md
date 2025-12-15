# Cramer Backend Documentation

> **Comprehensive documentation for the Cramer IELTS Learning Platform backend infrastructure**

---

## 📚 Documentation Index

### Database Schema & Structure

| Document | Description |
|----------|-------------|
| [**DATABASE_SCHEMA.md**](./DATABASE_SCHEMA.md) | **Complete database schema documentation** - All 21 tables, columns, relationships, indexes, RLS policies, functions, triggers, and migrations |

### Data Ingestion Guides

| Document | Description |
|----------|-------------|
| [**DATA_INGESTION_GUIDE_READING.md**](./DATA_INGESTION_GUIDE_READING.md) | Guide for AI agents to generate SQL for IELTS Reading test ingestion |
| [**DATA_INGESTION_GUIDE_LISTENING.md**](./DATA_INGESTION_GUIDE_LISTENING.md) | Guide for AI agents to generate SQL for IELTS Listening test ingestion |

### Integration Guides

| Document | Description |
|----------|-------------|
| [**DEEPSEEK_MIGRATION_GUIDE.md**](./DEEPSEEK_MIGRATION_GUIDE.md) | Guide for migrating to DeepSeek AI provider |
| [**supabase-backend.md**](./supabase-backend.md) | Supabase backend configuration and setup |

### SQL Reference Files

Located in the `./migrations/` folder:
- Database migration history
- Schema evolution scripts

### Example Data Files

| File | Description |
|------|-------------|
| `IELTS Cambridge 17_T1_R.sql` | Cambridge 17 Test 1 Reading data |
| `IELTS Cambridge 17_T1_W.sql` | Cambridge 17 Test 1 Writing data |
| `IELTS_Cambridge_17_T1_L.sql` | Cambridge 17 Test 1 Listening data |
| *(and more...)* | Additional test data files |

---

## 🗄️ Database Overview

**Platform:** Supabase (PostgreSQL)  
**Project ID:** `jpocdgkrvohmjkejclpl`

### Quick Statistics

| Metric | Count |
|--------|-------|
| Total Tables | 21 |
| Tables with RLS | 16 |
| Applied Migrations | 14 |
| Database Functions | 5 |
| Triggers | 4 |

### Core Domain Tables

```
┌──────────────────────────────────────────────────────────────────┐
│                        CRAMER DATABASE                           │
├──────────────────────────────────────────────────────────────────┤
│  User Management        │  IELTS Content          │  Learning    │
│  ─────────────────────  │  ─────────────────────  │  ─────────── │
│  • profiles             │  • sections             │  • vocabulary│
│  • user_two_factor_auth │  • questions            │  • target    │
│  • user_streaks         │  • test_attempts        │              │
│                         │  • user_answers         │              │
│                         │  • writing_submissions  │              │
├──────────────────────────────────────────────────────────────────┤
│  Subscription & Billing              │  AI Features              │
│  ──────────────────────────────────  │  ───────────────────────  │
│  • subscription_tiers                │  • chat_messages          │
│  • user_subscriptions                │  • chatbot_usage          │
│  • user_credits                      │  • translation_usage      │
│  • credit_transactions               │                           │
│  • lua_packs                         │                           │
│  • payment_orders                    │                           │
├──────────────────────────────────────────────────────────────────┤
│  Quota Management                                                │
│  ──────────────────────────────────────────────────────────────  │
│  • user_quotas (global monthly limits)                           │
│  • skill_quotas (per-skill monthly limits)                       │
└──────────────────────────────────────────────────────────────────┘
```

---

## 🔗 Quick Links

### Connection Strings

```bash
# JDBC (Java/Spring Boot)
jdbc:postgresql://db.jpocdgkrvohmjkejclpl.supabase.co:6543/postgres?sslmode=require&prepareThreshold=0

# PostgreSQL CLI
psql -h db.jpocdgkrvohmjkejclpl.supabase.co -p 6543 -d postgres -U postgres
```

### Supabase Dashboard

- **Project URL:** https://supabase.com/dashboard/project/jpocdgkrvohmjkejclpl
- **SQL Editor:** https://supabase.com/dashboard/project/jpocdgkrvohmjkejclpl/sql
- **Table Editor:** https://supabase.com/dashboard/project/jpocdgkrvohmjkejclpl/editor

---

## 📋 Key Concepts

### Subscription Tiers

| Tier | Code | Price | Monthly Attempts | AI Gradings |
|------|------|-------|------------------|-------------|
| Free | `cramerie` | 0 VND | 20 | 3 |
| Premium | `cramerich` | 69,000 VND | 40 | 20 |

### Virtual Currency (Lúa 🌾)

Lúa is the in-app virtual currency used for:
- Purchasing extra test attempts
- AI grading overages
- Chatbot message overages
- Translation overages

**Overage Costs:**
- Test attempt: 10 Lúa
- AI grading: 20 Lúa
- Chatbot message: 2 Lúa
- Translation: 1 Lúa

### Question Types

**Reading:**
- `FILL_IN_BLANK`, `SUMMARY_COMPLETION`, `TRUE_FALSE_NOT_GIVEN`
- `YES_NO_NOT_GIVEN`, `MATCHING_INFORMATION`, `MATCHING_HEADINGS`
- `MATCHING_FEATURES`, `MATCHING_SENTENCE_ENDINGS`, `MULTIPLE_CHOICE`
- `MULTIPLE_CHOICE_MULTIPLE_ANSWERS`, `SUMMARY_COMPLETION_OPTIONS`
- `DIAGRAM_LABEL_COMPLETION`, `TABLE_COMPLETION`, `FLOW_CHART_COMPLETION`

**Listening:**
- `FILL_IN_BLANK`, `MULTIPLE_CHOICE`, `MULTIPLE_CHOICE_MULTIPLE_ANSWERS`, `MATCHING`

---

## 🔒 Security

### Row Level Security (RLS)

All user data tables have RLS enabled with policies that:
1. Allow **service_role** full access for backend operations
2. Allow users to **read** their own data
3. Allow users to **write** their own data (where appropriate)

### Authentication

- Managed by **Supabase Auth**
- JWT tokens used for API authentication
- `auth.uid()` function used in RLS policies

---

## 📝 Contributing

When adding new backend documentation:

1. Create the document in `/docs/backend/`
2. Update this README with a link and description
3. Follow the existing markdown formatting conventions
4. Include SQL examples where applicable

---

*Last updated: 2025-12-15*
