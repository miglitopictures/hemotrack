import { createFileRoute, Link } from "@tanstack/react-router";
import { motion } from "motion/react";
import { useState } from "react";
import { AlertTriangle, Boxes, Inbox, Truck } from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
} from "recharts";
import { AppShell } from "@/components/app-shell";
import { TransporteCard } from "@/components/transporte-card";
import {
  EstadoErro,
  EstadoVazio,
  Grafico,
  PageHeader,
  Panel,
  PrioridadeBadge,
  SkeletonPagina,
  StatCard,
  Stagger,
  StaggerItem,
  StatusBadge,
  eixoTema,
  tooltipTema,
} from "@/components/ui-kit";
import { listarSolicitacoes, listarTransportes, obterAlertas } from "@/lib/api";
import {
  bolsasVencendoEm,
  estoquePorTipo,
  formatarDataHora,
  totalDisponivel,
} from "@/lib/data";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/hemocentro/")({
  head: () => ({
    meta: [
      { title: "Painel do hemocentro — HemoTrack" },
      {
        name: "description",
        content:
          "Visão geral do hemocentro: estoque por tipo sanguíneo, solicitações pendentes, transportes ativos e alertas críticos.",
      },
      { property: "og:title", content: "Painel do hemocentro — HemoTrack" },
      {
        property: "og:description",
        content: "Estoque, solicitações e transportes do hemocentro em tempo real.",
      },
    ],
  }),
  loader: async () => ({
    solicitacoes: await listarSolicitacoes(),
    transportes: await listarTransportes(),
    alertas: await obterAlertas(),
  }),
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
  component: HemocentroDashboard,
});

type Aba = "solicitacoes" | "transportes";

