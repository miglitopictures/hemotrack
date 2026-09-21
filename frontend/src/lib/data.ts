import { differenceInCalendarDays, format, parseISO } from "date-fns";
import { ptBR } from "date-fns/locale";

// ---------------------------------------------------------------------------
// Tipos
// ---------------------------------------------------------------------------

export type Status =
  | "Pendente"
  | "Aceita"
  | "Em separação"
  | "Em transporte"
  | "Entregue"
  | "Recusada";

export type Prioridade = "Rotina" | "Urgente" | "Emergência";

export type StatusBolsa = "Disponível" | "Reservada" | "Vence em breve" | "Enviada";

export type Solicitacao = {
  id: string;
  hospital: string;
  hemocentro: string;
  componente: string;
  tipoSanguineo: string;
  quantidade: number;
  prioridade: Prioridade;
  status: Status;
  /** ISO 8601 — formate na exibição com formatarDataHora(). */
  criadaEm: string;
  observacoes: string;
};

export type Bolsa = {
  codigo: string;
  componente: string;
  tipoSanguineo: string;
  /** ISO 8601 (data). */
  coleta: string;
  /** ISO 8601 (data). */
  validade: string;
  status: StatusBolsa;
  armazenamento: string;
};

export type Transporte = {
  id: string;
  solicitacao: string;
  origem: string;
  destino: string;
  bolsas: string[];
  status: Status;
  progresso: number;
  eta: string;
  veiculo: string;
  localizacao: string;
  /** Avisos operacionais que não vêm do sensor (trânsito, ocorrências). */
  observacoes: string[];
};

export type LeituraTemp = { hora: string; valor: number };

// ---------------------------------------------------------------------------
// Constantes de domínio
// ---------------------------------------------------------------------------

/**
 * TODO(back): "hoje" está congelado para os dados de demonstração ficarem
 * estáveis. Ao plugar a API, troque por `new Date()` — ou melhor, deixe o
 * servidor calcular validade/vencimento e devolva os dias já computados.
 */
export const HOJE = parseISO("2026-08-27T09:00:00");

export const componentes = [
  "Concentrado de hemácias",
  "Plasma fresco congelado",
  "Concentrado de plaquetas",
  "Crioprecipitado",
];

export const tiposSanguineos = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"];

export const HOSPITAL_ATUAL = "Hospital Santa Clara";
export const HEMOCENTRO_ATUAL = "Hemocentro Regional Recife";

/** Estoque mínimo de segurança por tipo sanguíneo. */
export const minimoPorTipo: Record<string, number> = {
  "O-": 20,
  "O+": 30,
  "A+": 25,
  "A-": 12,
  "B+": 15,
  "B-": 8,
  "AB+": 6,
  "AB-": 4,
};

export const armazenamentoPorComponente: Record<string, string> = {
  "Concentrado de hemácias": "Câmara A · 4 °C",
  "Concentrado de plaquetas": "Agitador P2 · 22 °C",
  "Plasma fresco congelado": "Freezer F1 · -30 °C",
  Crioprecipitado: "Freezer F2 · -30 °C",
};

/** Validade em dias a partir da coleta. */
const validadeEmDias: Record<string, number> = {
  "Concentrado de hemácias": 35,
  "Concentrado de plaquetas": 5,
  "Plasma fresco congelado": 365,
  Crioprecipitado: 365,
};

export const limitesTemperatura: Record<string, { min: number; max: number }> = {
  "Concentrado de hemácias": { min: 2, max: 6 },
  "Concentrado de plaquetas": { min: 20, max: 24 },
  "Plasma fresco congelado": { min: -40, max: -20 },
  Crioprecipitado: { min: -40, max: -20 },
};

export const motivosRecusa = [
  "Estoque insuficiente do hemocomponente",
  "Sem bolsas compatíveis disponíveis",
  "Prioridade redirecionada para outra unidade",
  "Dados da solicitação incompletos",
];

export const timelineEtapas: Status[] = [
  "Pendente",
  "Aceita",
  "Em separação",
  "Em transporte",
  "Entregue",
];

// ---------------------------------------------------------------------------
// Datas — tudo armazenado em ISO, formatado só na exibição
// ---------------------------------------------------------------------------

