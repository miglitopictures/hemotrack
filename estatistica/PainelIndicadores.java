import java.io.IOException;
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
 * PI2-108 — Gráficos e painel inicial de indicadores (HU09), Unidade 1.
 *
 * Programa Java de arquivo único, fora do build do Maven. Rodar a partir da raiz do repositório (JDK 21),
 * depois do gerador de dados:
 *
 *   java estatistica/GeradorDadosEst.java
 *   java estatistica/PainelIndicadores.java
 *
 * Lê estatistica/dados/*.csv e escreve:
 *   docs/est/graficos/*.svg          (um arquivo por gráfico; o GitHub mostra direto no Markdown)
 *   docs/est/painel-indicadores.html (painel completo: cards, gráficos e tabelas; abre no navegador, sem internet)
 *
 * Os indicadores seguem docs/est/indicadores.md (códigos E1…T4). Gráficos em SVG puro, sem bibliotecas.
 */
public final class PainelIndicadores {

    static final OffsetDateTime REFERENCIA = OffsetDateTime.parse("2026-09-30T15:00:00-03:00");
    static final Locale BR = Locale.forLanguageTag("pt-BR");

    // Paleta categórica validada (ordem fixa) e tinta neutra — contraste e daltonismo conferidos.
    static final String AZUL = "#2a78d6", LARANJA = "#eb6834", VERDE_AGUA = "#1baf7a", AMARELO = "#eda100";
    static final String SUPERFICIE = "#fcfcfb", TINTA = "#0b0b0b", TINTA_2 = "#52514e", TINTA_MUDA = "#898781";
    static final String GRADE = "#e1e0d9", EIXO = "#c3c2b7", CRITICO = "#d03b3b", ATENCAO = "#fab219", OK = "#0ca30c";
    static final String FONTE = "system-ui, -apple-system, 'Segoe UI', Roboto, Arial, sans-serif";

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

    static final Map<String, String> NOME_COMPONENTE = Map.of(
            "HEMACIAS", "Hemácias", "PLAQUETAS", "Plaquetas", "PLASMA", "Plasma", "CRIOPRECIPITADO", "Crioprecipitado");
    static final Map<String, String> NOME_PRIORIDADE = Map.of(
            "EMERGENCIA", "Emergência", "URGENCIA", "Urgência", "NORMAL", "Normal");

    /** Um gráfico pronto: arquivo SVG + tabela equivalente (acessibilidade) para o painel. */
    record Grafico(String arquivo, String titulo, String subtitulo, String svg, String tabelaHtml) {
    }

