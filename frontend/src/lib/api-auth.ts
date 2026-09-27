/**
 * Chamadas de cadastro e autenticação.
 *
 * Os tipos aqui espelham os DTOs do backend, campo a campo — por isso o
 * sufixo "DoBack". Não confunda com os tipos de `data.ts`, que são a
 * linguagem do front; se um dia as duas divergirem, a tradução vira um
 * adapter em `lib/adapters/`, como já é feito com requisição.
 *
 * Este arquivo só fala HTTP. Guardar token, redirecionar e mostrar erro
 * é trabalho das telas.
 */

import { httpGet, httpPatch, httpPost } from "./httpClient";

export type TipoInstituicao = "HOSPITAL" | "HEMOCENTRO";
export type StatusInstituicao = "PENDENTE_APROVACAO" | "APROVADA";
export type Papel = "ADMIN_SISTEMA" | "ADMIN_INSTITUICAO" | "OPERADOR";

export type InstituicaoDoBack = {
  id: number;
  razaoSocial: string;
  cnpj: string; // vem só com dígitos: "88777665555544"
  tipo: TipoInstituicao;
  status: StatusInstituicao;
  endereco: string;
  municipio: string;
  telefone: string | null;
};

export type UsuarioDoBack = {
  id: number;
  nome: string;
  email: string;
  papel: Papel;
  ativo: boolean;
  instituicaoId: number | null;
};

/** Corpo do POST /instituicoes. */
export type CadastroRequest = {
  instituicao: {
    razaoSocial: string;
    cnpj: string;
    tipo: TipoInstituicao;
    endereco: string;
    municipio: string;
    telefone: string | null;
  };
  administrador: {
    nome: string;
    email: string;
    senha: string;
  };
};

export type CadastroResponse = {
  instituicao: InstituicaoDoBack;
  administrador: UsuarioDoBack;
};

export type LoginResponse = {
  token: string;
  expiraEm: string; // ISO-8601, ex.: "2026-09-27T05:06:37.669Z"
};

/** Resposta do GET /auth/me: o usuário e a instituição dele já resolvida. */
export type UsuarioLogado = {
  id: number;
  nome: string;
  email: string;
  papel: Papel;
  ativo: boolean;
  instituicaoId: number | null;
  instituicao: InstituicaoDoBack | null;
};

/** Chave do React Query para a sessão. Usada no login e no app-shell. */
export const CHAVE_USUARIO_LOGADO = ["usuarioLogado"] as const;

export function cadastrarInstituicao(dados: CadastroRequest): Promise<CadastroResponse> {
  return httpPost<CadastroResponse>("/instituicoes", dados);
}

export function login(email: string, senha: string): Promise<LoginResponse> {
  return httpPost<LoginResponse>("/auth/login", { email, senha });
}

export function buscarUsuarioLogado(): Promise<UsuarioLogado> {
  return httpGet<UsuarioLogado>("/auth/me");
}

/** Chave do React Query para a lista de instituições, por status. */
export function chaveInstituicoes(status: StatusInstituicao) {
  return ["instituicoes", status] as const;
}

export function listarInstituicoes(status: StatusInstituicao): Promise<InstituicaoDoBack[]> {
  return httpGet<InstituicaoDoBack[]>(`/instituicoes?status=${status}`);
}

export function aprovarInstituicao(id: number): Promise<InstituicaoDoBack> {
  // PATCH sem corpo: a ação está no caminho, não no payload. O httpClient
  // omite o body e o Content-Type quando não há corpo.
  return httpPatch<InstituicaoDoBack>(`/instituicoes/${id}/aprovar`);
}
