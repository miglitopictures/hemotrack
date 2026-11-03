import { createFileRoute, notFound } from "@tanstack/react-router";
import { AppShell } from "@/components/app-shell";
import { MonitorTransporte } from "@/components/monitor-transporte";
import { EstadoErro, SkeletonPagina } from "@/components/ui-kit";
import { obterTransporte, transporteDaMinhaInstituicao } from "@/lib/api";

export const Route = createFileRoute("/hospital/transportes/$id")({
  head: () => ({
    meta: [
      { title: "Acompanhar transporte — HemoTrack" },
      {
        name: "description",
        content:
          "Acompanhe em tempo real a remessa de hemocomponentes a caminho do seu hospital: rota, temperatura e bolsas embarcadas.",
      },
      { property: "og:title", content: "Acompanhar transporte — HemoTrack" },
      {
        property: "og:description",
        content: "Rota, temperatura e status da remessa em tempo real.",
      },
    ],
  }),
  loader: async ({ params }) => {
    const transporte = await obterTransporte(params.id);
    // 404 tanto para id inexistente quanto para remessa de outro hospital
    if (!transporte || !transporteDaMinhaInstituicao(transporte)) throw notFound();
    return { transporte };
  },
  pendingComponent: () => (
    <AppShell role="hospital">
      <SkeletonPagina />
    </AppShell>
  ),
  errorComponent: ({ error, reset }) => (
    <AppShell role="hospital">
      <EstadoErro erro={error} aoTentarDeNovo={reset} />
    </AppShell>
  ),
  component: AcompanharTransporte,
});

function AcompanharTransporte() {
  const { transporte } = Route.useLoaderData();
  return (
    <AppShell role="hospital">
      <MonitorTransporte key={transporte.id} inicial={transporte} papel="hospital" />
    </AppShell>
  );
}
