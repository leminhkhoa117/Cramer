import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { FiSearch, FiArrowRight, FiAlertTriangle, FiRefreshCw, FiBookOpen } from 'react-icons/fi';
import { useCourseStore } from '../stores';
import {
  Page, Container, Input, Card, Badge, Button, Tabs, Pagination, EmptyState, Alert, Skeleton, SkeletonText,
} from '../ui';

const PAGE_SIZE = 9;

const SOURCE_LABELS = {
  CAMBRIDGE: 'Cambridge',
  CUSTOM: 'Tự tạo',
  OFFICIAL: 'Chính thức',
  AI: 'AI',
  ABTS: 'AI',
  AI_GENERATED: 'AI Generated',
  IELTS: 'IELTS',
};

function prettySource(value) {
  if (!value) return 'Khác';
  const key = String(value).toUpperCase();
  if (SOURCE_LABELS[key]) return SOURCE_LABELS[key];
  return String(value)
    .toLowerCase()
    .replace(/(^|[\s_-])(\w)/g, (_, sep, ch) => (sep ? ' ' : '') + ch.toUpperCase())
    .trim();
}

const gridVariants = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: { staggerChildren: 0.05 } },
};
const cardVariants = {
  hidden: { opacity: 0, y: 16 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.3, ease: 'easeOut' } },
};

