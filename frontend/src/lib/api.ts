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
  type Alerta,
  type Bolsa,
  type Prioridade,
  type ResultadoBusca,
  type PontoRota,
  type Solicitacao,
  type Transporte,
} from "./data";
import { httpGet, httpPatch, httpPost, httpPut, ErroDeApiHttp } from "./httpClient";
import {
  novaSolicitacaoParaRequisicao,
  registrarNomesInstituicoes,
  requisicaoParaSolicitacao,
  type RequisicaoBack,
} from "./adapters/requisicao";
import {
  indicadoresParaTela,
  type Indicadores,
  type IndicadoresBack,
} from "./adapters/indicadores";
import {
  pontosDaRota,
  requisicaoParaTransporte,
  type BolsaBack,
  type MonitoramentoBack,
  type RotaBack,
} from "./adapters/transporte";
import { listarInstituicoes } from "./api-auth";
import { lerClaims, lerToken } from "./sessao";
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

// A instituição vem do token de quem está logado (HU01).
function instituicaoLogada(): number {
  const token = lerToken();
  const claims = token ? lerClaims(token) : null;
  if (claims === null || claims.instituicaoId == null) {
    throw new ErroDeApi("Sessão sem instituição. Entre novamente.", 401);
  }
  return claims.instituicaoId;
}

// Busca os nomes das instituições aprovadas para o adapter mostrar
// "Hospital Santa Clara" em vez de "Hospital #3". Se falhar, a lista
// continua aparecendo, só que com o id no lugar do nome.
async function carregarNomesInstituicoes(): Promise<void> {
  try {
    registrarNomesInstituicoes(await listarInstituicoes("APROVADA"));
  } catch {
    // sem os nomes, o adapter usa o "Hospital #id"
  }
}

// ---------------------------------------------------------------------------
// Solicitações — ligado ao back real (/requisicoes)
// ---------------------------------------------------------------------------

export async function listarSolicitacoes(filtros?: { hospital?: string }): Promise<Solicitacao[]> {
  try {
    await carregarNomesInstituicoes();
    const query = filtros?.hospital ? `?hospitalId=${instituicaoLogada()}` : "";
    const dados = await httpGet<RequisicaoBack[]>(`/requisicoes${query}`);
    return dados.map(requisicaoParaSolicitacao);
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
    await carregarNomesInstituicoes();
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
    const corpo = novaSolicitacaoParaRequisicao(dados, instituicaoLogada());
    const criada = await httpPost<RequisicaoBack>("/requisicoes", corpo);
    return requisicaoParaSolicitacao(criada);
  } catch (erro) {
    throw paraErroDeApi(erro);
  }
}

