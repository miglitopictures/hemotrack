import { Outlet, createFileRoute, redirect } from "@tanstack/react-router";

import { temSessaoValida } from "@/lib/sessao";

export const Route = createFileRoute("/hemocentro")({
  beforeLoad: () => {
    if (!temSessaoValida()) {
      throw redirect({ to: "/login" });
    }
  },
  component: () => <Outlet />,
});