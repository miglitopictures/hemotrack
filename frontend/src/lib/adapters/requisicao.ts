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
import { HOSPITAL_ATUAL } from "../data";

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
  | "CANCELADA";

export type RequisicaoBack = {
  id: number;
  dataCriacao: string; // Instant do Java, ISO 8601
  hospitalId: number;
  tipo: TipoHemocomponenteBack;
  abo: TipoABOBack;
  rh: FatorRhBack;
  volumeMl: number;
  prioridade: PrioridadeBack;
  status: StatusRequisicaoBack;
  observacoes?: string | null;
  motivoRecusa?: string | null;
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
const COMPONENTE_DO_BACK: Record<TipoHemocomponenteBack, string> = {
  HEMACIAS: "Concentrado de hemácias",
  PLASMA: "Plasma fresco congelado",
  PLAQUETAS: "Concentrado de plaquetas",
  CRIOPRECIPITADO: "Crioprecipitado",
};

function tipoSanguineoParaBack(tipo: string): { abo: TipoABOBack; rh: FatorRhBack } {
  const abo = tipo.replace(/[+-]/, "") as TipoABOBack;
  const rh: FatorRhBack = tipo.endsWith("+") ? "POSITIVO" : "NEGATIVO";
  return { abo, rh };
}
function tipoSanguineoDoBack(abo: TipoABOBack, rh: FatorRhBack): string {
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

// O front (HU04) só conhece 6 status. O back já tem um a mais (ALOCADA, do
// HU06) e um status de cancelamento que o front ainda não trata em nenhuma
// tela. Por ora colapsamos ALOCADA em "Aceita" (é o mais próximo do que a
// tela de acompanhamento do hospital sabe exibir) e CANCELADA em "Recusada".
// Quando a tela de estoque/seleção de bolsas existir de verdade, vale criar
// um status "Alocada" próprio no front em vez de esconder essa diferença.
const STATUS_DO_BACK: Record<StatusRequisicaoBack, Status> = {
  ABERTA: "Pendente",
  ACEITA: "Aceita",
  ALOCADA: "Aceita", // TODO: front ainda não distingue "aceita" de "alocada"
  ATENDIDA: "Entregue",
  RECUSADA: "Recusada",
  CANCELADA: "Recusada", // TODO: front ainda não tem um status de cancelamento
};

// ---------------------------------------------------------------------------
// Tradutores públicos
// ---------------------------------------------------------------------------

// TODO(back): quando existir uma rota de instituições (/instituicoes), o
// nome do hospital deve vir de lá, não ser fixado aqui. Enquanto isso não
// existe, mapeamos à mão o único hospital que estamos usando para testar
// (id 1, o mesmo valor de HOSPITAL_ID_TEMP no api.ts) para o nome exato que
// a tela "minhas solicitações" já espera (HOSPITAL_ATUAL, em data.ts). Sem
// isso, o texto não bate e a tela do hospital fica vazia, mesmo a
// solicitação existindo (é exatamente o que estava acontecendo).
const NOME_DO_HOSPITAL: Record<number, string> = {
  1: HOSPITAL_ATUAL,
};

function nomeDoHospital(hospitalId: number): string {
  return NOME_DO_HOSPITAL[hospitalId] ?? `Hospital #${hospitalId}`;
}

export function requisicaoParaSolicitacao(r: RequisicaoBack): Solicitacao {
  return {
    id: `REQ-${r.id}`,
    hospital: nomeDoHospital(r.hospitalId),
    hemocentro: "", // TODO(back): a Requisicao não guarda hemocentroId hoje
    componente: COMPONENTE_DO_BACK[r.tipo],
    tipoSanguineo: tipoSanguineoDoBack(r.abo, r.rh),
    quantidade: r.volumeMl, // aproximação: back mede em mL, front mostra "quantidade"
    prioridade: PRIORIDADE_DO_BACK[r.prioridade],
    status: STATUS_DO_BACK[r.status],
    criadaEm: r.dataCriacao,
    observacoes: r.observacoes ?? "",
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
    volumeMl: dados.quantidade,
    prioridade: PRIORIDADE_PARA_BACK[dados.prioridade],
    observacoes: dados.observacoes,
  };
}