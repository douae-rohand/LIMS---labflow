import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { tanstackRouter } from "@tanstack/router-plugin/vite";
import { resolve } from "path";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const backendUrl = env["VITE_BACKEND_URL"] ?? "http://localhost:8081";

  return {
    plugins: [
      // tanstackRouter doit être placé AVANT react
      tanstackRouter({ target: "react", autoCodeSplitting: true }),
      react(),
      tailwindcss(),
    ],
    resolve: {
      alias: {
        "@": resolve(__dirname, "./src"),
      },
    },
    server: {
      proxy: {
        // /api/actuator AVANT /api pour éviter que le catch-all /api ne l'absorbe
        "/api/actuator": {
          target: backendUrl,
          rewrite: (path) => path.replace(/^\/api\/actuator/, "/actuator"),
          changeOrigin: true,
        },
        "/api": {
          target: backendUrl,
          changeOrigin: true,
        },
        "/actuator": {
          target: backendUrl,
          changeOrigin: true,
        },
      },
    },
  };
});
