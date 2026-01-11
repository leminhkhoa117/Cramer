import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  FiMessageCircle,
  FiX,
  FiSend,
  FiPhone,
  FiAlertCircle
} from 'react-icons/fi';
import { useAuthStore, useUserStatsStore } from '../stores';
import { chatApi } from '../api/backendApi';
import ChatBubble from './ChatBubble';
import '../css/floating-assistant.css';

/**
 * Floating Assistant Widget - "Trợ lý Cramer"
 *
 * A persistent floating UI element that provides:
 * - User's Lúa balance and subscription tier display
 * - AI-powered chatbot for IELTS help
 * - Quick access to customer support
 *
 * Two states:
 * - Collapsed: Compact pill showing tier emoji + "Cramer" + balance badge
 * - Expanded: Full chat interface
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

  // Single state: expanded or collapsed (pill)
  const [isExpanded, setIsExpanded] = useState(false);

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

    // Check if user has remaining questions (use monthly limit)
    const remaining = chatUsage.remainingThisMonth ?? chatUsage.remainingToday;
    if (remaining !== undefined && remaining !== null && remaining <= 0 && remaining !== -1) {
      setError('Bạn đã hết lượt hỏi tháng này. Hãy nâng cấp gói để có thêm lượt!');
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

  // Close widget (collapse to pill)
  const handleClose = (e) => {
    e.stopPropagation();
    setIsExpanded(false);
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

  // Collapsed state: Compact pill
  if (!isExpanded) {
    return (
      <button
        className="fa-widget__pill"
        onClick={() => setIsExpanded(true)}
        title={`Trợ lý Cramer - ${tierName} - ${credits.balance} Lúa`}
      >
        <span className="fa-widget__pill-emoji">{tierEmoji}</span>
        <span className="fa-widget__pill-label">Cramer</span>
        <FiMessageCircle className="fa-widget__pill-icon" />
        <span className="fa-widget__pill-badge">{credits.balance}</span>
      </button>
    );
  }

  // Expanded state: Full chat interface
  return (
    <div className="fa-widget fa-widget--expanded">
      {/* Header */}
      <div className="fa-widget__header">
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

        <div className="fa-widget__controls">
          <button
            className="fa-widget__control-btn"
            onClick={handleClose}
            title="Đóng"
          >
            <FiX />
          </button>
        </div>
      </div>

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
          {(chatUsage.remainingThisMonth ?? chatUsage.remainingToday) < 0
            ? 'Không giới hạn tin nhắn'
            : `Còn ${chatUsage.remainingThisMonth ?? chatUsage.remainingToday}/${chatUsage.monthlyLimit ?? chatUsage.dailyLimit} câu hỏi tháng này`
          }
        </span>
        {(chatUsage.remainingThisMonth ?? chatUsage.remainingToday) >= 0 && (
          <div className="fa-widget__usage-bar">
            <div
              className="fa-widget__usage-fill"
              style={{
                width: `${Math.min(100, ((chatUsage.remainingThisMonth ?? chatUsage.remainingToday) / (chatUsage.monthlyLimit ?? chatUsage.dailyLimit)) * 100)}%`
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
          disabled={isLoading || ((chatUsage.remainingThisMonth ?? chatUsage.remainingToday) <= 0 && (chatUsage.remainingThisMonth ?? chatUsage.remainingToday) !== -1)}
        />
        <button
          className="fa-widget__send-btn"
          onClick={handleSendMessage}
          disabled={!inputValue.trim() || isLoading || ((chatUsage.remainingThisMonth ?? chatUsage.remainingToday) <= 0 && (chatUsage.remainingThisMonth ?? chatUsage.remainingToday) !== -1)}
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
    </div>
  );
};

export default FloatingAssistant;
