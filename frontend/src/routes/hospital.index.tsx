import { createFileRoute, Link } from "@tanstack/react-router";
import { AnimatePresence, motion } from "motion/react";
import { useState } from "react";
import {
  Activity,
  CheckCircle2,
  ChevronDown,
  Clock,
  PackageSearch,
  PlusCircle,
  Truck,
} from "lucide-react";
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis } from "recharts";
import { AppShell } from "@/components/app-shell";
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
import { listarMinhasSolicitacoes } from "@/lib/api";
import { formatarDataHora, saidasSemana, type Solicitacao, type Status } from "@/lib/data";

export const Route = createFileRoute("/hospital/")({
  head: () => ({
    meta: [
      { title: "Painel do hospital — HemoTrack" },
      {
        name: "description",
        content:
          "Resumo das solicitações de hemocomponentes: pendentes, em andamento, em transporte e concluídas.",
      },
      { property: "og:title", content: "Painel do hospital — HemoTrack" },
      {
        property: "og:description",
        content: "Acompanhe pedidos, transportes e entregas do seu hospital.",
      },
    ],
  }),
  loader: async () => ({ solicitacoes: await listarMinhasSolicitacoes() }),
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
  component: HospitalDashboard,
});

type Filtro = "Todas" | "Pendente" | "Andamento" | "Em transporte" | "Entregue";

const emAndamentoStatus: Status[] = ["Aceita", "Em separação"];

const testes: Record<Exclude<Filtro, "Todas">, (s: Solicitacao) => boolean> = {
  Pendente: (s) => s.status === "Pendente",
  Andamento: (s) => emAndamentoStatus.includes(s.status),
  "Em transporte": (s) => s.status === "Em transporte",
  Entregue: (s) => s.status === "Entregue",
};

