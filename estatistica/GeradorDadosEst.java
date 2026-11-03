import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * PI2-106 — Gerador da massa de dados sintéticos de EST (Unidade 1).
 *
 * Programa Java de arquivo único, fora do build do Maven: não é compilado nem empacotado com o backend.
 * Rodar a partir da raiz do repositório (JDK 21):
 *
 *   java estatistica/GeradorDadosEst.java
 *
 * Gera, em estatistica/dados/:
 *   instituicoes.csv, hemocomponentes.csv, requisicoes.csv, telemetria.csv
 *
 * Mesma semente = mesmos arquivos (reprodutível). Nomes de campos e enums seguem docs/api/dominio.md.
 * Nenhum dado é real: instituições, bolsas e requisições são fictícias (LGPD).
 */
public final class GeradorDadosEst {

    // ---------------------------------------------------------------- parâmetros gerais
    static final long SEMENTE = 20261002L;
    static final ZoneOffset FUSO = ZoneOffset.ofHours(-3); // Recife, sem horário de verão
    /** "Agora" da simulação: tudo depois disso ainda não aconteceu. */
    static final OffsetDateTime REFERENCIA = OffsetDateTime.of(2026, 9, 30, 15, 0, 0, 0, FUSO);
    static final int PERIODO_DIAS = 90;
    static final int INTERVALO_TELEMETRIA_MIN = 5;
    static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    // ---------------------------------------------------------------- domínio (docs/api/dominio.md)
    enum TipoInstituicao { HOSPITAL, HEMOCENTRO }
    enum TipoABO { A, B, AB, O }
    enum FatorRh { POSITIVO, NEGATIVO }
    enum TipoHemocomponente { HEMACIAS, PLASMA, PLAQUETAS, CRIOPRECIPITADO }
    enum StatusHemocomponente { DISPONIVEL, RESERVADO, TRANSFUNDIDO, DESCARTADO }
    enum Prioridade { NORMAL, URGENCIA, EMERGENCIA }
    enum StatusRequisicao { ABERTA, ACEITA, ALOCADA, EM_TRANSITO, ATENDIDA, RECUSADA, CANCELADA }

    record Instituicao(long id, String razaoSocial, TipoInstituicao tipo, String municipio,
                       double latitude, double longitude, double pesoDemanda, long hemocentroPreferencial) {
    }

    /** Tipo sanguíneo completo (ABO + Rh) com a frequência aproximada na população brasileira. */
    record TipoSanguineo(TipoABO abo, FatorRh rh, double frequencia) {
        String rotulo() {
            return abo + (rh == FatorRh.POSITIVO ? "+" : "-");
        }
    }

    static final class Requisicao {
        long id;
        Instituicao hospital;
        Instituicao hemocentro;
        TipoHemocomponente tipo;
        TipoSanguineo sangue;
        int quantidade;
        Prioridade prioridade;
        StatusRequisicao status;
        OffsetDateTime criadaEm, aceitaEm, alocadaEm, enviadaEm, atendidaEm, recusadaEm, canceladaEm;
        String motivoRecusa;
    }

    static final class Hemocomponente {
        long id;
        TipoHemocomponente tipo;
        TipoABO abo;
        FatorRh rh;
        LocalDate dataColeta;
        LocalDate dataValidade;
        StatusHemocomponente status;
        long instituicaoId;
        Long requisicaoId;
    }

    // Distribuição ABO/Rh aproximada (valores de referência amplamente divulgados por hemocentros brasileiros).
    static final List<TipoSanguineo> TIPOS_SANGUINEOS = List.of(
            new TipoSanguineo(TipoABO.O, FatorRh.POSITIVO, 0.360),
            new TipoSanguineo(TipoABO.A, FatorRh.POSITIVO, 0.340),
            new TipoSanguineo(TipoABO.O, FatorRh.NEGATIVO, 0.090),
            new TipoSanguineo(TipoABO.A, FatorRh.NEGATIVO, 0.080),
            new TipoSanguineo(TipoABO.B, FatorRh.POSITIVO, 0.080),
            new TipoSanguineo(TipoABO.AB, FatorRh.POSITIVO, 0.025),
            new TipoSanguineo(TipoABO.B, FatorRh.NEGATIVO, 0.020),
            new TipoSanguineo(TipoABO.AB, FatorRh.NEGATIVO, 0.005));

