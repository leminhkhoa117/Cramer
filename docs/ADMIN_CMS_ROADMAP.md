# 🏛️ Admin CMS Roadmap

> **Status:** Planning Phase  
> **Priority:** Long-term (Phase 3+)  
> **Estimated Effort:** Very Large (8-12 weeks)  
> **Last Updated:** 2025-12-14

---

## 📋 Overview

Xây dựng hệ thống quản trị (CMS - Content Management System) cho phép Admin quản lý toàn bộ nền tảng Cramer. Hệ thống sẽ bao gồm phân quyền rõ ràng giữa User và Admin, với giao diện quản trị riêng biệt.

---

## 🎯 Goals

1. **Phân quyền User/Admin** - Hệ thống xác thực và phân quyền rõ ràng
2. **Quản lý người dùng** - CRUD users, xem thông tin, điều chỉnh subscription
3. **Quản lý Credit (Lúa)** - Cộng/trừ/điều chỉnh Lúa cho người dùng
4. **Quản lý đề thi** - Thêm/sửa/xóa exams, sections, questions
5. **Thống kê & Analytics** - Dashboard với các metrics quan trọng
6. **Audit Log** - Ghi lại mọi thao tác của Admin

---

## 🗂️ Module Breakdown

### Module 1: Authentication & Authorization (Week 1-2)

#### 1.1 Database Schema
```sql
-- Add role to profiles table
ALTER TABLE profiles ADD COLUMN role VARCHAR(20) DEFAULT 'USER' 
  CHECK (role IN ('USER', 'ADMIN', 'SUPER_ADMIN'));

-- Admin login attempts log
CREATE TABLE admin_login_attempts (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) NOT NULL,
  ip_address VARCHAR(45),
  user_agent TEXT,
  success BOOLEAN NOT NULL,
  failure_reason VARCHAR(100),
  attempted_at TIMESTAMPTZ DEFAULT NOW()
);

-- Admin audit log
CREATE TABLE admin_audit_log (
  id BIGSERIAL PRIMARY KEY,
  admin_id UUID REFERENCES profiles(id),
  action VARCHAR(100) NOT NULL,
  target_type VARCHAR(50), -- 'USER', 'EXAM', 'CREDIT', etc.
  target_id VARCHAR(100),
  old_value JSONB,
  new_value JSONB,
  ip_address VARCHAR(45),
  created_at TIMESTAMPTZ DEFAULT NOW()
);
```

#### 1.2 Backend Implementation
- **Files to create:**
  - `AdminAuthController.java` - `/admin/auth/**` endpoints
  - `AdminAuthService.java` - Admin authentication logic
  - `RoleBasedAccessFilter.java` - Filter for admin routes
  - `AuditLogService.java` - Logging all admin actions

- **Security Rules:**
  - Admin login at separate URL: `/admin/login`
  - Require 2FA for admin accounts
  - IP whitelist option for production
  - Session timeout: 30 minutes of inactivity
  - Rate limiting: 5 failed attempts = 15 min lockout

#### 1.3 Frontend Implementation
- **Files to create:**
  - `frontend/src/admin/` - Separate admin folder
  - `AdminLayout.jsx` - Admin dashboard layout
  - `AdminLogin.jsx` - Admin login page
  - `AdminRoute.jsx` - Protected route wrapper

---

### Module 2: User Management (Week 2-3)

#### 2.1 Features
| Feature | Description | Priority |
|---------|-------------|----------|
| User List | Paginated list with search/filter | P0 |
| User Details | View full user profile | P0 |
| Edit User | Update name, email, role | P0 |
| Suspend/Ban User | Temporarily or permanently ban | P1 |
| View Activity | Login history, test attempts | P1 |
| Impersonate | Login as user for debugging | P2 |

#### 2.2 API Endpoints
```
GET    /api/admin/users                  - List users (paginated)
GET    /api/admin/users/:id              - Get user details
PUT    /api/admin/users/:id              - Update user
POST   /api/admin/users/:id/suspend      - Suspend user
POST   /api/admin/users/:id/unsuspend    - Unsuspend user
DELETE /api/admin/users/:id              - Delete user (soft delete)
GET    /api/admin/users/:id/activity     - Get user activity log
POST   /api/admin/users/:id/impersonate  - Get impersonation token
```

#### 2.3 UI Components
- `UsersListPage.jsx` - DataTable with filters
- `UserDetailPage.jsx` - Full user info with tabs
- `UserEditModal.jsx` - Edit user form
- `UserActivityTab.jsx` - Activity timeline
- `BanUserModal.jsx` - Confirm ban with reason

---

### Module 3: Credit (Lúa) Management (Week 3-4)