function HospitalDashboard() {
  const { solicitacoes: minhas } = Route.useLoaderData();
  const [filtro, setFiltro] = useState<Filtro>("Todas");
  const [atividadeAberta, setAtividadeAberta] = useState(false);

  const contar = (f: (s: Solicitacao) => boolean) => minhas.filter(f).length;
  const listaFiltrada = filtro === "Todas" ? minhas : minhas.filter(testes[filtro]);

  const cards = [
    {
      id: "Pendente" as const,
      label: "Pendentes",
      hint: "aguardando análise",
      tone: "warning" as const,
      icon: <Clock className="size-4" />,
      valor: contar(testes.Pendente),
    },
    {
      id: "Andamento" as const,
      label: "Em andamento",
      hint: "aceitas e em separação",
      tone: "teal" as const,
      icon: <PackageSearch className="size-4" />,
      valor: contar(testes.Andamento),
    },
    {
      id: "Em transporte" as const,
      label: "Em transporte",
      hint: "cadeia de frio monitorada",
      tone: "primary" as const,
      icon: <Truck className="size-4" />,
      valor: contar(testes["Em transporte"]),
    },
    {
      id: "Entregue" as const,
      label: "Concluídas",
      hint: "últimos 7 dias",
      tone: "success" as const,
      icon: <CheckCircle2 className="size-4" />,
      valor: contar(testes.Entregue),
    },
  ];

  return (
    <AppShell role="hospital">
      <PageHeader
        title="Painel do hospital"
        subtitle="Hospital Santa Clara · visão geral das solicitações de hemocomponentes"
        actions={
          <Link to="/hospital/nova-solicitacao">
            <motion.span
              whileHover={{ y: -2 }}
              whileTap={{ scale: 0.96 }}
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground shadow-card"
            >
              <PlusCircle className="size-4" aria-hidden="true" /> Nova solicitação
            </motion.span>
          </Link>
        }
      />

      <Stagger className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {cards.map((c) => {
          const ativo = filtro === c.id;
          return (
            <StaggerItem key={c.id} className="h-full">
              <button
                type="button"
                onClick={() => setFiltro(ativo ? "Todas" : c.id)}
                className="block h-full w-full text-left"
                aria-pressed={ativo}
              >
                <StatCard
                  label={c.label}
                  value={c.valor}
                  hint={c.hint}
                  tone={c.tone}
                  icon={c.icon}
                  highlight={ativo}
                />
              </button>
            </StaggerItem>
          );
        })}
      </Stagger>

      <p className="-mt-2 text-xs text-muted-foreground">
        Toque em um indicador para filtrar a lista abaixo por aquele status.
      </p>

      <div className="grid gap-6 lg:grid-cols-[1.4fr_1fr]">
        <Panel
          title="Solicitações"
          description={
            filtro === "Todas"
              ? "Todas as solicitações do hospital"
              : `Filtrando por: ${filtro === "Andamento" ? "em andamento" : filtro.toLowerCase()}`
          }
          actions={
            filtro !== "Todas" ? (
              <button
                type="button"
                onClick={() => setFiltro("Todas")}
                className="text-xs font-semibold text-primary hover:underline"
              >
                Limpar filtro
              </button>
            ) : undefined
          }
        >
          <div className="divide-y divide-border">
            {listaFiltrada.slice(0, 6).map((s) => (
              <motion.div
                key={s.id}
                whileHover={{ backgroundColor: "var(--secondary)" }}
                className="flex flex-wrap items-center gap-3 px-5 py-4"
              >
                <div className="min-w-40 flex-1">
                  <Link
                    to="/hospital/solicitacoes/$id"
                    params={{ id: s.id }}
                    className="text-sm font-semibold hover:text-primary"
                  >
                    {s.id}
                  </Link>
                  <p className="text-xs text-muted-foreground">
                    {s.componente} · {s.tipoSanguineo} · {s.quantidade} bolsas ·{" "}
                    {formatarDataHora(s.criadaEm)}
                  </p>
                </div>
                <PrioridadeBadge prioridade={s.prioridade} />
                <StatusBadge status={s.status} />
              </motion.div>
            ))}
            {listaFiltrada.length === 0 ? (
              <EstadoVazio
                titulo="Nenhuma solicitação com esse status"
                descricao="Ajuste o filtro acima ou registre uma nova solicitação."
                acao={
                  <Link
                    to="/hospital/nova-solicitacao"
                    className="mt-1 inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground"
                  >
                    <PlusCircle className="size-4" aria-hidden="true" /> Nova solicitação
                  </Link>
                }
              />
            ) : null}
          </div>
          <div className="border-t border-border px-5 py-3">
            <Link
              to="/hospital/solicitacoes"
              className="text-xs font-semibold text-primary hover:underline"
            >
              {listaFiltrada.length > 6
                ? `Ver todas as ${listaFiltrada.length} solicitações`
                : "Ver histórico completo"}
            </Link>
          </div>
        </Panel>

        <Panel title="Consumo na semana" description="Bolsas recebidas por dia">
          <Grafico
            titulo="Bolsas recebidas por dia da semana"
            resumo={saidasSemana.map((d) => `${d.dia}: ${d.saidas}`).join(", ")}
            altura="h-56"
          >
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={saidasSemana}>
                <CartesianGrid vertical={false} stroke="var(--border)" />
                <XAxis dataKey="dia" {...eixoTema} />
                <Tooltip {...tooltipTema} />
                <Bar dataKey="saidas" name="Bolsas" fill="var(--chart-1)" radius={[6, 6, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </Grafico>
        </Panel>
      </div>

      <Panel>
        <button
          type="button"
          onClick={() => setAtividadeAberta((v) => !v)}
          aria-expanded={atividadeAberta}
          className="flex w-full items-center justify-between gap-3 px-5 py-4 text-left"
        >
          <div>
            <h2 className="text-base font-semibold">Atividade recente</h2>
            <p className="mt-0.5 text-xs text-muted-foreground">Eventos das últimas horas</p>
          </div>
          <motion.span animate={{ rotate: atividadeAberta ? 180 : 0 }} aria-hidden="true">
            <ChevronDown className="size-4 text-muted-foreground" />
          </motion.span>
        </button>
        <AnimatePresence>
          {atividadeAberta ? (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: "auto", opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="overflow-hidden border-t border-border"
            >
              <ul className="divide-y divide-border">
                {[
                  "SOL-2041 saiu para transporte — ETA 12 min",
                  "SOL-2040 entrou em separação no Hemocentro Regional",
                  "SOL-2038 aceita pelo Hemocentro Boa Viagem",
                  "SOL-2030 entregue e conferida no banco de sangue",
                ].map((e) => (
                  <li key={e} className="flex items-center gap-3 px-5 py-3 text-sm">
                    <span
                      className="grid size-7 shrink-0 place-items-center rounded-lg bg-primary-soft text-primary"
                      aria-hidden="true"
                    >
                      <Activity className="size-3.5" />
                    </span>
                    {e}
                  </li>
                ))}
              </ul>
            </motion.div>
          ) : null}
        </AnimatePresence>
      </Panel>
    </AppShell>
  );
}
