import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { FiCheckCircle, FiHelpCircle, FiTarget, FiTrendingUp, FiArrowRight, FiActivity } from 'react-icons/fi';
import { useDashboardStore, useProfileStore } from '../stores';
import { toast } from '../ui/toast';
import {
  Page, Container, PageHeader, Card, StatCard, Tabs, Progress, Badge,
  EmptyState, Skeleton, Button, Modal, Input, Avatar,
} from '../ui';

const pct = (v) => `${Math.round((v || 0) * 100)}%`;
const SKILL_LABEL = { reading: 'Reading', listening: 'Listening', writing: 'Writing', speaking: 'Speaking' };

export default function Dashboard() {
  const { summary, loading, fetchSummary, saveTarget } = useDashboardStore();
  const profile = useProfileStore((s) => s.profile);
  const [tab, setTab] = useState('progress');
  const [targetOpen, setTargetOpen] = useState(false);
  const [targetBand, setTargetBand] = useState('7.0');

  useEffect(() => { fetchSummary(); }, [fetchSummary]);

  const stats = summary?.stats;
  const target = summary?.target;

  const onSaveTarget = async () => {
    try { await saveTarget(parseFloat(targetBand), null); setTargetOpen(false); toast.success('Đã lưu mục tiêu'); }
    catch { toast.error('Không thể lưu mục tiêu'); }
  };

  return (
    <Page>
      <Container className="py-8">
        <PageHeader
          title={`Chào, ${profile?.fullName || profile?.username || 'bạn'} 👋`}
          subtitle="Theo dõi tiến độ luyện thi IELTS của bạn"
          actions={<Button variant="outline" iconLeft={<FiTarget size={16} />} onClick={() => setTargetOpen(true)}>
            {target ? `Mục tiêu: ${target.targetBand}` : 'Đặt mục tiêu'}
          </Button>}
        />

        <div className="mt-5 grid grid-cols-2 gap-3 lg:grid-cols-4">
          {loading && !summary ? (
            Array.from({ length: 4 }).map((_, i) => <Card key={i}><Skeleton className="h-12 w-full" /></Card>)
          ) : (
            <>
              <StatCard icon={<FiCheckCircle />} label="Bài đã hoàn thành" value={stats?.testsCompleted ?? 0} />
              <StatCard icon={<FiHelpCircle />} label="Câu đã trả lời" value={stats?.questionsAnswered ?? 0} />
              <StatCard icon={<FiCheckCircle />} label="Câu đúng" value={stats?.correctAnswers ?? 0} />
              <StatCard icon={<FiTrendingUp />} label="Độ chính xác" value={pct(stats?.accuracy)} />
            </>
          )}
        </div>

        <div className="mt-6">
          <Tabs value={tab} onChange={setTab} items={[
            { value: 'progress', label: 'Tiến độ khóa học', icon: <FiTrendingUp size={15} /> },
            { value: 'skills', label: 'Phân tích kỹ năng', icon: <FiActivity size={15} /> },
            { value: 'activity', label: 'Hoạt động', icon: <FiActivity size={15} /> },
          ]} />
        </div>

        <div className="mt-5">
          {tab === 'progress' && (
            <CourseProgress items={summary?.courseProgress} loading={loading && !summary} />
          )}
          {tab === 'skills' && (
            <SkillAnalysis items={summary?.perSkillAccuracy} loading={loading && !summary} />
          )}
          {tab === 'activity' && (
            <ActivityFeed items={summary?.recentActivity} loading={loading && !summary} />
          )}
        </div>
      </Container>

      <Modal open={targetOpen} onClose={() => setTargetOpen(false)} title="Đặt mục tiêu band" size="sm"
        footer={<><Button variant="ghost" onClick={() => setTargetOpen(false)}>Huỷ</Button><Button onClick={onSaveTarget}>Lưu</Button></>}>
        <Input label="Band mục tiêu" type="number" step="0.5" min="0" max="9" value={targetBand} onChange={(e) => setTargetBand(e.target.value)} />
      </Modal>
    </Page>
  );
}

function CourseProgress({ items, loading }) {
  if (loading) return <div className="flex flex-col gap-3">{Array.from({ length: 4 }).map((_, i) => <Card key={i}><Skeleton className="h-10 w-full" /></Card>)}</div>;
  if (!items?.length) return <EmptyState icon="📈" title="Chưa có dữ liệu" description="Hoàn thành một bài thi để xem tiến độ của bạn." action={<Link to="/courses"><Button>Bắt đầu luyện tập</Button></Link>} />;
  return (
    <Card padded={false} className="divide-y divide-line overflow-hidden">
      {items.map((c, i) => (
        <div key={i} className="flex flex-wrap items-center gap-3 px-4 py-3">
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2">
              <span className="text-base font-semibold text-ink">{c.examSource} · Test {c.testNumber}</span>
              <Badge size="sm" variant="neutral">{SKILL_LABEL[c.skill] || c.skill}</Badge>
            </div>
            <div className="mt-0.5 text-sm text-muted">{c.latestCorrect}/{c.latestAnswered} đúng · {c.attempts} lượt</div>
          </div>
          {c.band != null && <div className="text-center"><div className="text-xs text-muted">Band</div><div className="text-lg font-bold text-brand-700">{c.band}</div></div>}
          <div className="w-28"><Progress value={(c.completionPct || 0) * 100} /></div>
        </div>
      ))}
    </Card>
  );
}

function SkillAnalysis({ items, loading }) {
  if (loading) return <Card><Skeleton className="h-24 w-full" /></Card>;
  if (!items?.length) return <EmptyState icon="🎯" title="Chưa có phân tích" description="Làm bài để xem độ chính xác theo từng kỹ năng." />;
  const tint = { reading: 'bg-info', listening: 'bg-brand-600', writing: 'bg-warning', speaking: 'bg-success' };
  return (
    <Card className="flex flex-col gap-4">
      {items.map((s) => (
        <div key={s.skill} className="flex flex-col gap-1.5">
          <div className="flex items-center justify-between text-base">
            <span className="font-semibold text-ink-2">{SKILL_LABEL[s.skill] || s.skill}</span>
            <span className="text-muted">{s.correct}/{s.answered} · {pct(s.accuracy)}</span>
          </div>
          <Progress value={(s.accuracy || 0) * 100} barClassName={tint[s.skill] || 'bg-brand-600'} />
        </div>
      ))}
    </Card>
  );
}

function ActivityFeed({ items, loading }) {
  if (loading) return <Card><Skeleton className="h-24 w-full" /></Card>;
  if (!items?.length) return <EmptyState icon="🗂️" title="Chưa có hoạt động" description="Hoạt động gần đây của bạn sẽ hiển thị ở đây." />;
  return (
    <Card padded={false} className="divide-y divide-line overflow-hidden">
      {items.map((a, i) => (
        <div key={i} className="flex items-start gap-3 px-4 py-3">
          <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-soft text-brand-600"><FiActivity size={15} /></div>
          <div className="min-w-0 flex-1">
            <div className="text-base font-semibold text-ink">{a.title}</div>
            {a.description && <div className="text-sm text-muted">{a.description}</div>}
          </div>
          <div className="shrink-0 text-xs text-faint">{a.createdAt ? new Date(a.createdAt).toLocaleDateString('vi-VN') : ''}</div>
        </div>
      ))}
    </Card>
  );
}
