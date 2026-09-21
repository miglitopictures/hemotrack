import { motion, useReducedMotion } from "motion/react";
import type { ReactNode } from "react";
import { AlertTriangle, Inbox, RefreshCw } from "lucide-react";
import { cn } from "@/lib/utils";
import type { Prioridade, Status } from "@/lib/data";

export const fadeUp = {
  initial: { opacity: 0, y: 16 },
  animate: { opacity: 1, y: 0 },
  transition: { duration: 0.45, ease: [0.16, 1, 0.3, 1] as const },
};

export function Stagger({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <motion.div
      className={className}
      initial="hidden"
      animate="show"
      variants={{ hidden: {}, show: { transition: { staggerChildren: 0.07 } } }}
    >
      {children}
    </motion.div>
  );
}

export function StaggerItem({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <motion.div
      className={className}
      variants={{
        hidden: { opacity: 0, y: 18 },
        show: { opacity: 1, y: 0, transition: { duration: 0.5, ease: [0.16, 1, 0.3, 1] } },
      }}
    >
      {children}
    </motion.div>
  );
}

// ---------------------------------------------------------------------------
// Badges
// ---------------------------------------------------------------------------

const statusStyles: Record<Status, string> = {
  Pendente: "bg-warning-soft text-warning border-warning/20",
  Aceita: "bg-teal-soft text-teal border-teal/20",
  "Em separação": "bg-accent text-accent-foreground border-border",
  "Em transporte": "bg-primary-soft text-primary border-primary/20",
  Entregue: "bg-success-soft text-success border-success/20",
  Recusada: "bg-muted text-muted-foreground border-border",
};

export function StatusBadge({ status, className }: { status: Status; className?: string }) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-semibold",
        statusStyles[status],
        className,
      )}
    >
      <span className="size-1.5 rounded-full bg-current" aria-hidden="true" />
      {status}
    </span>
  );
}

const prioridadeStyles: Record<Prioridade, string> = {
  Rotina: "bg-secondary text-secondary-foreground border-border",
  Urgente: "bg-warning-soft text-warning border-warning/25",
  Emergência: "bg-primary text-primary-foreground border-primary",
};

export function PrioridadeBadge({ prioridade }: { prioridade: Prioridade }) {
  const reduzido = useReducedMotion();
  const emergencia = prioridade === "Emergência";

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-semibold",
        prioridadeStyles[prioridade],
      )}
    >
      {emergencia ? (
        reduzido ? (
          <span className="size-1.5 rounded-full bg-current" aria-hidden="true" />
        ) : (
          <motion.span
            aria-hidden="true"
            className="size-1.5 rounded-full bg-current"
            animate={{ opacity: [1, 0.35, 1], scale: [1, 1.5, 1] }}
            transition={{ duration: 1.6, repeat: Infinity, ease: "easeInOut" }}
          />
        )
      ) : null}
      {prioridade}
    </span>
  );
}

// ---------------------------------------------------------------------------
// Estrutura de página
// ---------------------------------------------------------------------------

export function PageHeader({
  title,
  subtitle,
  actions,
}: {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
}) {
  return (
    <motion.header
      {...fadeUp}
      className="flex flex-col gap-4 border-b border-border pb-6 sm:flex-row sm:items-end sm:justify-between"
    >
      <div>
        <h1 className="text-2xl font-semibold sm:text-3xl">{title}</h1>
        {subtitle ? <p className="mt-1.5 text-sm text-muted-foreground">{subtitle}</p> : null}
      </div>
      {actions ? <div className="flex flex-wrap gap-2">{actions}</div> : null}
    </motion.header>
  );
}

export type Tone = "neutral" | "primary" | "teal" | "warning" | "success";

const toneSoft: Record<Tone, string> = {
  neutral: "bg-secondary text-secondary-foreground",
  primary: "bg-primary-soft text-primary",
  teal: "bg-teal-soft text-teal",
  warning: "bg-warning-soft text-warning",
  success: "bg-success-soft text-success",
};

/** O gradiente do card em destaque segue o tom — antes era sempre vermelho. */
const toneGradiente: Record<Tone, string> = {
  neutral:
    "linear-gradient(135deg, var(--secondary-foreground), color-mix(in oklab, var(--secondary-foreground) 60%, var(--teal)))",
  primary:
    "linear-gradient(135deg, var(--primary), color-mix(in oklab, var(--primary) 55%, var(--teal)))",
  teal: "linear-gradient(135deg, var(--teal), color-mix(in oklab, var(--teal) 60%, var(--primary)))",
  warning:
    "linear-gradient(135deg, var(--warning), color-mix(in oklab, var(--warning) 60%, var(--primary)))",
  success:
    "linear-gradient(135deg, var(--success), color-mix(in oklab, var(--success) 60%, var(--teal)))",
};

