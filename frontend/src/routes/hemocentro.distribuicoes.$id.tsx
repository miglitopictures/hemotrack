import { createFileRoute, notFound } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import { DetalheTransporte } from "@/components/detalhe-transporte";
import { EstadoErro, SkeletonPagina } from "@/components/ui-kit";
import { obterTransporte } from "@/lib/api";

export const Route = createFileRoute("/hemocentro/distribuicoes/$id")({
  head: () => ({
    meta: [
      { title: "Monitoramento do transporte — HemoTrack" },
      {
        name: "description",
        content:
          "Rastreie a remessa de hemocomponentes em tempo real: rota, temperatura da carga, bolsas embarcadas e alertas.",
      },
      { property: "og:title", content: "Monitoramento do transporte — HemoTrack" },
      {
        property: "og:description",
        content: "Rota, temperatura e status da remessa em tempo real.",
      },
    ],
  }),
  loader: async ({ params }) => {
    const transporte = await obterTransporte(params.id);
    if (!transporte) throw notFound();
    return { transporte };
  },
  pendingComponent: () => (
    <AppShell role="hemocentro">
      <SkeletonPagina />
    </AppShell>
  ),
  errorComponent: ({ error, reset }) => (
    <AppShell role="hemocentro">
      <EstadoErro erro={error} aoTentarDeNovo={reset} />
    </AppShell>
  ),
  component: MonitoramentoTransporte,
});

function MonitoramentoTransporte() {
  const { transporte } = Route.useLoaderData();
  return (
    <AppShell role="hemocentro">
      <DetalheTransporte transporte={transporte} papel="hemocentro" />
    </AppShell>
  );
}
