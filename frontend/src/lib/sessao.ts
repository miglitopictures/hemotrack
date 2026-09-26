/**
 * Guarda a sessão do usuário no navegador.
 *
 * É o ÚNICO arquivo que fala com o localStorage. Todo o resto do front
 * pergunta aqui — assim, trocar de localStorage para cookie um dia mexe
 * só neste arquivo.
 */

const CHAVE = "hemotrack.token";

export type ClaimsDoToken = {
  sub: string; // id do usuário, como string (é o "subject" do JWT)
  instituicaoId: number;
  papel: string;
  iat: number; // emitido em, em SEGUNDOS desde 1970
  exp: number; // expira em, em SEGUNDOS desde 1970
};

export function guardarToken(token: string): void {
  localStorage.setItem(CHAVE, token);
}

export function lerToken(): string | null {
  return localStorage.getItem(CHAVE);
}

export function apagarToken(): void {
  localStorage.removeItem(CHAVE);
}

/**
 * Lê o payload do JWT sem validar a assinatura — o front NÃO tem a chave
 * e nem deveria ter. Serve só para saber quando parar de fingir que há
 * sessão; quem decide de verdade é sempre o backend.
 */
export function lerClaims(token: string): ClaimsDoToken | null {
  const [, payload] = token.split(".");

  if (payload === undefined) {
    return null;
  }

  try {
    // O JWT usa Base64URL ("-" e "_"); o atob espera Base64 ("+" e "/").
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(base64)) as ClaimsDoToken;
  } catch {
    return null; // token malformado: trata como se não houvesse sessão
  }
}

export function tokenExpirado(token: string): boolean {
  const claims = lerClaims(token);

  if (claims === null) {
    return true;
  }

  // exp vem em segundos; Date.now() devolve milissegundos.
  const agoraEmSegundos = Math.floor(Date.now() / 1000);
  return claims.exp <= agoraEmSegundos;
}

export function temSessaoValida(): boolean {
  const token = lerToken();
  return token !== null && !tokenExpirado(token);
}