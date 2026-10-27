package com.codigofacilito.adopcion.integration;

import com.codigofacilito.adopcion.entity.Mascota;
import com.codigofacilito.adopcion.entity.TipoMascota;
import com.codigofacilito.adopcion.entity.Usuario;
import com.codigofacilito.adopcion.repository.AdopcionRepository;
import com.codigofacilito.adopcion.repository.MascotaRepository;
import com.codigofacilito.adopcion.repository.TipoMascotaRepository;
import com.codigofacilito.adopcion.repository.UsuarioRepository;
import com.codigofacilito.adopcion.support.ClienteHttpPrueba;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Levanta el servidor HTTP real en un puerto aleatorio: cada solicitud se atiende en su propio
 * hilo del servidor y con su propia transacción, como ocurre con clientes reales concurrentes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdopcionConcurrenciaIT {

    private static final long ESPERA_MAXIMA_SEGUNDOS = 20;

    @Value("${local.server.port}")
    private int puerto;

    @Autowired
    private AdopcionRepository adopcionRepository;
    @Autowired
    private MascotaRepository mascotaRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private TipoMascotaRepository tipoMascotaRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private ClienteHttpPrueba cliente;
    private ExecutorService ejecutor;

    @BeforeEach
    void setUp() {
        adopcionRepository.deleteAllInBatch();
        mascotaRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
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
        assertThat(adopcionRepository.count()).isEqualTo(1);
        assertThat(mascotaRepository.findById(mascotaId)).get().extracting(Mascota::getDisponible).isEqualTo(false);
    }

    @Test
    void unaAdopcionEsperaMientrasOtraTransaccionBloqueaLaMascota() throws Exception {
        Long mascotaId = crearMascota();
        Long usuarioId = crearUsuario();
        CountDownLatch bloqueoTomado = new CountDownLatch(1);
        CountDownLatch liberarBloqueo = new CountDownLatch(1);

        Future<?> transaccionQueBloquea = ejecutor.submit(() -> transactionTemplate.executeWithoutResult(estado -> {
            mascotaRepository.findByIdForUpdate(mascotaId).orElseThrow();
            bloqueoTomado.countDown();
            esperar(liberarBloqueo);
        }));
        assertThat(bloqueoTomado.await(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS)).isTrue();

        Future<Integer> adopcion = ejecutor.submit(() -> cliente.adoptar(mascotaId, usuarioId).statusCode());

        // Mientras el bloqueo siga tomado, la solicitud no puede completarse.
        assertThatThrownBy(() -> adopcion.get(300, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
        assertThat(adopcionRepository.count()).isZero();

        liberarBloqueo.countDown();
        transaccionQueBloquea.get(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS);
        assertThat(adopcion.get(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS)).isEqualTo(201);
        assertThat(adopcionRepository.count()).isEqualTo(1);
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

    private Long crearMascota() {
        TipoMascota tipo = tipoMascotaRepository.findAll().getFirst();
        return mascotaRepository.save(new Mascota("Firulais", tipo, 2)).getId();
    }

    private Long crearUsuario() {
        return usuarioRepository.save(new Usuario("Adoptante", "c-" + UUID.randomUUID() + "@mail.com", "1155551234")).getId();
    }
}
