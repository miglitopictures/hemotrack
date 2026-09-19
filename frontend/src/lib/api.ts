/**
 * Camada de acesso a dados.
 *
 * ATUALIZAÇÃO: as funções de Solicitação agora chamam o back de verdade
 * (RequisicaoController, em /requisicoes). Todo o resto (Bolsas, Transportes,
 * Alertas) continua mockado, porque o back ainda não tem essas rotas
 * implementadas — só documentadas em contrato-api.md (ver os "TODO(back)"
 * abaixo, que dizem exatamente o que falta pedir pro Miguel implementar,
 * na ordem sugerida pelas HUs).
 *
 * IMPORTANTE: as regras de negócio validadas aqui no front (compatibilidade
 * ABO/Rh, FEFO, reserva de bolsa, faixa de temperatura) precisam ser validadas
 * de novo no servidor. O front nunca pode ser a única barreira.
 */

import {
  HOSPITAL_ATUAL,
  bolsaPorCodigo,
  bolsas,
  bolsasCompativeis,
  buscar,
  calcularAlertas,
  estoquePorTipo,
  solicitacaoPorId,
  solicitacoes,
  transporteDaSolicitacao,
  transportePorId,
  transportes,
  type Alerta,
  type Bolsa,
  type Prioridade,
  type ResultadoBusca,
  type Solicitacao,
  type Transporte,
} from "./data";
import { httpGet, httpPost, httpPut, ErroDeApiHttp } from "./httpClient";
import {
  novaSolicitacaoParaRequisicao,
  requisicaoParaSolicitacao,
  type RequisicaoBack,
} from "./adapters/requisicao";

/** Latência simulada (ms), só usada pelas partes ainda mockadas. */
const LATENCIA_MS = 0;

function responder<T>(valor: T): Promise<T> {
  if (LATENCIA_MS <= 0) return Promise.resolve(valor);
  return new Promise((resolve) => setTimeout(() => resolve(valor), LATENCIA_MS));
}

// Mantemos o mesmo nome/formato de erro que o resto do front já espera,
// só que agora ele pode nascer tanto de uma validação local quanto de uma
// resposta HTTP real (ErroDeApiHttp).
export class ErroDeApi extends Error {
  readonly status: number;
  constructor(mensagem: string, status = 500) {
    super(mensagem);
    this.name = "ErroDeApi";
    this.status = status;
  }
}

function paraErroDeApi(erro: unknown): ErroDeApi {
  if (erro instanceof ErroDeApiHttp) return new ErroDeApi(erro.message, erro.status);
  if (erro instanceof Error) return new ErroDeApi(erro.message);
  return new ErroDeApi("Erro desconhecido ao falar com o servidor.");
}

// TODO(back): não existe /auth nem /instituicoes ainda, então não há como
// descobrir o hospitalId de verdade a partir de um login. Fixamos um valor
// só para o front conseguir criar requisições de teste; troque isso assim
// que HU01 (login) estiver de pé.
const HOSPITAL_ID_TEMP = 1;

// ---------------------------------------------------------------------------
// Solicitações — ligado ao back real (/requisicoes)
// ---------------------------------------------------------------------------

export async function listarSolicitacoes(filtros?: { hospital?: string }): Promise<Solicitacao[]> {
  try {
    const dados = await httpGet<RequisicaoBack[]>("/requisicoes");
    const lista = dados.map(requisicaoParaSolicitacao);
    // TODO(back): filtro por hospital deveria ser feito no servidor
    // (?hospitalId=), como já é em contrato-api.md. Por enquanto filtramos
    // aqui porque a rota real ainda não aceita esse query param.
    return filtros?.hospital ? lista.filter((s) => s.hospital === filtros.hospital) : lista;
  } catch (erro) {
    throw paraErroDeApi(erro);
  }
}

export function listarMinhasSolicitacoes(): Promise<Solicitacao[]> {
  return listarSolicitacoes({ hospital: HOSPITAL_ATUAL });
}

export async function obterSolicitacao(id: string): Promise<Solicitacao | undefined> {
  const idBack = id.replace(/^REQ-/, "");
  try {
    const dado = await httpGet<RequisicaoBack>(`/requisicoes/${idBack}`);
    return requisicaoParaSolicitacao(dado);
  } catch (erro) {
    if (erro instanceof ErroDeApiHttp && erro.status === 404) return undefined;
    throw paraErroDeApi(erro);
  }
}

export type NovaSolicitacao = {
  componente: string;
  tipoSanguineo: string;
  quantidade: number;
  prioridade: Prioridade;
  observacoes: string;
};

export async function criarSolicitacao(dados: NovaSolicitacao): Promise<Solicitacao> {
  if (dados.quantidade < 1) {
    throw new ErroDeApi("A quantidade precisa ser de ao menos 1 bolsa.", 400);
  }
  try {
    const corpo = novaSolicitacaoParaRequisicao(dados, HOSPITAL_ID_TEMP);
    const criada = await httpPost<RequisicaoBack>("/requisicoes", corpo);
    return requisicaoParaSolicitacao(criada);
  } catch (erro) {
    throw paraErroDeApi(erro);
  }
}