export function formatarData(iso: string): string {
  return format(parseISO(iso), "dd/MM/yyyy", { locale: ptBR });
}

export function formatarDataHora(iso: string): string {
  return format(parseISO(iso), "dd/MM/yyyy 'às' HH:mm", { locale: ptBR });
}

export function diasParaVencer(validadeIso: string): number {
  return differenceInCalendarDays(parseISO(validadeIso), HOJE);
}

function somarDias(isoData: string, dias: number): string {
  const d = parseISO(isoData);
  d.setDate(d.getDate() + dias);
  return format(d, "yyyy-MM-dd");
}

// ---------------------------------------------------------------------------
// Compatibilidade ABO/Rh
// ---------------------------------------------------------------------------

/** Tipos de bolsa que podem ser transfundidos em um receptor (hemácias). */
export const compatibilidadeHemacias: Record<string, string[]> = {
  "O-": ["O-"],
  "O+": ["O-", "O+"],
  "A-": ["O-", "A-"],
  "A+": ["O-", "O+", "A-", "A+"],
  "B-": ["O-", "B-"],
  "B+": ["O-", "O+", "B-", "B+"],
  "AB-": ["O-", "A-", "B-", "AB-"],
  "AB+": ["O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+"],
};

/** Plasma segue a lógica invertida das hemácias (AB é doador universal). */
export const compatibilidadePlasma: Record<string, string[]> = {
  "O-": ["O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+"],
  "O+": ["O-", "O+", "A-", "A+", "B-", "B+", "AB-", "AB+"],
  "A-": ["A-", "A+", "AB-", "AB+"],
  "A+": ["A-", "A+", "AB-", "AB+"],
  "B-": ["B-", "B+", "AB-", "AB+"],
  "B+": ["B-", "B+", "AB-", "AB+"],
  "AB-": ["AB-", "AB+"],
  "AB+": ["AB-", "AB+"],
};

export function tiposCompativeis(componente: string, receptor: string): string[] {
  const plasmatico = componente.includes("Plasma") || componente.includes("Crioprecipitado");
  const mapa = plasmatico ? compatibilidadePlasma : compatibilidadeHemacias;
  return mapa[receptor] ?? [receptor];
}

/** O- é o recurso mais escasso: avisar quando for gasto sem necessidade. */
export function ehTipoEscasso(tipoBolsa: string, receptor: string): boolean {
  return tipoBolsa === "O-" && receptor !== "O-";
}

// ---------------------------------------------------------------------------
// Solicitações
// ---------------------------------------------------------------------------

