import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { useAuthStore, useProfileStore } from './stores';
import { AnimatePresence } from 'framer-motion';

import Header from './components/Header';
import Footer from './components/Footer';
import PageWrapper from './components/PageWrapper';
import FloatingAssistant from './components/FloatingAssistant';

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
import VocabularyPage from './pages/VocabularyPage';
import PricingPage from './pages/PricingPage';
import SubscriptionPage from './pages/SubscriptionPage';
import PaymentSuccessPage from './pages/PaymentSuccessPage';
import PaymentCancelPage from './pages/PaymentCancelPage';

// Admin imports
import { AdminLayout } from './admin/components/layout';
import AdminRouteGuard from './admin/components/AdminRouteGuard';
import AdminDashboard from './admin/pages/AdminDashboard';
import UserListPage from './admin/pages/users/UserListPage';
import UserDetailPage from './admin/pages/users/UserDetailPage';
import FinanceDashboard from './admin/pages/finance/FinanceDashboard';
import TransactionHistoryPage from './admin/pages/finance/TransactionHistoryPage';
import ReportsPage from './admin/pages/finance/ReportsPage';
import ContentListPage from './admin/pages/content/ContentListPage';
import TestEditorSelectPage from './admin/pages/content/TestEditorSelectPage';
import TestEditorPage from './admin/pages/content/TestEditorPage';
import AIGenerationPage from './admin/pages/content/AIGenerationPage';
import HashtagManagementPage from './admin/pages/content/HashtagManagementPage';
import SetListPage from './admin/pages/content/SetListPage';
import SetDetailPage from './admin/pages/content/SetDetailPage';

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
  // Admin pages have their own layout with AdminHeader/AdminSidebar
  const isAdminPage = location.pathname.startsWith('/admin');

  const showHeader = !isTestPage && !isReviewPage && !isAdminPage;

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
            <Route
              path="/vocabulary"
              element={
                <ProtectedRoute>
                  <PageWrapper><VocabularyPage /></PageWrapper>
                </ProtectedRoute>
              }
            />
            <Route
              path="/pricing"
              element={<PageWrapper><PricingPage /></PageWrapper>}
            />
            <Route
              path="/subscription"
              element={
                <ProtectedRoute>
                  <PageWrapper><SubscriptionPage /></PageWrapper>
                </ProtectedRoute>
              }
            />
            <Route
              path="/payment/success"
              element={
                <ProtectedRoute>
                  <PaymentSuccessPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/payment/cancel"
              element={<PaymentCancelPage />}
            />

            {/* Admin Routes - separate layout, no Header/Footer */}
            <Route
              path="/admin"
              element={
                <AdminRouteGuard>
                  <AdminLayout />
                </AdminRouteGuard>
              }
            >
              <Route index element={<AdminDashboard />} />
              <Route path="dashboard" element={<AdminDashboard />} />
              <Route path="users" element={<UserListPage />} />
              <Route path="users/:userId" element={<UserDetailPage />} />
              <Route path="finance" element={<FinanceDashboard />} />
              <Route path="finance/transactions" element={<TransactionHistoryPage />} />
              <Route path="finance/reports" element={<ReportsPage />} />
              <Route path="content" element={<ContentListPage />} />
              <Route path="content/hub" element={<Navigate to="/admin/content/sets" replace />} />
              <Route path="content/hashtags" element={<HashtagManagementPage />} />
              <Route path="content/generate" element={<AIGenerationPage />} />
              {/* Editor Routes */}
              <Route path="content/editor" element={<Navigate to="/admin/content/sets" replace />} />
              <Route path="content/sets" element={<SetListPage />} />
              <Route path="content/sets/:setId" element={<SetDetailPage />} />
              <Route path="content/editor/:testId" element={<TestEditorPage />} />
              <Route path="content/tests/:testId" element={<TestEditorPage />} />
              {/* Legacy Editor Route Support */}
              <Route path="content/editor/:examSource/:testNumber" element={<TestEditorPage />} />
            </Route>
          </Routes>
        </AnimatePresence>
      </main>
      {!isTestPage && !isReviewPage && !isAdminPage && <Footer />}
      {/* Floating Assistant Widget - visible on protected pages except test-taking and admin */}
      {!isTestPage && !isAdminPage && <FloatingAssistant />}
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
