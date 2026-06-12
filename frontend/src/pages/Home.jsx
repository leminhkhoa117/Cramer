import { useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import {
  FiArrowRight, FiBookOpen, FiHeadphones, FiEdit3, FiMic, FiCpu,
  FiBarChart2, FiChevronDown, FiCheckCircle, FiPlay,
} from 'react-icons/fi';
import { useAuthStore } from '../stores';
import { Page, Container, Section, Card, Button, Badge } from '../ui';

const fade = (d = 0) => ({
  initial: { opacity: 0, y: 18 },
  whileInView: { opacity: 1, y: 0 },
  viewport: { once: true, margin: '-80px' },
  transition: { duration: 0.5, delay: d },
});

const SKILLS = [
  { icon: <FiBookOpen size={22} />, name: 'Reading', desc: '40 câu hỏi, 3 đoạn văn học thuật, chấm tự động tức thì.' },
  { icon: <FiHeadphones size={22} />, name: 'Listening', desc: '4 phần, audio chuẩn thi thật, transcript chi tiết.' },
  { icon: <FiEdit3 size={22} />, name: 'Writing', desc: 'Task 1 & 2, chấm AI theo 4 tiêu chí band điểm.' },
  { icon: <FiMic size={22} />, name: 'Speaking', desc: '3 part mô phỏng, phản hồi phát âm & độ trôi chảy.' },
];

const STEPS = [
  { n: '01', title: 'Chọn bài thi', desc: 'Duyệt thư viện đề Cambridge và các bộ đề được tuyển chọn theo kỹ năng.' },
  { n: '02', title: 'Làm bài như thật', desc: 'Giao diện mô phỏng phòng thi với đồng hồ, highlight và ghi chú.' },
  { n: '03', title: 'Nhận phản hồi AI', desc: 'Chấm điểm tức thì kèm phân tích chi tiết và lộ trình cải thiện.' },
];

const TESTIMONIALS = [
  { name: 'Minh Anh', band: '7.5', text: 'Giao diện làm bài giống hệt thi thật, phần chấm Writing bằng AI cực kỳ chi tiết.' },
  { name: 'Quốc Bảo', band: '7.0', text: 'Theo dõi tiến độ rõ ràng giúp mình biết nên tập trung vào kỹ năng nào.' },
  { name: 'Thu Hà', band: '8.0', text: 'Mình thích cách Cramer gọn gàng, không rối mắt, tập trung vào việc luyện.' },
];

const FAQ = [
  { q: 'Cramer có miễn phí không?', a: 'Có. Bạn có thể tạo tài khoản và làm bài thi thử miễn phí. Các gói trả phí mở thêm hạn mức và chấm điểm AI.' },
  { q: 'AI chấm điểm có chính xác không?', a: 'AI chấm theo đúng 4 tiêu chí IELTS (Task Achievement, Coherence, Lexical Resource, Grammar) và đưa ra phản hồi cụ thể cho từng tiêu chí.' },
  { q: 'Đề thi lấy từ đâu?', a: 'Đề được tuyển chọn từ Cambridge và các nguồn học thuật uy tín, mô phỏng sát định dạng thi thật.' },
];

function FaqItem({ q, a }) {
  const [open, setOpen] = useState(false);
  return (
    <Card interactive padded={false} className="overflow-hidden">
      <button onClick={() => setOpen((o) => !o)} className="flex w-full items-center justify-between gap-3 px-5 py-4 text-left">
        <span className="text-base font-semibold text-ink">{q}</span>
        <FiChevronDown size={18} className={`shrink-0 text-muted transition-transform ${open ? 'rotate-180' : ''}`} />
      </button>
      {open && <div className="px-5 pb-4 text-base text-muted">{a}</div>}
    </Card>
  );
}

export default function Home() {
  const user = useAuthStore((s) => s.user);
  const startHref = user ? '/courses' : '/login';

  return (
    <Page className="overflow-clip">
      {/* ── Hero ───────────────────────────────── */}
      <div className="relative">
        <div className="absolute inset-0 -z-10 gradient-brand opacity-[0.07]" />
        <div className="absolute -top-24 left-1/2 -z-10 h-72 w-[42rem] -translate-x-1/2 rounded-full bg-brand-200/40 blur-3xl" />
        <Container className="grid items-center gap-10 py-16 lg:grid-cols-2 lg:py-20">
          <motion.div {...fade()}>
            <Badge variant="brand" dot className="mb-4">Nền tảng luyện thi IELTS</Badge>
            <h1 className="text-4xl font-bold leading-tight text-ink sm:text-5xl">
              Luyện IELTS thông minh với <span className="text-gradient-brand">phản hồi AI</span>
            </h1>
            <p className="mt-4 max-w-xl text-md text-muted">
              Làm bài thi thử sát thực tế cho cả 4 kỹ năng, nhận chấm điểm và phân tích chi tiết
              từ AI, theo dõi tiến bộ của bạn theo thời gian.
            </p>
            <div className="mt-7 flex flex-wrap items-center gap-3">
              <Link to={startHref}><Button size="lg" iconRight={<FiArrowRight size={17} />}>Bắt đầu miễn phí</Button></Link>
              <Link to="/pricing"><Button size="lg" variant="outline" iconLeft={<FiPlay size={15} />}>Xem các gói</Button></Link>
            </div>
            <div className="mt-6 flex flex-wrap items-center gap-x-6 gap-y-2 text-sm text-muted">
              {['Miễn phí để bắt đầu', 'Chấm điểm AI 4 tiêu chí', 'Sát đề thi thật'].map((t) => (
                <span key={t} className="flex items-center gap-1.5"><FiCheckCircle size={15} className="text-success" />{t}</span>
              ))}
            </div>
          </motion.div>

          {/* Hero visual: mock score card */}
          <motion.div {...fade(0.15)} className="relative">
            <Card variant="glass" className="shadow-xl">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl gradient-brand text-white"><FiEdit3 size={18} /></div>
                  <div>
                    <div className="text-base font-bold text-ink">Writing Task 2</div>
                    <div className="text-xs text-muted">Đã chấm bằng AI</div>
                  </div>
                </div>
                <div className="text-right">
                  <div className="text-3xl font-bold text-gradient-brand">7.5</div>
                  <div className="text-xs text-muted">Overall band</div>
                </div>
              </div>
              <div className="mt-5 space-y-3">
                {[['Task Response', 8.0], ['Coherence', 7.5], ['Lexical Resource', 7.0], ['Grammar', 7.5]].map(([label, v]) => (
                  <div key={label}>
                    <div className="mb-1 flex justify-between text-sm"><span className="text-ink-2">{label}</span><span className="font-semibold text-ink">{v}</span></div>
                    <div className="h-1.5 w-full rounded-full bg-surface-2">
                      <div className="h-full rounded-full gradient-brand" style={{ width: `${(v / 9) * 100}%` }} />
                    </div>
                  </div>
                ))}
              </div>
            </Card>
            <motion.div initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} transition={{ delay: 0.5 }}
              className="absolute -bottom-4 -left-4 hidden sm:block">
              <Card className="flex items-center gap-2 !py-2.5 !px-3 shadow-lg">
                <FiBarChart2 className="text-brand-600" size={18} /><span className="text-sm font-semibold text-ink">+1.5 band trong 8 tuần</span>
              </Card>
            </motion.div>
          </motion.div>
        </Container>
      </div>

      {/* ── Skills ─────────────────────────────── */}
      <Section>
        <Container>
          <motion.div {...fade()} className="mx-auto mb-10 max-w-2xl text-center">
            <h2 className="text-3xl font-bold text-ink">Luyện đủ 4 kỹ năng</h2>
            <p className="mt-2 text-md text-muted">Mỗi kỹ năng đều được thiết kế sát định dạng thi thật.</p>
          </motion.div>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {SKILLS.map((s, i) => (
              <motion.div key={s.name} {...fade(i * 0.06)}>
                <Card interactive className="h-full">
                  <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-xl bg-brand-soft text-brand-600">{s.icon}</div>
                  <h3 className="text-lg font-bold text-ink">{s.name}</h3>
                  <p className="mt-1 text-base text-muted">{s.desc}</p>
                </Card>
              </motion.div>
            ))}
          </div>
        </Container>
      </Section>

      {/* ── AI feature band ────────────────────── */}
      <Section className="!py-0">
        <Container>
          <motion.div {...fade()}>
            <Card className="grid items-center gap-6 overflow-hidden lg:grid-cols-2">
              <div>
                <Badge variant="brand" className="mb-3">Chấm điểm AI</Badge>
                <h2 className="text-2xl font-bold text-ink">Phản hồi chi tiết, không chỉ là điểm số</h2>
                <p className="mt-2 text-md text-muted">
                  AI phân tích bài Writing và Speaking của bạn theo từng tiêu chí band điểm,
                  chỉ ra điểm mạnh, lỗi sai và gợi ý cải thiện cụ thể.
                </p>
                <ul className="mt-4 space-y-2">
                  {['Đánh giá theo 4 tiêu chí chính thức', 'Gợi ý từ vựng và cấu trúc nâng band', 'Phản hồi tức thì sau khi nộp bài'].map((t) => (
                    <li key={t} className="flex items-start gap-2 text-base text-ink-2"><FiCheckCircle className="mt-0.5 shrink-0 text-success" size={16} />{t}</li>
                  ))}
                </ul>
              </div>
              <div className="flex items-center justify-center rounded-xl bg-surface-2 p-8">
                <div className="flex h-20 w-20 items-center justify-center rounded-2xl gradient-brand text-white shadow-lg"><FiCpu size={36} /></div>
              </div>
            </Card>
          </motion.div>
        </Container>
      </Section>

      {/* ── How it works ───────────────────────── */}
      <Section>
        <Container>
          <motion.div {...fade()} className="mx-auto mb-10 max-w-2xl text-center">
            <h2 className="text-3xl font-bold text-ink">Cách hoạt động</h2>
            <p className="mt-2 text-md text-muted">Ba bước đơn giản để bắt đầu hành trình IELTS của bạn.</p>
          </motion.div>
          <div className="grid gap-4 md:grid-cols-3">
            {STEPS.map((s, i) => (
              <motion.div key={s.n} {...fade(i * 0.08)}>
                <Card className="h-full">
                  <div className="text-3xl font-bold text-gradient-brand">{s.n}</div>
                  <h3 className="mt-2 text-lg font-bold text-ink">{s.title}</h3>
                  <p className="mt-1 text-base text-muted">{s.desc}</p>
                </Card>
              </motion.div>
            ))}
          </div>
        </Container>
      </Section>

      {/* ── Testimonials ───────────────────────── */}
      <Section className="!pt-0">
        <Container>
          <motion.div {...fade()} className="mx-auto mb-10 max-w-2xl text-center">
            <h2 className="text-3xl font-bold text-ink">Người học nói gì</h2>
          </motion.div>
          <div className="grid gap-4 md:grid-cols-3">
            {TESTIMONIALS.map((t, i) => (
              <motion.div key={t.name} {...fade(i * 0.06)}>
                <Card className="h-full">
                  <p className="text-base text-ink-2">“{t.text}”</p>
                  <div className="mt-4 flex items-center justify-between border-t border-line pt-3">
                    <span className="text-base font-semibold text-ink">{t.name}</span>
                    <Badge variant="brand">Band {t.band}</Badge>
                  </div>
                </Card>
              </motion.div>
            ))}
          </div>
        </Container>
      </Section>

      {/* ── FAQ ────────────────────────────────── */}
      <Section className="!pt-0">
        <Container size="narrow">
          <motion.div {...fade()} className="mb-6 text-center">
            <h2 className="text-3xl font-bold text-ink">Câu hỏi thường gặp</h2>
          </motion.div>
          <div className="flex flex-col gap-3">{FAQ.map((f) => <FaqItem key={f.q} {...f} />)}</div>
        </Container>
      </Section>

      {/* ── Final CTA ──────────────────────────── */}
      <Section className="!pt-0">
        <Container size="narrow">
          <motion.div {...fade()}>
            <Card className="gradient-brand text-center text-white">
              <h2 className="text-3xl font-bold">Sẵn sàng nâng band của bạn?</h2>
              <p className="mx-auto mt-2 max-w-md text-md text-white/85">Tham gia cùng những người học IELTS đang tiến bộ mỗi ngày với Cramer.</p>
              <div className="mt-5 flex justify-center">
                <Link to={startHref}><Button variant="secondary" size="lg" iconRight={<FiArrowRight size={17} />}>Bắt đầu ngay</Button></Link>
              </div>
            </Card>
          </motion.div>
        </Container>
      </Section>
    </Page>
  );
}
