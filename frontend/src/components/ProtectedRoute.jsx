import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '../stores';

export default function ProtectedRoute({ children }) {
  const user = useAuthStore((state) => state.user);
  const location = useLocation();

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return children ?? <Outlet />;
}
