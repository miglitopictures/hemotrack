import { createFileRoute, Link, notFound } from "@tanstack/react-router";
import { motion } from "motion/react";
import { ArrowLeft, Building2, Check, MapPin, Thermometer, Truck, XCircle } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import {
  EstadoErro,
  PageHeader,
  Panel,
  PrioridadeBadge,
  SkeletonPagina,
  StatusBadge,
  fadeUp,
} from "@/components/ui-kit";
import { obterSolicitacao, obterTransporteDaSolicitacao } from "@/lib/api";
import {
  componenteDoTransporte,
  formatarDataHora,
  formatarTemperatura,
  temperaturaAtual,
  temperaturaFora,
  timelineEtapas,
} from "@/lib/data";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/hospital/solicitacoes/$id")({
  head: () => ({
    meta: [
      { title: "Detalhes da solicitação — HemoTrack" },
      {
        name: "description",
        content:
          "Informações da solicitação, hemocentro responsável, status e linha do tempo até a entrega.",
      },
      { property: "og:title", content: "Detalhes da solicitação — HemoTrack" },
      {
        property: "og:description",
        content: "Linha do tempo: pendente, aceita, separação, transporte, entregue.",
      },
    ],
  }),
  loader: async ({ params }) => {
    const solicitacao = await obterSolicitacao(params.id);
    // Antes o app caía silenciosamente na primeira solicitação da lista e
    // mostrava dados de outro pedido. Agora é 404 de verdade.
    if (!solicitacao) throw notFound();
    const transporte = await obterTransporteDaSolicitacao(solicitacao.id);
    return { solicitacao, transporte: transporte ?? null };
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
  component: DetalhesSolicitacao,
});