    record Caixa(String rotulo, int n, double min, double q1, double mediana, double q3, double max, double media,
                 double bigodeInf, double bigodeSup, double[] outliers) {
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
        List<Map<String, String>> req = lerCsv(dados.resolve("requisicoes.csv"));
        List<Map<String, String>> hemo = lerCsv(dados.resolve("hemocomponentes.csv"));
        List<Map<String, String>> tel = lerCsv(dados.resolve("telemetria.csv"));

        LocalDate hoje = REFERENCIA.toLocalDate();
        List<Map<String, String>> elegiveis = filtrar(hemo, h -> h.get("status").equals("DISPONIVEL")
                && !LocalDate.parse(h.get("dataValidade")).isBefore(hoje));
        Map<String, Integer> porStatus = contar(req, "status");
        int finalizadas = porStatus.getOrDefault("ATENDIDA", 0) + porStatus.getOrDefault("RECUSADA", 0)
                + porStatus.getOrDefault("CANCELADA", 0);
        long vencendo7 = elegiveis.stream().filter(h -> !LocalDate.parse(h.get("dataValidade")).isAfter(hoje.plusDays(7))).count();
        long transportes = tel.stream().map(t -> t.get("requisicaoId")).distinct().count();
        long comAlerta = tel.stream().filter(PainelIndicadores::foraDaFaixa).map(t -> t.get("requisicaoId")).distinct().count();

        // ---------------------------------------------------------------- cards (E1, D1, E3, D4, T4)
        List<String[]> cards = List.of(
                new String[] { "Bolsas disponíveis", inteiro(elegiveis.size()), "E1 · elegíveis agora, nos 2 hemocentros", "" },
                new String[] { "Pendentes", inteiro(porStatus.getOrDefault("ABERTA", 0)), "D1 · status ABERTA agora", "" },
                new String[] { "Em transporte", inteiro(porStatus.getOrDefault("EM_TRANSITO", 0)), "D1 · status EM_TRANSITO agora", "" },
                new String[] { "Entregues", inteiro(porStatus.getOrDefault("ATENDIDA", 0)), "D1 · status ATENDIDA, últimos 90 dias", "" },
                new String[] { "Vencem em até 7 dias", inteiro((int) vencendo7), "E3 · priorizar na alocação (FEFO)", "atencao" },
                new String[] { "Taxa de atendimento", pct(porStatus.getOrDefault("ATENDIDA", 0), finalizadas),
                        "D4 · atendidas ÷ finalizadas", "" },
                new String[] { "Transportes com alerta térmico", pct((int) comAlerta, (int) transportes),
                        "T4 · " + inteiro((int) comAlerta) + " de " + inteiro((int) transportes) + " transportes", "atencao" });

        List<Grafico> graficos = new ArrayList<>();

        // ---------------------------------------------------------------- E2 estoque por tipo × mínimo
        Map<String, Integer> estoque = new LinkedHashMap<>();
        MINIMO_POR_TIPO.keySet().forEach(t -> estoque.put(t, 0));
        for (Map<String, String> h : elegiveis) estoque.merge(rotulo(h), 1, Integer::sum);
        graficos.add(graficoEstoque(estoque));

        // ---------------------------------------------------------------- D2 componentes mais solicitados
        Map<String, Integer> demanda = new LinkedHashMap<>();
        for (String c : new String[] { "HEMACIAS", "PLAQUETAS", "PLASMA", "CRIOPRECIPITADO" }) {
            demanda.put(c, (int) req.stream().filter(r -> r.get("tipo").equals(c)).count());
        }
        graficos.add(graficoDonut(demanda));

        // ---------------------------------------------------------------- T1 / T2 boxplots por prioridade
        String[] prioridades = { "EMERGENCIA", "URGENCIA", "NORMAL" };
        List<Map<String, String>> atendidas = filtrar(req, r -> r.get("status").equals("ATENDIDA"));
        List<Caixa> caixasT1 = new ArrayList<>();
        List<Caixa> caixasT2 = new ArrayList<>();
        for (String p : prioridades) {
            caixasT1.add(caixa(NOME_PRIORIDADE.get(p), minutos(filtrar(atendidas, r -> r.get("prioridade").equals(p)), "criadaEm", "atendidaEm")));
            caixasT2.add(caixa(NOME_PRIORIDADE.get(p), minutos(filtrar(req, r -> r.get("prioridade").equals(p)
                    && !r.get("aceitaEm").isEmpty()), "criadaEm", "aceitaEm")));
        }
        graficos.add(graficoBoxplot("03-boxplot-tempo-atendimento", "Tempo total de atendimento por prioridade",
                "T1 · da criação da requisição até a entrega, em minutos (requisições ATENDIDA)", caixasT1, "minutos"));
        graficos.add(graficoBoxplot("04-boxplot-espera-aceite", "Espera até o aceite do hemocentro, por prioridade",
                "T2 · da criação até o aceite, em minutos", caixasT2, "minutos"));

        // ---------------------------------------------------------------- T3 histograma do transporte
        double[] transporte = minutos(atendidas, "enviadaEm", "atendidaEm");
        graficos.add(graficoHistograma(transporte));

        // ---------------------------------------------------------------- D3 requisições por dia
        Map<LocalDate, Integer> porDia = new TreeMap<>();
        LocalDate primeiro = OffsetDateTime.parse(req.get(0).get("criadaEm")).toLocalDate();
        for (LocalDate d = primeiro; !d.isAfter(hoje); d = d.plusDays(1)) porDia.put(d, 0);
        for (Map<String, String> r : req) porDia.merge(OffsetDateTime.parse(r.get("criadaEm")).toLocalDate(), 1, Integer::sum);
        graficos.add(graficoPorDia(porDia));

        // ---------------------------------------------------------------- E3 prazo até o vencimento
        String[] faixas = { "vencida*", "0–3", "4–7", "8–14", "15–30", "31–90", "> 90" };
        Map<String, Integer> porFaixa = new LinkedHashMap<>();
        for (String f : faixas) porFaixa.put(f, 0);
        for (Map<String, String> h : hemo) {
            if (!h.get("status").equals("DISPONIVEL")) continue;
            long dias = Duration.between(hoje.atStartOfDay(), LocalDate.parse(h.get("dataValidade")).atStartOfDay()).toDays();
            String f = dias < 0 ? "vencida*" : dias <= 3 ? "0–3" : dias <= 7 ? "4–7" : dias <= 14 ? "8–14"
                    : dias <= 30 ? "15–30" : dias <= 90 ? "31–90" : "> 90";
            porFaixa.merge(f, 1, Integer::sum);
        }
        graficos.add(graficoVencimento(porFaixa));

        // ---------------------------------------------------------------- escrita
        Path docs = raiz.resolve("docs").resolve("est");
        Path pasta = docs.resolve("graficos");
        Files.createDirectories(pasta);
        for (Grafico g : graficos) {
            Files.writeString(pasta.resolve(g.arquivo() + ".svg"), g.svg(), StandardCharsets.UTF_8);
        }
        Files.writeString(docs.resolve("painel-indicadores.html"), painelHtml(cards, graficos, req.size(), hemo.size(), tel.size()),
                StandardCharsets.UTF_8);

        System.out.println("Painel gerado:");
        System.out.println("  " + docs.resolve("painel-indicadores.html").toAbsolutePath().normalize());
        System.out.println("  " + pasta.toAbsolutePath().normalize() + " (" + graficos.size() + " SVG)");
    }

    // ================================================================ gráficos

