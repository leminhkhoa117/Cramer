import { useState, useEffect } from 'react';
import BaseModal from './common/BaseModal';

/**
 * FilterModal - Filter course results
 * 
 * @param {boolean} isOpen - Modal visibility
 * @param {function} onClose - Close handler
 * @param {function} onApply - Apply filters callback
 * @param {object} availableFilters - Available filter options
 * @param {object} currentFilters - Currently selected filters
 */
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

  return (
    <BaseModal
      isOpen={isOpen}
      onClose={onClose}
      title="Lọc kết quả"
      showCloseButton={true}
      size="lg"
      footer={
        <>
          <button 
            type="button" 
            className="cm-btn cm-btn--secondary" 
            onClick={handleClear}
          >
            Xóa bộ lọc
          </button>
          <button 
            type="button" 
            className="cm-btn cm-btn--primary" 
            onClick={handleApply}
          >
            Áp dụng
          </button>
        </>
      }
    >
      {Object.entries(availableFilters).map(([groupKey, groupDetails]) => (
        <div key={groupKey} className="cm-filter-group">
          <h3 className="cm-filter-title">{groupDetails.label}</h3>
          <div className="cm-filter-options">
            {groupDetails.options.map(option => (
              <div key={option.value} className="cm-filter-option">
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
    </BaseModal>
  );
};

export default FilterModal;
