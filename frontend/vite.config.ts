import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import tsconfigPaths from "vite-tsconfig-paths";
import { tanstackRouter } from "@tanstack/router-plugin/vite";
import path from "path";

export default defineConfig({
  plugins: [
    // O plugin do TanStack Router precisa vir ANTES do plugin do React.
    // Ele lê os arquivos de src/routes e gera src/routeTree.gen.ts
    // automaticamente — é o mesmo mecanismo de antes, só que agora em
    // modo "react" simples, sem o SSR do Start.
    tanstackRouter({
      target: "react",
      autoCodeSplitting: true,
    }),
    react(),
    tailwindcss(),
    tsconfigPaths(),
  ],
  server: {
    port: 8081,
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  build: {
    target: "esnext",
    minify: "esbuild",
  },
});