#### 3.1 Features
| Feature | Description | Priority |
|---------|-------------|----------|
| View Balance | See any user's Lúa balance | P0 |
| Add Credits | Manually add Lúa | P0 |
| Deduct Credits | Manually deduct Lúa | P0 |
| Transaction History | View all transactions | P0 |
| Bulk Operations | Add Lúa to multiple users | P1 |
| Refund | Refund a purchase | P1 |

#### 3.2 API Endpoints
```
GET    /api/admin/credits/:userId        - Get user credit details
POST   /api/admin/credits/:userId/add    - Add credits
POST   /api/admin/credits/:userId/deduct - Deduct credits
GET    /api/admin/credits/transactions   - All transactions (paginated)
POST   /api/admin/credits/bulk-add       - Bulk add to users
POST   /api/admin/credits/:txId/refund   - Refund transaction
```

#### 3.3 UI Components
- `CreditManagementPage.jsx` - Search user + adjust credits
- `TransactionsListPage.jsx` - All transactions table
- `AdjustCreditModal.jsx` - Add/deduct with reason
- `BulkCreditModal.jsx` - CSV upload or multi-select

---

### Module 4: Exam/Content Management (Week 4-6)

#### 4.1 Features
| Feature | Description | Priority |
|---------|-------------|----------|
| Exam List | All exams with status | P0 |
| Create Exam | Add new exam source | P0 |
| Edit Exam | Modify exam metadata | P0 |
| Section Management | Add/edit/delete sections | P0 |
| Question Editor | WYSIWYG question editor | P0 |
| Answer Key | Set/edit correct answers | P0 |
| Bulk Import | Import from CSV/JSON | P1 |
| Preview Mode | Preview as student | P1 |

#### 4.2 Database Considerations
- Current schema: `sections` → `questions` with JSONB content
- Need versioning for content changes
- Consider adding:
  - `exam_metadata` table for exam-level info
  - `content_versions` for change tracking

#### 4.3 API Endpoints
```
# Exams
GET    /api/admin/exams                  - List all exams
POST   /api/admin/exams                  - Create exam
PUT    /api/admin/exams/:id              - Update exam
DELETE /api/admin/exams/:id              - Delete exam

# Sections
GET    /api/admin/exams/:id/sections     - List sections
POST   /api/admin/exams/:id/sections     - Create section
PUT    /api/admin/sections/:id           - Update section
DELETE /api/admin/sections/:id           - Delete section

# Questions
GET    /api/admin/sections/:id/questions - List questions
POST   /api/admin/sections/:id/questions - Create question
PUT    /api/admin/questions/:id          - Update question
DELETE /api/admin/questions/:id          - Delete question

# Import
POST   /api/admin/exams/import           - Import from file
```

#### 4.4 UI Components
- `ExamListPage.jsx` - Grid of exam cards
- `ExamEditPage.jsx` - Exam details + sections list
- `SectionEditor.jsx` - Section with passage/audio
- `QuestionEditor.jsx` - Rich editor for questions
- `AnswerKeyEditor.jsx` - Set correct answers
- `ImportWizard.jsx` - Multi-step import flow

---

### Module 5: Analytics Dashboard (Week 6-7)

#### 5.1 Metrics to Display
| Metric | Description | Chart Type |
|--------|-------------|------------|
| Daily Active Users | Users per day | Line |
| Test Attempts | Attempts per skill | Bar |
| Completion Rate | % tests completed | Gauge |
| Average Scores | By skill/exam | Bar |
| Revenue | Subscriptions + Lúa | Line |
| Popular Exams | Most attempted | Pie |
| AI Usage | Grading requests | Line |
| Error Rate | Failed API calls | Line |

#### 5.2 API Endpoints
```
GET /api/admin/analytics/overview       - Key metrics summary
GET /api/admin/analytics/users          - User growth data
GET /api/admin/analytics/tests          - Test attempt data
GET /api/admin/analytics/revenue        - Revenue data
GET /api/admin/analytics/ai-usage       - AI/LLM usage data
```

#### 5.3 UI Components
- `AnalyticsDashboard.jsx` - Main dashboard
- `MetricCard.jsx` - Single metric with sparkline
- `ChartContainer.jsx` - Recharts wrapper
- `DateRangePicker.jsx` - Filter by date range
- `ExportButton.jsx` - Export to CSV/PDF

---

### Module 6: Subscription Management (Week 7-8)

#### 6.1 Features
- View all subscriptions
- Manually upgrade/downgrade users
- Extend subscription period
- Cancel subscriptions
- View payment history

