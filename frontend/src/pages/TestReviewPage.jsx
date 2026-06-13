import { useEffect, useMemo, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  FiArrowLeft, FiRefreshCw, FiCheckCircle, FiXCircle, FiHelpCircle, FiRotateCw, FiChevronDown,
} from 'react-icons/fi';
import { attemptApi } from '../lib/api';
import { getApiError } from '../lib/api/client';
import { sanitizeHtml } from '../utils/sanitize';
import { Button, Badge, Card, Spinner, Alert, EmptyState } from '../ui';
import { toast } from '../ui/toast';

/* ── JsonNode render helpers ─────────────────────────────── */
function pickText(node) {
  if (node == null) return '';
  if (typeof node === 'string') return node;
  if (typeof node !== 'object') return String(node);
  for (const k of ['text', 'statement', 'question', 'sentence', 'incomplete_sentence', 'prompt', 'item', 'paragraph', 'heading', 'content']) {
    if (typeof node[k] === 'string') return node[k];
  }
  return '';
}
function renderAnswer(v) {
  if (v == null || v === '') return '—';
  if (Array.isArray(v)) return v.filter(Boolean).join('  /  ') || '—';
  if (typeof v === 'object') return Object.values(v).filter((x) => typeof x === 'string').join('  /  ') || '—';
  return String(v);
}
function explanationText(e) {
  if (e == null) return '';
  if (typeof e === 'string') return e;
  if (typeof e === 'object') {
    return [e.explanation, e.detail, e.reason, e.quote, e.strategy, e.text].filter((x) => typeof x === 'string').join('\n\n');
  }
  return '';
}

const fmtDuration = (s) => {
  if (s == null) return 'N/A';
  const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), sec = s % 60;
  const pad = (n) => String(n).padStart(2, '0');
  return h > 0 ? `${pad(h)}:${pad(m)}:${pad(sec)}` : `${pad(m)}:${pad(sec)}`;
};

function QuestionCard({ q }) {
  const [open, setOpen] = useState(false);
  const correct = q.isCorrect === true;
  const unanswered = q.userAnswer == null || q.userAnswer === '';
  const status = correct ? 'correct' : unanswered ? 'blank' : 'wrong';
  const exp = explanationText(q.explanation);
  const content = pickText(q.questionContent);

  const ring = correct ? 'border-l-success' : unanswered ? 'border-l-faint' : 'border-l-danger';
  const icon = correct ? <FiCheckCircle className="text-success" size={18} />
    : unanswered ? <FiHelpCircle className="text-faint" size={18} />
      : <FiXCircle className="text-danger" size={18} />;

  return (
    <Card padded={false} className={`border-l-4 ${ring}`}>
      <div className="flex items-start gap-3 p-4">
        <span className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-lg bg-surface-2 text-sm font-bold text-ink">{q.questionNumber}</span>
        <div className="min-w-0 flex-1">
          {content && <div className="cr-prose text-base text-ink" dangerouslySetInnerHTML={{ __html: sanitizeHtml(content) }} />}
          <div className="mt-3 grid gap-2 sm:grid-cols-2">
            <div className={`rounded-lg border px-3 py-2 ${status === 'correct' ? 'border-success-soft bg-success-soft/40' : status === 'wrong' ? 'border-danger-soft bg-danger-soft/40' : 'border-line bg-surface-2'}`}>
              <div className="text-xs font-semibold uppercase tracking-wide text-muted">Câu trả lời của bạn</div>
              <div className="mt-0.5 text-base font-medium text-ink">{renderAnswer(q.userAnswer)}</div>
            </div>
            <div className="rounded-lg border border-success-soft bg-success-soft/40 px-3 py-2">
              <div className="text-xs font-semibold uppercase tracking-wide text-muted">Đáp án đúng</div>
              <div className="mt-0.5 text-base font-medium text-success">{renderAnswer(q.correctAnswer)}</div>
            </div>
          </div>
          {exp && (
            <div className="mt-2">
              <button onClick={() => setOpen((o) => !o)} className="flex items-center gap-1.5 text-sm font-semibold text-brand-600 hover:text-brand-700">
                <FiChevronDown size={15} className={`transition-transform ${open ? 'rotate-180' : ''}`} />Giải thích
              </button>
              {open && <p className="mt-1.5 whitespace-pre-line rounded-lg bg-surface-2 px-3 py-2 text-base text-ink-2">{exp}</p>}
            </div>
          )}
        </div>
        <div className="shrink-0">{icon}</div>
      </div>
    </Card>
  );
}

