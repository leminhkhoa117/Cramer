import React from 'react';
import { FiEye } from 'react-icons/fi';
import { parseOptionsFromStrings } from './questionParsers';

export default function QuestionPreview({ questionNumber, questionType, content, answer, config }) {
    const renderContent = () => {
        const text = content.text || 'Chưa có nội dung';
        const displayText = text
            .replace(/____/g, '<span class="qem-preview-blank">________</span>')
            .replace(/<b>(\d+)<\/b>/g, '<strong class="qem-preview-number">$1</strong>');

        return (
            <div
                className="qem-preview-text"
                dangerouslySetInnerHTML={{ __html: displayText }}
            />
        );
    };

    const renderOptions = () => {
        if (!config) return null;

        if (config.category === 'boolean') {
            return (
                <div className="qem-preview-options">
                    {config.options.map((option) => (
                        <label key={option} className="qem-preview-option">
                            <span className={`qem-preview-radio ${answer[0] === option ? 'qem-preview-radio--selected' : ''}`}>
                                {answer[0] === option ? '●' : '○'}
                            </span>
                            <span>{option}</span>
                        </label>
                    ))}
                </div>
            );
        }

        if (config.category === 'choice' && content.options) {
            const parsedOptions = parseOptionsFromStrings(content.options);
            return (
                <div className="qem-preview-options">
                    {parsedOptions.map((option) => (
                        <label key={option.letter} className="qem-preview-option">
                            <span className={`qem-preview-radio ${answer.includes(option.letter) ? 'qem-preview-radio--selected' : ''}`}>
                                {answer.includes(option.letter) ? '●' : '○'}
                            </span>
                            <span><strong>{option.letter}</strong> {option.text}</span>
                        </label>
                    ))}
                </div>
            );
        }

        if (config.category === 'matching') {
            return (
                <div className="qem-preview-matching">
                    <span className="qem-preview-answer-box">
                        {answer[0] || '?'}
                    </span>
                </div>
            );
        }

        if (config.category === 'text') {
            return (
                <div className="qem-preview-text-answer">
                    Đáp án: <strong>{answer.join(' / ') || '____'}</strong>
                </div>
            );
        }

        return null;
    };

    return (
        <div className="qem-preview">
            <div className="qem-preview-header">
                <FiEye />
                <span>Xem trước</span>
            </div>

            <div className="qem-preview-content">
                <div className="qem-preview-question">
                    <span className="qem-preview-qnum">{questionNumber}.</span>
                    {content.section_title && (
                        <div className="qem-preview-section-title">
                            {content.section_title}
                        </div>
                    )}
                    {renderContent()}
                </div>

                {renderOptions()}
            </div>

            <div className="qem-preview-type-badge">
                {config?.label || questionType}
            </div>
        </div>
    );
}