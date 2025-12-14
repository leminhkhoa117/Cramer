# Frontend Setup & Run Instructions

## Quick Start

```powershell
cd frontend

# Install dependencies (first time only)
npm install

# Start development server
npm run dev
```

Access at: `http://localhost:5173`

## Environment Setup

### 1. Create `.env.local` file

Copy from `.env.example` and fill in values:

```properties
# Supabase Configuration
VITE_SUPABASE_URL=https://jpocdgkrvohmjkejclpl.supabase.co
VITE_SUPABASE_ANON_KEY=your-supabase-anon-key-here

# Backend API URL
VITE_API_BASE_URL=http://localhost:8080/api
```

### 2. Get Supabase Keys

1. Go to [Supabase Dashboard](https://supabase.com/dashboard)
2. Select your project
3. Settings → API
4. Copy:
   - **URL** → `VITE_SUPABASE_URL`
   - **anon public** key → `VITE_SUPABASE_ANON_KEY`

## Project Structure

```
frontend/
├── src/
│   ├── api/
│   │   ├── backendApi.js        # Backend REST API calls
│   │   ├── supabaseClient.js    # Supabase Auth setup
│   │   └── config.js            # API configuration
│   ├── components/
│   │   ├── Header.jsx           # Header component
│   │   └── Footer.jsx           # Footer component
│   ├── contexts/
│   │   └── AuthContext.jsx      # Authentication state management
│   ├── pages/
│   │   ├── Home.jsx             # Landing page
│   │   ├── Login.jsx            # Login/Signup page
│   │   └── Dashboard.jsx        # User dashboard
│   ├── App.jsx                  # Main app with routing
│   ├── main.jsx                 # Entry point
│   └── styles.css               # Tailwind CSS
├── package.json
├── vite.config.js
└── tailwind.config.js
```

## Available Scripts

### `npm run dev`
Starts development server with hot reload at `http://localhost:5173`

### `npm run build`
Builds production bundle to `dist/` folder

### `npm run preview`
Preview production build locally

## Features

### 🔐 Authentication (Supabase)
- Email/Password sign up
- Email/Password sign in
- Auto JWT token management
- Protected routes

### 📊 Dashboard
- User statistics (accuracy, total answers)
- List of available practice sections
- Quick start buttons

### 🔗 API Integration
- Automatic JWT token injection
- Error handling with retry
- Loading states

## Usage

### 1. Sign Up

1. Navigate to `/login`
2. Click "Sign up"
3. Enter:
   - Username
   - Email
   - Password
4. Check email for confirmation link
5. Click confirmation link
6. Sign in

### 2. Sign In

1. Navigate to `/login`
2. Enter email and password
3. Click "Sign in"
4. Redirected to `/dashboard`

### 3. View Dashboard

- See your statistics:
  - Total answers
  - Correct answers
  - Incorrect answers
  - Accuracy percentage
- Browse available practice sections
- Click "Start Practice" to begin

## API Calls

The frontend automatically:
- ✅ Gets JWT token from Supabase
- ✅ Includes token in all backend requests
- ✅ Handles 401 Unauthorized errors
- ✅ Refreshes token when needed

### Example API Call

```javascript
import { sectionApi } from './api/backendApi';

// Get all sections (automatically includes JWT)
const sections = await sectionApi.getAll();

// Get user statistics
const stats = await userAnswerApi.getUserStats(userId);
```

## Troubleshooting

### Issue: "Cannot connect to backend"
**Cause:** Backend not running or wrong URL  
**Fix:**
1. Check backend is running: `http://localhost:8080/actuator/health`
2. Verify `VITE_API_BASE_URL` in `.env.local`

### Issue: "401 Unauthorized"
**Cause:** Invalid or expired JWT token  
**Fix:**
1. Sign out and sign in again
2. Check backend JWT_SECRET matches Supabase
3. Clear browser localStorage

### Issue: "Module not found"
**Cause:** Dependencies not installed  
**Fix:**
```powershell
npm install
```

### Issue: "Vite error: Cannot find module"
**Cause:** Missing .env.local file  
**Fix:**
1. Copy `.env.example` to `.env.local`
2. Fill in Supabase credentials

### Issue: "CORS error"
**Cause:** Backend CORS not configured for frontend URL  
**Fix:**
1. Check backend `SecurityConfig.java`
2. Add your frontend URL to `allowedOrigins`
3. Restart backend

## Development Tips

### Hot Reload

Vite automatically reloads when you save files. No manual refresh needed!

### React DevTools

Install [React Developer Tools](https://chrome.google.com/webstore/detail/react-developer-tools) Chrome extension to debug components.

### Check Network Requests

1. Open Chrome DevTools (F12)
2. Network tab
3. Filter: XHR
4. See all API calls and responses

### Debugging Auth

```javascript
// In browser console
import { supabase } from './src/api/supabaseClient';

// Check current session
const { data } = await supabase.auth.getSession();
console.log(data.session);

// Check current user
const { data: userData } = await supabase.auth.getUser();
console.log(userData.user);
```

## Building for Production

### 1. Build

```powershell
npm run build
```

Outputs to `dist/` folder.

### 2. Preview

```powershell
npm run preview
```

Test production build locally at `http://localhost:4173`

### 3. Deploy

**Option A: Vercel**
```powershell
npm install -g vercel
vercel
```

**Option B: Netlify**
```powershell
npm install -g netlify-cli
netlify deploy --prod
```

**Environment Variables:**
Don't forget to set in deployment platform:
- `VITE_SUPABASE_URL`
- `VITE_SUPABASE_ANON_KEY`
- `VITE_API_BASE_URL` (your production backend URL)

## Next Steps

1. ✅ Frontend is running
2. ✅ Can sign in/sign up
3. ✅ Dashboard shows data
4. 🔜 Implement Practice Page
5. 🔜 Add question components
6. 🔜 Add timer and scoring
