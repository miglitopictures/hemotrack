import { CheckCircle2, Loader2 } from "lucide-react";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { DetalheTransporte } from "@/components/detalhe-transporte";
import { confirmarRecebimento, obterTransporte } from "@/lib/api";
import type { Transporte } from "@/lib/data";

// o back simula 10 km/min: atualizar a cada 10 s já mostra o veículo andando
const INTERVALO_MS = 10_000;

/**
 * HU08: tela de monitoramento ao vivo. Enquanto o lote está "Em transporte",
 * consulta o back de tempos em tempos (localização, temperatura, alerta).
 * O hospital confirma o recebimento por aqui (cenário 3).
 */
export function MonitorTransporte({
  inicial,
  papel,
}: {
  inicial: Transporte;
  papel: "hospital" | "hemocentro";
}) {
  const [transporte, setTransporte] = useState(inicial);
  const [recebendo, setRecebendo] = useState(false);
  const emTransporte = transporte.status === "Em transporte";

  useEffect(() => {
    if (!emTransporte) return;
    const timer = setInterval(() => {
      obterTransporte(transporte.id)
        .then((atual) => {
          if (atual) setTransporte(atual);
        })
        .catch(() => {
          // falha pontual de rede: mantém a última leitura e tenta no próximo ciclo
        });
    }, INTERVALO_MS);
    return () => clearInterval(timer);
  }, [emTransporte, transporte.id]);

  async function receber() {
    setRecebendo(true);
    try {
      setTransporte(await confirmarRecebimento(transporte.id));
      toast.success("Recebimento confirmado", {
        description: "A requisição passou para Entregue.",
      });
    } catch (e) {
      toast.error("Não foi possível confirmar o recebimento", {
        description: e instanceof Error ? e.message : "Erro inesperado.",
      });
    } finally {
      setRecebendo(false);
    }
  }

  const acoes =
    papel === "hospital" && emTransporte ? (
      <button
        type="button"
        onClick={() => void receber()}
        disabled={recebendo}
        className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-semibold text-primary-foreground shadow-card disabled:cursor-not-allowed disabled:opacity-50"
      >
        {recebendo ? (
          <Loader2 className="size-4 animate-spin" aria-hidden="true" />
        ) : (
          <CheckCircle2 className="size-4" aria-hidden="true" />
        )}
        Confirmar recebimento
      </button>
    ) : null;

  return <DetalheTransporte transporte={transporte} papel={papel} acoes={acoes} />;
}
