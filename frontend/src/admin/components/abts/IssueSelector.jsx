/**
 * IssueSelector - Component for selecting validation issues to fix via Agent 2
 * 
 * Displays within the warnings panel and allows users to:
 * - Toggle individual issues for refinement
 * - Select/deselect all issues
 * - Trigger refinement with selected issues
 * 
 * @since 2026-01-04
 */
import React from 'react';
import useABTSStore from '../../stores/useABTSStore';
import './IssueSelector.css';

const IssueSelector = ({ issues = [], type = 'warning' }) => {
    const {
        selectedIssues,
        toggleIssueSelection,
        selectAllIssues,
        clearIssueSelection,
        startRefinement,
        isRefining
    } = useABTSStore();

    // Generate IDs for issues if not present
    const issuesWithIds = issues.map((issue, idx) => ({
        ...issue,
        id: issue.id || `${type}-${idx}`
    }));

    const allIds = issuesWithIds.map(i => i.id);
    const allSelected = allIds.length > 0 && allIds.every(id => selectedIssues.includes(id));
    const someSelected = selectedIssues.some(id => allIds.includes(id));

    const handleSelectAll = () => {
        if (allSelected) {
            clearIssueSelection();
        } else {
            selectAllIssues(allIds);
        }
    };

    const handleFix = () => {
        if (selectedIssues.length > 0 && !isRefining) {
            startRefinement();
        }
    };

    if (issuesWithIds.length === 0) return null;

    return (
        <div className="issue-selector" onClick={(e) => e.stopPropagation()}>
            <div className="issue-selector__header">
                <label className="issue-selector__select-all">
                    <input
                        type="checkbox"
                        checked={allSelected}
                        ref={el => el && (el.indeterminate = someSelected && !allSelected)}
                        onChange={handleSelectAll}
                    />
                    <span>Chọn tất cả ({selectedIssues.length}/{allIds.length})</span>
                </label>
                <button
                    className="issue-selector__fix-btn"
                    onClick={handleFix}
                    disabled={selectedIssues.length === 0 || isRefining}
                >
                    {isRefining ? (
                        <>
                            <span className="spinner-small"></span>
                            Đang sửa...
                        </>
                    ) : (
                        <>🔧 Sửa ({selectedIssues.length})</>
                    )}
                </button>
            </div>

            <div className="issue-selector__list">
                {issuesWithIds.map(issue => (
                    <label
                        key={issue.id}
                        className={`issue-selector__item ${selectedIssues.includes(issue.id) ? 'selected' : ''}`}
                    >
                        <input
                            type="checkbox"
                            checked={selectedIssues.includes(issue.id)}
                            onChange={() => toggleIssueSelection(issue.id)}
                        />
                        <span className="issue-selector__message">
                            {issue.message || issue}
                        </span>
                    </label>
                ))}
            </div>
        </div>
    );
};

export default IssueSelector;
