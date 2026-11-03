import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

/**
 * PI2-107 — Medidas de tendência central, dispersão e separatrizes sobre a massa sintética (Unidade 1).
 *
 * Programa Java de arquivo único, fora do build do Maven. Rodar a partir da raiz do repositório (JDK 21),
 * depois do gerador:
 *
 *   java estatistica/GeradorDadosEst.java
 *   java estatistica/AnaliseDescritiva.java
 *
 * Lê estatistica/dados/*.csv e escreve:
 *   estatistica/saida/medidas-descritivas.csv   (números com ponto decimal, para planilha/gráficos)
 *   docs/est/medidas-descritivas.md             (tabela consolidada, formato brasileiro)
 */
public final class AnaliseDescritiva {

    /** Mesmo "agora" do gerador: o estoque é uma fotografia neste instante. */
    static final OffsetDateTime REFERENCIA = OffsetDateTime.parse("2026-09-30T15:00:00-03:00");
    static final Locale BR = Locale.forLanguageTag("pt-BR");

    /** Estoque mínimo de segurança por tipo (mesma tabela de frontend/src/lib/data.ts). */
    static final Map<String, Integer> MINIMO_POR_TIPO = new LinkedHashMap<>();
    static {
        MINIMO_POR_TIPO.put("O-", 20);
        MINIMO_POR_TIPO.put("O+", 30);
        MINIMO_POR_TIPO.put("A+", 25);
        MINIMO_POR_TIPO.put("A-", 12);
        MINIMO_POR_TIPO.put("B+", 15);
        MINIMO_POR_TIPO.put("B-", 8);
        MINIMO_POR_TIPO.put("AB+", 6);
        MINIMO_POR_TIPO.put("AB-", 4);
    }

    /** Faixa térmica permitida por hemocomponente, em °C (mesma do frontend). */
    static double[] faixa(String tipo) {
        return switch (tipo) {
            case "HEMACIAS" -> new double[] { 2, 6 };
            case "PLAQUETAS" -> new double[] { 20, 24 };
            default -> new double[] { -40, -20 }; // PLASMA, CRIOPRECIPITADO
        };
    }

    record Medidas(String grupo, String variavel, String unidade, int n, double media, double mediana, String moda,
                   double minimo, double maximo, double amplitude, double variancia, double desvioPadrao, double cv,
                   double q1, double q3, double aiq, double limiteInferior, double limiteSuperior, int outliers) {
    }

