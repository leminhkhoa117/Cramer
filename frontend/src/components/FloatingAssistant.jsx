import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  FiMessageCircle,
  FiX,
  FiMinus,
  FiSend,
  FiPhone,
  FiAlertCircle
} from 'react-icons/fi';
import { useAuthStore, useUserStatsStore } from '../stores';
import { chatApi } from '../api/backendApi';
import ChatBubble from './ChatBubble';
import '../css/FloatingAssistant.css';

/**
 * Floating Assistant Widget - "Trợ lý Cramer"
 * 
 * A persistent floating UI element that provides:
 * - User's Lúa balance and subscription tier display
 * - AI-powered chatbot for IELTS help
 * - Quick access to customer support
 * 
 * Fixed position at bottom-right corner (not draggable).
 */
const FloatingAssistant = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAuthStore((state) => state.user);

  const {
    credits,
    chatUsage,
    fetchUserStats,
    incrementChatUsage,
    refreshChatUsage,
    getTierEmoji,
    getTierName,
  } = useUserStatsStore();

  // Widget states
  const [isExpanded, setIsExpanded] = useState(false);
  const [isMinimized, setIsMinimized] = useState(false);

  // Chat states
  const [messages, setMessages] = useState([
    {
      id: 'welcome',
      role: 'assistant',
      content: 'Xin chào! 👋 Mình là Cramer, trợ lý học IELTS của bạn. Bạn cần giúp gì hôm nay?',
      timestamp: new Date().toISOString(),
    }
  ]);
  const [inputValue, setInputValue] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  // Refs
  const chatContainerRef = useRef(null);
  const inputRef = useRef(null);

  // Pages where widget should be hidden
  const hiddenPaths = ['/', '/login', '/register', '/about'];

  // Check if on test-taking pages (Reading/Listening test or Writing test)
  const isTestPage = /^\/test\/\w+\/\d+\/\w+$/.test(location.pathname) ||
    /^\/test\/writing\/(?!review)\w+\/\d+$/.test(location.pathname);

  const shouldHide = !user || hiddenPaths.includes(location.pathname) || isTestPage;

  // Fetch user stats on mount
  useEffect(() => {
    if (user) {
      fetchUserStats();
    }
  }, [user, fetchUserStats]);

  // Scroll to bottom when new messages arrive
  useEffect(() => {
    if (chatContainerRef.current) {
      chatContainerRef.current.scrollTop = chatContainerRef.current.scrollHeight;
    }
  }, [messages]);

  // Focus input when expanded
  useEffect(() => {
    if (isExpanded && inputRef.current) {
      setTimeout(() => inputRef.current?.focus(), 300);
    }
  }, [isExpanded]);

  // Send message to AI
  const handleSendMessage = async () => {
    if (!inputValue.trim() || isLoading) return;

    // Check if user has remaining questions
    if (chatUsage.remainingToday <= 0) {
      setError('Bạn đã hết lượt hỏi hôm nay. Hãy nâng cấp gói để có thêm lượt!');
      return;
    }

    const userMessage = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: inputValue.trim(),
      timestamp: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInputValue('');
    setIsLoading(true);
    setError(null);

    // Optimistic update
    incrementChatUsage();

    try {
      const response = await chatApi.sendMessage(userMessage.content);

      const assistantMessage = {
        id: `assistant-${Date.now()}`,
        role: 'assistant',
        content: response.data?.message || response.data?.content || 'Xin lỗi, mình không hiểu. Bạn có thể nói rõ hơn được không?',
        timestamp: new Date().toISOString(),
      };

      setMessages((prev) => [...prev, assistantMessage]);

      // Refresh chat usage from server to get accurate remaining count
      await refreshChatUsage();
    } catch (err) {
      console.error('Chat error:', err);

      // Handle specific errors
      if (err.response?.status === 429) {
        setError('Bạn đã hết lượt hỏi hôm nay.');
        refreshChatUsage();
      } else if (err.response?.status === 401) {
        setError('Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.');
      } else {
        // Add error message to chat
        setMessages((prev) => [
          ...prev,
          {
            id: `error-${Date.now()}`,
            role: 'assistant',
            content: 'Xin lỗi, có lỗi xảy ra. Vui lòng thử lại sau nhé! 🙏',
            timestamp: new Date().toISOString(),
          }
        ]);
      }
    } finally {
      setIsLoading(false);
    }
  };

  // Handle Enter key
  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  // Toggle expand/collapse
  const toggleExpand = () => {
    if (isMinimized) {
      setIsMinimized(false);
    }
    setIsExpanded((prev) => !prev);
  };

  // Minimize widget (collapse to smaller form, shows header only)
  const handleMinimize = (e) => {
    e.stopPropagation();
    setIsExpanded(false);
  };

  // Minimize to floating icon button (X button behavior)
  const handleMinimizeToIcon = (e) => {
    e.stopPropagation();
    setIsExpanded(false);
    setIsMinimized(true);
  };

  // Navigate to credits page
  const handleCreditsClick = (e) => {
    e.stopPropagation();
    navigate('/pricing');
  };

  // Don't render if should be hidden
  if (shouldHide) {
    return null;
  }

  // Compute tier display
  const tierEmoji = getTierEmoji();
  const tierName = getTierName();

  // Minimized icon button (when X is clicked)
  if (isMinimized) {
    return (
      <button
        className="fa-widget__minimized-btn"
        onClick={() => {
          setIsMinimized(false);
          setIsExpanded(true);
        }}
        title="Mở Trợ lý Cramer"
      >
        <FiMessageCircle className="fa-widget__minimized-icon" />
        <span className="fa-widget__minimized-badge">{tierEmoji}</span>
      </button>
    );
  }

  return (
    <div className={`fa-widget ${isExpanded ? 'fa-widget--expanded' : ''}`}>
      {/* Header - Always visible */}
      <div
        className="fa-widget__header"
        onClick={!isExpanded ? toggleExpand : undefined}
      >
        <div className="fa-widget__tier">
          <span className="fa-widget__tier-emoji">{tierEmoji}</span>
          <span className="fa-widget__tier-name">{tierName}</span>
        </div>

        <button
          className="fa-widget__balance"
          onClick={handleCreditsClick}
          title="Xem chi tiết Lúa"
        >
          <span className="fa-widget__balance-icon">💰</span>
          <span className="fa-widget__balance-value">{credits.balance}</span>
          <span className="fa-widget__balance-label">Lúa</span>
        </button>

        {isExpanded && (
          <div className="fa-widget__controls">
            <button
              className="fa-widget__control-btn"
              onClick={handleMinimize}
              title="Thu nhỏ"
            >
              <FiMinus />
            </button>
            <button
              className="fa-widget__control-btn"
              onClick={handleMinimizeToIcon}
              title="Thu gọn thành nút"
            >
              <FiX />
            </button>
          </div>
        )}

        {!isExpanded && (
          <button className="fa-widget__expand-btn" onClick={toggleExpand}>
            <FiMessageCircle />
          </button>
        )}
      </div>

      {/* Expandable content */}
      {isExpanded && (
        <>
          {/* Chat area */}
          <div className="fa-widget__chat-area" ref={chatContainerRef}>
            {messages.map((msg) => (
              <ChatBubble
                key={msg.id}
                message={msg.content}
                isUser={msg.role === 'user'}
                timestamp={msg.timestamp}
              />
            ))}

            {isLoading && (
              <ChatBubble isLoading={true} />
            )}
          </div>

          {/* Usage indicator */}
          <div className="fa-widget__usage">
            <span className="fa-widget__usage-text">
              {chatUsage.remainingToday < 0
                ? 'Không giới hạn tin nhắn'
                : `Còn ${chatUsage.remainingThisMonth ?? chatUsage.remainingToday}/${chatUsage.monthlyLimit ?? chatUsage.dailyLimit} câu hỏi tháng này`
              }
            </span>
            {chatUsage.remainingToday >= 0 && (
              <div className="fa-widget__usage-bar">
                <div
                  className="fa-widget__usage-fill"
                  style={{
                    width: `${((chatUsage.remainingThisMonth ?? chatUsage.remainingToday) / (chatUsage.monthlyLimit ?? chatUsage.dailyLimit)) * 100}%`
                  }}
                />
              </div>
            )}
          </div>

          {/* Error message */}
          {error && (
            <div className="fa-widget__error">
              <FiAlertCircle />
              <span>{error}</span>
              <button onClick={() => setError(null)}>×</button>
            </div>
          )}

          {/* Input area */}
          <div className="fa-widget__input-area">
            <input
              ref={inputRef}
              type="text"
              className="fa-widget__input"
              placeholder="Nhập câu hỏi..."
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              onKeyDown={handleKeyDown}
              disabled={isLoading || chatUsage.remainingToday <= 0}
            />
            <button
              className="fa-widget__send-btn"
              onClick={handleSendMessage}
              disabled={!inputValue.trim() || isLoading || chatUsage.remainingToday <= 0}
              title="Gửi"
            >
              <FiSend />
            </button>
          </div>

          {/* Support link */}
          <div className="fa-widget__support">
            <a
              href="mailto:support@cramer.edu.vn"
              className="fa-widget__support-link"
              onClick={(e) => e.stopPropagation()}
            >
              <FiPhone />
              <span>Liên hệ hỗ trợ</span>
            </a>
          </div>
        </>
      )}
    </div>
  );
};

export default FloatingAssistant;