    static Grafico graficoEstoque(Map<String, Integer> estoque) {
        int w = 760, h = 380, ml = 56, mr = 24, mt = 64, mb = 72;
        int pw = w - ml - mr, ph = h - mt - mb;
        int max = 0;
        for (String t : estoque.keySet()) max = Math.max(max, Math.max(estoque.get(t), MINIMO_POR_TIPO.get(t)));
        double[] ticks = ticks(0, max);
        double topo = ticks[ticks.length - 1];
        StringBuilder s = inicioSvg(w, h, "Estoque disponível por tipo sanguíneo",
                "E2 · bolsas elegíveis nos 2 hemocentros; traço escuro = estoque mínimo de segurança");
        eixoY(s, ticks, topo, ml, mt, pw, ph, "bolsas");
        int n = estoque.size();
        double banda = (double) pw / n, larg = Math.min(44, banda - 18);
        int i = 0;
        StringBuilder tabela = new StringBuilder("<table><thead><tr><th>Tipo</th><th>Disponíveis</th><th>Mínimo</th><th>Cobertura</th><th>Situação</th></tr></thead><tbody>");
        for (Map.Entry<String, Integer> e : estoque.entrySet()) {
            int v = e.getValue(), min = MINIMO_POR_TIPO.get(e.getKey());
            double cx = ml + banda * i + banda / 2;
            double y = mt + ph - v / topo * ph, ym = mt + ph - min / topo * ph;
            double cob = 100.0 * v / min;
            String situacao = cob < 100 ? "abaixo do mínimo" : cob < 150 ? "atenção" : "ok";
            s.append(barra(cx - larg / 2, y, larg, mt + ph - y, AZUL,
                    e.getKey() + ": " + v + " bolsas (mínimo " + min + ", cobertura " + numero1(cob) + "%)"));
            s.append(String.format(Locale.ROOT, "<line x1='%.1f' y1='%.1f' x2='%.1f' y2='%.1f' stroke='%s' stroke-width='3' stroke-linecap='round'/>",
                    cx - larg / 2 - 6, ym, cx + larg / 2 + 6, ym, TINTA));
            s.append(texto(cx, Math.min(y, ym) - 8, String.valueOf(v), 13, TINTA, "middle", "600"));
            s.append(texto(cx, mt + ph + 20, e.getKey(), 13, TINTA, "middle", "600"));
            if (cob < 100) {
                s.append(selo(cx, mt + ph + 40, "▲ abaixo", CRITICO));
            } else if (cob < 150) {
                s.append(selo(cx, mt + ph + 40, "● atenção", "#9a6a00"));
            }
            tabela.append("<tr><td>").append(e.getKey()).append("</td><td>").append(v).append("</td><td>").append(min)
                    .append("</td><td>").append(numero1(cob)).append("%</td><td>").append(situacao).append("</td></tr>");
            i++;
        }
        linhaBase(s, ml, mt + ph, pw);
        s.append("</svg>\n");
        return new Grafico("01-estoque-por-tipo", "Estoque disponível por tipo sanguíneo",
                "E2 · bolsas elegíveis × estoque mínimo de segurança", s.toString(), tabela.append("</tbody></table>").toString());
    }

    static Grafico graficoDonut(Map<String, Integer> demanda) {
        int w = 760, h = 380;
        String[] cores = { AZUL, LARANJA, VERDE_AGUA, AMARELO };
        StringBuilder s = inicioSvg(w, h, "Hemocomponentes mais solicitados",
                "D2 · participação de cada componente nas requisições dos últimos 90 dias");
        int total = demanda.values().stream().mapToInt(Integer::intValue).sum();
        double cx = 230, cy = 215, rExt = 120, rInt = 74;
        double ang = -Math.PI / 2;
        int i = 0;
        StringBuilder tabela = new StringBuilder("<table><thead><tr><th>Componente</th><th>Requisições</th><th>%</th></tr></thead><tbody>");
        for (Map.Entry<String, Integer> e : demanda.entrySet()) {
            double frac = (double) e.getValue() / total;
            double a2 = ang + frac * 2 * Math.PI;
            String nome = NOME_COMPONENTE.get(e.getKey());
            s.append(String.format(Locale.ROOT, "<path d='%s' fill='%s' stroke='%s' stroke-width='2'><title>%s: %d requisições (%s)</title></path>",
                    arco(cx, cy, rExt, rInt, ang, a2), cores[i], SUPERFICIE, nome, e.getValue(), pct(e.getValue(), total)));
            // legenda com rótulo direto (identidade nunca só pela cor)
            double ly = 128 + i * 52;
            s.append(String.format(Locale.ROOT, "<rect x='430' y='%.1f' width='14' height='14' rx='3' fill='%s'/>", ly - 11, cores[i]));
            s.append(texto(454, ly, nome, 15, TINTA, "start", "600"));
            s.append(texto(454, ly + 20, inteiro(e.getValue()) + " requisições · " + pct(e.getValue(), total), 13, TINTA_2, "start", "400"));
            tabela.append("<tr><td>").append(nome).append("</td><td>").append(e.getValue()).append("</td><td>")
                    .append(pct(e.getValue(), total)).append("</td></tr>");
            ang = a2;
            i++;
        }
        s.append(texto(cx, cy - 4, inteiro(total), 26, TINTA, "middle", "700"));
        s.append(texto(cx, cy + 18, "requisições", 13, TINTA_2, "middle", "400"));
        s.append("</svg>\n");
        return new Grafico("02-demanda-por-componente", "Hemocomponentes mais solicitados",
                "D2 · participação nas requisições dos últimos 90 dias", s.toString(), tabela.append("</tbody></table>").toString());
    }

