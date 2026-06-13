import { useEffect, useState } from 'react';
import { FiZap, FiCreditCard, FiClock, FiCheck, FiStar } from 'react-icons/fi';
import { useUserStatsStore } from '../stores';
import { subscriptionApi, paymentApi, getApiError } from '../lib/api';
import { toast } from '../ui/toast';
import {
  Page, Container, PageHeader, Card, Button, Badge, Switch, Tabs, Progress,
  StatCard, EmptyState, Skeleton,
} from '../ui';

const fmtVnd = (n) => new Intl.NumberFormat('vi-VN').format(n || 0) + '₫';
const TIER_EMOJI = { cramerous: '🌟', cramerich: '🌻', cramerie: '🌾' };

function UsageRow({ label, used, limit }) {
  const unlimited = limit == null || limit < 0;
  const pct = unlimited || limit === 0 ? 0 : Math.min(100, (used / limit) * 100);
  return (
    <div className="flex flex-col gap-1.5">
      <div className="flex items-center justify-between text-base">
        <span className="font-semibold text-ink-2">{label}</span>
        <span className="text-muted">{unlimited ? 'Không giới hạn' : `${used} / ${limit}`}</span>
      </div>
      {!unlimited && <Progress value={pct} />}
    </div>
  );
}

export default function SubscriptionPage() {
  const subscription = useUserStatsStore((s) => s.subscription);
  const credits = useUserStatsStore((s) => s.credits);
  const tiers = useUserStatsStore((s) => s.tiers);
  const grading = useUserStatsStore((s) => s.grading);
  const loading = useUserStatsStore((s) => s.loading);
  const fetchUserStats = useUserStatsStore((s) => s.fetchUserStats);

  const [tab, setTab] = useState('limits');
  const [packs, setPacks] = useState([]);
  const [history, setHistory] = useState([]);
  const [aiGrading, setAiGrading] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => { fetchUserStats(true); }, [fetchUserStats]);
  useEffect(() => { setAiGrading(!!subscription?.aiGradingEnabled); }, [subscription]);
  useEffect(() => {
    paymentApi.luaPacks().then(setPacks).catch(() => {});
    paymentApi.history({ page: 0, size: 20 }).then(setHistory).catch(() => {});
  }, []);

  const premium = subscription?.premium;
  const tierCode = subscription?.tierCode || 'cramerie';

  const toggleAi = async (next) => {
    if (!premium) return toast.error('Chấm điểm AI chỉ dành cho gói trả phí.');
    setAiGrading(next);
    try { await subscriptionApi.setAiGrading(next); toast.success('Đã cập nhật chấm điểm AI'); fetchUserStats(true); }
    catch (e) { setAiGrading(!next); toast.error(getApiError(e).message); }
  };

  const buyPack = async (code) => {
    setBusy(true);
    try { const order = await paymentApi.createLuaOrder(code); if (order.checkoutUrl) window.location.assign(order.checkoutUrl); }
    catch (e) { toast.error(getApiError(e).message); } finally { setBusy(false); }
  };

  const upgradeTier = async (tier) => {
    setBusy(true);
    try { const order = await paymentApi.createSubscriptionOrder(tier.id, tier.code); if (order.checkoutUrl) window.location.assign(order.checkoutUrl); }
    catch (e) { toast.error(getApiError(e).message); } finally { setBusy(false); }
  };

  return (
    <Page>
      <Container className="py-8">
        <PageHeader title="Gói đăng ký & Lúa" subtitle="Quản lý gói, hạn mức và số dư Lúa của bạn" />

        {/* Current plan card */}
        <Card className="mt-5 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-4">
            <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-brand-soft text-3xl">{TIER_EMOJI[tierCode] || '🌾'}</div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-xl font-bold text-ink">{subscription?.tierName || 'Cramerie'}</h2>
                <Badge variant={premium ? 'brand' : 'neutral'}>{premium ? 'Trả phí' : 'Miễn phí'}</Badge>
              </div>
              <p className="text-base text-muted">
                {subscription?.expiresAt ? `Gia hạn: ${new Date(subscription.expiresAt).toLocaleDateString('vi-VN')}` : 'Không giới hạn thời gian'}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-3 rounded-xl bg-surface-2 px-4 py-2.5">
            <span className="text-2xl">🌾</span>
            <div>
              <div className="text-xs font-semibold uppercase text-muted">Số dư Lúa</div>
              <div className="text-xl font-bold text-ink">{credits?.balance ?? 0}</div>
            </div>
          </div>
        </Card>

        <div className="mt-6">
          <Tabs value={tab} onChange={setTab} items={[
            { value: 'limits', label: 'Hạn mức', icon: <FiZap size={15} /> },
            { value: 'packages', label: 'Nâng cấp & Lúa', icon: <FiCreditCard size={15} /> },
            { value: 'history', label: 'Lịch sử', icon: <FiClock size={15} /> },
          ]} />
        </div>

        <div className="mt-5">
          {tab === 'limits' && (
            <div className="grid gap-4 lg:grid-cols-3">
              <Card className="lg:col-span-2 flex flex-col gap-4">
                <h3 className="text-lg font-bold text-ink">Hạn mức tháng này</h3>
                {loading && !subscription ? <Skeleton className="h-24 w-full" /> : (
                  <>
                    <UsageRow label="Lượt làm bài" used={subscription?.attemptsUsed ?? 0} limit={premium ? -1 : 60} />
                    <UsageRow label="Lượt làm bài AI" used={subscription?.attemptAisUsed ?? 0} limit={premium ? -1 : 30} />
                    <UsageRow label="Tin nhắn trợ lý" used={subscription?.chatbotUsed ?? 0} limit={subscription?.chatMonthlyLimit ?? 50} />
                  </>
                )}
              </Card>
              <Card className="flex flex-col gap-3">
                <h3 className="text-lg font-bold text-ink">Chấm điểm AI</h3>
                <p className="text-base text-muted">Bật để dùng AI chấm bài Writing chi tiết.</p>
                <Switch checked={aiGrading} onChange={toggleAi} label={aiGrading ? 'Đang bật' : 'Đang tắt'} disabled={!premium} />
                {!premium && <p className="text-sm text-warning">Nâng cấp gói trả phí để bật tính năng này.</p>}
                <div className="mt-1 rounded-lg bg-surface-2 px-3 py-2 text-sm text-muted">
                  Còn lại <span className="font-bold text-ink">{grading?.gradingsRemaining ?? 0}</span> lượt chấm AI miễn phí
                </div>
              </Card>
            </div>
          )}

          {tab === 'packages' && (
            <div className="flex flex-col gap-6">
              <div>
                <h3 className="mb-3 text-lg font-bold text-ink">Nâng cấp gói</h3>
                <div className="grid gap-4 auto-rows-fr sm:grid-cols-2">
                  {tiers.filter((t) => t.priceVnd > 0).map((t) => (
                    <Card key={t.code} className="flex h-full flex-col gap-3">
                      <div className="flex items-center gap-2">
                        <span className="text-2xl">{TIER_EMOJI[t.code] || '🌻'}</span>
                        <h4 className="text-lg font-bold text-ink">{t.name}</h4>
                      </div>
                      <div className="text-2xl font-bold text-brand-700">{fmtVnd(t.priceVnd)}<span className="text-sm font-medium text-muted">/tháng</span></div>
                      <ul className="flex flex-col gap-1.5 text-base text-ink-2">
                        <li className="flex items-center gap-2"><FiCheck className="text-success" size={15} />{t.monthlyAttemptLimit < 0 ? 'Không giới hạn' : t.monthlyAttemptLimit} lượt làm bài</li>
                        <li className="flex items-center gap-2"><FiCheck className="text-success" size={15} />{t.includedAiGradings} lượt chấm AI</li>
                        <li className="flex items-center gap-2"><FiCheck className="text-success" size={15} />{t.monthlyLuaBonus} Lúa mỗi tháng</li>
                      </ul>
                      <Button className="mt-auto" fullWidth loading={busy} disabled={t.code === tierCode} onClick={() => upgradeTier(t)}>
                        {t.code === tierCode ? 'Gói hiện tại' : 'Nâng cấp'}
                      </Button>
                    </Card>
                  ))}
                </div>
              </div>
              <div>
                <h3 className="mb-3 text-lg font-bold text-ink">Mua thêm Lúa</h3>
                {packs.length === 0 ? <EmptyState icon="🌾" title="Chưa có gói Lúa" /> : (
                  <div className="grid gap-4 auto-rows-fr grid-cols-2 lg:grid-cols-3">
                    {packs.map((p) => (
                      <Card key={p.code} className="flex h-full flex-col gap-2 text-center">
                        <div className="text-3xl">🌾</div>
                        <div className="text-xl font-bold text-ink">{p.luaAmount} Lúa</div>
                        {p.description && <div className="text-sm text-muted">{p.description}</div>}
                        <div className="text-lg font-bold text-brand-700">{fmtVnd(p.priceVnd)}</div>
                        <Button className="mt-auto" fullWidth variant="outline" loading={busy} onClick={() => buyPack(p.code)}>Mua</Button>
                      </Card>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {tab === 'history' && (
            history.length === 0 ? <EmptyState icon="🧾" title="Chưa có giao dịch" description="Lịch sử thanh toán của bạn sẽ hiển thị ở đây." /> : (
              <Card padded={false} className="divide-y divide-line overflow-hidden">
                {history.map((o) => (
                  <div key={o.orderCode} className="flex items-center justify-between gap-3 px-4 py-3">
                    <div>
                      <div className="text-base font-semibold text-ink">{o.type === 'SUBSCRIPTION' ? 'Nâng cấp gói' : 'Mua Lúa'} · #{o.orderCode}</div>
                      <div className="text-sm text-muted">{o.createdAt ? new Date(o.createdAt).toLocaleString('vi-VN') : ''}</div>
                    </div>
                    <div className="text-right">
                      <div className="text-base font-bold text-ink">{fmtVnd(o.amountVnd)}</div>
                      <Badge size="sm" variant={o.status === 'PAID' ? 'success' : o.status === 'PENDING' ? 'warning' : 'danger'}>{o.status}</Badge>
                    </div>
                  </div>
                ))}
              </Card>
            )
          )}
        </div>
      </Container>
    </Page>
  );
}
