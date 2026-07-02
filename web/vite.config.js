import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

const SERVER = "http://localhost:8080";

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/user": SERVER,
      "/session": SERVER,
      "/game": SERVER,
      "/moves": SERVER,
      "/db": SERVER,
      "/ws": { target: "ws://localhost:8080", ws: true },
    },
  },
});
