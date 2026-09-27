import { createFileRoute, redirect } from "@tanstack/react-router";

import { AppShell } from "@/components/app-shell";
import { MembrosPainel } from "@/components/membros-painel";
import { PageHeader } from "@/components/ui-kit";
import { CHAVE_USUARIO_LOGADO, buscarUsuarioLogado } from "@/lib/api-auth";

export const Route = createFileRoute("/hospital/membros")({
  beforeLoad: async ({ context }) => {
    // A rota pai /hospital já cuidou da sessão e da instituição pendente.
    // Aqui só falta o papel. O ensureQueryData reaproveita o cache que ela
    // preencheu, então isso não dispara requisição nova.
    const usuario = await context.queryClient.ensureQueryData({
      queryKey: CHAVE_USUARIO_LOGADO,
      queryFn: buscarUsuarioLogado,
    });

    if (usuario.papel !== "ADMIN_INSTITUICAO" || usuario.instituicaoId === null) {
      // Operador que digitou a URL na mão volta para o painel, não para o
      // login: ele está autenticado, só não é dono desta tela.
      throw redirect({ to: "/hospital" });
    }

    // Devolvido daqui, vira contexto tipado da rota — a página recebe um
    // number, não um "number | null" que ela teria que destratar de novo.
    return { instituicaoId: usuario.instituicaoId };
  },

  head: () => ({
    meta: [
      { title: "Membros — HemoTrack" },
      {
        name: "description",
        content: "Cadastro e desativação dos operadores da instituição.",
      },
    ],
  }),

  component: MembrosHospitalPage,
});

function MembrosHospitalPage() {
  const { instituicaoId } = Route.useRouteContext();

  return (
    <AppShell role="hospital">
      <PageHeader
        title="Membros"
        subtitle="Cadastre operadores e controle quem tem acesso ao sistema."
      />
      <div className="mt-6">
        <MembrosPainel instituicaoId={instituicaoId} />
      </div>
    </AppShell>
  );
}