function HemocentroDashboard() {
  const { solicitacoes, transportes, alertas } = Route.useLoaderData();
  const [aba, setAba] = useState<Aba>("solicitacoes");

  const pendentes = solicitacoes.filter((s) => s.status === "Pendente");
  const emTransporte = transportes.filter((t) => t.status === "Em transporte");
  const vencendo = bolsasVencendoEm(3).length;
  const abaixoDoMinimo = estoquePorTipo.filter((t) => t.bolsas < t.minimo).length;

  return (
    <AppShell role="hemocentro">
      <PageHeader
        title="Painel do hemocentro"
        subtitle="Hemocentro Regional Recife · operação de hoje"
        actions={
          <Link to="/hemocentro/solicitacoes">
            <motion.span
              whileHover={{ y: -2 }}
              whileTap={{ scale: 0.96 }}
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground shadow-card"
            >
              <Inbox className="size-4" aria-hidden="true" /> Analisar solicitações
            </motion.span>
          </Link>
        }
      />

      <Stagger className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StaggerItem className="h-full">
          <StatCard
            label="Solicitações pendentes"
            value={pendentes.length}
            hint="Aguardando análise"
            tone="warning"
            icon={<Inbox className="size-4" />}
            highlight
          />
        </StaggerItem>
        <StaggerItem className="h-full">
          <StatCard
            label="Bolsas disponíveis"
            value={totalDisponivel}
            hint="Prontas para distribuição"
            tone="teal"
            icon={<Boxes className="size-4" />}
          />
        </StaggerItem>
        <StaggerItem className="h-full">
          <StatCard
            label="Transportes ativos"
            value={emTransporte.length}
            hint="Em rota agora"
            tone="primary"
            icon={<Truck className="size-4" />}
          />
        </StaggerItem>
        <StaggerItem className="h-full">
          <StatCard
            label="Vencem em até 3 dias"
            value={vencendo}
            hint={
              abaixoDoMinimo > 0
                ? `${abaixoDoMinimo} tipo(s) abaixo do mínimo`
                : "Estoque dentro do mínimo"
            }
            tone="warning"
            icon={<AlertTriangle className="size-4" />}
          />
        </StaggerItem>
      </Stagger>

      <div className="grid gap-6 lg:grid-cols-3">
        <Panel
          title="Estoque por tipo sanguíneo"
          description="Bolsas disponíveis · a linha marca o estoque mínimo"
          className="lg:col-span-2"
        >
          <Grafico
            titulo="Bolsas disponíveis por tipo sanguíneo"
            resumo={estoquePorTipo
              .map((t) => `${t.tipo}: ${t.bolsas} de mínimo ${t.minimo}`)
              .join(", ")}
            altura="h-64"
          >
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={estoquePorTipo}>
                <CartesianGrid vertical={false} stroke="var(--border)" />
                <XAxis dataKey="tipo" {...eixoTema} />
                <Tooltip {...tooltipTema} />
                <Bar dataKey="bolsas" name="Disponíveis" radius={[8, 8, 0, 0]}>
                  {estoquePorTipo.map((t) => (
                    <Cell
                      key={t.tipo}
                      fill={t.bolsas < t.minimo ? "var(--warning)" : "var(--primary)"}
                    />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </Grafico>
          <p className="border-t border-border px-5 py-3 text-xs text-muted-foreground">
            Em laranja, os tipos abaixo do estoque mínimo de segurança.
          </p>
        </Panel>

        <Panel title="Alertas" description="Calculados a partir do estoque e da fila">
          {alertas.length === 0 ? (
            <EstadoVazio titulo="Nenhum alerta no momento" />
          ) : (
            <ul className="divide-y divide-border">
              {alertas.slice(0, 4).map((a, i) => (
                <motion.li
                  key={a.titulo}
                  initial={{ opacity: 0, x: 12 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.06 * i, duration: 0.4 }}
                  className="flex gap-3 px-5 py-4"
                >
                  <span
                    aria-hidden="true"
                    className={cn(
                      "mt-0.5 grid size-8 shrink-0 place-items-center rounded-xl",
                      a.nivel === "alto"
                        ? "bg-primary-soft text-primary"
                        : "bg-warning-soft text-warning",
                    )}
                  >
                    <AlertTriangle className="size-4" />
                  </span>
                  <div>
                    <p className="text-sm font-semibold leading-tight">{a.titulo}</p>
                    <p className="mt-1 text-xs text-muted-foreground">{a.detalhe}</p>
                  </div>
                </motion.li>
              ))}
            </ul>
          )}
        </Panel>
      </div>

      <Panel
        actions={
          <Link
            to={aba === "solicitacoes" ? "/hemocentro/solicitacoes" : "/hemocentro/distribuicoes"}
            className="text-xs font-semibold text-primary hover:underline"
          >
            Ver tudo
          </Link>
        }
      >
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border px-5 py-4">
          <div role="tablist" aria-label="Resumo operacional" className="flex gap-2">
            <button
              type="button"
              role="tab"
              id="aba-solicitacoes"
              aria-selected={aba === "solicitacoes"}
              aria-controls="painel-solicitacoes"
              onClick={() => setAba("solicitacoes")}
              className={cn(
                "rounded-full border px-3.5 py-1.5 text-sm font-semibold transition-colors",
                aba === "solicitacoes"
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border text-muted-foreground hover:border-primary/40",
              )}
            >
              Solicitações pendentes ({pendentes.length})
            </button>
            <button
              type="button"
              role="tab"
              id="aba-transportes"
              aria-selected={aba === "transportes"}
              aria-controls="painel-transportes"
              onClick={() => setAba("transportes")}
              className={cn(
                "rounded-full border px-3.5 py-1.5 text-sm font-semibold transition-colors",
                aba === "transportes"
                  ? "border-primary bg-primary text-primary-foreground"
                  : "border-border text-muted-foreground hover:border-primary/40",
              )}
            >
              Transportes ativos ({emTransporte.length})
            </button>
          </div>
        </div>

        {aba === "solicitacoes" ? (
          <div role="tabpanel" id="painel-solicitacoes" aria-labelledby="aba-solicitacoes">
            <ul className="divide-y divide-border">
              {pendentes.slice(0, 4).map((s, i) => (
                <motion.li
                  key={s.id}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.05 * i, duration: 0.35 }}
                  className="flex flex-wrap items-center gap-3 px-5 py-4 transition-colors hover:bg-secondary/60"
                >
                  <span
                    className="grid size-9 place-items-center rounded-xl bg-primary-soft text-xs font-bold text-primary"
                    aria-hidden="true"
                  >
                    {s.tipoSanguineo}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-semibold">
                      {s.id} · {s.componente}
                    </p>
                    <p className="mt-0.5 text-xs text-muted-foreground">
                      {s.hospital} · {s.quantidade} bolsas · {formatarDataHora(s.criadaEm)}
                    </p>
                  </div>
                  <PrioridadeBadge prioridade={s.prioridade} />
                  <StatusBadge status={s.status} />
                  <Link
                    to="/hemocentro/solicitacoes/$id"
                    params={{ id: s.id }}
                    className="text-xs font-semibold text-primary hover:underline"
                  >
                    Analisar
                  </Link>
                </motion.li>
              ))}
            </ul>
            {pendentes.length === 0 ? (
              <EstadoVazio titulo="Nenhuma solicitação pendente" descricao="A fila está vazia." />
            ) : null}
          </div>
        ) : (
          <div
            role="tabpanel"
            id="painel-transportes"
            aria-labelledby="aba-transportes"
            className="grid gap-4 p-5 lg:grid-cols-2"
          >
            {transportes.slice(0, 4).map((t, i) => (
              <TransporteCard key={t.id} transporte={t} papel="hemocentro" atraso={i * 0.05} />
            ))}
            {transportes.length === 0 ? (
              <div className="lg:col-span-2">
                <EstadoVazio titulo="Nenhum transporte em andamento" />
              </div>
            ) : null}
          </div>
        )}
      </Panel>
    </AppShell>
  );
}
