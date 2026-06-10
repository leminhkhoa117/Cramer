import { useEffect } from 'react';
import { RouterProvider } from 'react-router-dom';
import { useAuthStore, useProfileStore } from './stores';
import { router } from './router';
import './css/full-page-loader.css';

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

export default function App() {
  return (
    <AuthInitializer>
      <RouterProvider router={router} />
    </AuthInitializer>
  );
}