    static Grafico graficoBoxplot(String arquivo, String titulo, String subtitulo, List<Caixa> caixas, String unidade) {
        int w = 760, ml = 104, mr = 32, mt = 64, mb = 76;
        int altCaixa = 70;
        int h = mt + mb + altCaixa * caixas.size();
        int pw = w - ml - mr, ph = altCaixa * caixas.size();
        double max = 0;
        for (Caixa c : caixas) max = Math.max(max, c.max());
        double[] ticks = ticks(0, max);
        double topo = ticks[ticks.length - 1];
        StringBuilder s = inicioSvg(w, h, titulo, subtitulo);
        // grade vertical
        for (double t : ticks) {
            double x = ml + t / topo * pw;
            s.append(String.format(Locale.ROOT, "<line x1='%.1f' y1='%d' x2='%.1f' y2='%d' stroke='%s' stroke-width='1'/>", x, mt, x, mt + ph, GRADE));
            s.append(texto(x, mt + ph + 18, numeroCurto(t), 12, TINTA_MUDA, "middle", "400"));
        }
        s.append(texto(ml + pw / 2.0, mt + ph + 38, unidade, 12, TINTA_2, "middle", "400"));
        StringBuilder tabela = new StringBuilder("<table><thead><tr><th>Grupo</th><th>n</th><th>Mín.</th><th>Q1</th><th>Mediana</th>"
                + "<th>Média</th><th>Q3</th><th>Máx.</th><th>Outliers</th></tr></thead><tbody>");
        int i = 0;
        for (Caixa c : caixas) {
            double cy = mt + altCaixa * i + altCaixa / 2.0;
            double x = 0;
            s.append(texto(ml - 12, cy + 4, c.rotulo(), 13, TINTA, "end", "600"));
            s.append(texto(ml - 12, cy + 20, "n = " + inteiro(c.n()), 11, TINTA_MUDA, "end", "400"));
            // bigodes
            double xi = ml + c.bigodeInf() / topo * pw, xs = ml + c.bigodeSup() / topo * pw;
            double x1 = ml + c.q1() / topo * pw, x3 = ml + c.q3() / topo * pw, xm = ml + c.mediana() / topo * pw;
            s.append(String.format(Locale.ROOT, "<line x1='%.1f' y1='%.1f' x2='%.1f' y2='%.1f' stroke='%s' stroke-width='2'/>", xi, cy, x1, cy, TINTA_2));
            s.append(String.format(Locale.ROOT, "<line x1='%.1f' y1='%.1f' x2='%.1f' y2='%.1f' stroke='%s' stroke-width='2'/>", x3, cy, xs, cy, TINTA_2));
            s.append(String.format(Locale.ROOT, "<line x1='%.1f' y1='%.1f' x2='%.1f' y2='%.1f' stroke='%s' stroke-width='2'/>", xi, cy - 10, xi, cy + 10, TINTA_2));
            s.append(String.format(Locale.ROOT, "<line x1='%.1f' y1='%.1f' x2='%.1f' y2='%.1f' stroke='%s' stroke-width='2'/>", xs, cy - 10, xs, cy + 10, TINTA_2));
            // caixa
            s.append(String.format(Locale.ROOT, "<rect x='%.1f' y='%.1f' width='%.1f' height='34' rx='4' fill='%s' fill-opacity='0.22' stroke='%s' stroke-width='2'>"
                    + "<title>%s — Q1 %s · mediana %s · Q3 %s · média %s (%s)</title></rect>",
                    x1, cy - 17, Math.max(1, x3 - x1), AZUL, AZUL, c.rotulo(), numero1(c.q1()), numero1(c.mediana()),
                    numero1(c.q3()), numero1(c.media()), unidade));
            s.append(String.format(Locale.ROOT, "<line x1='%.1f' y1='%.1f' x2='%.1f' y2='%.1f' stroke='%s' stroke-width='3'/>", xm, cy - 17, xm, cy + 17, AZUL));
            // outliers
            for (double o : c.outliers()) {
                x = ml + o / topo * pw;
                s.append(String.format(Locale.ROOT, "<circle cx='%.1f' cy='%.1f' r='4' fill='%s' fill-opacity='0.55' stroke='%s' stroke-width='1.5'><title>outlier: %s %s</title></circle>",
                        x, cy, LARANJA, SUPERFICIE, numero1(o), unidade));
            }
            // média (losango) — mostra o deslocamento média × mediana
            double xmed = ml + c.media() / topo * pw;
            s.append(String.format(Locale.ROOT, "<path d='M%.1f %.1f l6 6 l-6 6 l-6 -6 z' fill='%s' stroke='%s' stroke-width='1.5'><title>média: %s %s</title></path>",
                    xmed, cy - 6, TINTA, SUPERFICIE, numero1(c.media()), unidade));
            s.append(texto(xm, cy - 23, "mediana " + numero0(c.mediana()), 11, TINTA_2, "middle", "400"));
            tabela.append("<tr><td>").append(c.rotulo()).append("</td><td>").append(c.n()).append("</td><td>").append(numero1(c.min()))
                    .append("</td><td>").append(numero1(c.q1())).append("</td><td>").append(numero1(c.mediana())).append("</td><td>")
                    .append(numero1(c.media())).append("</td><td>").append(numero1(c.q3())).append("</td><td>").append(numero1(c.max()))
                    .append("</td><td>").append(c.outliers().length).append("</td></tr>");
            i++;
        }
        // legenda
        double ly = h - 18;
        s.append(String.format(Locale.ROOT, "<rect x='%d' y='%.1f' width='22' height='12' rx='3' fill='%s' fill-opacity='0.22' stroke='%s' stroke-width='2'/>", ml, ly - 10, AZUL, AZUL));
        s.append(texto(ml + 30, ly, "caixa = Q1 a Q3, traço = mediana", 12, TINTA_2, "start", "400"));
        s.append(String.format(Locale.ROOT, "<path d='M%d %.1f l6 6 l-6 6 l-6 -6 z' fill='%s'/>", ml + 262, ly - 10, TINTA));
        s.append(texto(ml + 274, ly, "média", 12, TINTA_2, "start", "400"));
        s.append(String.format(Locale.ROOT, "<circle cx='%d' cy='%.1f' r='4' fill='%s' fill-opacity='0.55'/>", ml + 338, ly - 4, LARANJA));
        s.append(texto(ml + 348, ly, "outlier (fora de 1,5 × AIQ)", 12, TINTA_2, "start", "400"));
        s.append("</svg>\n");
        return new Grafico(arquivo, titulo, subtitulo, s.toString(), tabela.append("</tbody></table>").toString());
    }