function DetalhesSolicitacao() {
  const { solicitacao: s, transporte } = Route.useLoaderData();
  const recusada = s.status === "Recusada";
  const etapaAtual = Math.max(timelineEtapas.indexOf(s.status), 0);
  const temp = transporte ? temperaturaAtual(transporte) : null;
  const tempFora =
    transporte && temp !== null ? temperaturaFora(componenteDoTransporte(transporte), temp) : false;

  return (
    <AppShell role="hospital">
      <Link
        to="/hospital/solicitacoes"
        className="inline-flex items-center gap-1.5 text-sm font-medium text-muted-foreground hover:text-primary"
      >
        <ArrowLeft className="size-4" aria-hidden="true" /> Minhas solicitações
      </Link>

      <PageHeader
        title={`Solicitação ${s.id}`}
        subtitle={`${s.componente} · ${s.tipoSanguineo} · ${s.quantidade} bolsas`}
        actions={
          <div className="flex items-center gap-2">
            <PrioridadeBadge prioridade={s.prioridade} />
            <StatusBadge status={s.status} />
          </div>
        }
      />

      <div className="grid gap-6 lg:grid-cols-[1.4fr_1fr]">
        <div className="space-y-6">
          <Panel title="Linha do tempo">
            {recusada ? (
              <div className="flex items-start gap-3 p-5">
                <XCircle className="mt-0.5 size-5 shrink-0 text-destructive" aria-hidden="true" />
                <div>
                  <p className="text-sm font-semibold">Solicitação recusada pelo hemocentro</p>
                  <p className="mt-1 text-sm text-muted-foreground">{s.observacoes}</p>
                </div>
              </div>
            ) : (
              <ol className="space-y-0 p-5">
                {timelineEtapas.map((etapa, i) => {
                  const concluida = i <= etapaAtual;
                  const atual = i === etapaAtual;
                  return (
                    <motion.li
                      key={etapa}
                      initial={{ opacity: 0, x: -12 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: i * 0.1, duration: 0.4 }}
                      className="relative flex gap-4 pb-6 last:pb-0"
                    >
                      {i < timelineEtapas.length - 1 ? (
                        <span
                          aria-hidden="true"
                          className={cn(
                            "absolute left-[15px] top-8 h-full w-0.5",
                            i < etapaAtual ? "bg-primary" : "bg-border",
                          )}
                        />
                      ) : null}
                      <span
                        aria-hidden="true"
                        className={cn(
                          "relative z-10 grid size-8 shrink-0 place-items-center rounded-full border-2 text-xs font-bold",
                          concluida
                            ? "border-primary bg-primary text-primary-foreground"
                            : "border-border bg-card text-muted-foreground",
                        )}
                      >
                        {concluida ? <Check className="size-4" /> : i + 1}
                      </span>
                      <div className="pt-1">
                        <p
                          className={cn(
                            "text-sm font-semibold",
                            atual ? "text-primary" : concluida ? "" : "text-muted-foreground",
                          )}
                        >
                          {etapa}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {concluida ? (atual ? "Etapa atual" : "Concluída") : "Aguardando"}
                        </p>
                      </div>
                    </motion.li>
                  );
                })}
              </ol>
            )}
          </Panel>

          <Panel title="Informações da solicitação">
            <dl className="grid gap-4 p-5 sm:grid-cols-2">
              {[
                ["Hospital solicitante", s.hospital],
                ["Data do pedido", formatarDataHora(s.criadaEm)],
                ["Componente", s.componente],
                ["Tipo sanguíneo", s.tipoSanguineo],
                ["Quantidade", `${s.quantidade} bolsas`],
                ["Prioridade", s.prioridade],
              ].map(([k, v]) => (
                <div key={k} className="rounded-xl border border-border p-3">
                  <dt className="text-xs text-muted-foreground">{k}</dt>
                  <dd className="mt-0.5 text-sm font-medium">{v}</dd>
                </div>
              ))}
              <div className="rounded-xl border border-border p-3 sm:col-span-2">
                <dt className="text-xs text-muted-foreground">Observações</dt>
                <dd className="mt-0.5 text-sm">{s.observacoes || "—"}</dd>
              </div>
            </dl>
          </Panel>
        </div>

        <motion.div {...fadeUp} className="space-y-4">
          <Panel title="Hemocentro responsável">
            <div className="flex items-start gap-3 p-5">
              <span
                className="grid size-10 place-items-center rounded-xl bg-primary-soft text-primary"
                aria-hidden="true"
              >
                <Building2 className="size-5" />
              </span>
              <div>
                <p className="text-sm font-semibold">{s.hemocentro}</p>
                <p className="mt-0.5 text-xs text-muted-foreground">
                  Av. Rui Barbosa, 320 — Recife/PE · (81) 3182-4600
                </p>
              </div>
            </div>
          </Panel>

          {transporte ? (
            <Panel title="Transporte">
              <div className="space-y-4 p-5 text-sm">
                <div className="flex items-center justify-between">
                  <span className="flex items-center gap-2 font-medium">
                    <Truck className="size-4 text-primary" aria-hidden="true" /> {transporte.id}
                  </span>
                  <StatusBadge status={transporte.status} />
                </div>
                <div
                  className="h-2 overflow-hidden rounded-full bg-secondary"
                  role="progressbar"
                  aria-valuenow={transporte.progresso}
                  aria-valuemin={0}
                  aria-valuemax={100}
                  aria-label="Progresso do transporte"
                >
                  <motion.div
                    initial={{ width: 0 }}
                    animate={{ width: `${transporte.progresso}%` }}
                    transition={{ duration: 1, ease: "easeOut" }}
                    className="h-full rounded-full bg-primary"
                  />
                </div>
                <p className="flex items-center gap-2 text-xs text-muted-foreground">
                  <MapPin className="size-3.5" aria-hidden="true" /> {transporte.localizacao}
                </p>
                <p
                  className={cn(
                    "flex items-center gap-2 text-xs",
                    tempFora ? "font-semibold text-warning" : "text-muted-foreground",
                  )}
                >
                  <Thermometer className="size-3.5" aria-hidden="true" />
                  {formatarTemperatura(temp)} · ETA {transporte.eta}
                </p>
                <Link
                  to="/hospital/transportes/$id"
                  params={{ id: transporte.id }}
                  className="block rounded-xl border border-border py-2 text-center text-xs font-semibold text-primary transition-colors hover:border-primary/40"
                >
                  Acompanhar transporte
                </Link>
              </div>
            </Panel>
          ) : null}

          <Panel title="Bolsas vinculadas">
            {transporte && transporte.bolsas.length > 0 ? (
              <ul className="divide-y divide-border text-sm">
                {transporte.bolsas.map((b) => (
                  <li key={b} className="px-5 py-3 font-medium">
                    {b}
                  </li>
                ))}
              </ul>
            ) : (
              <p className="px-5 py-6 text-sm text-muted-foreground">
                Aguardando a seleção das bolsas pelo hemocentro.
              </p>
            )}
          </Panel>
        </motion.div>
      </div>
    </AppShell>
  );
}
