package com.cramer.assessment.service;

import com.cramer.assessment.repository.UserAnswerRepository;
import com.cramer.catalog.service.TestDependencyGuard;
import org.springframework.stereotype.Component;

/**
 * Implements the catalog {@link TestDependencyGuard} SPI (SPEC-11 §4.1) so an admin cannot
 * silently destroy a test that has user attempts/answers. Resolves user data via the FK chain
 * {@code user_answers → questions → sections.test_id}.
 */
@Component
public class AttemptTestDependencyGuard implements TestDependencyGuard {

    private final UserAnswerRepository answers;

    public AttemptTestDependencyGuard(UserAnswerRepository answers) {
        this.answers = answers;
    }

    @Override
    public boolean hasUserData(long testId) {
        return answers.existsForTestId(testId);
    }
}
