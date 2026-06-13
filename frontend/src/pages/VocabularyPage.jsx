import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FiBook, FiPlus, FiSearch, FiStar, FiEdit2, FiTrash2, FiVolume2, FiGrid, FiList } from 'react-icons/fi';
import { useVocabularyStore } from '../stores';
import { toast } from '../ui/toast';
import {
  Page, Container, Button, IconButton, Input, Textarea, Badge, Card,
  Tabs, Pagination, EmptyState, Skeleton, StatCard, Modal, ConfirmDialog,
} from '../ui';

const EMPTY = { word: '', translation: '', phonetic: '', partOfSpeech: '', definition: '', exampleSentence: '' };

function VocabModal({ open, onClose, onSave, initial }) {
  const [form, setForm] = useState(EMPTY);
  const [saving, setSaving] = useState(false);
  useEffect(() => { setForm(initial ? { ...EMPTY, ...initial } : EMPTY); }, [initial, open]);
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));

  const save = async () => {
    if (!form.word.trim()) return toast.error('Vui lòng nhập từ');
    setSaving(true);
    try { await onSave(form); onClose(); toast.success(initial ? 'Đã cập nhật từ' : 'Đã thêm từ mới'); }
    catch { toast.error('Không thể lưu từ. Thử lại.'); }
    finally { setSaving(false); }
  };

  return (
    <Modal open={open} onClose={onClose} title={initial ? 'Sửa từ vựng' : 'Thêm từ mới'} size="md"
      footer={<><Button variant="ghost" onClick={onClose}>Huỷ</Button><Button onClick={save} loading={saving}>Lưu</Button></>}>
      <div className="flex flex-col gap-3">
        <div className="grid grid-cols-2 gap-3">
          <Input label="Từ" value={form.word} onChange={set('word')} required />
          <Input label="Phiên âm" value={form.phonetic} onChange={set('phonetic')} placeholder="/ˈwɜːd/" />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <Input label="Nghĩa" value={form.translation} onChange={set('translation')} />
          <Input label="Loại từ" value={form.partOfSpeech} onChange={set('partOfSpeech')} placeholder="noun, verb…" />
        </div>
        <Textarea label="Định nghĩa" value={form.definition} onChange={set('definition')} rows={2} />
        <Textarea label="Ví dụ" value={form.exampleSentence} onChange={set('exampleSentence')} rows={2} />
      </div>
    </Modal>
  );
}

function VocabCard({ v, onEdit, onDelete, onToggle }) {
  return (
    <Card padded className="flex flex-col gap-2">
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <h3 className="truncate text-lg font-bold text-ink">{v.word}</h3>
            {v.partOfSpeech && <Badge variant="neutral" size="sm">{v.partOfSpeech}</Badge>}
          </div>
          {v.phonetic && <span className="text-sm text-muted">{v.phonetic}</span>}
        </div>
        <IconButton aria-label={v.isMastered ? 'Bỏ đánh dấu thuộc' : 'Đánh dấu đã thuộc'} size="sm"
          onClick={() => onToggle(v.id)} className={v.isMastered ? 'text-warning' : 'text-faint'}>
          <FiStar size={16} fill={v.isMastered ? 'currentColor' : 'none'} />
        </IconButton>
      </div>
      {v.translation && <p className="text-base font-semibold text-brand-700">{v.translation}</p>}
      {v.definition && <p className="text-base text-ink-2 line-clamp-2">{v.definition}</p>}
      {v.exampleSentence && <p className="text-sm italic text-muted line-clamp-2">“{v.exampleSentence}”</p>}
      <div className="mt-1 flex items-center justify-end gap-1 border-t border-line pt-2">
        <IconButton aria-label="Sửa" size="sm" onClick={() => onEdit(v)}><FiEdit2 size={14} /></IconButton>
        <IconButton aria-label="Xoá" size="sm" className="text-danger" onClick={() => onDelete(v.id)}><FiTrash2 size={14} /></IconButton>
      </div>
    </Card>
  );
}

