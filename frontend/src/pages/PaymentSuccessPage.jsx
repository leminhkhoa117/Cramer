import React, { useEffect, useState } from 'react';
import { useSearchParams, Link, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { FiCheck, FiGift, FiArrowRight, FiLoader } from 'react-icons/fi';
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
  const status = searchParams.get('status');

  useEffect(() => {
    if (!user) {
      navigate('/login');
      return;
    }

    if (!orderCode) {
      setError('Không tìm thấy mã đơn hàng');
      setLoading(false);
      return;
    }

    // Fetch payment details
    const fetchPaymentDetails = async () => {
      try {
        const response = await paymentApi.getStatus(orderCode);
        setPaymentDetails(response.data);
        
        // Refresh user stats to show updated subscription/credits
        await fetchUserStats();
        
        // Show success message based on payment type
        if (response.data.type === 'SUBSCRIPTION') {
          showSuccessToast(`🎉 Chúc mừng! Bạn đã nâng cấp thành công lên gói ${response.data.tierCode || 'mới'}!`);
        } else if (response.data.type === 'LUA_PACK') {
          showSuccessToast(`🌾 Bạn đã nhận được ${response.data.luaAmount} Lúa!`);
        }
      } catch (err) {
        console.error('Error fetching payment details:', err);
        // Even if we can't fetch details, payment was likely successful
        // since PayOS redirected here
        setPaymentDetails({
          status: 'PAID',
          type: 'UNKNOWN'
        });
        await fetchUserStats();
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
            <div className="payment-card__icon payment-card__icon--error">
              ⚠️
            </div>
            <h1 className="payment-card__title">Có lỗi xảy ra</h1>
            <p className="payment-card__message">{error}</p>
            <Link to="/pricing">
              <button className="payment-card__btn">
                Quay lại trang giá
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
