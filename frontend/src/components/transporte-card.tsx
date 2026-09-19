import { Link } from "@tanstack/react-router";
import { motion } from "motion/react";
import { AlertTriangle, MapPin, Thermometer } from "lucide-react";
import { StatusBadge } from "@/components/ui-kit";
import {
  componenteDoTransporte,
  formatarTemperatura,
  temperaturaAtual,
  temperaturaFora,
  type Transporte,
} from "@/lib/data";
import { cn } from "@/lib/utils";

export function TransporteCard({
  transporte: t,
  papel,
  atraso = 0,
}: {
  transporte: Transporte;
  papel: "hospital" | "hemocentro";
  atraso?: number;
}) {
  const temp = temperaturaAtual(t);
  const fora = temp !== null && temperaturaFora(componenteDoTransporte(t), temp);

  return (
    <motion.div
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: atraso, duration: 0.4 }}
      whileHover={{ y: -4 }}
      className={cn(
        "rounded-2xl border bg-background p-5",
        fora ? "border-warning/40" : "border-border",
      )}
    >
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <p className="text-sm font-semibold">
            {t.id} · {t.solicitacao}
          </p>
          <p className="mt-0.5 text-xs text-muted-foreground">{t.veiculo}</p>
        </div>
        <StatusBadge status={t.status} />
      </div>

      <div className="mt-4 space-y-1.5 text-xs text-muted-foreground">
        <p className="flex items-center gap-1.5">
          <MapPin className="size-3.5 text-primary" aria-hidden="true" /> {t.origem}
        </p>
        <p className="flex items-center gap-1.5">
          <MapPin className="size-3.5 text-teal" aria-hidden="true" /> {t.destino}
        </p>
      </div>

      <div
        className="mt-4 h-1.5 overflow-hidden rounded-full bg-secondary"
        role="progressbar"
        aria-valuenow={t.progresso}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={`Progresso do transporte ${t.id}`}
      >
        <motion.div
          initial={{ width: 0 }}
          animate={{ width: `${t.progresso}%` }}
          transition={{ duration: 1, ease: [0.16, 1, 0.3, 1] }}
          className={cn("h-full rounded-full", fora ? "bg-warning" : "bg-primary")}
        />
      </div>

      <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-xs">
        <span
          className={cn(
            "inline-flex items-center gap-1.5",
            fora ? "font-semibold text-warning" : "text-muted-foreground",
          )}
        >
          {fora ? (
            <AlertTriangle className="size-3.5" aria-hidden="true" />
          ) : (
            <Thermometer className="size-3.5 text-primary" aria-hidden="true" />
          )}
          {formatarTemperatura(temp)}
        </span>
        <span className="text-muted-foreground">
          {t.bolsas.length} bolsas · ETA {t.eta}
        </span>
        {papel === "hospital" ? (
          <Link
            to="/hospital/transportes/$id"
            params={{ id: t.id }}
            className="font-semibold text-primary hover:underline"
          >
            Acompanhar transporte
          </Link>
        ) : (
          <Link
            to="/hemocentro/distribuicoes/$id"
            params={{ id: t.id }}
            className="font-semibold text-primary hover:underline"
          >
            Monitorar transporte
          </Link>
        )}
      </div>
    </motion.div>
  );
}
