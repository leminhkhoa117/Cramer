export const parseQuestionContent = (content) => {
    if (!content) return { text: '' };
    if (typeof content === 'object') return content;

    try {
        return JSON.parse(content);
    } catch {
        return { text: String(content) };
    }
};

export const parseCorrectAnswer = (answer) => {
    if (!answer) return [];
    if (Array.isArray(answer)) return answer;
    if (typeof answer === 'object') return [answer];

    try {
        const parsed = JSON.parse(answer);
        return Array.isArray(parsed) ? parsed : [parsed];
    } catch {
        return [String(answer)];
    }
};

export const parseOptionsFromStrings = (options) => {
    if (!Array.isArray(options)) return [];

    return options.map((option) => {
        if (typeof option === 'object' && option.letter) return option;

        const optionText = String(option);
        const match = optionText.match(/^([A-Za-z])\s+(.+)$/);
        if (match) {
            return { letter: match[1].toUpperCase(), text: match[2] };
        }

        const letterMatch = optionText.match(/^([A-Za-z])\s*[-.):]?\s*(.*)$/);
        if (letterMatch) {
            return { letter: letterMatch[1].toUpperCase(), text: letterMatch[2] || optionText };
        }

        return { letter: '', text: optionText };
    });
};

export const optionsToStrings = (options) => {
    if (!Array.isArray(options)) return [];

    return options.map((option) => {
        if (typeof option === 'string') return option;
        return `${option.letter} ${option.text}`;
    });
};

export const getNextLetter = (options) => {
    if (!options || options.length === 0) return 'A';

    const letters = options.map((option) => option.letter).filter(Boolean).sort();
    const lastLetter = letters[letters.length - 1] || '@';
    return String.fromCharCode(lastLetter.charCodeAt(0) + 1);
};

export const parseExplanation = (explanation) => {
    if (!explanation) {
        return { detail: '', quote: '', strategy: '' };
    }

    let parsed = explanation;
    if (typeof explanation === 'string') {
        try {
            parsed = JSON.parse(explanation);
        } catch {
            return { detail: explanation, quote: '', strategy: '' };
        }
    }

    if (typeof parsed === 'object') {
        return {
            detail: parsed.detail || parsed.giaiThich || parsed.giai_thich || '',
            quote: parsed.quote || parsed.trichDan || parsed.trich_dan || '',
            strategy: parsed.strategy || parsed.chienlược || parsed.chien_luoc || '',
        };
    }

    return { detail: String(explanation), quote: '', strategy: '' };
};

export const explanationToString = (structured) => ({
    detail: structured.detail || '',
    quote: structured.quote || '',
    strategy: structured.strategy || '',
});