export function StatCard({
  label,
  value,
  hint,
  icon,
  tone = "neutral",
  highlight = false,
}: {
  label: string;
  value: string | number;
  hint?: string;
  icon: ReactNode;
  tone?: Tone;
  highlight?: boolean;
}) {
  if (highlight) {
    return (
      <motion.div
        whileHover={{ y: -4 }}
        transition={{ type: "spring", stiffness: 320, damping: 22 }}
        className="relative h-full overflow-hidden rounded-3xl p-5 text-white shadow-lift"
        style={{ backgroundImage: toneGradiente[tone] }}
      >
        <div className="surface-grid pointer-events-none absolute inset-0 opacity-[0.15]" />
        <div className="relative flex items-start justify-between gap-3">
          <p className="text-sm font-normal text-white/80">{label}</p>
          <span
            className="grid size-9 place-items-center rounded-xl bg-white/15 backdrop-blur-sm"
            aria-hidden="true"
          >
            {icon}
          </span>
        </div>
        <p className="relative mt-4 font-display text-3xl font-semibold tabular-nums">{value}</p>
        {hint ? <p className="relative mt-1 text-xs text-white/75">{hint}</p> : null}
      </motion.div>
    );
  }

  return (
    <motion.div
      whileHover={{ y: -4 }}
      transition={{ type: "spring", stiffness: 320, damping: 22 }}
      className="h-full rounded-2xl border border-border bg-card p-5 shadow-card"
    >
      <div className="flex items-start justify-between gap-3">
        <p className="text-sm font-normal text-muted-foreground">{label}</p>
        <span
          className={cn("grid size-9 place-items-center rounded-xl", toneSoft[tone])}
          aria-hidden="true"
        >
          {icon}
        </span>
      </div>
      <p className="mt-4 font-display text-3xl font-semibold tabular-nums">{value}</p>
      {hint ? <p className="mt-1 text-xs text-muted-foreground">{hint}</p> : null}
    </motion.div>
  );
}

export function Panel({
  title,
  description,
  actions,
  children,
  className,
}: {
  title?: string;
  description?: string;
  actions?: ReactNode;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={cn("rounded-2xl border border-border bg-card shadow-card", className)}>
      {title ? (
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-border px-5 py-4">
          <div>
            <h2 className="text-base font-semibold">{title}</h2>
            {description ? (
              <p className="mt-0.5 text-xs text-muted-foreground">{description}</p>
            ) : null}
          </div>
          {actions}
        </div>
      ) : null}
      {children}
    </section>
  );
}

// ---------------------------------------------------------------------------
// Grupos acessíveis (radiogroup / tablist)
// ---------------------------------------------------------------------------

export type Opcao<T extends string> = { valor: T; rotulo: string; descricao?: string };

/**
 * Grupo de botões que se comporta como um conjunto de rádios para leitores de
 * tela (antes eram <button> soltos, sem semântica nenhuma).
 */
export function GrupoOpcoes<T extends string>({
  legenda,
  opcoes,
  valor,
  aoMudar,
  className,
  itemClassName,
  variante = "pilula",
}: {
  legenda: string;
  opcoes: Opcao<T>[];
  valor: T;
  aoMudar: (v: T) => void;
  className?: string;
  itemClassName?: string;
  variante?: "pilula" | "cartao" | "compacto";
}) {
  return (
    <div role="radiogroup" aria-label={legenda} className={cn("flex flex-wrap gap-2", className)}>
      {opcoes.map((o) => {
        const ativo = o.valor === valor;
        return (
          <motion.button
            key={o.valor}
            type="button"
            role="radio"
            aria-checked={ativo}
            whileTap={{ scale: 0.97 }}
            onClick={() => aoMudar(o.valor)}
            className={cn(
              "text-left transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring",
              variante === "pilula" &&
                "rounded-full border px-3 py-1.5 text-xs font-semibold " +
                  (ativo
                    ? "border-primary bg-primary text-primary-foreground"
                    : "border-border text-muted-foreground hover:border-primary/40"),
              variante === "compacto" &&
                "min-w-14 rounded-xl border px-3 py-2 text-center text-sm font-semibold " +
                  (ativo
                    ? "border-primary bg-primary text-primary-foreground"
                    : "border-border hover:border-primary/40"),
              variante === "cartao" &&
                "rounded-xl border px-4 py-3 text-sm font-medium " +
                  (ativo
                    ? "border-primary bg-primary-soft text-primary"
                    : "border-border hover:border-primary/40"),
              itemClassName,
            )}
          >
            {o.rotulo}
            {o.descricao ? (
              <span className="mt-0.5 block text-xs font-normal text-muted-foreground">
                {o.descricao}
              </span>
            ) : null}
          </motion.button>
        );
      })}
    </div>
  );
}

