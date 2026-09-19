import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { motion } from "motion/react";
import { useState } from "react";
import { Droplet, Info, Loader2, Send } from "lucide-react";
import { toast } from "sonner";
import { AppShell } from "@/components/app-shell";
import { GrupoOpcoes, PageHeader, Panel, fadeUp } from "@/components/ui-kit";
import { criarSolicitacao } from "@/lib/api";
import {
  componentes,
  tiposCompativeis,
  tiposSanguineos,
  type Prioridade,
} from "@/lib/data";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/hospital/nova-solicitacao")({
  head: () => ({
    meta: [
      { title: "Nova solicitação — HemoTrack" },
      {
        name: "description",
        content:
          "Registre uma solicitação de hemocomponente com tipo sanguíneo, quantidade e prioridade clínica.",
      },
      { property: "og:title", content: "Nova solicitação — HemoTrack" },
      {
        property: "og:description",
        content: "Formulário de solicitação de hemocomponentes ao hemocentro.",
      },
    ],
  }),
  component: NovaSolicitacao,
});

const prioridades: Prioridade[] = ["Rotina", "Urgente", "Emergência"];
const QTD_MIN = 1;
const QTD_MAX = 40;

function limitar(n: number): number {
  if (!Number.isFinite(n)) return QTD_MIN;
  return Math.min(QTD_MAX, Math.max(QTD_MIN, Math.round(n)));
}

