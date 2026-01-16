/**
 * MSW Server Setup for Vitest
 * 
 * Creates and exports a mock server for use in tests.
 * Import this in setupTests.js for integration tests.
 * 
 * @author Cramer Test Team
 * @since 2026-01-11
 */

import { setupServer } from 'msw/node';
import { handlers } from './handlers.js';

// Create the mock server with the handlers
export const server = setupServer(...handlers);

// Export for use in individual tests that need custom handlers
export { handlers };
