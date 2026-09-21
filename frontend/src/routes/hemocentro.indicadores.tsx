import { createFileRoute } from "@tanstack/react-router";
import { motion } from "motion/react";
import { useState } from "react";
import { Activity, AlertTriangle, Boxes, Sigma, TrendingUp } from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { AppShell } from "@/components/app-shell";
import {
  Abas,
  EstadoErro,
  Grafico,
  PageHeader,
  PainelAba,
  Panel,
  SkeletonPagina,
  StatCard,
  Stagger,
  StaggerItem,
  eixoTema,
  tooltipTema,
} from "@/components/ui-kit";
import { listarSolicitacoes } from "@/lib/api";
import {
  bolsasVencendoEm,
  demandaPorComponente,
  desvioPadrao,
  estoquePorTipo,
  media,
  mediana,
  saidasSemana,
  temposAtendimentoMin,
  totalDisponivel,
} from "@/lib/data";

export const Route = createFileRoute("/hemocentro/indicadores")({
  head: () => ({
    meta: [
      { title: "Indicadores e estatística — HemoTrack" },
      {
        name: "description",
        content:
          "Estatísticas da operação: estoque por tipo sanguíneo, demanda por hemocomponente, tempo médio de atendimento e bolsas próximas do vencimento.",
      },
      { property: "og:title", content: "Indicadores e estatística — HemoTrack" },
      {
        property: "og:description",
        content: "Média, mediana, desvio padrão e taxa de atendimento das solicitações.",
      },
    ],
  }),
  loader: async () => ({ solicitacoes: await listarSolicitacoes() }),
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
  component: Indicadores,
});

type Aba = "estoque" | "demanda" | "atendimento";

const abas: { id: Aba; rotulo: string }[] = [
  { id: "estoque", rotulo: "Estoque" },
  { id: "demanda", rotulo: "Demanda" },
  { id: "atendimento", rotulo: "Atendimento" },
];

