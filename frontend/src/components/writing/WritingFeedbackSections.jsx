import React from 'react';
import {
    FiAward,
    FiBook,
    FiCheckCircle,
    FiChevronDown,
    FiChevronRight,
    FiEdit3,
    FiFileText,
    FiInfo,
    FiTrendingUp,
    FiXCircle,
    FiZap,
} from 'react-icons/fi';

const wordTypeLabels = {
    noun: 'danh từ',
    verb: 'động từ',
    adjective: 'tính từ',
    adverb: 'trạng từ',
    preposition: 'giới từ',
    conjunction: 'liên từ',
    phrase: 'cụm từ',
};

export default function WritingFeedbackSections({
    aiFeedback,
    currentTaskReview,
    expandedSections,
    getErrorStyle,
    itemRefs,
    selectedItemId,
    toggleSection,
}) {
    return (
        <>
            {aiFeedback.errorAnalysis && (aiFeedback.errorAnalysis.major_errors !== undefined || aiFeedback.errorAnalysis.minor_errors !== undefined) && (
                <div className="error-analysis-summary">
                    <div className="error-counts">
                        <div className="error-count major">
                            <span className="count">{aiFeedback.errorAnalysis.major_errors || 0}</span>
                            <span className="label">Lỗi lớn</span>
                        </div>
                        <div className="error-count minor">
                            <span className="count">{aiFeedback.errorAnalysis.minor_errors || 0}</span>
                            <span className="label">Lỗi nhỏ</span>
                        </div>
                    </div>
                    {aiFeedback.errorAnalysis.summary && (
                        <p className="error-summary-text">{aiFeedback.errorAnalysis.summary}</p>
                    )}
                </div>
            )}

            {aiFeedback.sentenceCorrections?.length > 0 && (
                <div className={`expandable-section ${expandedSections.corrections ? 'open' : ''}`}>
                    <button className="section-toggle" onClick={() => toggleSection('corrections')}>
                        <FiEdit3 size={16} className="section-icon" />
                        <span className="dropdown-title">Sửa lỗi câu ({aiFeedback.sentenceCorrections.length})</span>
                        <span className="toggle-arrow">{expandedSections.corrections ? <FiChevronDown size={14} /> : <FiChevronRight size={14} />}</span>
                    </button>
                    {expandedSections.corrections && (
                        <div className="section-content">
                            {aiFeedback.sentenceCorrections.map((corr, idx) => {
                                const itemId = `correction-${idx}`;
                                const style = getErrorStyle(corr.error_type);
                                return (
                                    <div
                                        key={idx}
                                        ref={el => itemRefs.current[itemId] = el}
                                        className={`correction-item ${selectedItemId === itemId ? 'selected' : ''}`}
                                        style={{ borderLeftColor: style.border }}
                                    >
                                        <div className="correction-header">
                                            <span className="error-type-badge" style={{ backgroundColor: style.border }}>
                                                {corr.error_type || 'error'}
                                            </span>
                                        </div>
                                        <div className="correction-original">
                                            <span className="label"><FiXCircle size={14} /></span>
                                            <span className="text">{corr.original}</span>
                                        </div>
                                        <div className="correction-fixed">
                                            <span className="label"><FiCheckCircle size={14} /></span>
                                            <span className="text">{corr.corrected}</span>
                                        </div>
                                        {corr.explanation && (
                                            <div className="correction-explanation">
                                                <FiInfo size={14} /> {corr.explanation}
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>
            )}

            {aiFeedback.paragraphRewrites?.length > 0 && (
                <div className={`expandable-section ${expandedSections.paragraphs ? 'open' : ''}`}>
                    <button className="section-toggle" onClick={() => toggleSection('paragraphs')}>
                        <FiBook size={16} className="section-icon" />
                        <span className="dropdown-title">Viết lại đoạn ({aiFeedback.paragraphRewrites.length})</span>
                        <span className="toggle-arrow">{expandedSections.paragraphs ? <FiChevronDown size={14} /> : <FiChevronRight size={14} />}</span>
                    </button>
                    {expandedSections.paragraphs && (
                        <div className="section-content">
                            {aiFeedback.paragraphRewrites.map((para, idx) => {
                                const itemId = `paragraph-${idx}`;
                                return (
                                    <div
                                        key={idx}
                                        ref={el => itemRefs.current[itemId] = el}
                                        className={`paragraph-item ${selectedItemId === itemId ? 'selected' : ''}`}
                                    >
                                        <div className="para-header">Đoạn {(para.paragraph_index || idx) + 1}</div>
                                        <div className="original-para">
                                            <span className="para-label"><FiFileText size={14} /> Bản gốc:</span>
                                            <p>{para.original}</p>
                                        </div>
                                        <div className="improved-para">
                                            <span className="para-label"><FiTrendingUp size={14} /> Bản cải thiện:</span>
                                            <p>{para.improved}</p>
                                        </div>
                                        {para.improvements_made?.length > 0 && (
                                            <div className="improvements-made">
                                                <span className="para-label"><FiZap size={14} /> Các cải thiện:</span>
                                                <ul>{para.improvements_made.map((imp, i) => <li key={i}>{imp}</li>)}</ul>
                                            </div>
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>
            )}

            {aiFeedback.sampleEssayBandPlus && (
                <div className={`expandable-section ${expandedSections.sampleBandPlus ? 'open' : ''}`}>
                    <button className="section-toggle sample-toggle" onClick={() => toggleSection('sampleBandPlus')}>
                        <FiTrendingUp size={16} className="section-icon" />
                        <span className="dropdown-title">
                            Phiên bản cải tiến Band {Math.min(9, Math.floor((currentTaskReview?.overallBand || 6)) + 1)}
                        </span>
                        <span className="toggle-arrow">{expandedSections.sampleBandPlus ? <FiChevronDown size={14} /> : <FiChevronRight size={14} />}</span>
                    </button>
                    {expandedSections.sampleBandPlus && (
                        <div className="section-content sample-content">
                            <p className="sample-description">Phiên bản cải thiện, đạt band cao hơn 1 điểm.</p>
                            <div className="sample-essay-text">
                                {aiFeedback.sampleEssayBandPlus.split('\n').map((para, idx) => <p key={idx}>{para || '\u00A0'}</p>)}
                            </div>
                        </div>
                    )}
                </div>
            )}

            {aiFeedback.sampleEssayBand9 && (
                <div className={`expandable-section ${expandedSections.sampleBand9 ? 'open' : ''}`}>
                    <button className="section-toggle sample-toggle band-9" onClick={() => toggleSection('sampleBand9')}>
                        <FiAward size={16} className="section-icon" />
                        <span className="dropdown-title">Bài mẫu Band 9.0</span>
                        <span className="toggle-arrow">{expandedSections.sampleBand9 ? <FiChevronDown size={14} /> : <FiChevronRight size={14} />}</span>
                    </button>
                    {expandedSections.sampleBand9 && (
                        <div className="section-content sample-content band-9">
                            <p className="sample-description">Bài mẫu đạt band 9.0 cho đề bài này.</p>
                            <div className="sample-essay-text">
                                {aiFeedback.sampleEssayBand9.split('\n').map((para, idx) => <p key={idx}>{para || '\u00A0'}</p>)}
                            </div>
                        </div>
                    )}
                </div>
            )}

            {aiFeedback.wordAnalysis?.length > 0 && (
                <div className={`expandable-section ${expandedSections.wordAnalysis ? 'open' : ''}`}>
                    <button className="section-toggle" onClick={() => toggleSection('wordAnalysis')}>
                        <FiBook size={16} className="section-icon" />
                        <span className="dropdown-title">Phân tích từ vựng ({aiFeedback.wordAnalysis.length})</span>
                        <span className="toggle-arrow">{expandedSections.wordAnalysis ? <FiChevronDown size={14} /> : <FiChevronRight size={14} />}</span>
                    </button>
                    {expandedSections.wordAnalysis && (
                        <div className="section-content">
                            <div className="word-analysis-list">
                                {aiFeedback.wordAnalysis.map((word, idx) => {
                                    const itemId = `word-${idx}`;
                                    const wordTypeVi = wordTypeLabels[word.word_type?.toLowerCase()] || word.word_type;
                                    return (
                                        <div
                                            key={idx}
                                            ref={el => itemRefs.current[itemId] = el}
                                            className={`word-item usage-${word.usage_quality || 'acceptable'} ${selectedItemId === itemId ? 'selected' : ''}`}
                                        >
                                            <div className="word-header">
                                                <div className="word-title">
                                                    <span className="word-text">{word.word}</span>
                                                    {word.correction && (
                                                        <span className="word-correction">
                                                            <span className="correction-arrow">→</span>
                                                            <span className="correction-text">{word.correction}</span>
                                                        </span>
                                                    )}
                                                    {wordTypeVi && <span className="word-type">({wordTypeVi})</span>}
                                                </div>
                                                <span className={`usage-badge ${word.usage_quality || 'acceptable'}`}>
                                                    {word.usage_quality === 'good' ? '✓ Tốt' :
                                                        word.usage_quality === 'incorrect' ? '✗ Sai' : '○ Được'}
                                                </span>
                                            </div>
                                            <div className="word-definition">{word.definition}</div>
                                            <div className="word-context">
                                                <span className="context-label">Ngữ cảnh:</span> {word.context}
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    )}
                </div>
            )}
        </>
    );
}