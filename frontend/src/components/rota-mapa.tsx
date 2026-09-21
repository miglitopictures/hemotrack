import { motion, useReducedMotion } from "motion/react";
import { MapPin, Truck } from "lucide-react";
import { cn } from "@/lib/utils";

type Ponto = { ponto: string; detalhe: string; km: number };

const P0 = { x: 46, y: 172 };
const C1 = { x: 190, y: 36 };
const C2 = { x: 400, y: 214 };
const P3 = { x: 554, y: 62 };

function bezier(t: number) {
  const u = 1 - t;
  return {
    x: u * u * u * P0.x + 3 * u * u * t * C1.x + 3 * u * t * t * C2.x + t * t * t * P3.x,
    y: u * u * u * P0.y + 3 * u * u * t * C1.y + 3 * u * t * t * C2.y + t * t * t * P3.y,
  };
}

/** de Casteljau: devolve o trecho da curva entre 0 e t (parte já percorrida). */
function trechoPercorrido(t: number): string {
  const lerp = (a: { x: number; y: number }, b: { x: number; y: number }, k: number) => ({
    x: a.x + (b.x - a.x) * k,
    y: a.y + (b.y - a.y) * k,
  });

  const a = lerp(P0, C1, t);
  const b = lerp(C1, C2, t);
  const c = lerp(C2, P3, t);
  const d = lerp(a, b, t);
  const e = lerp(b, c, t);
  const f = lerp(d, e, t);

  return `M ${P0.x} ${P0.y} C ${a.x} ${a.y}, ${d.x} ${d.y}, ${f.x} ${f.y}`;
}

const caminhoCompleto = `M ${P0.x} ${P0.y} C ${C1.x} ${C1.y}, ${C2.x} ${C2.y}, ${P3.x} ${P3.y}`;

export function RotaMapa({
  pontos,
  progresso,
  localizacao,
  alerta = false,
  className,
}: {
  pontos: Ponto[];
  progresso: number;
  localizacao: string;
  alerta?: boolean;
  className?: string;
}) {
  const reduzido = useReducedMotion();
  const t = Math.min(1, Math.max(0, progresso / 100));
  const posicao = bezier(t);
  const totalKm = pontos.length > 0 ? (pontos[pontos.length - 1]?.km ?? 1) : 1;

  return (
    <div className={cn("relative", className)}>
      <svg
        viewBox="0 0 600 240"
        className="surface-grid h-56 w-full bg-secondary/40"
        role="img"
        aria-label={`Rota do transporte, ${progresso}% percorrido. Posição atual: ${localizacao}.`}
      >
        {/* trajeto completo */}
        <path
          d={caminhoCompleto}
          fill="none"
          stroke="var(--border)"
          strokeWidth={6}
          strokeLinecap="round"
          strokeDasharray="1 10"
        />
        {/* trecho já percorrido */}
        <motion.path
          d={trechoPercorrido(t)}
          fill="none"
          stroke={alerta ? "var(--warning)" : "var(--primary)"}
          strokeWidth={5}
          strokeLinecap="round"
          initial={reduzido ? false : { pathLength: 0, opacity: 0 }}
          animate={{ pathLength: 1, opacity: 1 }}
          transition={{ duration: 1.1, ease: [0.16, 1, 0.3, 1] }}
        />

        {/* pontos de passagem */}
        {pontos.map((p, i) => {
          const tp = totalKm > 0 ? p.km / totalKm : 0;
          const pos = bezier(tp);
          const inicio = i === 0;
          const fim = i === pontos.length - 1;
          return (
            <g key={p.ponto}>
              <circle
                cx={pos.x}
                cy={pos.y}
                r={inicio || fim ? 7 : 4.5}
                fill="var(--card)"
                stroke={inicio ? "var(--teal)" : fim ? "var(--primary)" : "var(--border)"}
                strokeWidth={3}
              />
              {inicio || fim ? (
                <text
                  x={pos.x}
                  y={pos.y - 16}
                  textAnchor="middle"
                  className="fill-[var(--muted-foreground)] text-[11px] font-semibold"
                >
                  {inicio ? "Origem" : "Destino"}
                </text>
              ) : null}
            </g>
          );
        })}

        {/* veículo */}
        <motion.g
          initial={reduzido ? false : { x: bezier(0).x - 18, y: bezier(0).y - 18 }}
          animate={{ x: posicao.x - 18, y: posicao.y - 18 }}
          transition={{ duration: 1.2, ease: [0.16, 1, 0.3, 1] }}
        >
          <rect
            width={36}
            height={36}
            rx={12}
            fill={alerta ? "var(--warning)" : "var(--primary)"}
            className="drop-shadow"
          />
          <g transform="translate(8 8)" stroke="white" strokeWidth={1.8} fill="none">
            <path d="M1 3h11v9H1z" />
            <path d="M12 6h4l3 3v3h-7z" />
            <circle cx="5" cy="15" r="2" />
            <circle cx="15" cy="15" r="2" />
          </g>
        </motion.g>
      </svg>

      <div className="absolute bottom-3 left-3 flex items-center gap-2 rounded-xl border border-border bg-card/90 px-3 py-2 text-xs font-medium backdrop-blur">
        <MapPin className="size-3.5 text-primary" aria-hidden="true" />
        {localizacao}
      </div>
      <div className="absolute right-3 top-3 flex items-center gap-1.5 rounded-xl border border-border bg-card/90 px-3 py-1.5 text-xs font-semibold backdrop-blur">
        <Truck className="size-3.5 text-primary" aria-hidden="true" />
        {progresso}% do trajeto
      </div>
    </div>
  );
}