#### 6.2 API Endpoints
```
GET    /api/admin/subscriptions           - List all subscriptions
GET    /api/admin/subscriptions/:userId   - User subscription details
PUT    /api/admin/subscriptions/:userId   - Update subscription
POST   /api/admin/subscriptions/:userId/extend - Extend period
DELETE /api/admin/subscriptions/:userId   - Cancel subscription
```

---

### Module 7: Flexible Vocabulary System (Week 8-9)

> **Goal:** Allow users to save vocabulary from any page in the app

#### 7.1 Implementation Strategy

**Option A: Global Word Popup Component**
- Create `<WordPopup />` component that renders globally
- On text selection (double-click or highlight), show popup
- Popup shows: word, definition, "Save to notebook" button
- Works on any page with reading content

**Option B: Context Menu Integration**
- Right-click on selected text → "Look up word"
- Opens modal with translation + save option

#### 7.2 Pages to Enable
| Page | Content Source | Implementation |
|------|----------------|----------------|
| TestPage | Passage text | Wrap in `<SelectableText>` |
| WritingResultPage | Sample essays | Wrap in `<SelectableText>` |
| TestReviewPage | Answers/explanations | Wrap in `<SelectableText>` |
| CourseDetailPage | Test descriptions | Wrap in `<SelectableText>` |

#### 7.3 Components to Create
```jsx
// Global context for word selection
<VocabularyLookupProvider>
  <App />
</VocabularyLookupProvider>

// Wrapper component for selectable text
<SelectableText onWordSelect={handleWordSelect}>
  {passageContent}
</SelectableText>

// Popup shown when word is selected
<WordLookupPopup 
  word={selectedWord}
  context={surroundingText}
  onSave={handleSaveToNotebook}
  onClose={handleClose}
/>
```

#### 7.4 UX Flow
1. User double-clicks or selects a word
2. Popup appears near selection
3. Shows: word, pronunciation (loading), definition (loading)
4. "Dịch" button fetches AI translation
5. "Lưu vào sổ tay" saves to vocabulary notebook
6. Toast confirmation: "Đã lưu 'word' vào sổ tay"

---

## 🗓️ Implementation Timeline

| Phase | Weeks | Modules | Dependencies |
|-------|-------|---------|--------------|
| Phase 1 | 1-2 | Auth & Authorization | None |
| Phase 2 | 2-4 | User + Credit Management | Phase 1 |
| Phase 3 | 4-6 | Exam/Content Management | Phase 1 |
| Phase 4 | 6-7 | Analytics Dashboard | Phase 2, 3 |
| Phase 5 | 7-8 | Subscription Management | Phase 2 |
| Phase 6 | 8-9 | Flexible Vocabulary | None (independent) |

---

## 🔧 Technical Considerations

### Frontend Architecture
```
frontend/
├── src/
│   ├── admin/                    # Admin-specific code
│   │   ├── components/           # Admin UI components
│   │   ├── pages/                # Admin pages
│   │   ├── stores/               # Admin Zustand stores
│   │   ├── api/                  # Admin API client
│   │   └── AdminApp.jsx          # Admin app entry
│   └── ...                       # Existing user code
```

### Backend Architecture
```
backend/src/main/java/com/cramer/
├── admin/                        # Admin-specific code
│   ├── controller/               # Admin controllers
│   ├── service/                  # Admin services
│   ├── dto/                      # Admin DTOs
│   └── security/                 # Admin security
└── ...                           # Existing code
```

### Security Checklist
- [ ] Separate JWT claims for admin (`role: ADMIN`)
- [ ] Rate limiting on admin endpoints
- [ ] All actions logged to audit_log
- [ ] 2FA required for admin accounts
- [ ] IP whitelist for production
- [ ] HTTPS only
- [ ] CSRF protection
- [ ] Input validation on all forms

### Performance Considerations
- Paginate all list endpoints (default 20 per page)
- Cache analytics data (5 min TTL)
- Lazy load heavy components
- Use virtual scrolling for large tables
- Index frequently queried columns

---

## 📚 References

- [Supabase Auth with Row Level Security](https://supabase.com/docs/guides/auth)
- [React Admin Framework](https://marmelab.com/react-admin/) (potential library)
- [Recharts Documentation](https://recharts.org/)
- [RBAC Best Practices](https://auth0.com/docs/manage-users/access-control/rbac)

---

## ✅ Pre-Implementation Checklist

Before starting implementation:

- [ ] Finalize admin role requirements with stakeholders
- [ ] Design database migration strategy
- [ ] Create detailed wireframes for each page
- [ ] Define API contract (OpenAPI spec)
- [ ] Set up admin-specific testing environment
- [ ] Plan gradual rollout strategy
- [ ] Document admin user manual

---

> **Note:** This roadmap is a living document. Update as requirements evolve.
