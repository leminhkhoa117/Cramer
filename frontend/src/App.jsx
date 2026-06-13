import { useEffect } from 'react';
import { RouterProvider } from 'react-router-dom';
import { useAuthStore, useProfileStore } from './stores';
import { router } from './router';
import { Spinner, Button, Toaster } from './ui';

function AuthInitializer({ children }) {
  const { loading, error, initializeAuth } = useAuthStore();

  useEffect(() => {
    initializeAuth();
  }, [initializeAuth]);

  useEffect(() => {
    void useProfileStore.getState();
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center gap-3 bg-page">
        <Spinner size="lg" className="text-brand-600" />
        <p className="text-base font-semibold text-muted">Đang khởi tạo ứng dụng…</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-page p-4">
        <div className="max-w-md w-full rounded-2xl border border-line bg-surface p-6 shadow-lg text-center">
          <div className="text-5xl mb-3">🔒</div>
          <h2 className="text-xl font-bold text-danger mb-2">Lỗi Xác Thực</h2>
          <p className="text-base text-ink-2 mb-4">{error}</p>
          <div className="text-left bg-surface-2 p-4 rounded-lg mb-4">
            <p className="font-bold text-ink mb-2">💡 Cách khắc phục:</p>
            <ul className="list-disc list-inside space-y-1 text-sm text-muted">
              <li>Bật cookies trong cài đặt trình duyệt</li>
              <li>Cho phép lưu trữ dữ liệu cho trang này</li>
              <li>Hoặc thử trình duyệt khác (Chrome, Firefox, Zen)</li>
            </ul>
          </div>
          <Button onClick={() => window.location.reload()} fullWidth>Thử lại</Button>
        </div>
      </div>
    );
  }

  return children;
}

export default function App() {
  return (
    <AuthInitializer>
      <RouterProvider router={router} />
      <Toaster />
    </AuthInitializer>
  );
}