function VocabRow({ v, onEdit, onDelete, onToggle }) {
  return (
    <Card padded className="flex items-center gap-4">
      <IconButton aria-label={v.isMastered ? 'Bỏ đánh dấu thuộc' : 'Đánh dấu đã thuộc'} size="sm"
        onClick={() => onToggle(v.id)} className={v.isMastered ? 'text-warning' : 'text-faint'}>
        <FiStar size={16} fill={v.isMastered ? 'currentColor' : 'none'} />
      </IconButton>
      <div className="flex min-w-0 flex-1 flex-wrap items-baseline gap-x-2 gap-y-0.5">
        <h3 className="text-base font-bold text-ink">{v.word}</h3>
        {v.phonetic && <span className="text-sm text-muted">{v.phonetic}</span>}
        {v.partOfSpeech && <Badge variant="neutral" size="sm">{v.partOfSpeech}</Badge>}
        {v.translation && <span className="text-base font-semibold text-brand-700">{v.translation}</span>}
        {v.definition && <span className="w-full truncate text-sm text-muted">{v.definition}</span>}
      </div>
      <div className="flex shrink-0 items-center gap-1">
        <IconButton aria-label="Sửa" size="sm" onClick={() => onEdit(v)}><FiEdit2 size={14} /></IconButton>
        <IconButton aria-label="Xoá" size="sm" className="text-danger" onClick={() => onDelete(v.id)}><FiTrash2 size={14} /></IconButton>
      </div>
    </Card>
  );
}

