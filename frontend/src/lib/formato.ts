/** Formatação para exibição. O backend guarda dado cru; formatar é do front. */

/** "88777665555544" → "88.777.665/5554-44" */
export function formatarCnpj(cnpj: string): string {
  const digitos = cnpj.replace(/\D/g, "");

  // Se não tiver o tamanho de um CNPJ, devolve como veio em vez de
  // produzir uma string estranha. Formatar não é validar.
  if (digitos.length !== 14) {
    return cnpj;
  }

  return digitos.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, "$1.$2.$3/$4-$5");
}