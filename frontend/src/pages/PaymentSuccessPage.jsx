import { useEffect, useState } from 'react';
import { useSearchParams, Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { FiCheck, FiArrowRight, FiClock, FiAlertTriangle } from 'react-icons/fi';
import { paymentApi, getApiError } from '../lib/api';
import { useUserStatsStore, useAuthStore } from '../stores';
import { toast } from '../ui/toast';
import { Button, Spinner } from '../ui';

export default function PaymentSuccessPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const fetchUserStats = useUserStatsStore((s) => s.fetchUserStats);

  const [loading, setLoading] = useState(true);
  const [details, setDetails] = useState(null);
  const [error, setError] = useState(null);
  const orderCode = searchParams.get('orderCode');

  useEffect(() => {
    if (!user) { navigate('/login'); return; }
    if (!orderCode) { setError('Không tìm thấy mã đơn hàng trong URL.'); setLoading(false); return; }

    (async () => {
      try {
        const data = await paymentApi.status(orderCode);
        setDetails(data);
        if (data.status === 'PAID') {
          await fetchUserStats(true);
          toast.success(data.type === 'SUBSCRIPTION' ? '🎉 Nâng cấp gói thành công!' : '🌾 Đã nhận Lúa vào tài khoản!');
        } else if (data.status === 'PENDING') {
          toast.info('Đơn hàng đang được xử lý. Vui lòng đợi vài giây và làm mới trang.');
        } else {
          toast.error('Giao dịch không thành công.');
        }
      } catch (err) {
        const e = getApiError(err);
        setError(e.status === 404 ? 'Không tìm thấy đơn hàng.' :
          e.status === 403 ? 'Đơn hàng này không thuộc về tài khoản của bạn.' :
          e.status === 401 ? 'Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.' :
          'Không thể xác minh trạng thái thanh toán. Vui lòng kiểm tra lịch sử thanh toán.');
      } finally { setLoading(false); }
    })();
  }, [orderCode, user, navigate, fetchUserStats]);

  if (loading) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-page">
        <Spinner size="lg" className="text-brand-600" />
        <p className="text-base font-semibold text-muted">Đang xác nhận thanh toán…</p>
      </div>
    );
  }

  const status = details?.status;
  const paid = status === 'PAID';
  const pending = status === 'PENDING';

  const icon = error || (!paid && !pending) ? <FiAlertTriangle size={32} /> : pending ? <FiClock size={32} /> : <FiCheck size={32} />;
  const tint = error || (!paid && !pending) ? 'bg-danger-soft text-danger' : pending ? 'bg-warning-soft text-warning' : 'bg-success-soft text-success';
  const title = error ? 'Có lỗi xảy ra' : paid ? 'Thanh toán thành công' : pending ? 'Đang xử lý' : 'Giao dịch không thành công';
  const message = error || (paid
    ? 'Cảm ơn bạn! Giao dịch đã được xác nhận và tài khoản của bạn đã được cập nhật.'
    : pending ? 'Đơn hàng đang được xử lý. Vui lòng làm mới trang sau giây lát.'
    : 'Giao dịch chưa hoàn tất. Vui lòng kiểm tra lịch sử thanh toán.');

  return (
    <div className="flex min-h-screen items-center justify-center bg-page p-4">
      <motion.div
        className="w-full max-w-md rounded-2xl border border-line bg-surface p-7 text-center shadow-lg"
        initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} transition={{ duration: 0.3 }}
      >
        <div className={`mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full ${tint}`}>{icon}</div>
        <h1 className="text-2xl font-bold text-ink">{title}</h1>
        <p className="mt-2 text-base text-muted">{message}</p>
        {details?.orderCode && (
          <div className="mt-4 rounded-lg bg-surface-2 px-4 py-2.5 text-base">
            <span className="text-muted">Mã đơn hàng: </span>
            <span className="font-semibold text-ink">#{details.orderCode}</span>
          </div>
        )}
        <div className="mt-6 flex flex-col gap-2">
          <Link to="/dashboard"><Button fullWidth iconRight={<FiArrowRight size={16} />}>Về bảng điều khiển</Button></Link>
          <Link to="/subscription"><Button variant="ghost" fullWidth>Xem gói đăng ký</Button></Link>
        </div>
      </motion.div>
    </div>
  );
}
