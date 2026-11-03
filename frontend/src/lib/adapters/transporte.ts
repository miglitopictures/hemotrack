/**
 * Tradutor do transporte (HU07 + HU08): requisição + bolsas + rota/monitoramento
 * do back viram um `Transporte`, o formato que os cards, o mapa e o gráfico de
 * temperatura do front já usavam com os mocks.
 */

import type { LeituraTemp, PontoRota, Transporte } from "../data";
import {
  COMPONENTE_DO_BACK,
  STATUS_DO_BACK,
  nomeDoHemocentro,
  nomeDoHospital,
  tipoSanguineoDoBack,
  type FatorRhBack,
  type RequisicaoBack,
  type TipoABOBack,
} from "./requisicao";

// ---------------------------------------------------------------------------
// O que o back devolve
// ---------------------------------------------------------------------------

type PontoBack = { instituicaoId: number; instituicao: string; municipio: string };

/** GET /requisicoes/{id}/rota (RotaResponse.java) */
export type RotaBack = {
  requisicaoId: number;
  origem: PontoBack;
  destino: PontoBack;
  locais: string[];
  kmAcumulado: number[];
  distanciaKm: number;
};

/** GET /requisicoes/{id}/monitoramento (MonitoramentoResponse.java) */
export type MonitoramentoBack = {
  requisicaoId: number;
  origem: string;
  destino: string;
  locais: string[];
  kmAcumulado: number[];
  localAtual: string;
  proximoLocal: string | null;
  progresso: number;
  kmPercorridos: number;
  distanciaKm: number;
  temperaturaC: number;
  faixa: { minC: number | null; maxC: number };
  alertaTemperatura: boolean;
  historico: { instante: string; temperaturaC: number }[];
  atualizadoEm: string;
};

/** GET /requisicoes/{id}/bolsas (Hemocomponente.java) */
export type BolsaBack = {
  id: number;
  codigoBolsa: string;
  abo: TipoABOBack;
  rh: FatorRhBack;
  dataValidade: string; // LocalDate, "2026-11-30"
};

// ---------------------------------------------------------------------------
// Tradução
// ---------------------------------------------------------------------------

const VEICULO_SIMULADO = "Veículo refrigerado (simulado)";

function horaMinuto(iso: string): string {
  return new Date(iso).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
}

/** Pontos do mapa a partir dos municípios da rota e da distância até cada um. */
export function pontosDaRota(
  locais: string[],
  kmAcumulado: number[],
  nomeOrigem: string,
  nomeDestino: string,
): PontoRota[] {
  const pontos: PontoRota[] = [];
  for (let i = 0; i < locais.length; i++) {
    let detalhe = "Passagem";
    if (i === 0) detalhe = `Saída · ${nomeOrigem}`;
    else if (i === locais.length - 1) detalhe = `Entrega · ${nomeDestino}`;
    pontos.push({ ponto: locais[i] ?? "", detalhe, km: kmAcumulado[i] ?? 0 });
  }
  return pontos;
}

/**
 * Previsão de chegada pela velocidade média desde o envio (o back simula a
 * viagem a velocidade constante, então a média já é a velocidade da simulação).
 */
function previsaoDeChegada(r: RequisicaoBack, m: MonitoramentoBack): string {
  if (m.proximoLocal === null) return "no destino";
  if (!r.dataEnvio || m.kmPercorridos <= 0) return "calculando";
  const decorridoMs = new Date(m.atualizadoEm).getTime() - new Date(r.dataEnvio).getTime();
  const kmPorMs = m.kmPercorridos / decorridoMs;
  const faltaMs = (m.distanciaKm - m.kmPercorridos) / kmPorMs;
  return horaMinuto(new Date(new Date(m.atualizadoEm).getTime() + faltaMs).toISOString());
}

function diasAte(dataIso: string): number {
  const hoje = new Date();
  hoje.setHours(0, 0, 0, 0);
  const validade = new Date(`${dataIso}T00:00:00`);
  return Math.round((validade.getTime() - hoje.getTime()) / 86_400_000);
}

export function requisicaoParaTransporte(
  r: RequisicaoBack,
  bolsas: BolsaBack[],
  monitoramento: MonitoramentoBack | null,
  rota: RotaBack | null,
): Transporte {
  const id = `REQ-${r.id}`;
  const status = STATUS_DO_BACK[r.status];
  const hemocentro = r.hemocentroId != null ? nomeDoHemocentro(r.hemocentroId) : "—";
  const hospital = nomeDoHospital(r.hospitalId);

  // município de cada ponta, quando o back mandou a rota ou o monitoramento
  const municipioOrigem = monitoramento?.origem ?? rota?.origem.municipio;
  const municipioDestino = monitoramento?.destino ?? rota?.destino.municipio;

  let progresso = 0;
  let localizacao = "No hemocentro, aguardando despacho";
  let eta = "aguardando despacho";
  const observacoes: string[] = [];

  if (monitoramento) {
    progresso = monitoramento.progresso;
    localizacao = monitoramento.proximoLocal
      ? `${monitoramento.localAtual} → ${monitoramento.proximoLocal}`
      : `${monitoramento.localAtual} · aguardando recebimento`;
    eta = previsaoDeChegada(r, monitoramento);
    if (monitoramento.proximoLocal === null) {
      observacoes.push("Carga no destino. Falta o hospital confirmar o recebimento.");
    }
  } else if (status === "Entregue") {
    progresso = 100;
    localizacao = "Entregue ao hospital";
    eta = r.dataEntrega ? `entregue às ${horaMinuto(r.dataEntrega)}` : "entregue";
  }

  let pontos: PontoRota[] | undefined;
  if (monitoramento) {
    pontos = pontosDaRota(monitoramento.locais, monitoramento.kmAcumulado, hemocentro, hospital);
  } else if (rota) {
    pontos = pontosDaRota(rota.locais, rota.kmAcumulado, hemocentro, hospital);
  }

  const leituras: LeituraTemp[] = [];
  for (const h of monitoramento?.historico ?? []) {
    leituras.push({ hora: horaMinuto(h.instante), valor: h.temperaturaC });
  }

  const bolsasDetalhe = [];
  for (const b of bolsas) {
    bolsasDetalhe.push({
      codigo: b.codigoBolsa,
      tipoSanguineo: tipoSanguineoDoBack(b.abo, b.rh),
      validade: b.dataValidade,
      diasParaVencer: diasAte(b.dataValidade),
    });
  }

  return {
    id,
    solicitacao: id,
    origem: municipioOrigem ? `${hemocentro} · ${municipioOrigem}` : hemocentro,
    destino: municipioDestino ? `${hospital} · ${municipioDestino}` : hospital,
    bolsas: bolsasDetalhe.map((b) => b.codigo),
    status,
    progresso,
    eta,
    veiculo: VEICULO_SIMULADO,
    localizacao,
    observacoes,
    hospitalId: r.hospitalId,
    ...(r.hemocentroId != null ? { hemocentroId: r.hemocentroId } : {}),
    componente: COMPONENTE_DO_BACK[r.tipo],
    temperatura: monitoramento ? monitoramento.temperaturaC : null,
    alertaTemperatura: monitoramento?.alertaTemperatura ?? false,
    leituras,
    ...(pontos ? { pontos } : {}),
    bolsasDetalhe,
  };
}
