import { createFileRoute } from "@tanstack/react-router";
import { AlertTriangle, MapPin, PackageCheck, Truck } from "lucide-react";
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
import { listarTransportes } from "@/lib/api";
import { transporteComAlerta } from "@/lib/data";

export const Route = createFileRoute("/hemocentro/distribuicoes/")({
  head: () => ({
    meta: [
      { title: "Distribuição e transporte — HemoTrack" },
      {
        name: "description",
        content:
          "Acompanhe as remessas de hemocomponentes: rotas, veículos, temperatura da carga e previsão de entrega.",
      },
      { property: "og:title", content: "Distribuição e transporte — HemoTrack" },
      { property: "og:description", content: "Remessas em separação, em rota e entregues." },
    ],
  }),
  loader: async () => ({ transportes: await listarTransportes() }),
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
  component: Distribuicoes,
});

function Distribuicoes() {
  const { transportes } = Route.useLoaderData();
  const emRota = transportes.filter((t) => t.status === "Em transporte");
  const entregues = transportes.filter((t) => t.status === "Entregue").length;
  const bolsasEmRota = emRota.reduce((acc, t) => acc + t.bolsas.length, 0);
  const comAlerta = transportes.filter(transporteComAlerta).length;

  return (
    <AppShell role="hemocentro">
      <PageHeader
        title="Distribuição e transporte"
        subtitle="Remessas de hemocomponentes do hemocentro para os hospitais"
      />

      <Stagger className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
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
        <StaggerItem className="h-full">
          <StatCard
            label="Remessas com desvio de temperatura"
            value={comAlerta}
            hint="Verificar cadeia de frio"
            tone="warning"
            icon={<AlertTriangle className="size-4" />}
          />
        </StaggerItem>
      </Stagger>

      <Panel title="Remessas" description="Clique para monitorar o transporte em detalhe">
        {transportes.length === 0 ? (
          <EstadoVazio titulo="Nenhuma remessa registrada" />
        ) : (
          <div className="grid gap-4 p-5 lg:grid-cols-2">
            {transportes.map((t, i) => (
              <TransporteCard key={t.id} transporte={t} papel="hemocentro" atraso={i * 0.06} />
            ))}
          </div>
        )}
      </Panel>
    </AppShell>
  );
}
