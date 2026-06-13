import { useSearchParams, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { FiX, FiArrowLeft, FiHelpCircle } from 'react-icons/fi';
import { Button } from '../ui';

export default function PaymentCancelPage() {
  const [searchParams] = useSearchParams();
  const orderCode = searchParams.get('orderCode');

  return (
    <div className="flex min-h-screen items-center justify-center bg-page p-4">
      <motion.div
        className="w-full max-w-md rounded-2xl border border-line bg-surface p-7 text-center shadow-lg"
        initial={{ scale: 0.95, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ duration: 0.3 }}
      >
        <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-danger-soft text-danger">
          <FiX size={32} />
        </div>
        <h1 className="text-2xl font-bold text-ink">Thanh toán đã bị hủy</h1>
        <p className="mt-2 text-base text-muted">Bạn đã hủy giao dịch. Không có khoản phí nào được thu.</p>
        {orderCode && (
          <div className="mt-4 rounded-lg bg-surface-2 px-4 py-2.5 text-base">
            <span className="text-muted">Mã đơn hàng: </span>
            <span className="font-semibold text-ink">#{orderCode}</span>
          </div>
        )}
        <div className="mt-6 flex flex-col gap-2">
          <Link to="/pricing"><Button fullWidth iconLeft={<FiArrowLeft size={16} />}>Quay lại trang giá</Button></Link>
          <a href="mailto:support@cramer.vn"><Button variant="ghost" fullWidth iconLeft={<FiHelpCircle size={16} />}>Liên hệ hỗ trợ</Button></a>
        </div>
      </motion.div>
    </div>
  );
}