export const solicitacoes: Solicitacao[] = [
  {
    id: "SOL-2041",
    hospital: HOSPITAL_ATUAL,
    hemocentro: HEMOCENTRO_ATUAL,
    componente: "Concentrado de hemácias",
    tipoSanguineo: "O-",
    quantidade: 3,
    prioridade: "Emergência",
    status: "Em transporte",
    criadaEm: "2026-08-26T21:10:00",
    observacoes: "Paciente politraumatizado em centro cirúrgico.",
  },
  {
    id: "SOL-2040",
    hospital: HOSPITAL_ATUAL,
    hemocentro: HEMOCENTRO_ATUAL,
    componente: "Concentrado de plaquetas",
    tipoSanguineo: "A+",
    quantidade: 2,
    prioridade: "Urgente",
    status: "Em separação",
    criadaEm: "2026-08-26T18:32:00",
    observacoes: "Paciente oncológico, plaquetopenia severa.",
  },
  {
    id: "SOL-2038",
    hospital: HOSPITAL_ATUAL,
    hemocentro: "Hemocentro Boa Viagem",
    componente: "Concentrado de hemácias",
    tipoSanguineo: "AB+",
    quantidade: 1,
    prioridade: "Rotina",
    status: "Em transporte",
    criadaEm: "2026-08-26T14:05:00",
    observacoes: "Reposição de estoque interno.",
  },
  {
    id: "SOL-2035",
    hospital: HOSPITAL_ATUAL,
    hemocentro: HEMOCENTRO_ATUAL,
    componente: "Concentrado de hemácias",
    tipoSanguineo: "AB+",
    quantidade: 2,
    prioridade: "Rotina",
    status: "Pendente",
    criadaEm: "2026-08-27T07:48:00",
    observacoes: "Cirurgia eletiva agendada para quinta.",
  },
  {
    id: "SOL-2030",
    hospital: HOSPITAL_ATUAL,
    hemocentro: HEMOCENTRO_ATUAL,
    componente: "Crioprecipitado",
    tipoSanguineo: "O+",
    quantidade: 1,
    prioridade: "Urgente",
    status: "Entregue",
    criadaEm: "2026-08-26T16:20:00",
    observacoes: "Entregue com temperatura estável.",
  },
  {
    id: "SOL-2028",
    hospital: "Hospital São Lucas",
    hemocentro: HEMOCENTRO_ATUAL,
    componente: "Concentrado de hemácias",
    tipoSanguineo: "A-",
    quantidade: 4,
    prioridade: "Emergência",
    status: "Pendente",
    criadaEm: "2026-08-27T06:02:00",
    observacoes: "Unidade de queimados.",
  },
  {
    id: "SOL-2026",
    hospital: "Maternidade Aurora",
    hemocentro: HEMOCENTRO_ATUAL,
    componente: "Plasma fresco congelado",
    tipoSanguineo: "O+",
    quantidade: 3,
    prioridade: "Urgente",
    status: "Pendente",
    criadaEm: "2026-08-27T05:15:00",
    observacoes: "Risco de hemorragia pós-parto.",
  },
  {
    id: "SOL-2019",
    hospital: "Hospital Vila Nova",
    hemocentro: HEMOCENTRO_ATUAL,
    componente: "Concentrado de plaquetas",
    tipoSanguineo: "B-",
    quantidade: 2,
    prioridade: "Rotina",
    status: "Recusada",
    criadaEm: "2026-08-25T19:40:00",
    observacoes: "Estoque insuficiente do tipo solicitado.",
  },
];

export function solicitacaoPorId(id: string): Solicitacao | undefined {
  return solicitacoes.find((s) => s.id === id);
}

// ---------------------------------------------------------------------------
// Transportes
// ---------------------------------------------------------------------------

export const transportes: Transporte[] = [
  {
    id: "TRP-501",
    solicitacao: "SOL-2041",
    origem: HEMOCENTRO_ATUAL,
    destino: `${HOSPITAL_ATUAL} · Boa Viagem`,
    bolsas: ["BL-88214", "BL-88215", "BL-88262"],
    status: "Em transporte",
    progresso: 68,
    eta: "12 min",
    veiculo: "Van refrigerada · PE-4G21",
    localizacao: "Av. Domingos Ferreira, 1.240",
    observacoes: ["Trânsito moderado na Av. Conselheiro Aguiar"],
  },
  {
    id: "TRP-499",
    solicitacao: "SOL-2040",
    origem: HEMOCENTRO_ATUAL,
    destino: `${HOSPITAL_ATUAL} · Boa Viagem`,
    bolsas: ["BL-88230", "BL-88231"],
    status: "Em separação",
    progresso: 15,
    eta: "42 min",
    veiculo: "Aguardando coleta",
    localizacao: "Setor de expedição",
    observacoes: [],
  },
  {
    id: "TRP-494",
    solicitacao: "SOL-2030",
    origem: HEMOCENTRO_ATUAL,
    destino: `${HOSPITAL_ATUAL} · Boa Viagem`,
    bolsas: ["BL-88250"],
    status: "Entregue",
    progresso: 100,
    eta: "—",
    veiculo: "Van refrigerada · PE-8J07",
    localizacao: "Entregue na recepção do banco de sangue",
    observacoes: [],
  },
  {
    id: "TRP-502",
    solicitacao: "SOL-2038",
    origem: "Hemocentro Boa Viagem",
    destino: `${HOSPITAL_ATUAL} · Boa Viagem`,
    bolsas: ["BL-88261"],
    status: "Em transporte",
    progresso: 44,
    eta: "18 min",
    veiculo: "Van refrigerada · PE-2K55",
    localizacao: "Av. Boa Viagem, 3.120",
    observacoes: [],
  },
];

export function transportePorId(id: string): Transporte | undefined {
  return transportes.find((t) => t.id === id);
}