    public static void main(String[] args) throws IOException {
        Path base = args.length > 0 ? Path.of(args[0])
                : (Files.isDirectory(Path.of("estatistica")) ? Path.of("estatistica") : Path.of("."));
        Path raiz = base.toAbsolutePath().normalize().getParent();
        Path dados = base.resolve("dados");
        if (!Files.exists(dados.resolve("requisicoes.csv"))) {
            System.err.println("Arquivos de dados não encontrados. Rode antes: java estatistica/GeradorDadosEst.java");
            System.exit(1);
        }

        List<Map<String, String>> requisicoes = lerCsv(dados.resolve("requisicoes.csv"));
        List<Map<String, String>> hemocomponentes = lerCsv(dados.resolve("hemocomponentes.csv"));
        List<Map<String, String>> telemetria = lerCsv(dados.resolve("telemetria.csv"));

        List<Medidas> tabela = new ArrayList<>();
        String[] prioridades = { "EMERGENCIA", "URGENCIA", "NORMAL" };

        // ------------------------------------------------ tempos de atendimento (requisições ATENDIDA)
        List<Map<String, String>> atendidas = filtrar(requisicoes, r -> r.get("status").equals("ATENDIDA"));
        tabela.add(medir("Tempos de atendimento", "Tempo total de atendimento (criação → entrega) — geral", "min",
                minutos(atendidas, "criadaEm", "atendidaEm")));
        for (String p : prioridades) {
            tabela.add(medir("Tempos de atendimento", "Tempo total de atendimento — " + p, "min",
                    minutos(filtrar(atendidas, r -> r.get("prioridade").equals(p)), "criadaEm", "atendidaEm")));
        }
        List<Map<String, String>> aceitas = filtrar(requisicoes, r -> !r.get("aceitaEm").isEmpty());
        for (String p : prioridades) {
            tabela.add(medir("Tempos de atendimento", "Espera até o aceite do hemocentro — " + p, "min",
                    minutos(filtrar(aceitas, r -> r.get("prioridade").equals(p)), "criadaEm", "aceitaEm")));
        }
        tabela.add(medir("Tempos de atendimento", "Tempo de transporte (envio → entrega)", "min",
                minutos(atendidas, "enviadaEm", "atendidaEm")));

        // ------------------------------------------------ demanda
        Map<LocalDate, Integer> porDia = new TreeMap<>();
        LocalDate primeiro = OffsetDateTime.parse(requisicoes.get(0).get("criadaEm")).toLocalDate();
        for (LocalDate d = primeiro; !d.isAfter(REFERENCIA.toLocalDate()); d = d.plusDays(1)) {
            porDia.put(d, 0);
        }
        for (Map<String, String> r : requisicoes) {
            porDia.merge(OffsetDateTime.parse(r.get("criadaEm")).toLocalDate(), 1, Integer::sum);
        }
        tabela.add(medir("Demanda", "Requisições por dia", "requisições/dia",
                porDia.values().stream().mapToDouble(Integer::doubleValue).toArray()));
        Map<LocalDate, Integer> bolsasPorDia = new TreeMap<>();
        porDia.keySet().forEach(d -> bolsasPorDia.put(d, 0));
        for (Map<String, String> r : requisicoes) {
            bolsasPorDia.merge(OffsetDateTime.parse(r.get("criadaEm")).toLocalDate(),
                    Integer.parseInt(r.get("quantidade")), Integer::sum);
        }
        tabela.add(medir("Demanda", "Bolsas solicitadas por dia", "bolsas/dia",
                bolsasPorDia.values().stream().mapToDouble(Integer::doubleValue).toArray()));
        tabela.add(medir("Demanda", "Bolsas por requisição", "bolsas",
                requisicoes.stream().mapToDouble(r -> Double.parseDouble(r.get("quantidade"))).toArray()));

        // ------------------------------------------------ estoque (fotografia em REFERENCIA)
        LocalDate hoje = REFERENCIA.toLocalDate();
        List<Map<String, String>> elegiveis = filtrar(hemocomponentes, h -> h.get("status").equals("DISPONIVEL")
                && !LocalDate.parse(h.get("dataValidade")).isBefore(hoje));
        Map<String, Integer> estoquePorTipo = new LinkedHashMap<>();
        MINIMO_POR_TIPO.keySet().forEach(t -> estoquePorTipo.put(t, 0));
        for (Map<String, String> h : elegiveis) {
            estoquePorTipo.merge(rotulo(h), 1, Integer::sum);
        }
        tabela.add(medir("Estoque", "Bolsas disponíveis por tipo sanguíneo (8 tipos)", "bolsas",
                estoquePorTipo.values().stream().mapToDouble(Integer::doubleValue).toArray()));
        for (String comp : new String[] { "HEMACIAS", "PLAQUETAS", "PLASMA", "CRIOPRECIPITADO" }) {
            double[] dias = filtrar(elegiveis, h -> h.get("tipo").equals(comp)).stream()
                    .mapToDouble(h -> Duration.between(hoje.atStartOfDay(), LocalDate.parse(h.get("dataValidade")).atStartOfDay()).toDays())
                    .toArray();
            tabela.add(medir("Estoque", "Dias até o vencimento das bolsas disponíveis — " + comp, "dias", dias));
        }

        // ------------------------------------------------ cadeia fria (telemetria)
        for (String comp : new String[] { "HEMACIAS", "PLAQUETAS", "PLASMA", "CRIOPRECIPITADO" }) {
            double[] temps = filtrar(telemetria, t -> t.get("tipo").equals(comp)).stream()
                    .mapToDouble(t -> Double.parseDouble(t.get("temperaturaC"))).toArray();
            tabela.add(medir("Cadeia fria", "Temperatura no transporte — " + comp, "°C", temps));
        }

        // ------------------------------------------------ contagens de apoio aos indicadores (PI2-105)
        Map<String, String> apoio = new LinkedHashMap<>();
        Map<String, Integer> porStatus = contar(requisicoes, "status");
        int total = requisicoes.size();
        int finalizadas = porStatus.getOrDefault("ATENDIDA", 0) + porStatus.getOrDefault("RECUSADA", 0)
                + porStatus.getOrDefault("CANCELADA", 0);
        apoio.put("Requisições no período", inteiro(total));
        for (String s : new String[] { "ABERTA", "ACEITA", "ALOCADA", "EM_TRANSITO", "ATENDIDA", "RECUSADA", "CANCELADA" }) {
            apoio.put("Requisições " + s + (s.equals("ABERTA") || s.equals("ACEITA") || s.equals("ALOCADA")
                    || s.equals("EM_TRANSITO") ? " (em aberto agora)" : ""), inteiro(porStatus.getOrDefault(s, 0)));
        }
        apoio.put("Taxa de atendimento (ATENDIDA ÷ finalizadas)", pct(porStatus.getOrDefault("ATENDIDA", 0), finalizadas));
        apoio.put("Taxa de recusa (RECUSADA ÷ finalizadas)", pct(porStatus.getOrDefault("RECUSADA", 0), finalizadas));
        contar(requisicoes, "tipo").forEach((t, n) -> apoio.put("Demanda de " + t + " (requisições)", inteiro(n) + " (" + pct(n, total) + ")"));
        apoio.put("Bolsas disponíveis (elegíveis) agora", inteiro(elegiveis.size()));
        estoquePorTipo.forEach((t, n) -> apoio.put("Estoque " + t + " (mínimo " + MINIMO_POR_TIPO.get(t) + ")",
                inteiro(n) + " → cobertura " + pct(n, MINIMO_POR_TIPO.get(t))));
        long vencendo7 = elegiveis.stream().filter(h -> !LocalDate.parse(h.get("dataValidade")).isAfter(hoje.plusDays(7))).count();
        long vencidasNaoDescartadas = hemocomponentes.stream().filter(h -> h.get("status").equals("DISPONIVEL")
                && LocalDate.parse(h.get("dataValidade")).isBefore(hoje)).count();
        long descartadas = contar(hemocomponentes, "status").getOrDefault("DESCARTADO", 0);
        apoio.put("Bolsas disponíveis que vencem em até 7 dias", inteiro((int) vencendo7));
        apoio.put("Bolsas vencidas ainda não descartadas", inteiro((int) vencidasNaoDescartadas));
        apoio.put("Taxa de descarte (DESCARTADO ÷ total de bolsas)", pct((int) descartadas, hemocomponentes.size()));
        long foraFaixa = telemetria.stream().filter(t -> {
            double[] f = faixa(t.get("tipo"));
            double v = Double.parseDouble(t.get("temperaturaC"));
            return v < f[0] || v > f[1];
        }).count();
        long transportesComAlerta = telemetria.stream().filter(t -> {
            double[] f = faixa(t.get("tipo"));
            double v = Double.parseDouble(t.get("temperaturaC"));
            return v < f[0] || v > f[1];
        }).map(t -> t.get("requisicaoId")).distinct().count();
        long transportes = telemetria.stream().map(t -> t.get("requisicaoId")).distinct().count();
        apoio.put("Leituras de temperatura fora da faixa", inteiro((int) foraFaixa) + " de " + inteiro(telemetria.size())
                + " (" + pct((int) foraFaixa, telemetria.size()) + ")");
        apoio.put("Transportes com ao menos um alerta térmico", inteiro((int) transportesComAlerta) + " de "
                + inteiro((int) transportes) + " (" + pct((int) transportesComAlerta, (int) transportes) + ")");

        Path saida = base.resolve("saida");
        Files.createDirectories(saida);
        escreverCsv(saida.resolve("medidas-descritivas.csv"), tabela);
        Path docs = raiz.resolve("docs").resolve("est");
        Files.createDirectories(docs);
        escreverMarkdown(docs.resolve("medidas-descritivas.md"), tabela, apoio, requisicoes.size(),
                hemocomponentes.size(), telemetria.size());

        System.out.println("Tabela gerada:");
        System.out.println("  " + saida.resolve("medidas-descritivas.csv").toAbsolutePath().normalize());
        System.out.println("  " + docs.resolve("medidas-descritivas.md").toAbsolutePath().normalize());
    }

