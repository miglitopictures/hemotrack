import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { motion } from "motion/react";
import { useState } from "react";
import { Boxes, Filter, PlusCircle, Search, Snowflake, Thermometer } from "lucide-react";
import { AppShell } from "@/components/app-shell";
import {
  EstadoErro,
  EstadoVazio,
  GrupoOpcoes,
  PageHeader,
  Panel,
  SkeletonPagina,
  StatCard,
  Stagger,
  StaggerItem,
} from "@/components/ui-kit";
import { listarBolsas } from "@/lib/api";
import {
  bolsaSelecionavel,
  bolsasVencendoEm,
  componentes,
  contarPorArmazenamento,
  diasParaVencer,
  estoquePorTipo,
  formatarData,
  totalDisponivel,
  type StatusBolsa,
} from "@/lib/data";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/hemocentro/estoque/")({
  head: () => ({
    meta: [
      { title: "Estoque de hemocomponentes — HemoTrack" },
      {
        name: "description",
        content:
          "Controle de bolsas por componente, tipo sanguíneo, validade e local de armazenamento no hemocentro.",
      },
      { property: "og:title", content: "Estoque de hemocomponentes — HemoTrack" },
      {
        property: "og:description",
        content: "Bolsas disponíveis, reservadas e próximas do vencimento.",
      },
    ],
  }),
  // A busca global do header manda o código da bolsa por aqui (?q=BL-88214).
  validateSearch: (search: Record<string, unknown>) => ({
    q: typeof search["q"] === "string" ? search["q"] : "",
  }),
  loader: async () => ({ bolsas: await listarBolsas() }),
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
  component: Estoque,
});

const statusStyles: Record<StatusBolsa, string> = {
  Disponível: "bg-success-soft text-success border-success/20",
  Reservada: "bg-teal-soft text-teal border-teal/20",
  "Vence em breve": "bg-warning-soft text-warning border-warning/25",
  Enviada: "bg-muted text-muted-foreground border-border",
};

const LIMITE_LINHAS = 40;

