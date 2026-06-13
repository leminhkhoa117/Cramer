import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  FiClock, FiTrendingUp, FiPieChart, FiTarget, FiEdit3, FiCheckCircle,
  FiHelpCircle, FiAward, FiRefreshCw, FiEye, FiChevronDown, FiChevronUp,
  FiActivity, FiArrowRight,
} from 'react-icons/fi';
import {
  ResponsiveContainer, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Cell,
  LineChart, Line,
} from 'recharts';
import { useDashboardStore, useProfileStore } from '../stores';
import { toast } from '../ui/toast';
import {
  Page, Container, Card, Button, IconButton, Avatar, Badge, Progress,
  StatCard, EmptyState, Skeleton, Modal, Select, Alert, Pagination,
} from '../ui';
import { cn } from '../lib/cn';

const SKILL_LABEL = { reading: 'Reading', listening: 'Listening', writing: 'Writing', speaking: 'Speaking' };
const SKILL_VI = { reading: 'Đọc', listening: 'Nghe', writing: 'Viết', speaking: 'Nói' };
const SKILL_ORDER = ['reading', 'listening', 'writing', 'speaking'];
const SKILL_TINT = { reading: 'bg-info', listening: 'bg-brand-600', writing: 'bg-warning', speaking: 'bg-success' };
const STATUS_LABEL = {
  COMPLETED: 'Hoàn thành', IN_PROGRESS: 'Đang làm', GRADING: 'Đang chấm',
  ABANDONED: 'Đã huỷ', EXPIRED: 'Hết hạn', PENDING: 'Chờ xử lý',
};
const PAGE_SIZE = 6;
const BAND_OPTIONS = Array.from({ length: 11 }, (_, i) => (4 + i * 0.5).toFixed(1)); // 4.0 → 9.0

// Chart palette (recharts needs concrete colors, mirrors brand tokens).
const C = { brand: '#7c3aed', accent: '#6366f1', grid: '#ece7f6', axis: '#6b7280', surface: '#ffffff', ink: '#1f2937' };

const containerVar = { hidden: { opacity: 0 }, visible: { opacity: 1, transition: { staggerChildren: 0.06 } } };
const itemVar = { hidden: { y: 12, opacity: 0 }, visible: { y: 0, opacity: 1 } };

/** Accuracy / completion can arrive as 0..1 or 0..100 — normalise to 0..100. */
const normPct = (v) => {
  const n = Number(v);
  if (!Number.isFinite(n) || n <= 0) return 0;
  return Math.min(100, n <= 1 ? n * 100 : n);
};
const fmtPct = (v) => `${Math.round(normPct(v))}%`;
const fmtDate = (d) => {
  if (!d) return '—';
  const dt = new Date(d);
  return Number.isNaN(dt.getTime())
    ? '—'
    : dt.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
};
const isCompleted = (s) => String(s || '').toUpperCase() === 'COMPLETED';
const statusLabel = (s) => STATUS_LABEL[String(s || '').toUpperCase()] || s || '—';
const courseKey = (c) => `${c.examSource}-${c.testNumber}-${c.skill}`;
const retakePath = (c) =>
  c.skill === 'writing'
    ? `/test/writing/${c.examSource}/${c.testNumber}`
    : `/test/${c.examSource}/${c.testNumber}/${c.skill}`;
const reviewPath = (skill, attemptId) =>
  skill === 'writing' ? `/test/writing/review/${attemptId}` : `/test/review/${attemptId}`;

const NAV = [
  { value: 'courses', label: 'Lịch sử làm bài', icon: <FiClock size={16} /> },
  { value: 'progress', label: 'Biểu đồ tiến độ', icon: <FiTrendingUp size={16} /> },
  { value: 'analysis', label: 'Phân tích kỹ năng', icon: <FiPieChart size={16} /> },
];

