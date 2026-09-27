import { Outlet, createFileRoute, redirect } from "@tanstack/react-router";

import { CHAVE_USUARIO_LOGADO, buscarUsuarioLogado } from "@/lib/api-auth";
import { temSessaoValida } from "@/lib/sessao";

export const Route = createFileRoute("/hemocentro")({
  beforeLoad: async ({ context }) => {
    if (!temSessaoValida()) {
      throw redirect({ to: "/login" });
    }

    // O status da instituição NÃO está no token — só em /auth/me. O
    // ensureQueryData usa o cache quando já há dado e busca quando não há.
    const usuario = await context.queryClient.ensureQueryData({
      queryKey: CHAVE_USUARIO_LOGADO,
      queryFn: buscarUsuarioLogado,
    });

    if (usuario.instituicao?.status === "PENDENTE_APROVACAO") {
      throw redirect({ to: "/aguardando-aprovacao" });
    }
  },
  component: () => <Outlet />,
});