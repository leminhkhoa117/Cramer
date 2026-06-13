import React from 'react';
import { FiCheck, FiInfo, FiPlus, FiTrash2 } from 'react-icons/fi';
import { parseOptionsFromStrings } from './questionParsers';

const TextAnswerEditor = ({ answer, onChange }) => {
    const primaryAnswer = answer[0] || '';
    const alternatives = answer.slice(1);

    const handlePrimaryChange = (value) => {
        onChange([value, ...alternatives]);
    };

    const handleAltChange = (index, value) => {
        const newAlternatives = [...alternatives];
        newAlternatives[index] = value;
        onChange([primaryAnswer, ...newAlternatives]);
    };

    const handleAddAlt = () => {
        onChange([primaryAnswer, ...alternatives, '']);
    };

    const handleRemoveAlt = (index) => {
        const newAlternatives = alternatives.filter((_, alternativeIndex) => alternativeIndex !== index);
        onChange([primaryAnswer, ...newAlternatives]);
    };

    return (
        <div className="qem-editor-section">
            <div className="qem-editor-header">
                <FiCheck />
                <span>Đáp án đúng</span>
            </div>

            <div className="form-group">
                <label>Đáp án chính</label>
                <input
                    type="text"
                    className="form-input"
                    value={primaryAnswer}
                    onChange={(event) => handlePrimaryChange(event.target.value)}
                    placeholder="Nhập đáp án đúng..."
                />
            </div>

            {alternatives.length > 0 && (
                <div className="form-group">
                    <label>Đáp án thay thế (chấp nhận)</label>
                    {alternatives.map((alternative, index) => (
                        <div key={index} className="qem-alt-answer">
                            <input
                                type="text"
                                className="form-input"
                                value={alternative}
                                onChange={(event) => handleAltChange(index, event.target.value)}
                                placeholder="Đáp án thay thế..."
                            />
                            <button
                                type="button"
                                className="qem-icon-btn qem-icon-btn--danger"
                                onClick={() => handleRemoveAlt(index)}
                            >
                                <FiTrash2 size={14} />
                            </button>
                        </div>
                    ))}
                </div>
            )}

            <button
                type="button"
                className="qem-add-btn qem-add-btn--small"
                onClick={handleAddAlt}
            >
                <FiPlus size={12} />
                Thêm đáp án thay thế
            </button>
        </div>
    );
};

const BooleanAnswerEditor = ({ answer, onChange, options }) => {
    const selectedAnswer = answer[0] || '';

    return (
        <div className="qem-editor-section">
            <div className="qem-editor-header">
                <FiCheck />
                <span>Đáp án đúng</span>
            </div>

            <div className="qem-radio-group">
                {options.map((option) => (
                    <label key={option} className="qem-radio-option">
                        <input
                            type="radio"
                            name="booleanAnswer"
                            value={option}
                            checked={selectedAnswer === option}
                            onChange={(event) => onChange([event.target.value])}
                        />
                        <span className={`qem-radio-label qem-radio-label--${option.toLowerCase().replace(' ', '-')}`}>
                            {option}
                        </span>
                    </label>
                ))}
            </div>
        </div>
    );
};

const SingleSelectAnswerEditor = ({ answer, onChange, options }) => {
    const selectedAnswer = answer[0] || '';
    const parsedOptions = parseOptionsFromStrings(options);

    return (
        <div className="qem-editor-section">
            <div className="qem-editor-header">
                <FiCheck />
                <span>Chọn đáp án đúng</span>
            </div>

            <div className="qem-radio-group qem-radio-group--options">
                {parsedOptions.map((option) => (
                    <label key={option.letter} className="qem-radio-option qem-radio-option--full">
                        <input
                            type="radio"
                            name="singleAnswer"
                            value={option.letter}
                            checked={selectedAnswer === option.letter}
                            onChange={(event) => onChange([event.target.value])}
                        />
                        <span className="qem-option-letter">{option.letter}</span>
                        <span className="qem-option-text">{option.text}</span>
                    </label>
                ))}
            </div>

            {parsedOptions.length === 0 && (
                <div className="qem-empty-state">
                    Chưa có lựa chọn nào. Vui lòng thêm options ở phần Nội dung câu hỏi.
                </div>
            )}
        </div>
    );
};

const MultiSelectAnswerEditor = ({ answer, onChange, options }) => {
    const selectedAnswers = answer || [];
    const parsedOptions = parseOptionsFromStrings(options);

    const handleToggle = (letter) => {
        if (selectedAnswers.includes(letter)) {
            onChange(selectedAnswers.filter((answerLetter) => answerLetter !== letter));
        } else {
            onChange([...selectedAnswers, letter].sort());
        }
    };

    return (
        <div className="qem-editor-section">
            <div className="qem-editor-header">
                <FiCheck />
                <span>Chọn tất cả đáp án đúng</span>
            </div>

            <div className="qem-checkbox-group">
                {parsedOptions.map((option) => (
                    <label key={option.letter} className="qem-checkbox-option">
                        <input
                            type="checkbox"
                            checked={selectedAnswers.includes(option.letter)}
                            onChange={() => handleToggle(option.letter)}
                        />
                        <span className="qem-option-letter">{option.letter}</span>
                        <span className="qem-option-text">{option.text}</span>
                    </label>
                ))}
            </div>

            {selectedAnswers.length > 0 && (
                <div className="qem-selected-summary">
                    Đã chọn: <strong>{selectedAnswers.join(', ')}</strong>
                </div>
            )}
        </div>
    );
};

const LetterAnswerEditor = ({ answer, onChange, matchingOptions = [] }) => {
    const selectedAnswer = answer[0] || '';
    const availableLetters = matchingOptions.length > 0
        ? matchingOptions
        : ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'];

    return (
        <div className="qem-editor-section">
            <div className="qem-editor-header">
                <FiCheck />
                <span>Đáp án ghép nối</span>
            </div>

            <div className="form-group">
                <label>Chọn lựa chọn đúng</label>
                <select
                    className="form-select"
                    value={selectedAnswer}
                    onChange={(event) => onChange([event.target.value])}
                >
                    <option value="">-- Chọn đáp án --</option>
                    {availableLetters.map((letter) => (
                        <option key={letter} value={letter}>
                            {letter}
                        </option>
                    ))}
                </select>
            </div>

            <div className="qem-tip">
                <FiInfo size={14} />
                <span>
                    Đáp án phải khớp với một trong các options được định nghĩa ở Section Layout.
                </span>
            </div>
        </div>
    );
};

export default function AnswerEditors({ config, answer, onChange, contentOptions, matchingOptions }) {
    if (!config) return null;

    switch (config.answerType) {
        case 'text':
            return (
                <TextAnswerEditor
                    answer={answer}
                    onChange={onChange}
                />
            );
        case 'tfng':
        case 'ynng':
            return (
                <BooleanAnswerEditor
                    answer={answer}
                    onChange={onChange}
                    options={config.options}
                />
            );
        case 'single-select':
            return (
                <SingleSelectAnswerEditor
                    answer={answer}
                    onChange={onChange}
                    options={contentOptions || []}
                />
            );
        case 'multi-select':
            return (
                <MultiSelectAnswerEditor
                    answer={answer}
                    onChange={onChange}
                    options={contentOptions || []}
                />
            );
        case 'letter':
            return (
                <LetterAnswerEditor
                    answer={answer}
                    onChange={onChange}
                    matchingOptions={matchingOptions}
                />
            );
        default:
            return (
                <TextAnswerEditor
                    answer={answer}
                    onChange={onChange}
                />
            );
    }
}