export default function Dashboard() {
  const { summary, loading, error, fetchSummary, refreshSummary, courseHistory, saveTarget } = useDashboardStore();
  const profile = useProfileStore((s) => s.profile);
  const navigate = useNavigate();

  const [tab, setTab] = useState('courses');
  const [page, setPage] = useState(0);
  const [targetOpen, setTargetOpen] = useState(false);

  useEffect(() => { fetchSummary(); }, [fetchSummary]);
  useEffect(() => { setPage(0); }, [tab]);

  const stats = summary?.stats;
  const target = summary?.target;
  const hasTarget = target && target.targetBand != null && !Number.isNaN(Number(target.targetBand));
  const courseProgress = summary?.courseProgress ?? [];
  const perSkill = summary?.perSkillAccuracy ?? [];
  const recentActivity = summary?.recentActivity ?? [];
  const initialLoading = loading && !summary;

  const profName =
    summary?.profile?.fullName || profile?.fullName ||
    summary?.profile?.username || profile?.username || 'Người dùng';
  const profUser = summary?.profile?.username || profile?.username;
  const profAvatar = summary?.profile?.avatarUrl || profile?.avatarUrl;

  const sidebar = (
    <Card padded={false} className="overflow-hidden">
      <div className="relative h-24 gradient-brand">
        {profile?.heroBackgroundUrl && (
          <img src={profile.heroBackgroundUrl} alt="" className="h-full w-full object-cover" />
        )}
      </div>
      <div className="flex flex-col items-center px-4 pb-4 -mt-10">
        <Avatar src={profAvatar} name={profName} size="xl" className="ring-4 ring-surface" />
        <h2 className="mt-3 text-center text-lg font-bold text-ink">{profName}</h2>
        {profUser && <p className="text-sm text-muted">@{profUser}</p>}
      </div>

      <nav className="border-t border-line p-2">
        {NAV.map((t) => (
          <button
            key={t.value}
            onClick={() => setTab(t.value)}
            className={cn(
              'flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-base font-medium transition-colors',
              tab === t.value ? 'bg-brand-soft text-brand-700' : 'text-ink-2 hover:bg-surface-2'
            )}
          >
            {t.icon}{t.label}
          </button>
        ))}
      </nav>

      <div className="border-t border-line p-4">
        <div className="flex items-center justify-between">
          <span className="flex items-center gap-1.5 text-sm font-semibold text-ink-2">
            <FiTarget size={15} /> Mục tiêu
          </span>
          {hasTarget && (
            <IconButton size="sm" variant="ghost" aria-label="Chỉnh sửa mục tiêu" onClick={() => setTargetOpen(true)}>
              <FiEdit3 size={14} />
            </IconButton>
          )}
        </div>

        {hasTarget ? (
          <div className="mt-3 rounded-xl gradient-brand p-4 text-white">
            <div className="text-xs font-semibold uppercase tracking-wide text-white/80">Band mục tiêu</div>
            <div className="mt-0.5 text-3xl font-bold leading-none">{Number(target.targetBand).toFixed(1)}</div>
            <div className="mt-2 text-xs text-white/85">
              {target.targetSkill ? `Kỹ năng ${SKILL_LABEL[target.targetSkill] || target.targetSkill}` : 'Mục tiêu tổng thể'}
            </div>
          </div>
        ) : (
          <div className="mt-3">
            <p className="text-sm text-muted">Chưa đặt mục tiêu cho hành trình của bạn.</p>
            <Button size="sm" variant="secondary" fullWidth className="mt-3" iconLeft={<FiTarget size={14} />} onClick={() => setTargetOpen(true)}>
              Đặt mục tiêu
            </Button>
          </div>
        )}
      </div>
    </Card>
  );

  return (
    <Page>
      <Container className="py-8">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-start">
          <aside className="w-full lg:w-72 lg:shrink-0">{sidebar}</aside>

          <div className="min-w-0 flex-1">
            {error && !summary && (
              <Alert variant="danger" title="Không thể tải bảng điều khiển" className="mb-5">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <span>{error}</span>
                  <Button size="sm" variant="outline" iconLeft={<FiRefreshCw size={14} />} onClick={refreshSummary}>
                    Thử lại
                  </Button>
                </div>
              </Alert>
            )}

            <motion.div
              variants={containerVar}
              initial="hidden"
              animate="visible"
              className="grid grid-cols-2 gap-4 lg:grid-cols-4"
            >
              {initialLoading ? (
                Array.from({ length: 4 }).map((_, i) => (
                  <Card key={i}><Skeleton className="h-12 w-full" /></Card>
                ))
              ) : (
                <>
                  <motion.div variants={itemVar}>
                    <StatCard icon={<FiCheckCircle />} label="Bài đã hoàn thành" value={stats?.testsCompleted ?? 0} />
                  </motion.div>
                  <motion.div variants={itemVar}>
                    <StatCard icon={<FiHelpCircle />} label="Câu đã trả lời" value={stats?.questionsAnswered ?? 0} />
                  </motion.div>
                  <motion.div variants={itemVar}>
                    <StatCard icon={<FiAward />} label="Câu đúng" value={stats?.correctAnswers ?? 0} />
                  </motion.div>
                  <motion.div variants={itemVar}>
                    <StatCard icon={<FiTrendingUp />} label="Độ chính xác" value={fmtPct(stats?.accuracy)} />
                  </motion.div>
                </>
              )}
            </motion.div>

            <motion.div
              key={tab}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.25 }}
              className="mt-5"
            >
              {tab === 'courses' && (
                <HistoryTab
                  items={courseProgress}
                  recentActivity={recentActivity}
                  loading={initialLoading}
                  page={page}
                  setPage={setPage}
                  courseHistory={courseHistory}
                  navigate={navigate}
                />
              )}
              {tab === 'progress' && (
                <ProgressTab perSkill={perSkill} courseProgress={courseProgress} loading={initialLoading} />
              )}
              {tab === 'analysis' && (
                <AnalysisTab perSkill={perSkill} loading={initialLoading} />
              )}
            </motion.div>
          </div>
        </div>
      </Container>

      <TargetModal open={targetOpen} onClose={() => setTargetOpen(false)} target={target} saveTarget={saveTarget} />
    </Page>
  );
}

