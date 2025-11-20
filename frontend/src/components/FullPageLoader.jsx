import React from 'react';
import { motion } from 'framer-motion';
import '../css/FullPageLoader.css';
import cramerLoadingGif from '../../pictures/anim/cramer_loading.gif';

const FullPageLoader = ({
  message = 'Đang xử lý yêu cầu của bạn...',
  subMessage = 'Vui lòng giữ trang mở trong giây lát.',
}) => {
  return (
    <motion.div 
      className="fullpage-loader-backdrop"
      initial={{ opacity: 0, backdropFilter: "blur(0px)" }}
      animate={{ opacity: 1, backdropFilter: "blur(8px)" }}
      exit={{ opacity: 0, backdropFilter: "blur(0px)" }}
      transition={{ duration: 0.4, ease: "easeInOut" }}
    >
      <motion.div
        className="fullpage-loader-card"
        initial={{ opacity: 0, scale: 0.9, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.9, y: 20 }}
        transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }} // Custom cubic bezier for elegance
      >
        <img 
          src={cramerLoadingGif} 
          alt="Loading..." 
          className="fullpage-loader-gif"
        />

        <h2 className="fullpage-loader-title">{message}</h2>
        {subMessage && <p className="fullpage-loader-subtext">{subMessage}</p>}
      </motion.div>
    </motion.div>
  );
};

export default FullPageLoader;

