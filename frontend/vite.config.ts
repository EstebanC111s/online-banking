import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// /api va al backend de Spring en el 8080
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
