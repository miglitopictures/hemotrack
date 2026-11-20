/**
 * Tradutor do estoque (HU02): `HemocomponenteResponse.java` <-> `Bolsa` (formato das telas).
 */

import { armazenamentoPorComponente, type Bolsa, type StatusBolsa } from "../data";
import {
  COMPONENTE_DO_BACK,
  COMPONENTE_PARA_BACK,
  tipoSanguineoDoBack,
  tipoSanguineoParaBack,
  type FatorRhBack,
  type TipoABOBack,
  type TipoHemocomponenteBack,
} from "./requisicao";

/** GET /instituicoes/{id}/estoque */
export type BolsaEstoqueBack = {
  id: number;
  codigoBolsa: string;
  tipo: TipoHemocomponenteBack;
  abo: TipoABOBack;
  rh: FatorRhBack;
  volumeMl: number;
  dataColeta: string;
  dataValidade: string;
  status: "DISPONIVEL" | "RESERVADO" | "DESCARTADO" | "TRANSFUNDIDO";
  situacao: "DISPONIVEL" | "RESERVADA" | "UTILIZADA" | "VENCIDA" | "DESCARTADA";
  diasParaVencer: number;
  emTransito: boolean;
};

/** POST /instituicoes/{id}/estoque */
export type NovaBolsaBack = {
  codigoBolsa: string;
  tipo: TipoHemocomponenteBack;
  abo: TipoABOBack;
  rh: FatorRhBack;
  dataColeta: string;
  dataValidade: string;
  volumeMl?: number;
};

// mesma janela do card "Vencem em até 3 dias" da tela de estoque
const DIAS_VENCE_EM_BREVE = 3;

function statusNaTela(b: BolsaEstoqueBack): StatusBolsa {
  switch (b.situacao) {
    case "DISPONIVEL":
      return b.diasParaVencer <= DIAS_VENCE_EM_BREVE ? "Vence em breve" : "Disponível";
    case "RESERVADA":
      return b.emTransito ? "Enviada" : "Reservada";
    case "UTILIZADA":
      return "Utilizada";
    case "VENCIDA":
      return "Vencida";
    case "DESCARTADA":
      return "Descartada";
  }
}

export function bolsaEstoqueParaTela(b: BolsaEstoqueBack): Bolsa {
  const componente = COMPONENTE_DO_BACK[b.tipo];
  return {
    codigo: b.codigoBolsa,
    componente,
    tipoSanguineo: tipoSanguineoDoBack(b.abo, b.rh),
    coleta: b.dataColeta,
    validade: b.dataValidade,
    status: statusNaTela(b),
    // o back não guarda a câmara; mostramos a de padrão para o componente
    armazenamento: armazenamentoPorComponente[componente] ?? "—",
    dias: b.diasParaVencer,
  };
}

export function novaBolsaParaBack(dados: {
  codigo: string;
  componente: string;
  tipoSanguineo: string;
  coleta: string;
  validade: string;
  volumeMl?: number;
}): NovaBolsaBack {
  const tipo = COMPONENTE_PARA_BACK[dados.componente];
  if (!tipo) {
    throw new Error(`Componente de hemocomponente não reconhecido: "${dados.componente}"`);
  }
  const { abo, rh } = tipoSanguineoParaBack(dados.tipoSanguineo);
  return {
    codigoBolsa: dados.codigo,
    tipo,
    abo,
    rh,
    dataColeta: dados.coleta,
    dataValidade: dados.validade,
    ...(dados.volumeMl !== undefined ? { volumeMl: dados.volumeMl } : {}),
  };
}
