import { createFileRoute, Link } from "@tanstack/react-router";
import { motion } from "motion/react";
import { useState } from "react";
import { ChevronRight, Filter, PlusCircle } from "lucide-react";
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
} from "@/components/ui-kit";
import { listarMinhasSolicitacoes } from "@/lib/api";
import { formatarDataHora, type Status } from "@/lib/data";

export const Route = createFileRoute("/hospital/solicitacoes/")({
  head: () => ({
    meta: [
      { title: "Minhas solicitações — HemoTrack" },
      {
        name: "description",
        content:
          "Lista completa das solicitações de hemocomponentes com componente, prioridade, status e data.",
      },
      { property: "og:title", content: "Minhas solicitações — HemoTrack" },
      { property: "og:description", content: "Histórico e status das solicitações do hospital." },
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
  component: MinhasSolicitacoes,
});

type Filtro = Status | "Todas";

const filtros: Filtro[] = [
  "Todas",
  "Pendente",
  "Aceita",
  "Em separação",
  "Em transporte",
  "Entregue",
  "Recusada",
];

function MinhasSolicitacoes() {
  const { solicitacoes: minhas } = Route.useLoaderData();
  const [filtro, setFiltro] = useState<Filtro>("Todas");
  const lista = filtro === "Todas" ? minhas : minhas.filter((s) => s.status === filtro);

  return (
    <AppShell role="hospital">
      <PageHeader
        title="Minhas solicitações"
        subtitle={`${minhas.length} solicitações registradas`}
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

      <div className="flex flex-wrap items-center gap-2">
        <span className="flex items-center gap-1.5 text-xs font-semibold text-muted-foreground">
          <Filter className="size-3.5" aria-hidden="true" /> Status
        </span>
        <GrupoOpcoes
          legenda="Filtrar por status"
          opcoes={filtros.map((f) => ({
            valor: f,
            rotulo: `${f}${f === "Todas" ? "" : ` (${minhas.filter((s) => s.status === f).length})`}`,
          }))}
          valor={filtro}
          aoMudar={setFiltro}
        />
      </div>

      <Panel>
        <ul className="divide-y divide-border">
          {lista.map((s, i) => (
            <motion.li
              key={s.id}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: Math.min(i, 8) * 0.04, duration: 0.35 }}
            >
              <Link
                to="/hospital/solicitacoes/$id"
                params={{ id: s.id }}
                className="flex flex-wrap items-center gap-3 px-5 py-4 transition-colors hover:bg-secondary/60"
              >
                <span
                  className="grid size-9 shrink-0 place-items-center rounded-xl bg-primary-soft text-xs font-bold text-primary"
                  aria-hidden="true"
                >
                  {s.tipoSanguineo}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-semibold">
                    {s.id} · {s.componente} · {s.quantidade} bolsas
                  </p>
                  <p className="mt-0.5 truncate text-xs text-muted-foreground">
                    Registrada em {formatarDataHora(s.criadaEm)}
                  </p>
                </div>
                <PrioridadeBadge prioridade={s.prioridade} />
                <StatusBadge status={s.status} />
                <ChevronRight className="size-4 shrink-0 text-muted-foreground" aria-hidden="true" />
              </Link>
            </motion.li>
          ))}
        </ul>
        {lista.length === 0 ? (
          <EstadoVazio
            titulo="Nenhuma solicitação com esse status"
            descricao="Troque o filtro para ver outras solicitações."
          />
        ) : null}
      </Panel>
    </AppShell>
  );
}