export function transporteDaSolicitacao(idSolicitacao: string): Transporte | undefined {
  return transportes.find((t) => t.solicitacao === idSolicitacao);
}

/** Série de temperatura por transporte (no back, virá do datalogger). */
export const leiturasTemperatura: Record<string, LeituraTemp[]> = {
  "TRP-501": [
    { hora: "20:40", valor: 4.0 },
    { hora: "20:50", valor: 4.1 },
    { hora: "21:00", valor: 4.4 },
    { hora: "21:10", valor: 4.2 },
    { hora: "21:20", valor: 4.3 },
    { hora: "21:30", valor: 4.2 },
  ],
  "TRP-499": [
    { hora: "21:00", valor: 21.8 },
    { hora: "21:10", valor: 22.0 },
    { hora: "21:20", valor: 22.1 },
  ],
  "TRP-494": [
    { hora: "16:00", valor: -30.1 },
    { hora: "16:10", valor: -29.6 },
    { hora: "16:20", valor: -29.4 },
  ],
  "TRP-502": [
    { hora: "22:10", valor: 5.4 },
    { hora: "22:20", valor: 6.2 },
    { hora: "22:30", valor: 7.1 },
    { hora: "22:40", valor: 8.3 },
  ],
};

export function temperaturaFora(componente: string, valor: number): boolean {
  const lim = limitesTemperatura[componente];
  if (!lim) return false;
  return valor < lim.min || valor > lim.max;
}

/** Componente transportado, inferido pela solicitação vinculada. */
export function componenteDoTransporte(t: Transporte): string {
  return solicitacaoPorId(t.solicitacao)?.componente ?? "Concentrado de hemácias";
}

export function leiturasDoTransporte(t: Transporte): LeituraTemp[] {
  return leiturasTemperatura[t.id] ?? [];
}

export function temperaturaAtual(t: Transporte): number | null {
  const leituras = leiturasDoTransporte(t);
  return leituras.length > 0 ? leituras[leituras.length - 1]!.valor : null;
}

export function formatarTemperatura(valor: number | null): string {
  if (valor === null) return "—";
  return `${valor.toFixed(1).replace(".", ",")} °C`;
}

/** Alertas do trajeto, calculados das leituras + observações manuais. */
export function alertasDoTransporte(t: Transporte): string[] {
  const componente = componenteDoTransporte(t);
  const lim = limitesTemperatura[componente];
  const fora = leiturasDoTransporte(t).filter((l) => temperaturaFora(componente, l.valor));

  const alertas: string[] = [];
  if (lim && fora.length > 0) {
    const ultima = fora[fora.length - 1]!.valor;
    alertas.push(
      `Temperatura fora da faixa de ${lim.min} °C a ${lim.max} °C em ${fora.length} leitura(s) — última: ${formatarTemperatura(ultima)}`,
    );
  }
  return [...alertas, ...t.observacoes];
}

export function transporteComAlerta(t: Transporte): boolean {
  const componente = componenteDoTransporte(t);
  return leiturasDoTransporte(t).some((l) => temperaturaFora(componente, l.valor));
}

// ---------------------------------------------------------------------------
// Estoque de bolsas — fonte única de verdade
// ---------------------------------------------------------------------------

/** Distribuição alvo do inventário por tipo sanguíneo (só para o mock). */
const inventarioAlvo: Record<string, number> = {
  "O-": 12,
  "O+": 38,
  "A+": 29,
  "A-": 9,
  "B+": 17,
  "B-": 5,
  "AB+": 8,
  "AB-": 3,
};