function HistoryTab({ items, recentActivity, loading, page, setPage, courseHistory, navigate }) {
  if (loading) {
    return (
      <div className="flex flex-col gap-3">
        {Array.from({ length: 4 }).map((_, i) => <Card key={i}><Skeleton className="h-20 w-full" /></Card>)}
      </div>
    );
  }

  const total = items.length;
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const safePage = Math.min(page, totalPages - 1);
  const visible = items.slice(safePage * PAGE_SIZE, safePage * PAGE_SIZE + PAGE_SIZE);

  return (
    <div className="flex flex-col gap-5">
      <Card padded>
        <Card.Header>
          <Card.Title>Lịch sử làm bài</Card.Title>
          {total > 0 && <Badge variant="neutral">{total} khóa học</Badge>}
        </Card.Header>

        {total === 0 ? (
          <EmptyState
            icon={<FiClock />}
            title="Chưa có dữ liệu luyện tập"
            description="Hoàn thành một bài thi để theo dõi tiến độ và xem lại bài làm của bạn."
            action={<Button iconLeft={<FiArrowRight size={15} />} onClick={() => navigate('/courses')}>Bắt đầu luyện tập</Button>}
          />
        ) : (
          <div className="flex flex-col gap-3">
            {visible.map((c) => (
              <CourseRow key={courseKey(c)} course={c} courseHistory={courseHistory} navigate={navigate} />
            ))}
            {totalPages > 1 && (
              <div className="pt-2">
                <Pagination page={safePage} totalPages={totalPages} onPageChange={setPage} />
              </div>
            )}
          </div>
        )}
      </Card>

      {recentActivity.length > 0 && (
        <Card padded>
          <Card.Header><Card.Title>Hoạt động gần đây</Card.Title></Card.Header>
          <div className="flex flex-col">
            {recentActivity.slice(0, 6).map((a, i) => (
              <div key={i} className="flex items-start gap-3 border-b border-line py-2.5 last:border-0">
                <span className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-soft text-brand-600">
                  <FiActivity size={15} />
                </span>
                <div className="min-w-0 flex-1">
                  <div className="text-base font-semibold text-ink">{a.title}</div>
                  {a.description && <div className="text-sm text-muted">{a.description}</div>}
                </div>
                <span className="shrink-0 text-xs text-faint">{fmtDate(a.createdAt)}</span>
              </div>
            ))}
          </div>
        </Card>
      )}
    </div>
  );
}

