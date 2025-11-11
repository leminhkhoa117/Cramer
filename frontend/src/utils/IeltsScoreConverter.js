export const IeltsScoreConverter = {
    /**
     * Converts an IELTS Academic Reading or Listening raw score to the corresponding band score.
     * @param {number} correctAnswers The number of correct answers (out of 40).
     * @returns {number} The corresponding IELTS band score.
     */
    convertToBand: (correctAnswers) => {
        if (correctAnswers >= 39) return 9.0;
        if (correctAnswers >= 37) return 8.5;
        if (correctAnswers >= 35) return 8.0;
        if (correctAnswers >= 33) return 7.5;
        if (correctAnswers >= 30) return 7.0;
        if (correctAnswers >= 27) return 6.5;
        if (correctAnswers >= 23) return 6.0;
        if (correctAnswers >= 19) return 5.5;
        if (correctAnswers >= 15) return 5.0;
        if (correctAnswers >= 13) return 4.5;
        if (correctAnswers >= 10) return 4.0;
        if (correctAnswers >= 8) return 3.5;
        if (correctAnswers >= 6) return 3.0;
        if (correctAnswers >= 4) return 2.5;
        return 0.0;
    }
};
