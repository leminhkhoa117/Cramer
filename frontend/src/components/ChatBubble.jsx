import React, { useState } from 'react';
import { FiUser, FiCpu, FiCopy, FiCheck } from 'react-icons/fi';
import ReactMarkdown from 'react-markdown';
import '../css/FloatingAssistant.css';

/**
 * Chat bubble component for displaying messages in the floating assistant.
 * Supports user and assistant messages with markdown rendering.
 */
const ChatBubble = ({ message, isUser, timestamp, isLoading = false }) => {
  const [showTimestamp, setShowTimestamp] = useState(false);
  const [copied, setCopied] = useState(false);
  
  const formatTime = (ts) => {
    if (!ts) return '';
    const date = new Date(ts);
    return date.toLocaleTimeString('vi-VN', { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  };
  
  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(message);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };
  
  if (isLoading) {
    return (
      <div className="chat-bubble chat-bubble--assistant">
        <div className="chat-bubble__avatar">
          <FiCpu />
        </div>
        <div className="chat-bubble__content">
          <div className="chat-bubble__loading">
            <span className="chat-bubble__dot"></span>
            <span className="chat-bubble__dot"></span>
            <span className="chat-bubble__dot"></span>
          </div>
        </div>
      </div>
    );
  }
  
  return (
    <div 
      className={`chat-bubble ${isUser ? 'chat-bubble--user' : 'chat-bubble--assistant'}`}
      onMouseEnter={() => setShowTimestamp(true)}
      onMouseLeave={() => setShowTimestamp(false)}
    >
      {!isUser && (
        <div className="chat-bubble__avatar">
          <FiCpu />
        </div>
      )}
      
      <div className="chat-bubble__content">
        {isUser ? (
          <p className="chat-bubble__text">{message}</p>
        ) : (
          <div className="chat-bubble__markdown">
            <ReactMarkdown
              components={{
                p: ({ children }) => <p className="mb-2 last:mb-0">{children}</p>,
                ul: ({ children }) => <ul className="list-disc pl-4 mb-2">{children}</ul>,
                ol: ({ children }) => <ol className="list-decimal pl-4 mb-2">{children}</ol>,
                li: ({ children }) => <li className="mb-1">{children}</li>,
                code: ({ inline, children }) => 
                  inline ? (
                    <code className="bg-purple-100 text-purple-800 px-1 py-0.5 rounded text-sm">{children}</code>
                  ) : (
                    <pre className="bg-gray-100 p-2 rounded text-sm overflow-x-auto my-2">
                      <code>{children}</code>
                    </pre>
                  ),
                strong: ({ children }) => <strong className="font-semibold">{children}</strong>,
                em: ({ children }) => <em className="italic">{children}</em>,
              }}
            >
              {message}
            </ReactMarkdown>
          </div>
        )}
        
        {/* Timestamp tooltip */}
        {showTimestamp && timestamp && (
          <div className="chat-bubble__timestamp">
            {formatTime(timestamp)}
          </div>
        )}
      </div>
      
      {isUser && (
        <div className="chat-bubble__avatar chat-bubble__avatar--user">
          <FiUser />
        </div>
      )}
      
      {/* Copy button for assistant messages */}
      {!isUser && message && (
        <button 
          className="chat-bubble__copy"
          onClick={handleCopy}
          title="Sao chép"
        >
          {copied ? <FiCheck /> : <FiCopy />}
        </button>
      )}
    </div>
  );
};

export default ChatBubble;
