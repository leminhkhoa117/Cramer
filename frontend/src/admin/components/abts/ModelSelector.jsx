import { useEffect, useMemo, useRef, useState } from 'react';
import useABTSStore, { selectDefaultModelId } from '../../stores/useABTSStore';

/**
 * ModelSelector — capability-driven model picker.
 *
 * The model catalog (and each model's capability descriptor) comes entirely from
 * the backend via the store; there are no hardcoded "featured" models. The
 * recommended model (DEFAULT_MODEL_ID) is surfaced via sort order + a badge.
 */

const VENDOR_LABELS = {
    deepseek: 'DeepSeek',
    anthropic: 'Anthropic',
    google: 'Google',
    openai: 'OpenAI',
    qwen: 'Qwen',
    'z-ai': 'Z-AI',
    moonshotai: 'Moonshot',
    moonshot: 'Moonshot',
    mistralai: 'Mistral',
    'x-ai': 'xAI',
    meta: 'Meta',
    'meta-llama': 'Meta'
};

function vendorLabel(model) {
    const raw = model?.capabilities?.vendor || (model?.id ? model.id.split('/')[0] : '');
    if (!raw) return '';
    const key = String(raw).toLowerCase();
    return VENDOR_LABELS[key] || (raw.charAt(0).toUpperCase() + raw.slice(1));
}

function reasoningTag(capabilities) {
    switch (capabilities?.knobType) {
        case 'EFFORT_LOW_MED_HIGH':
            return { label: 'Effort', tone: 'reason' };
        case 'ANTHROPIC_BUDGET':
        case 'GEMINI_BUDGET':
            return { label: 'Budget', tone: 'reason' };
        case 'DEEPSEEK_TOGGLE':
        case 'QWEN_THINKING':
        case 'GLM_THINKING':
            return { label: 'Thinking ✓', tone: 'reason' };
        default:
            return { label: 'No reasoning', tone: 'muted' };
    }
}

function modelContextLength(model) {
    return model?.capabilities?.contextLength ?? model?.context_length ?? null;
}

function isFreeModel(model) {
    return model?.id?.endsWith(':free')
        || (model?.pricing && Number.parseFloat(model.pricing.prompt) === 0);
}

function formatPrice(pricing) {
    if (!pricing) return 'N/A';
    const promptPrice = Number.parseFloat(pricing.prompt);
    const completionPrice = Number.parseFloat(pricing.completion);

    if (promptPrice === 0 && completionPrice === 0) return 'Free';

    const averagePrice = ((promptPrice + completionPrice) / 2) * 1000000;
    if (averagePrice < 0.01) return '$<0.01/M';
    if (averagePrice < 1) return `$${averagePrice.toFixed(2)}/M`;
    return `$${averagePrice.toFixed(1)}/M`;
}

function formatContext(length) {
    if (!length) return 'N/A';
    if (length >= 1000000) return `${(length / 1000000).toFixed(1)}M`;
    if (length >= 1000) return `${(length / 1000).toFixed(0)}K`;
    return length.toString();
}

