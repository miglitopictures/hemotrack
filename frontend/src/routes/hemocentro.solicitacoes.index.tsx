import { createFileRoute, Link } from "@tanstack/react-router";
import { motion } from "motion/react";
import { useState } from "react";
import { ChevronRight, Filter } from "lucide-react";
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
import { listarSolicitacoes } from "@/lib/api";
import { formatarDataHora, type Prioridade, type Status } from "@/lib/data";

export const Route = createFileRoute("/hemocentro/solicitacoes/")({
  head: () => ({
    meta: [
      { title: "Solicitações recebidas — HemoTrack" },
      {
        name: "description",
        content:
          "Fila de solicitações de hemocomponentes recebidas dos hospitais, com prioridade, status e análise rápida.",
      },
      { property: "og:title", content: "Solicitações recebidas — HemoTrack" },
      {
        property: "og:description",
        content: "Analise, aceite ou recuse pedidos dos hospitais parceiros.",
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
  component: SolicitacoesRecebidas,
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

/** Emergência primeiro — é a fila que o hemocentro realmente trabalha. */
const pesoPrioridade: Record<Prioridade, number> = { Emergência: 0, Urgente: 1, Rotina: 2 };

function SolicitacoesRecebidas() {
  const { solicitacoes } = Route.useLoaderData();
  const [filtro, setFiltro] = useState<Filtro>("Pendente");

  const lista = (filtro === "Todas" ? solicitacoes : solicitacoes.filter((s) => s.status === filtro))
    .slice()
    .sort((a, b) => {
      const p = pesoPrioridade[a.prioridade] - pesoPrioridade[b.prioridade];
      return p !== 0 ? p : a.criadaEm.localeCompare(b.criadaEm);
    });

  const hospitais = new Set(solicitacoes.map((s) => s.hospital)).size;

  return (
    <AppShell role="hemocentro">
      <PageHeader
        title="Solicitações recebidas"
        subtitle={`${solicitacoes.length} pedidos de ${hospitais} hospitais · ordenados por prioridade`}
      />

      <div className="flex flex-wrap items-center gap-2">
        <span className="flex items-center gap-1.5 text-xs font-semibold text-muted-foreground">
          <Filter className="size-3.5" aria-hidden="true" /> Status
        </span>
        <GrupoOpcoes
          legenda="Filtrar por status"
          opcoes={filtros.map((f) => ({
            valor: f,
            rotulo: `${f}${f === "Todas" ? "" : ` (${solicitacoes.filter((s) => s.status === f).length})`}`,
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
                to="/hemocentro/solicitacoes/$id"
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
                    {s.hospital} · recebida em {formatarDataHora(s.criadaEm)}
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
            descricao="Troque o filtro para ver o restante da fila."
          />
        ) : null}
      </Panel>
    </AppShell>
  );
}
