/**
 * ModelSelector - Advanced model selection component with search, filter and sort.
 * Fetches models from OpenRouter API and displays them in a searchable dropdown.
 * 
 * Features:
 * - Real-time search by model name or ID
 * - Sort by price, context length, or name
 * - Filter by capabilities (structured outputs, free, etc.)
 * - Shows pricing and context info
 * 
 * @since 2025-12-21 - ABTS v2.1
 */

import React, { useState, useMemo, useRef, useEffect } from 'react';
import useABTSStore from '../../stores/useABTSStore';

// Popular/recommended models with descriptions
const FEATURED_MODELS = [
    { id: 'mistralai/devstral-2512:free', name: 'Devstral (Free)', desc: 'Free & Fast', badge: 'recommended' },
    { id: 'deepseek/deepseek-r1', name: 'DeepSeek R1', desc: 'Best Reasoning', badge: 'quality' },
    { id: 'google/gemini-2.0-flash-001', name: 'Gemini 2.0 Flash', desc: 'Fast & Reliable', badge: 'popular' },
    { id: 'deepseek/deepseek-chat', name: 'DeepSeek V3', desc: 'Best Value', badge: 'value' },
    { id: 'anthropic/claude-3.5-sonnet', name: 'Claude 3.5 Sonnet', desc: 'Most Capable', badge: null },
    { id: 'openai/gpt-4o', name: 'GPT-4o', desc: 'OpenAI Latest', badge: null },
    { id: 'google/gemini-2.0-flash-exp:free', name: 'Gemini 2.0 Flash (Free)', desc: 'Free Tier', badge: 'free' },
];

// Default model used when none selected (matches backend application.properties)
const DEFAULT_MODEL_ID = 'mistralai/devstral-2512:free';
const DEFAULT_MODEL_DISPLAY = 'Devstral (Free - Recommended)';

