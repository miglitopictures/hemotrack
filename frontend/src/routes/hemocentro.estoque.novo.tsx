import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { motion } from "motion/react";
import { useState } from "react";
import { ArrowLeft, Droplet, Loader2, Save } from "lucide-react";
import { toast } from "sonner";
import { AppShell } from "@/components/app-shell";
import { GrupoOpcoes, PageHeader, Panel, fadeUp } from "@/components/ui-kit";
import { cadastrarBolsa } from "@/lib/api";
import { COMPONENTE_PARA_BACK, VOLUME_PADRAO_ML } from "@/lib/adapters/requisicao";
import {
  armazenamentoPorComponente,
  componentes,
  formatarData,
  tiposSanguineos,
} from "@/lib/data";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/hemocentro/estoque/novo")({
  head: () => ({
    meta: [
      { title: "Cadastrar hemocomponente — HemoTrack" },
      {
        name: "description",
        content:
          "Registre uma nova bolsa no estoque do hemocentro: componente, tipo sanguíneo, coleta, validade e armazenamento.",
      },
      { property: "og:title", content: "Cadastrar hemocomponente — HemoTrack" },
      {
        property: "og:description",
        content: "Entrada de bolsas no inventário do hemocentro.",
      },
    ],
  }),
  component: NovoHemocomponente,
});

/** Validade padrão por componente, em dias (o operador ainda pode ajustar). */
const validadePadrao: Record<string, number> = {
  "Concentrado de hemácias": 35,
  "Concentrado de plaquetas": 5,
  "Plasma fresco congelado": 365,
  Crioprecipitado: 365,
};