// TODO(back): a rota real de aceitar (`POST /requisicoes/{id}/aceitar`) não
// recebe bolsas no corpo — ela só muda o status para ACEITA e agora também
// grava o hemocentroId (via query param, enquanto não existe login pra
// descobrir isso sozinho). A seleção de bolsas compatíveis (HU06, `/alocar`
// em contrato-api.md) ainda não foi implementada em código nenhum. Por isso
// a assinatura desta função muda: ela aceita a requisição no back, mas a
// escolha de bolsas continua só visual, com os mocks de `data.ts`, até o
// Miguel implementar `/alocar`.
export async function aceitarSolicitacao(id: string, codigosBolsas: string[]): Promise<Solicitacao> {
  const sol = solicitacaoPorId(id);
  if (sol && codigosBolsas.length !== sol.quantidade) {
    throw new ErroDeApi(`Selecione exatamente ${sol.quantidade} bolsa(s) antes de aceitar.`, 400);
  }
  const idBack = id.replace(/^REQ-/, "");
  try {
    const atualizada = await httpPost<RequisicaoBack>(
      `/requisicoes/${idBack}/aceitar?hemocentroId=${instituicaoLogada()}`,
    );
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
// Transportes e distribuição — ligado ao back (HU07 rota, HU08 monitoramento)
// Um "transporte" é uma requisição a partir de ALOCADA (bolsas separadas):
// ALOCADA = "Em separação", EM_TRANSITO = "Em transporte", ATENDIDA = "Entregue".
// ---------------------------------------------------------------------------

const STATUS_DE_TRANSPORTE = new Set(["ALOCADA", "EM_TRANSITO", "ATENDIDA"]);

// ordem na tela: o que está na estrada primeiro, depois o que falta despachar
const ORDEM_NA_TELA: Record<string, number> = { "Em transporte": 0, "Em separação": 1, Entregue: 2 };

async function montarTransporte(r: RequisicaoBack): Promise<Transporte> {
  // rota e monitoramento podem falhar (ex.: município fora do mapa, 422);
  // nesse caso o card aparece sem mapa em vez de quebrar a tela inteira.
  const [bolsasDoLote, monitoramento, rota] = await Promise.all([
    httpGet<BolsaBack[]>(`/requisicoes/${r.id}/bolsas`),
    r.status === "EM_TRANSITO"
      ? httpGet<MonitoramentoBack>(`/requisicoes/${r.id}/monitoramento`).catch(() => null)
      : Promise.resolve(null),
    r.status === "ALOCADA"
      ? httpGet<RotaBack>(`/requisicoes/${r.id}/rota`).catch(() => null)
      : Promise.resolve(null),
  ]);
  return requisicaoParaTransporte(r, bolsasDoLote, monitoramento, rota);
}

/** Transportes em que a instituição logada é a origem (hemocentro) ou o destino (hospital). */
export async function listarTransportes(): Promise<Transporte[]> {
  try {
    await carregarNomesInstituicoes();
    const minha = instituicaoLogada();
    const requisicoes = await httpGet<RequisicaoBack[]>("/requisicoes");

    const doTransporte: RequisicaoBack[] = [];
    for (const r of requisicoes) {
      const envolvida = r.hospitalId === minha || r.hemocentroId === minha;
      if (envolvida && STATUS_DE_TRANSPORTE.has(r.status)) {
        doTransporte.push(r);
      }
    }

    const lista = await Promise.all(doTransporte.map(montarTransporte));
    return lista.sort((a, b) => (ORDEM_NA_TELA[a.status] ?? 9) - (ORDEM_NA_TELA[b.status] ?? 9));
  } catch (erro) {
    throw paraErroDeApi(erro);
  }
}

export function listarMeusTransportes(): Promise<Transporte[]> {
  return listarTransportes();
}

/** Transporte de uma requisição; `undefined` se não existe ou ainda não chegou à separação. */
export async function obterTransporte(id: string): Promise<Transporte | undefined> {
  const idBack = id.replace(/^REQ-/, "");
  try {
    await carregarNomesInstituicoes();
    const r = await httpGet<RequisicaoBack>(`/requisicoes/${idBack}`);
    if (!STATUS_DE_TRANSPORTE.has(r.status)) return undefined;
    return await montarTransporte(r);
  } catch (erro) {
    if (erro instanceof ErroDeApiHttp && erro.status === 404) return undefined;
    throw paraErroDeApi(erro);
  }
}

export function obterTransporteDaSolicitacao(id: string): Promise<Transporte | undefined> {
  return obterTransporte(id);
}

/** O transporte é da instituição logada (origem ou destino)? */
export function transporteDaMinhaInstituicao(t: Transporte): boolean {
  const minha = instituicaoLogada();
  return t.hospitalId === minha || t.hemocentroId === minha;
}

/** HU07: rota planejada (só para requisição com bolsas separadas, senão `null`). */
export async function obterRotaDaSolicitacao(
  id: string,
): Promise<{ pontos: PontoRota[]; distanciaKm: number } | null> {
  const idBack = id.replace(/^REQ-/, "");
  try {
    await carregarNomesInstituicoes();
    const rota = await httpGet<RotaBack>(`/requisicoes/${idBack}/rota`);
    return {
      pontos: pontosDaRota(rota.locais, rota.kmAcumulado, rota.origem.instituicao, rota.destino.instituicao),
      distanciaKm: rota.distanciaKm,
    };
  } catch (erro) {
    // 409: ainda não está ALOCADA; 422: município fora do mapa
    if (erro instanceof ErroDeApiHttp && (erro.status === 409 || erro.status === 422)) return null;
    throw paraErroDeApi(erro);
  }
}

/** HU08: o hemocentro despacha o lote (ALOCADA -> EM_TRANSITO). O veículo ainda não é gravado no back. */
export async function despacharRemessa(dados: { solicitacao: string; veiculo: string }): Promise<Transporte> {
  const idBack = dados.solicitacao.replace(/^REQ-/, "");
  try {
    await httpPatch<RequisicaoBack>(`/requisicoes/${idBack}/enviar`);
  } catch (erro) {
    throw paraErroDeApi(erro);
  }
  const transporte = await obterTransporte(dados.solicitacao);
  if (!transporte) throw new ErroDeApi("Remessa despachada, mas não foi possível carregá-la.", 500);
  return { ...transporte, veiculo: dados.veiculo };
}

/** HU08, cenário 3: o hospital confirma o recebimento (EM_TRANSITO -> ATENDIDA). */
export async function confirmarRecebimento(id: string): Promise<Transporte> {
  const idBack = id.replace(/^REQ-/, "");
  try {
    await httpPatch<RequisicaoBack>(`/requisicoes/${idBack}/receber`);
  } catch (erro) {
    throw paraErroDeApi(erro);
  }
  const transporte = await obterTransporte(id);
  if (!transporte) throw new ErroDeApi("Recebimento registrado, mas não foi possível carregar a remessa.", 500);
  return transporte;
}

// ---------------------------------------------------------------------------
// Indicadores (HU09) — GET /indicadores, calculado na hora pelo back
// ---------------------------------------------------------------------------

export async function obterIndicadores(): Promise<Indicadores> {
  try {
    return indicadoresParaTela(await httpGet<IndicadoresBack>("/indicadores"));
  } catch (erro) {
    throw paraErroDeApi(erro);
  }
}

// ---------------------------------------------------------------------------
// Diversos
// ---------------------------------------------------------------------------

export function obterAlertas(): Promise<Alerta[]> {
  return responder(calcularAlertas());
}

export function buscarGlobal(termo: string): Promise<ResultadoBusca[]> {
  return responder(buscar(termo));
}
