import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { FiArrowLeft, FiBookOpen, FiHeadphones, FiEdit3, FiArrowRight } from 'react-icons/fi';
import { useCourseStore } from '../stores';
import { Page, Container, Card, Badge, EmptyState, Skeleton } from '../ui';

const formatCourseName = (s) => (s?.toLowerCase().startsWith('cam') ? `IELTS Cambridge ${s.substring(3)}` : s);

const SKILLS = [
  { name: 'Reading', key: 'reading', icon: <FiBookOpen size={18} />, meta: '60 phút · 40 câu', tint: 'bg-info-soft text-info' },
  { name: 'Listening', key: 'listening', icon: <FiHeadphones size={18} />, meta: '30 phút · 40 câu', tint: 'bg-brand-soft text-brand-600' },
  { name: 'Writing', key: 'writing', icon: <FiEdit3 size={18} />, meta: '60 phút · 2 phần', tint: 'bg-warning-soft text-warning' },
];

export default function CourseDetailPage() {
  const { courseName } = useParams();
  const { courseTests, fetchCourseTests, fetchCourseDetails, loading, error } = useCourseStore();
  const [displayName, setDisplayName] = useState(null);
  const [ready, setReady] = useState(false);

  const tests = courseTests[courseName] || [];

  useEffect(() => {
    let active = true;
    (async () => {
      const details = await fetchCourseDetails(courseName);
      if (active && details?.name) setDisplayName(details.name);
      await fetchCourseTests(courseName);
      if (active) setReady(true);
    })();
    return () => { active = false; };
  }, [courseName, fetchCourseTests, fetchCourseDetails]);

  const title = displayName || formatCourseName(courseName);

  return (
    <Page>
      {/* Banner */}
      <div className="gradient-brand text-white">
        <Container className="py-10">
          <Link to="/courses" className="inline-flex items-center gap-1.5 text-base font-semibold text-white/85 hover:text-white">
            <FiArrowLeft size={16} /> Quay lại danh sách
          </Link>
          <h1 className="mt-3 text-3xl font-bold">{title}</h1>
          <p className="mt-1 max-w-2xl text-md text-white/85">
            Bộ đề thi chính thức với 3 kỹ năng: Reading, Listening và Writing.
          </p>
        </Container>
      </div>

      <Container className="py-8">
        {error && <p className="rounded-lg bg-danger-soft px-4 py-3 text-base text-danger">{error}</p>}

        {!ready && loading ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, i) => <Card key={i}><Skeleton className="h-6 w-24" /><Skeleton className="mt-3 h-24 w-full" /></Card>)}
          </div>
        ) : tests.length === 0 ? (
          <EmptyState icon="📭" title="Chưa có bài test" description="Bộ đề này hiện chưa có bài test nào." />
        ) : (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {tests.map((testNumber) => (
              <Card key={testNumber} padded>
                <div className="mb-3 flex items-center justify-between">
                  <h2 className="text-lg font-bold text-ink">Test {testNumber}</h2>
                  <Badge variant="brand">Full Test</Badge>
                </div>
                <div className="flex flex-col gap-2">
                  {SKILLS.map((skill) => (
                    <Link
                      key={skill.key}
                      to={`/test/${courseName}/${testNumber}/${skill.key}`}
                      className="group flex items-center gap-3 rounded-lg border border-line p-2.5 transition-colors hover:border-brand-300 hover:bg-surface-2"
                    >
                      <span className={`flex h-9 w-9 items-center justify-center rounded-lg ${skill.tint}`}>{skill.icon}</span>
                      <div className="min-w-0 flex-1">
                        <div className="text-base font-semibold text-ink">{skill.name}</div>
                        <div className="text-xs text-muted">{skill.meta}</div>
                      </div>
                      <span className="inline-flex items-center gap-1 text-sm font-semibold text-brand-600 opacity-0 transition-opacity group-hover:opacity-100">
                        Làm bài <FiArrowRight size={14} />
                      </span>
                    </Link>
                  ))}
                </div>
              </Card>
            ))}
          </div>
        )}
      </Container>
    </Page>
  );
}
