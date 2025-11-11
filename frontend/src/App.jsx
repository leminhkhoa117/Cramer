import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { AnimatePresence } from 'framer-motion';

import Header from './components/Header';
import Footer from './components/Footer';
import PageWrapper from './components/PageWrapper';

import Home from './pages/Home';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import About from './pages/About';
import TestPage from './pages/TestPage';
import Courses from './pages/Courses';
import CourseDetailPage from './pages/CourseDetailPage';
import TestLayout from './components/TestLayout';
import TestReviewPage from './pages/TestReviewPage';

// This component waits for the initial auth loading to complete
function AuthInitializer({ children }) {
  const { loading, error } = useAuth();

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <div className="text-xl font-semibold text-gray-700">Initializing Application...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-100">
        <div className="max-w-md p-8 bg-white rounded-lg shadow-lg">
          <div className="text-center">
            <div className="text-6xl mb-4">🔒</div>
            <h2 className="text-2xl font-bold text-red-600 mb-4">Lỗi Xác Thực</h2>
            <p className="text-gray-700 mb-4">{error}</p>
            <div className="text-left bg-gray-50 p-4 rounded mb-4">
              <p className="font-semibold mb-2">💡 Cách khắc phục:</p>
              <ul className="list-disc list-inside space-y-1 text-sm text-gray-600">
                <li>Bật cookies trong cài đặt trình duyệt</li>
                <li>Cho phép lưu trữ dữ liệu cho trang này</li>
                <li>Hoặc thử trình duyệt khác (Chrome, Firefox, Zen)</li>
              </ul>
            </div>
            <button 
              onClick={() => window.location.reload()} 
              className="px-6 py-2 bg-purple-600 text-white rounded hover:bg-purple-700 transition"
            >
              Thử Lại
            </button>
          </div>
        </div>
      </div>
    );
  }

  return children;
}

// Protected Route component remains the same, but now it runs *after* initial loading
function ProtectedRoute({ children }) {
  const { user } = useAuth();
  return user ? children : <Navigate to="/login" />;
}

// This component contains the actual app layout and routes
function AppContent() {
  const location = useLocation();
  // Only hide header/footer on the actual test-taking page, not on the review page.
  const isTestPage = /^\/test\/\w+\/\d+\/\w+$/.test(location.pathname);

  return (
    <>
      {!isTestPage && <Header />}
      <main>
        <AnimatePresence mode="wait">
          <Routes location={location} key={location.pathname}>
            <Route path="/" element={<PageWrapper><Home /></PageWrapper>} />
            <Route path="/login" element={<PageWrapper><Login /></PageWrapper>} />
            <Route path="/about" element={<PageWrapper><About /></PageWrapper>} />
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <PageWrapper><Dashboard /></PageWrapper>
                </ProtectedRoute>
              }
            />
            <Route
              path="/test/:source/:testNum/:skill"
              element={
                <ProtectedRoute>
                  <TestLayout>
                    <TestPage />
                  </TestLayout>
                </ProtectedRoute>
              }
            />
            <Route
              path="/courses"
              element={
                <ProtectedRoute>
                  <PageWrapper><Courses /></PageWrapper>
                </ProtectedRoute>
              }
            />
            <Route
              path="/courses/:courseName"
              element={
                <ProtectedRoute>
                  <PageWrapper><CourseDetailPage /></PageWrapper>
                </ProtectedRoute>
              }
            />
            <Route
              path="/test/review/:attemptId"
              element={
                <ProtectedRoute>
                  <PageWrapper>
                    <TestReviewPage />
                  </PageWrapper>
                </ProtectedRoute>
              }
            />
          </Routes>
        </AnimatePresence>
      </main>
      {!isTestPage && <Footer />}
    </>
  );
}

export default function App() {
  return (
    <Router>
      <AuthProvider>
        <AuthInitializer>
          <AppContent />
        </AuthInitializer>
      </AuthProvider>
    </Router>
  );
}
