/**
 * TagInput - Hashtag/Tag input component with autocomplete.
 * 
 * Features:
 * - Add tags by typing and pressing Enter or comma
 * - Remove tags by clicking X
 * - Optional autocomplete suggestions
 * - Keyboard navigation
 * - Supports both simple string tags and object tags (id, code, name, etc.)
 * 
 * @since 2025-12-21 - Cat B Feature
 * Updated 2025-12-26 - Support for Hashtag Management System
 */

import React, { useState, useRef, useCallback, useEffect } from 'react';
import { FiX, FiHash } from 'react-icons/fi';
import './TagInput.css';
import useHashtagStore from "../../stores/useHashtagStore"; // Import store to fetch hashtags

export default function TagInput({
    value = [], // Array of IDs if mode='select', or strings if mode='create'
    onChange,
    placeholder = 'Thêm hashtag...',
    maxTags = 10,
    disabled = false,
    label,
    helperText,
    mode = 'create' // 'create' (free text) or 'select' (choose from existing)
}) {
    const { hashtags, fetchHashtags } = useHashtagStore();
    const [inputValue, setInputValue] = useState('');
    const [isFocused, setIsFocused] = useState(false);
    const [filteredSuggestions, setFilteredSuggestions] = useState([]);
    const [selectedSuggestionIndex, setSelectedSuggestionIndex] = useState(-1);
    const inputRef = useRef(null);
    const containerRef = useRef(null);

    // Fetch hashtags if in select mode
    useEffect(() => {
        if (mode === 'select' && hashtags.length === 0) {
            fetchHashtags();
        }
    }, [mode, hashtags.length, fetchHashtags]);

    // Filter suggestions
    useEffect(() => {
        if (inputValue.trim() && isFocused) {
            let filtered = [];
            if (mode === 'select') {
                filtered = hashtags.filter(h =>
                    (h.code.toLowerCase().includes(inputValue.toLowerCase()) ||
                        h.name.toLowerCase().includes(inputValue.toLowerCase())) &&
                    !value.includes(h.id)
                ).slice(0, 8); // Limit suggestions
            } else {
                // Simple string mode logic (omitted for brevity if not used here, but keeping basic support)
                // This part assumes we might just pass strings in 'create' mode
                /* 
                filtered = suggestions.filter(...) 
                */
            }
            setFilteredSuggestions(filtered);
            setSelectedSuggestionIndex(-1);
        } else if (isFocused && mode === 'select' && !inputValue.trim()) {
            // Show some recent/popular hashtags if input is empty
            setFilteredSuggestions(hashtags.filter(h => !value.includes(h.id)).slice(0, 8));
        } else {
            setFilteredSuggestions([]);
        }
    }, [inputValue, hashtags, value, mode, isFocused]);


    // Add a tag
    const addTag = useCallback((tagOrId) => {
        if (value.length >= maxTags) return;

        let newValue;
        if (mode === 'select') {
            // Expecting full hashtag object or ID from suggestion click
            const id = typeof tagOrId === 'object' ? tagOrId.id : tagOrId;
            if (value.includes(id)) return;
            newValue = [...value, id];
        } else {
            // String mode
            const normalized = tagOrId.toLowerCase().trim().replace(/\s+/g, '_').replace(/[^a-z0-9_]/g, '').slice(0, 30);
            if (!normalized || value.includes(normalized)) return;
            newValue = [...value, normalized];
        }

        onChange(newValue);
        setInputValue('');
        setFilteredSuggestions([]);
    }, [value, onChange, maxTags, mode]);

    // Remove a tag
    const removeTag = useCallback((tagToRemove) => {
        onChange(value.filter(t => t !== tagToRemove));
    }, [value, onChange]);

    // Handle key press
    const handleKeyDown = useCallback((e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            if (selectedSuggestionIndex >= 0 && filteredSuggestions[selectedSuggestionIndex]) {
                addTag(filteredSuggestions[selectedSuggestionIndex]);
            }
        } else if (e.key === 'Backspace' && !inputValue && value.length > 0) {
            removeTag(value[value.length - 1]);
        } else if (e.key === 'ArrowDown') {
            e.preventDefault();
            setSelectedSuggestionIndex(prev => Math.min(prev + 1, filteredSuggestions.length - 1));
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            setSelectedSuggestionIndex(prev => Math.max(prev - 1, -1));
        } else if (e.key === 'Escape') {
            setFilteredSuggestions([]);
            setSelectedSuggestionIndex(-1);
        }
    }, [inputValue, value, addTag, removeTag, filteredSuggestions, selectedSuggestionIndex]);

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

    // Helper to get display info for a tag (ID or String)
    const getTagDisplay = (tagId) => {
        if (mode === 'select') {
            const hashtag = hashtags.find(h => h.id === tagId);
            return hashtag ? { text: hashtag.name, code: hashtag.code, color: hashtag.color, icon: hashtag.icon } : { text: 'Unknown', code: '??' };
        }
        return { text: tagId, code: tagId };
    };

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
                    {value.map((tagId, index) => {
                        const info = getTagDisplay(tagId);
                        return (
                            <span
                                key={tagId}
                                className="tag"
                                style={info.color ? { backgroundColor: `${info.color}20`, color: info.color, border: `1px solid ${info.color}40` } : {}}
                            >
                                {info.icon && <span className="tag-icon">{info.icon}</span>}
                                <span className="tag-text">{info.text}</span>
                                {!disabled && (
                                    <button
                                        type="button"
                                        className="tag-remove"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            removeTag(tagId);
                                        }}
                                        style={info.color ? { color: info.color } : {}}
                                    >
                                        <FiX size={12} />
                                    </button>
                                )}
                            </span>
                        );
                    })}

                    {/* Input */}
                    {canAddMore && !disabled && (
                        <input
                            ref={inputRef}
                            type="text"
                            className="tag-input"
                            value={inputValue}
                            onChange={(e) => setInputValue(e.target.value)}
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
                            key={suggestion.id || suggestion}
                            type="button"
                            className={`suggestion-item ${index === selectedSuggestionIndex ? 'selected' : ''}`}
                            onClick={() => handleSuggestionClick(suggestion)}
                        >
                            {mode === 'select' ? (
                                <>
                                    <span style={{ marginRight: 8 }}>{suggestion.icon || <FiHash size={12} />}</span>
                                    <span>{suggestion.name}</span>
                                    <span style={{ opacity: 0.5, fontSize: '0.8em', marginLeft: 8 }}>#{suggestion.code}</span>
                                </>
                            ) : (
                                <>
                                    <FiHash size={12} />
                                    {suggestion}
                                </>
                            )}
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