    // ---------------------------------------------------------------- estatística descritiva
    static Medidas medir(String grupo, String variavel, String unidade, double[] valores) {
        double[] v = valores.clone();
        Arrays.sort(v);
        int n = v.length;
        if (n == 0) {
            return new Medidas(grupo, variavel, unidade, 0, Double.NaN, Double.NaN, "—", Double.NaN, Double.NaN,
                    Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN,
                    Double.NaN, 0);
        }
        double soma = 0;
        for (double x : v) soma += x;
        double media = soma / n;
        double sq = 0;
        for (double x : v) sq += (x - media) * (x - media);
        double variancia = n > 1 ? sq / (n - 1) : 0; // variância amostral (divisor n − 1)
        double dp = Math.sqrt(variancia);
        double q1 = quantil(v, 0.25), q2 = quantil(v, 0.50), q3 = quantil(v, 0.75);
        double aiq = q3 - q1;
        double li = q1 - 1.5 * aiq, ls = q3 + 1.5 * aiq;
        int outliers = 0;
        for (double x : v) if (x < li || x > ls) outliers++;
        return new Medidas(grupo, variavel, unidade, n, media, q2, moda(v), v[0], v[n - 1], v[n - 1] - v[0],
                variancia, dp, media != 0 ? dp / Math.abs(media) * 100 : Double.NaN, q1, q3, aiq, li, ls, outliers);
    }

