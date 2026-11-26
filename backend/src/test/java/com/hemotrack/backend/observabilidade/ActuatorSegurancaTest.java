package com.hemotrack.backend.observabilidade;

import static org.assertj.core.api.Assertions.assertThat;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.hemotrack.backend.service.TelemetriaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:observabilidade;DB_CLOSE_DELAY=-1",
    "hemotrack.jwt.segredo=segredo-de-teste-com-pelo-menos-32-caracteres",
    "management.server.port=0"
})
class ActuatorSegurancaTest {
    @Value("${local.server.port}") int publica;
    @Value("${local.management.port}") int interna;
    @Autowired TelemetriaService telemetria;
    final HttpClient http = HttpClient.newHttpClient();

    HttpResponse<String> get(int porta, String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + porta + path))
            .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void prometheusExpostoApenasNaPortaInternaComTagsEMetricasDeNegocio() throws Exception {
        // Scrape apos trafego real de observacoes detecta colisoes entre familias
        // de metricas automaticas e timers de negocio (nao basta raspar a partida).
        telemetria.consultar(() -> "ok");
        try {
            telemetria.consultar(() -> { throw new IllegalArgumentException("erro esperado"); });
        } catch (IllegalArgumentException esperado) { }
        assertThat(get(publica, "/health").statusCode()).isEqualTo(200);
        assertThat(get(publica, "/health").body()).contains("UP").doesNotContain("components", "jdbc");
        var externo = get(publica, "/actuator/prometheus");
        assertThat(externo.statusCode()).isIn(401, 403, 404);
        assertThat(externo.body()).doesNotContain("jvm_memory_used_bytes");
        var interno = get(interna, "/actuator/prometheus");
        assertThat(interno.statusCode()).isEqualTo(200);
        assertThat(interno.body()).contains("application=\"hemotrack-backend\"",
            "hemotrack_requisicoes_criadas_total", "hemotrack_requisicoes_consulta_seconds_count",
            "jvm_memory_used_bytes", "hikaricp_connections");
        assertThat(get(interna, "/actuator/health").body()).contains("UP");
        assertThat(get(interna, "/actuator/info").statusCode()).isEqualTo(200);
        assertThat(get(interna, "/actuator/metrics").statusCode()).isEqualTo(200);
    }
}
