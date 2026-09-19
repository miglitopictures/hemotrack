import { createFileRoute, Link, notFound, useNavigate } from "@tanstack/react-router";
import { AnimatePresence, motion } from "motion/react";
import { useState } from "react";
import {
  AlertTriangle,
  ArrowLeft,
  Building2,
  CheckCircle2,
  ClipboardCheck,
  Droplet,
  Loader2,
  MessageSquare,
  Thermometer,
  Truck,
  XCircle,
} from "lucide-react";
import { toast } from "sonner";
import { AppShell } from "@/components/app-shell";
import {
  EstadoErro,
  EstadoVazio,
  GrupoOpcoes,
  PageHeader,
  Panel,
  PrioridadeBadge,
  SkeletonPagina,
  StatusBadge,
  fadeUp,
} from "@/components/ui-kit";
import { aceitarSolicitacao, listarBolsasCompativeis, obterSolicitacao, recusarSolicitacao } from "@/lib/api";
import {
  diasParaVencer,
  ehTipoEscasso,
  formatarData,
  formatarDataHora,
  limitesTemperatura,
  motivosRecusa,
  tiposCompativeis,
} from "@/lib/data";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/hemocentro/solicitacoes/$id")({
  head: () => ({
    meta: [
      { title: "Análise da solicitação — HemoTrack" },
      {
        name: "description",
        content:
          "Analise a solicitação, selecione as bolsas compatíveis do estoque e aprove ou recuse o atendimento.",
      },
      { property: "og:title", content: "Análise da solicitação — HemoTrack" },
      {
        property: "og:description",
        content: "Compatibilidade, estoque disponível e decisão de atendimento.",
      },
    ],
  }),
  loader: async ({ params }) => {
    const solicitacao = await obterSolicitacao(params.id);
    if (!solicitacao) throw notFound();
    const compativeis = await listarBolsasCompativeis(
      solicitacao.componente,
      solicitacao.tipoSanguineo,
    );
    return { solicitacao, compativeis };
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
  component: AnaliseSolicitacao,
});

