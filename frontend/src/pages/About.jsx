import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { FiTarget, FiCpu, FiHeart, FiTrendingUp, FiArrowRight, FiGithub } from 'react-icons/fi';
import { Page, Container, Section, Card, Button, Badge } from '../ui';

const VALUES = [
  { icon: <FiTarget size={20} />, title: 'Thực chiến', desc: 'Đề thi sát thực tế từ Cambridge và các nguồn chính thống, mô phỏng đúng điều kiện phòng thi.' },
  { icon: <FiCpu size={20} />, title: 'AI hỗ trợ', desc: 'Chấm điểm Writing & Speaking chi tiết theo 4 tiêu chí IELTS, phản hồi tức thì.' },
  { icon: <FiTrendingUp size={20} />, title: 'Theo dõi tiến độ', desc: 'Biểu đồ tiến bộ, phân tích kỹ năng và lộ trình cá nhân hóa giúp bạn tiến đều.' },
  { icon: <FiHeart size={20} />, title: 'Thân thiện', desc: 'Một góc nhỏ ấm áp cho người học IELTS Việt Nam — giao diện gọn gàng, dễ dùng.' },
];

export default function About() {
  return (
    <Page>
      {/* Hero */}
      <div className="relative overflow-hidden">
        <div className="absolute inset-0 gradient-brand opacity-[0.06]" />
        <Container className="relative py-16 text-center">
          <Badge variant="brand" className="mb-3">Về Cramer</Badge>
          <motion.h1 initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="text-4xl font-bold text-ink">
            Giúp người Việt chinh phục IELTS
          </motion.h1>
          <motion.p initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.08 }}
            className="mx-auto mt-3 max-w-2xl text-md text-muted">
            Cramer là nền tảng luyện thi IELTS kết hợp đề thi chất lượng cao và công nghệ AI,
            được xây dựng bởi những người yêu thích việc học ngôn ngữ.
          </motion.p>
        </Container>
      </div>

      <Section>
        <Container>
          <div className="grid gap-4 sm:grid-cols-2">
            {VALUES.map((v, i) => (
              <motion.div key={v.title} initial={{ opacity: 0, y: 16 }} whileInView={{ opacity: 1, y: 0 }} viewport={{ once: true }} transition={{ delay: i * 0.05 }}>
                <Card className="flex h-full items-start gap-4">
                  <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-brand-soft text-brand-600">{v.icon}</div>
                  <div>
                    <h3 className="text-lg font-bold text-ink">{v.title}</h3>
                    <p className="mt-1 text-base text-muted">{v.desc}</p>
                  </div>
                </Card>
              </motion.div>
            ))}
          </div>
        </Container>
      </Section>

      <Section className="!pt-0">
        <Container size="narrow">
          <Card className="gradient-brand text-center text-white">
            <h2 className="text-2xl font-bold">Sẵn sàng bắt đầu?</h2>
            <p className="mx-auto mt-2 max-w-md text-md text-white/85">
              Tạo tài khoản miễn phí và làm bài thi thử đầu tiên của bạn ngay hôm nay.
            </p>
            <div className="mt-5 flex items-center justify-center gap-3">
              <Link to="/login"><Button variant="secondary" size="lg" iconRight={<FiArrowRight size={16} />}>Bắt đầu miễn phí</Button></Link>
              <a href="https://github.com" target="_blank" rel="noreferrer">
                <Button variant="ghost" size="lg" className="text-white hover:bg-white/15" iconLeft={<FiGithub size={16} />}>GitHub</Button>
              </a>
            </div>
          </Card>
        </Container>
      </Section>
    </Page>
  );
}