    /** Mesma proporção usada pelo gerador de inventário do frontend (frontend/src/lib/data.ts). */
    static double proporcao(TipoHemocomponente t) {
        return switch (t) {
            case HEMACIAS -> 0.62;
            case PLAQUETAS -> 0.16;
            case PLASMA -> 0.14;
            case CRIOPRECIPITADO -> 0.08;
        };
    }

    /** Validade em dias a partir da coleta (mesmos valores do frontend). */
    static int validadeDias(TipoHemocomponente t) {
        return switch (t) {
            case HEMACIAS -> 35;
            case PLAQUETAS -> 5;
            case PLASMA, CRIOPRECIPITADO -> 365;
        };
    }

    /** Temperatura-alvo do transporte, em °C (centro da faixa permitida do frontend). */
    static double temperaturaAlvo(TipoHemocomponente t) {
        return switch (t) {
            case HEMACIAS -> 4.0;
            case PLAQUETAS -> 22.0;
            case PLASMA, CRIOPRECIPITADO -> -30.0;
        };
    }

    static double ruidoTemperatura(TipoHemocomponente t) {
        return (t == TipoHemocomponente.PLASMA || t == TipoHemocomponente.CRIOPRECIPITADO) ? 1.5 : 0.5;
    }

    static final String[] MOTIVOS_RECUSA = {
            "Estoque insuficiente do hemocomponente",
            "Sem bolsas compatíveis disponíveis",
            "Prioridade redirecionada para outra unidade",
            "Dados da solicitação incompletos",
    };

    // Instituições fictícias. Coordenadas aproximadas dos municípios (só para a telemetria simulada).
    static final List<Instituicao> INSTITUICOES = List.of(
            new Instituicao(1, "Hemocentro Regional Recife", TipoInstituicao.HEMOCENTRO, "Recife", -8.0476, -34.8770, 0, 0),
            new Instituicao(2, "Hemocentro Regional Caruaru", TipoInstituicao.HEMOCENTRO, "Caruaru", -8.2760, -35.9819, 0, 0),
            new Instituicao(3, "Hospital Santa Clara", TipoInstituicao.HOSPITAL, "Recife", -8.0630, -34.8990, 0.22, 1),
            new Instituicao(4, "Hospital Boa Vista", TipoInstituicao.HOSPITAL, "Recife", -8.0540, -34.8850, 0.18, 1),
            new Instituicao(5, "Hospital São Lucas", TipoInstituicao.HOSPITAL, "Olinda", -8.0089, -34.8553, 0.12, 1),
            new Instituicao(6, "Hospital Maria Aparecida", TipoInstituicao.HOSPITAL, "Jaboatão dos Guararapes", -8.1127, -35.0147, 0.12, 1),
            new Instituicao(7, "Hospital Monte Verde", TipoInstituicao.HOSPITAL, "Paulista", -7.9408, -34.8728, 0.08, 1),
            new Instituicao(8, "Hospital Beira-Rio", TipoInstituicao.HOSPITAL, "Camaragibe", -8.0235, -34.9782, 0.06, 1),
            new Instituicao(9, "Hospital do Agreste", TipoInstituicao.HOSPITAL, "Caruaru", -8.2850, -35.9700, 0.14, 2),
            new Instituicao(10, "Hospital Vale do Ipojuca", TipoInstituicao.HOSPITAL, "Gravatá", -8.2012, -35.5649, 0.08, 2));

    // ---------------------------------------------------------------- execução
    public static void main(String[] args) throws IOException {
        Path base = resolverBase(args);
        Path dados = base.resolve("dados");
        Files.createDirectories(dados);

        Random rnd = new Random(SEMENTE);
        List<Requisicao> requisicoes = gerarRequisicoes(rnd);
        List<Hemocomponente> hemocomponentes = gerarHemocomponentes(requisicoes, rnd);

        escreverInstituicoes(dados.resolve("instituicoes.csv"));
        escreverRequisicoes(dados.resolve("requisicoes.csv"), requisicoes);
        escreverHemocomponentes(dados.resolve("hemocomponentes.csv"), hemocomponentes);
        int leituras = escreverTelemetria(dados.resolve("telemetria.csv"), requisicoes, rnd);

        System.out.printf(Locale.ROOT, "Dados gerados em %s%n", dados.toAbsolutePath().normalize());
        System.out.printf(Locale.ROOT, "  instituicoes.csv    %6d linhas%n", INSTITUICOES.size());
        System.out.printf(Locale.ROOT, "  requisicoes.csv     %6d linhas%n", requisicoes.size());
        System.out.printf(Locale.ROOT, "  hemocomponentes.csv %6d linhas%n", hemocomponentes.size());
        System.out.printf(Locale.ROOT, "  telemetria.csv      %6d linhas%n", leituras);
    }