function Estoque() {
  const { bolsas } = Route.useLoaderData();
  const { q } = Route.useSearch();
  const navigate = useNavigate();

  const [comp, setComp] = useState<string>("Todos");
  const [termo, setTermo] = useState(q);

  const busca = termo.trim().toLowerCase();
  const lista = bolsas
    .filter((b) => (comp === "Todos" ? true : b.componente === comp))
    .filter(
      (b) =>
        busca.length === 0 ||
        b.codigo.toLowerCase().includes(busca) ||
        b.tipoSanguineo.toLowerCase() === busca ||
        b.armazenamento.toLowerCase().includes(busca),
    )
    .sort((a, b) => diasParaVencer(a.validade) - diasParaVencer(b.validade));

  const visiveis = lista.slice(0, LIMITE_LINHAS);

  return (
    <AppShell role="hemocentro">
      <PageHeader
        title="Estoque"
        subtitle={`Hemocentro Regional Recife · ${bolsas.length} bolsas no inventário`}
        actions={
          <Link to="/hemocentro/estoque/novo">
            <motion.span
              whileHover={{ y: -2 }}
              whileTap={{ scale: 0.96 }}
              className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground shadow-card"
            >
              <PlusCircle className="size-4" aria-hidden="true" /> Cadastrar hemocomponente
            </motion.span>
          </Link>
        }
      />

      <Stagger className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StaggerItem className="h-full">
          <StatCard
            label="Disponíveis para uso"
            value={totalDisponivel}
            hint={`de ${bolsas.length} bolsas no inventário`}
            tone="primary"
            icon={<Boxes className="size-4" />}
          />
        </StaggerItem>
        <StaggerItem className="h-full">
          <StatCard
            label="Refrigeradas (4 °C)"
            value={contarPorArmazenamento("4 °C")}
            hint="Hemácias"
            tone="teal"
            icon={<Thermometer className="size-4" />}
          />
        </StaggerItem>
        <StaggerItem className="h-full">
          <StatCard
            label="Congeladas (-30 °C)"
            value={contarPorArmazenamento("-30 °C")}
            hint="Plasma e crioprecipitado"
            tone="neutral"
            icon={<Snowflake className="size-4" />}
          />
        </StaggerItem>
        <StaggerItem className="h-full">
          <StatCard
            label="Vencem em até 3 dias"
            value={bolsasVencendoEm(3).length}
            hint="Priorizar distribuição"
            tone="warning"
            icon={<Thermometer className="size-4" />}
            highlight
          />
        </StaggerItem>
      </Stagger>

      <Panel
        title="Distribuição por tipo sanguíneo"
        description="Bolsas disponíveis por grupo ABO/Rh · em laranja, abaixo do mínimo"
      >
        <div className="grid grid-cols-2 gap-3 p-5 sm:grid-cols-4 xl:grid-cols-8">
          {estoquePorTipo.map((t, i) => {
            const critico = t.bolsas < t.minimo;
            return (
              <motion.div
                key={t.tipo}
                initial={{ opacity: 0, y: 12 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.04, duration: 0.4 }}
                whileHover={{ y: -3 }}
                className={cn(
                  "rounded-2xl border bg-background p-4 text-center",
                  critico ? "border-warning/40 bg-warning-soft/40" : "border-border",
                )}
              >
                <p
                  className={cn(
                    "font-display text-lg font-semibold",
                    critico ? "text-warning" : "text-primary",
                  )}
                >
                  {t.tipo}
                </p>
                <p className="mt-1 font-display text-2xl font-semibold tabular-nums">{t.bolsas}</p>
                <p className="text-xs text-muted-foreground">mín. {t.minimo}</p>
              </motion.div>
            );
          })}
        </div>
      </Panel>

      <div className="flex flex-wrap items-center gap-3">
        <div className="flex flex-wrap items-center gap-2">
          <span className="flex items-center gap-1.5 text-xs font-semibold text-muted-foreground">
            <Filter className="size-3.5" aria-hidden="true" /> Componente
          </span>
          <GrupoOpcoes
            legenda="Filtrar por componente"
            opcoes={["Todos", ...componentes].map((c) => ({ valor: c, rotulo: c }))}
            valor={comp}
            aoMudar={setComp}
          />
        </div>

        <label className="ml-auto flex min-w-56 items-center gap-2 rounded-xl border border-border bg-card px-3 py-2 text-sm">
          <Search className="size-4 text-muted-foreground" aria-hidden="true" />
          <input
            type="search"
            value={termo}
            onChange={(e) => {
              setTermo(e.target.value);
              void navigate({ to: "/hemocentro/estoque", search: { q: e.target.value } });
            }}
            placeholder="Código, tipo ou câmara"
            aria-label="Buscar no estoque"
            className="w-full bg-transparent outline-none placeholder:text-muted-foreground"
          />
        </label>
      </div>

      <Panel>
        {visiveis.length === 0 ? (
          <EstadoVazio
            titulo="Nenhuma bolsa encontrada"
            descricao="Ajuste o filtro de componente ou limpe a busca."
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-3xl text-sm">
              <caption className="sr-only">
                Inventário de bolsas do hemocentro, ordenado pelo vencimento mais próximo
              </caption>
              <thead>
                <tr className="border-b border-border text-left text-xs uppercase tracking-wide text-muted-foreground">
                  <th scope="col" className="px-5 py-3 font-semibold">
                    Código
                  </th>
                  <th scope="col" className="px-5 py-3 font-semibold">
                    Componente
                  </th>
                  <th scope="col" className="px-5 py-3 font-semibold">
                    Tipo
                  </th>
                  <th scope="col" className="px-5 py-3 font-semibold">
                    Coleta
                  </th>
                  <th scope="col" className="px-5 py-3 font-semibold">
                    Validade
                  </th>
                  <th scope="col" className="px-5 py-3 font-semibold">
                    Armazenamento
                  </th>
                  <th scope="col" className="px-5 py-3 font-semibold">
                    Status
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {visiveis.map((b, i) => {
                  const dias = diasParaVencer(b.validade);
                  return (
                    <motion.tr
                      key={b.codigo}
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: Math.min(i, 10) * 0.03, duration: 0.3 }}
                      className="transition-colors hover:bg-secondary/70"
                    >
                      <th scope="row" className="px-5 py-4 text-left font-semibold">
                        {b.codigo}
                      </th>
                      <td className="px-5 py-4">{b.componente}</td>
                      <td className="px-5 py-4 font-semibold text-primary">{b.tipoSanguineo}</td>
                      <td className="whitespace-nowrap px-5 py-4 text-muted-foreground">
                        {formatarData(b.coleta)}
                      </td>
                      <td className="whitespace-nowrap px-5 py-4 text-muted-foreground">
                        {formatarData(b.validade)}
                        <span
                          className={cn(
                            "ml-2 text-xs font-semibold",
                            dias <= 3 ? "text-warning" : "text-muted-foreground",
                          )}
                        >
                          {dias <= 0 ? "vencida" : `${dias}d`}
                        </span>
                      </td>
                      <td className="px-5 py-4 text-xs text-muted-foreground">
                        {b.armazenamento}
                      </td>
                      <td className="px-5 py-4">
                        <span
                          className={cn(
                            "inline-flex items-center rounded-full border px-2.5 py-1 text-xs font-semibold",
                            statusStyles[b.status],
                          )}
                        >
                          {b.status}
                        </span>
                      </td>
                    </motion.tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
        {lista.length > LIMITE_LINHAS ? (
          <p className="border-t border-border px-5 py-3 text-xs text-muted-foreground">
            Mostrando {LIMITE_LINHAS} de {lista.length} bolsas · refine a busca para ver o restante.
            {/* TODO(back): trocar por paginação real (GET /bolsas?page=&limit=). */}
          </p>
        ) : null}
        <p className="border-t border-border px-5 py-3 text-xs text-muted-foreground">
          {lista.filter(bolsaSelecionavel).length} bolsa(s) desta seleção estão livres para
          separação.
        </p>
      </Panel>
    </AppShell>
  );
}
