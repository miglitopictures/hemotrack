import { createFileRoute } from "@tanstack/react-router";
import { MapPin, PackageCheck, Truck } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import { TransporteCard } from "@/components/transporte-card";
import {
  EstadoErro,
  EstadoVazio,
  PageHeader,
  Panel,
  SkeletonPagina,
  StatCard,
  Stagger,
  StaggerItem,
} from "@/components/ui-kit";
import { listarMeusTransportes } from "@/lib/api";

export const Route = createFileRoute("/hospital/transportes/")({
  head: () => ({
    meta: [
      { title: "Transportes — HemoTrack" },
      {
        name: "description",
        content:
          "Remessas de hemocomponentes a caminho do hospital, com veículo, temperatura e previsão de entrega.",
      },
      { property: "og:title", content: "Transportes — HemoTrack" },
      { property: "og:description", content: "Remessas em rota e entregas concluídas." },
    ],
  }),
  loader: async () => ({ transportes: await listarMeusTransportes() }),
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
  component: TransportesHospital,
});

function TransportesHospital() {
  const { transportes: meus } = Route.useLoaderData();
  const emRota = meus.filter((t) => t.status === "Em transporte");
  const entregues = meus.filter((t) => t.status === "Entregue").length;
  // Só conta o que está de fato em trânsito (antes incluía o que ainda estava
  // em separação no hemocentro).
  const bolsasEmRota = emRota.reduce((acc, t) => acc + t.bolsas.length, 0);

  return (
    <AppShell role="hospital">
      <PageHeader
        title="Transportes"
        subtitle="Remessas de hemocomponentes a caminho do Hospital Santa Clara"
      />

      <Stagger className="grid gap-4 sm:grid-cols-3">
        <StaggerItem className="h-full">
          <StatCard
            label="Em rota"
            value={emRota.length}
            hint="Veículos refrigerados ativos"
            tone="primary"
            icon={<Truck className="size-4" />}
            highlight
          />
        </StaggerItem>
        <StaggerItem className="h-full">
          <StatCard
            label="Bolsas em trânsito"
            value={bolsasEmRota}
            hint="Cargas monitoradas agora"
            tone="teal"
            icon={<PackageCheck className="size-4" />}
          />
        </StaggerItem>
        <StaggerItem className="h-full">
          <StatCard
            label="Entregas concluídas"
            value={entregues}
            hint="Confirmadas pelo hospital"
            tone="success"
            icon={<MapPin className="size-4" />}
          />
        </StaggerItem>
      </Stagger>

      <Panel title="Remessas" description="Clique para acompanhar o transporte em detalhe">
        {meus.length === 0 ? (
          <EstadoVazio
            titulo="Nenhum transporte a caminho"
            descricao="Assim que o hemocentro despachar uma remessa, ela aparece aqui."
          />
        ) : (
          <div className="grid gap-4 p-5 lg:grid-cols-2">
            {meus.map((t, i) => (
              <TransporteCard key={t.id} transporte={t} papel="hospital" atraso={i * 0.06} />
            ))}
          </div>
        )}
      </Panel>
    </AppShell>
  );
}