    /** Aceita rodar da raiz do repositório ou de dentro de estatistica/. Um argumento opcional força a pasta. */
    static Path resolverBase(String[] args) {
        if (args.length > 0) {
            return Path.of(args[0]);
        }
        Path daRaiz = Path.of("estatistica");
        return Files.isDirectory(daRaiz) ? daRaiz : Path.of(".");
    }

    // ---------------------------------------------------------------- requisições
    static List<Requisicao> gerarRequisicoes(Random rnd) {
        List<Instituicao> hospitais = INSTITUICOES.stream().filter(i -> i.tipo() == TipoInstituicao.HOSPITAL).toList();
        OffsetDateTime inicio = REFERENCIA.truncatedTo(java.time.temporal.ChronoUnit.DAYS).minusDays(PERIODO_DIAS);
        List<Requisicao> lista = new ArrayList<>();

        for (int d = 0; d <= PERIODO_DIAS; d++) {
            OffsetDateTime dia = inicio.plusDays(d);
            double fatorSemana = switch (dia.getDayOfWeek()) {
                case SATURDAY -> 0.70;
                case SUNDAY -> 0.55;
                default -> 1.0;
            };
            int n = poisson(15.0 * fatorSemana, rnd);
            for (int k = 0; k < n; k++) {
                OffsetDateTime criada = dia.plusMinutes(minutoDoDia(rnd));
                if (criada.isAfter(REFERENCIA)) {
                    continue;
                }
                lista.add(novaRequisicao(criada, hospitais, rnd));
            }
        }
        lista.sort(Comparator.comparing(r -> r.criadaEm));
        for (int i = 0; i < lista.size(); i++) {
            lista.get(i).id = i + 1L;
        }
        return lista;
    }

    static Requisicao novaRequisicao(OffsetDateTime criada, List<Instituicao> hospitais, Random rnd) {
        Requisicao r = new Requisicao();
        r.criadaEm = criada;
        r.hospital = sortearPorPeso(hospitais, Instituicao::pesoDemanda, rnd);
        long idHemocentro = rnd.nextDouble() < 0.93 ? r.hospital.hemocentroPreferencial()
                : (r.hospital.hemocentroPreferencial() == 1 ? 2 : 1);
        r.hemocentro = INSTITUICOES.get((int) idHemocentro - 1);
        r.tipo = sortearPorPeso(List.of(TipoHemocomponente.values()), GeradorDadosEst::proporcao, rnd);
        r.sangue = sortearPorPeso(TIPOS_SANGUINEOS, TipoSanguineo::frequencia, rnd);
        double p = rnd.nextDouble();
        r.prioridade = p < 0.60 ? Prioridade.NORMAL : p < 0.88 ? Prioridade.URGENCIA : Prioridade.EMERGENCIA;
        r.quantidade = quantidade(r, rnd);

        // --- linha do tempo (minutos, distribuições lognormais: assimétricas e com cauda à direita)
        OffsetDateTime aceite = criada.plusMinutes(esperaAceite(r, rnd));
        boolean tipoRaro = r.sangue.frequencia() <= 0.02 || (r.sangue.abo() == TipoABO.O && r.sangue.rh() == FatorRh.NEGATIVO);
        double chanceRecusa = tipoRaro ? 0.15 : 0.05;

        if (rnd.nextDouble() < chanceRecusa) {
            r.recusadaEm = aceite;
            r.motivoRecusa = MOTIVOS_RECUSA[tipoRaro ? rnd.nextInt(2) : rnd.nextInt(MOTIVOS_RECUSA.length)];
        } else {
            r.aceitaEm = aceite;
            r.alocadaEm = aceite.plusMinutes(lognormal(mediana(r.prioridade, 12, 30, 120), sigma(r.prioridade, 0.4, 0.5, 0.7), rnd));
            if (rnd.nextDouble() < 0.03) {
                // cancelamento pelo hospital antes da alocação
                long janela = Math.max(1, Duration.between(criada, r.alocadaEm).toMinutes());
                r.canceladaEm = criada.plusMinutes(1 + (long) (rnd.nextDouble() * janela));
                r.aceitaEm = r.canceladaEm.isAfter(r.aceitaEm) ? r.aceitaEm : null;
                r.alocadaEm = null;
            } else {
                r.enviadaEm = r.alocadaEm.plusMinutes(lognormal(mediana(r.prioridade, 5, 15, 60), sigma(r.prioridade, 0.5, 0.5, 0.8), rnd));
                r.atendidaEm = r.enviadaEm.plusMinutes(tempoTransporte(r, rnd));
            }
        }
        fecharNaReferencia(r);
        return r;
    }