    /** Quantil por interpolação linear, posição (n − 1)·p — igual a QUARTIL.INC do Excel e QUARTILE do Sheets. */
    static double quantil(double[] ordenado, double p) {
        double pos = (ordenado.length - 1) * p;
        int i = (int) Math.floor(pos);
        double frac = pos - i;
        return i + 1 < ordenado.length ? ordenado[i] + frac * (ordenado[i + 1] - ordenado[i]) : ordenado[i];
    }

    /** Moda dos valores (dados já em unidades inteiras ou com 1 casa). Empates viram "multimodal". */
    static String moda(double[] ordenado) {
        Map<Double, Integer> freq = new HashMap<>();
        for (double x : ordenado) freq.merge(x, 1, Integer::sum);
        int max = freq.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (max <= 1) {
            return "amodal";
        }
        List<Double> modas = freq.entrySet().stream().filter(e -> e.getValue() == max).map(Map.Entry::getKey).sorted().toList();
        if (modas.size() > 3) {
            return "multimodal (" + modas.size() + " valores, f = " + max + ")";
        }
        StringBuilder sb = new StringBuilder();
        for (double m : modas) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(numero(m));
        }
        return sb + " (f = " + max + ")";
    }

    static double[] minutos(List<Map<String, String>> linhas, String de, String ate) {
        return linhas.stream().mapToDouble(r -> Duration.between(OffsetDateTime.parse(r.get(de)),
                OffsetDateTime.parse(r.get(ate))).toMinutes()).toArray();
    }

    // ---------------------------------------------------------------- leitura
    /** Leitor de CSV simples: vírgula como separador, campos entre aspas quando contêm vírgula. */
    static List<Map<String, String>> lerCsv(Path arquivo) throws IOException {
        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        String[] cab = separar(linhas.get(0));
        List<Map<String, String>> out = new ArrayList<>(linhas.size());
        for (int i = 1; i < linhas.size(); i++) {
            if (linhas.get(i).isBlank()) continue;
            String[] c = separar(linhas.get(i));
            Map<String, String> m = new HashMap<>();
            for (int j = 0; j < cab.length; j++) {
                m.put(cab[j], j < c.length ? c[j] : "");
            }
            out.add(m);
        }
        return out;
    }

    static String[] separar(String linha) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean aspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char ch = linha.charAt(i);
            if (aspas) {
                if (ch == '"' && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                    atual.append('"');
                    i++;
                } else if (ch == '"') {
                    aspas = false;
                } else {
                    atual.append(ch);
                }
            } else if (ch == '"') {
                aspas = true;
            } else if (ch == ',') {
                campos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(ch);
            }
        }
        campos.add(atual.toString());
        return campos.toArray(String[]::new);
    }

    static List<Map<String, String>> filtrar(List<Map<String, String>> l, Predicate<Map<String, String>> p) {
        return l.stream().filter(p).toList();
    }

    static Map<String, Integer> contar(List<Map<String, String>> l, String campo) {
        Map<String, Integer> m = new TreeMap<>();
        for (Map<String, String> r : l) m.merge(r.get(campo), 1, Integer::sum);
        return m;
    }

    static String rotulo(Map<String, String> h) {
        return h.get("abo") + (h.get("rh").equals("POSITIVO") ? "+" : "-");
    }

    // ---------------------------------------------------------------- escrita
    static void escreverCsv(Path arquivo, List<Medidas> tabela) throws IOException {
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8))) {
            out.print("grupo,variavel,unidade,n,media,mediana,moda,minimo,maximo,amplitude,variancia,desvioPadrao,"
                    + "cvPercentual,q1,q3,aiq,limiteInferior,limiteSuperior,outliers\n");
            for (Medidas m : tabela) {
                out.print(String.join(",", aspas(m.grupo()), aspas(m.variavel()), aspas(m.unidade()), String.valueOf(m.n()),
                        d(m.media()), d(m.mediana()), aspas(m.moda()), d(m.minimo()), d(m.maximo()), d(m.amplitude()),
                        d(m.variancia()), d(m.desvioPadrao()), d(m.cv()), d(m.q1()), d(m.q3()), d(m.aiq()),
                        d(m.limiteInferior()), d(m.limiteSuperior()), String.valueOf(m.outliers())) + "\n");
            }
        }
    }

    static void escreverMarkdown(Path arquivo, List<Medidas> tabela, Map<String, String> apoio,
                                 int nReq, int nHemo, int nTel) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# EST — Medidas descritivas (PI2-107)\n\n");
        sb.append("> Arquivo **gerado** por `estatistica/AnaliseDescritiva.java`. Não edite à mão: rode de novo o programa.\n\n");
        sb.append("Base: massa sintética de `estatistica/dados/` (").append(inteiro(nReq)).append(" requisições, ")
                .append(inteiro(nHemo)).append(" hemocomponentes, ").append(inteiro(nTel))
                .append(" leituras de telemetria). Fotografia do estoque em 30/09/2026, 15h (horário de Recife).\n\n");
        sb.append("## Como ler\n\n");
        sb.append("- **Variância e desvio padrão** são amostrais (divisor *n − 1*). **CV** = desvio padrão ÷ média × 100.\n");
        sb.append("- **Quartis** por interpolação linear na posição *(n − 1)·p* — o mesmo de `QUARTIL.INC` no Excel e `QUARTILE` no Google Sheets.\n");
        sb.append("- **Outliers**: valores fora de [Q1 − 1,5·AIQ ; Q3 + 1,5·AIQ], com AIQ = Q3 − Q1 (critério do boxplot). Limite inferior negativo em tempos e contagens significa que não há outlier possível por baixo.\n");
        sb.append("- **Moda** sobre os valores como estão (minutos inteiros, contagens, °C com 1 casa); *f* é a frequência. Empates com mais de 3 valores aparecem como multimodal.\n");
        sb.append("- Os mesmos números, com ponto decimal, estão em `estatistica/saida/medidas-descritivas.csv` (para planilhas e para os gráficos da PI2-108).\n\n");

        String grupoAtual = "";
        for (Medidas m : tabela) {
            if (!m.grupo().equals(grupoAtual)) {
                grupoAtual = m.grupo();
                sb.append("## ").append(grupoAtual).append("\n\n");
                sb.append("### Tendência central e dispersão\n\n");
                sb.append("| Variável | Unid. | n | Média | Mediana | Moda | Mín. | Máx. | Amplitude | Variância | Desvio padrão | CV |\n");
                sb.append("|---|---|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|\n");
                for (Medidas x : tabela) {
                    if (!x.grupo().equals(grupoAtual)) continue;
                    sb.append("| ").append(x.variavel()).append(" | ").append(x.unidade()).append(" | ").append(inteiro(x.n()))
                            .append(" | ").append(numero(x.media())).append(" | ").append(numero(x.mediana()))
                            .append(" | ").append(x.moda()).append(" | ").append(numero(x.minimo()))
                            .append(" | ").append(numero(x.maximo())).append(" | ").append(numero(x.amplitude()))
                            .append(" | ").append(numero(x.variancia())).append(" | ").append(numero(x.desvioPadrao()))
                            .append(" | ").append(Double.isNaN(x.cv()) ? "—" : numero(x.cv()) + "%").append(" |\n");
                }
                sb.append("\n### Separatrizes e outliers\n\n");
                sb.append("| Variável | Q1 | Q2 (mediana) | Q3 | AIQ | Limite inferior | Limite superior | Outliers |\n");
                sb.append("|---|---:|---:|---:|---:|---:|---:|---:|\n");
                for (Medidas x : tabela) {
                    if (!x.grupo().equals(grupoAtual)) continue;
                    sb.append("| ").append(x.variavel()).append(" | ").append(numero(x.q1())).append(" | ")
                            .append(numero(x.mediana())).append(" | ").append(numero(x.q3())).append(" | ")
                            .append(numero(x.aiq())).append(" | ").append(numero(x.limiteInferior())).append(" | ")
                            .append(numero(x.limiteSuperior())).append(" | ").append(inteiro(x.outliers()))
                            .append(x.n() > 0 ? " (" + pct(x.outliers(), x.n()) + ")" : "").append(" |\n");
                }
                sb.append("\n");
            }
        }
        sb.append("## Contagens de apoio aos indicadores (PI2-105)\n\n");
        sb.append("| Indicador | Valor |\n|---|---|\n");
        apoio.forEach((k, v) -> sb.append("| ").append(k).append(" | ").append(v).append(" |\n"));
        Files.writeString(arquivo, sb.toString(), StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------- formatação (independe do idioma da máquina)
    static String numero(double x) {
        if (Double.isNaN(x)) return "—";
        return String.format(BR, "%,.2f", x);
    }

    static String inteiro(int x) {
        return String.format(BR, "%,d", x);
    }

    static String pct(int parte, int todo) {
        return todo == 0 ? "—" : String.format(BR, "%.1f%%", 100.0 * parte / todo);
    }

    static String d(double x) {
        return Double.isNaN(x) ? "" : String.format(Locale.ROOT, "%.4f", x);
    }

    static String aspas(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
