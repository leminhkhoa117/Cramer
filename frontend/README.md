# Cramer - Frontend

This is a starter React + Vite frontend for the Cramer IELTS learning site.

## Quick Start

1. Install dependencies:

   ```bash
   npm install
   ```

2. Create a `.env` file in this folder with the following variables (from Supabase):

   ```env
   VITE_SUPABASE_URL=https://your-project.supabase.co
   VITE_SUPABASE_ANON_KEY=your-anon-key
   VITE_API_BASE_URL=http://localhost:8080  # Optional, for backend API
   ```

3. Run the dev server:

   ```bash
   npm run dev
   ```

The app will start on http://localhost:5173 by default (Vite default port).

## State Management (Zustand)

As of **2025-12-10**, the app uses [Zustand](https://github.com/pmndrs/zustand) for all global state management. React Context (`AuthContext.jsx`) is deprecated.

### Available Stores

All stores are located in `src/stores/` and exported from `src/stores/index.js`:

| Store | Purpose |
|-------|----------|
| `useAuthStore` | Auth user, session, signIn/signOut/OAuth actions |
| `useProfileStore` | User profile with auto-sync to auth changes |
| `useTestStore` | Test-taking UI state (answers, timer, modals, navigation) |
| `useTestSessionStore` | Test API operations with 5-min caching TTL |
| `useDashboardStore` | Dashboard data with pagination (sessionStorage persisted) |
| `useCourseStore` | Courses list with caching + pagination |

### Usage Patterns

**Import pattern:**
```javascript
import { useAuthStore, useProfileStore } from '../stores';
```

**Selector pattern** (recommended for performance — prevents unnecessary re-renders):
```javascript
// ✅ Good: Subscribe only to the specific value you need
const user = useAuthStore(state => state.user);
const isLoading = useAuthStore(state => state.loading);

// ❌ Avoid: Subscribing to entire store causes re-render on any change
const authStore = useAuthStore();
```

**Multiple selectors:**
```javascript
const { user, signOut } = useAuthStore(state => ({
  user: state.user,
  signOut: state.signOut
}));
```

**Actions (don't need selectors):**
```javascript
// Actions are stable references, can be called directly
const signOut = useAuthStore.getState().signOut;
```

### Benefits Over React Context

- **Data caching**: Prevents refetching data on navigation
- **Granular subscriptions**: Components only re-render when their specific data changes
- **DevTools integration**: Install [Redux DevTools](https://chrome.google.com/webstore/detail/redux-devtools/lmhkpmbekcpmknklioeibfkpmmfibljd) for state debugging
- **Simpler code**: No Provider wrappers, no prop drilling

## Project Structure

```
src/
├── api/           # API clients (backendApi.js, supabaseClient.js)
├── components/    # Reusable UI components
├── contexts/      # (Deprecated) React Contexts - kept for reference
├── css/           # Component-specific CSS files
├── hooks/         # Custom React hooks
├── pages/         # Page components (routes)
├── stores/        # Zustand stores (global state)
└── utils/         # Utility functions (sanitize.js, etc.)
```