/** Data de hoje no fuso do navegador, no formato do input date (AAAA-MM-DD). */
function hojeIso(): string {
  const d = new Date();
  const mes = String(d.getMonth() + 1).padStart(2, "0");
  const dia = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${mes}-${dia}`;
}

/** Volume de uma bolsa padrão do componente (o mesmo que o back usa quando não vem volume). */
function volumePadrao(componente: string): string {
  const tipo = COMPONENTE_PARA_BACK[componente];
  return tipo ? String(VOLUME_PADRAO_ML[tipo]) : "";
}

function somarDias(iso: string, dias: number): string {
  const d = new Date(`${iso}T00:00:00`);
  if (Number.isNaN(d.getTime())) return "";
  d.setDate(d.getDate() + dias);
  return d.toISOString().slice(0, 10);
}

function Field({
  label,
  placeholder,
  value,
  onChange,
  type = "text",
  required = false,
}: {
  label: string;
  placeholder?: string;
  value: string;
  onChange: (v: string) => void;
  type?: string;
  required?: boolean;
}) {
  return (
    <label className="flex flex-col gap-1.5">
      <span className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
        {label}
        {required ? <span className="text-primary"> *</span> : null}
      </span>
      <input
        type={type}
        value={value}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
        className="rounded-xl border border-border bg-background px-3 py-2.5 text-sm outline-none transition-colors focus:border-primary"
      />
    </label>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-start justify-between gap-3 border-b border-border pb-3 last:border-0">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="text-right text-sm font-semibold">{value}</dd>
    </div>
  );
}

function NovoHemocomponente() {
  const navigate = useNavigate();
  const hoje = hojeIso();
  const componenteInicial = componentes[0] ?? "Concentrado de hemácias";

  const [componente, setComponente] = useState(componenteInicial);
  const [tipo, setTipo] = useState("O-");
  const [codigo, setCodigo] = useState("");
  const [volume, setVolume] = useState(volumePadrao(componenteInicial));
  const [coleta, setColeta] = useState(hoje);
  const [validade, setValidade] = useState(somarDias(hoje, validadePadrao[componenteInicial] ?? 35));
  const [erro, setErro] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  // o back não guarda a câmara: o armazenamento mostrado é o padrão do componente
  const local = armazenamentoPorComponente[componente] ?? "—";

  /** Trocar o componente ajusta volume e validade sugeridos. */
  function escolherComponente(c: string) {
    setComponente(c);
    setVolume(volumePadrao(c));
    setValidade(somarDias(coleta || hoje, validadePadrao[c] ?? 35));
  }

  function ajustarColeta(novaColeta: string) {
    setColeta(novaColeta);
    setValidade(somarDias(novaColeta, validadePadrao[componente] ?? 35));
  }

  async function salvar(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!codigo.trim()) {
      setErro("Informe o código da bolsa.");
      return;
    }
    if (!coleta || !validade) {
      setErro("Informe as datas de coleta e validade.");
      return;
    }
    if (validade <= coleta) {
      setErro("A validade precisa ser posterior à data de coleta.");
      return;
    }
    const volumeMl = Number(volume.replace(",", "."));
    if (volume.trim() && (!Number.isFinite(volumeMl) || volumeMl <= 0)) {
      setErro("O volume precisa ser um número maior que zero.");
      return;
    }
    setErro(null);
    setSalvando(true);
    try {
      await cadastrarBolsa({
        codigo: codigo.trim().toUpperCase(),
        componente,
        tipoSanguineo: tipo,
        coleta,
        validade,
        ...(volume.trim() ? { volumeMl } : {}),
      });
      toast.success("Bolsa registrada no estoque", {
        description: `${codigo.trim().toUpperCase()} · ${componente} · ${tipo}`,
      });
      void navigate({ to: "/hemocentro/estoque", search: { q: "" } });
    } catch (err) {
      const mensagem = err instanceof Error ? err.message : "Não foi possível salvar a bolsa.";
      setErro(mensagem);
      toast.error("Falha ao salvar", { description: mensagem });
    } finally {
      setSalvando(false);
    }
  }

  return (
    <AppShell role="hemocentro">
      <Link
        to="/hemocentro/estoque"
        search={{ q: "" }}
        className="inline-flex items-center gap-2 text-sm font-medium text-muted-foreground transition-colors hover:text-primary"
      >
        <ArrowLeft className="size-4" aria-hidden="true" /> Estoque
      </Link>

      <PageHeader
        title="Cadastrar hemocomponente"
        subtitle="Entrada de nova bolsa no inventário do hemocentro"
      />

      <form onSubmit={salvar} noValidate className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <Panel title="Hemocomponente" description="Selecione o tipo de bolsa">
            <div className="p-5">
              <GrupoOpcoes
                legenda="Hemocomponente"
                variante="cartao"
                className="grid gap-3 sm:grid-cols-2"
                opcoes={componentes.map((c) => ({ valor: c, rotulo: c }))}
                valor={componente}
                aoMudar={escolherComponente}
              />
              <p className="mt-3 flex items-center gap-2 text-xs text-muted-foreground">
                <Droplet className="size-3.5 text-primary" aria-hidden="true" />
                Volume e validade são sugeridos automaticamente pelo componente.
              </p>
            </div>
          </Panel>

          <Panel title="Tipo sanguíneo" description="Grupo ABO e fator Rh da doação">
            <div className="p-5">
              <GrupoOpcoes
                legenda="Tipo sanguíneo"
                variante="compacto"
                className="grid grid-cols-4 gap-3 sm:grid-cols-8"
                opcoes={tiposSanguineos.map((t) => ({ valor: t, rotulo: t }))}
                valor={tipo}
                aoMudar={setTipo}
              />
            </div>
          </Panel>

          <Panel title="Dados da bolsa">
            <div className="grid gap-4 p-5 sm:grid-cols-2">
              <Field
                label="Código da bolsa"
                placeholder="BL-88300"
                value={codigo}
                onChange={setCodigo}
                required
              />
              <Field label="Volume (mL)" placeholder="450" value={volume} onChange={setVolume} />
              <Field label="Data da coleta" type="date" value={coleta} onChange={ajustarColeta} required />
              <Field label="Validade" type="date" value={validade} onChange={setValidade} required />
            </div>
          </Panel>
        </div>

        <motion.div
          {...fadeUp}
          className="h-fit rounded-2xl border border-border bg-card p-5 shadow-card lg:sticky lg:top-24"
        >
          <h2 className="text-base font-semibold">Resumo da entrada</h2>
          <dl className="mt-4 space-y-3 text-sm">
            <Row label="Código" value={codigo.trim().toUpperCase() || "—"} />
            <Row label="Componente" value={componente} />
            <Row label="Tipo sanguíneo" value={tipo} />
            <Row label="Coleta" value={coleta ? formatarData(coleta) : "—"} />
            <Row label="Validade" value={validade ? formatarData(validade) : "—"} />
            <Row label="Volume" value={volume.trim() ? `${volume} mL` : "padrão"} />
            <Row label="Armazenamento (padrão)" value={local} />
            <Row label="Status inicial" value="Disponível" />
          </dl>

          {erro ? (
            <p role="alert" className="mt-4 text-sm font-medium text-destructive">
              {erro}
            </p>
          ) : null}

          <motion.button
            type="submit"
            disabled={salvando}
            whileHover={{ y: -2 }}
            whileTap={{ scale: 0.96 }}
            className={cn(
              "mt-5 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground shadow-card",
              salvando && "cursor-not-allowed opacity-60",
            )}
          >
            {salvando ? (
              <Loader2 className="size-4 animate-spin" aria-hidden="true" />
            ) : (
              <Save className="size-4" aria-hidden="true" />
            )}
            {salvando ? "Salvando…" : "Salvar no estoque"}
          </motion.button>
        </motion.div>
      </form>
    </AppShell>
  );
}