/** Bolsas nomeadas — referenciadas pelos transportes, precisam existir. */
const bolsasBase: Omit<Bolsa, "status">[] = [
  {
    codigo: "BL-88214",
    componente: "Concentrado de hemácias",
    tipoSanguineo: "O-",
    coleta: "2026-08-10",
    validade: "2026-09-14",
    armazenamento: "Câmara A · 4 °C",
  },
  {
    codigo: "BL-88215",
    componente: "Concentrado de hemácias",
    tipoSanguineo: "O-",
    coleta: "2026-08-11",
    validade: "2026-09-15",
    armazenamento: "Câmara A · 4 °C",
  },
  {
    codigo: "BL-88230",
    componente: "Concentrado de plaquetas",
    tipoSanguineo: "A+",
    coleta: "2026-08-25",
    validade: "2026-08-30",
    armazenamento: "Agitador P2 · 22 °C",
  },
  {
    codigo: "BL-88231",
    componente: "Concentrado de plaquetas",
    tipoSanguineo: "A+",
    coleta: "2026-08-25",
    validade: "2026-08-30",
    armazenamento: "Agitador P2 · 22 °C",
  },
  {
    codigo: "BL-88244",
    componente: "Plasma fresco congelado",
    tipoSanguineo: "B+",
    coleta: "2026-07-02",
    validade: "2027-07-02",
    armazenamento: "Freezer F1 · -30 °C",
  },
  {
    codigo: "BL-88250",
    componente: "Crioprecipitado",
    tipoSanguineo: "O+",
    coleta: "2026-07-15",
    validade: "2027-07-15",
    armazenamento: "Freezer F2 · -30 °C",
  },
  {
    codigo: "BL-88261",
    componente: "Concentrado de hemácias",
    tipoSanguineo: "AB+",
    coleta: "2026-08-18",
    validade: "2026-09-22",
    armazenamento: "Câmara B · 4 °C",
  },
  {
    codigo: "BL-88262",
    componente: "Concentrado de hemácias",
    tipoSanguineo: "A-",
    coleta: "2026-08-19",
    validade: "2026-09-23",
    armazenamento: "Câmara B · 4 °C",
  },
];

/**
 * Gerador determinístico (LCG com semente fixa): o inventário precisa ser
 * idêntico no servidor e no cliente, senão a hidratação do React quebra.
 * Math.random() aqui seria um bug de SSR.
 */
function criarRandom(semente: number) {
  let estado = semente;
  return () => {
    estado = (estado * 1664525 + 1013904223) % 4294967296;
    return estado / 4294967296;
  };
}

function gerarInventario(): Omit<Bolsa, "status">[] {
  const random = criarRandom(20260827);
  const lista: Omit<Bolsa, "status">[] = [...bolsasBase];
  let sequencial = 88300;

  for (const tipo of tiposSanguineos) {
    const jaExistem = bolsasBase.filter((b) => b.tipoSanguineo === tipo).length;
    const faltam = Math.max(0, (inventarioAlvo[tipo] ?? 0) - jaExistem);

    for (let i = 0; i < faltam; i++) {
      const sorteio = random();
      const componente =
        sorteio < 0.62
          ? "Concentrado de hemácias"
          : sorteio < 0.78
            ? "Concentrado de plaquetas"
            : sorteio < 0.92
              ? "Plasma fresco congelado"
              : "Crioprecipitado";

      const shelfLife = validadeEmDias[componente] ?? 35;
      // Plaquetas são sempre recém-coletadas; o resto se espalha no tempo.
      const idade =
        componente === "Concentrado de plaquetas"
          ? Math.floor(random() * 5)
          : Math.floor(random() * Math.min(shelfLife - 2, 60));

      const coleta = somarDias("2026-08-27", -idade);
      lista.push({
        codigo: `BL-${sequencial}`,
        componente,
        tipoSanguineo: tipo,
        coleta,
        validade: somarDias(coleta, shelfLife),
        armazenamento: armazenamentoPorComponente[componente] ?? "Câmara A · 4 °C",
      });
      sequencial += 1;
    }
  }

  return lista;
}

const codigosEnviados = new Set(
  transportes.filter((t) => t.status === "Entregue").flatMap((t) => t.bolsas),
);

const codigosReservados = new Set(
  transportes.filter((t) => t.status !== "Entregue").flatMap((t) => t.bolsas),
);

/** Status derivado — nunca escrito à mão, para não divergir do resto. */
export function calcularStatusBolsa(b: Omit<Bolsa, "status">): StatusBolsa {
  if (codigosEnviados.has(b.codigo)) return "Enviada";
  if (codigosReservados.has(b.codigo)) return "Reservada";
  if (diasParaVencer(b.validade) <= 3) return "Vence em breve";
  return "Disponível";
}

export const bolsas: Bolsa[] = gerarInventario().map((b) => ({
  ...b,
  status: calcularStatusBolsa(b),
}));

