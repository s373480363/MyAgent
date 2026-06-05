import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const BACKEND_DEV_TARGET = process.env.AGENT_STUDIO_BACKEND_DEV_TARGET ?? "http://127.0.0.1:8080";

/**
 * Vite 基线配置。
 */
export default defineConfig({
  plugins: [react()],
  server: {
    host: "0.0.0.0",
    port: 5173,
    proxy: {
      "/api": BACKEND_DEV_TARGET,
      "/v3/api-docs": BACKEND_DEV_TARGET,
      "/swagger-ui.html": BACKEND_DEV_TARGET,
      "/swagger-ui": BACKEND_DEV_TARGET,
      "/actuator": BACKEND_DEV_TARGET
    }
  },
  preview: {
    host: "0.0.0.0",
    port: 4173
  }
});
