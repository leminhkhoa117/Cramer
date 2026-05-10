import { createBrowserRouter, Navigate, Link, useLocation, useNavigation, useOutlet } from 'react-router-dom';
import { cloneElement, useRef } from 'react';
import { AnimatePresence } from 'framer-motion';

import Home from './pages/Home';
import Login from './pages/Login';
import Header from './components/Header';
import Footer from './components/Footer';
import FloatingAssistant from './components/FloatingAssistant';
import PageWrapper from './components/PageWrapper';
import ProtectedRoute from './components/ProtectedRoute';
import SmallViewportWarning from './components/SmallViewportWarning';

function lazyRoute(importFn) {
  return {
    async lazy() {
      const mod = await importFn();
      return { Component: mod.default };
    },
  };
}

function lazyPage(importFn) {
  return {
    async lazy() {
      const mod = await importFn();
      const Page = mod.default;
      return {
        Component: () => <PageWrapper><Page /></PageWrapper>
      };
    },
  };
}

function lazyTestPage() {
  return {
    async lazy() {
      const { default: TestPage } = await import('./pages/TestPage');
      return { Component: TestPage };
    },
  };
}

function AnimatedOutlet() {
  const outlet = useOutlet();
  const keyRef = useRef(0);
  const prevOutletRef = useRef(outlet);

  if (outlet !== prevOutletRef.current) {
    keyRef.current += 1;
    prevOutletRef.current = outlet;
  }

  if (!outlet) return null;

  return (
    <AnimatePresence mode="wait">
      {cloneElement(outlet, { key: String(keyRef.current) })}
    </AnimatePresence>
  );
}

function RouteError() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="text-center p-8">
        <h1 className="text-4xl font-bold text-gray-800 mb-4">Lỗi tải trang</h1>
        <p className="text-gray-600 mb-4">Không thể tải trang này. Vui lòng thử lại.</p>
        <button
          onClick={() => window.location.reload()}
          className="px-6 py-2 bg-purple-600 text-white rounded hover:bg-purple-700 transition"
        >
          Thử lại
        </button>
      </div>
    </div>
  );
}

function RootLayout() {
  const location = useLocation();
  const navigation = useNavigation();
  const isTestPage =
    /^\/test\/\w+\/\d+\/\w+$/.test(location.pathname) ||
    /^\/test\/writing\/(?!review)\w+\/\d+$/.test(location.pathname);
  const isReviewPage =
    /^\/test\/writing\/review\/\d+$/.test(location.pathname) ||
    /^\/test\/review\/\d+$/.test(location.pathname);
  const isAdminPage = location.pathname.startsWith('/admin');
  const showHeader = !isTestPage && !isReviewPage && !isAdminPage;

  return (
    <>
      {navigation.state === 'loading' && (
        <div
          className="fixed top-0 left-0 w-full h-0.5 z-[9999] bg-purple-100"
          style={{ overflow: 'hidden' }}
        >
          <div
            className="h-full bg-purple-600 animate-pulse"
            style={{
              width: '40%',
              animation: 'navLoader 1.2s ease-in-out infinite',
            }}
          />
          <style>{`
            @keyframes navLoader {
              0% { transform: translateX(-100%); }
              100% { transform: translateX(350%); }
            }
          `}</style>
        </div>
      )}
      {showHeader && <Header />}
      <main className={showHeader ? 'with-fixed-header' : ''}>
        <AnimatedOutlet />
      </main>
      {(isTestPage) && <SmallViewportWarning minWidth={768} />}
      {!isTestPage && !isReviewPage && !isAdminPage && <Footer />}
      {!isTestPage && !isAdminPage && <FloatingAssistant />}
    </>
  );
}