export default function Courses() {
  const { courses, loading, error, fetchCourses } = useCourseStore();
  const [search, setSearch] = useState('');
  const [source, setSource] = useState('ALL');
  const [page, setPage] = useState(0);

  useEffect(() => { fetchCourses(); }, [fetchCourses]);
  useEffect(() => { setPage(0); }, [search, source]);

  const sourceCounts = useMemo(() => {
    const map = new Map();
    for (const c of courses) {
      const key = c.sourceType || 'KHÁC';
      map.set(key, (map.get(key) || 0) + 1);
    }
    return Array.from(map.entries());
  }, [courses]);

  const sourceTabs = useMemo(() => ([
    { value: 'ALL', label: 'Tất cả', badge: courses.length || undefined },
    ...sourceCounts.map(([value, n]) => ({
      value,
      label: prettySource(value === 'KHÁC' ? null : value),
      badge: n,
    })),
  ]), [sourceCounts, courses.length]);

  const filtered = useMemo(() => {
    let list = courses;
    if (source !== 'ALL') list = list.filter((c) => (c.sourceType || 'KHÁC') === source);
    const q = search.trim().toLowerCase();
    if (q) {
      list = list.filter((c) =>
        [c.name, c.code, c.description].some((v) => v && v.toLowerCase().includes(q))
      );
    }
    return list;
  }, [courses, source, search]);

  const totalTests = useMemo(
    () => courses.reduce((sum, c) => sum + (c.testCount || 0), 0),
    [courses]
  );

  const totalPages = Math.ceil(filtered.length / PAGE_SIZE);
  const safePage = Math.min(page, Math.max(0, totalPages - 1));
  const pageItems = filtered.slice(safePage * PAGE_SIZE, safePage * PAGE_SIZE + PAGE_SIZE);

  const initialLoading = loading && courses.length === 0;
  const hardError = error && courses.length === 0;
  const filtersActive = search.trim() !== '' || source !== 'ALL';

  return (
    <Page>
      <Container className="py-8 md:py-10">
        {/* Hero band */}
        <section className="relative overflow-hidden rounded-3xl gradient-brand px-6 py-10 shadow-lg md:px-10 md:py-12">
          <div aria-hidden className="pointer-events-none absolute -right-10 -top-16 h-56 w-56 rounded-full bg-white/10 blur-2xl" />
          <div aria-hidden className="pointer-events-none absolute -bottom-20 -left-10 h-56 w-56 rounded-full bg-white/10 blur-2xl" />
          <div className="relative max-w-2xl">
            <span className="inline-flex items-center gap-1.5 rounded-full border border-white/25 bg-white/15 px-3 py-1 text-xs font-semibold text-white">
              <FiBookOpen size={13} /> Thư viện đề thi
            </span>
            <h1 className="mt-4 text-3xl font-bold leading-tight text-white md:text-4xl">
              Khám phá các bộ đề IELTS
            </h1>
            <p className="mt-3 text-md text-white/85">
              Nâng cao kỹ năng của bạn với các bài thi chất lượng cao từ Cambridge và nhiều nguồn chính thống khác.
            </p>
            {courses.length > 0 && (
              <div className="mt-5 flex flex-wrap items-center gap-2">
                <span className="inline-flex items-center rounded-full bg-white/15 px-3 py-1 text-sm font-semibold text-white">
                  {courses.length} bộ đề
                </span>
                <span className="inline-flex items-center rounded-full bg-white/15 px-3 py-1 text-sm font-semibold text-white">
                  {totalTests} bài test
                </span>
              </div>
            )}
          </div>
        </section>

        {/* Body */}
        <div className="pb-4 pt-8">
          {hardError ? (
            <div className="flex min-h-[42vh] items-center justify-center">
              <EmptyState
                icon={<FiAlertTriangle />}
                title="Không thể tải danh sách bộ đề"
                description={error || 'Đã có lỗi xảy ra khi kết nối tới máy chủ. Vui lòng thử lại sau giây lát.'}
                action={
                  <Button iconLeft={<FiRefreshCw size={16} />} loading={loading} onClick={() => fetchCourses(true)}>
                    Thử lại
                  </Button>
                }
              />
            </div>
          ) : initialLoading ? (
            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
              {Array.from({ length: 9 }).map((_, i) => (
                <Card key={i} padded={false} className="overflow-hidden">
                  <Skeleton rounded="rounded-none" className="aspect-[16/10] w-full" />
                  <div className="p-5">
                    <Skeleton className="h-3 w-16" />
                    <Skeleton className="mt-3 h-5 w-2/3" />
                    <SkeletonText lines={2} className="mt-3" />
                    <div className="mt-4 flex gap-2">
                      <Skeleton className="h-6 w-24 rounded-full" />
                      <Skeleton className="h-6 w-20 rounded-full" />
                    </div>
                    <Skeleton className="mt-5 h-10 w-full rounded-lg" />
                  </div>
                </Card>
              ))}
            </div>
          ) : (
            <>
              {error && courses.length > 0 && (
                <Alert variant="danger" className="mb-5">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <span>Không thể làm mới danh sách. Đang hiển thị dữ liệu đã tải trước đó.</span>
                    <Button size="sm" variant="outline" iconLeft={<FiRefreshCw size={14} />} onClick={() => fetchCourses(true)}>
                      Thử lại
                    </Button>
                  </div>
                </Alert>
              )}

              {/* Controls: source filter + search */}
              <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                {sourceTabs.length > 1 ? (
                  <div className="-mx-1 overflow-x-auto px-1 pb-1">
                    <Tabs variant="pill" items={sourceTabs} value={source} onChange={setSource} />
                  </div>
                ) : (
                  <span />
                )}
                <div className="w-full lg:w-80 lg:shrink-0">
                  <Input
                    placeholder="Tìm theo tên hoặc mã bộ đề…"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    iconLeft={<FiSearch size={16} />}
                  />
                </div>
              </div>

              {/* Result meta */}
              <p className="mt-4 text-sm text-muted">
                {filtered.length === 0 ? (
                  'Không có bộ đề'
                ) : (
                  <>
                    <span className="font-semibold text-ink-2">{filtered.length}</span> bộ đề
                    {filtersActive ? ' phù hợp' : ''}
                  </>
                )}
              </p>

              {filtered.length === 0 ? (
                <EmptyState
                  className="!py-16"
                  icon={<FiSearch />}
                  title="Không tìm thấy bộ đề nào phù hợp"
                  description="Hãy thử một từ khóa khác hoặc đổi bộ lọc nguồn đề để xem thêm kết quả."
                  action={filtersActive ? (
                    <Button variant="outline" onClick={() => { setSearch(''); setSource('ALL'); }}>
                      Xóa bộ lọc
                    </Button>
                  ) : undefined}
                />
              ) : (
                <>
                  <motion.div
                    key={`${source}-${search}-${safePage}`}
                    className="mt-5 grid gap-5 sm:grid-cols-2 lg:grid-cols-3"
                    variants={gridVariants}
                    initial="hidden"
                    animate="visible"
                  >
                    {pageItems.map((course) => (
                      <motion.div key={course.id ?? course.code} variants={cardVariants} className="h-full">
                        <Link to={`/courses/${course.code}`} className="group block h-full">
                          <Card
                            padded={false}
                            className="flex h-full flex-col overflow-hidden transition duration-200 hover:-translate-y-1 hover:border-brand-200 hover:shadow-lg"
                          >
                            {/* Cover */}
                            <div className="relative aspect-[16/10] w-full overflow-hidden">
                              {course.coverImageUrl ? (
                                <img
                                  src={course.coverImageUrl}
                                  alt={course.name || course.code}
                                  loading="lazy"
                                  className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105"
                                />
                              ) : (
                                <div className="relative flex h-full w-full items-center justify-center gradient-brand">
                                  <span aria-hidden className="pointer-events-none absolute -right-6 -top-8 h-24 w-24 rounded-full bg-white/15 blur-xl" />
                                  <span aria-hidden className="pointer-events-none absolute -bottom-8 -left-6 h-24 w-24 rounded-full bg-white/10 blur-xl" />
                                  <span className="relative text-2xl font-bold tracking-wide text-white">
                                    {course.code}
                                  </span>
                                </div>
                              )}
                            </div>

                            {/* Body */}
                            <div className="flex flex-1 flex-col p-5">
                              <span className="text-xs font-semibold uppercase tracking-wide text-brand-600">
                                {course.code}
                              </span>
                              <h3 className="mt-2 line-clamp-1 text-lg font-bold text-ink">
                                {course.name || course.code}
                              </h3>
                              <p className="mt-2 line-clamp-2 text-base text-muted">
                                {course.description || `Bộ đề luyện thi ${course.name || course.code}.`}
                              </p>

                              <div className="mt-3 flex flex-wrap items-center gap-2">
                                <Badge variant="brand">
                                  <FiBookOpen size={12} /> {course.testCount ?? 0} bài test
                                </Badge>
                                <Badge variant="neutral">{prettySource(course.sourceType)}</Badge>
                              </div>

                              <div className="mt-auto pt-5">
                                <span className="inline-flex w-full items-center justify-center gap-2 rounded-lg bg-brand-600 px-4 py-2.5 text-base font-semibold text-white shadow-sm transition-colors group-hover:bg-brand-700">
                                  Xem các bài test
                                  <FiArrowRight size={16} className="transition-transform group-hover:translate-x-0.5" />
                                </span>
                              </div>
                            </div>
                          </Card>
                        </Link>
                      </motion.div>
                    ))}
                  </motion.div>

                  {totalPages > 1 && (
                    <div className="mt-8">
                      <Pagination page={safePage} totalPages={totalPages} onPageChange={setPage} />
                    </div>
                  )}
                </>
              )}
            </>
          )}
        </div>
      </Container>
    </Page>
  );
}
