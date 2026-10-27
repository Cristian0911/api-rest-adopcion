package com.codigofacilito.adopcion.postgres;

import com.codigofacilito.adopcion.support.ClienteHttpPrueba;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Concurrencia contra PostgreSQL real a través del servidor HTTP. El perfil de prueba fija
 * lock_timeout en 1 s (DB_LOCK_TIMEOUT) para comprobar también el caso de espera agotada.
 */
class ConcurrenciaPostgresIT extends PostgresIntegracionBase {

    private static final long ESPERA_MAXIMA_SEGUNDOS = 20;

    private ClienteHttpPrueba cliente;
    private ExecutorService ejecutor;

    @BeforeEach
    void setUp() {
        cliente = new ClienteHttpPrueba(puerto);
        ejecutor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        ejecutor.shutdownNow();
        assertThat(ejecutor.awaitTermination(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS))
                .as("los hilos de la prueba deben terminar").isTrue();
        cliente.close();
    }

    @RepeatedTest(5)
    void dosSolicitudesSimultaneasProducenUnaSolaAdopcion() throws Exception {
        Long mascotaId = crearMascota();
        Long primerUsuario = crearUsuario();
        Long segundoUsuario = crearUsuario();
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch largada = new CountDownLatch(1);

        Future<Integer> primera = ejecutor.submit(() -> adoptarAlUnisono(mascotaId, primerUsuario, listos, largada));
        Future<Integer> segunda = ejecutor.submit(() -> adoptarAlUnisono(mascotaId, segundoUsuario, listos, largada));
        assertThat(listos.await(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS)).isTrue();
        largada.countDown();

        List<Integer> estados = List.of(
                primera.get(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS),
                segunda.get(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS));

        assertThat(estados).containsExactlyInAnyOrder(201, 409);
        assertThat(adopcionesEnBd()).isEqualTo(1);
        assertThat(disponibleEnBd(mascotaId)).isFalse();
    }

    @Test
    void unaAdopcionEsperaMientrasOtraTransaccionBloqueaLaMascota() throws Exception {
        Long mascotaId = crearMascota();
        Long usuarioId = crearUsuario();
        CountDownLatch bloqueoTomado = new CountDownLatch(1);
        CountDownLatch liberarBloqueo = new CountDownLatch(1);

        Future<?> transaccionQueBloquea = ejecutor.submit(() -> bloquearMascota(mascotaId, bloqueoTomado, liberarBloqueo));
        assertThat(bloqueoTomado.await(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS)).isTrue();

        Future<HttpResponse<String>> adopcion = ejecutor.submit(() -> cliente.adoptar(mascotaId, usuarioId));

        // Menos que el lock_timeout de 1 s: la solicitud sigue esperando el bloqueo.
        assertThatThrownBy(() -> adopcion.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
        liberarBloqueo.countDown();
        transaccionQueBloquea.get(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS);

        assertThat(adopcion.get(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS).statusCode()).isEqualTo(201);
        assertThat(adopcionesEnBd()).isEqualTo(1);
    }

    @Test
    void siElBloqueoNoSeLiberaATiempoLaSolicitudRecibe409YNoCambiaNada() throws Exception {
        Long mascotaId = crearMascota();
        Long usuarioId = crearUsuario();
        CountDownLatch bloqueoTomado = new CountDownLatch(1);
        CountDownLatch liberarBloqueo = new CountDownLatch(1);

        Future<?> transaccionQueBloquea = ejecutor.submit(() -> bloquearMascota(mascotaId, bloqueoTomado, liberarBloqueo));
        assertThat(bloqueoTomado.await(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS)).isTrue();
        try {
            HttpResponse<String> respuesta = cliente.adoptar(mascotaId, usuarioId);

            assertThat(respuesta.statusCode()).isEqualTo(409);
            assertThat(respuesta.body()).contains("intente nuevamente");
        } finally {
            liberarBloqueo.countDown();
            transaccionQueBloquea.get(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS);
        }

        assertThat(adopcionesEnBd()).isZero();
        assertThat(disponibleEnBd(mascotaId)).isTrue();
    }

    private void bloquearMascota(Long mascotaId, CountDownLatch bloqueoTomado, CountDownLatch liberarBloqueo) {
        transactionTemplate.executeWithoutResult(estado -> {
            mascotaRepository.findByIdForUpdate(mascotaId).orElseThrow();
            bloqueoTomado.countDown();
            esperar(liberarBloqueo);
        });
    }

    private int adoptarAlUnisono(Long mascotaId, Long usuarioId, CountDownLatch listos, CountDownLatch largada)
            throws Exception {
        listos.countDown();
        esperar(largada);
        return cliente.adoptar(mascotaId, usuarioId).statusCode();
    }

    private static void esperar(CountDownLatch latch) {
        try {
            if (!latch.await(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Tiempo de espera agotado en la sincronización de la prueba");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(ex);
        }
    }
}
