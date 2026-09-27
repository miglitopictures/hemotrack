import { Outlet, createFileRoute, redirect } from "@tanstack/react-router";

import { temSessaoValida } from "@/lib/sessao";

/**
 * Rota de layout do /hospital. No roteamento por arquivos do TanStack,
 * "hospital.tsx" é o pai de todos os "hospital.*.tsx" — então este
 * beforeLoad protege /hospital e tudo que está abaixo dele.
 *
 * Não desenha nada: só deixa passar, ou redireciona.
 */
export const Route = createFileRoute("/hospital")({
  beforeLoad: () => {
    if (!temSessaoValida()) {
      throw redirect({ to: "/login" });
    }
  },
  component: () => <Outlet />,
});