export default function ModelSelector({ value, onChange }) {
    const { models, fetchModels, isLoadingModels } = useABTSStore();
    // FIX 9: prefer the backend-advertised default; fall back to DEFAULT_MODEL_ID.
    const defaultModelId = useABTSStore(selectDefaultModelId);
    const [isOpen, setIsOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [sortBy, setSortBy] = useState('recommended');
    const [activeFilters, setActiveFilters] = useState([]);
    const [showCustomInput, setShowCustomInput] = useState(false);
    const [customModel, setCustomModel] = useState('');

    const dropdownRef = useRef(null);
    const searchInputRef = useRef(null);

    useEffect(() => {
        if (models.length === 0) {
            fetchModels();
        }
    }, [models.length, fetchModels]);

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };

        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    useEffect(() => {
        if (isOpen && searchInputRef.current) {
            searchInputRef.current.focus();
        }
    }, [isOpen]);

    const processedModels = useMemo(() => {
        let result = [...models];

        if (searchQuery.trim()) {
            const query = searchQuery.toLowerCase();
            result = result.filter(model =>
                model.id?.toLowerCase().includes(query) ||
                model.name?.toLowerCase().includes(query) ||
                model.capabilities?.displayName?.toLowerCase().includes(query)
            );
        }

        if (activeFilters.includes('free')) {
            result = result.filter(isFreeModel);
        }
        if (activeFilters.includes('structured')) {
            result = result.filter(model =>
                model.capabilities?.supportsJsonSchema
                || model.supported_parameters?.includes('structured_outputs'));
        }
        if (activeFilters.includes('reasoning')) {
            result = result.filter(model => {
                const knob = model.capabilities?.knobType;
                return knob && knob !== 'NONE' && knob !== 'KIMI_NONE';
            });
        }
        if (activeFilters.includes('large')) {
            result = result.filter(model => {
                const ctx = modelContextLength(model);
                return ctx && ctx >= 100000;
            });
        }

        const rank = (model) => {
            if (model.id === defaultModelId) return 0;
            if (isFreeModel(model)) return 1;
            return 2;
        };

        switch (sortBy) {
            case 'price':
                result.sort((left, right) => {
                    const leftPrice = left.pricing ? Number.parseFloat(left.pricing.prompt) : Infinity;
                    const rightPrice = right.pricing ? Number.parseFloat(right.pricing.prompt) : Infinity;
                    return leftPrice - rightPrice;
                });
                break;
            case 'context':
                result.sort((left, right) => (modelContextLength(right) || 0) - (modelContextLength(left) || 0));
                break;
            case 'name':
                result.sort((left, right) => (left.name || left.id || '').localeCompare(right.name || right.id || ''));
                break;
            default: // recommended
                result.sort((left, right) => {
                    const diff = rank(left) - rank(right);
                    if (diff !== 0) return diff;
                    return (left.name || left.id || '').localeCompare(right.name || right.id || '');
                });
                break;
        }

        return result.slice(0, 100);
    }, [models, searchQuery, sortBy, activeFilters, defaultModelId]);

    const currentModel = useMemo(() => {
        if (!value) return null;
        const fromList = models.find(model => model.id === value);
        if (fromList) {
            return {
                id: fromList.id,
                name: fromList.capabilities?.displayName || fromList.name || fromList.id,
                desc: vendorLabel(fromList)
            };
        }
        return { id: value, name: value, desc: 'Custom / Unlisted' };
    }, [value, models]);

    const toggleFilter = (filter) => {
        setActiveFilters(previous =>
            previous.includes(filter)
                ? previous.filter(item => item !== filter)
                : [...previous, filter]
        );
    };

    const handleSelect = (modelId) => {
        onChange(modelId);
        setIsOpen(false);
        setSearchQuery('');
    };

    const handleCustomSubmit = () => {
        if (!customModel.trim()) return;
        onChange(customModel.trim());
        setShowCustomInput(false);
        setCustomModel('');
        setIsOpen(false);
    };

    return (
        <div className="model-selector" ref={dropdownRef}>
            <button
                className="model-selector-trigger"
                onClick={() => setIsOpen(!isOpen)}
                type="button"
                aria-haspopup="listbox"
                aria-expanded={isOpen}
                aria-label="Select AI model"
            >
                <div className="selected-model">
                    {currentModel ? (
                        <>
                            <span className="model-name">{currentModel.name || currentModel.id || 'Unknown Model'}</span>
                            {currentModel.desc && <span className="model-desc">{currentModel.desc}</span>}
                        </>
                    ) : (
                        <span className="placeholder">Select a model…</span>
                    )}
                </div>
                <span className="dropdown-arrow">{isOpen ? '▲' : '▼'}</span>
            </button>

            {isOpen && (
                <div className="model-selector-dropdown" role="listbox" aria-label="Available models">
                    <div className="dropdown-search">
                        <input
                            ref={searchInputRef}
                            type="text"
                            placeholder="Search models..."
                            value={searchQuery}
                            onChange={(event) => setSearchQuery(event.target.value)}
                            className="search-input"
                            aria-label="Search models"
                        />
                    </div>

                    <div className="dropdown-controls">
                        <div className="filter-buttons">
                            {activeFilters.length > 0 && (
                                <button
                                    className="filter-btn filter-btn-clear"
                                    onClick={() => setActiveFilters([])}
                                    title="Clear all filters"
                                >X</button>
                            )}
                            <button className={`filter-btn ${activeFilters.includes('free') ? 'active' : ''}`} onClick={() => toggleFilter('free')} aria-pressed={activeFilters.includes('free')} aria-label="Filter free models">Free</button>
                            <button className={`filter-btn ${activeFilters.includes('structured') ? 'active' : ''}`} onClick={() => toggleFilter('structured')} aria-pressed={activeFilters.includes('structured')} aria-label="Filter models supporting JSON">JSON</button>
                            <button className={`filter-btn ${activeFilters.includes('reasoning') ? 'active' : ''}`} onClick={() => toggleFilter('reasoning')} aria-pressed={activeFilters.includes('reasoning')} aria-label="Filter reasoning models">Reasoning</button>
                            <button className={`filter-btn ${activeFilters.includes('large') ? 'active' : ''}`} onClick={() => toggleFilter('large')} aria-pressed={activeFilters.includes('large')} aria-label="Filter large context models">100K+</button>
                        </div>
                        <select className="sort-select" value={sortBy} onChange={(event) => setSortBy(event.target.value)} aria-label="Sort models by">
                            <option value="recommended">Recommended</option>
                            <option value="price">Cheapest</option>
                            <option value="context">Largest Context</option>
                            <option value="name">A-Z</option>
                        </select>
                    </div>

                    <div className="model-list">
                        <div className="model-section">
                            <div className="section-header">
                                {isLoadingModels ? 'Loading models...' : `Models (${processedModels.length})`}
                            </div>
                            {!isLoadingModels && processedModels.length === 0 && (
                                <div className="model-item model-empty">
                                    <span className="model-desc">No models match your filters.</span>
                                </div>
                            )}
                            {processedModels.map(model => {
                                const caps = model.capabilities;
                                const tag = reasoningTag(caps);
                                const recommended = model.id === defaultModelId;
                                const free = isFreeModel(model);
                                const rawName = caps?.displayName || model.name || model.id;
                                // Backend `displayName` strips the vendor prefix, so slugs like
                                // `openrouter/free` render as the unhelpful "free". Fall back to
                                // the slug suffix when the name is too short or matches a generic word.
                                const displayName = (typeof rawName === 'string' && rawName.length <= 4)
                                    ? (model.id || rawName)
                                    : rawName;
                                return (
                                    <button
                                        key={model.id}
                                        type="button"
                                        role="option"
                                        aria-selected={value === model.id}
                                        tabIndex={0}
                                        className={`model-item ${value === model.id ? 'selected' : ''}`}
                                        onClick={() => handleSelect(model.id)}
                                    >
                                        <div className="model-info">
                                            <span className="model-name">
                                                {displayName}
                                                {recommended && <span className="model-badge badge-recommended">recommended</span>}
                                                {!recommended && free && <span className="model-badge badge-free">free</span>}
                                            </span>
                                            <span className="model-meta">
                                                {vendorLabel(model) && <span className="meta-vendor">{vendorLabel(model)}</span>}
                                                <span className="meta-price">{formatPrice(model.pricing)}</span>
                                                <span className="meta-context">{formatContext(modelContextLength(model))}</span>
                                                <span className={`meta-reason ${tag.tone === 'muted' ? 'is-muted' : ''}`}>{tag.label}</span>
                                                {caps?.supportsJsonSchema && <span className="meta-json">JSON</span>}
                                            </span>
                                        </div>
                                        <span className="model-id">{model.id}</span>
                                    </button>
                                );
                            })}
                        </div>

                        <div className="model-section">
                            <div className="section-header">Custom Model</div>
                            {!showCustomInput ? (
                                <div className="model-item custom-trigger" onClick={() => setShowCustomInput(true)}>
                                    <span className="model-name">Enter custom model ID...</span>
                                </div>
                            ) : (
                                <div className="custom-input-wrapper">
                                    <input
                                        type="text"
                                        placeholder="e.g., anthropic/claude-3-opus"
                                        value={customModel}
                                        onChange={(event) => setCustomModel(event.target.value)}
                                        onKeyDown={(event) => event.key === 'Enter' && handleCustomSubmit()}
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
        </div>
    );
}
