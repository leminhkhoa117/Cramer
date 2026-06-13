import React, { useEffect, useState } from 'react';
import { useSearchParams, Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { FiCheck, FiGift, FiArrowRight, FiLoader, FiClock, FiAlertTriangle } from 'react-icons/fi';
import { paymentApi } from '../api/backendApi';
import { useUserStatsStore, useAuthStore } from '../stores';
import { showSuccessToast, showErrorToast } from '../utils/toast';
import '../css/payment-page.css';

export default function PaymentSuccessPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const user = useAuthStore(state => state.user);
  const { fetchUserStats } = useUserStatsStore();

  const [loading, setLoading] = useState(true);
  const [paymentDetails, setPaymentDetails] = useState(null);
  const [error, setError] = useState(null);

  // Get order code from URL params
  const orderCode = searchParams.get('orderCode');

  useEffect(() => {
    if (!user) {
      navigate('/login');
      return;
    }

    if (!orderCode) {
      setError('Không tìm thấy mã đơn hàng trong URL.');
      setLoading(false);
      return;
    }

    // Fetch payment details — DO NOT assume PAID on error.
    // PayOS redirects to /payment/success even on user cancel / expire flows
    // (depending on PayOS config). The only authoritative source for payment
    // status is our backend. See bug U1 in BUG_AUDIT_2026-04-23.md.
    const fetchPaymentDetails = async () => {
      try {
        const response = await paymentApi.getStatus(orderCode);
        const data = response.data;
        setPaymentDetails(data);

        if (data.status === 'PAID') {
          // Refresh user stats to reflect updated subscription/credits
          await fetchUserStats();

          if (data.type === 'SUBSCRIPTION') {
            showSuccessToast(
              `🎉 Chúc mừng! Bạn đã nâng cấp thành công lên gói ${data.tierCode || 'mới'}!`
            );
          } else if (data.type === 'LUA_PACK') {
            showSuccessToast(`🌾 Bạn đã nhận được ${data.luaAmount} Lúa!`);
          }
        } else if (data.status === 'PENDING') {
          // PayOS may redirect before webhook fires — let user know
          showErrorToast('Đơn hàng đang được xử lý. Vui lòng đợi vài giây và làm mới trang.');
        } else if (data.status === 'CANCELLED' || data.status === 'EXPIRED' || data.status === 'FAILED') {
          showErrorToast('Giao dịch không thành công.');
        }
      } catch (err) {
        console.error('Error fetching payment details:', err);
        const httpStatus = err?.response?.status;
        if (httpStatus === 404) {
          setError('Không tìm thấy đơn hàng. Vui lòng kiểm tra lại lịch sử thanh toán.');
        } else if (httpStatus === 403) {
          setError('Đơn hàng này không thuộc về tài khoản của bạn.');
        } else if (httpStatus === 401) {
          setError('Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.');
        } else {
          setError('Không thể xác minh trạng thái thanh toán. Vui lòng kiểm tra lịch sử thanh toán hoặc thử lại sau.');
        }
        setPaymentDetails(null);
      } finally {
        setLoading(false);
      }
    };

    fetchPaymentDetails();
  }, [orderCode, user, navigate, fetchUserStats]);

  if (loading) {
    return (
      <div className="payment-page">
        <div className="payment-container">
          <div className="payment-loading">
            <FiLoader className="payment-loading__spinner" />
            <p>Đang xác nhận thanh toán...</p>
          </div>
        </div>
      </div>
    );
  }

  // Network/HTTP error path
  if (error) {
    return (
      <div className="payment-page">
        <div className="payment-container">
          <motion.div
            className="payment-card payment-card--error"
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ duration: 0.4 }}
          >
            <div className="payment-card__icon payment-card__icon--error">⚠️</div>
            <h1 className="payment-card__title">Có lỗi xảy ra</h1>
            <p className="payment-card__message">{error}</p>
            <div className="payment-card__actions">
              <Link to="/subscription?tab=history">
                <button className="payment-card__btn payment-card__btn--primary">
                  Lịch sử thanh toán
                  <FiArrowRight />
                </button>
              </Link>
              <Link to="/pricing">
                <button className="payment-card__btn payment-card__btn--secondary">
                  Quay lại trang giá
                </button>
              </Link>
            </div>
          </motion.div>
        </div>
      </div>
    );
  }

  // Backend returned a non-PAID status — render the appropriate variant
  const status = paymentDetails?.status;

  if (status === 'PENDING') {
    return (
      <div className="payment-page">
        <div className="payment-container">
          <motion.div
            className="payment-card payment-card--pending"
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ duration: 0.4 }}
          >
            <div className="payment-card__icon payment-card__icon--pending">
              <FiClock />
            </div>
            <h1 className="payment-card__title">Đơn hàng đang xử lý</h1>
            <p className="payment-card__message">
              Hệ thống đang chờ xác nhận từ cổng thanh toán. Vui lòng đợi vài giây và làm mới trang.
            </p>
            {orderCode && (
              <div className="payment-card__details">
                <div className="payment-detail">
                  <span className="payment-detail__label">Mã đơn hàng:</span>
                  <span className="payment-detail__value">#{orderCode}</span>
                </div>
              </div>
            )}
            <div className="payment-card__actions">
              <button
                className="payment-card__btn payment-card__btn--primary"
                onClick={() => window.location.reload()}
              >
                Làm mới
              </button>
              <Link to="/subscription?tab=history">
                <button className="payment-card__btn payment-card__btn--secondary">
                  Lịch sử thanh toán
                </button>
              </Link>
            </div>
          </motion.div>
        </div>
      </div>
    );
  }

  if (status === 'CANCELLED' || status === 'EXPIRED' || status === 'FAILED') {
    const titleMap = {
      CANCELLED: 'Giao dịch đã huỷ',
      EXPIRED: 'Đơn hàng hết hạn',
      FAILED: 'Thanh toán không thành công',
    };
    const messageMap = {
      CANCELLED: 'Bạn đã huỷ giao dịch. Bạn có thể tạo đơn hàng mới bất kỳ lúc nào.',
      EXPIRED: 'Đơn hàng đã quá thời hạn thanh toán. Vui lòng tạo đơn hàng mới.',
      FAILED: 'Giao dịch của bạn không thành công. Vui lòng thử lại hoặc chọn phương thức khác.',
    };
    return (
      <div className="payment-page">
        <div className="payment-container">
          <motion.div
            className="payment-card payment-card--error"
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ duration: 0.4 }}
          >
            <div className="payment-card__icon payment-card__icon--error">
              <FiAlertTriangle />
            </div>
            <h1 className="payment-card__title">{titleMap[status]}</h1>
            <p className="payment-card__message">{messageMap[status]}</p>
            {orderCode && (
              <div className="payment-card__details">
                <div className="payment-detail">
                  <span className="payment-detail__label">Mã đơn hàng:</span>
                  <span className="payment-detail__value">#{orderCode}</span>
                </div>
              </div>
            )}
            <div className="payment-card__actions">
              <Link to="/pricing">
                <button className="payment-card__btn payment-card__btn--primary">
                  Thử lại
                  <FiArrowRight />
                </button>
              </Link>
              <Link to="/subscription?tab=history">
                <button className="payment-card__btn payment-card__btn--secondary">
                  Lịch sử thanh toán
                </button>
              </Link>
            </div>
          </motion.div>
        </div>
      </div>
    );
  }

  // status === 'PAID' (or unknown but data exists): render the success card
  if (status !== 'PAID') {
    // Defensive fallback: unknown status — treat as error rather than success
    return (
      <div className="payment-page">
        <div className="payment-container">
          <motion.div
            className="payment-card payment-card--error"
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
          >
            <div className="payment-card__icon payment-card__icon--error">⚠️</div>
            <h1 className="payment-card__title">Trạng thái không xác định</h1>
            <p className="payment-card__message">
              Không thể xác định trạng thái đơn hàng. Vui lòng kiểm tra lịch sử thanh toán.
            </p>
            <Link to="/subscription?tab=history">
              <button className="payment-card__btn payment-card__btn--primary">
                Lịch sử thanh toán
                <FiArrowRight />
              </button>
            </Link>
          </motion.div>
        </div>
      </div>
    );
  }

  return (
    <div className="payment-page">
      <div className="payment-container">
        <motion.div
          className="payment-card payment-card--success"
          initial={{ scale: 0.9, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ duration: 0.4 }}
        >
          <motion.div
            className="payment-card__icon payment-card__icon--success"
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ delay: 0.2, type: 'spring', stiffness: 200 }}
          >
            <FiCheck />
          </motion.div>

          <h1 className="payment-card__title">Thanh toán thành công! 🎉</h1>

          {paymentDetails?.type === 'SUBSCRIPTION' ? (
            <>
              <p className="payment-card__message">
                Chúc mừng bạn đã nâng cấp lên gói <strong>{paymentDetails.tierCode || 'Premium'}</strong>!
              </p>
              <div className="payment-card__details">
                <div className="payment-detail">
                  <span className="payment-detail__label">Số tiền:</span>
                  <span className="payment-detail__value">
                    {paymentDetails.amountVnd?.toLocaleString('vi-VN')}đ
                  </span>
                </div>
                <div className="payment-detail">
                  <span className="payment-detail__label">Mã đơn hàng:</span>
                  <span className="payment-detail__value">#{orderCode}</span>
                </div>
              </div>
              <p className="payment-card__benefit">
                <FiGift className="benefit-icon" />
                Bạn đã được cộng thêm Lúa và mở khóa các tính năng AI!
              </p>
            </>
          ) : paymentDetails?.type === 'LUA_PACK' ? (
            <>
              <p className="payment-card__message">
                Bạn đã mua thành công <strong>{paymentDetails.luaAmount} Lúa</strong>!
              </p>
              <div className="payment-card__details">
                <div className="payment-detail">
                  <span className="payment-detail__label">Số tiền:</span>
                  <span className="payment-detail__value">
                    {paymentDetails.amountVnd?.toLocaleString('vi-VN')}đ
                  </span>
                </div>
                <div className="payment-detail">
                  <span className="payment-detail__label">Mã đơn hàng:</span>
                  <span className="payment-detail__value">#{orderCode}</span>
                </div>
              </div>
              <p className="payment-card__benefit">
                <FiGift className="benefit-icon" />
                Lúa đã được cộng vào tài khoản của bạn!
              </p>
            </>
          ) : (
            <>
              <p className="payment-card__message">
                Giao dịch của bạn đã được xác nhận thành công!
              </p>
              {orderCode && (
                <div className="payment-card__details">
                  <div className="payment-detail">
                    <span className="payment-detail__label">Mã đơn hàng:</span>
                    <span className="payment-detail__value">#{orderCode}</span>
                  </div>
                </div>
              )}
            </>
          )}

          <div className="payment-card__actions">
            <Link to="/dashboard">
              <button className="payment-card__btn payment-card__btn--primary">
                Về trang chủ
                <FiArrowRight />
              </button>
            </Link>
            <Link to="/courses">
              <button className="payment-card__btn payment-card__btn--secondary">
                Làm bài thi ngay
              </button>
            </Link>
          </div>
        </motion.div>
      </div>
    </div>
  );
}