    static Grafico graficoHistograma(double[] valores) {
        int w = 760, h = 380, ml = 56, mr = 24, mt = 64, mb = 64;
        int pw = w - ml - mr, ph = h - mt - mb;
        int largura = 10, limite = 200;
        int nb = limite / largura + 1; // último = "> 200"
        int[] cont = new int[nb];
        for (double v : valores) cont[Math.min(nb - 1, (int) (v / largura))]++;
        int max = Arrays.stream(cont).max().orElse(1);
        double[] ticks = ticks(0, max);
        double topo = ticks[ticks.length - 1];
        StringBuilder s = inicioSvg(w, h, "Distribuição do tempo de transporte",
                "T3 · do envio até a entrega, em faixas de 10 minutos (requisições ATENDIDA)");
        eixoY(s, ticks, topo, ml, mt, pw, ph, "requisições");
        double banda = (double) pw / nb;
        StringBuilder tabela = new StringBuilder("<table><thead><tr><th>Faixa (min)</th><th>Requisições</th></tr></thead><tbody>");
        for (int i = 0; i < nb; i++) {
            String faixa = i == nb - 1 ? "> " + limite : (i * largura) + "–" + (i * largura + largura - 1);
            double y = mt + ph - cont[i] / topo * ph;
            s.append(barra(ml + banda * i + 1, y, banda - 2, mt + ph - y, i == nb - 1 ? LARANJA : AZUL, faixa + " min: " + cont[i] + " requisições"));
            if (i % 2 == 0 && i < nb - 1) s.append(texto(ml + banda * i, mt + ph + 18, String.valueOf(i * largura), 12, TINTA_MUDA, "middle", "400"));
            tabela.append("<tr><td>").append(faixa).append("</td><td>").append(cont[i]).append("</td></tr>");
        }
        s.append(texto(ml + banda * (nb - 0.5), mt + ph + 18, "> 200", 12, LARANJA_TEXTO, "middle", "600"));
        s.append(texto(ml + pw / 2.0, mt + ph + 40, "minutos", 12, TINTA_2, "middle", "400"));
        s.append(texto(ml + banda * 1.6 + 8, mt + 18, "rotas na mesma região", 12, TINTA_2, "start", "400"));
        s.append(texto(ml + banda * 12.5, mt + ph - 40, "rotas entre regiões e incidentes", 12, TINTA_2, "middle", "400"));
        linhaBase(s, ml, mt + ph, pw);
        s.append("</svg>\n");
        return new Grafico("05-histograma-transporte", "Distribuição do tempo de transporte",
                "T3 · faixas de 10 minutos", s.toString(), tabela.append("</tbody></table>").toString());
    }

    static final String LARANJA_TEXTO = "#b2461c"; // laranja escurecido para texto (contraste ≥ 4,5:1)

    static Grafico graficoPorDia(Map<LocalDate, Integer> porDia) {
        int w = 760, h = 380, ml = 56, mr = 24, mt = 64, mb = 56;
        int pw = w - ml - mr, ph = h - mt - mb;
        int max = porDia.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        double media = porDia.values().stream().mapToInt(Integer::intValue).average().orElse(0);
        double[] ticks = ticks(0, max);
        double topo = ticks[ticks.length - 1];
        StringBuilder s = inicioSvg(w, h, "Requisições por dia",
                "D3 · 02/07 a 30/09/2026; linha tracejada = média diária");
        eixoY(s, ticks, topo, ml, mt, pw, ph, "requisições");
        double banda = (double) pw / porDia.size();
        int i = 0;
        StringBuilder tabela = new StringBuilder("<table><thead><tr><th>Dia</th><th>Requisições</th></tr></thead><tbody>");
        for (Map.Entry<LocalDate, Integer> e : porDia.entrySet()) {
            double y = mt + ph - e.getValue() / topo * ph;
            String dia = String.format("%02d/%02d", e.getKey().getDayOfMonth(), e.getKey().getMonthValue());
            boolean fds = e.getKey().getDayOfWeek().getValue() >= 6;
            s.append(barra(ml + banda * i + 1, y, Math.max(1, banda - 2), mt + ph - y, fds ? "#86b6ef" : AZUL,
                    dia + (fds ? " (fim de semana)" : "") + ": " + e.getValue() + " requisições"));
            if (e.getKey().getDayOfMonth() == 1 || e.getKey().getDayOfMonth() == 15) {
                s.append(texto(ml + banda * i + banda / 2, mt + ph + 18, dia, 12, TINTA_MUDA, "middle", "400"));
            }
            tabela.append("<tr><td>").append(dia).append("</td><td>").append(e.getValue()).append("</td></tr>");
            i++;
        }
        double ym = mt + ph - media / topo * ph;
        s.append(String.format(Locale.ROOT, "<line x1='%d' y1='%.1f' x2='%d' y2='%.1f' stroke='%s' stroke-width='2' stroke-dasharray='6 4'/>", ml, ym, ml + pw, ym, TINTA));
        s.append(texto(ml + pw - 4, ym - 8, "média " + numero1(media) + "/dia", 12, TINTA, "end", "600"));
        // legenda
        s.append(String.format(Locale.ROOT, "<rect x='%d' y='%d' width='12' height='12' rx='3' fill='%s'/>", ml, h - 22, AZUL));
        s.append(texto(ml + 18, h - 12, "dia útil", 12, TINTA_2, "start", "400"));
        s.append(String.format(Locale.ROOT, "<rect x='%d' y='%d' width='12' height='12' rx='3' fill='%s'/>", ml + 90, h - 22, "#86b6ef"));
        s.append(texto(ml + 108, h - 12, "sábado e domingo", 12, TINTA_2, "start", "400"));
        linhaBase(s, ml, mt + ph, pw);
        s.append("</svg>\n");
        return new Grafico("06-requisicoes-por-dia", "Requisições por dia", "D3 · série diária com a média",
                s.toString(), tabela.append("</tbody></table>").toString());
    }

