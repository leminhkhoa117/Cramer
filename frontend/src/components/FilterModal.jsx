import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import '../css/FilterModal.css';

const FilterModal = ({ isOpen, onClose, onApply, availableFilters, currentFilters }) => {
    const [selectedFilters, setSelectedFilters] = useState(currentFilters);

    useEffect(() => {
        setSelectedFilters(currentFilters);
    }, [currentFilters]);

    const handleCheckboxChange = (group, value) => {
        setSelectedFilters(prev => {
            const newGroupValues = new Set(prev[group] || []);
            if (newGroupValues.has(value)) {
                newGroupValues.delete(value);
            } else {
                newGroupValues.add(value);
            }
            return { ...prev, [group]: Array.from(newGroupValues) };
        });
    };

    const handleApply = () => {
        onApply(selectedFilters);
        onClose();
    };

    const handleClear = () => {
        setSelectedFilters({});
        onApply({});
        onClose();
    };

    if (!isOpen) {
        return null;
    }

    return (
        <AnimatePresence>
            <div className="filter-modal-backdrop" onClick={onClose}>
                <motion.div
                    className="filter-modal-content"
                    onClick={(e) => e.stopPropagation()}
                    initial={{ y: -50, opacity: 0 }}
                    animate={{ y: 0, opacity: 1 }}
                    exit={{ y: 50, opacity: 0 }}
                    transition={{ type: 'spring', stiffness: 300, damping: 30 }}
                >
                    <h2 className="filter-modal-title">Lọc kết quả</h2>
                    <div className="filter-modal-body">
                        {Object.entries(availableFilters).map(([groupKey, groupDetails]) => (
                            <div key={groupKey} className="filter-group">
                                <h3 className="filter-group-title">{groupDetails.label}</h3>
                                <div className="filter-options">
                                    {groupDetails.options.map(option => (
                                        <div key={option.value} className="filter-option">
                                            <input
                                                type="checkbox"
                                                id={`filter-${groupKey}-${option.value}`}
                                                checked={selectedFilters[groupKey]?.includes(option.value) || false}
                                                onChange={() => handleCheckboxChange(groupKey, option.value)}
                                            />
                                            <label htmlFor={`filter-${groupKey}-${option.value}`}>
                                                {option.label}
                                            </label>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        ))}
                    </div>
                    <div className="filter-modal-footer">
                        <button type="button" className="btn btn-secondary" onClick={handleClear}>
                            Xóa bộ lọc
                        </button>
                        <button type="button" className="btn btn-primary" onClick={handleApply}>
                            Áp dụng
                        </button>
                    </div>
                </motion.div>
            </div>
        </AnimatePresence>
    );
};

export default FilterModal;
