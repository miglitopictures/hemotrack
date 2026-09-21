import {
  CartesianGrid,
  Line,
  LineChart,
  ReferenceArea,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { AlertTriangle, ThermometerSnowflake } from "lucide-react";
import { eixoTema, tooltipTema } from "@/components/ui-kit";
import {
  formatarTemperatura,
  limitesTemperatura,
  temperaturaFora,
  type LeituraTemp,
} from "@/lib/data";

/**
 * Curva de temperatura da carga com a faixa aceitável sombreada.
 * Os pontos fora do limite aparecem em destaque — é o que transforma o
 * "monitoramento de cadeia de frio" em algo verificável.
 */
export function TemperaturaGrafico({
  componente,
  leituras,
}: {
  componente: string;
  leituras: LeituraTemp[];
}) {
  const limite = limitesTemperatura[componente];

  if (leituras.length === 0) {
    return (
      <p className="px-5 py-10 text-center text-sm text-muted-foreground">
        Sem leituras de temperatura para esta remessa.
      </p>
    );
  }

  const valores = leituras.map((l) => l.valor);
  const fora = leituras.filter((l) => temperaturaFora(componente, l.valor));
  const min = Math.min(...valores, limite ? limite.min : Infinity);
  const max = Math.max(...valores, limite ? limite.max : -Infinity);
  const folga = Math.max(1, (max - min) * 0.25);

  const resumo = limite
    ? `Faixa aceitável de ${limite.min} °C a ${limite.max} °C. ${leituras.length} leituras, ${fora.length} fora da faixa. Última leitura: ${formatarTemperatura(valores[valores.length - 1] ?? null)}.`
    : `${leituras.length} leituras de temperatura.`;

  return (
    <div>
      <div
        className="h-56 w-full px-3 py-4"
        role="img"
        aria-label={`Gráfico de temperatura da carga. ${resumo}`}
      >
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={leituras} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
            <CartesianGrid vertical={false} stroke="var(--border)" />
            <XAxis dataKey="hora" {...eixoTema} />
            <YAxis
              {...eixoTema}
              width={46}
              domain={[Number((min - folga).toFixed(1)), Number((max + folga).toFixed(1))]}
              tickFormatter={(v: number) => `${v}°`}
            />
            {limite ? (
              <ReferenceArea
                y1={limite.min}
                y2={limite.max}
                fill="var(--success)"
                fillOpacity={0.12}
                stroke="var(--success)"
                strokeOpacity={0.25}
              />
            ) : null}
            <Tooltip
              {...tooltipTema}
              formatter={(valor) => [formatarTemperatura(Number(valor)), "Temperatura"]}
            />
            <Line
              type="monotone"
              dataKey="valor"
              stroke={fora.length > 0 ? "var(--warning)" : "var(--teal)"}
              strokeWidth={2.5}
              dot={(props) => {
                const { cx, cy, payload, index } = props as {
                  cx: number;
                  cy: number;
                  index: number;
                  payload: LeituraTemp;
                };
                const ruim = temperaturaFora(componente, payload.valor);
                return (
                  <circle
                    key={`ponto-${index}`}
                    cx={cx}
                    cy={cy}
                    r={ruim ? 5 : 3}
                    fill={ruim ? "var(--warning)" : "var(--teal)"}
                    stroke="var(--card)"
                    strokeWidth={2}
                  />
                );
              }}
              activeDot={{ r: 6 }}
              isAnimationActive={false}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>

      <div className="flex flex-wrap items-center gap-3 border-t border-border px-5 py-3 text-xs">
        {limite ? (
          <span className="inline-flex items-center gap-1.5 text-muted-foreground">
            <ThermometerSnowflake className="size-3.5 text-success" aria-hidden="true" />
            Faixa aceitável: {limite.min} °C a {limite.max} °C
          </span>
        ) : null}
        {fora.length > 0 ? (
          <span className="inline-flex items-center gap-1.5 font-semibold text-warning">
            <AlertTriangle className="size-3.5" aria-hidden="true" />
            {fora.length} leitura(s) fora da faixa
          </span>
        ) : (
          <span className="font-semibold text-success">Cadeia de frio íntegra</span>
        )}
      </div>
    </div>
  );
}