export function bolsaPorCodigo(codigo: string): Bolsa | undefined {
  return bolsas.find((b) => b.codigo === codigo);
}

/** Uma bolsa só pode ser separada se não estiver reservada nem enviada. */
export function bolsaSelecionavel(b: Bolsa): boolean {
  return b.status === "Disponível" || b.status === "Vence em breve";
}

/** Bolsas compatíveis e livres, priorizando o vencimento mais próximo (FEFO). */
export function bolsasCompativeis(componente: string, receptor: string): Bolsa[] {
  const aceitos = tiposCompativeis(componente, receptor);
  return bolsas
    .filter(
      (b) =>
        b.componente === componente && aceitos.includes(b.tipoSanguineo) && bolsaSelecionavel(b),
    )
    .sort((a, b) => diasParaVencer(a.validade) - diasParaVencer(b.validade));
}

// ---------------------------------------------------------------------------
// Agregações derivadas do estoque
// ---------------------------------------------------------------------------

export const estoquePorTipo = tiposSanguineos.map((tipo) => ({
  tipo,
  bolsas: bolsas.filter((b) => b.tipoSanguineo === tipo && bolsaSelecionavel(b)).length,
  minimo: minimoPorTipo[tipo] ?? 0,
}));

export const totalDisponivel = bolsas.filter(bolsaSelecionavel).length;

export function bolsasVencendoEm(dias: number): Bolsa[] {
  return bolsas.filter((b) => bolsaSelecionavel(b) && diasParaVencer(b.validade) <= dias);
}

export function contarPorArmazenamento(trecho: string): number {
  return bolsas.filter((b) => b.armazenamento.includes(trecho) && b.status !== "Enviada").length;
}

export type Alerta = {
  titulo: string;
  detalhe: string;
  nivel: "alto" | "medio";
};

/** Alertas calculados a partir do estado atual — nada fixo no código. */
export function calcularAlertas(): Alerta[] {
  const lista: Alerta[] = [];

  for (const item of estoquePorTipo) {
    if (item.minimo > 0 && item.bolsas < item.minimo) {
      lista.push({
        titulo: `Estoque crítico de ${item.tipo}`,
        detalhe: `${item.bolsas} bolsas disponíveis · mínimo de ${item.minimo}`,
        nivel: item.bolsas < item.minimo / 2 ? "alto" : "medio",
      });
    }
  }

  const vencendo = bolsasVencendoEm(3);
  if (vencendo.length > 0) {
    lista.push({
      titulo: `${vencendo.length} bolsa(s) vencem em até 3 dias`,
      detalhe: vencendo
        .slice(0, 4)
        .map((b) => b.codigo)
        .join(" · "),
      nivel: "medio",
    });
  }

  const pendentes = solicitacoes.filter((s) => s.status === "Pendente");
  if (pendentes.length > 0) {
    lista.push({
      titulo: `${pendentes.length} solicitações aguardando análise`,
      detalhe: pendentes.map((s) => s.id).join(" · "),
      nivel: pendentes.some((s) => s.prioridade === "Emergência") ? "alto" : "medio",
    });
  }

  return lista.sort((a, b) => (a.nivel === b.nivel ? 0 : a.nivel === "alto" ? -1 : 1));
}

// ---------------------------------------------------------------------------
// Indicadores
// ---------------------------------------------------------------------------

export const saidasSemana = [
  { dia: "Seg", saidas: 14 },
  { dia: "Ter", saidas: 22 },
  { dia: "Qua", saidas: 18 },
  { dia: "Qui", saidas: 31 },
  { dia: "Sex", saidas: 26 },
  { dia: "Sáb", saidas: 12 },
  { dia: "Dom", saidas: 9 },
];

export const demandaPorComponente = [
  { componente: "Hemácias", pedidos: 42, atendidos: 38 },
  { componente: "Plaquetas", pedidos: 21, atendidos: 17 },
  { componente: "Plasma", pedidos: 18, atendidos: 18 },
  { componente: "Crio", pedidos: 9, atendidos: 8 },
];

export const temposAtendimentoMin = [38, 42, 51, 27, 63, 45, 33, 58, 40, 47, 29, 55];

export function media(v: number[]): number {
  if (v.length === 0) return 0;
  return v.reduce((a, b) => a + b, 0) / v.length;
}

