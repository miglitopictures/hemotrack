/**
 * Tradutor entre `Requisicao` (formato que o back Spring devolve, hoje) e
 * `Solicitacao` (formato que as telas do front já conhecem, de `data.ts`).
 *
 * Isso existe porque o front e o back nasceram de HUs, mas cada um modelou
 * os dados do seu jeito. Nenhum dos dois está "errado" — só falam línguas
 * diferentes. Em vez de reescrever as telas do front para entender o back
 * (ou vice-versa), concentramos a tradução aqui, num só lugar.
 */

import type { Prioridade, Solicitacao, Status } from "../data";

// ---------------------------------------------------------------------------
// O que o back REALMENTE devolve hoje (RequisicaoController + Requisicao.java)
// ---------------------------------------------------------------------------

export type TipoHemocomponenteBack = "HEMACIAS" | "PLASMA" | "PLAQUETAS" | "CRIOPRECIPITADO";
export type TipoABOBack = "A" | "B" | "AB" | "O";
export type FatorRhBack = "POSITIVO" | "NEGATIVO";
export type PrioridadeBack = "NORMAL" | "EMERGENCIA" | "URGENCIA";
export type StatusRequisicaoBack =
  | "ABERTA"
  | "ACEITA"
  | "ALOCADA"
  | "ATENDIDA"
  | "RECUSADA"
  | "CANCELADA"
  | "EM_TRANSITO";

export type RequisicaoBack = {
  id: number;
  dataCriacao: string; // Instant do Java, ISO 8601
  hospitalId: number;
  hemocentroId?: number | null; // preenchido quando um hemocentro aceita a requisição
  tipo: TipoHemocomponenteBack;
  abo: TipoABOBack;
  rh: FatorRhBack;
  volumeMl: number;
  prioridade: PrioridadeBack;
  status: StatusRequisicaoBack;
  observacoes?: string | null;
  motivoRecusa?: string | null;
  dataEnvio?: string | null; // HU08: quando o hemocentro despachou
  dataEntrega?: string | null; // HU08: quando o hospital confirmou o recebimento
};

/** O corpo que o `POST /requisicoes` do back espera. */
export type NovaRequisicaoBack = {
  hospitalId: number;
  tipo: TipoHemocomponenteBack;
  abo: TipoABOBack;
  rh: FatorRhBack;
  volumeMl: number;
  prioridade: PrioridadeBack;
  observacoes?: string;
};

// ---------------------------------------------------------------------------
// Tabelas de tradução
// ---------------------------------------------------------------------------
// TODO(back): o ideal é o back devolver texto de exibição pronto (ou o front
// e o back combinarem um enum único). Enquanto isso não acontece, a tradução
// mora aqui.

const COMPONENTE_PARA_BACK: Record<string, TipoHemocomponenteBack> = {
  "Concentrado de hemácias": "HEMACIAS",
  "Plasma fresco congelado": "PLASMA",
  "Concentrado de plaquetas": "PLAQUETAS",
  Crioprecipitado: "CRIOPRECIPITADO",
};
export const COMPONENTE_DO_BACK: Record<TipoHemocomponenteBack, string> = {
  HEMACIAS: "Concentrado de hemácias",
  PLASMA: "Plasma fresco congelado",
  PLAQUETAS: "Concentrado de plaquetas",
  CRIOPRECIPITADO: "Crioprecipitado",
};

// O back mede o pedido em mL (volumeMl); as telas falam em bolsas. A conversão
// usa o volume padrão de uma bolsa de cada componente (o mesmo do seed do back).
export const VOLUME_PADRAO_ML: Record<TipoHemocomponenteBack, number> = {
  HEMACIAS: 300,
  PLASMA: 200,
  PLAQUETAS: 50,
  CRIOPRECIPITADO: 15,
};

function tipoSanguineoParaBack(tipo: string): { abo: TipoABOBack; rh: FatorRhBack } {
  const abo = tipo.replace(/[+-]/, "") as TipoABOBack;
  const rh: FatorRhBack = tipo.endsWith("+") ? "POSITIVO" : "NEGATIVO";
  return { abo, rh };
}
export function tipoSanguineoDoBack(abo: TipoABOBack, rh: FatorRhBack): string {
  return `${abo}${rh === "POSITIVO" ? "+" : "-"}`;
}

