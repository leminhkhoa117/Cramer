/**
 * TagInput - Hashtag/Tag input component with autocomplete.
 * 
 * Features:
 * - Add tags by typing and pressing Enter or comma
 * - Remove tags by clicking X
 * - Optional autocomplete suggestions
 * - Keyboard navigation
 * 
 * @since 2025-12-21 - Cat B Feature
 */

import React, { useState, useRef, useCallback, useEffect } from 'react';
import { FiX, FiHash } from 'react-icons/fi';
import './TagInput.css';

// Common IELTS topic hashtags for suggestions
const COMMON_HASHTAGS = [
    'climate_change', 'technology', 'education', 'health', 'environment',
    'globalization', 'urbanization', 'culture', 'science', 'economics',
    'transportation', 'energy', 'wildlife', 'agriculture', 'communication',
    'social_media', 'artificial_intelligence', 'sustainability', 'migration',
    'employment', 'tourism', 'architecture', 'psychology', 'history',
    'language', 'innovation', 'space_exploration', 'biodiversity', 'nutrition',
    'mental_health', 'renewable_energy', 'digital_revolution', 'workplace'
];

export default function TagInput({
    value = [],
    onChange,
    placeholder = 'Add hashtags...',
    maxTags = 10,
    suggestions = COMMON_HASHTAGS,
    disabled = false,
    showSuggestions = true,
    label,
    helperText
}) {
    const [inputValue, setInputValue] = useState('');
    const [isFocused, setIsFocused] = useState(false);
    const [filteredSuggestions, setFilteredSuggestions] = useState([]);
    const [selectedSuggestionIndex, setSelectedSuggestionIndex] = useState(-1);
    const inputRef = useRef(null);
    const containerRef = useRef(null);

    // Filter suggestions based on input
    useEffect(() => {
        if (inputValue.trim() && showSuggestions) {
            const filtered = suggestions
                .filter(tag =>
                    tag.toLowerCase().includes(inputValue.toLowerCase()) &&
                    !value.includes(tag)
                )
                .slice(0, 6);
            setFilteredSuggestions(filtered);
            setSelectedSuggestionIndex(-1);
        } else {
            setFilteredSuggestions([]);
        }
    }, [inputValue, suggestions, value, showSuggestions]);

    // Normalize tag (lowercase, replace spaces with underscores)
    const normalizeTag = useCallback((tag) => {
        return tag
            .toLowerCase()
            .trim()
            .replace(/\s+/g, '_')
            .replace(/[^a-z0-9_]/g, '')
            .slice(0, 30);
    }, []);

    // Add a tag
    const addTag = useCallback((tag) => {
        const normalized = normalizeTag(tag);

        if (!normalized) return;
        if (value.includes(normalized)) return;
        if (value.length >= maxTags) return;

        onChange([...value, normalized]);
        setInputValue('');
        setFilteredSuggestions([]);
    }, [value, onChange, maxTags, normalizeTag]);

    // Remove a tag
    const removeTag = useCallback((tagToRemove) => {
        onChange(value.filter(tag => tag !== tagToRemove));
    }, [value, onChange]);

    // Handle key press
    const handleKeyDown = useCallback((e) => {
        if (e.key === 'Enter' || e.key === ',') {
            e.preventDefault();

            // If suggestion is selected, add it
            if (selectedSuggestionIndex >= 0 && filteredSuggestions[selectedSuggestionIndex]) {
                addTag(filteredSuggestions[selectedSuggestionIndex]);
            } else if (inputValue.trim()) {
                addTag(inputValue);
            }
        } else if (e.key === 'Backspace' && !inputValue && value.length > 0) {
            // Remove last tag on backspace if input is empty
            removeTag(value[value.length - 1]);
        } else if (e.key === 'ArrowDown') {
            e.preventDefault();
            setSelectedSuggestionIndex(prev =>
                Math.min(prev + 1, filteredSuggestions.length - 1)
            );
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            setSelectedSuggestionIndex(prev => Math.max(prev - 1, -1));
        } else if (e.key === 'Escape') {
            setFilteredSuggestions([]);
            setSelectedSuggestionIndex(-1);
        }
    }, [inputValue, value, addTag, removeTag, filteredSuggestions, selectedSuggestionIndex]);

    // Handle input change
    const handleInputChange = (e) => {
        const newValue = e.target.value;

        // Check for comma to add tag
        if (newValue.includes(',')) {
            const tags = newValue.split(',');
            tags.forEach((tag, index) => {
                if (index < tags.length - 1 && tag.trim()) {
                    addTag(tag);
                }
            });
            setInputValue(tags[tags.length - 1]);
        } else {
            setInputValue(newValue);
        }
    };

    // Handle suggestion click
    const handleSuggestionClick = (suggestion) => {
        addTag(suggestion);
        inputRef.current?.focus();
    };

    // Close suggestions on click outside
    useEffect(() => {
        const handleClickOutside = (e) => {
            if (containerRef.current && !containerRef.current.contains(e.target)) {
                setFilteredSuggestions([]);
                setIsFocused(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    const canAddMore = value.length < maxTags;

    return (
        <div className="tag-input-wrapper" ref={containerRef}>
            {label && (
                <label className="tag-input-label">
                    <FiHash size={14} />
                    {label}
                </label>
            )}

            <div
                className={`tag-input-container ${isFocused ? 'focused' : ''} ${disabled ? 'disabled' : ''}`}
                onClick={() => inputRef.current?.focus()}
            >
                {/* Tags */}
                <div className="tags-list">
                    {value.map((tag, index) => (
                        <span key={tag} className="tag">
                            <span className="tag-hash">#</span>
                            <span className="tag-text">{tag}</span>
                            {!disabled && (
                                <button
                                    type="button"
                                    className="tag-remove"
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        removeTag(tag);
                                    }}
                                >
                                    <FiX size={12} />
                                </button>
                            )}
                        </span>
                    ))}

                    {/* Input */}
                    {canAddMore && !disabled && (
                        <input
                            ref={inputRef}
                            type="text"
                            className="tag-input"
                            value={inputValue}
                            onChange={handleInputChange}
                            onKeyDown={handleKeyDown}
                            onFocus={() => setIsFocused(true)}
                            placeholder={value.length === 0 ? placeholder : ''}
                            disabled={disabled}
                        />
                    )}
                </div>
            </div>

            {/* Suggestions dropdown */}
            {filteredSuggestions.length > 0 && isFocused && (
                <div className="tag-suggestions">
                    {filteredSuggestions.map((suggestion, index) => (
                        <button
                            key={suggestion}
                            type="button"
                            className={`suggestion-item ${index === selectedSuggestionIndex ? 'selected' : ''}`}
                            onClick={() => handleSuggestionClick(suggestion)}
                        >
                            <FiHash size={12} />
                            {suggestion}
                        </button>
                    ))}
                </div>
            )}

            {/* Helper text and count */}
            <div className="tag-input-footer">
                {helperText && (
                    <span className="helper-text">{helperText}</span>
                )}
                <span className={`tag-count ${value.length >= maxTags ? 'max-reached' : ''}`}>
                    {value.length}/{maxTags}
                </span>
            </div>
        </div>
    );
}