export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootLayout />,
    errorElement: <RouteError />,
    children: [
      // ============ PUBLIC ROUTES ============
      { index: true, element: <PageWrapper><Home /></PageWrapper> },
      { path: 'login', element: <Login /> },
      { path: 'about', ...lazyPage(() => import('./pages/About')) },
      { path: 'pricing', ...lazyPage(() => import('./pages/PricingPage')) },
      { path: 'payment/cancel', ...lazyRoute(() => import('./pages/PaymentCancelPage')) },

      // ============ PROTECTED ROUTES ============
      {
        element: <ProtectedRoute />,
        children: [
          { path: 'dashboard', ...lazyPage(() => import('./pages/Dashboard')) },
          { path: 'courses', ...lazyPage(() => import('./pages/Courses')) },
          { path: 'courses/:courseName', ...lazyPage(() => import('./pages/CourseDetailPage')) },
          { path: 'profile', ...lazyPage(() => import('./pages/Profile')) },
          { path: 'vocabulary', ...lazyPage(() => import('./pages/VocabularyPage')) },
          { path: 'subscription', ...lazyPage(() => import('./pages/SubscriptionPage')) },
          { path: 'test/review/:attemptId', ...lazyPage(() => import('./pages/TestReviewPage')) },
          { path: 'test/writing/review/:attemptId', ...lazyPage(() => import('./pages/WritingResultPage')) },
          { path: 'test/:source/:testNum/:skill', ...lazyTestPage() },
          { path: 'test/writing/:source/:testNum', ...lazyRoute(() => import('./pages/WritingTestPage')) },
          { path: 'payment/success', ...lazyRoute(() => import('./pages/PaymentSuccessPage')) },
        ],
      },

      // ============ ADMIN ROUTES ============
      {
        path: 'admin',
        async lazy() {
          const mod = await import('./admin/components/AdminRouteGuard');
          return { Component: mod.default };
        },
        children: [
          {
            async lazy() {
              const mod = await import('./admin/components/layout/AdminLayout');
              return { Component: mod.default };
            },
            children: [
              { index: true, ...lazyRoute(() => import('./admin/pages/AdminDashboard')) },
              { path: 'dashboard', ...lazyRoute(() => import('./admin/pages/AdminDashboard')) },
              { path: 'users', ...lazyRoute(() => import('./admin/pages/users/UserListPage')) },
              { path: 'users/:userId', ...lazyRoute(() => import('./admin/pages/users/UserDetailPage')) },
              { path: 'finance', ...lazyRoute(() => import('./admin/pages/finance/FinanceDashboard')) },
              { path: 'finance/transactions', ...lazyRoute(() => import('./admin/pages/finance/TransactionHistoryPage')) },
              { path: 'finance/reports', ...lazyRoute(() => import('./admin/pages/finance/ReportsPage')) },
              { path: 'content', ...lazyRoute(() => import('./admin/pages/content/ContentListPage')) },
              { path: 'content/hub', element: <Navigate to="/admin/content/sets" replace /> },
              { path: 'content/hashtags', ...lazyRoute(() => import('./admin/pages/content/HashtagManagementPage')) },
              { path: 'content/generate', ...lazyRoute(() => import('./admin/pages/content/AIGenerationPage')) },
              { path: 'content/editor', element: <Navigate to="/admin/content/sets" replace /> },
              { path: 'content/sets', ...lazyRoute(() => import('./admin/pages/content/SetListPage')) },
              { path: 'content/sets/:setId', ...lazyRoute(() => import('./admin/pages/content/SetDetailPage')) },
              { path: 'content/editor/:testId', ...lazyRoute(() => import('./admin/pages/content/TestEditorPage')) },
              { path: 'content/tests/:testId', ...lazyRoute(() => import('./admin/pages/content/TestEditorPage')) },
              { path: 'content/editor/:examSource/:testNumber', ...lazyRoute(() => import('./admin/pages/content/TestEditorPage')) },
            ],
          },
        ],
      },

      // ============ 404 CATCH-ALL ============
      { path: '*', element: <NotFound /> },
    ],
  },
]);

function NotFound() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="text-center p-8">
        <h1 className="text-6xl font-bold text-gray-300 mb-4">404</h1>
        <p className="text-xl text-gray-600 mb-6">Trang bạn tìm kiếm không tồn tại.</p>
        <Link to="/" className="px-6 py-2 bg-purple-600 text-white rounded hover:bg-purple-700 transition inline-block">
          Về trang chủ
        </Link>
      </div>
    </div>
  );
}
