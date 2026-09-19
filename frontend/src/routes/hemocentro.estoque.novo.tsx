import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { motion } from "motion/react";
import { useState } from "react";
import { ArrowLeft, Droplet, Loader2, Save } from "lucide-react";
import { toast } from "sonner";
import { AppShell } from "@/components/app-shell";
import { GrupoOpcoes, PageHeader, Panel, fadeUp } from "@/components/ui-kit";
import { cadastrarBolsa } from "@/lib/api";
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

const locais = ["Câmara A · 4 °C", "Câmara B · 4 °C", "Agitador P2 · 22 °C", "Freezer F1 · -30 °C"];

/** Validade padrão por componente, em dias (o operador ainda pode ajustar). */
const validadePadrao: Record<string, number> = {
  "Concentrado de hemácias": 35,
  "Concentrado de plaquetas": 5,
  "Plasma fresco congelado": 365,
  Crioprecipitado: 365,
};

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
  const hoje = "2026-08-27"; // TODO(back): usar a data do servidor.

  const [componente, setComponente] = useState(componentes[0] ?? "Concentrado de hemácias");
  const [tipo, setTipo] = useState("O-");
  const [local, setLocal] = useState(locais[0] ?? "");
  const [codigo, setCodigo] = useState("");
  const [volume, setVolume] = useState("450");
  const [coleta, setColeta] = useState(hoje);
  const [validade, setValidade] = useState(somarDias(hoje, 35));
  const [doador, setDoador] = useState("");
  const [lote, setLote] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const [salvando, setSalvando] = useState(false);

  /** Trocar o componente ajusta armazenamento e validade sugeridos. */
  function escolherComponente(c: string) {
    setComponente(c);
    setLocal(armazenamentoPorComponente[c] ?? locais[0] ?? "");
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
    setErro(null);
    setSalvando(true);
    try {
      await cadastrarBolsa({
        codigo: codigo.trim().toUpperCase(),
        componente,
        tipoSanguineo: tipo,
        coleta,
        validade,
        armazenamento: local,
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
                Armazenamento e validade são sugeridos automaticamente pelo componente.
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
              <Field label="Doador (código)" placeholder="DR-1042" value={doador} onChange={setDoador} />
              <Field label="Lote de testes" placeholder="LT-2026-08" value={lote} onChange={setLote} />
            </div>
            <div className="border-t border-border p-5">
              <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                Local de armazenamento
              </p>
              <GrupoOpcoes
                legenda="Local de armazenamento"
                className="mt-3"
                opcoes={locais.map((l) => ({ valor: l, rotulo: l }))}
                valor={local}
                aoMudar={setLocal}
              />
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
            <Row label="Armazenamento" value={local} />
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