export function desvioPadrao(v: number[]): number {
  if (v.length < 2) return 0;
  const m = media(v);
  return Math.sqrt(v.reduce((a, b) => a + (b - m) ** 2, 0) / (v.length - 1));
}

export function mediana(v: number[]): number {
  if (v.length === 0) return 0;
  const s = [...v].sort((a, b) => a - b);
  const meio = Math.floor(s.length / 2);
  return s.length % 2 ? s[meio]! : (s[meio - 1]! + s[meio]!) / 2;
}

// ---------------------------------------------------------------------------
// Distribuição
// ---------------------------------------------------------------------------

export const etapasDistribuicao = [
  {
    id: "conferencia",
    titulo: "Conferência das bolsas",
    detalhe: "Dupla checagem de código, tipo e validade",
    duracao: "6 min",
  },
  {
    id: "embalagem",
    titulo: "Embalagem térmica",
    detalhe: "Caixa isotérmica com gelo reciclável e datalogger",
    duracao: "8 min",
  },
  {
    id: "remessa",
    titulo: "Emissão da remessa",
    detalhe: "Nota de distribuição e lacre numerado",
    duracao: "3 min",
  },
  {
    id: "coleta",
    titulo: "Coleta pelo veículo",
    detalhe: "Van refrigerada + conferência do motorista",
    duracao: "5 min",
  },
];

export const rotaSimplificada = [
  { ponto: HEMOCENTRO_ATUAL, detalhe: "Saída · Av. Rui Barbosa", km: 0 },
  { ponto: "Av. Agamenon Magalhães", detalhe: "Corredor principal", km: 3.2 },
  { ponto: "Ponte Paulo Guerra", detalhe: "Travessia", km: 6.8 },
  { ponto: "Av. Conselheiro Aguiar", detalhe: "Trecho com tráfego moderado", km: 9.4 },
  { ponto: `${HOSPITAL_ATUAL} · Boa Viagem`, detalhe: "Entrega no banco de sangue", km: 11.7 },
];

export const veiculos = [
  { nome: "Van refrigerada · PE-4G21", detalhe: "2–6 °C · disponível agora" },
  { nome: "Van refrigerada · PE-8J07", detalhe: "2–6 °C · retorna em 20 min" },
  { nome: "Moto com bolsa isotérmica", detalhe: "Até 2 bolsas · rota expressa" },
];

// ---------------------------------------------------------------------------
// Busca global (usada no header)
// ---------------------------------------------------------------------------

export type ResultadoBusca = {
  tipo: "Solicitação" | "Bolsa" | "Transporte";
  id: string;
  titulo: string;
  detalhe: string;
};

export function buscar(termo: string, limite = 8): ResultadoBusca[] {
  const q = termo.trim().toLowerCase();
  if (q.length < 2) return [];

  const resultados: ResultadoBusca[] = [];

  for (const s of solicitacoes) {
    if (
      s.id.toLowerCase().includes(q) ||
      s.hospital.toLowerCase().includes(q) ||
      s.componente.toLowerCase().includes(q) ||
      s.tipoSanguineo.toLowerCase() === q
    ) {
      resultados.push({
        tipo: "Solicitação",
        id: s.id,
        titulo: `${s.id} · ${s.componente}`,
        detalhe: `${s.hospital} · ${s.tipoSanguineo} · ${s.status}`,
      });
    }
  }

  for (const b of bolsas) {
    if (b.codigo.toLowerCase().includes(q) || b.componente.toLowerCase().includes(q)) {
      resultados.push({
        tipo: "Bolsa",
        id: b.codigo,
        titulo: `${b.codigo} · ${b.tipoSanguineo}`,
        detalhe: `${b.componente} · ${b.status}`,
      });
    }
  }

  for (const t of transportes) {
    if (
      t.id.toLowerCase().includes(q) ||
      t.solicitacao.toLowerCase().includes(q) ||
      t.destino.toLowerCase().includes(q)
    ) {
      resultados.push({
        tipo: "Transporte",
        id: t.id,
        titulo: `${t.id} · ${t.veiculo}`,
        detalhe: `${t.destino} · ${t.status}`,
      });
    }
  }

  return resultados.slice(0, limite);
}