function AnaliseSolicitacao() {
  const { solicitacao: sol, compativeis } = Route.useLoaderData();
  const navigate = useNavigate();
  const aceitos = tiposCompativeis(sol.componente, sol.tipoSanguineo);
  const limite = limitesTemperatura[sol.componente];

  const [selecionadas, setSelecionadas] = useState<string[]>([]);
  const [decisao, setDecisao] = useState<"aceita" | "recusada" | null>(null);
  const [motivo, setMotivo] = useState(motivosRecusa[0] ?? "");
  const [processando, setProcessando] = useState(false);

  const jaDecidida = sol.status !== "Pendente";
  const completa = selecionadas.length === sol.quantidade;
  const faltam = sol.quantidade - selecionadas.length;

  const bolsasSelecionadas = compativeis.filter((b) => selecionadas.includes(b.codigo));
  const escassasUsadas = bolsasSelecionadas.filter((b) =>
    ehTipoEscasso(b.tipoSanguineo, sol.tipoSanguineo),
  );
  const resumoTipos = Object.entries(
    bolsasSelecionadas.reduce<Record<string, number>>((acc, b) => {
      acc[b.tipoSanguineo] = (acc[b.tipoSanguineo] ?? 0) + 1;
      return acc;
    }, {}),
  );

  function alternar(codigo: string) {
    setSelecionadas((prev) => {
      if (prev.includes(codigo)) return prev.filter((c) => c !== codigo);
      // Nunca deixa separar mais bolsas do que o pedido.
      if (prev.length >= sol.quantidade) {
        toast.warning(`A solicitação pede ${sol.quantidade} bolsa(s)`, {
          description: "Desmarque uma bolsa antes de escolher outra.",
        });
        return prev;
      }
      return [...prev, codigo];
    });
  }

  async function aceitar() {
    setProcessando(true);
    try {
      await aceitarSolicitacao(sol.id, selecionadas);
      setDecisao("aceita");
      toast.success(`${sol.id} aceita`, {
        description: `${selecionadas.length} bolsa(s) reservadas para separação.`,
      });
    } catch (e) {
      toast.error("Não foi possível aceitar", {
        description: e instanceof Error ? e.message : "Erro inesperado.",
      });
    } finally {
      setProcessando(false);
    }
  }

  async function recusar() {
    setProcessando(true);
    try {
      await recusarSolicitacao(sol.id, motivo);
      setDecisao("recusada");
      toast.success(`${sol.id} recusada`, { description: `Motivo informado: ${motivo}` });
    } catch (e) {
      toast.error("Não foi possível recusar", {
        description: e instanceof Error ? e.message : "Erro inesperado.",
      });
    } finally {
      setProcessando(false);
    }
  }

  return (
    <AppShell role="hemocentro">
      <Link
        to="/hemocentro/solicitacoes"
        className="inline-flex items-center gap-2 text-sm font-medium text-muted-foreground transition-colors hover:text-primary"
      >
        <ArrowLeft className="size-4" aria-hidden="true" /> Solicitações recebidas
      </Link>

      <PageHeader
        title={`Análise · ${sol.id}`}
        subtitle={`${sol.hospital} · recebida em ${formatarDataHora(sol.criadaEm)}`}
        actions={
          <div className="flex items-center gap-2">
            <PrioridadeBadge prioridade={sol.prioridade} />
            <StatusBadge status={sol.status} />
          </div>
        }
      />

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <Panel title="Dados do pedido" description="Informações enviadas pelo hospital">
            <dl className="grid gap-4 p-5 sm:grid-cols-2">
              {[
                { label: "Hemocomponente", value: sol.componente },
                { label: "Tipo sanguíneo", value: sol.tipoSanguineo },
                { label: "Quantidade", value: `${sol.quantidade} bolsas` },
                { label: "Prioridade", value: sol.prioridade },
                { label: "Hospital solicitante", value: sol.hospital },
                { label: "Hemocentro responsável", value: sol.hemocentro },
              ].map((item) => (
                <div key={item.label} className="rounded-xl border border-border bg-background p-4">
                  <dt className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                    {item.label}
                  </dt>
                  <dd className="mt-1 text-sm font-semibold">{item.value}</dd>
                </div>
              ))}
            </dl>
            <div className="flex gap-3 border-t border-border px-5 py-4">
              <MessageSquare className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden="true" />
              <p className="text-sm text-muted-foreground">{sol.observacoes || "Sem observações."}</p>
            </div>
          </Panel>

          <Panel
            title="Bolsas compatíveis em estoque"
            description={`${compativeis.length} bolsa(s) livres de ${sol.componente.toLowerCase()} · tipos aceitos para ${sol.tipoSanguineo}: ${aceitos.join(", ")}`}
            actions={
              <span className="rounded-full border border-warning/25 bg-warning-soft px-2.5 py-1 text-xs font-semibold text-warning">
                Ordenado por validade mais próxima
              </span>
            }
          >
            {compativeis.length === 0 ? (
              <EstadoVazio
                titulo="Nenhuma bolsa compatível disponível"
                descricao="Recuse a solicitação informando o motivo, para o hospital procurar outro hemocentro."
              />
            ) : (
              <ul className="divide-y divide-border">
                {compativeis.map((b) => {
                  const ativa = selecionadas.includes(b.codigo);
                  const dias = diasParaVencer(b.validade);
                  const escassa = ehTipoEscasso(b.tipoSanguineo, sol.tipoSanguineo);
                  return (
                    <li key={b.codigo}>
                      <button
                        type="button"
                        role="checkbox"
                        aria-checked={ativa}
                        disabled={jaDecidida || decisao !== null}
                        onClick={() => alternar(b.codigo)}
                        className={cn(
                          "flex w-full flex-wrap items-center gap-3 px-5 py-4 text-left transition-colors disabled:cursor-not-allowed disabled:opacity-60",
                          ativa ? "bg-primary-soft/60" : "hover:bg-secondary/60",
                        )}
                      >
                        <span
                          aria-hidden="true"
                          className={cn(
                            "grid size-8 place-items-center rounded-lg border text-xs font-bold transition-colors",
                            ativa
                              ? "border-primary bg-primary text-primary-foreground"
                              : "border-border text-muted-foreground",
                          )}
                        >
                          {ativa ? <CheckCircle2 className="size-4" /> : b.tipoSanguineo}
                        </span>
                        <div className="min-w-0 flex-1">
                          <p className="text-sm font-semibold">
                            {b.codigo} · {b.tipoSanguineo}
                          </p>
                          <p className="mt-0.5 text-xs text-muted-foreground">
                            Validade {formatarData(b.validade)} · {b.armazenamento}
                          </p>
                        </div>
                        {escassa ? (
                          <span className="inline-flex items-center gap-1 rounded-full border border-warning/25 bg-warning-soft px-2.5 py-1 text-xs font-semibold text-warning">
                            <AlertTriangle className="size-3" aria-hidden="true" /> O- universal
                          </span>
                        ) : null}
                        <span
                          className={cn(
                            "rounded-full border px-2.5 py-1 text-xs font-semibold",
                            dias <= 5
                              ? "border-warning/25 bg-warning-soft text-warning"
                              : "border-border text-muted-foreground",
                          )}
                        >
                          {dias} dias p/ vencer
                        </span>
                        <span className="rounded-full border border-border px-2.5 py-1 text-xs font-semibold text-muted-foreground">
                          {b.status}
                        </span>
                      </button>
                    </li>
                  );
                })}
              </ul>
            )}
          </Panel>
        </div>

        <div className="space-y-6">
          <motion.div
            {...fadeUp}
            className="rounded-2xl border border-border bg-card p-5 shadow-card lg:sticky lg:top-24"
          >
            <h2 className="text-base font-semibold">Decisão</h2>
            <p className="mt-1 text-xs text-muted-foreground">
              {selecionadas.length} de {sol.quantidade} bolsas selecionadas
            </p>
            <div
              className="mt-3 h-1.5 overflow-hidden rounded-full bg-secondary"
              role="progressbar"
              aria-valuenow={selecionadas.length}
              aria-valuemin={0}
              aria-valuemax={sol.quantidade}
              aria-label="Bolsas selecionadas"
            >
              <motion.div
                animate={{
                  width: `${Math.min(100, (selecionadas.length / sol.quantidade) * 100)}%`,
                }}
                transition={{ type: "spring", stiffness: 220, damping: 26 }}
                className={cn("h-full rounded-full", completa ? "bg-success" : "bg-primary")}
              />
            </div>

            {resumoTipos.length > 0 ? (
              <div className="mt-3 flex flex-wrap gap-1.5">
                {resumoTipos.map(([tipo, qtd]) => (
                  <span
                    key={tipo}
                    className="rounded-full border border-border px-2 py-0.5 text-xs font-semibold"
                  >
                    {qtd}× {tipo}
                  </span>
                ))}
              </div>
            ) : null}

            {escassasUsadas.length > 0 ? (
              <p className="mt-3 flex gap-2 rounded-xl border border-warning/25 bg-warning-soft px-3 py-2 text-xs font-medium text-warning">
                <AlertTriangle className="mt-0.5 size-3.5 shrink-0" aria-hidden="true" />
                {escassasUsadas.length} bolsa(s) O- para receptor {sol.tipoSanguineo}. O- é o estoque
                mais escasso — use só se não houver alternativa.
              </p>
            ) : null}

            {jaDecidida ? (
              <p className="mt-4 rounded-xl border border-border bg-secondary px-3 py-2.5 text-xs font-medium text-muted-foreground">
                Esta solicitação já está com status “{sol.status}”. Não há decisão pendente.
              </p>
            ) : (
              <div className="mt-5 flex flex-col gap-2">
                <motion.button
                  type="button"
                  whileHover={completa ? { y: -2 } : undefined}
                  whileTap={completa ? { scale: 0.96 } : undefined}
                  onClick={aceitar}
                  disabled={!completa || processando || decisao !== null}
                  className="inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground shadow-card disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {processando ? (
                    <Loader2 className="size-4 animate-spin" aria-hidden="true" />
                  ) : (
                    <ClipboardCheck className="size-4" aria-hidden="true" />
                  )}
                  Aceitar e separar
                </motion.button>
                {!completa ? (
                  <p className="text-center text-xs text-muted-foreground">
                    {faltam > 0
                      ? `Selecione mais ${faltam} bolsa(s) compatível(is) para aceitar.`
                      : "Ajuste a seleção para a quantidade pedida."}
                  </p>
                ) : null}
                <motion.button
                  type="button"
                  whileTap={{ scale: 0.96 }}
                  onClick={() => setDecisao(decisao === "recusada" ? null : "recusada")}
                  disabled={processando || decisao === "aceita"}
                  className="inline-flex items-center justify-center gap-2 rounded-xl border border-border px-4 py-2.5 text-sm font-semibold text-muted-foreground transition-colors hover:border-primary/40 hover:text-primary disabled:cursor-not-allowed disabled:opacity-50"
                >
                  <XCircle className="size-4" aria-hidden="true" /> Recusar solicitação
                </motion.button>
              </div>
            )}

            <AnimatePresence>
              {decisao === "recusada" ? (
                <motion.div
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: "auto" }}
                  exit={{ opacity: 0, height: 0 }}
                  className="mt-4 overflow-hidden"
                >
                  <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                    Motivo da recusa
                  </p>
                  <GrupoOpcoes
                    legenda="Motivo da recusa"
                    variante="cartao"
                    className="mt-2 flex flex-col"
                    itemClassName="text-xs"
                    opcoes={motivosRecusa.map((m) => ({ valor: m, rotulo: m }))}
                    valor={motivo}
                    aoMudar={setMotivo}
                  />
                  <button
                    type="button"
                    onClick={recusar}
                    disabled={processando || !motivo}
                    className="mt-3 w-full rounded-xl border border-destructive/30 bg-destructive/10 px-4 py-2 text-xs font-semibold text-destructive disabled:opacity-50"
                  >
                    Confirmar recusa
                  </button>
                </motion.div>
              ) : null}
            </AnimatePresence>

            <AnimatePresence>
              {decisao === "aceita" ? (
                <motion.div
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0 }}
                  className="mt-4 space-y-2"
                >
                  <p className="rounded-xl border border-success/20 bg-success-soft px-3 py-2.5 text-xs font-medium text-success">
                    Solicitação aceita (protótipo). Bolsas reservadas para separação.
                  </p>
                  <button
                    type="button"
                    onClick={() =>
                      void navigate({
                        to: "/hemocentro/distribuicoes/planejar",
                        search: { sol: sol.id },
                      })
                    }
                    className="flex w-full items-center justify-center gap-2 rounded-xl border border-primary/30 bg-primary-soft px-4 py-2.5 text-sm font-semibold text-primary"
                  >
                    <Truck className="size-4" aria-hidden="true" /> Planejar distribuição
                  </button>
                </motion.div>
              ) : null}
            </AnimatePresence>
          </motion.div>

          <Panel title="Condições de armazenamento">
            <ul className="space-y-3 p-5 text-sm">
              <li className="flex items-center gap-2 text-muted-foreground">
                <Thermometer className="size-4 text-primary" aria-hidden="true" />
                {limite
                  ? `Faixa em transporte: ${limite.min} °C a ${limite.max} °C`
                  : "Faixa de transporte não definida"}
              </li>
              <li className="flex items-center gap-2 text-muted-foreground">
                <Droplet className="size-4 text-primary" aria-hidden="true" /> Testes sorológicos
                concluídos
              </li>
              <li className="flex items-center gap-2 text-muted-foreground">
                <Building2 className="size-4 text-primary" aria-hidden="true" /> Destino:{" "}
                {sol.hospital}
              </li>
            </ul>
          </Panel>
        </div>
      </div>
    </AppShell>
  );
}
