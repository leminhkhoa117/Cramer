import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  FiBook,
  FiPlus,
  FiSearch,
  FiStar,
  FiGrid,
  FiList,
  FiLoader,
  FiAlertCircle,
  FiBookOpen,
} from 'react-icons/fi';
import { useVocabularyStore } from '../stores';
import VocabularyCard from '../components/VocabularyCard';
import VocabularyModal from '../components/VocabularyModal';
import Pagination from '../components/Pagination';
import '../css/vocabulary-page.css';

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.05 },
  },
};

const itemVariants = {
  hidden: { y: 20, opacity: 0 },
  visible: {
    y: 0,
    opacity: 1,
    transition: { duration: 0.3 },
  },
};

const VocabularyPage = () => {
  const {
    vocabulary,
    stats,
    loading,
    error,
    currentPage,
    totalPages,
    searchQuery,
    filter,
    fetchVocabulary,
    fetchStats,
    addWord,
    updateWord,
    deleteWord,
    toggleMastered,
    setPage,
    setSearchQuery,
    setDebouncedSearchQuery,
    setFilter,
    clearError,
  } = useVocabularyStore();

  const [viewMode, setViewMode] = useState('grid');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingVocabulary, setEditingVocabulary] = useState(null);
  const [deletingId, setDeletingId] = useState(null);

  useEffect(() => {
    fetchVocabulary();
    fetchStats();
  }, [fetchVocabulary, fetchStats]);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchQuery(searchQuery);
    }, 500);
    return () => clearTimeout(timer);
  }, [searchQuery, setDebouncedSearchQuery]);

  const handleSearchChange = useCallback(
    (e) => {
      setSearchQuery(e.target.value);
    },
    [setSearchQuery],
  );

  const handleFilterChange = useCallback(
    (newFilter) => {
      setFilter(newFilter);
    },
    [setFilter],
  );

  const handleAddNew = useCallback(() => {
    setEditingVocabulary(null);
    setIsModalOpen(true);
  }, []);

  const handleEdit = useCallback((vocab) => {
    setEditingVocabulary(vocab);
    setIsModalOpen(true);
  }, []);

  const handleCloseModal = useCallback(() => {
    setIsModalOpen(false);
    setEditingVocabulary(null);
  }, []);

  const handleSaveWord = useCallback(
    async (wordData) => {
      if (editingVocabulary) {
        await updateWord(editingVocabulary.id, wordData);
      } else {
        await addWord(wordData);
      }
    },
    [editingVocabulary, updateWord, addWord],
  );

  const handleDelete = useCallback(
    async (id) => {
      if (window.confirm('Bạn có chắc muốn xóa từ này khỏi sổ tay?')) {
        setDeletingId(id);
        try {
          await deleteWord(id);
        } finally {
          setDeletingId(null);
        }
      }
    },
    [deleteWord],
  );

  const handleToggleMastered = useCallback(
    async (id) => {
      await toggleMastered(id);
    },
    [toggleMastered],
  );

  const handlePageChange = useCallback(
    (newPage) => {
      setPage(newPage);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    },
    [setPage],
  );

  const statsDisplay = useMemo(() => {
    if (!stats) return { total: 0, mastered: 0, unmastered: 0 };
    return {
      total: stats.total || 0,
      mastered: stats.mastered || 0,
      unmastered: stats.learning || (stats.total || 0) - (stats.mastered || 0),
    };
  }, [stats]);

  const filterTabs = [
    { id: 'all', label: 'Tất cả', count: statsDisplay.total },
    { id: 'unmastered', label: 'Chưa thuộc', count: statsDisplay.unmastered },
    { id: 'mastered', label: 'Đã thuộc', count: statsDisplay.mastered },
  ];

  return (
    <div className="vocab-page">
      <div className="vocab-page__bg">
        <div className="vocab-page__orb vocab-page__orb--1" />
        <div className="vocab-page__orb vocab-page__orb--2" />
      </div>

      <div className="vocab-page__container">
        <motion.header
          className="vocab-page__header"
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
        >
          <div className="vocab-page__header-left">
            <div className="vocab-page__header-icon">
              <FiBook />
            </div>
            <div className="vocab-page__header-text">
              <h1 className="vocab-page__header-title">Sổ tay Từ vựng</h1>
              <p className="vocab-page__header-subtitle">
                Quản lý và học các từ vựng đã lưu
              </p>
            </div>
          </div>
          <div className="vocab-page__header-right">
            <div className="vocab-page__stats">
              <div className="vocab-page__stat">
                <span className="vocab-page__stat-value">{statsDisplay.total}</span>
                <span className="vocab-page__stat-label">Tổng từ</span>
              </div>
              <div className="vocab-page__stat vocab-page__stat--mastered">
                <FiStar className="vocab-page__stat-star" />
                <span className="vocab-page__stat-value">{statsDisplay.mastered}</span>
                <span className="vocab-page__stat-label">Đã thuộc</span>
              </div>
            </div>
            <button className="vocab-page__add-btn" onClick={handleAddNew}>
              <FiPlus />
              <span>Thêm từ mới</span>
            </button>
          </div>
        </motion.header>

        <motion.div
          className="vocab-page__controls"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.1 }}
        >
          <div className="vocab-page__search">
            <FiSearch className="vocab-page__search-icon" />
            <input
              type="text"
              className="vocab-page__search-input"
              placeholder="Tìm kiếm từ vựng..."
              value={searchQuery}
              onChange={handleSearchChange}
            />
          </div>

          <div className="vocab-page__filters">
            {filterTabs.map((tab) => (
              <button
                key={tab.id}
                className={`vocab-page__filter-btn${
                  filter === tab.id ? ' vocab-page__filter-btn--active' : ''
                }`}
                onClick={() => handleFilterChange(tab.id)}
              >
                <span>{tab.label}</span>
                <span className="vocab-page__filter-count">{tab.count}</span>
              </button>
            ))}
          </div>

          <div className="vocab-page__view-toggle">
            <button
              className={`vocab-page__view-btn${
                viewMode === 'grid' ? ' vocab-page__view-btn--active' : ''
              }`}
              onClick={() => setViewMode('grid')}
              aria-label="Xem dạng lưới"
            >
              <FiGrid />
            </button>
            <button
              className={`vocab-page__view-btn${
                viewMode === 'list' ? ' vocab-page__view-btn--active' : ''
              }`}
              onClick={() => setViewMode('list')}
              aria-label="Xem dạng danh sách"
            >
              <FiList />
            </button>
          </div>
        </motion.div>

        {error && (
          <motion.div
            className="vocab-page__error"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <FiAlertCircle />
            <span>{error}</span>
            <button onClick={clearError}>Đóng</button>
          </motion.div>
        )}

        {loading && vocabulary.length === 0 && (
          <div className="vocab-page__loading">
            <FiLoader className="vocab-page__loading-icon" />
            <span>Đang tải từ vựng...</span>
          </div>
        )}

        {!loading && vocabulary.length === 0 ? (
          <motion.div
            className="vocab-page__empty"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.4 }}
          >
            <div className="vocab-page__empty-icon">
              <FiBookOpen />
            </div>
            <h3 className="vocab-page__empty-title">
              {searchQuery ? 'Không tìm thấy từ vựng' : 'Chưa có từ vựng nào'}
            </h3>
            <p className="vocab-page__empty-text">
              {searchQuery
                ? 'Thử tìm kiếm với từ khóa khác'
                : 'Thêm từ đầu tiên của bạn để bắt đầu học!'}
            </p>
            {!searchQuery && (
              <button className="vocab-page__empty-btn" onClick={handleAddNew}>
                <FiPlus />
                <span>Thêm từ mới</span>
              </button>
            )}
          </motion.div>
        ) : (
          <>
            <motion.div
              className={`vocab-page__grid${
                viewMode === 'list' ? ' vocab-page__grid--list' : ''
              }`}
              variants={containerVariants}
              initial="hidden"
              animate="visible"
            >
              <AnimatePresence mode="popLayout">
                {vocabulary.map((vocab) => (
                  <motion.div key={vocab.id} variants={itemVariants} layout>
                    <VocabularyCard
                      vocabulary={vocab}
                      onEdit={handleEdit}
                      onDelete={handleDelete}
                      onToggleMastered={handleToggleMastered}
                      isDeleting={deletingId === vocab.id}
                    />
                  </motion.div>
                ))}
              </AnimatePresence>
            </motion.div>

            {totalPages > 1 && (
              <div className="vocab-page__pagination">
                <Pagination
                  currentPage={currentPage}
                  totalPages={totalPages}
                  onPageChange={handlePageChange}
                />
              </div>
            )}
          </>
        )}
      </div>

      <VocabularyModal
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        vocabulary={editingVocabulary}
        onSave={handleSaveWord}
      />
    </div>
  );
};

export default VocabularyPage;