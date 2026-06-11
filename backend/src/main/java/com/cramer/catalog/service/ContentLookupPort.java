package com.cramer.catalog.service;

import com.cramer.platform.common.ielts.Skill;

import java.util.List;

/**
 * Published cross-module contract (SPEC-04 §4, SPEC-11 §5) for resolving content. Consumers
 * (assessment, speaking, abts) inject this port; they never touch catalog repositories or
 * entities. Returns records/primitives only.
 *
 * <p>Answer-bearing methods ({@link #gradableQuestions(long)}) are for <strong>server-side
 * scoring/blueprint only</strong> and are never exposed over HTTP.
 *
 * <p>Note: {@code speakingBank(...)} from SPEC-11 §5 is added together with the speaking module.
 */
public interface ContentLookupPort {

    /** Published sections for a test + skill via the FK path, ordered by part number. */
    List<SectionRef> sectionsForTest(long testId, Skill skill);

    /** Published sections via the legacy {@code exam_source}/{@code test_number} shim. */
    List<SectionRef> sectionsForExam(String examSource, int testNumber, Skill skill);

    /** Gradable questions for a section, including {@code correct_answer} (assessment scoring only). */
    List<GradableQuestion> gradableQuestions(long sectionId);

    /**
     * Full authored content (sections + questions with answer keys/explanations) for attempt
     * review (SPEC-12 §5), resolved via the legacy exam shim. Owner-review use only.
     */
    List<ReviewSection> reviewContent(String examSource, int testNumber, Skill skill);

    /** Total published questions for a test skill via the legacy exam shim (SPEC-12 §4.1). */
    int totalQuestions(String examSource, int testNumber, Skill skill);

    /**
     * Authored Speaking prompts for a test, optionally filtered to a part (SPEC-11 §5, SPEC-14 §3).
     * Resolves {@code sections.skill = speaking} via the FK path; {@code partNumber <= 0} returns
     * all parts. Used to build the frozen session blueprint.
     */
    List<SpeakingQuestionRef> speakingBank(long testId, int partNumber);
}