    static Grafico graficoVencimento(Map<String, Integer> porFaixa) {
        int w = 760, h = 380, ml = 56, mr = 24, mt = 64, mb = 64;
        int pw = w - ml - mr, ph = h - mt - mb;
        int max = porFaixa.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        double[] ticks = ticks(0, max);
        double topo = ticks[ticks.length - 1];
        StringBuilder s = inicioSvg(w, h, "Bolsas disponíveis por prazo até o vencimento",
                "E3 · status DISPONIVEL, em dias até a data de validade (todos os componentes)");
        eixoY(s, ticks, topo, ml, mt, pw, ph, "bolsas");
        double banda = (double) pw / porFaixa.size(), larg = Math.min(56, banda - 20);
        int i = 0;
        StringBuilder tabela = new StringBuilder("<table><thead><tr><th>Dias até vencer</th><th>Bolsas</th></tr></thead><tbody>");
        for (Map.Entry<String, Integer> e : porFaixa.entrySet()) {
            double cx = ml + banda * i + banda / 2;
            double y = mt + ph - e.getValue() / topo * ph;
            String cor = i == 0 ? CRITICO : (i <= 2 ? AMARELO : AZUL);
            s.append(barra(cx - larg / 2, y, larg, mt + ph - y, cor, e.getKey() + " dias: " + e.getValue() + " bolsas"));
            s.append(texto(cx, y - 8, String.valueOf(e.getValue()), 13, TINTA, "middle", "600"));
            s.append(texto(cx, mt + ph + 20, e.getKey(), 13, TINTA, "middle", "600"));
            tabela.append("<tr><td>").append(e.getKey()).append("</td><td>").append(e.getValue()).append("</td></tr>");
            i++;
        }
        s.append(texto(ml + pw / 2.0, mt + ph + 40, "dias até o vencimento", 12, TINTA_2, "middle", "400"));
        s.append(texto(ml, h - 8, "* vencida = validade anterior a hoje e ainda não descartada (não pode ser alocada). "
                + "Amarelo = vence em até 7 dias.", 11, TINTA_2, "start", "400"));
        linhaBase(s, ml, mt + ph, pw);
        s.append("</svg>\n");
        return new Grafico("07-prazo-vencimento", "Bolsas disponíveis por prazo até o vencimento",
                "E3 · dias até a data de validade", s.toString(), tabela.append("</tbody></table>").toString());
    }

    // ================================================================ painel HTML

