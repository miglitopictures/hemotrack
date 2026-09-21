import { Link } from "@tanstack/react-router";
import { motion } from "motion/react";
import {
  AlertTriangle,
  ArrowLeft,
  CheckCircle2,
  Clock,
  MapPin,
  PackageCheck,
  Thermometer,
} from "lucide-react";
import type { ReactNode } from "react";
import { RotaMapa } from "@/components/rota-mapa";
import { TemperaturaGrafico } from "@/components/temperatura-grafico";
import { PageHeader, Panel, StatusBadge, fadeUp } from "@/components/ui-kit";
import {
  alertasDoTransporte,
  bolsaPorCodigo,
  componenteDoTransporte,
  diasParaVencer,
  formatarData,
  formatarTemperatura,
  leiturasDoTransporte,
  rotaSimplificada,
  temperaturaAtual,
  temperaturaFora,
  timelineEtapas,
  type Transporte,
} from "@/lib/data";
import { cn } from "@/lib/utils";

function Metric({
  icon,
  label,
  value,
  destaque = false,
}: {
  icon: ReactNode;
  label: string;
  value: string;
  destaque?: boolean;
}) {
  return (
    <div
      className={cn(
        "rounded-xl border bg-background p-4",
        destaque ? "border-warning/40" : "border-border",
      )}
    >
      <span
        className={cn(
          "grid size-8 place-items-center rounded-lg",
          destaque ? "bg-warning-soft text-warning" : "bg-primary-soft text-primary",
        )}
        aria-hidden="true"
      >
        {icon}
      </span>
      <p className="mt-3 text-xs text-muted-foreground">{label}</p>
      <p className="mt-0.5 font-display text-lg font-semibold">{value}</p>
    </div>
  );
}

export function DetalheTransporte({
  transporte: t,
  papel,
}: {
  transporte: Transporte;
  papel: "hospital" | "hemocentro";
}) {
  const componente = componenteDoTransporte(t);
  const leituras = leiturasDoTransporte(t);
  const temp = temperaturaAtual(t);
  const tempFora = temp !== null && temperaturaFora(componente, temp);
  const alertas = alertasDoTransporte(t);
  const etapaAtual = timelineEtapas.indexOf(t.status);

  return (
    <>
      {papel === "hospital" ? (
        <Link
          to="/hospital/transportes"
          className="inline-flex items-center gap-2 text-sm font-medium text-muted-foreground transition-colors hover:text-primary"
        >
          <ArrowLeft className="size-4" aria-hidden="true" /> Transportes
        </Link>
      ) : (
        <Link
          to="/hemocentro/distribuicoes"
          className="inline-flex items-center gap-2 text-sm font-medium text-muted-foreground transition-colors hover:text-primary"
        >
          <ArrowLeft className="size-4" aria-hidden="true" /> Distribuição
        </Link>
      )}

      <PageHeader
        title={`Transporte ${t.id}`}
        subtitle={`Solicitação ${t.solicitacao} · ${t.veiculo} · ${componente}`}
        actions={<StatusBadge status={t.status} />}
      />

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          <motion.div
            {...fadeUp}
            className="overflow-hidden rounded-2xl border border-border bg-card shadow-card"
          >
            <RotaMapa
              pontos={rotaSimplificada}
              progresso={t.progresso}
              localizacao={t.localizacao}
              alerta={tempFora}
            />
            <div className="grid gap-4 border-t border-border p-5 sm:grid-cols-3">
              <Metric
                icon={<Clock className="size-4" />}
                label="Previsão de chegada"
                value={t.eta}
              />
              <Metric
                icon={tempFora ? <AlertTriangle className="size-4" /> : <Thermometer className="size-4" />}
                label="Temperatura da carga"
                value={formatarTemperatura(temp)}
                destaque={tempFora}
              />
              <Metric
                icon={<PackageCheck className="size-4" />}
                label="Bolsas embarcadas"
                value={`${t.bolsas.length}`}
              />
            </div>
          </motion.div>

          <Panel
            title="Cadeia de frio"
            description={`Leituras do datalogger · ${componente.toLowerCase()}`}
          >
            <TemperaturaGrafico componente={componente} leituras={leituras} />
          </Panel>

          <Panel title="Linha do tempo da remessa">
            <ol className="relative space-y-6 p-5 pl-8">
              <span className="absolute bottom-7 left-[1.6rem] top-7 w-0.5 bg-border" aria-hidden="true" />
              {timelineEtapas.map((etapa, i) => {
                const feito = i <= etapaAtual;
                return (
                  <motion.li
                    key={etapa}
                    initial={{ opacity: 0, x: 12 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: i * 0.08, duration: 0.4 }}
                    className="relative flex items-start gap-4"
                  >
                    <span
                      aria-hidden="true"
                      className={cn(
                        "relative z-10 grid size-7 shrink-0 place-items-center rounded-full border-2",
                        feito
                          ? "border-primary bg-primary text-primary-foreground"
                          : "border-border bg-card text-muted-foreground",
                      )}
                    >
                      {feito ? <CheckCircle2 className="size-3.5" /> : <Clock className="size-3.5" />}
                    </span>
                    <div>
                      <p
                        className={cn(
                          "text-sm font-semibold",
                          feito ? "text-foreground" : "text-muted-foreground",
                        )}
                      >
                        {etapa}
                      </p>
                      <p className="mt-0.5 text-xs text-muted-foreground">
                        {feito ? "Etapa concluída" : "Aguardando"}
                      </p>
                    </div>
                  </motion.li>
                );
              })}
            </ol>
          </Panel>
        </div>

        <div className="space-y-6">
          <Panel title="Bolsas na remessa" description="Código, tipo e validade conferidos">
            <ul className="divide-y divide-border">
              {t.bolsas.map((codigo) => {
                const b = bolsaPorCodigo(codigo);
                return (
                  <li key={codigo} className="flex items-center gap-3 px-5 py-3.5">
                    <span
                      className="grid size-8 shrink-0 place-items-center rounded-lg bg-primary-soft text-xs font-bold text-primary"
                      aria-hidden="true"
                    >
                      {b?.tipoSanguineo ?? "?"}
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-semibold">{codigo}</p>
                      {b ? (
                        <p className="mt-0.5 text-xs text-muted-foreground">
                          Validade {formatarData(b.validade)} · {diasParaVencer(b.validade)} dias
                        </p>
                      ) : null}
                    </div>
                  </li>
                );
              })}
            </ul>
          </Panel>

          <Panel title="Rota">
            <div className="space-y-3 p-5 text-sm">
              <p className="flex items-start gap-2 text-muted-foreground">
                <MapPin className="mt-0.5 size-4 shrink-0 text-teal" aria-hidden="true" /> {t.origem}
              </p>
              <p className="flex items-start gap-2 text-muted-foreground">
                <MapPin className="mt-0.5 size-4 shrink-0 text-primary" aria-hidden="true" />{" "}
                {t.destino}
              </p>
            </div>
          </Panel>

          <Panel title="Alertas do trajeto">
            {alertas.length > 0 ? (
              <ul className="divide-y divide-border">
                {alertas.map((a) => (
                  <li key={a} className="flex gap-3 px-5 py-4">
                    <AlertTriangle className="mt-0.5 size-4 shrink-0 text-warning" aria-hidden="true" />
                    <p className="text-sm text-muted-foreground">{a}</p>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="px-5 py-6 text-sm text-muted-foreground">
                Nenhum alerta registrado nesta remessa.
              </p>
            )}
          </Panel>
        </div>
      </div>
    </>
  );
}