// TODO(back): a rota real de aceitar (`POST /requisicoes/{id}/aceitar`) não
// recebe bolsas no corpo — ela só muda o status para ACEITA. A seleção de
// bolsas compatíveis (HU06, `/alocar` em contrato-api.md) ainda não foi
// implementada em código nenhum. Por isso a assinatura desta função muda:
// ela aceita a requisição no back, mas a escolha de bolsas continua só
// visual, com os mocks de `data.ts`, até o Miguel implementar `/alocar`.
export async function aceitarSolicitacao(id: string, codigosBolsas: string[]): Promise<Solicitacao> {
  const sol = solicitacaoPorId(id);
  if (sol && codigosBolsas.length !== sol.quantidade) {
    throw new ErroDeApi(`Selecione exatamente ${sol.quantidade} bolsa(s) antes de aceitar.`, 400);
  }
  const idBack = id.replace(/^REQ-/, "");
  try {
    const atualizada = await httpPost<RequisicaoBack>(`/requisicoes/${idBack}/aceitar`);
    return requisicaoParaSolicitacao(atualizada);
  } catch (erro) {
    throw paraErroDeApi(erro);
  }
}

export async function recusarSolicitacao(id: string, motivo: string): Promise<Solicitacao> {
  if (!motivo.trim()) {
    throw new ErroDeApi("Informe o motivo da recusa.", 400);
  }
  const idBack = id.replace(/^REQ-/, "");
  try {
    const atualizada = await httpPost<RequisicaoBack>(`/requisicoes/${idBack}/recusar`, {
      motivoRecusa: motivo,
    });
    return requisicaoParaSolicitacao(atualizada);
  } catch (erro) {
    throw paraErroDeApi(erro);
  }
}

// ---------------------------------------------------------------------------
// Estoque — TODO(back): implementar família `/instituicoes/{id}/estoque`
// (HU02). Hoje só existe em contrato-api.md, nenhum controller/service/model
// de Hemocomponente foi ligado a um endpoint ainda (o model Java existe,
// mas não tem controller). Continua 100% mockado até isso existir.
// ---------------------------------------------------------------------------

export function listarBolsas(filtros?: { componente?: string }): Promise<Bolsa[]> {
  const componente = filtros?.componente;
  const lista = componente ? bolsas.filter((b) => b.componente === componente) : bolsas;
  return responder([...lista]);
}

export function listarBolsasCompativeis(componente: string, receptor: string): Promise<Bolsa[]> {
  return responder(bolsasCompativeis(componente, receptor));
}

export function obterEstoquePorTipo(): Promise<typeof estoquePorTipo> {
  return responder(estoquePorTipo);
}

export type NovaBolsa = {
  codigo: string;
  componente: string;
  tipoSanguineo: string;
  coleta: string;
  validade: string;
  armazenamento: string;
};

export function cadastrarBolsa(dados: NovaBolsa): Promise<Bolsa> {
  if (!dados.codigo.trim()) {
    return Promise.reject(new ErroDeApi("Informe o código da bolsa.", 400));
  }
  if (bolsaPorCodigo(dados.codigo)) {
    return Promise.reject(new ErroDeApi(`Já existe uma bolsa com o código ${dados.codigo}.`, 409));
  }
  return responder({ ...dados, status: "Disponível" as const });
}

// ---------------------------------------------------------------------------
// Transportes e distribuição — TODO(back): HU07 (rota/grafo) e HU08
// (telemetria simulada) não têm NENHUMA rota, nem no documento nem em
// código. É provavelmente a próxima coisa a alinhar com o Miguel, já que
// hoje não dá pra ligar nem a versão mockada nem uma real. Continua mockado.
// ---------------------------------------------------------------------------

export function listarTransportes(filtros?: { destino?: string }): Promise<Transporte[]> {
  const destino = filtros?.destino;
  const lista = destino ? transportes.filter((t) => t.destino.startsWith(destino)) : transportes;
  return responder([...lista]);
}

export function listarMeusTransportes(): Promise<Transporte[]> {
  return listarTransportes({ destino: HOSPITAL_ATUAL });
}

export function obterTransporte(id: string): Promise<Transporte | undefined> {
  return responder(transportePorId(id));
}

export function obterTransporteDaSolicitacao(id: string): Promise<Transporte | undefined> {
  return responder(transporteDaSolicitacao(id));
}

export function despacharRemessa(dados: { solicitacao: string; veiculo: string }): Promise<Transporte> {
  const existente = transporteDaSolicitacao(dados.solicitacao);
  if (existente) return responder({ ...existente, veiculo: dados.veiculo });
  return Promise.reject(new ErroDeApi("Não há remessa vinculada a esta solicitação.", 404));
}

// ---------------------------------------------------------------------------
// Diversos — TODO(back): HU09 (`/indicadores`) também não existe ainda.
// ---------------------------------------------------------------------------

export function obterAlertas(): Promise<Alerta[]> {
  return responder(calcularAlertas());
}

export function buscarGlobal(termo: string): Promise<ResultadoBusca[]> {
  return responder(buscar(termo));
}