import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  FiBook, 
  FiPlus, 
  FiSearch, 
  FiStar, 
  FiGrid, 
  FiList,
  FiFilter,
  FiLoader,
  FiAlertCircle,
  FiBookOpen,
} from 'react-icons/fi';
import { useVocabularyStore } from '../stores';
import VocabularyCard from '../components/VocabularyCard';
import VocabularyModal from '../components/VocabularyModal';
import Pagination from '../components/Pagination';
import '../css/vocabulary-page.css';

// Animation variants
const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.05,
    },
  },
};

const itemVariants = {
  hidden: { y: 20, opacity: 0 },
  visible: {
    y: 0,
    opacity: 1,
    transition: {
      duration: 0.3,
    },
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
    totalElements,
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

  const [viewMode, setViewMode] = useState('grid'); // 'grid' or 'list'
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingVocabulary, setEditingVocabulary] = useState(null);
  const [deletingId, setDeletingId] = useState(null);

  // Fetch data on mount
  useEffect(() => {
    fetchVocabulary();
    fetchStats();
  }, [fetchVocabulary, fetchStats]);

  // Debounce search
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchQuery(searchQuery);
    }, 500);
    return () => clearTimeout(timer);
  }, [searchQuery, setDebouncedSearchQuery]);

  // Handle search change
  const handleSearchChange = useCallback((e) => {
    setSearchQuery(e.target.value);
  }, [setSearchQuery]);

  // Handle filter change
  const handleFilterChange = useCallback((newFilter) => {
    setFilter(newFilter);
  }, [setFilter]);

  // Open modal for adding new word
  const handleAddNew = useCallback(() => {
    setEditingVocabulary(null);
    setIsModalOpen(true);
  }, []);

  // Open modal for editing
  const handleEdit = useCallback((vocab) => {
    setEditingVocabulary(vocab);
    setIsModalOpen(true);
  }, []);

  // Close modal
  const handleCloseModal = useCallback(() => {
    setIsModalOpen(false);
    setEditingVocabulary(null);
  }, []);

  // Save word (add or update)
  const handleSaveWord = useCallback(async (wordData) => {
    if (editingVocabulary) {
      await updateWord(editingVocabulary.id, wordData);
    } else {
      await addWord(wordData);
    }
  }, [editingVocabulary, updateWord, addWord]);

  // Delete word
  const handleDelete = useCallback(async (id) => {
    if (window.confirm('Bạn có chắc muốn xóa từ này khỏi sổ tay?')) {
      setDeletingId(id);
      try {
        await deleteWord(id);
      } finally {
        setDeletingId(null);
      }
    }
  }, [deleteWord]);

  // Toggle mastered
  const handleToggleMastered = useCallback(async (id) => {
    await toggleMastered(id);
  }, [toggleMastered]);

  // Page change
  const handlePageChange = useCallback((newPage) => {
    setPage(newPage);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }, [setPage]);

  // Stats display - using correct field names from backend API
  const statsDisplay = useMemo(() => {
    if (!stats) return { total: 0, mastered: 0, unmastered: 0 };
    return {
      total: stats.total || 0,
      mastered: stats.mastered || 0,
      unmastered: stats.learning || (stats.total || 0) - (stats.mastered || 0),
    };
  }, [stats]);

  // Filter tabs
  const filterTabs = [
    { id: 'all', label: 'Tất cả', count: statsDisplay.total },
    { id: 'unmastered', label: 'Chưa thuộc', count: statsDisplay.unmastered },
    { id: 'mastered', label: 'Đã thuộc', count: statsDisplay.mastered },
  ];

  return (
    <div className="vocabulary-page">
      {/* Background decoration */}
      <div className="vocabulary-page__bg">
        <div className="vocabulary-page__orb vocabulary-page__orb--1" />
        <div className="vocabulary-page__orb vocabulary-page__orb--2" />
      </div>

      <div className="vocabulary-page__container">
        {/* Header Section */}
        <motion.header 
          className="vocabulary-page__header"
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4 }}
        >
          <div className="vocabulary-page__title-section">
            <div className="vocabulary-page__icon">
              <FiBook />
            </div>
            <div>
              <h1 className="vocabulary-page__title">Sổ tay Từ vựng</h1>
              <p className="vocabulary-page__subtitle">
                Quản lý và học các từ vựng đã lưu
              </p>
            </div>
          </div>

          {/* Stats Cards */}
          <div className="vocabulary-page__stats">
            <div className="vocabulary-page__stat">
              <span className="vocabulary-page__stat-value">{statsDisplay.total}</span>
              <span className="vocabulary-page__stat-label">Tổng từ</span>
            </div>
            <div className="vocabulary-page__stat vocabulary-page__stat--mastered">
              <FiStar className="vocabulary-page__stat-icon" />
              <span className="vocabulary-page__stat-value">{statsDisplay.mastered}</span>
              <span className="vocabulary-page__stat-label">Đã thuộc</span>
            </div>
            <button 
              className="vocabulary-page__add-btn"
              onClick={handleAddNew}
            >
              <FiPlus />
              <span>Thêm từ mới</span>
            </button>
          </div>
        </motion.header>

        {/* Controls Section */}
        <motion.div 
          className="vocabulary-page__controls"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.4, delay: 0.1 }}
        >
          {/* Search Bar */}
          <div className="vocabulary-page__search">
            <FiSearch className="vocabulary-page__search-icon" />
            <input
              type="text"
              className="vocabulary-page__search-input"
              placeholder="Tìm kiếm từ vựng..."
              value={searchQuery}
              onChange={handleSearchChange}
            />
          </div>

          {/* Filter Tabs */}
          <div className="vocabulary-page__filters">
            {filterTabs.map((tab) => (
              <button
                key={tab.id}
                className={`vocabulary-page__filter-btn ${filter === tab.id ? 'vocabulary-page__filter-btn--active' : ''}`}
                onClick={() => handleFilterChange(tab.id)}
              >
                <span className="vocabulary-page__filter-label">{tab.label}</span>
                <span className="vocabulary-page__filter-count">{tab.count}</span>
              </button>
            ))}
          </div>

          {/* View Toggle */}
          <div className="vocabulary-page__view-toggle">
            <button
              className={`vocabulary-page__view-btn ${viewMode === 'grid' ? 'vocabulary-page__view-btn--active' : ''}`}
              onClick={() => setViewMode('grid')}
              aria-label="Xem dạng lưới"
            >
              <FiGrid />
            </button>
            <button
              className={`vocabulary-page__view-btn ${viewMode === 'list' ? 'vocabulary-page__view-btn--active' : ''}`}
              onClick={() => setViewMode('list')}
              aria-label="Xem dạng danh sách"
            >
              <FiList />
            </button>
          </div>
        </motion.div>

        {/* Error Display */}
        {error && (
          <motion.div 
            className="vocabulary-page__error"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <FiAlertCircle />
            <span>{error}</span>
            <button onClick={clearError}>Đóng</button>
          </motion.div>
        )}

        {/* Loading State */}
        {loading && vocabulary.length === 0 && (
          <div className="vocabulary-page__loading">
            <FiLoader className="vocabulary-page__loading-icon" />
            <span>Đang tải từ vựng...</span>
          </div>
        )}

        {/* Vocabulary Grid/List */}
        {!loading && vocabulary.length === 0 ? (
          <motion.div 
            className="vocabulary-page__empty"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.4 }}
          >
            <div className="vocabulary-page__empty-icon">
              <FiBookOpen />
            </div>
            <h3 className="vocabulary-page__empty-title">
              {searchQuery ? 'Không tìm thấy từ vựng' : 'Chưa có từ vựng nào'}
            </h3>
            <p className="vocabulary-page__empty-text">
              {searchQuery 
                ? 'Thử tìm kiếm với từ khóa khác'
                : 'Thêm từ đầu tiên của bạn để bắt đầu học!'}
            </p>
            {!searchQuery && (
              <button 
                className="vocabulary-page__empty-btn"
                onClick={handleAddNew}
              >
                <FiPlus />
                <span>Thêm từ mới</span>
              </button>
            )}
          </motion.div>
        ) : (
          <>
            <motion.div 
              className={`vocabulary-page__grid ${viewMode === 'list' ? 'vocabulary-page__grid--list' : ''}`}
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

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="vocabulary-page__pagination">
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

      {/* Add/Edit Modal */}
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
