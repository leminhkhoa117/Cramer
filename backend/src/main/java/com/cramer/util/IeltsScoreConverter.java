package com.cramer.util;

public class IeltsScoreConverter {

    /**
     * Converts an IELTS Academic Reading or Listening raw score (number of correct answers)
     * to the corresponding band score. The conversion table is based on the standard
     * 40-question test format.
     *
     * @param correctAnswers The number of correct answers, from 0 to 40.
     * @return The corresponding IELTS band score as a double (e.g., 7.5).
     */
    public static double convertToBand(int correctAnswers) {
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
        // Note: Scores below 10 fall into bands below 4.0, 
        // but the provided table stops here. We can extrapolate or return a base value.
        // For now, let's add a few more common values.
        if (correctAnswers >= 8) return 3.5;
        if (correctAnswers >= 6) return 3.0;
        if (correctAnswers >= 4) return 2.5;
        return 0.0; // Or handle as an error/exception
    }
}