export default function TestReviewPage() {
  const { attemptId } = useParams();
  const navigate = useNavigate();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [regrading, setRegrading] = useState(false);

  const load = async () => {
    setLoading(true); setError(null);
    try { setData(await attemptApi.review(attemptId)); }
    catch (err) { setError(getApiError(err).message || 'Không thể tải dữ liệu xem lại bài làm.'); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); /* eslint-disable-next-line */ }, [attemptId]);

  const regrade = async () => {
    if (regrading) return;
    setRegrading(true);
    try { await attemptApi.regrade(attemptId); await load(); toast.success('Đã chấm lại bài làm'); }
    catch { toast.error('Không thể chấm lại bài làm'); }
    finally { setRegrading(false); }
  };

  const meta = useMemo(() => {
    if (!data) return {};
    return {
      title: [data.examSource?.toUpperCase(), data.testNumber && `Test ${data.testNumber}`, data.skill && data.skill[0].toUpperCase() + data.skill.slice(1)].filter(Boolean).join(' · '),
      date: data.completedAt ? new Date(data.completedAt).toLocaleDateString('vi-VN') : 'N/A',
      accuracy: data.totalQuestions ? Math.round(((data.score || 0) / data.totalQuestions) * 100) : 0,
    };
  }, [data]);

  if (loading) return <div className="flex min-h-screen items-center justify-center bg-page"><Spinner size="lg" /></div>;
  if (error) return <div className="mx-auto max-w-2xl p-6"><Alert variant="danger" title="Lỗi">{error}</Alert><div className="mt-4"><Button variant="outline" iconLeft={<FiArrowLeft size={15} />} onClick={() => navigate('/dashboard')}>Về trang chính</Button></div></div>;
  if (!data) return null;

  return (
    <div className="min-h-screen bg-page">
      {/* Header */}
      <div className="sticky top-0 z-20 border-b border-line bg-surface/90 backdrop-blur">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center gap-3 px-4 py-3 sm:px-6">
          <Button variant="ghost" size="sm" iconLeft={<FiArrowLeft size={15} />} onClick={() => navigate('/dashboard')}>Trang chính</Button>
          <div className="min-w-0 flex-1">
            <h1 className="truncate text-lg font-bold text-ink">{meta.title}</h1>
          </div>
          {data.bandScore != null && <Badge variant="brand" size="md" className="text-sm">Band {data.bandScore}</Badge>}
          <Button variant="outline" size="sm" loading={regrading} iconLeft={<FiRotateCw size={15} />} onClick={regrade}>Chấm lại</Button>
          <Button size="sm" iconLeft={<FiRefreshCw size={15} />} onClick={() => navigate(`/test/${data.examSource}/${data.testNumber}/${data.skill}`, { state: { forceNew: true } })}>Làm lại</Button>
        </div>
      </div>

      <div className="mx-auto max-w-5xl px-4 py-6 sm:px-6">
        {/* Summary */}
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
          {[
            { label: 'Điểm', value: `${data.score ?? 0}/${data.totalQuestions}` },
            { label: 'Độ chính xác', value: `${meta.accuracy}%` },
            { label: 'Thời gian', value: fmtDuration(data.durationSeconds) },
            { label: 'Ngày làm', value: meta.date },
          ].map((s) => (
            <Card key={s.label} className="!p-4 text-center">
              <div className="text-2xl font-bold text-ink">{s.value}</div>
              <div className="mt-0.5 text-sm text-muted">{s.label}</div>
            </Card>
          ))}
        </div>

        {/* Section accuracy */}
        {data.sections?.length > 1 && (
          <div className="mt-4 flex flex-wrap gap-2">
            {data.sections.map((sec) => (
              <Badge key={sec.sectionId} variant="neutral" size="md">
                Part {sec.partNumber}: {sec.correctCount}/{sec.totalQuestions} ({Math.round((sec.accuracy || 0) * 100)}%)
              </Badge>
            ))}
          </div>
        )}

        {/* Questions */}
        <div className="mt-6 flex flex-col gap-3">
          {data.questions?.length
            ? data.questions.map((q, i) => (
              <motion.div key={q.questionId} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: Math.min(i * 0.02, 0.3) }}>
                <QuestionCard q={q} />
              </motion.div>
            ))
            : <EmptyState title="Không có câu hỏi" description="Bài làm này chưa có dữ liệu chi tiết." />}
        </div>
      </div>
    </div>
  );
}