function Indicadores() {
  const { solicitacoes } = Route.useLoaderData();
  const [aba, setAba] = useState<Aba>("estoque");

  const vencendo = bolsasVencendoEm(7).length;
  const pedidos = demandaPorComponente.reduce((a, b) => a + b.pedidos, 0);
  const atendidos = demandaPorComponente.reduce((a, b) => a + b.atendidos, 0);
  const taxa = pedidos > 0 ? ((atendidos / pedidos) * 100).toFixed(1) : "0,0";
  const tempoMedio = media(temposAtendimentoMin);
  const desvio = desvioPadrao(temposAtendimentoMin);

  const estatisticas = [
    { label: "Média", valor: `${tempoMedio.toFixed(1)} min` },
    { label: "Mediana", valor: `${mediana(temposAtendimentoMin).toFixed(1)} min` },
    { label: "Desvio padrão", valor: `${desvio.toFixed(1)} min` },
    { label: "Mínimo", valor: `${Math.min(...temposAtendimentoMin)} min` },
    { label: "Máximo", valor: `${Math.max(...temposAtendimentoMin)} min` },
    {
      label: "Coef. de variação",
      valor: tempoMedio > 0 ? `${((desvio / tempoMedio) * 100).toFixed(1)} %` : "—",
    },
  ];

  const serieTempos = temposAtendimentoMin.map((valor, i) => ({
    pedido: `#${i + 1}`,
    minutos: valor,
  }));

  return (
    <AppShell role="hemocentro">
      <PageHeader
        title="Indicadores"
        subtitle="Estoque, demanda e desempenho do atendimento — últimos 30 dias"
      />

      <Stagger className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StaggerItem className="h-full">
          <StatCard
            label="Bolsas disponíveis"
            value={totalDisponivel}
            hint="Somando todos os tipos"
            tone="primary"
            icon={<Boxes className="size-4" />}
          />
        </StaggerItem>
        <StaggerItem className="h-full">
          <StatCard
            label="Solicitações registradas"
            value={solicitacoes.length}
            hint={`${pedidos} pedidos no período`}
            tone="teal"
            icon={<Activity className="size-4" />}
          />
        </StaggerItem>
        <StaggerItem className="h-full">
          <StatCard
            label="Taxa de atendimento"
            value={`${taxa}%`}
            hint={`${atendidos} de ${pedidos} pedidos atendidos`}
            tone="success"
            icon={<TrendingUp className="size-4" />}
            highlight
          />
        </StaggerItem>
        <StaggerItem className="h-full">
          <StatCard
            label="Vencem em 7 dias"
            value={vencendo}
            hint="Priorizar na próxima remessa"
            tone="warning"
            icon={<AlertTriangle className="size-4" />}
          />
        </StaggerItem>
      </Stagger>

      <Abas legenda="Grupos de indicadores" abas={abas} ativa={aba} aoMudar={setAba} />

      {aba === "estoque" ? (
        <PainelAba id="estoque">
          <Panel
            title="Estoque por tipo sanguíneo"
            description="Bolsas disponíveis · laranja indica abaixo do mínimo"
          >
            <Grafico
              titulo="Estoque por tipo sanguíneo"
              resumo={estoquePorTipo
                .map((t) => `${t.tipo}: ${t.bolsas} (mínimo ${t.minimo})`)
                .join(", ")}
              altura="h-72"
            >
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={estoquePorTipo}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border)" />
                  <XAxis dataKey="tipo" {...eixoTema} />
                  <YAxis {...eixoTema} width={34} />
                  <Tooltip {...tooltipTema} />
                  <Bar dataKey="bolsas" name="Disponíveis" radius={[8, 8, 0, 0]}>
                    {estoquePorTipo.map((t) => (
                      <Cell
                        key={t.tipo}
                        fill={t.bolsas < t.minimo ? "var(--warning)" : "var(--chart-1)"}
                      />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </Grafico>
          </Panel>
        </PainelAba>
      ) : null}

      {aba === "demanda" ? (
        <PainelAba id="demanda" className="space-y-6">
          <Panel title="Saídas por dia da semana" description="Bolsas distribuídas">
            <Grafico
              titulo="Saídas por dia da semana"
              resumo={saidasSemana.map((d) => `${d.dia}: ${d.saidas}`).join(", ")}
              altura="h-64"
            >
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={saidasSemana}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border)" />
                  <XAxis dataKey="dia" {...eixoTema} />
                  <YAxis {...eixoTema} width={34} />
                  <Tooltip {...tooltipTema} />
                  <Line
                    type="monotone"
                    dataKey="saidas"
                    name="Saídas"
                    stroke="var(--chart-2)"
                    strokeWidth={2.5}
                    dot={{ r: 3 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </Grafico>
          </Panel>

          <Panel
            title="Demanda por hemocomponente"
            description="Pedidos recebidos versus efetivamente atendidos"
          >
            <Grafico
              titulo="Demanda por hemocomponente"
              resumo={demandaPorComponente
                .map((d) => `${d.componente}: ${d.atendidos} de ${d.pedidos}`)
                .join(", ")}
              altura="h-72"
            >
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={demandaPorComponente}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border)" />
                  <XAxis dataKey="componente" {...eixoTema} />
                  <YAxis {...eixoTema} width={34} />
                  <Tooltip {...tooltipTema} />
                  <Legend wrapperStyle={{ fontSize: 12 }} />
                  <Bar
                    dataKey="pedidos"
                    name="Pedidos"
                    radius={[8, 8, 0, 0]}
                    fill="var(--chart-1)"
                  />
                  <Bar
                    dataKey="atendidos"
                    name="Atendidos"
                    radius={[8, 8, 0, 0]}
                    fill="var(--chart-2)"
                  />
                </BarChart>
              </ResponsiveContainer>
            </Grafico>
          </Panel>
        </PainelAba>
      ) : null}

      {aba === "atendimento" ? (
        <PainelAba id="atendimento" className="grid gap-6 lg:grid-cols-2">
          <Panel
            title="Tempo de atendimento"
            description="Da criação da solicitação até o despacho (min)"
          >
            <Grafico
              titulo="Tempo de atendimento por pedido"
              resumo={`${temposAtendimentoMin.length} atendimentos, média de ${tempoMedio.toFixed(1)} minutos`}
              altura="h-64"
            >
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={serieTempos}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--border)" />
                  <XAxis dataKey="pedido" {...eixoTema} />
                  <YAxis {...eixoTema} width={34} />
                  <Tooltip {...tooltipTema} />
                  <Line
                    type="monotone"
                    dataKey="minutos"
                    name="Minutos"
                    stroke="var(--chart-1)"
                    strokeWidth={2.5}
                    dot={{ r: 3 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </Grafico>
          </Panel>

          <Panel
            title="Resumo estatístico"
            description={`Amostra de ${temposAtendimentoMin.length} atendimentos`}
            actions={
              <span
                className="grid size-8 place-items-center rounded-lg bg-primary-soft text-primary"
                aria-hidden="true"
              >
                <Sigma className="size-4" />
              </span>
            }
          >
            <div className="grid gap-4 p-5 sm:grid-cols-2">
              {estatisticas.map((e, i) => (
                <motion.div
                  key={e.label}
                  initial={{ opacity: 0, y: 12 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: i * 0.05, duration: 0.4 }}
                  className="rounded-xl border border-border bg-background p-4"
                >
                  <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                    {e.label}
                  </p>
                  <p className="mt-1 font-display text-xl font-semibold tabular-nums">{e.valor}</p>
                </motion.div>
              ))}
            </div>
          </Panel>
        </PainelAba>
      ) : null}
    </AppShell>
  );
}
