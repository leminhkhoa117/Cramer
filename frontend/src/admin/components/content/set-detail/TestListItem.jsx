import {
    FiChevronDown,
    FiChevronRight,
    FiCopy,
    FiEdit2,
    FiEye,
    FiEyeOff,
    FiTrash2
} from 'react-icons/fi';

const skills = [
    { key: 'reading', label: 'Reading', shortLabel: 'R', color: '#3B82F6', partLabel: 'Passage' },
    { key: 'listening', label: 'Listening', shortLabel: 'L', color: '#10B981', partLabel: 'Part' },
    { key: 'writing', label: 'Writing', shortLabel: 'W', color: '#F59E0B', partLabel: 'Task' },
    { key: 'speaking', label: 'Speaking', shortLabel: 'S', color: '#EC4899', partLabel: 'Part' }
];

export default function TestListItem({
    test,
    isExpanded,
    isSelected,
    onToggleExpand,
    onToggleSelect,
    onEdit,
    onDuplicate,
    onPublish,
    onDelete,
    onClick
}) {
    const getSkillCount = (skillKey) => {
        return test.skillSectionCounts?.[skillKey] || test.skillSectionCounts?.[skillKey.toUpperCase()] || 0;
    };

    const hasAnyContent = skills.some(s => getSkillCount(s.key) > 0);

    return (
        <div className={`test-list-item ${isExpanded ? 'test-list-item--expanded' : ''} ${isSelected ? 'test-list-item--selected' : ''}`}>
            {/* Collapsed Header - Always Visible */}
            <div className="test-list-item__header">
                {/* Left Section: Expand Toggle + Checkbox + Info */}
                <div className="test-list-item__left">
                    <button
                        className="test-list-item__expand-btn"
                        onClick={(e) => { e.stopPropagation(); onToggleExpand(); }}
                        title={isExpanded ? 'Thu gọn' : 'Mở rộng'}
                    >
                        {isExpanded ? <FiChevronDown size={16} /> : <FiChevronRight size={16} />}
                    </button>
                    <input
                        type="checkbox"
                        className="test-list-item__checkbox"
                        checked={isSelected}
                        onChange={onToggleSelect}
                        onClick={(e) => e.stopPropagation()}
                    />
                    <div className="test-list-item__info" onClick={onClick}>
                        <span className="test-list-item__number">Bài {test.testNumber}</span>
                        <span className="test-list-item__name">{test.name || `Test ${test.testNumber}`}</span>
                    </div>
                </div>

                {/* Center Section: Status + Skills (always visible) */}
                <div className="test-list-item__center">
                    <span className={`status-dot ${test.isPublished ? 'status-dot--published' : 'status-dot--draft'}`}
                        title={test.isPublished ? 'Đã xuất bản' : 'Bản nháp'} />
                    <div className="skill-indicators skill-indicators--compact">
                        {skills.map(skill => {
                            const count = getSkillCount(skill.key);
                            const hasContent = count > 0;
                            return (
                                <span
                                    key={skill.key}
                                    className={`skill-badge ${hasContent ? 'skill-badge--complete' : 'skill-badge--empty'}`}
                                    style={{
                                        borderColor: skill.color,
                                        backgroundColor: hasContent ? skill.color + '20' : 'transparent',
                                        color: hasContent ? skill.color : 'var(--admin-text-muted)'
                                    }}
                                    title={`${skill.label}: ${count} ${skill.partLabel.toLowerCase()}${count !== 1 ? 's' : ''}`}
                                >
                                    {skill.shortLabel}
                                </span>
                            );
                        })}
                    </div>
                </div>

                {/* Right Section: Actions */}
                <div className="test-list-item__actions" onClick={(e) => e.stopPropagation()}>
                    <button className="test-list-item__action-btn" onClick={onEdit} title="Chỉnh sửa">
                        <FiEdit2 size={14} />
                    </button>
                    <button className="test-list-item__action-btn" onClick={onDuplicate} title="Nhân bản">
                        <FiCopy size={14} />
                    </button>
                    <button className="test-list-item__action-btn" onClick={onPublish} title={test.isPublished ? 'Gỡ xuất bản' : 'Xuất bản'}>
                        {test.isPublished ? <FiEyeOff size={14} /> : <FiEye size={14} />}
                    </button>
                    <button className="test-list-item__action-btn test-list-item__action-btn--delete" onClick={onDelete} title="Xóa">
                        <FiTrash2 size={14} />
                    </button>
                </div>
            </div>

            {/* Expanded Content */}
            {isExpanded && (
                <div className="test-list-item__content">
                    {/* Sections by Skill */}
                    <div className="test-list-item__sections">
                        {skills.map(skill => {
                            const count = getSkillCount(skill.key);
                            if (count === 0) return null;
                            return (
                                <div key={skill.key} className="test-list-item__skill-section">
                                    <span
                                        className="test-list-item__skill-label"
                                        style={{ color: skill.color }}
                                    >
                                        {skill.label}
                                    </span>
                                    <span className="test-list-item__skill-count">
                                        {count} {skill.partLabel}{count !== 1 ? 's' : ''}
                                    </span>
                                </div>
                            );
                        })}
                        {!hasAnyContent && (
                            <span className="test-list-item__no-content">Chưa có nội dung</span>
                        )}
                    </div>

                    {/* Metadata Row */}
                    <div className="test-list-item__meta">
                        {/* Difficulty Badge */}
                        {test.difficulty && (
                            <span className={`difficulty-badge difficulty-badge--${test.difficulty.toLowerCase()}`}>
                                {test.difficulty}
                            </span>
                        )}

                        {/* AI Badge */}
                        {test.isAiGenerated && (
                            <span className="ai-badge">AI</span>
                        )}

                        {/* Hashtags */}
                        {test.hashtags && test.hashtags.length > 0 && (
                            <div className="test-list-item__hashtags">
                                {test.hashtags.slice(0, 5).map(tag => (
                                    <span
                                        key={tag.id}
                                        className="hashtag"
                                        style={{
                                            backgroundColor: (tag.color || '#8B5CF6') + '20',
                                            color: tag.color || '#8B5CF6'
                                        }}
                                    >
                                        {tag.icon} {tag.name}
                                    </span>
                                ))}
                                {test.hashtags.length > 5 && (
                                    <span className="hashtag hashtag--more">+{test.hashtags.length - 5}</span>
                                )}
                            </div>
                        )}
                    </div>

                    {/* AI Generation Metadata */}
                    {test.isAiGenerated && test.generationMetadata && (
                        <div className="test-list-item__ai-meta">
                            {test.generationMetadata.model && (
                                <span className="ai-meta-item ai-meta-item--model">
                                    Model: {test.generationMetadata.model.split('/').pop()}
                                </span>
                            )}
                            {test.generationMetadata.topic && (
                                <span className="ai-meta-item ai-meta-item--topic">
                                    Topic: {test.generationMetadata.topic.length > 40
                                        ? test.generationMetadata.topic.substring(0, 40) + '...'
                                        : test.generationMetadata.topic}
                                </span>
                            )}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}