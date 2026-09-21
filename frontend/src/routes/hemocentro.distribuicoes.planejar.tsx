import { createFileRoute, Link, notFound, useNavigate } from "@tanstack/react-router";
import { motion } from "motion/react";
import { useState } from "react";
import {
  ArrowLeft,
  CheckCircle2,
  Clock,
  Loader2,
  MapPin,
  PackageCheck,
  Route as RouteIcon,
  Thermometer,
  Truck,
} from "lucide-react";
import { toast } from "sonner";
import { AppShell } from "@/components/app-shell";
import {
  EstadoErro,
  GrupoOpcoes,
  PageHeader,
  Panel,
  SkeletonPagina,
  StatCard,
  Stagger,
  StaggerItem,
  fadeUp,
} from "@/components/ui-kit";
import { despacharRemessa, listarSolicitacoes, obterSolicitacao, obterTransporteDaSolicitacao } from "@/lib/api";
import {
  etapasDistribuicao,
  limitesTemperatura,
  rotaSimplificada,
  veiculos,
} from "@/lib/data";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/hemocentro/distribuicoes/planejar")({
  head: () => ({
    meta: [
      { title: "Planejar distribuição — HemoTrack" },
      {
        name: "description",
        content:
          "Monte a remessa: conferência das bolsas separadas, embalagem térmica, rota simplificada até o hospital e despacho do veículo.",
      },
      { property: "og:title", content: "Planejar distribuição — HemoTrack" },
      {
        property: "og:description",
        content: "Etapas de separação, rota simplificada e despacho da remessa.",
      },
    ],
  }),
  // A solicitação vem pela URL (?sol=SOL-2040) — antes a tela adivinhava
  // qual pedido estava sendo preparado.
  validateSearch: (search: Record<string, unknown>) => ({
    sol: typeof search["sol"] === "string" ? search["sol"] : "",
  }),
  loaderDeps: ({ search }) => ({ sol: search.sol }),
  loader: async ({ deps }) => {
    if (deps.sol) {
      const solicitacao = await obterSolicitacao(deps.sol);
      if (!solicitacao) throw notFound();
      const transporte = await obterTransporteDaSolicitacao(solicitacao.id);
      return { solicitacao, transporte: transporte ?? null };
    }
    // Sem parâmetro: cai na primeira remessa em separação, mas deixando claro.
    const lista = await listarSolicitacoes();
    const solicitacao =
      lista.find((s) => s.status === "Em separação") ?? lista.find((s) => s.status === "Aceita");
    if (!solicitacao) throw notFound();
    const transporte = await obterTransporteDaSolicitacao(solicitacao.id);
    return { solicitacao, transporte: transporte ?? null };
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
  component: PlanejarDistribuicao,
});

