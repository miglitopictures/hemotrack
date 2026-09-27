import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/admin")({
  component: () => <p className="p-8">Administração — em construção.</p>,
});