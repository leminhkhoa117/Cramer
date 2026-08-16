import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { fileURLToPath, URL } from 'node:url';

export default defineConfig({
  plugins: [react()],
  test: {
    // Use jsdom environment for React testing
    environment: 'jsdom',
    
    // Setup files to run before each test file
    setupFiles: ['./src/__tests__/setupTests.js'],
    
    // Global test configuration
    globals: true,
    
    // Include patterns
    include: ['src/**/*.{test,spec}.{js,jsx,ts,tsx}'],
    
    // Exclude patterns
    exclude: ['node_modules', 'dist', '.git'],
    
    // Coverage configuration
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      reportsDirectory: './coverage',
      include: [
        'src/stores/**/*.js',
        'src/components/**/*.jsx',
        'src/pages/**/*.jsx',
        'src/api/**/*.js',
        'src/utils/**/*.js',
      ],
      exclude: [
        'src/__tests__/**',
        'src/**/*.test.{js,jsx}',
        'src/**/*.spec.{js,jsx}',
      ],
    },
    
    // Reporter configuration
    reporters: ['default', 'html'],
    
    // Watch mode configuration
    watchExclude: ['node_modules', 'dist'],
    
    // Timeout for tests (10 seconds)
    testTimeout: 10000,
    
    // Mock reset
    mockReset: true,
    restoreMocks: true,
  },
  
  // Resolve aliases (match vite.config.js if you have aliases)
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
});
