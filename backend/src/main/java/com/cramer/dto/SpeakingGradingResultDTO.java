package com.cramer.dto;

import java.util.List;

/**
 * DTO for IELTS Speaking grading result from AI evaluation.
 * Contains overall scores, criterion-specific scores, and detailed feedback.
 */
public class SpeakingGradingResultDTO {

    private Double overallBand;
    private Double fluencyCoherence;
    private Double lexicalResource;
    private Double grammaticalRange;
    private Double pronunciation;
    private Feedback feedback;
    private List<PartScore> partScores;
    private String transcript;

    // Getters and Setters

    public Double getOverallBand() {
        return overallBand;
    }

    public void setOverallBand(Double overallBand) {
        this.overallBand = overallBand;
    }

    public Double getFluencyCoherence() {
        return fluencyCoherence;
    }

    public void setFluencyCoherence(Double fluencyCoherence) {
        this.fluencyCoherence = fluencyCoherence;
    }

    public Double getLexicalResource() {
        return lexicalResource;
    }

    public void setLexicalResource(Double lexicalResource) {
        this.lexicalResource = lexicalResource;
    }

    public Double getGrammaticalRange() {
        return grammaticalRange;
    }

    public void setGrammaticalRange(Double grammaticalRange) {
        this.grammaticalRange = grammaticalRange;
    }

    public Double getPronunciation() {
        return pronunciation;
    }

    public void setPronunciation(Double pronunciation) {
        this.pronunciation = pronunciation;
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
    }

    public List<PartScore> getPartScores() {
        return partScores;
    }

    public void setPartScores(List<PartScore> partScores) {
        this.partScores = partScores;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    /**
     * Nested class for feedback details.
     */
    public static class Feedback {
        private List<String> strengths;
        private List<String> weaknesses;
        private List<String> recommendations;
        private String fluencyNotes;
        private String lexicalNotes;
        private String grammarNotes;
        private String pronunciationNotes;

        public List<String> getStrengths() {
            return strengths;
        }

        public void setStrengths(List<String> strengths) {
            this.strengths = strengths;
        }

        public List<String> getWeaknesses() {
            return weaknesses;
        }

        public void setWeaknesses(List<String> weaknesses) {
            this.weaknesses = weaknesses;
        }

        public List<String> getRecommendations() {
            return recommendations;
        }

        public void setRecommendations(List<String> recommendations) {
            this.recommendations = recommendations;
        }

        public String getFluencyNotes() {
            return fluencyNotes;
        }

        public void setFluencyNotes(String fluencyNotes) {
            this.fluencyNotes = fluencyNotes;
        }

        public String getLexicalNotes() {
            return lexicalNotes;
        }

        public void setLexicalNotes(String lexicalNotes) {
            this.lexicalNotes = lexicalNotes;
        }

        public String getGrammarNotes() {
            return grammarNotes;
        }

        public void setGrammarNotes(String grammarNotes) {
            this.grammarNotes = grammarNotes;
        }

        public String getPronunciationNotes() {
            return pronunciationNotes;
        }

        public void setPronunciationNotes(String pronunciationNotes) {
            this.pronunciationNotes = pronunciationNotes;
        }
    }

    /**
     * Nested class for per-part scores.
     */
    public static class PartScore {
        private Integer partNumber;
        private Double score;
        private String notes;

        public Integer getPartNumber() {
            return partNumber;
        }

        public void setPartNumber(Integer partNumber) {
            this.partNumber = partNumber;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}