export default function VocabularyPage() {
  const {
    vocabulary, stats, loading, page, totalPages, search, filter,
    fetchVocabulary, fetchStats, addWord, updateWord, deleteWord, toggleMastered, setPage, setSearch, setFilter,
  } = useVocabularyStore();

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [deletingId, setDeletingId] = useState(null);
  const [view, setView] = useState('card');

  useEffect(() => { fetchVocabulary(); fetchStats(); }, [fetchVocabulary, fetchStats]);
  useEffect(() => {
    const t = setTimeout(() => fetchVocabulary(), 400);
    return () => clearTimeout(t);
  }, [search]); // eslint-disable-line react-hooks/exhaustive-deps

  const total = stats?.total ?? stats?.totalWords ?? 0;
  const mastered = stats?.mastered ?? stats?.masteredWords ?? 0;

  const onSave = (data) => (editing ? updateWord(editing.id, data) : addWord(data));

  return (
    <Page>
      <Container className="py-8">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-brand-soft text-brand-600"><FiBook size={22} /></div>
            <div>
              <h1 className="text-2xl font-bold text-ink">Sổ tay từ vựng</h1>
              <p className="text-base text-muted">Quản lý và ôn tập các từ đã lưu</p>
            </div>
          </div>
          <Button iconLeft={<FiPlus size={16} />} onClick={() => { setEditing(null); setModalOpen(true); }}>Thêm từ mới</Button>
        </div>

        <div className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-3">
          <StatCard icon={<FiBook />} label="Tổng từ" value={total} />
          <StatCard icon={<FiStar />} label="Đã thuộc" value={mastered} />
          <StatCard icon={<FiStar />} label="Đang học" value={Math.max(0, total - mastered)} />
        </div>

        <div className="mt-5 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <Tabs
            variant="pill"
            value={filter}
            onChange={setFilter}
            items={[
              { value: 'all', label: 'Tất cả' },
              { value: 'unmastered', label: 'Chưa thuộc' },
              { value: 'mastered', label: 'Đã thuộc' },
            ]}
          />
          <div className="flex items-center gap-3">
            <div className="flex shrink-0 items-center gap-0.5 rounded-lg border border-line bg-surface p-0.5">
              <motion.button
                type="button"
                whileTap={{ scale: 0.9 }}
                aria-label="Xem dạng thẻ"
                aria-pressed={view === 'card'}
                onClick={() => setView('card')}
                className={`flex h-8 w-8 items-center justify-center rounded-md transition-colors ${view === 'card' ? 'bg-brand-soft text-brand-700' : 'text-muted hover:text-ink-2'}`}
              >
                <FiGrid size={16} />
              </motion.button>
              <motion.button
                type="button"
                whileTap={{ scale: 0.9 }}
                aria-label="Xem dạng danh sách"
                aria-pressed={view === 'list'}
                onClick={() => setView('list')}
                className={`flex h-8 w-8 items-center justify-center rounded-md transition-colors ${view === 'list' ? 'bg-brand-soft text-brand-700' : 'text-muted hover:text-ink-2'}`}
              >
                <FiList size={16} />
              </motion.button>
            </div>
            <div className="w-full lg:w-64">
              <Input placeholder="Tìm từ vựng…" value={search} onChange={(e) => setSearch(e.target.value)} iconLeft={<FiSearch size={16} />} />
            </div>
          </div>
        </div>

        <div className="mt-5">
          {loading && vocabulary.length === 0 ? (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {Array.from({ length: 6 }).map((_, i) => <Card key={i}><Skeleton className="h-5 w-1/2" /><Skeleton className="mt-2 h-3 w-full" /><Skeleton className="mt-2 h-3 w-2/3" /></Card>)}
            </div>
          ) : vocabulary.length === 0 ? (
            <EmptyState icon="📖" title="Chưa có từ nào" description="Bắt đầu xây dựng sổ tay của bạn bằng cách thêm từ mới hoặc lưu từ khi luyện đọc."
              action={<Button iconLeft={<FiPlus size={16} />} onClick={() => { setEditing(null); setModalOpen(true); }}>Thêm từ đầu tiên</Button>} />
          ) : (
            <AnimatePresence mode="wait" initial={false}>
              <motion.div
                key={view}
                className={view === 'list' ? 'flex flex-col gap-2.5' : 'grid gap-4 sm:grid-cols-2 lg:grid-cols-3'}
                initial={{ opacity: 0, y: 8, scale: 0.99 }}
                animate={{ opacity: 1, y: 0, scale: 1, transition: { duration: 0.22, ease: 'easeOut', staggerChildren: 0.035 } }}
                exit={{ opacity: 0, y: -8, scale: 0.99, transition: { duration: 0.14, ease: 'easeIn' } }}
              >
                {vocabulary.map((v) => (
                  <motion.div
                    key={v.id}
                    layout
                    variants={{ hidden: { opacity: 0, y: 12 }, visible: { opacity: 1, y: 0 } }}
                    initial="hidden"
                    animate="visible"
                  >
                    {view === 'list'
                      ? <VocabRow v={v} onEdit={(x) => { setEditing(x); setModalOpen(true); }} onDelete={setDeletingId} onToggle={toggleMastered} />
                      : <VocabCard v={v} onEdit={(x) => { setEditing(x); setModalOpen(true); }} onDelete={setDeletingId} onToggle={toggleMastered} />}
                  </motion.div>
                ))}
              </motion.div>
            </AnimatePresence>
          )}

          {totalPages > 1 && <Pagination className="mt-6" page={page} totalPages={totalPages} onPageChange={(p) => { setPage(p); window.scrollTo({ top: 0, behavior: 'smooth' }); }} />}
        </div>
      </Container>

      <VocabModal open={modalOpen} onClose={() => setModalOpen(false)} onSave={onSave} initial={editing} />
      <ConfirmDialog
        open={deletingId != null}
        onClose={() => setDeletingId(null)}
        onConfirm={async () => { await deleteWord(deletingId); setDeletingId(null); toast.success('Đã xoá từ'); }}
        title="Xoá từ vựng"
        message="Bạn có chắc muốn xoá từ này khỏi sổ tay?"
        confirmLabel="Xoá"
        variant="danger"
      />
    </Page>
  );
}