    /** Descarta eventos que ficariam depois de REFERENCIA e define o status atual. */
    static void fecharNaReferencia(Requisicao r) {
        if (depois(r.recusadaEm)) r.recusadaEm = null;
        if (depois(r.canceladaEm)) r.canceladaEm = null;
        if (depois(r.aceitaEm)) r.aceitaEm = null;
        if (depois(r.alocadaEm)) r.alocadaEm = null;
        if (depois(r.enviadaEm)) r.enviadaEm = null;
        if (depois(r.atendidaEm)) r.atendidaEm = null;
        if (r.aceitaEm == null) r.alocadaEm = null;
        if (r.alocadaEm == null) r.enviadaEm = null;
        if (r.enviadaEm == null) r.atendidaEm = null;

        if (r.recusadaEm != null) r.status = StatusRequisicao.RECUSADA;
        else if (r.canceladaEm != null) r.status = StatusRequisicao.CANCELADA;
        else if (r.atendidaEm != null) r.status = StatusRequisicao.ATENDIDA;
        else if (r.enviadaEm != null) r.status = StatusRequisicao.EM_TRANSITO;
        else if (r.alocadaEm != null) r.status = StatusRequisicao.ALOCADA;
        else if (r.aceitaEm != null) r.status = StatusRequisicao.ACEITA;
        else r.status = StatusRequisicao.ABERTA;

        if (r.status == StatusRequisicao.RECUSADA) {
            r.motivoRecusa = r.motivoRecusa == null ? MOTIVOS_RECUSA[0] : r.motivoRecusa;
        } else {
            r.motivoRecusa = null;
        }
    }

    static boolean depois(OffsetDateTime t) {
        return t != null && t.isAfter(REFERENCIA);
    }

    static int quantidade(Requisicao r, Random rnd) {
        if (r.tipo == TipoHemocomponente.CRIOPRECIPITADO) {
            return 4 + rnd.nextInt(5); // crio é transfundido em "pool" de várias unidades
        }
        int base = sortearPorPeso(List.of(1, 2, 3, 4), q -> switch (q) {
            case 1 -> 0.35;
            case 2 -> 0.35;
            case 3 -> 0.20;
            default -> 0.10;
        }, rnd);
        return r.prioridade == Prioridade.EMERGENCIA ? base + 1 + rnd.nextInt(2) : base;
    }

    static long esperaAceite(Requisicao r, Random rnd) {
        int hora = r.criadaEm.getHour();
        boolean madrugada = hora >= 22 || hora < 6;
        if (r.prioridade == Prioridade.NORMAL && madrugada && rnd.nextDouble() < 0.7) {
            // pedido NORMAL feito de madrugada espera a equipe do turno das 7h
            OffsetDateTime seteHoras = r.criadaEm.withHour(7).withMinute(0).withSecond(0);
            if (!seteHoras.isAfter(r.criadaEm)) {
                seteHoras = seteHoras.plusDays(1);
            }
            return Duration.between(r.criadaEm, seteHoras).toMinutes() + lognormal(20, 0.6, rnd);
        }
        return lognormal(mediana(r.prioridade, 6, 20, 90), sigma(r.prioridade, 0.5, 0.6, 0.8), rnd);
    }

    static long tempoTransporte(Requisicao r, Random rnd) {
        double km = distanciaKm(r.hemocentro, r.hospital);
        double minutos = (10 + 1.6 * km) * Math.exp(0.25 * rnd.nextGaussian());
        if (rnd.nextDouble() < 0.03) {
            minutos += 45 + rnd.nextDouble() * 135; // incidente de trânsito / veículo
        }
        return Math.max(5, Math.round(minutos));
    }

    static double mediana(Prioridade p, double emergencia, double urgencia, double normal) {
        return switch (p) {
            case EMERGENCIA -> emergencia;
            case URGENCIA -> urgencia;
            case NORMAL -> normal;
        };
    }

