import React from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { FiX, FiArrowLeft, FiHelpCircle } from 'react-icons/fi';
import '../css/payment-page.css';

export default function PaymentCancelPage() {
  const [searchParams] = useSearchParams();
  const orderCode = searchParams.get('orderCode');

  return (
    <div className="payment-page">
      <div className="payment-container">
        <motion.div 
          className="payment-card payment-card--cancel"
          initial={{ scale: 0.9, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          transition={{ duration: 0.4 }}
        >
          <motion.div 
            className="payment-card__icon payment-card__icon--cancel"
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ delay: 0.2, type: 'spring', stiffness: 200 }}
          >
            <FiX />
          </motion.div>
          
          <h1 className="payment-card__title">Thanh toán đã bị hủy</h1>
          
          <p className="payment-card__message">
            Bạn đã hủy giao dịch thanh toán. Không có khoản phí nào được thu.
          </p>

          {orderCode && (
            <div className="payment-card__details">
              <div className="payment-detail">
                <span className="payment-detail__label">Mã đơn hàng:</span>
                <span className="payment-detail__value">#{orderCode}</span>
              </div>
            </div>
          )}

          <p className="payment-card__subtext">
            Nếu bạn gặp vấn đề trong quá trình thanh toán, vui lòng liên hệ với chúng tôi.
          </p>

          <div className="payment-card__actions">
            <Link to="/pricing">
              <button className="payment-card__btn payment-card__btn--primary">
                <FiArrowLeft />
                Quay lại trang giá
              </button>
            </Link>
            <a href="mailto:support@cramer.vn">
              <button className="payment-card__btn payment-card__btn--secondary">
                <FiHelpCircle />
                Liên hệ hỗ trợ
              </button>
            </a>
          </div>
        </motion.div>
      </div>
    </div>
  );
}