/** Abas com semântica de tablist. */
export function Abas<T extends string>({
  legenda,
  abas,
  ativa,
  aoMudar,
}: {
  legenda: string;
  abas: { id: T; rotulo: string }[];
  ativa: T;
  aoMudar: (v: T) => void;
}) {
  return (
    <div role="tablist" aria-label={legenda} className="flex flex-wrap gap-2">
      {abas.map((a) => {
        const selecionada = a.id === ativa;
        return (
          <button
            key={a.id}
            role="tab"
            type="button"
            id={`aba-${a.id}`}
            aria-selected={selecionada}
            aria-controls={`painel-${a.id}`}
            onClick={() => aoMudar(a.id)}
            className={cn(
              "rounded-full border px-4 py-1.5 text-sm font-semibold transition-colors",
              selecionada
                ? "border-primary bg-primary text-primary-foreground"
                : "border-border text-muted-foreground hover:border-primary/40",
            )}
          >
            {a.rotulo}
          </button>
        );
      })}
    </div>
  );
}

export function PainelAba({
  id,
  children,
  className,
}: {
  id: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <div role="tabpanel" id={`painel-${id}`} aria-labelledby={`aba-${id}`} className={className}>
      {children}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Estados: carregando, vazio, erro
// ---------------------------------------------------------------------------

export function Skeleton({ className }: { className?: string }) {
  return <div className={cn("animate-pulse rounded-xl bg-secondary", className)} />;
}

export function SkeletonPagina() {
  return (
    <div className="space-y-6" aria-busy="true" aria-live="polite">
      <span className="sr-only">Carregando…</span>
      <div className="space-y-3 border-b border-border pb-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-4 w-80" />
      </div>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[0, 1, 2, 3].map((i) => (
          <Skeleton key={i} className="h-32" />
        ))}
      </div>
      <Skeleton className="h-64" />
    </div>
  );
}

export function EstadoVazio({
  titulo,
  descricao,
  acao,
}: {
  titulo: string;
  descricao?: string;
  acao?: ReactNode;
}) {
  return (
    <div className="flex flex-col items-center gap-3 px-5 py-12 text-center">
      <span className="grid size-11 place-items-center rounded-2xl bg-secondary text-muted-foreground">
        <Inbox className="size-5" />
      </span>
      <p className="text-sm font-semibold">{titulo}</p>
      {descricao ? <p className="max-w-sm text-sm text-muted-foreground">{descricao}</p> : null}
      {acao}
    </div>
  );
}

export function EstadoErro({ erro, aoTentarDeNovo }: { erro: Error; aoTentarDeNovo?: () => void }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-2xl border border-destructive/20 bg-card px-5 py-12 text-center shadow-card">
      <span className="grid size-11 place-items-center rounded-2xl bg-warning-soft text-warning">
        <AlertTriangle className="size-5" />
      </span>
      <p className="text-sm font-semibold">Não foi possível carregar estes dados</p>
      <p className="max-w-sm text-sm text-muted-foreground">{erro.message}</p>
      {aoTentarDeNovo ? (
        <button
          onClick={aoTentarDeNovo}
          className="mt-1 inline-flex items-center gap-2 rounded-xl border border-border px-4 py-2 text-sm font-semibold transition-colors hover:border-primary/40 hover:text-primary"
        >
          <RefreshCw className="size-4" /> Tentar de novo
        </button>
      ) : null}
    </div>
  );
}

// ---------------------------------------------------------------------------
// Gráficos
// ---------------------------------------------------------------------------

/** Tooltip do recharts seguindo o tema (antes ficava branco no modo escuro). */
export const tooltipTema = {
  contentStyle: {
    background: "var(--card)",
    border: "1px solid var(--border)",
    borderRadius: 12,
    fontSize: 12,
    color: "var(--foreground)",
    boxShadow: "0 8px 24px -12px rgb(0 0 0 / 0.25)",
  },
  labelStyle: { color: "var(--muted-foreground)", marginBottom: 4 },
  itemStyle: { color: "var(--foreground)" },
  cursor: { fill: "var(--secondary)", opacity: 0.5 },
} as const;

export const eixoTema = {
  tickLine: false,
  axisLine: false,
  tick: { fontSize: 12, fill: "var(--muted-foreground)" },
} as const;

/**
 * Envolve um gráfico com descrição textual — leitor de tela não enxerga SVG.
 */
export function Grafico({
  titulo,
  resumo,
  altura = "h-64",
  children,
}: {
  titulo: string;
  resumo: string;
  altura?: string;
  children: ReactNode;
}) {
  return (
    <figure className="m-0">
      <div role="img" aria-label={`${titulo}. ${resumo}`} className={cn("w-full p-5", altura)}>
        {children}
      </div>
      <figcaption className="sr-only">{resumo}</figcaption>
    </figure>
  );
}