    static double sigma(Prioridade p, double emergencia, double urgencia, double normal) {
        return mediana(p, emergencia, urgencia, normal);
    }

    // ---------------------------------------------------------------- hemocomponentes
    static List<Hemocomponente> gerarHemocomponentes(List<Requisicao> requisicoes, Random rnd) {
        List<Hemocomponente> lista = new ArrayList<>();
        LocalDate hoje = REFERENCIA.toLocalDate();

        // 1) Bolsas vinculadas às requisições já alocadas: RESERVADO (no hemocentro) ou TRANSFUNDIDO (no hospital).
        for (Requisicao r : requisicoes) {
            if (r.alocadaEm == null) {
                continue;
            }
            for (int i = 0; i < r.quantidade; i++) {
                Hemocomponente h = new Hemocomponente();
                h.tipo = r.tipo;
                TipoSanguineo doador = doadorCompativel(r, rnd);
                h.abo = doador.abo();
                h.rh = doador.rh();
                int validade = validadeDias(r.tipo);
                int idadeMax = Math.min(validade - 1, 30);
                h.dataColeta = r.alocadaEm.toLocalDate().minusDays(rnd.nextInt(idadeMax + 1));
                h.dataValidade = h.dataColeta.plusDays(validade);
                h.requisicaoId = r.id;
                if (r.status == StatusRequisicao.ATENDIDA) {
                    h.status = StatusHemocomponente.TRANSFUNDIDO;
                    h.instituicaoId = r.hospital.id();
                } else {
                    h.status = StatusHemocomponente.RESERVADO;
                    h.instituicaoId = r.hemocentro.id();
                }
                lista.add(h);
            }
        }

        // 2) Estoque livre dos hemocentros. Tipos raros com oferta abaixo da proporção populacional.
        estoqueLivre(lista, 1, 420, rnd, hoje);
        estoqueLivre(lista, 2, 180, rnd, hoje);

        // 3) Descartes do período (vencimento, quebra de cadeia de frio, descarte manual).
        for (int i = 0; i < 300; i++) {
            Hemocomponente h = novaBolsaLivre(i % 10 < 7 ? 1 : 2, rnd);
            h.dataColeta = hoje.minusDays(validadeDias(h.tipo) >= 35 ? 5 + rnd.nextInt(PERIODO_DIAS) : rnd.nextInt(PERIODO_DIAS));
            h.dataValidade = h.dataColeta.plusDays(validadeDias(h.tipo));
            h.status = StatusHemocomponente.DESCARTADO;
            lista.add(h);
        }

        lista.sort(Comparator.comparing((Hemocomponente h) -> h.dataColeta).thenComparing(h -> h.tipo));
        for (int i = 0; i < lista.size(); i++) {
            lista.get(i).id = i + 1L;
        }
        return lista;
    }

    static void estoqueLivre(List<Hemocomponente> lista, long hemocentroId, int total, Random rnd, LocalDate hoje) {
        for (int i = 0; i < total; i++) {
            Hemocomponente h = novaBolsaLivre(hemocentroId, rnd);
            int validade = validadeDias(h.tipo);
            int idade = rnd.nextDouble() < 0.04
                    ? validade + 1 + rnd.nextInt(4)                   // vencida e ainda não descartada
                    : rnd.nextInt(Math.min(validade, 120));           // dentro da validade
            h.dataColeta = hoje.minusDays(idade);
            h.dataValidade = h.dataColeta.plusDays(validade);
            h.status = StatusHemocomponente.DISPONIVEL;
            lista.add(h);
        }
    }

    static Hemocomponente novaBolsaLivre(long hemocentroId, Random rnd) {
        Hemocomponente h = new Hemocomponente();
        h.tipo = sortearPorPeso(List.of(TipoHemocomponente.values()), GeradorDadosEst::proporcao, rnd);
        TipoSanguineo t = sortearPorPeso(TIPOS_SANGUINEOS, GeradorDadosEst::ofertaRelativa, rnd);
        h.abo = t.abo();
        h.rh = t.rh();
        h.instituicaoId = hemocentroId;
        return h;
    }

    /** Oferta de doação: tipos negativos chegam abaixo da proporção populacional (cenário de risco). */
    static double ofertaRelativa(TipoSanguineo t) {
        double fator = switch (t.rotulo()) {
            case "O-" -> 0.60;
            case "B-" -> 0.80;
            case "AB-" -> 0.90;
            default -> 1.0;
        };
        return t.frequencia() * fator;
    }