function CourseRow({ course: c, courseHistory, navigate }) {
  const [expanded, setExpanded] = useState(false);
  const [history, setHistory] = useState(null);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [reviewing, setReviewing] = useState(false);

  const loadHistory = async () => {
    setLoadingHistory(true);
    try {
      const data = await courseHistory({ examSource: c.examSource, testNumber: c.testNumber, skill: c.skill });
      const list = Array.isArray(data) ? data : [];
      setHistory(list);
      return list;
    } finally {
      setLoadingHistory(false);
    }
  };

  const toggleExpand = async () => {
    const next = !expanded;
    setExpanded(next);
    if (next && history === null) await loadHistory();
  };

  const onReview = async () => {
    setReviewing(true);
    try {
      const list = history ?? (await loadHistory());
      const pick = list
        .filter((a) => isCompleted(a.status))
        .sort((a, b) => new Date(b.completedAt || 0) - new Date(a.completedAt || 0))[0];
      if (!pick) { toast.error('Chưa có lần làm hoàn thành để xem lại'); return; }
      navigate(reviewPath(c.skill, pick.attemptId));
    } catch {
      toast.error('Không thể mở bài xem lại');
    } finally {
      setReviewing(false);
    }
  };

  const onRetake = () => navigate(retakePath(c), { state: { forceNew: true } });

  return (
    <div className="rounded-xl border border-line bg-surface p-4 transition-colors hover:border-brand-200">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-base font-bold text-ink">{String(c.examSource).toUpperCase()} · Test {c.testNumber}</span>
            <Badge size="sm" variant="neutral">{SKILL_LABEL[c.skill] || c.skill}</Badge>
            {c.band != null && <Badge size="sm" variant="brand">Band {Number(c.band).toFixed(1)}</Badge>}
          </div>
          <div className="mt-1 text-sm text-muted">
            {c.latestCorrect ?? 0}/{c.latestAnswered ?? 0} câu đúng · {c.attempts ?? 0} lượt làm · {fmtDate(c.latestAt)}
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button size="sm" variant="primary" iconLeft={<FiRefreshCw size={14} />} onClick={onRetake}>Làm lại</Button>
          <Button size="sm" variant="outline" loading={reviewing} iconLeft={<FiEye size={14} />} onClick={onReview}>Xem lại</Button>
        </div>
      </div>

      <div className="mt-3">
        <Progress value={normPct(c.completionPct)} label="Hoàn thành" />
      </div>

      <button
        type="button"
        onClick={toggleExpand}
        className="mt-3 inline-flex items-center gap-1 text-sm font-semibold text-brand-600 transition-colors hover:text-brand-700"
      >
        {expanded ? <FiChevronUp size={14} /> : <FiChevronDown size={14} />}
        {expanded ? 'Ẩn chi tiết' : 'Chi tiết lần làm'}
      </button>

      {expanded && (
        <div className="mt-3 rounded-lg border border-line bg-surface-2 p-3">
          {loadingHistory ? (
            <Skeleton className="h-16 w-full" />
          ) : !history?.length ? (
            <p className="text-sm text-muted">Chưa có lần làm nào được ghi nhận.</p>
          ) : (
            <div className="flex flex-col divide-y divide-line">
              {history.map((a) => (
                <div key={a.attemptId} className="flex flex-wrap items-center justify-between gap-2 py-2 first:pt-0 last:pb-0">
                  <div className="min-w-0">
                    <div className="flex items-center gap-2 text-sm">
                      <span className="font-semibold text-ink-2">#{a.attemptId}</span>
                      <Badge size="sm" variant={isCompleted(a.status) ? 'success' : 'warning'}>{statusLabel(a.status)}</Badge>
                      {a.score != null && <span className="text-muted">Điểm {a.score}</span>}
                    </div>
                    <div className="mt-0.5 text-xs text-faint">
                      {a.correct ?? 0}/{a.answered ?? 0} đúng · {fmtDate(a.completedAt || a.startedAt)}
                    </div>
                  </div>
                  {isCompleted(a.status) && (
                    <Button size="sm" variant="ghost" iconLeft={<FiEye size={13} />} onClick={() => navigate(reviewPath(c.skill, a.attemptId))}>
                      Xem lại
                    </Button>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function ProgressTab({ perSkill, courseProgress, loading }) {
  if (loading) return <Card><Skeleton className="h-64 w-full" /></Card>;

  const skillData = perSkill.map((s) => ({ name: SKILL_LABEL[s.skill] || s.skill, value: Math.round(normPct(s.accuracy)) }));
  const bandData = courseProgress
    .filter((c) => c.band != null)
    .slice(-12)
    .map((c) => ({ name: `${String(c.examSource).toUpperCase()} T${c.testNumber}`, band: Number(c.band) }));

  if (!skillData.length && !bandData.length) {
    return (
      <EmptyState
        icon={<FiTrendingUp />}
        title="Chưa có biểu đồ"
        description="Hoàn thành một vài bài thi để xem biểu đồ tiến độ của bạn."
      />
    );
  }

  const tooltipStyle = {
    borderRadius: 12,
    border: `1px solid ${C.grid}`,
    background: C.surface,
    boxShadow: '0 8px 24px rgba(15,23,42,0.10)',
    fontSize: 12,
  };

  return (
    <div className="flex flex-col gap-5">
      {skillData.length > 0 && (
        <Card padded>
          <Card.Header><Card.Title>Độ chính xác theo kỹ năng</Card.Title></Card.Header>
          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={skillData} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke={C.grid} vertical={false} />
                <XAxis dataKey="name" tick={{ fontSize: 12, fill: C.axis }} tickLine={false} axisLine={{ stroke: C.grid }} />
                <YAxis domain={[0, 100]} unit="%" tick={{ fontSize: 12, fill: C.axis }} tickLine={false} axisLine={false} />
                <Tooltip
                  cursor={{ fill: 'rgba(124,58,237,0.06)' }}
                  contentStyle={tooltipStyle}
                  labelStyle={{ color: C.ink, fontWeight: 700 }}
                  formatter={(v) => [`${v}%`, 'Độ chính xác']}
                />
                <Bar dataKey="value" name="Độ chính xác" radius={[6, 6, 0, 0]} maxBarSize={56}>
                  {skillData.map((_, i) => <Cell key={i} fill={i % 2 ? C.accent : C.brand} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </Card>
      )}

      {bandData.length > 0 && (
        <Card padded>
          <Card.Header><Card.Title>Band theo bài học gần đây</Card.Title></Card.Header>
          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={bandData} margin={{ top: 8, right: 12, left: -16, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke={C.grid} vertical={false} />
                <XAxis dataKey="name" tick={{ fontSize: 11, fill: C.axis }} tickLine={false} axisLine={{ stroke: C.grid }} />
                <YAxis domain={[0, 9]} tick={{ fontSize: 12, fill: C.axis }} tickLine={false} axisLine={false} />
                <Tooltip
                  cursor={{ stroke: C.grid }}
                  contentStyle={tooltipStyle}
                  labelStyle={{ color: C.ink, fontWeight: 700 }}
                  formatter={(v) => [Number(v).toFixed(1), 'Band']}
                />
                <Line type="monotone" dataKey="band" name="Band" stroke={C.brand} strokeWidth={2.5} dot={{ r: 3, fill: C.brand }} activeDot={{ r: 5 }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </Card>
      )}
    </div>
  );
}

function AnalysisTab({ perSkill, loading }) {
  if (loading) return <Card><Skeleton className="h-24 w-full" /></Card>;
  if (!perSkill.length) {
    return (
      <EmptyState
        icon={<FiPieChart />}
        title="Chưa có phân tích"
        description="Làm bài để xem độ chính xác theo từng kỹ năng."
      />
    );
  }

  const ordered = [...perSkill].sort(
    (a, b) => SKILL_ORDER.indexOf(a.skill) - SKILL_ORDER.indexOf(b.skill)
  );

  return (
    <div className="grid gap-4 sm:grid-cols-2">
      {ordered.map((s) => (
        <Card key={s.skill} padded>
          <div className="flex items-center justify-between">
            <span className="text-base font-bold text-ink">{SKILL_LABEL[s.skill] || s.skill}</span>
            <Badge variant="brand">{fmtPct(s.accuracy)}</Badge>
          </div>
          <p className="mt-0.5 text-sm text-muted">{SKILL_VI[s.skill] || ''}</p>
          <div className="mt-3">
            <Progress value={normPct(s.accuracy)} barClassName={SKILL_TINT[s.skill] || 'bg-brand-600'} />
          </div>
          <div className="mt-2 text-sm text-muted">
            <span className="font-semibold text-ink-2">{s.correct ?? 0}</span>/{s.answered ?? 0} câu đúng
          </div>
        </Card>
      ))}
    </div>
  );
}

function TargetModal({ open, onClose, target, saveTarget }) {
  const [band, setBand] = useState('7.0');
  const [skill, setSkill] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!open) return;
    setBand(target?.targetBand != null ? Number(target.targetBand).toFixed(1) : '7.0');
    setSkill(target?.targetSkill || '');
  }, [open, target]);

  const onSave = async () => {
    setSaving(true);
    try {
      await saveTarget(parseFloat(band), skill || null);
      toast.success('Đã lưu mục tiêu');
      onClose();
    } catch {
      toast.error('Không thể lưu mục tiêu');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Mục tiêu band"
      size="sm"
      footer={
        <>
          <Button variant="ghost" onClick={onClose}>Huỷ</Button>
          <Button loading={saving} onClick={onSave}>Lưu mục tiêu</Button>
        </>
      }
    >
      <div className="flex flex-col gap-4">
        <Select label="Kỹ năng" hint="Để trống cho mục tiêu tổng thể" value={skill} onChange={(e) => setSkill(e.target.value)}>
          <option value="">Tổng thể</option>
          {SKILL_ORDER.map((s) => (
            <option key={s} value={s}>{SKILL_LABEL[s]} — {SKILL_VI[s]}</option>
          ))}
        </Select>
        <Select label="Band mục tiêu" value={band} onChange={(e) => setBand(e.target.value)}>
          {BAND_OPTIONS.map((b) => <option key={b} value={b}>{b}</option>)}
        </Select>
      </div>
    </Modal>
  );
}
