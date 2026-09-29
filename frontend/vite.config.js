import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { defineConfig, loadEnv } from 'vite'
import { fileURLToPath, URL } from 'node:url'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      proxy: {
        '/api/actuator': {
          target: env.VITE_BACKEND_URL || 'http://localhost:8081',
          changeOrigin: true,
          secure: false,
          rewrite: (path) => path.replace(/^\/api\/actuator/, '/actuator'),
        },
        '/api': {
          target: env.VITE_BACKEND_URL || 'http://localhost:8081',
          changeOrigin: true,
          secure: false,
        },
        '/actuator': {
          target: env.VITE_BACKEND_URL || 'http://localhost:8081',
          changeOrigin: true,
          secure: false,
        }
      }
    }
  };
});