    /** 90% mesmo tipo; 10% doador compatível (tabela didática de docs/api/dominio.md). */
    static TipoSanguineo doadorCompativel(Requisicao r, Random rnd) {
        if (rnd.nextDouble() >= 0.10) {
            return r.sangue;
        }
        List<TipoSanguineo> compativeis = TIPOS_SANGUINEOS.stream().filter(d -> compativel(d, r.sangue)).toList();
        return compativeis.get(rnd.nextInt(compativeis.size()));
    }

    static boolean compativel(TipoSanguineo doador, TipoSanguineo receptor) {
        boolean abo = switch (receptor.abo()) {
            case O -> doador.abo() == TipoABO.O;
            case A -> doador.abo() == TipoABO.A || doador.abo() == TipoABO.O;
            case B -> doador.abo() == TipoABO.B || doador.abo() == TipoABO.O;
            case AB -> true;
        };
        boolean rh = receptor.rh() == FatorRh.POSITIVO || doador.rh() == FatorRh.NEGATIVO;
        return abo && rh;
    }

    // ---------------------------------------------------------------- escrita dos CSV
    static void escreverInstituicoes(Path arquivo) throws IOException {
        try (PrintWriter out = abrir(arquivo)) {
            out.println("id,razaoSocial,tipo,municipio,latitude,longitude");
            for (Instituicao i : INSTITUICOES) {
                out.println(linha(i.id(), i.razaoSocial(), i.tipo(), i.municipio(),
                        String.format(Locale.ROOT, "%.4f", i.latitude()), String.format(Locale.ROOT, "%.4f", i.longitude())));
            }
        }
    }

    static void escreverRequisicoes(Path arquivo, List<Requisicao> lista) throws IOException {
        try (PrintWriter out = abrir(arquivo)) {
            out.println("id,hospitalId,hemocentroId,tipo,abo,rh,quantidade,prioridade,status,"
                    + "criadaEm,aceitaEm,alocadaEm,enviadaEm,atendidaEm,recusadaEm,canceladaEm,motivoRecusa,distanciaKm");
            for (Requisicao r : lista) {
                boolean temHemocentro = r.status != StatusRequisicao.ABERTA
                        && !(r.status == StatusRequisicao.CANCELADA && r.aceitaEm == null);
                out.println(linha(r.id, r.hospital.id(), temHemocentro ? r.hemocentro.id() : null,
                        r.tipo, r.sangue.abo(), r.sangue.rh(), r.quantidade, r.prioridade, r.status,
                        data(r.criadaEm), data(r.aceitaEm), data(r.alocadaEm), data(r.enviadaEm), data(r.atendidaEm),
                        data(r.recusadaEm), data(r.canceladaEm), r.motivoRecusa,
                        temHemocentro ? String.format(Locale.ROOT, "%.1f", distanciaKm(r.hemocentro, r.hospital)) : null));
            }
        }
    }

    static void escreverHemocomponentes(Path arquivo, List<Hemocomponente> lista) throws IOException {
        try (PrintWriter out = abrir(arquivo)) {
            out.println("id,codigoBolsa,tipo,abo,rh,dataColeta,dataValidade,status,instituicaoId,requisicaoId");
            for (Hemocomponente h : lista) {
                out.println(linha(h.id, String.format(Locale.ROOT, "BL-%06d", h.id), h.tipo, h.abo, h.rh,
                        h.dataColeta, h.dataValidade, h.status, h.instituicaoId, h.requisicaoId));
            }
        }
    }

