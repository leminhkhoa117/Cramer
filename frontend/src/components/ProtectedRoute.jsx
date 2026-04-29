import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../stores';

export default function ProtectedRoute({ children }) {
  const user = useAuthStore((state) => state.user);
  if (!user) return <Navigate to="/login" replace />;
  return children ?? <Outlet />;
}
