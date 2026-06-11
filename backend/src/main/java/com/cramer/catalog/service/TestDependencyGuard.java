package com.cramer.catalog.service;

/**
 * Inbound SPI (SPEC-11 §4.1): lets the owning module of user data (assessment) veto destruction
 * of a test that has dependent attempts/answers. Catalog defines the contract; the implementation
 * is provided by {@code assessment} and injected optionally (absent ⇒ treated as "no user data").
 * This keeps catalog free of any dependency on assessment.
 */
public interface TestDependencyGuard {

    /** @return true if the given test has dependent user data (attempts/answers/writing). */
    boolean hasUserData(long testId);
}