export default function ModelSelector({ value, onChange }) {
    const { models, fetchModels, isLoadingModels } = useABTSStore();

    const [isOpen, setIsOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [sortBy, setSortBy] = useState('recommended'); // recommended, price, context, name
    const [activeFilters, setActiveFilters] = useState([]); // Multi-select: ['free', 'structured', 'fast']
    const [showCustomInput, setShowCustomInput] = useState(false);
    const [customModel, setCustomModel] = useState('');

    const dropdownRef = useRef(null);
    const searchInputRef = useRef(null);

    // Fetch models on mount
    useEffect(() => {
        if (models.length === 0) {
            fetchModels();
        }
    }, [models.length, fetchModels]);

    // Close dropdown when clicking outside
    useEffect(() => {
        const handleClickOutside = (e) => {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
                setIsOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    // Focus search input when dropdown opens
    useEffect(() => {
        if (isOpen && searchInputRef.current) {
            searchInputRef.current.focus();
        }
    }, [isOpen]);

    // Toggle filter (multi-select)
    const toggleFilter = (filter) => {
        setActiveFilters(prev =>
            prev.includes(filter)
                ? prev.filter(f => f !== filter)
                : [...prev, filter]
        );
    };

    // Clear all filters
    const clearFilters = () => {
        setActiveFilters([]);
    };

    // Format price for display
    const formatPrice = (pricing) => {
        if (!pricing) return 'N/A';
        const promptPrice = parseFloat(pricing.prompt);
        const completionPrice = parseFloat(pricing.completion);

        if (promptPrice === 0 && completionPrice === 0) return 'Free';

        // Convert to $/M tokens
        const avgPrice = ((promptPrice + completionPrice) / 2) * 1000000;
        if (avgPrice < 0.01) return '$<0.01/M';
        if (avgPrice < 1) return `$${avgPrice.toFixed(2)}/M`;
        return `$${avgPrice.toFixed(1)}/M`;
    };

    // Format context length
    const formatContext = (length) => {
        if (!length) return 'N/A';
        if (length >= 1000000) return `${(length / 1000000).toFixed(1)}M`;
        if (length >= 1000) return `${(length / 1000).toFixed(0)}K`;
        return length.toString();
    };

    // Filter and sort models
    const processedModels = useMemo(() => {
        let result = [...models];

        // Apply search filter
        if (searchQuery.trim()) {
            const query = searchQuery.toLowerCase();
            result = result.filter(m =>
                m.id?.toLowerCase().includes(query) ||
                m.name?.toLowerCase().includes(query)
            );
        }

        // Apply multi-select filters (AND logic - model must match ALL active filters)
        if (activeFilters.includes('free')) {
            result = result.filter(m =>
                m.id?.endsWith(':free') ||
                (m.pricing && parseFloat(m.pricing.prompt) === 0)
            );
        }
        if (activeFilters.includes('structured')) {
            result = result.filter(m =>
                m.supported_parameters?.includes('structured_outputs')
            );
        }
        if (activeFilters.includes('fast')) {
            result = result.filter(m =>
                m.id?.includes(':nitro') || m.id?.includes('flash')
            );
        }
        if (activeFilters.includes('large')) {
            result = result.filter(m =>
                m.context_length && m.context_length >= 100000
            );
        }

        // Apply sorting
        switch (sortBy) {
            case 'price':
                result.sort((a, b) => {
                    const priceA = a.pricing ? parseFloat(a.pricing.prompt) : Infinity;
                    const priceB = b.pricing ? parseFloat(b.pricing.prompt) : Infinity;
                    return priceA - priceB;
                });
                break;
            case 'context':
                result.sort((a, b) => (b.context_length || 0) - (a.context_length || 0));
                break;
            case 'name':
                result.sort((a, b) => (a.name || '').localeCompare(b.name || ''));
                break;
            default:
                // Keep original order for 'recommended'
                break;
        }

        return result.slice(0, 100); // Limit to 100 for performance
    }, [models, searchQuery, sortBy, activeFilters]);

    // Get current model info
    const currentModel = useMemo(() => {
        if (!value) return null;

        // 1. Try featured models
        const featured = FEATURED_MODELS.find(m => m.id === value);
        if (featured) return featured;

        // 2. Try fetched models list
        const fromList = models.find(m => m.id === value);
        if (fromList) return { id: fromList.id, name: fromList.name, desc: '' };

        // 3. Fallback: just show value if we have one
        return { id: value, name: value, desc: 'Custom/Unlisted' };
    }, [value, models]);

    const handleSelect = (modelId) => {
        onChange(modelId);
        setIsOpen(false);
        setSearchQuery('');
    };

    const handleCustomSubmit = () => {
        if (customModel.trim()) {
            onChange(customModel.trim());
            setShowCustomInput(false);
            setCustomModel('');
            setIsOpen(false);
        }
    };

    const getBadgeClass = (badge) => {
        switch (badge) {
            case 'recommended': return 'badge-recommended';
            case 'quality': return 'badge-quality';
            case 'value': return 'badge-value';
            case 'free': return 'badge-free';
            case 'popular': return 'badge-popular';
            default: return '';
        }
    };

    return (
        <div className="model-selector" ref={dropdownRef}>
            {/* Selected Model Display */}
            <button
                className="model-selector-trigger"
                onClick={() => setIsOpen(!isOpen)}
                type="button"
            >
                <div className="selected-model">
                    {currentModel ? (
                        <>
                            <span className="model-name" style={{ color: 'white' }}>
                                {currentModel.name || currentModel.id || 'Unknown Model'}
                            </span>
                            {currentModel.desc && (
                                <span className="model-desc" style={{ color: 'rgba(255,255,255,0.6)' }}>
                                    {currentModel.desc}
                                </span>
                            )}
                        </>
                    ) : (
                        <span className="placeholder" style={{ color: 'rgba(255, 255, 255, 0.5)', opacity: 1, backgroundColor: 'rgba(255, 255, 255, 0)' }}>
                            Use Default ({DEFAULT_MODEL_DISPLAY})
                        </span>
                    )}
                </div>
                <span className="dropdown-arrow" style={{ color: 'rgba(255,255,255,0.6)' }}>
                    {isOpen ? '▲' : '▼'}
                </span>
            </button>

            {/* Dropdown Panel */}
            {isOpen && (
                <div className="model-selector-dropdown">
                    {/* Search Bar */}
                    <div className="dropdown-search">
                        <input
                            ref={searchInputRef}
                            type="text"
                            placeholder="Search models..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="search-input"
                        />
                    </div>

                    {/* Filter & Sort Controls */}
                    <div className="dropdown-controls">
                        <div className="filter-buttons">
                            {activeFilters.length > 0 && (
                                <button
                                    className="filter-btn filter-btn-clear"
                                    onClick={clearFilters}
                                    title="Clear all filters"
                                >✕</button>
                            )}
                            <button
                                className={`filter-btn ${activeFilters.includes('free') ? 'active' : ''}`}
                                onClick={() => toggleFilter('free')}
                            >Free</button>
                            <button
                                className={`filter-btn ${activeFilters.includes('structured') ? 'active' : ''}`}
                                onClick={() => toggleFilter('structured')}
                            >JSON</button>
                            <button
                                className={`filter-btn ${activeFilters.includes('fast') ? 'active' : ''}`}
                                onClick={() => toggleFilter('fast')}
                            >Fast</button>
                            <button
                                className={`filter-btn ${activeFilters.includes('large') ? 'active' : ''}`}
                                onClick={() => toggleFilter('large')}
                            >100K+</button>
                        </div>
                        <select
                            className="sort-select"
                            value={sortBy}
                            onChange={(e) => setSortBy(e.target.value)}
                        >
                            <option value="recommended">Recommended</option>
                            <option value="price">Cheapest</option>
                            <option value="context">Largest Context</option>
                            <option value="name">A-Z</option>
                        </select>
                    </div>

                    {/* Model List */}
                    <div className="model-list">
                        {/* Default Option */}
                        <div
                            className={`model-item default-item ${!value ? 'selected' : ''}`}
                            onClick={() => handleSelect(null)}
                        >
                            <div className="model-info">
                                <span className="model-name">Use Default</span>
                                <span className="model-desc">{DEFAULT_MODEL_DISPLAY}</span>
                            </div>
                        </div>

                        {/* Featured Models */}
                        {!searchQuery && activeFilters.length === 0 && (
                            <div className="model-section">
                                <div className="section-header">Featured Models</div>
                                {FEATURED_MODELS.map(model => (
                                    <div
                                        key={model.id}
                                        className={`model-item ${value === model.id ? 'selected' : ''}`}
                                        onClick={() => handleSelect(model.id)}
                                    >
                                        <div className="model-info">
                                            <span className="model-name">
                                                {model.name}
                                                {model.badge && (
                                                    <span className={`model-badge ${getBadgeClass(model.badge)}`}>
                                                        {model.badge}
                                                    </span>
                                                )}
                                            </span>
                                            <span className="model-desc">{model.desc}</span>
                                        </div>
                                        <span className="model-id">{model.id}</span>
                                    </div>
                                ))}
                            </div>
                        )}

                        {/* All Models */}
                        <div className="model-section">
                            <div className="section-header">
                                {isLoadingModels ? 'Loading models...' : `All Models (${processedModels.length})`}
                            </div>
                            {processedModels.map(model => (
                                <div
                                    key={model.id}
                                    className={`model-item ${value === model.id ? 'selected' : ''}`}
                                    onClick={() => handleSelect(model.id)}
                                >
                                    <div className="model-info">
                                        <span className="model-name">{model.name}</span>
                                        <span className="model-meta">
                                            <span className="meta-price">{formatPrice(model.pricing)}</span>
                                            <span className="meta-context">{formatContext(model.context_length)}</span>
                                            {model.supported_parameters?.includes('structured_outputs') && (
                                                <span className="meta-json">JSON</span>
                                            )}
                                        </span>
                                    </div>
                                    <span className="model-id">{model.id}</span>
                                </div>
                            ))}
                        </div>

                        {/* Custom Model Option */}
                        <div className="model-section">
                            <div className="section-header">Custom Model</div>
                            {!showCustomInput ? (
                                <div
                                    className="model-item custom-trigger"
                                    onClick={() => setShowCustomInput(true)}
                                >
                                    <span className="model-name">Enter custom model ID...</span>
                                </div>
                            ) : (
                                <div className="custom-input-wrapper">
                                    <input
                                        type="text"
                                        placeholder="e.g., anthropic/claude-3-opus"
                                        value={customModel}
                                        onChange={(e) => setCustomModel(e.target.value)}
                                        onKeyDown={(e) => e.key === 'Enter' && handleCustomSubmit()}
                                        className="custom-input"
                                        autoFocus
                                    />
                                    <div className="custom-actions">
                                        <button className="btn-apply" onClick={handleCustomSubmit}>Apply</button>
                                        <button className="btn-cancel" onClick={() => setShowCustomInput(false)}>Cancel</button>
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            )}

            <style>{`
                .model-selector {
                    position: relative;
                    width: 100%;
                }

                .model-selector-trigger {
                    width: 100%;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    padding: var(--admin-spacing-md, 12px) var(--admin-spacing-md, 16px);
                    background: var(--admin-bg-input, rgba(255, 255, 255, 0.05));
                    border: 1px solid var(--admin-border-primary, rgba(255, 255, 255, 0.1));
                    border-radius: var(--admin-radius-md, 8px);
                    color: var(--admin-text-primary, #fff);
                    cursor: pointer;
                    transition: var(--admin-transition-normal, 250ms ease);
                    text-align: left;
                }

                .model-selector-trigger:hover {
                    border-color: var(--admin-primary, #8B5CF6);
                    background: var(--admin-bg-card-hover, rgba(255, 255, 255, 0.08));
                }

                .selected-model {
                    display: flex;
                    flex-direction: column;
                    gap: 2px;
                }

                .selected-model .model-name {
                    font-weight: 500;
                    color: var(--admin-text-primary, #fff);
                }

                .selected-model .model-desc {
                    font-size: 0.8rem;
                    color: var(--admin-text-muted, rgba(255, 255, 255, 0.5));
                }

                .placeholder {
                    color: rgba(255, 255, 255, 0.5);
                    opacity: 1;
                    background-color: rgba(255, 255, 255, 0);
                }

                .dropdown-arrow {
                    font-size: 0.7rem;
                    color: var(--admin-text-muted, rgba(255, 255, 255, 0.5));
                }

                .model-selector-dropdown {
                    position: absolute;
                    top: calc(100% + 4px);
                    left: 0;
                    right: 0;
                    max-height: 450px;
                    background: var(--admin-bg-secondary, #1A1A2E);
                    border: 1px solid var(--admin-border-primary, rgba(255, 255, 255, 0.1));
                    border-radius: var(--admin-radius-lg, 12px);
                    box-shadow: var(--admin-shadow-xl, 0 20px 25px -5px rgba(0, 0, 0, 0.3));
                    z-index: var(--admin-z-dropdown, 100);
                    overflow: hidden;
                    display: flex;
                    flex-direction: column;
                }

                .dropdown-search {
                    padding: var(--admin-spacing-sm, 8px);
                    border-bottom: 1px solid var(--admin-border-secondary, rgba(255, 255, 255, 0.05));
                }

                .search-input {
                    width: 100%;
                    padding: var(--admin-spacing-sm, 8px) var(--admin-spacing-md, 12px);
                    background: var(--admin-bg-input, rgba(255, 255, 255, 0.05));
                    border: 1px solid var(--admin-border-secondary, rgba(255, 255, 255, 0.05));
                    border-radius: var(--admin-radius-sm, 6px);
                    color: var(--admin-text-primary, #fff);
                    font-size: 0.9rem;
                }

                .search-input:focus {
                    outline: none;
                    border-color: var(--admin-primary, #8B5CF6);
                }

                .dropdown-controls {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    padding: var(--admin-spacing-sm, 8px);
                    gap: var(--admin-spacing-sm, 8px);
                    border-bottom: 1px solid var(--admin-border-secondary, rgba(255, 255, 255, 0.05));
                }

                .filter-buttons {
                    display: flex;
                    gap: 4px;
                }

                .filter-btn {
                    padding: 4px 10px;
                    background: transparent;
                    border: 1px solid var(--admin-border-secondary, rgba(255, 255, 255, 0.1));
                    border-radius: var(--admin-radius-sm, 6px);
                    color: var(--admin-text-secondary, rgba(255, 255, 255, 0.7));
                    font-size: 0.75rem;
                    cursor: pointer;
                    transition: var(--admin-transition-fast, 150ms ease);
                }

                .filter-btn:hover {
                    background: var(--admin-bg-card-hover, rgba(255, 255, 255, 0.08));
                }

                .filter-btn.active {
                    background: var(--admin-primary, #8B5CF6);
                    border-color: var(--admin-primary, #8B5CF6);
                    color: white;
                }

                .filter-btn-clear {
                    background: var(--admin-danger, #EF4444) !important;
                    border-color: var(--admin-danger, #EF4444) !important;
                    color: white !important;
                    padding: 4px 8px !important;
                }

                .filter-btn-clear:hover {
                    opacity: 0.8;
                }

                .sort-select {
                    padding: 4px 8px;
                    background: var(--admin-bg-input, rgba(255, 255, 255, 0.05));
                    border: 1px solid var(--admin-border-secondary, rgba(255, 255, 255, 0.1));
                    border-radius: var(--admin-radius-sm, 6px);
                    color: var(--admin-text-primary, #fff);
                    font-size: 0.75rem;
                    cursor: pointer;
                }

                .model-list {
                    overflow-y: auto;
                    max-height: 350px;
                }

                .model-section {
                    padding: var(--admin-spacing-xs, 4px) 0;
                }

                .section-header {
                    padding: var(--admin-spacing-xs, 4px) var(--admin-spacing-md, 12px);
                    font-size: 0.7rem;
                    text-transform: uppercase;
                    color: var(--admin-text-muted, rgba(255, 255, 255, 0.4));
                    font-weight: 600;
                    letter-spacing: 0.5px;
                }

                .model-item {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    padding: var(--admin-spacing-sm, 10px) var(--admin-spacing-md, 12px);
                    cursor: pointer;
                    transition: var(--admin-transition-fast, 150ms ease);
                }

                .model-item:hover {
                    background: var(--admin-bg-card-hover, rgba(255, 255, 255, 0.05));
                }

                .model-item.selected {
                    background: var(--admin-primary-light, rgba(139, 92, 246, 0.15));
                    border-left: 3px solid var(--admin-primary, #8B5CF6);
                }

                .model-item.default-item {
                    border-bottom: 1px solid var(--admin-border-secondary, rgba(255, 255, 255, 0.05));
                }

                .model-info {
                    display: flex;
                    flex-direction: column;
                    gap: 2px;
                    flex: 1;
                    min-width: 0;
                }

                .model-info .model-name {
                    font-weight: 500;
                    color: var(--admin-text-primary, #fff);
                    display: flex;
                    align-items: center;
                    gap: 6px;
                }

                .model-badge {
                    font-size: 0.65rem;
                    padding: 2px 6px;
                    border-radius: 10px;
                    text-transform: uppercase;
                    font-weight: 600;
                }

                .badge-recommended { background: #10B981; color: white; }
                .badge-quality { background: #8B5CF6; color: white; }
                .badge-value { background: #F59E0B; color: black; }
                .badge-free { background: #3B82F6; color: white; }
                .badge-popular { background: #EC4899; color: white; }

                .model-info .model-desc {
                    font-size: 0.8rem;
                    color: var(--admin-text-muted, rgba(255, 255, 255, 0.5));
                }

                .model-info .model-meta {
                    display: flex;
                    gap: 8px;
                    font-size: 0.75rem;
                    color: var(--admin-text-secondary, rgba(255, 255, 255, 0.6));
                }

                .meta-price {
                    color: var(--admin-success, #10B981);
                }

                .meta-context {
                    color: var(--admin-info, #3B82F6);
                }

                .model-id {
                    font-size: 0.7rem;
                    color: var(--admin-text-disabled, rgba(255, 255, 255, 0.3));
                    font-family: monospace;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    max-width: 180px;
                }

                .custom-trigger {
                    border-top: 1px solid var(--admin-border-secondary, rgba(255, 255, 255, 0.05));
                }

                .custom-input-wrapper {
                    padding: var(--admin-spacing-sm, 8px) var(--admin-spacing-md, 12px);
                }

                .custom-input {
                    width: 100%;
                    padding: var(--admin-spacing-sm, 8px);
                    background: var(--admin-bg-input, rgba(255, 255, 255, 0.05));
                    border: 1px solid var(--admin-primary, #8B5CF6);
                    border-radius: var(--admin-radius-sm, 6px);
                    color: var(--admin-text-primary, #fff);
                    margin-bottom: 8px;
                }

                .custom-actions {
                    display: flex;
                    gap: 8px;
                }

                .btn-apply, .btn-cancel {
                    padding: 6px 12px;
                    border-radius: var(--admin-radius-sm, 6px);
                    font-size: 0.8rem;
                    cursor: pointer;
                    transition: var(--admin-transition-fast, 150ms ease);
                }

                .btn-apply {
                    background: var(--admin-primary, #8B5CF6);
                    border: none;
                    color: white;
                }

                .btn-cancel {
                    background: transparent;
                    border: 1px solid var(--admin-border-primary, rgba(255, 255, 255, 0.2));
                    color: var(--admin-text-secondary, rgba(255, 255, 255, 0.7));
                }

                .btn-apply:hover { opacity: 0.9; }
                .btn-cancel:hover { background: var(--admin-bg-card-hover, rgba(255, 255, 255, 0.08)); }
            `}</style>
        </div>
    );
}
