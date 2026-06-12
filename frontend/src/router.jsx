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
    <div className="min-h-screen flex items-center justify-center bg-page">
      <div className="text-center p-8">
        <h1 className="text-3xl font-bold text-ink mb-2">Lỗi tải trang</h1>
        <p className="text-base text-muted mb-5">Không thể tải trang này. Vui lòng thử lại.</p>
        <button
          onClick={() => window.location.reload()}
          className="inline-flex items-center rounded-lg bg-brand-600 px-5 py-2.5 text-base font-semibold text-white transition-colors hover:bg-brand-700"
        >
          Thử lại
        </button>
      </div>
    </div>
  );
}

function RouteLoadingFallback() {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center gap-3 bg-page">
      <span className="inline-block h-7 w-7 rounded-full border-2 border-brand-200 border-t-brand-600 animate-[cr-spin_0.6s_linear_infinite]" />
      <p className="text-base font-semibold text-muted">Đang tải…</p>
    </div>
  );
}

function AdminRouteLoadingFallback() {
  return (
    <div
      className="admin-root"
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#0F0F23',
        color: '#F8FAFC',
      }}
    >
      <div style={{ textAlign: 'center' }}>
        <div
          style={{
            width: 36,
            height: 36,
            margin: '0 auto 14px',
            border: '3px solid rgba(139, 92, 246, 0.25)',
            borderTopColor: '#8B5CF6',
            borderRadius: '50%',
            animation: 'adminRouteSpin 0.9s linear infinite',
          }}
        />
        <style>{`@keyframes adminRouteSpin { to { transform: rotate(360deg); } }`}</style>
        <div style={{ fontSize: '0.95rem', fontWeight: 600 }}>Đang mở trang quản trị...</div>
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
          className="fixed top-0 left-0 w-full h-0.5 bg-brand-100"
          style={{ zIndex: 'var(--z-toast)', overflow: 'hidden' }}
        >
          <div
            className="h-full bg-brand-600"
            style={{ width: '40%', animation: 'navLoader 1.2s ease-in-out infinite' }}
          />
          <style>{`@keyframes navLoader { 0% { transform: translateX(-100%); } 100% { transform: translateX(350%); } }`}</style>
        </div>
      )}
      {showHeader && <Header />}
      <main className={showHeader ? 'with-fixed-header' : ''}>
        {navigation.state === 'loading' && isAdminPage ? <AdminRouteLoadingFallback /> : <AnimatedOutlet />}
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
    HydrateFallback: RouteLoadingFallback,
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
    <div className="min-h-screen flex items-center justify-center bg-page">
      <div className="text-center p-8">
        <h1 className="text-6xl font-bold text-brand-200 mb-2">404</h1>
        <p className="text-lg text-muted mb-6">Trang bạn tìm kiếm không tồn tại.</p>
        <Link to="/" className="inline-flex items-center rounded-lg bg-brand-600 px-5 py-2.5 text-base font-semibold text-white transition-colors hover:bg-brand-700">
          Về trang chủ
        </Link>
      </div>
    </div>
  );
}