const PRIORIDADE_PARA_BACK: Record<Prioridade, PrioridadeBack> = {
  Rotina: "NORMAL",
  Urgente: "URGENCIA",
  Emergência: "EMERGENCIA",
};
const PRIORIDADE_DO_BACK: Record<PrioridadeBack, Prioridade> = {
  NORMAL: "Rotina",
  URGENCIA: "Urgente",
  EMERGENCIA: "Emergência",
};

// O front (HU04) conhece 6 status. ALOCADA (HU06: bolsas já reservadas) é o
// "Em separação" do front: o hemocentro separou as bolsas e falta despachar.
// CANCELADA ainda não tem tela própria e aparece como "Recusada".
export const STATUS_DO_BACK: Record<StatusRequisicaoBack, Status> = {
  ABERTA: "Pendente",
  ACEITA: "Aceita",
  ALOCADA: "Em separação", // HU06: bolsas reservadas, aguardando despacho
  ATENDIDA: "Entregue",
  RECUSADA: "Recusada",
  CANCELADA: "Recusada", // TODO: front ainda não tem um status de cancelamento
  EM_TRANSITO: "Em transporte", // HU08: lote despachado pelo hemocentro
};

// ---------------------------------------------------------------------------
// Tradutores públicos
// ---------------------------------------------------------------------------

// Nomes das instituições aprovadas, vindos de GET /instituicoes. O api.ts
// preenche este mapa antes de traduzir as requisições; se a busca falhar,
// a tela mostra "Hospital #id" / "Hemocentro #id".
const nomesInstituicoes = new Map<number, string>();

export function registrarNomesInstituicoes(lista: { id: number; razaoSocial: string }[]): void {
  for (const instituicao of lista) {
    nomesInstituicoes.set(instituicao.id, instituicao.razaoSocial);
  }
}

export function nomeDoHospital(hospitalId: number): string {
  return nomesInstituicoes.get(hospitalId) ?? `Hospital #${hospitalId}`;
}

export function nomeDoHemocentro(hemocentroId: number): string {
  return nomesInstituicoes.get(hemocentroId) ?? `Hemocentro #${hemocentroId}`;
}

export function requisicaoParaSolicitacao(r: RequisicaoBack): Solicitacao {
  return {
    id: `REQ-${r.id}`,
    hospital: nomeDoHospital(r.hospitalId),
    hemocentro: r.hemocentroId != null ? nomeDoHemocentro(r.hemocentroId) : "",
    componente: COMPONENTE_DO_BACK[r.tipo],
    tipoSanguineo: tipoSanguineoDoBack(r.abo, r.rh),
    quantidade: Math.max(1, Math.ceil(r.volumeMl / VOLUME_PADRAO_ML[r.tipo])), // mL -> bolsas
    prioridade: PRIORIDADE_DO_BACK[r.prioridade],
    status: STATUS_DO_BACK[r.status],
    criadaEm: r.dataCriacao,
    observacoes: r.observacoes ?? "",
    motivoRecusa: r.motivoRecusa ?? null,
  };
}

export function novaSolicitacaoParaRequisicao(
  dados: { componente: string; tipoSanguineo: string; quantidade: number; prioridade: Prioridade; observacoes: string },
  hospitalId: number,
): NovaRequisicaoBack {
  const { abo, rh } = tipoSanguineoParaBack(dados.tipoSanguineo);

  // A tabela COMPONENTE_PARA_BACK só conhece os 4 nomes que estão nela.
  // Se dados.componente vier com um texto diferente (por exemplo, um erro
  // de digitação num formulário, ou um nome novo que a tela passou a usar
  // sem eu saber), isso daria "undefined" e quebraria o pedido pro back
  // silenciosamente. Preferimos avisar logo aqui, com uma mensagem clara.
  const tipo = COMPONENTE_PARA_BACK[dados.componente];
  if (!tipo) {
    throw new Error(`Componente de hemocomponente não reconhecido: "${dados.componente}"`);
  }

  return {
    hospitalId,
    tipo,
    abo,
    rh,
    volumeMl: dados.quantidade * VOLUME_PADRAO_ML[tipo], // bolsas -> mL
    prioridade: PRIORIDADE_PARA_BACK[dados.prioridade],
    observacoes: dados.observacoes,
  };
}