    /** Uma leitura a cada 5 min durante o transporte (EM_TRANSITO até a entrega ou até REFERENCIA). */
    static int escreverTelemetria(Path arquivo, List<Requisicao> lista, Random rnd) throws IOException {
        int total = 0;
        try (PrintWriter out = abrir(arquivo)) {
            out.println("requisicaoId,instante,tipo,latitude,longitude,temperaturaC");
            for (Requisicao r : lista) {
                if (r.enviadaEm == null) {
                    continue;
                }
                OffsetDateTime fim = r.atendidaEm != null ? r.atendidaEm : REFERENCIA;
                long duracao = Math.max(1, Duration.between(r.enviadaEm, fim).toMinutes());
                double alvo = temperaturaAlvo(r.tipo);
                double ruido = ruidoTemperatura(r.tipo);
                double desvio = 0;

                // ~4% dos transportes têm uma excursão térmica (falha de refrigeração / porta aberta)
                boolean excursao = rnd.nextDouble() < 0.04;
                long inicioExc = excursao ? (long) (rnd.nextDouble() * duracao * 0.7) : -1;
                long duracaoExc = 15 + rnd.nextInt(20);
                double picoExc = (r.tipo == TipoHemocomponente.PLASMA || r.tipo == TipoHemocomponente.CRIOPRECIPITADO)
                        ? 12 + rnd.nextDouble() * 6 : 3.5 + rnd.nextDouble() * 3;

                for (long m = 0; m <= duracao; m += INTERVALO_TELEMETRIA_MIN) {
                    double frac = (double) m / duracao;
                    double lat = r.hemocentro.latitude() + frac * (r.hospital.latitude() - r.hemocentro.latitude())
                            + rnd.nextGaussian() * 0.0008;
                    double lon = r.hemocentro.longitude() + frac * (r.hospital.longitude() - r.hemocentro.longitude())
                            + rnd.nextGaussian() * 0.0008;
                    desvio = 0.7 * desvio + rnd.nextGaussian() * ruido * 0.7; // ruído com memória (AR(1))
                    double temp = alvo + desvio;
                    if (excursao && m >= inicioExc && m <= inicioExc + duracaoExc) {
                        temp += picoExc;
                    }
                    out.println(linha(r.id, data(r.enviadaEm.plusMinutes(m)), r.tipo,
                            String.format(Locale.ROOT, "%.5f", lat), String.format(Locale.ROOT, "%.5f", lon),
                            String.format(Locale.ROOT, "%.1f", temp)));
                    total++;
                }
            }
        }
        return total;
    }

    static PrintWriter abrir(Path arquivo) throws IOException {
        // UTF-8 e quebra de linha "\n" em qualquer sistema operacional (não depende da máquina de quem roda)
        return new PrintWriter(Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) {
            @Override
            public void println(String x) {
                write(x);
                write('\n');
            }
        };
    }

    static String linha(Object... campos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            Object c = campos[i];
            if (c == null) {
                continue; // campo vazio = nulo
            }
            String s = c.toString();
            if (s.contains(",") || s.contains("\"")) {
                s = "\"" + s.replace("\"", "\"\"") + "\"";
            }
            sb.append(s);
        }
        return sb.toString();
    }

    static String data(OffsetDateTime t) {
        return t == null ? null : FORMATO.format(t.withSecond(0).withNano(0));
    }

    // ---------------------------------------------------------------- utilitários numéricos
    static long lognormal(double mediana, double sigma, Random rnd) {
        return Math.max(1, Math.round(mediana * Math.exp(sigma * rnd.nextGaussian())));
    }

    static int poisson(double lambda, Random rnd) {
        double l = Math.exp(-lambda), p = 1;
        int k = 0;
        do {
            k++;
            p *= rnd.nextDouble();
        } while (p > l);
        return k - 1;
    }

    /** Minuto do dia: 75% dos pedidos entre 7h e 19h, o resto espalhado nas outras horas. */
    static long minutoDoDia(Random rnd) {
        if (rnd.nextDouble() < 0.75) {
            return 7 * 60 + rnd.nextInt(12 * 60);
        }
        int m = rnd.nextInt(12 * 60);
        return m < 7 * 60 ? m : m + 12 * 60;
    }

    static <T> T sortearPorPeso(List<T> itens, java.util.function.ToDoubleFunction<T> peso, Random rnd) {
        double soma = 0;
        for (T t : itens) {
            soma += peso.applyAsDouble(t);
        }
        double x = rnd.nextDouble() * soma;
        for (T t : itens) {
            x -= peso.applyAsDouble(t);
            if (x < 0) {
                return t;
            }
        }
        return itens.get(itens.size() - 1);
    }

    /** Distância por estrada estimada: linha reta (haversine) × 1,3. */
    static double distanciaKm(Instituicao a, Instituicao b) {
        double r = 6371.0;
        double dLat = Math.toRadians(b.latitude() - a.latitude());
        double dLon = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(a.latitude())) * Math.cos(Math.toRadians(b.latitude())) * Math.pow(Math.sin(dLon / 2), 2);
        return 1.3 * 2 * r * Math.asin(Math.sqrt(h));
    }
}