function PlanejarDistribuicao() {
  const { solicitacao: sol, transporte } = Route.useLoaderData();
  const navigate = useNavigate();

  const [concluidas, setConcluidas] = useState<string[]>([]);
  const [veiculo, setVeiculo] = useState(veiculos[0]?.nome ?? "");
  const [despachando, setDespachando] = useState(false);
  const [despachado, setDespachado] = useState(false);

  const distancia = rotaSimplificada[rotaSimplificada.length - 1]?.km ?? 0;
  const limite = limitesTemperatura[sol.componente];
  const tudoPronto = concluidas.length === etapasDistribuicao.length;

  // Agora cada etapa é um checkbox independente — antes clicar na etapa 3
  // marcava as anteriores e não dava para desfazer.
  function alternarEtapa(id: string) {
    setConcluidas((prev) => (prev.includes(id) ? prev.filter((e) => e !== id) : [...prev, id]));
  }

  async function despachar() {
    setDespachando(true);
    try {
      const remessa = await despacharRemessa({ solicitacao: sol.id, veiculo });
      setDespachado(true);
      toast.success(`Remessa ${remessa.id} despachada`, { description: veiculo });
    } catch (e) {
      toast.error("Não foi possível despachar", {
        description: e instanceof Error ? e.message : "Erro inesperado.",
      });
    } finally {
      setDespachando(false);
    }
  }

  return (
    <AppShell role="hemocentro">
      <Link
        to="/hemocentro/distribuicoes"
        className="inline-flex items-center gap-2 text-sm font-medium text-muted-foreground transition-colors hover:text-primary"
      >
        <ArrowLeft className="size-4" aria-hidden="true" /> Distribuição
      </Link>

      <PageHeader
        title="Planejar distribuição"
        subtitle={`Remessa da solicitação ${sol.id} · ${sol.hospital} · ${sol.componente}`}
        actions={
          <Link
            to="/hemocentro/solicitacoes/$id"
            params={{ id: sol.id }}
            className="inline-flex items-center gap-2 rounded-xl border border-border px-4 py-2.5 text-sm font-semibold transition-colors hover:border-primary/40 hover:text-primary"
          >
            Ver solicitação
          </Link>
        }
      />

      <Stagger className="grid gap-4 sm:grid-cols-3">
        <StaggerItem className="h-full">
          <StatCard
            label="Bolsas separadas"
            value={transporte ? transporte.bolsas.length : sol.quantidade}
            hint={sol.componente}
            tone="primary"
            icon={<PackageCheck className="size-4" />}
          />
        </StaggerItem>
        <StaggerItem className="h-full">
          <StatCard
            label="Distância da rota"
            value={`${distancia} km`}
            hint={`${rotaSimplificada.length} pontos de passagem`}
            tone="teal"
            icon={<RouteIcon className="size-4" />}
          />
        </StaggerItem>
        <StaggerItem className="h-full">
          <StatCard
            label="Tempo estimado"
            value="34 min"
            hint="Preparo + trajeto"
            tone="warning"
            icon={<Clock className="size-4" />}
          />
        </StaggerItem>
      </Stagger>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <Panel
            title="Preparo da remessa"
            description={`${concluidas.length} de ${etapasDistribuicao.length} etapas concluídas`}
          >
            <ul className="divide-y divide-border">
              {etapasDistribuicao.map((e, i) => {
                const feito = concluidas.includes(e.id);
                return (
                  <li key={e.id}>
                    <button
                      type="button"
                      role="checkbox"
                      aria-checked={feito}
                      onClick={() => alternarEtapa(e.id)}
                      className={cn(
                        "flex w-full items-center gap-4 px-5 py-4 text-left transition-colors",
                        feito ? "bg-primary-soft/40" : "hover:bg-secondary/60",
                      )}
                    >
                      <motion.span
                        animate={{ scale: feito ? 1 : 0.94 }}
                        aria-hidden="true"
                        className={cn(
                          "grid size-8 shrink-0 place-items-center rounded-lg border-2",
                          feito
                            ? "border-primary bg-primary text-primary-foreground"
                            : "border-border text-muted-foreground",
                        )}
                      >
                        {feito ? <CheckCircle2 className="size-4" /> : i + 1}
                      </motion.span>
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-semibold">{e.titulo}</p>
                        <p className="mt-0.5 text-xs text-muted-foreground">{e.detalhe}</p>
                      </div>
                      <span className="text-xs font-semibold text-muted-foreground">
                        {e.duracao}
                      </span>
                    </button>
                  </li>
                );
              })}
            </ul>
          </Panel>

          <Panel title="Rota simplificada" description="Hemocentro → hospital solicitante">
            <ol className="relative space-y-6 p-5 pl-8">
              <span
                className="absolute bottom-7 left-[1.6rem] top-7 w-0.5 bg-border"
                aria-hidden="true"
              />
              {rotaSimplificada.map((p, i) => (
                <motion.li
                  key={p.ponto}
                  initial={{ opacity: 0, x: 12 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: i * 0.08, duration: 0.4 }}
                  className="relative flex items-start gap-4"
                >
                  <span
                    aria-hidden="true"
                    className={cn(
                      "relative z-10 grid size-7 shrink-0 place-items-center rounded-full border-2 bg-card",
                      i === 0
                        ? "border-teal text-teal"
                        : i === rotaSimplificada.length - 1
                          ? "border-primary bg-primary text-primary-foreground"
                          : "border-border text-muted-foreground",
                    )}
                  >
                    <MapPin className="size-3.5" />
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold">{p.ponto}</p>
                    <p className="mt-0.5 text-xs text-muted-foreground">{p.detalhe}</p>
                  </div>
                  <span className="text-xs font-semibold tabular-nums text-muted-foreground">
                    {p.km} km
                  </span>
                </motion.li>
              ))}
            </ol>
          </Panel>
        </div>

        <div className="space-y-6">
          {transporte ? (
            <Panel title="Bolsas conferidas">
              <ul className="divide-y divide-border text-sm">
                {transporte.bolsas.map((b) => (
                  <li key={b} className="px-5 py-3 font-medium">
                    {b}
                  </li>
                ))}
              </ul>
            </Panel>
          ) : null}

          <Panel title="Veículo da remessa">
            <div className="p-5">
              <GrupoOpcoes
                legenda="Veículo da remessa"
                variante="cartao"
                className="flex-col"
                opcoes={veiculos.map((v) => ({
                  valor: v.nome,
                  rotulo: v.nome,
                  descricao: v.detalhe,
                }))}
                valor={veiculo}
                aoMudar={setVeiculo}
              />
            </div>
          </Panel>

          <motion.div
            {...fadeUp}
            className="rounded-2xl border border-border bg-card p-5 shadow-card"
          >
            <h2 className="text-base font-semibold">Despacho</h2>
            <p className="mt-1 flex items-center gap-1.5 text-xs text-muted-foreground">
              <Thermometer className="size-3.5 text-primary" aria-hidden="true" />
              {limite
                ? `Faixa monitorada: ${limite.min} °C a ${limite.max} °C`
                : "Faixa de temperatura não definida"}
            </p>
            <motion.button
              type="button"
              whileHover={tudoPronto ? { y: -2 } : undefined}
              whileTap={tudoPronto ? { scale: 0.96 } : undefined}
              onClick={despachar}
              disabled={!tudoPronto || despachando || despachado}
              className="mt-4 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground shadow-card disabled:cursor-not-allowed disabled:opacity-50"
            >
              {despachando ? (
                <Loader2 className="size-4 animate-spin" aria-hidden="true" />
              ) : (
                <Truck className="size-4" aria-hidden="true" />
              )}
              {despachado ? "Remessa despachada" : "Despachar remessa"}
            </motion.button>
            {!tudoPronto ? (
              <p className="mt-2 text-center text-xs text-muted-foreground">
                Conclua as {etapasDistribuicao.length} etapas de preparo para liberar o despacho.
              </p>
            ) : null}
            {despachado && transporte ? (
              <motion.div
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                className="mt-4 rounded-xl border border-success/20 bg-success-soft px-3 py-2.5 text-xs font-medium text-success"
              >
                Remessa despachada (protótipo) com {veiculo}.{" "}
                <button
                  type="button"
                  onClick={() =>
                    void navigate({
                      to: "/hemocentro/distribuicoes/$id",
                      params: { id: transporte.id },
                    })
                  }
                  className="font-semibold underline"
                >
                  Monitorar transporte
                </button>
              </motion.div>
            ) : null}
          </motion.div>
        </div>
      </div>
    </AppShell>
  );
}
