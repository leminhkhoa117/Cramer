import { useEffect, useRef } from 'react';
import { useTestStore, useTestSessionStore } from '../stores';

const AUTO_SAVE_INTERVAL_MS = 30_000;

/**
 * Custom hook that periodically auto-saves test progress (answers/essays + timer + currentPart).
 *
 * Guards:
 *  - Skips if testStatus !== 'running' (submitted, idle, loading, error)
 *  - Skips if isSubmitting (final submission in flight)
 *  - Skips if isSavingProgress (another save already in flight, including manual Save & Exit)
 *  - Skips if no attempt.id (test hasn't started)
 *  - Skips if data hasn't changed since last successful save (dirty check)
 *
 * Also registers a `beforeunload` listener to warn users about unsaved changes.
 *
 * @param {boolean} active - whether auto-save should be active (e.g., test is running)
 * @param {'reading'|'listening'|'writing'} skill - test skill type
 */
export default function useAutoSave(active, skill) {
    const timerRef = useRef(null);
    const lastSnapshotRef = useRef(null);

    useEffect(() => {
        if (!active) return;

        timerRef.current = setInterval(() => {
            const store = useTestStore.getState();
            const sessionStore = useTestSessionStore.getState();

            if (store.testStatus !== 'running') return;
            if (store.isSubmitting) return;
            if (store.isSavingProgress) return;
            if (!store.attempt?.id) return;

            // Set saving flag BEFORE building payload to minimise
            // the race window with manual Save & Exit
            store.setIsSavingProgress(true);

            const payload = store.getAutoSavePayload(skill);
            const snapshot = JSON.stringify(payload);
            if (snapshot === lastSnapshotRef.current) {
                store.setIsSavingProgress(false);
                return;
            }

            sessionStore.saveProgress(store.attempt.id, payload)
                .then(() => {
                    lastSnapshotRef.current = snapshot;
                })
                .catch(() => {
                    // Silently ignore — retry next cycle
                })
                .finally(() => {
                    store.setIsSavingProgress(false);
                });
        }, AUTO_SAVE_INTERVAL_MS);

        return () => {
            clearInterval(timerRef.current);
            timerRef.current = null;
            lastSnapshotRef.current = null;
        };
    }, [active, skill]);

    // beforeunload: warn if test is in progress
    useEffect(() => {
        if (!active) return;

        const handler = (e) => {
            const store = useTestStore.getState();
            if (store.testStatus !== 'running') return;
            if (store.isSubmitting) return;

            e.preventDefault();
            e.returnValue = '';
        };

        window.addEventListener('beforeunload', handler);
        return () => window.removeEventListener('beforeunload', handler);
    }, [active]);
}
