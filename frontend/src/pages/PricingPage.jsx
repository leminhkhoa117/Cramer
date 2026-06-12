import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { FiCheck, FiArrowRight } from 'react-icons/fi';
import { subscriptionApi, paymentApi } from '../lib/api';
import { useAuthStore } from '../stores';
import { Page, Container, Section, Card, Button, Badge, Skeleton } from '../ui';

const fmtVnd = (n) => (n > 0 ? new Intl.NumberFormat('vi-VN').format(n) + '₫' : 'Miễn phí');
const EMOJI = { cramerous: '🌟', cramerich: '🌻', cramerie: '🌾' };

const FAQ = [
  { q: 'Lúa là gì?', a: 'Lúa là đơn vị tín dụng trong Cramer, dùng để mở khóa các tính năng cao cấp như chấm điểm AI, dịch từ vựng và làm thêm bài khi vượt hạn mức tháng.' },
  { q: 'Sự khác nhau giữa các gói?', a: 'Gói miễn phí (Cramerie) phù hợp để bắt đầu. Các gói trả phí mở thêm hạn mức làm bài, chấm điểm AI và Lúa thưởng hàng tháng.' },
  { q: 'Tôi có thể hủy bất cứ lúc nào không?', a: 'Có. Gói của bạn vẫn hoạt động đến hết chu kỳ đã thanh toán và sẽ không tự động gia hạn nếu bạn tắt.' },
];

function TierCard({ tier, popular, authed, onBuy, busy }) {
  const features = [
    `${tier.monthlyAttemptLimit < 0 ? 'Không giới hạn' : tier.monthlyAttemptLimit} lượt làm bài/tháng`,
    `${tier.includedAiGradings} lượt chấm điểm AI`,
    `${tier.monthlyTranslationLimit < 0 ? 'Không giới hạn' : tier.monthlyTranslationLimit} lượt dịch từ vựng`,
    tier.monthlyLuaBonus > 0 ? `${tier.monthlyLuaBonus} Lúa thưởng mỗi tháng` : `${tier.initialLua} Lúa khởi tạo`,
  ];
  return (
    <Card className={`relative flex flex-col gap-4 ${popular ? 'ring-2 ring-brand-500' : ''}`}>
      {popular && <Badge variant="brand" className="absolute -top-2.5 left-5">Phổ biến nhất</Badge>}
      <div className="flex items-center gap-2">
        <span className="text-3xl">{EMOJI[tier.code] || '🌾'}</span>
        <div>
          <h3 className="text-lg font-bold text-ink">{tier.name}</h3>
          <Badge size="sm" variant={tier.priceVnd > 0 ? 'brand' : 'neutral'}>{tier.priceVnd > 0 ? 'Trả phí' : 'Miễn phí'}</Badge>
        </div>
      </div>
      <div className="text-3xl font-bold text-ink">{fmtVnd(tier.priceVnd)}{tier.priceVnd > 0 && <span className="text-sm font-medium text-muted">/tháng</span>}</div>
      <ul className="flex flex-1 flex-col gap-2">
        {features.map((f) => (
          <li key={f} className="flex items-start gap-2 text-base text-ink-2">
            <FiCheck className="mt-0.5 shrink-0 text-success" size={16} />{f}
          </li>
        ))}
      </ul>
      {tier.priceVnd > 0 ? (
        authed
          ? <Button fullWidth loading={busy} onClick={() => onBuy(tier)}>Nâng cấp</Button>
          : <Link to="/login"><Button fullWidth>Bắt đầu</Button></Link>
      ) : (
        <Link to={authed ? '/dashboard' : '/login'}><Button fullWidth variant="outline">{authed ? 'Đang dùng' : 'Dùng miễn phí'}</Button></Link>
      )}
    </Card>
  );
}

export default function PricingPage() {
  const user = useAuthStore((s) => s.user);
  const [tiers, setTiers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    subscriptionApi.tiers().then((t) => setTiers((t || []).sort((a, b) => a.priceVnd - b.priceVnd))).catch(() => {}).finally(() => setLoading(false));
  }, []);

  const buy = async (tier) => {
    setBusy(true);
    try { const order = await paymentApi.createSubscriptionOrder(tier.id, tier.code); if (order.checkoutUrl) window.location.assign(order.checkoutUrl); }
    finally { setBusy(false); }
  };

  return (
    <Page>
      {/* Hero */}
      <div className="relative overflow-hidden">
        <div className="absolute inset-0 gradient-brand opacity-[0.06]" />
        <Container className="relative py-14 text-center">
          <Badge variant="brand" className="mb-3">Bảng giá minh bạch</Badge>
          <h1 className="text-4xl font-bold text-ink">Chọn gói phù hợp với bạn</h1>
          <p className="mx-auto mt-3 max-w-2xl text-md text-muted">
            Bắt đầu miễn phí, nâng cấp khi cần thêm hạn mức làm bài, chấm điểm AI và Lúa thưởng.
          </p>
        </Container>
      </div>

      <Container className="pb-4">
        {loading ? (
          <div className="grid gap-4 md:grid-cols-3">{Array.from({ length: 3 }).map((_, i) => <Card key={i}><Skeleton className="h-64 w-full" /></Card>)}</div>
        ) : (
          <div className="grid gap-4 md:grid-cols-3">
            {tiers.map((t, i) => (
              <motion.div key={t.code} initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.06 }}>
                <TierCard tier={t} popular={t.code === 'cramerich'} authed={!!user} onBuy={buy} busy={busy} />
              </motion.div>
            ))}
          </div>
        )}
      </Container>

      <Section>
        <Container size="narrow">
          <h2 className="mb-4 text-center text-2xl font-bold text-ink">Câu hỏi thường gặp</h2>
          <div className="flex flex-col gap-3">
            {FAQ.map((f) => (
              <Card key={f.q}>
                <h3 className="text-base font-bold text-ink">{f.q}</h3>
                <p className="mt-1 text-base text-muted">{f.a}</p>
              </Card>
            ))}
          </div>
          <div className="mt-8 text-center">
            <Link to={user ? '/courses' : '/login'}>
              <Button size="lg" iconRight={<FiArrowRight size={16} />}>{user ? 'Bắt đầu luyện tập' : 'Tạo tài khoản miễn phí'}</Button>
            </Link>
          </div>
        </Container>
      </Section>
    </Page>
  );
}
