/**
 * Tradutor do painel de indicadores (HU09): `GET /indicadores` (IndicadoresResponse.java)
 * para o formato que a tela `hemocentro/indicadores` já usava com os mocks de `data.ts`.
 */

import { minimoPorTipo } from "../data";
import type { TipoHemocomponenteBack } from "./requisicao";

// ---------------------------------------------------------------------------
// O que o back devolve
// ---------------------------------------------------------------------------

export type IndicadoresBack = {
  bolsasDisponiveis: number;
  bolsasVencendo: number;
  diasParaVencimento: number;
  requisicoesPendentes: number;
  requisicoesEmTransporte: number;
  requisicoesEntregues: number;
  demandaPorComponente: {
    componente: TipoHemocomponenteBack;
    pedidos: number;
    atendidos: number;
  }[];
  estoquePorTipoSanguineo: { tipo: string; bolsas: number }[];
  geradoEm: string; // Instant do Java, ISO 8601
};

// ---------------------------------------------------------------------------
// O que a tela usa
// ---------------------------------------------------------------------------

export type Indicadores = {
  totalDisponivel: number;
  vencendo: number;
  diasParaVencimento: number;
  pendentes: number;
  emTransporte: number;
  entregues: number;
  demandaPorComponente: { componente: string; pedidos: number; atendidos: number }[];
  estoquePorTipo: { tipo: string; bolsas: number; minimo: number }[];
  geradoEm: Date;
};

// rótulos curtos: cabem no eixo do gráfico
const COMPONENTE_CURTO: Record<TipoHemocomponenteBack, string> = {
  HEMACIAS: "Hemácias",
  PLASMA: "Plasma",
  PLAQUETAS: "Plaquetas",
  CRIOPRECIPITADO: "Crio",
};

export function indicadoresParaTela(back: IndicadoresBack): Indicadores {
  const demanda = [];
  for (const d of back.demandaPorComponente) {
    demanda.push({
      componente: COMPONENTE_CURTO[d.componente],
      pedidos: d.pedidos,
      atendidos: d.atendidos,
    });
  }

  // o mínimo por tipo é regra de exibição do front (barra laranja), não vem do back
  const estoque = [];
  for (const e of back.estoquePorTipoSanguineo) {
    estoque.push({ tipo: e.tipo, bolsas: e.bolsas, minimo: minimoPorTipo[e.tipo] ?? 0 });
  }

  return {
    totalDisponivel: back.bolsasDisponiveis,
    vencendo: back.bolsasVencendo,
    diasParaVencimento: back.diasParaVencimento,
    pendentes: back.requisicoesPendentes,
    emTransporte: back.requisicoesEmTransporte,
    entregues: back.requisicoesEntregues,
    demandaPorComponente: demanda,
    estoquePorTipo: estoque,
    geradoEm: new Date(back.geradoEm),
  };
}