    static String painelHtml(List<String[]> cards, List<Grafico> graficos, int nReq, int nHemo, int nTel) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html>\n<html lang=\"pt-BR\">\n<head>\n<meta charset=\"utf-8\">\n")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
                .append("<title>Painel de indicadores — HemoTrack (EST U1)</title>\n<style>\n")
                .append(":root{color-scheme:light;--fundo:#f9f9f7;--superficie:#fcfcfb;--tinta:#0b0b0b;--tinta2:#52514e;--muda:#898781;")
                .append("--borda:rgba(11,11,11,.10);--marca:#c0262d;--critico:#d03b3b;--atencao:#9a6a00}\n")
                .append("*{box-sizing:border-box}body{margin:0;background:var(--fundo);color:var(--tinta);font-family:")
                .append(FONTE).append(";line-height:1.45}\n")
                .append("header{background:var(--superficie);border-bottom:1px solid var(--borda);padding:20px 24px}\n")
                .append("header h1{margin:0;font-size:22px}header p{margin:4px 0 0;color:var(--tinta2);font-size:14px}\n")
                .append(".marca{color:var(--marca);font-weight:700}main{max-width:1200px;margin:0 auto;padding:24px 16px 48px}\n")
                .append(".cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:12px;margin-bottom:24px}\n")
                .append(".card{background:var(--superficie);border:1px solid var(--borda);border-radius:12px;padding:16px}\n")
                .append(".card .rot{font-size:13px;color:var(--tinta2)}.card .val{font-size:30px;font-weight:700;margin:4px 0}\n")
                .append(".card .det{font-size:12px;color:var(--muda)}.card.atencao{border-left:4px solid #fab219}\n")
                .append(".grade{display:grid;grid-template-columns:repeat(auto-fit,minmax(520px,1fr));gap:16px}\n")
                .append("@media (max-width:600px){.grade{grid-template-columns:1fr}}\n")
                .append("figure{margin:0;background:var(--superficie);border:1px solid var(--borda);border-radius:12px;padding:8px 8px 12px}\n")
                .append("figure svg{width:100%;height:auto;display:block}details{margin:4px 12px 0;font-size:13px}\n")
                .append("summary{cursor:pointer;color:var(--tinta2)}table{border-collapse:collapse;margin-top:8px;width:100%}\n")
                .append("th,td{border-bottom:1px solid var(--borda);padding:4px 8px;text-align:right}th:first-child,td:first-child{text-align:left}\n")
                .append("th{color:var(--tinta2);font-weight:600}.tab-rolagem{max-height:260px;overflow:auto}\n")
                .append("footer{max-width:1200px;margin:0 auto;padding:0 16px 32px;color:var(--muda);font-size:12px}\n")
                .append("</style>\n</head>\n<body>\n<header>\n<h1><span class=\"marca\">HemoTrack</span> · Painel inicial de indicadores</h1>\n")
                .append("<p>HU09 — Estatística (Unidade 1). Fotografia em 30/09/2026, 15h. Dados <strong>sintéticos</strong>: ")
                .append(inteiro(nReq)).append(" requisições, ").append(inteiro(nHemo)).append(" bolsas, ").append(inteiro(nTel))
                .append(" leituras de telemetria.</p>\n</header>\n<main>\n<section class=\"cards\" aria-label=\"Indicadores rápidos\">\n");
        for (String[] c : cards) {
            sb.append("<div class=\"card").append(c[3].isEmpty() ? "" : " " + c[3]).append("\"><div class=\"rot\">").append(c[0])
                    .append("</div><div class=\"val\">").append(c[1]).append("</div><div class=\"det\">").append(c[2]).append("</div></div>\n");
        }
        sb.append("</section>\n<section class=\"grade\">\n");
        for (Grafico g : graficos) {
            sb.append("<figure>\n").append(g.svg().replaceFirst("<\\?xml[^>]*>\\s*", ""))
                    .append("<details><summary>Ver dados em tabela</summary><div class=\"tab-rolagem\">").append(g.tabelaHtml())
                    .append("</div></details>\n</figure>\n");
        }
        sb.append("</section>\n</main>\n<footer>Gerado por <code>estatistica/PainelIndicadores.java</code> (PI2-108). ")
                .append("Definição dos indicadores em <code>docs/est/indicadores.md</code>; medidas em <code>docs/est/medidas-descritivas.md</code>. ")
                .append("Passe o mouse sobre barras, fatias e pontos para ver os valores.</footer>\n</body>\n</html>\n");
        return sb.toString();
    }

    // ================================================================ peças de SVG

    static StringBuilder inicioSvg(int w, int h, String titulo, String subtitulo) {
        StringBuilder s = new StringBuilder();
        s.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        s.append(String.format(Locale.ROOT, "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 %d %d' width='%d' height='%d' role='img' "
                + "aria-label='%s' font-family=\"%s\">", w, h, w, h, esc(titulo), FONTE));
        s.append(String.format(Locale.ROOT, "<rect width='%d' height='%d' rx='12' fill='%s'/>", w, h, SUPERFICIE));
        s.append(texto(24, 32, titulo, 17, TINTA, "start", "700"));
        s.append(texto(24, 52, subtitulo, 12.5, TINTA_2, "start", "400"));
        return s;
    }

    static void eixoY(StringBuilder s, double[] ticks, double topo, int ml, int mt, int pw, int ph, String unidade) {
        for (double t : ticks) {
            double y = mt + ph - t / topo * ph;
            if (t > 0) {
                s.append(String.format(Locale.ROOT, "<line x1='%d' y1='%.1f' x2='%d' y2='%.1f' stroke='%s' stroke-width='1'/>", ml, y, ml + pw, y, GRADE));
            }
            s.append(texto(ml - 8, y + 4, numeroCurto(t), 12, TINTA_MUDA, "end", "400"));
        }
        s.append(String.format(Locale.ROOT, "<text x='%d' y='%d' font-size='12' fill='%s' transform='rotate(-90 %d %d)' text-anchor='middle'>%s</text>",
                14, mt + ph / 2, TINTA_2, 14, mt + ph / 2, esc(unidade)));
    }

    static void linhaBase(StringBuilder s, int ml, double y, int pw) {
        s.append(String.format(Locale.ROOT, "<line x1='%d' y1='%.1f' x2='%d' y2='%.1f' stroke='%s' stroke-width='1.5'/>", ml, y, ml + pw, y, EIXO));
    }

    /** Barra com topo arredondado (4px) e base reta, ancorada no eixo. */
    static String barra(double x, double y, double w, double h, String cor, String dica) {
        double r = Math.min(4, Math.min(w / 2, h));
        if (h <= 0) {
            return "";
        }
        String d = String.format(Locale.ROOT, "M%.1f %.1f v%.1f a%.1f %.1f 0 0 1 %.1f %.1f h%.1f a%.1f %.1f 0 0 1 %.1f %.1f v%.1f z",
                x, y + h, -(h - r), r, r, r, -r, w - 2 * r, r, r, r, r, h - r);
        return String.format(Locale.ROOT, "<path d='%s' fill='%s'><title>%s</title></path>", d, cor, esc(dica));
    }

    static String selo(double cx, double y, String rotulo, String cor) {
        return texto(cx, y, rotulo, 11, cor, "middle", "700");
    }

    static String texto(double x, double y, String t, double tamanho, String cor, String ancora, String peso) {
        return String.format(Locale.ROOT, "<text x='%.1f' y='%.1f' font-size='%s' fill='%s' text-anchor='%s' font-weight='%s' "
                + "paint-order='stroke' stroke='%s' stroke-width='3' stroke-linejoin='round'>%s</text>",
                x, y, numeroSvg(tamanho), cor, ancora, peso, SUPERFICIE, esc(t));
    }

    static String arco(double cx, double cy, double r1, double r0, double a1, double a2) {
        int grande = (a2 - a1) > Math.PI ? 1 : 0;
        return String.format(Locale.ROOT, "M%.2f %.2f A%.1f %.1f 0 %d 1 %.2f %.2f L%.2f %.2f A%.1f %.1f 0 %d 0 %.2f %.2f Z",
                cx + r1 * Math.cos(a1), cy + r1 * Math.sin(a1), r1, r1, grande, cx + r1 * Math.cos(a2), cy + r1 * Math.sin(a2),
                cx + r0 * Math.cos(a2), cy + r0 * Math.sin(a2), r0, r0, grande, cx + r0 * Math.cos(a1), cy + r0 * Math.sin(a1));
    }

    /** Marcas de eixo "redondas" (1, 2, 2,5, 5 × 10ⁿ) cobrindo [min, max]. */
    static double[] ticks(double min, double max) {
        if (max <= min) max = min + 1;
        double bruto = (max - min) / 5;
        double pot = Math.pow(10, Math.floor(Math.log10(bruto)));
        double passo = pot;
        for (double m : new double[] { 1, 2, 2.5, 5, 10 }) {
            if (m * pot >= bruto) {
                passo = m * pot;
                break;
            }
        }
        int n = (int) Math.ceil(max / passo);
        double[] t = new double[n + 1];
        for (int i = 0; i <= n; i++) t[i] = i * passo;
        return t;
    }

    // ================================================================ estatística

    static Caixa caixa(String rotulo, double[] valores) {
        double[] v = valores.clone();
        Arrays.sort(v);
        double q1 = quantil(v, 0.25), q2 = quantil(v, 0.5), q3 = quantil(v, 0.75), aiq = q3 - q1;
        double li = q1 - 1.5 * aiq, ls = q3 + 1.5 * aiq;
        double bi = v[0], bs = v[v.length - 1];
        for (double x : v) if (x >= li) { bi = x; break; }
        for (int i = v.length - 1; i >= 0; i--) if (v[i] <= ls) { bs = v[i]; break; }
        double soma = 0;
        for (double x : v) soma += x;
        double[] out = Arrays.stream(v).filter(x -> x < li || x > ls).toArray();
        return new Caixa(rotulo, v.length, v[0], q1, q2, q3, v[v.length - 1], soma / v.length, bi, bs, out);
    }

    /** Mesmo método de AnaliseDescritiva.java: interpolação linear na posição (n − 1)·p. */
    static double quantil(double[] ordenado, double p) {
        double pos = (ordenado.length - 1) * p;
        int i = (int) Math.floor(pos);
        double frac = pos - i;
        return i + 1 < ordenado.length ? ordenado[i] + frac * (ordenado[i + 1] - ordenado[i]) : ordenado[i];
    }

    static double[] minutos(List<Map<String, String>> linhas, String de, String ate) {
        return linhas.stream().mapToDouble(r -> Duration.between(OffsetDateTime.parse(r.get(de)),
                OffsetDateTime.parse(r.get(ate))).toMinutes()).toArray();
    }

    static boolean foraDaFaixa(Map<String, String> t) {
        double v = Double.parseDouble(t.get("temperaturaC"));
        return switch (t.get("tipo")) {
            case "HEMACIAS" -> v < 2 || v > 6;
            case "PLAQUETAS" -> v < 20 || v > 24;
            default -> v < -40 || v > -20;
        };
    }

    // ================================================================ CSV e formatação

    static List<Map<String, String>> lerCsv(Path arquivo) throws IOException {
        List<String> linhas = Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        String[] cab = separar(linhas.get(0));
        List<Map<String, String>> out = new ArrayList<>(linhas.size());
        for (int i = 1; i < linhas.size(); i++) {
            if (linhas.get(i).isBlank()) continue;
            String[] c = separar(linhas.get(i));
            Map<String, String> m = new HashMap<>();
            for (int j = 0; j < cab.length; j++) m.put(cab[j], j < c.length ? c[j] : "");
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

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("'", "&#39;");
    }

    static String inteiro(int x) {
        return String.format(BR, "%,d", x);
    }

    static String numero0(double x) {
        return String.format(BR, "%,.0f", x);
    }

    static String numero1(double x) {
        return String.format(BR, "%,.1f", x);
    }

    static String numeroCurto(double x) {
        return x == Math.rint(x) ? String.format(BR, "%,.0f", x) : String.format(BR, "%,.1f", x);
    }

    static String numeroSvg(double x) {
        return x == Math.rint(x) ? String.valueOf((long) x) : String.format(Locale.ROOT, "%.1f", x);
    }

    static String pct(int parte, int todo) {
        return todo == 0 ? "—" : String.format(BR, "%.1f%%", 100.0 * parte / todo);
    }
}
