import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { FiSearch, FiArrowRight } from 'react-icons/fi';
import { useCourseStore } from '../stores';
import { Page, Container, Section, Input, Card, EmptyState, Skeleton } from '../ui';

export default function Courses() {
  const { courses, loading, error, fetchCourses } = useCourseStore();
  const [search, setSearch] = useState('');

  useEffect(() => { fetchCourses(); }, [fetchCourses]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return courses;
    return courses.filter((c) =>
      [c.name, c.code, c.description].some((v) => v && v.toLowerCase().includes(q))
    );
  }, [courses, search]);

  return (
    <Page>
      <Container>
        <Section className="!pb-4">
          <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
            <div>
              <h1 className="text-3xl font-bold text-ink">Khám phá bộ đề IELTS</h1>
              <p className="mt-1 max-w-2xl text-md text-muted">
                Luyện tập với các bài thi chất lượng cao từ Cambridge và nhiều nguồn chính thống khác.
              </p>
            </div>
            <div className="w-full md:w-72">
              <Input
                placeholder="Tìm theo tên bộ đề…"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                iconLeft={<FiSearch size={16} />}
              />
            </div>
          </div>
        </Section>

        <div className="pb-12">
          {error && <p className="rounded-lg bg-danger-soft px-4 py-3 text-base text-danger">{error}</p>}

          {loading && courses.length === 0 ? (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {Array.from({ length: 6 }).map((_, i) => (
                <Card key={i} className="overflow-hidden !p-0">
                  <Skeleton className="h-36 w-full" rounded="rounded-none" />
                  <div className="p-4"><Skeleton className="h-5 w-2/3" /><Skeleton className="mt-2 h-3 w-full" /></div>
                </Card>
              ))}
            </div>
          ) : filtered.length === 0 ? (
            <EmptyState title="Không tìm thấy bộ đề" description="Thử từ khóa khác hoặc xóa bộ lọc tìm kiếm." />
          ) : (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {filtered.map((course, i) => (
                <motion.div
                  key={course.code}
                  initial={{ opacity: 0, y: 12 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ duration: 0.25, delay: Math.min(i * 0.03, 0.3) }}
                >
                  <Link to={`/courses/${course.code}`} className="group block h-full">
                    <Card interactive padded={false} className="flex h-full flex-col overflow-hidden">
                      <div className="relative h-36 w-full overflow-hidden bg-brand-soft">
                        {course.coverImageUrl ? (
                          <img src={course.coverImageUrl} alt={course.name || course.code}
                            className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105" />
                        ) : (
                          <div className="flex h-full items-center justify-center text-4xl">📚</div>
                        )}
                      </div>
                      <div className="flex flex-1 flex-col p-4">
                        <h3 className="text-lg font-bold text-ink">{course.name || course.code}</h3>
                        <p className="mt-1 line-clamp-2 flex-1 text-base text-muted">
                          {course.description || `Bộ đề ${course.name || course.code}`}
                        </p>
                        <span className="mt-3 inline-flex items-center gap-1.5 text-base font-semibold text-brand-600">
                          Xem các bài test <FiArrowRight size={15} className="transition-transform group-hover:translate-x-0.5" />
                        </span>
                      </div>
                    </Card>
                  </Link>
                </motion.div>
              ))}
            </div>
          )}
        </div>
      </Container>
    </Page>
  );
}
