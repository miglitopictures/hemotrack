package com.hemotrack.backend.aceitacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.hemotrack.backend.model.sangue.Hemocomponente;
import com.hemotrack.backend.model.sangue.TipoHemocomponente;
import com.hemotrack.backend.model.shared.FatorRh;
import com.hemotrack.backend.model.shared.TipoABO;
import com.hemotrack.backend.repositories.HemocomponenteRepository;

// TESTES DE ACEITACAO: um cenario por historia, do ponto de vista de quem usa a API.
//
// O Spring sobe de verdade numa porta aleatoria (RANDOM_PORT) e os testes fazem requisicoes
// HTTP reais, passando por tudo: filtro JWT, regras de seguranca, controller, service e banco.
// Nada e mockado. Cada teste segue o formato BDD (Dado / Quando / Entao), espelhando os
// cenarios de docs/historias.md.
//
// O ADMIN_SISTEMA de teste e criado pelo SeedAdminSistema a partir das propriedades abaixo.
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:aceitacao;DB_CLOSE_DELAY=-1",
        "hemotrack.jwt.segredo=segredo-de-teste-com-pelo-menos-32-caracteres",
        "hemotrack.admin.email=admin@hemotrack.test",
        "hemotrack.admin.senha=senha-admin-123",
        "management.otlp.metrics.export.enabled=false"
    })
class HistoriasAceitacaoTest {

    @Value("${local.server.port}")
    private int porta;

    private final HttpClient http = HttpClient.newHttpClient();

    // HU02 (estoque) ainda nao tem rota na API: as bolsas do "Dado" entram direto pelo repositorio.
    @Autowired
    private HemocomponenteRepository hemocomponentes;

    // o banco e compartilhado entre os testes da classe: cada instituicao recebe CNPJ e e-mail unicos.
    private static final AtomicInteger SEQUENCIA = new AtomicInteger(1);

    // =====================================================================================
    // HU01 - Cadastro e login de instituicao
    // =====================================================================================

    @Test
    void hu01_cadastroELogin_instituicaoNascePendenteEAdminConsegueEntrar() throws Exception {
        // Dado um hospital que ainda nao tem conta
        int n = SEQUENCIA.getAndIncrement();
        String email = "admin" + n + "@hospital.test";

        // Quando ele se cadastra
        HttpResponse<String> cadastro = post("/instituicoes", corpoCadastro(cnpjFormatado(n), "HOSPITAL", email), null);

        // Entao a instituicao e criada aguardando aprovacao, com o responsavel como admin
        assertThat(cadastro.statusCode()).isEqualTo(201);
        assertThat(cadastro.body()).contains("PENDENTE_APROVACAO").contains("ADMIN_INSTITUICAO");

        // E o responsavel consegue entrar e ver os proprios dados
        String token = login(email, "senha123");
        HttpResponse<String> me = get("/auth/me", token);
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains(email);
    }

    @Test
    void hu01_cnpjJaCadastradoEmOutroFormato_retorna409() throws Exception {
        // Dado um hospital ja cadastrado com o CNPJ formatado
        int n = SEQUENCIA.getAndIncrement();
        post("/instituicoes", corpoCadastro(cnpjFormatado(n), "HOSPITAL", "a" + n + "@hospital.test"), null);

        // Quando outra instituicao tenta usar o mesmo CNPJ, so com digitos
        HttpResponse<String> repetido =
            post("/instituicoes", corpoCadastro(cnpjDigitos(n), "HOSPITAL", "b" + n + "@hospital.test"), null);

        // Entao o cadastro e recusado por conflito
        assertThat(repetido.statusCode()).isEqualTo(409);
        assertThat(repetido.body()).contains("cnpj-duplicado");
    }

    // cenario da regra nova (ciclo C do TDD), agora de ponta a ponta
    @Test
    void hu01_cnpjIncompleto_retorna400ApontandoOCampo() throws Exception {
        // Dado um cadastro com CNPJ incompleto
        int n = SEQUENCIA.getAndIncrement();

        // Quando ele e enviado
        HttpResponse<String> resposta = post("/instituicoes", corpoCadastro("12.345", "HOSPITAL", "c" + n + "@hospital.test"), null);

        // Entao a API responde 400 indicando o campo, no formato que o front usa para marcar o input
        assertThat(resposta.statusCode()).isEqualTo(400);
        assertThat(resposta.body()).contains("instituicao.cnpj");
    }