function NovaSolicitacao() {
  const navigate = useNavigate();
  const [componente, setComponente] = useState(componentes[0] ?? "Concentrado de hemácias");
  const [tipo, setTipo] = useState("O-");
  // Campo de quantidade guardado como texto para o usuário poder apagar
  // sem virar NaN; a validação acontece no envio.
  const [quantidadeTexto, setQuantidadeTexto] = useState("4");
  const [prioridade, setPrioridade] = useState<Prioridade>("Urgente");
  const [observacoes, setObservacoes] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const quantidade = limitar(Number(quantidadeTexto));
  const quantidadeInvalida = quantidadeTexto.trim() === "" || Number(quantidadeTexto) < QTD_MIN;
  const compativeis = tiposCompativeis(componente, tipo);

  async function enviar(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (quantidadeInvalida) {
      setErro(`Informe uma quantidade entre ${QTD_MIN} e ${QTD_MAX} bolsas.`);
      return;
    }
    setErro(null);
    setEnviando(true);
    try {
      const criada = await criarSolicitacao({
        componente,
        tipoSanguineo: tipo,
        quantidade,
        prioridade,
        observacoes,
      });
      toast.success(`Solicitação ${criada.id} enviada`, {
        description: `${quantidade} bolsa(s) de ${componente.toLowerCase()} · ${tipo} · ${prioridade}`,
      });
      void navigate({ to: "/hospital/solicitacoes" });
    } catch (e) {
      const mensagem = e instanceof Error ? e.message : "Não foi possível enviar a solicitação.";
      setErro(mensagem);
      toast.error("Falha ao enviar", { description: mensagem });
    } finally {
      setEnviando(false);
    }
  }

  return (
    <AppShell role="hospital">
      <PageHeader
        title="Nova solicitação"
        subtitle="Os dados do hospital são preenchidos automaticamente pelo cadastro"
      />

      <div className="grid gap-6 lg:grid-cols-[1.5fr_1fr]">
        <Panel title="Dados da solicitação">
          <form className="space-y-6 p-5" onSubmit={enviar} noValidate>
            <div>
              <span className="text-sm font-medium">Tipo de hemocomponente</span>
              <GrupoOpcoes
                legenda="Tipo de hemocomponente"
                variante="cartao"
                className="mt-2 grid gap-2 sm:grid-cols-2"
                opcoes={componentes.map((c) => ({ valor: c, rotulo: c }))}
                valor={componente}
                aoMudar={setComponente}
              />
            </div>

            <div>
              <span className="text-sm font-medium">Tipo sanguíneo do receptor</span>
              <GrupoOpcoes
                legenda="Tipo sanguíneo do receptor"
                variante="compacto"
                className="mt-2"
                opcoes={tiposSanguineos.map((t) => ({ valor: t, rotulo: t }))}
                valor={tipo}
                aoMudar={setTipo}
              />
              <p className="mt-2 text-xs text-muted-foreground">
                Bolsas compatíveis para {tipo}: {compativeis.join(", ")}
              </p>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <label className="block">
                <span className="text-sm font-medium">Quantidade de bolsas</span>
                <input
                  type="number"
                  inputMode="numeric"
                  min={QTD_MIN}
                  max={QTD_MAX}
                  name="quantidade"
                  value={quantidadeTexto}
                  onChange={(e) => setQuantidadeTexto(e.target.value)}
                  onBlur={() => setQuantidadeTexto(String(limitar(Number(quantidadeTexto))))}
                  aria-invalid={quantidadeInvalida}
                  className={cn(
                    "mt-1.5 w-full rounded-xl border bg-background px-3 py-2.5 text-sm outline-none focus:border-primary",
                    quantidadeInvalida ? "border-destructive" : "border-input",
                  )}
                />
                <span className="mt-1 block text-xs text-muted-foreground">
                  De {QTD_MIN} a {QTD_MAX} bolsas por solicitação.
                </span>
              </label>

              <div>
                <span className="text-sm font-medium">Prioridade</span>
                <div
                  role="radiogroup"
                  aria-label="Prioridade"
                  className="mt-1.5 flex rounded-xl border border-border p-1"
                >
                  {prioridades.map((p) => {
                    const ativo = prioridade === p;
                    return (
                      <button
                        key={p}
                        type="button"
                        role="radio"
                        aria-checked={ativo}
                        onClick={() => setPrioridade(p)}
                        className="relative flex-1 rounded-lg px-2 py-1.5 text-xs font-semibold"
                      >
                        {ativo ? (
                          <motion.span
                            layoutId="prioridade-pill"
                            className="absolute inset-0 rounded-lg bg-primary"
                            transition={{ type: "spring", stiffness: 400, damping: 30 }}
                          />
                        ) : null}
                        <span
                          className={cn(
                            "relative",
                            ativo ? "text-primary-foreground" : "text-muted-foreground",
                          )}
                        >
                          {p}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>
            </div>

            <label className="block">
              <span className="text-sm font-medium">Observações</span>
              <textarea
                rows={4}
                name="observacoes"
                value={observacoes}
                onChange={(e) => setObservacoes(e.target.value)}
                maxLength={400}
                placeholder="Contexto clínico, unidade de destino, horário desejado..."
                className="mt-1.5 w-full resize-none rounded-xl border border-input bg-background px-3 py-2.5 text-sm outline-none focus:border-primary"
              />
              <span className="mt-1 block text-right text-xs text-muted-foreground">
                {observacoes.length}/400
              </span>
            </label>

            {erro ? (
              <p role="alert" className="text-sm font-medium text-destructive">
                {erro}
              </p>
            ) : null}

            <motion.button
              type="submit"
              disabled={enviando}
              whileHover={{ y: -2 }}
              whileTap={{ scale: 0.97 }}
              className="flex w-full items-center justify-center gap-2 rounded-xl bg-primary px-4 py-3 text-sm font-semibold text-primary-foreground shadow-card disabled:cursor-not-allowed disabled:opacity-60"
            >
              {enviando ? (
                <Loader2 className="size-4 animate-spin" aria-hidden="true" />
              ) : (
                <Send className="size-4" aria-hidden="true" />
              )}
              {enviando ? "Enviando…" : "Enviar solicitação"}
            </motion.button>
          </form>
        </Panel>

        <motion.div {...fadeUp} className="space-y-4">
          <Panel title="Resumo">
            <dl className="space-y-3 p-5 text-sm">
              {[
                ["Hospital", "Hospital Santa Clara"],
                ["Componente", componente],
                ["Tipo sanguíneo", tipo],
                ["Quantidade", `${quantidade} bolsa(s)`],
                ["Prioridade", prioridade],
                ["Observações", observacoes.trim() || "—"],
              ].map(([k, v]) => (
                <div key={k} className="flex justify-between gap-4">
                  <dt className="shrink-0 text-muted-foreground">{k}</dt>
                  <dd className="text-right font-medium">{v}</dd>
                </div>
              ))}
            </dl>
          </Panel>

          <div className="rounded-2xl border border-teal/20 bg-teal-soft p-5">
            <span
              className="grid size-9 place-items-center rounded-xl bg-teal text-teal-foreground"
              aria-hidden="true"
            >
              <Info className="size-4" />
            </span>
            <p className="mt-3 text-sm font-semibold text-teal">Compatibilidade</p>
            <p className="mt-1 text-xs leading-relaxed text-teal">
              O hemocentro separa as bolsas seguindo a compatibilidade ABO/Rh e a validade mais
              próxima. Solicitações de emergência entram na frente da fila de análise.
            </p>
          </div>

          <Link
            to="/hospital/solicitacoes"
            className="block rounded-2xl border border-dashed border-border p-4 text-center text-sm font-medium text-muted-foreground transition-colors hover:border-primary/40 hover:text-primary"
          >
            <Droplet className="mx-auto mb-1 size-4" aria-hidden="true" />
            Ver minhas solicitações
          </Link>
        </motion.div>
      </div>
    </AppShell>
  );
}
