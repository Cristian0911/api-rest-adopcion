package com.codigofacilito.adopcion.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import static com.codigofacilito.adopcion.support.CredencialesPrueba.USER;
import static com.codigofacilito.adopcion.support.CredencialesPrueba.USER_PASSWORD;

/**
 * Cliente HTTP real para pruebas con servidor en puerto aleatorio: cada solicitud se atiende en su
 * propio hilo y transacción, como ocurre con clientes concurrentes reales.
 */
public final class ClienteHttpPrueba implements AutoCloseable {

    private static final Duration TIEMPO_MAXIMO = Duration.ofSeconds(20);

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final String urlBase;
    private final String autorizacion = "Basic " + Base64.getEncoder()
            .encodeToString((USER + ":" + USER_PASSWORD).getBytes(StandardCharsets.UTF_8));

    public ClienteHttpPrueba(int puerto) {
        this.urlBase = "http://localhost:" + puerto;
    }

    public HttpResponse<String> adoptar(Long mascotaId, Long usuarioId) throws IOException, InterruptedException {
        HttpRequest solicitud = HttpRequest.newBuilder(URI.create(urlBase + "/api/adopciones"))
                .timeout(TIEMPO_MAXIMO)
                .header("Authorization", autorizacion)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"mascotaId\": %d, \"usuarioId\": %d}".formatted(mascotaId, usuarioId)))
                .build();
        return httpClient.send(solicitud, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    @Override
    public void close() {
        httpClient.close();
    }
}