    // =====================================================================================
    // HU03 - Criar requisicao de hemocomponentes
    // =====================================================================================

    @Test
    void hu03_hospitalAprovadoCriaRequisicao_nasceAberta() throws Exception {
        // Dado um hospital aprovado e logado
        Instituicao hospital = instituicaoAprovada("HOSPITAL");

        // Quando ele cria uma requisicao
        HttpResponse<String> resposta = post("/requisicoes", corpoRequisicao(hospital.id(), 450.0), hospital.token());

        // Entao a requisicao e registrada como ABERTA (o "Pendente" da historia)
        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.body()).contains("\"status\":\"ABERTA\"");
    }

    @Test
    void hu03_instituicaoAindaPendente_naoPodeCriarRequisicao() throws Exception {
        // Dado um hospital cadastrado mas ainda nao aprovado
        int n = SEQUENCIA.getAndIncrement();
        String email = "pendente" + n + "@hospital.test";
        HttpResponse<String> cadastro = post("/instituicoes", corpoCadastro(cnpjDigitos(n), "HOSPITAL", email), null);
        long id = Long.parseLong(extrair(cadastro.body(), "id"));
        String token = login(email, "senha123");

        // Quando ele tenta criar uma requisicao
        HttpResponse<String> resposta = post("/requisicoes", corpoRequisicao(id, 450.0), token);

        // Entao o acesso e negado (regra InstituicaoAprovada)
        assertThat(resposta.statusCode()).isEqualTo(403);
    }

    // cenario da regra nova (ciclo A do TDD), agora de ponta a ponta
    @Test
    void hu03_volumeZero_retorna400() throws Exception {
        // Dado um hospital aprovado e logado
        Instituicao hospital = instituicaoAprovada("HOSPITAL");

        // Quando ele envia uma requisicao com volume zero
        HttpResponse<String> resposta = post("/requisicoes", corpoRequisicao(hospital.id(), 0.0), hospital.token());

        // Entao a requisicao e recusada
        assertThat(resposta.statusCode()).isEqualTo(400);
    }

    // =====================================================================================
    // HU05 - Aceitar ou recusar requisicoes
    // =====================================================================================

    @Test
    void hu05_hemocentroAceita_hospitalVeRequisicaoAceita() throws Exception {
        // Dado um hospital com uma requisicao aberta e um hemocentro aprovado
        Instituicao hospital = instituicaoAprovada("HOSPITAL");
        Instituicao hemocentro = instituicaoAprovada("HEMOCENTRO");
        long requisicaoId = criarRequisicao(hospital);

        // Quando o hemocentro aceita a requisicao
        HttpResponse<String> aceite = post("/requisicoes/" + requisicaoId + "/aceitar?hemocentroId=" + hemocentro.id(),
                                           null, hemocentro.token());

        // Entao ela passa para ACEITA
        assertThat(aceite.statusCode()).isEqualTo(200);
        assertThat(aceite.body()).contains("\"status\":\"ACEITA\"");

        // E o hospital acompanha a mudanca de status (HU04)
        HttpResponse<String> consulta = get("/requisicoes/" + requisicaoId, hospital.token());
        assertThat(consulta.body()).contains("\"status\":\"ACEITA\"");
    }

    @Test
    void hu05_recusaExigeMotivo_eComMotivoFicaRecusada() throws Exception {
        // Dado um hospital com uma requisicao aberta e um hemocentro aprovado
        Instituicao hospital = instituicaoAprovada("HOSPITAL");
        Instituicao hemocentro = instituicaoAprovada("HEMOCENTRO");
        long requisicaoId = criarRequisicao(hospital);

        // Quando o hemocentro tenta recusar sem motivo
        HttpResponse<String> semMotivo = post("/requisicoes/" + requisicaoId + "/recusar",
                                              "{\"motivoRecusa\":\"\"}", hemocentro.token());

        // Entao a recusa e rejeitada
        assertThat(semMotivo.statusCode()).isEqualTo(400);

        // Quando ele recusa informando o motivo
        HttpResponse<String> comMotivo = post("/requisicoes/" + requisicaoId + "/recusar",
                                              "{\"motivoRecusa\":\"Estoque insuficiente\"}", hemocentro.token());

        // Entao a requisicao fica RECUSADA com o motivo registrado
        assertThat(comMotivo.statusCode()).isEqualTo(200);
        assertThat(comMotivo.body()).contains("\"status\":\"RECUSADA\"").contains("Estoque insuficiente");
    }

    // =====================================================================================
    // HU07 - Planejar a distribuicao
    // =====================================================================================

    @Test
    void hu07_requisicaoAlocada_hemocentroVeRotaAteOHospital() throws Exception {
        // Dado um hemocentro em Recife e um hospital em Caruaru
        Instituicao hospital = instituicaoAprovada("HOSPITAL", "Caruaru");
        Instituicao hemocentro = instituicaoAprovada("HEMOCENTRO", "Recife");

        // E uma requisicao aceita pelo hemocentro, com as bolsas ja selecionadas (ALOCADA)
        long requisicaoId = criarRequisicao(hospital);
        HttpResponse<String> aceite = post("/requisicoes/" + requisicaoId + "/aceitar?hemocentroId=" + hemocentro.id(),
                                           null, hemocentro.token());
        assertThat(aceite.statusCode()).isEqualTo(200);

        long bolsaId = bolsaNoEstoque(hemocentro.id());
        HttpResponse<String> alocacao = patch("/requisicoes/" + requisicaoId + "/alocar",
                                              "{\"hemocomponenteIds\":[" + bolsaId + "]}", hemocentro.token());
        assertThat(alocacao.statusCode()).isEqualTo(200);

        // Quando o hemocentro planeja a distribuicao
        HttpResponse<String> rota = get("/requisicoes/" + requisicaoId + "/rota", hemocentro.token());

        // Entao o sistema apresenta origem, destino e uma rota disponivel
        assertThat(rota.statusCode()).isEqualTo(200);
        assertThat(rota.body())
            .contains("\"municipio\":\"Recife\"")
            .contains("\"municipio\":\"Caruaru\"")
            .contains("\"locais\":[\"Recife\"")
            .contains("\"Caruaru\"]");
    }

    @Test
    void hu07_requisicaoAindaPendente_naoPodeSerDistribuida() throws Exception {
        // Dado uma requisicao ainda pendente (ABERTA)
        Instituicao hospital = instituicaoAprovada("HOSPITAL");
        Instituicao hemocentro = instituicaoAprovada("HEMOCENTRO");
        long requisicaoId = criarRequisicao(hospital);

        // Quando o hemocentro tenta planejar a distribuicao
        HttpResponse<String> rota = get("/requisicoes/" + requisicaoId + "/rota", hemocentro.token());

        // Entao o sistema impede a operacao
        assertThat(rota.statusCode()).isEqualTo(409);
    }

    // =====================================================================================
    // HU08 - Monitorar o transporte
    // =====================================================================================

    @Test
    void hu08_hemocentroEnvia_hospitalMonitoraStatusLocalizacaoETemperatura() throws Exception {
        // Dado uma requisicao alocada, de um hemocentro em Recife para um hospital em Caruaru
        Instituicao hospital = instituicaoAprovada("HOSPITAL", "Caruaru");
        Instituicao hemocentro = instituicaoAprovada("HEMOCENTRO", "Recife");
        long requisicaoId = requisicaoAlocada(hospital, hemocentro);

        // E o hemocentro despacha o lote
        HttpResponse<String> envio = patch("/requisicoes/" + requisicaoId + "/enviar", hemocentro.token());
        assertThat(envio.statusCode()).isEqualTo(200);
        assertThat(envio.body()).contains("\"status\":\"EM_TRANSITO\"");

        // Quando o hospital acessa o monitoramento
        HttpResponse<String> monitoramento = get("/requisicoes/" + requisicaoId + "/monitoramento", hospital.token());

        // Entao o sistema apresenta status, localizacao e temperatura simuladas
        assertThat(monitoramento.statusCode()).isEqualTo(200);
        assertThat(monitoramento.body())
            .contains("\"status\":\"EM_TRANSITO\"")
            .contains("\"localAtual\":\"Recife\"") // acabou de sair
            .contains("\"temperaturaC\"")
            .contains("\"alertaTemperatura\"");
    }

    @Test
    void hu08_requisicaoNaoAlocada_naoPodeSerEnviada() throws Exception {
        // Dado uma requisicao ainda pendente
        Instituicao hospital = instituicaoAprovada("HOSPITAL");
        Instituicao hemocentro = instituicaoAprovada("HEMOCENTRO");
        long requisicaoId = criarRequisicao(hospital);

        // Quando o hemocentro tenta despachar
        HttpResponse<String> envio = patch("/requisicoes/" + requisicaoId + "/enviar", hemocentro.token());

        // Entao o sistema impede a operacao
        assertThat(envio.statusCode()).isEqualTo(409);
    }

    @Test
    void hu08_hospitalRegistraEntrega_transporteFinalizadoERequisicaoEntregue() throws Exception {
        // Dado um transporte em andamento
        Instituicao hospital = instituicaoAprovada("HOSPITAL", "Caruaru");
        Instituicao hemocentro = instituicaoAprovada("HEMOCENTRO", "Recife");
        long requisicaoId = requisicaoAlocada(hospital, hemocentro);
        assertThat(patch("/requisicoes/" + requisicaoId + "/enviar", hemocentro.token()).statusCode()).isEqualTo(200);

        // Quando o hospital registra a entrega
        HttpResponse<String> recebimento = patch("/requisicoes/" + requisicaoId + "/receber", hospital.token());

        // Entao a requisicao assume o status "Entregue" (ATENDIDA)
        assertThat(recebimento.statusCode()).isEqualTo(200);
        assertThat(recebimento.body()).contains("\"status\":\"ATENDIDA\"");

        // E o transporte e finalizado: nao ha mais o que monitorar
        HttpResponse<String> monitoramento = get("/requisicoes/" + requisicaoId + "/monitoramento", hospital.token());
        assertThat(monitoramento.statusCode()).isEqualTo(409);
    }

    // =====================================================================================
    // HU09 - Visualizar indicadores da operacao
    // =====================================================================================

    @Test
    void hu09_usuarioAutenticado_vePainelDeIndicadores() throws Exception {
        // Dado que o usuario esta autenticado
        Instituicao hospital = instituicaoAprovada("HOSPITAL");

        // Quando acessa o painel de indicadores
        HttpResponse<String> painel = get("/indicadores", hospital.token());

        // Entao o sistema apresenta os principais indicadores da operacao
        assertThat(painel.statusCode()).isEqualTo(200);
        assertThat(painel.body())
            .contains("\"bolsasDisponiveis\"")
            .contains("\"bolsasVencendo\"")
            .contains("\"requisicoesPendentes\"")
            .contains("\"requisicoesEmTransporte\"")
            .contains("\"requisicoesEntregues\"")
            .contains("\"demandaPorComponente\"")
            .contains("\"estoquePorTipoSanguineo\"");
    }

    @Test
    void hu09_novaRequisicao_atualizaRequisicoesPendentes() throws Exception {
        Instituicao hospital = instituicaoAprovada("HOSPITAL");
        long antes = Long.parseLong(extrair(get("/indicadores", hospital.token()).body(), "requisicoesPendentes"));

        // Dado que uma nova requisicao foi criada
        criarRequisicao(hospital);

        // Quando os indicadores forem atualizados
        HttpResponse<String> painel = get("/indicadores", hospital.token());

        // Entao a quantidade de requisicoes pendentes deve ser atualizada
        long depois = Long.parseLong(extrair(painel.body(), "requisicoesPendentes"));
        assertThat(depois).isEqualTo(antes + 1);
    }

    @Test
    void hu09_semLogin_naoVeIndicadores() throws Exception {
        HttpResponse<String> painel = get("/indicadores", null);

        assertThat(painel.statusCode()).isEqualTo(401);
    }

    // =====================================================================================
    // CORS - o navegador so deixa o front chamar a API se a origem estiver liberada
    // (origens vem de CORS_ALLOWED_ORIGINS; sem a variavel, valem as portas locais)
    // =====================================================================================

    @Test
    void cors_origemDoFrontLocal_recebePermissao() throws Exception {
        // Dado o front rodando em localhost:8081 (o servico "web" do compose)
        // Quando o navegador pergunta se pode fazer um POST (preflight)
        HttpResponse<String> resposta = preflight("http://localhost:8081");

        // Entao a API autoriza aquela origem
        assertThat(resposta.headers().firstValue("Access-Control-Allow-Origin")).hasValue("http://localhost:8081");
    }

    @Test
    void cors_origemDesconhecida_naoRecebePermissao() throws Exception {
        // Dado um site qualquer, fora da lista de origens
        // Quando o navegador faz o preflight a partir dele
        HttpResponse<String> resposta = preflight("https://site-qualquer.example");

        // Entao a API nao autoriza (o navegador bloqueia a chamada)
        assertThat(resposta.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
    }

    // =====================================================================================
    // Auxiliares: montam os passos repetidos ("Dado ...") usando a propria API
    // =====================================================================================

    // id da instituicao + token do admin dela
    private record Instituicao(long id, String token) { }

    // cadastra, aprova com o ADMIN_SISTEMA e faz login com o admin da instituicao
    private Instituicao instituicaoAprovada(String tipo) throws Exception {
        return instituicaoAprovada(tipo, "Recife");
    }

    private Instituicao instituicaoAprovada(String tipo, String municipio) throws Exception {
        int n = SEQUENCIA.getAndIncrement();
        String email = tipo.toLowerCase() + n + "@aprovada.test";

        HttpResponse<String> cadastro = post("/instituicoes", corpoCadastro(cnpjDigitos(n), tipo, email, municipio), null);
        assertThat(cadastro.statusCode()).isEqualTo(201);
        // o primeiro "id" do JSON e o da instituicao (ela vem antes do administrador na resposta)
        long id = Long.parseLong(extrair(cadastro.body(), "id"));

        String tokenAdminSistema = login("admin@hemotrack.test", "senha-admin-123");
        HttpResponse<String> aprovacao = patch("/instituicoes/" + id + "/aprovar", tokenAdminSistema);
        assertThat(aprovacao.statusCode()).isEqualTo(200);

        return new Instituicao(id, login(email, "senha123"));
    }

    // requisicao aceita pelo hemocentro e com uma bolsa compativel ja alocada (status ALOCADA)
    private long requisicaoAlocada(Instituicao hospital, Instituicao hemocentro) throws Exception {
        long requisicaoId = criarRequisicao(hospital);

        HttpResponse<String> aceite = post("/requisicoes/" + requisicaoId + "/aceitar?hemocentroId=" + hemocentro.id(),
                                           null, hemocentro.token());
        assertThat(aceite.statusCode()).isEqualTo(200);

        long bolsaId = bolsaNoEstoque(hemocentro.id());
        HttpResponse<String> alocacao = patch("/requisicoes/" + requisicaoId + "/alocar",
                                              "{\"hemocomponenteIds\":[" + bolsaId + "]}", hemocentro.token());
        assertThat(alocacao.statusCode()).isEqualTo(200);
        return requisicaoId;
    }

    private long criarRequisicao(Instituicao hospital) throws Exception {
        HttpResponse<String> resposta = post("/requisicoes", corpoRequisicao(hospital.id(), 450.0), hospital.token());
        assertThat(resposta.statusCode()).isEqualTo(200);
        return Long.parseLong(extrair(resposta.body(), "id"));
    }

    private String login(String email, String senha) throws Exception {
        String corpo = "{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha);
        HttpResponse<String> resposta = post("/auth/login", corpo, null);
        assertThat(resposta.statusCode()).isEqualTo(200);
        return extrair(resposta.body(), "token");
    }

    // CNPJ unico por teste: 14 digitos a partir da sequencia
    private String cnpjDigitos(int n) {
        String cnpj = "%014d".formatted(10000000000000L + n);
        return cnpj;
    }

    private String cnpjFormatado(int n) {
        String d = cnpjDigitos(n);
        String cnpj = d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8) + "/"
                    + d.substring(8, 12) + "-" + d.substring(12);
        return cnpj;
    }

    private String corpoCadastro(String cnpj, String tipo, String email) {
        return corpoCadastro(cnpj, tipo, email, "Recife");
    }

    private String corpoCadastro(String cnpj, String tipo, String email, String municipio) {
        String corpo = """
            {"instituicao":{"razaoSocial":"Instituicao de Teste","cnpj":"%s","tipo":"%s",
             "endereco":"Rua Teste, 100","municipio":"%s","telefone":null},
             "administrador":{"nome":"Admin de Teste","email":"%s","senha":"senha123"}}
            """.formatted(cnpj, tipo, municipio, email);
        return corpo;
    }

    // bolsa de hemacias O- (serve para a requisicao padrao dos testes) no estoque do hemocentro
    private long bolsaNoEstoque(long hemocentroId) {
        Hemocomponente bolsa = new Hemocomponente();
        bolsa.setCodigoBolsa("BOLSA-TESTE-" + SEQUENCIA.getAndIncrement());
        bolsa.setInstituicaoAtualId(hemocentroId);
        bolsa.setTipo(TipoHemocomponente.HEMACIAS);
        bolsa.setAbo(TipoABO.O);
        bolsa.setRh(FatorRh.NEGATIVO);
        bolsa.setVolumeMl(450);
        bolsa.setDataColeta(LocalDate.now().minusDays(1));
        bolsa.setDataValidade(LocalDate.now().plusDays(30));
        return hemocomponentes.save(bolsa).getId();
    }

    private String corpoRequisicao(long hospitalId, double volumeMl) {
        String corpo = """
            {"hospitalId":%d,"tipo":"HEMACIAS","abo":"O","rh":"NEGATIVO","volumeMl":%s,"prioridade":"URGENCIA"}
            """.formatted(hospitalId, volumeMl);
        return corpo;
    }

    // le o valor de um campo simples do JSON (o primeiro que aparecer).
    // regex basta aqui: os campos lidos sao numeros ou textos sem aspas internas.
    private String extrair(String json, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\"\\s*:\\s*\"?([^\",}]+)").matcher(json);
        assertThat(m.find()).as("campo '%s' na resposta: %s", campo, json).isTrue();
        return m.group(1);
    }

    // ---- HTTP ----

    private HttpResponse<String> get(String caminho, String token) throws IOException, InterruptedException {
        HttpRequest.Builder req = HttpRequest.newBuilder(uri(caminho)).GET();
        return enviar(req, token);
    }

    private HttpResponse<String> post(String caminho, String json, String token) throws IOException, InterruptedException {
        HttpRequest.BodyPublisher corpo = json == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(json);
        HttpRequest.Builder req = HttpRequest.newBuilder(uri(caminho))
            .header("Content-Type", "application/json")
            .POST(corpo);
        return enviar(req, token);
    }

    private HttpResponse<String> patch(String caminho, String token) throws IOException, InterruptedException {
        HttpRequest.Builder req = HttpRequest.newBuilder(uri(caminho))
            .method("PATCH", HttpRequest.BodyPublishers.noBody());
        return enviar(req, token);
    }

    private HttpResponse<String> patch(String caminho, String json, String token) throws IOException, InterruptedException {
        HttpRequest.Builder req = HttpRequest.newBuilder(uri(caminho))
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(json));
        return enviar(req, token);
    }

    // preflight = a pergunta que o navegador faz antes de um POST vindo de outra origem
    private HttpResponse<String> preflight(String origem) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(uri("/instituicoes"))
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
            .header("Origin", origem)
            .header("Access-Control-Request-Method", "POST")
            .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> enviar(HttpRequest.Builder req, String token) throws IOException, InterruptedException {
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        return http.send(req.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String caminho) {
        return URI.create("http://localhost:" + porta + caminho);
    }
}
