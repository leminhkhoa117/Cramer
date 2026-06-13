import { useEffect, useState } from 'react';
import { useAuthStore, useProfileStore } from '../stores';
import { toast } from '../ui/toast';
import { FiUser, FiShield, FiCpu, FiEdit3, FiCamera, FiCheck, FiLock } from 'react-icons/fi';
import UploadImageModal from '../components/UploadImageModal';
import ChangePasswordModal from '../components/ChangePasswordModal';
import {
  Page, Container, Card, Button, Input, Avatar, Badge, Tabs, Switch, Skeleton, IconButton,
} from '../ui';

const STATUS_LABEL = { ACTIVE: 'Đang hoạt động', SUSPENDED: 'Tạm khóa', BANNED: 'Đã khóa' };

export default function Profile() {
  const user = useAuthStore((s) => s.user);
  const profile = useProfileStore((s) => s.profile);
  const updateProfile = useProfileStore((s) => s.updateProfile);
  const loading = useProfileStore((s) => s.loading);

  const [tab, setTab] = useState('personal');
  const [editing, setEditing] = useState(false);
  const [form, setForm] = useState({});
  const [imageModal, setImageModal] = useState(false);
  const [pwModal, setPwModal] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => { if (profile) setForm(profile); }, [profile]);

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const save = async () => {
    setSaving(true);
    try {
      await updateProfile({
        fullName: form.fullName, phoneNumber: form.phoneNumber, address: form.address,
        llmModel: form.llmModel, llmProvider: form.llmProvider,
      });
      setEditing(false);
      toast.success('Đã lưu hồ sơ');
    } catch { toast.error('Không thể lưu hồ sơ'); }
    finally { setSaving(false); }
  };

  const onImageUpdate = async (data) => {
    try { await updateProfile(data); toast.success('Đã cập nhật ảnh'); }
    catch { toast.error('Không thể cập nhật ảnh'); }
  };

  if (!profile && loading) {
    return <Page><Container className="py-8"><Card><Skeleton className="h-40 w-full" /></Card></Container></Page>;
  }

  const displayName = profile?.fullName || profile?.username || 'Người dùng';

  const sidebar = (
    <Card padded={false} className="overflow-hidden">
      <div className="relative h-24 gradient-brand">
        {profile?.heroBackgroundUrl && <img src={profile.heroBackgroundUrl} alt="" className="h-full w-full object-cover" />}
      </div>
      <div className="relative z-10 flex flex-col items-center px-4 pb-4 -mt-10">
        <div className="relative">
          <Avatar src={profile?.avatarUrl} name={displayName} size="xl" className="ring-4 ring-surface" />
          <IconButton aria-label="Đổi ảnh" size="sm" variant="primary" className="absolute -bottom-1 -right-1 shadow-md" onClick={() => setImageModal(true)}>
            <FiCamera size={14} />
          </IconButton>
        </div>
        <h2 className="mt-3 text-lg font-bold text-ink">{displayName}</h2>
        <p className="text-sm text-muted">@{profile?.username}</p>
        {profile?.isAdmin && <Badge variant="brand" className="mt-2">Quản trị viên</Badge>}
      </div>
      <nav className="border-t border-line p-2">
        {[
          { value: 'personal', label: 'Thông tin cá nhân', icon: <FiUser size={16} /> },
          { value: 'security', label: 'Bảo mật', icon: <FiShield size={16} /> },
          { value: 'ai', label: 'Tùy chỉnh AI', icon: <FiCpu size={16} /> },
        ].map((t) => (
          <button key={t.value} onClick={() => setTab(t.value)}
            className={`flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-base font-medium transition-colors ${tab === t.value ? 'bg-brand-soft text-brand-700' : 'text-ink-2 hover:bg-surface-2'}`}>
            {t.icon}{t.label}
          </button>
        ))}
      </nav>
    </Card>
  );

  return (
    <Page>
      <Container className="py-8">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-start">
          <aside className="w-full lg:w-72 lg:shrink-0">{sidebar}</aside>
          <div className="min-w-0 flex-1">
            {tab === 'personal' && (
              <Card>
                <Card.Header>
                  <Card.Title>Thông tin cá nhân</Card.Title>
                  {editing ? (
                    <div className="flex gap-2">
                      <Button size="sm" variant="ghost" onClick={() => { setForm(profile); setEditing(false); }}>Huỷ</Button>
                      <Button size="sm" loading={saving} iconLeft={<FiCheck size={15} />} onClick={save}>Lưu</Button>
                    </div>
                  ) : (
                    <Button size="sm" variant="outline" iconLeft={<FiEdit3 size={15} />} onClick={() => setEditing(true)}>Chỉnh sửa</Button>
                  )}
                </Card.Header>
                <div className="grid gap-4 sm:grid-cols-2">
                  <Input label="Họ và tên" value={form.fullName || ''} onChange={set('fullName')} disabled={!editing} />
                  <Input label="Tên người dùng" value={profile?.username || ''} disabled hint="Không thể thay đổi" />
                  <Input label="Email" value={user?.email || ''} disabled />
                  <Input label="Số điện thoại" value={form.phoneNumber || ''} onChange={set('phoneNumber')} disabled={!editing} />
                  <Input label="Địa chỉ" value={form.address || ''} onChange={set('address')} disabled={!editing} className="sm:col-span-2" />
                </div>
                <div className="mt-4 flex flex-wrap items-center gap-4 border-t border-line pt-4 text-sm text-muted">
                  <span>Trạng thái: <Badge size="sm" variant={profile?.accountStatus === 'ACTIVE' ? 'success' : 'warning'}>{STATUS_LABEL[profile?.accountStatus] || profile?.accountStatus}</Badge></span>
                  {profile?.createdAt && <span>Thành viên từ {new Date(profile.createdAt).toLocaleDateString('vi-VN')}</span>}
                </div>
              </Card>
            )}

            {tab === 'security' && (
              <Card className="flex flex-col gap-4">
                <Card.Header><Card.Title>Bảo mật</Card.Title></Card.Header>
                <div className="flex items-center justify-between rounded-lg border border-line p-4">
                  <div className="flex items-center gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-brand-soft text-brand-600"><FiLock size={18} /></div>
                    <div>
                      <div className="text-base font-semibold text-ink">Mật khẩu</div>
                      <div className="text-sm text-muted">Đổi mật khẩu đăng nhập của bạn</div>
                    </div>
                  </div>
                  <Button variant="outline" size="sm" onClick={() => setPwModal(true)}>Đổi mật khẩu</Button>
                </div>
              </Card>
            )}

            {tab === 'ai' && (
              <Card>
                <Card.Header>
                  <Card.Title>Tùy chỉnh AI</Card.Title>
                  {editing ? (
                    <Button size="sm" loading={saving} iconLeft={<FiCheck size={15} />} onClick={save}>Lưu</Button>
                  ) : (
                    <Button size="sm" variant="outline" iconLeft={<FiEdit3 size={15} />} onClick={() => setEditing(true)}>Chỉnh sửa</Button>
                  )}
                </Card.Header>
                <div className="grid gap-4 sm:grid-cols-2">
                  <Input label="Nhà cung cấp LLM" value={form.llmProvider || ''} onChange={set('llmProvider')} disabled={!editing} placeholder="deepseek" />
                  <Input label="Model" value={form.llmModel || ''} onChange={set('llmModel')} disabled={!editing} placeholder="deepseek-chat" />
                </div>
                <div className="mt-4 rounded-lg bg-surface-2 px-4 py-3 text-base">
                  Khóa API cá nhân: {profile?.hasLlmApiKey
                    ? <Badge variant="success">Đã thiết lập</Badge>
                    : <Badge variant="neutral">Chưa thiết lập</Badge>}
                  <span className="ml-1 text-sm text-muted">(khóa không bao giờ được hiển thị)</span>
                </div>
              </Card>
            )}
          </div>
        </div>
      </Container>

      <UploadImageModal isOpen={imageModal} onClose={() => setImageModal(false)} onConfirm={onImageUpdate} />
      <ChangePasswordModal isOpen={pwModal} onClose={() => setPwModal(false)} />
    </Page>
  );
}
