import { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { FiMessageCircle, FiX, FiSend } from 'react-icons/fi';
import { useAuthStore, useUserStatsStore } from '../stores';
import { chatApi, getApiError } from '../lib/api';
import { IconButton, Spinner } from '../ui';
import { cn } from '../lib/cn';

const HIDDEN_PATHS = ['/login'];
const WELCOME = {
  id: 'welcome',
  role: 'assistant',
  content: 'Xin chào! 👋 Mình là trợ lý Cramer. Bạn cần giúp gì về IELTS hôm nay?',
};

/** Floating AI assistant (SPEC-F16): tier + Lúa balance pill that expands into a chat. */
export default function FloatingAssistant() {
  const navigate = useNavigate();
  const location = useLocation();
  const user = useAuthStore((s) => s.user);
  const credits = useUserStatsStore((s) => s.credits);
  const chat = useUserStatsStore((s) => s.chat);
  const fetchUserStats = useUserStatsStore((s) => s.fetchUserStats);
  const refreshChat = useUserStatsStore((s) => s.refreshChat);
  const getTierEmoji = useUserStatsStore((s) => s.getTierEmoji);

  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([WELCOME]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const scrollRef = useRef(null);
  const inputRef = useRef(null);

  const isTestPage =
    /^\/test\/\w+\/\d+\/\w+$/.test(location.pathname) ||
    /^\/test\/writing\/(?!review)\w+\/\d+$/.test(location.pathname);
  const shouldHide = !user || HIDDEN_PATHS.includes(location.pathname) || isTestPage;

  useEffect(() => { if (user) fetchUserStats(); }, [user, fetchUserStats]);
  useEffect(() => { if (scrollRef.current) scrollRef.current.scrollTop = scrollRef.current.scrollHeight; }, [messages]);
  useEffect(() => { if (open) setTimeout(() => inputRef.current?.focus(), 250); }, [open]);

  if (shouldHide) return null;

  const remaining = chat?.remaining ?? 0;
  const unlimited = chat?.unlimited || remaining < 0;

  const send = async () => {
    const text = input.trim();
    if (!text || loading) return;
    if (!unlimited && remaining <= 0) {
      setError('Bạn đã hết lượt hỏi tháng này. Nâng cấp gói để có thêm lượt!');
      return;
    }
    setMessages((m) => [...m, { id: `u-${Date.now()}`, role: 'user', content: text }]);
    setInput('');
    setLoading(true);
    setError(null);
    try {
      const res = await chatApi.send(text);
      setMessages((m) => [...m, { id: `a-${Date.now()}`, role: 'assistant', content: res.reply || '…' }]);
      refreshChat();
    } catch (err) {
      const e = getApiError(err);
      setError(e.blockType === 'CHAT_LIMIT' ? 'Bạn đã hết lượt hỏi tháng này.' : e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed bottom-4 right-4" style={{ zIndex: 'var(--z-drawer)' }}>
      {open ? (
        <div className="flex h-[min(560px,calc(100vh-2rem))] w-[min(380px,calc(100vw-2rem))] flex-col overflow-hidden rounded-2xl border border-line bg-surface shadow-xl animate-[cr-slide-up_0.2s_ease-out]">
          <div className="flex items-center justify-between gap-2 gradient-brand px-4 py-3 text-white">
            <div className="flex items-center gap-2">
              <span className="text-xl">{getTierEmoji()}</span>
              <div className="leading-tight">
                <div className="text-base font-bold">Trợ lý Cramer</div>
                <div className="text-xs text-white/80">
                  {unlimited ? 'Không giới hạn' : `Còn ${remaining} lượt hỏi`} · 🌾 {credits?.balance ?? 0}
                </div>
              </div>
            </div>
            <IconButton aria-label="Đóng" size="sm" onClick={() => setOpen(false)} className="text-white hover:bg-white/15">
              <FiX size={18} />
            </IconButton>
          </div>

          <div ref={scrollRef} className="flex-1 space-y-3 overflow-y-auto cr-scroll bg-surface-2 p-3">
            {messages.map((m) => (
              <div key={m.id} className={cn('flex', m.role === 'user' ? 'justify-end' : 'justify-start')}>
                <div
                  className={cn(
                    'max-w-[80%] rounded-2xl px-3 py-2 text-base',
                    m.role === 'user' ? 'bg-brand-600 text-white rounded-br-sm' : 'bg-surface text-ink-2 border border-line rounded-bl-sm'
                  )}
                >
                  {m.content}
                </div>
              </div>
            ))}
            {loading && (
              <div className="flex justify-start">
                <div className="rounded-2xl rounded-bl-sm border border-line bg-surface px-3 py-2">
                  <Spinner size="sm" className="text-brand-500" />
                </div>
              </div>
            )}
          </div>

          {error && (
            <div className="bg-danger-soft px-3 py-2 text-sm text-danger">
              {error}{' '}
              <button className="font-bold underline" onClick={() => navigate('/subscription')}>Nâng cấp</button>
            </div>
          )}

          <div className="flex items-center gap-2 border-t border-line p-2.5">
            <input
              ref={inputRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && send()}
              placeholder="Hỏi mình bất cứ điều gì…"
              className="h-9 flex-1 rounded-lg border border-line bg-surface px-3 text-base text-ink placeholder:text-faint focus-visible:outline-none focus:border-brand-400"
            />
            <IconButton aria-label="Gửi" variant="primary" onClick={send} disabled={loading || !input.trim()}>
              <FiSend size={16} />
            </IconButton>
          </div>
        </div>
      ) : (
        <button
          onClick={() => setOpen(true)}
          className="flex items-center gap-2 rounded-full gradient-brand py-2 pl-2 pr-4 text-white shadow-lg transition-transform hover:scale-[1.03]"
        >
          <span className="flex h-8 w-8 items-center justify-center rounded-full bg-white/20 text-lg">{getTierEmoji()}</span>
          <span className="text-base font-bold">Cramer</span>
          <span className="rounded-full bg-white/20 px-2 py-0.5 text-xs font-bold">🌾 {credits?.balance ?? 0}</span>
        </button>
      )}
    </div>
  );
}
