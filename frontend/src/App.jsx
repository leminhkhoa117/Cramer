import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { useAuthStore, useProfileStore } from './stores';
import { AnimatePresence } from 'framer-motion';

import Header from './components/Header';
import Footer from './components/Footer';
import PageWrapper from './components/PageWrapper';

import Home from './pages/Home';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import About from './pages/About';
import TestPage from './pages/TestPage';
import WritingTestPage from './pages/WritingTestPage';
import Courses from './pages/Courses';
import CourseDetailPage from './pages/CourseDetailPage';
import TestLayout from './components/TestLayout';
import TestReviewPage from './pages/TestReviewPage';
import WritingResultPage from './pages/WritingResultPage';
import Profile from './pages/Profile';

// This component waits for the initial auth loading to complete
function AuthInitializer({ children }) {
  const { loading, error, initializeAuth } = useAuthStore();

  // Initialize auth on mount
  useEffect(() => {
    initializeAuth();
  }, [initializeAuth]);

  // Initialize profile store subscription by importing it
  useEffect(() => {
    // useProfileStore subscription is set up on import
    // This ensures the module is loaded and subscription is active
    void useProfileStore.getState();
  }, []);

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
  const user = useAuthStore((state) => state.user);
  return user ? children : <Navigate to="/login" />;
}

// This component contains the actual app layout and routes
function AppContent() {
  const location = useLocation();
  // Hide header/footer on test-taking pages and review pages (they have their own header)
  const isTestPage = /^\/test\/\w+\/\d+\/\w+$/.test(location.pathname) ||
                     /^\/test\/writing\/(?!review)\w+\/\d+$/.test(location.pathname);
  // Review pages have their own internal header, so hide the main header
  const isReviewPage = /^\/test\/writing\/review\/\d+$/.test(location.pathname) ||
                       /^\/test\/review\/\d+$/.test(location.pathname);

  const showHeader = !isTestPage && !isReviewPage;

  return (
    <>
      {showHeader && <Header />}
      <main className={showHeader ? 'with-fixed-header' : ''}>
        <AnimatePresence mode="wait">
          <Routes location={location} key={location.pathname}>
            <Route path="/" element={<PageWrapper><Home /></PageWrapper>} />
            <Route path="/login" element={<Login />} />
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
              path="/test/writing/:source/:testNum"
              element={
                <ProtectedRoute>
                  <WritingTestPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/test/writing/review/:attemptId"
              element={
                <ProtectedRoute>
                  <PageWrapper>
                    <WritingResultPage />
                  </PageWrapper>
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
            <Route
              path="/profile"
              element={
                <ProtectedRoute>
                  <PageWrapper><Profile /></PageWrapper>
                </ProtectedRoute>
              }
            />
          </Routes>
        </AnimatePresence>
      </main>
      {!isTestPage && !isReviewPage && <Footer />}
    </>
  );
}

export default function App() {
  return (
    <Router>
      <AuthInitializer>
        <AppContent />
      </AuthInitializer>
    </Router>
  );
}
