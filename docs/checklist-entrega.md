# Checklist de entrega

**Proyecto:** API-REST-AdopcionCodigoFacilito
**Versión revisada:** commit `cafff03`
**Fecha límite de entrega:** 5 de enero de 2027

Leyenda: ✅ cumplido y verificado · 🟡 preparado o parcial · ⏳ pendiente (acción del equipo)

> Este checklist se basa en los requisitos técnicos definidos para el proyecto. Antes de entregar,
> debe contrastarse con el enunciado oficial del bootcamp y con sus criterios de evaluación.

## 1. Tecnologías

| Requisito | Estado | Evidencia |
|---|---|---|
| Java 21 | ✅ | `pom.xml` (`java.version` 21) |
| Spring Boot estable compatible con Java 21 | ✅ | Spring Boot 4.1.2 (`pom.xml`) |
| Maven | ✅ | Maven Wrapper 3.9.16 (`mvnw`, `mvnw.cmd`) |
| Spring Web, Spring Data JPA, Bean Validation | ✅ | Dependencias en `pom.xml` |
| H2 para desarrollo y evaluación | ✅ | Perfiles `dev` y `test` |
| Spring Security con HTTP Basic, sin APIs obsoletas | ✅ | `SecurityConfig` con `SecurityFilterChain` |
| Actuator | ✅ | `health` e `info` expuestos |
| OpenAPI y Swagger UI | ✅ | springdoc-openapi 3.2.0 |
| JUnit 5, Mockito, MockMvc | ✅ | JUnit Jupiter (versión 6.0.3, la que incluye Spring Boot 4; misma API de Jupiter), Mockito 5.23.0, MockMvc |

## 2. Modelo de datos

| Requisito | Estado | Evidencia |
|---|---|---|
| `TipoMascota`, `Mascota`, `Usuario`, `Adopcion` con los campos pedidos | ✅ | Paquete `entity` |
| `edad` obligatoria y ≥ 0 | ✅ | `@PositiveOrZero` en DTOs; `CHECK (edad >= 0)`; `PersistenciaIT`, `RestriccionesPostgresIT` |
| `disponible` inicia en `true` | ✅ | `Mascota`; `MascotaServiceTest.registrarGuardaLaMascotaDisponibleConSuTipo` |
| `correoElectronico` válido y único | ✅ | `@Email`; `uk_usuario_correo`; `UsuarioIT`, `RestriccionesPostgresIT` |
| `fechaAdopcion` asignada por el servidor | ✅ | `Clock` inyectado; `AdopcionServiceTest` |
| Restricción única sobre la mascota en adopciones | ✅ | `uk_adopcion_mascota`; `PersistenciaIT`, `RestriccionesPostgresIT` |
| Datos iniciales de tipos de mascota | ✅ | `data.sql` (H2); migración V2 (PostgreSQL) |

## 3. Endpoints

| Requisito | Estado | Evidencia |
|---|---|---|
| `POST/GET/PUT/DELETE /api/mascotas` (GET solo disponibles) | ✅ | `MascotaController`; `MascotaControllerTest`, `MascotaIT`, `AdopcionIT` |
| `POST /api/usuarios` | ✅ | `UsuarioController.registrar`; `UsuarioControllerTest`, `UsuarioIT` |
| `POST /api/adopciones` | ✅ | `AdopcionController.adoptar`; `AdopcionControllerTest`, `AdopcionIT` |
| Requests con ids (`tipoMascotaId`, `mascotaId`, `usuarioId`) | ✅ | `MascotaRequest`, `AdopcionRequest` |
| Extra: `GET /api/usuarios`, `GET/PUT/DELETE /api/usuarios/{id}` | ✅ | `UsuarioController`; `UsuarioControllerTest`, `UsuarioIT` |

## 4. Reglas de negocio

| Requisito | Estado | Evidencia |
|---|---|---|
| No adoptar mascota o usuario inexistente (404) | ✅ | `AdopcionService.adoptar`; `AdopcionIT.adoptarConUsuarioOMascotaInexistenteDevuelve404SinCambios` |
| No adoptar mascota no disponible (409) | ✅ | `AdopcionIT.unaMascotaNoPuedeAdoptarseDosVeces` |
| Adopción cambia `disponible` a `false` en una única transacción | ✅ | `AdopcionIT.adopcionCambiaDisponibilidadYGetMascotasLaExcluye`; `AdopcionRollbackIT` |
| Protección ante solicitudes concurrentes | ✅ | Bloqueo pesimista + UNIQUE; `AdopcionConcurrenciaIT`, `ConcurrenciaPostgresIT` |
| PUT no reactiva una mascota adoptada (409) | ✅ | `AdopcionIT.unaMascotaAdoptadaNoPuedeReactivarseMediantePut` |
| No eliminar mascotas o usuarios con adopciones (409) | ✅ | `AdopcionIT.noSePuedenEliminarMascotasNiUsuariosConAdopciones` |
| Correos duplicados rechazados (409) | ✅ | `UsuarioIT.unCorreoDuplicadoSeRechazaAunqueCambienLasMayusculas` |
| Validación de entradas (400) | ✅ | `@Valid` en los cinco cuerpos; pruebas de 400 en los tres controllers |

## 5. Arquitectura y calidad

| Requisito | Estado | Evidencia |
|---|---|---|
| Capas `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `exception`, `config` | ✅ | Estructura de paquetes |
| Controllers sin acceso a repositories | ✅ | Ningún archivo de `controller` importa `repository` ni `entity` |
| Lógica de negocio y transacciones en servicios | ✅ | `@Transactional` solo en `MascotaService`, `UsuarioService`, `AdopcionService` |
| Inyección por constructor | ✅ | Todas las clases con dependencias |
| DTOs de entrada y salida; sin exponer entidades | ✅ | Paquete `dto`; los controllers devuelven DTOs envueltos en modelos HAL |

## 6. Seguridad

| Requisito | Estado | Evidencia |
|---|---|---|
| `/api/**` protegido con HTTP Basic | ✅ | `SecurityConfig`; `DocumentacionYMonitoreoIT.losEndpointsDeNegocioSiguenRechazandoSolicitudesSinAutenticacion` |
| Credenciales configurables por variables de entorno | ✅ | `APP_USER_*`, `APP_ADMIN_*`; valores de demostración solo en `dev` |
| Usuarios adoptantes distintos de las cuentas de acceso | ✅ | `SecurityProperties` frente a la entidad `Usuario` |
| Contraseñas con `PasswordEncoder` | ✅ | BCrypt (`PasswordEncoderFactories`) |
| Acceso a Swagger y Actuator documentado | ✅ | README e informe |
| Consola H2 y CSRF limitados y justificados | ✅ | Cadena propia de la consola solo en `dev`; alcance de CSRF documentado |
| Advertencia de HTTPS fuera del entorno local | ✅ | README e informe |

## 7. Respuestas y errores

| Requisito | Estado | Evidencia |
|---|---|---|
| `@RestControllerAdvice` | ✅ | `GlobalExceptionHandler` |
| 201 + Location, 200, 204, 400, 401, 403, 404, 409 | ✅ | Pruebas de controller e integración |
| Formato de error con fecha, estado, mensaje, ruta y detalles | ✅ | `ErrorResponse` |
| Sin trazas ni información sensible | ✅ | `AdopcionControllerTest.errorInesperadoDevuelve500SinDetallesInternos` |

## 8. Pruebas

| Requisito | Estado | Evidencia |
|---|---|---|
| Unitarias de servicios con JUnit y Mockito | ✅ | `MascotaServiceTest`, `UsuarioServiceTest`, `AdopcionServiceTest` |
| Controllers con MockMvc y Spring Security Test | ✅ | `MascotaControllerTest`, `UsuarioControllerTest`, `AdopcionControllerTest` |
| Integración con H2 | ✅ | Clases `*IT` del paquete `integration` |
| Casos exitosos, inválidos, inexistentes, conflictos y sin autenticación | ✅ | Ver informe, sección 15 |
| La adopción cambia la disponibilidad | ✅ | `AdopcionIT`, `AdopcionPostgresIT` |
| Una mascota no se adopta dos veces | ✅ | `AdopcionIT`, `AdopcionConcurrenciaIT`, `ConcurrenciaPostgresIT` |
| Sin adopción parcial si falla la operación | ✅ | `AdopcionRollbackIT`, `AdopcionIT`, `AdopcionPostgresIT` |
| `GET /api/mascotas` no devuelve adoptadas | ✅ | `AdopcionIT`, `AdopcionPostgresIT` |
| Separación clara entre unitarias e integración | ✅ | Surefire (`*Test`) y Failsafe (`*IT`, `*PostgresIT` con `-Ppostgres`) |
| **Resultado:** 183 pruebas con H2, 131 con el perfil PostgreSQL, 204 distintas, 0 fallos | ✅ | `mvnw clean verify` y `mvnw clean verify -Ppostgres` sobre `cafff03` |

## 9. Documentación y monitoreo

| Requisito | Estado | Evidencia |
|---|---|---|
| OpenAPI con endpoints, esquemas, errores y autenticación Basic | ✅ | `OpenApiConfig`; `DocumentacionYMonitoreoIT` |
| Swagger UI funcional | 🟡 | Comprobado por HTTP y con pruebas automatizadas; uso manual en navegador pendiente |
| Actuator `health` e `info` sin endpoints sensibles | ✅ | `DocumentacionYMonitoreoIT` |
| README con requisitos, ejecución, pruebas y ejemplos curl | ✅ | `README.md` |
| Guía de la consola H2, variables de entorno y credenciales | ✅ | `README.md` |

## 10. Puntos extra

| Extra | Estado | Evidencia |
|---|---|---|
| Validación de inputs | ✅ | DTOs con Bean Validation, `@Valid`, normalización del correo; `UsuarioRequestTest` |
| CRUD de Usuario | ✅ | Crear, listar, consultar, actualizar y eliminar; ver informe, sección 6.1 |
| Hyperlinks (HATEOAS) | ✅ | HAL con enlaces por estado y rol; `HateoasIT`; demostración con Docker y H2. La navegación por enlaces no se probó específicamente contra PostgreSQL. |
| Base de datos distinta de H2 | ✅ con PostgreSQL · 🟡 Supabase | PostgreSQL 17: suite `-Ppostgres` y comprobaciones con JAR y contenedor. Supabase: configuración preparada, **conexión y SSL no probados** |
| Perfil PostgreSQL con variables de entorno, sin secretos | ✅ | `application-postgres.properties`, `.env.example` |
| Docker | ✅ | `Dockerfile`, `.dockerignore`; imagen verificada con H2 (versión actual) y con PostgreSQL (versión previa a HATEOAS) |

## 11. Entregables

| Entregable | Estado | Observación |
|---|---|---|
| Título y descripción del proyecto | ✅ | Informe, portada y sección 1 |
| Repositorio público organizado y sin secretos | ⏳ | Revisión sin secretos realizada; falta crear el repositorio remoto y publicar (push) |
| Documento para exportar a PDF | 🟡 | `docs/informe-proyecto.md` listo; falta completar integrantes, URL y fecha, y exportar |
| Guion del video (5 a 8 minutos) | 🟡 | `docs/guion-video.md` listo; falta ensayar y grabar |
| Checklist de cumplimiento | ✅ | Este documento |
| Plan de trabajo hasta el 5 de enero de 2027 | ✅ | Sección 13 |

## 12. Verificaciones pendientes

| Pendiente | Responsable | Estado |
|---|---|---|
| Contrastar este checklist con el enunciado oficial y los criterios de evaluación | Equipo | ⏳ |
| Confirmar si la institución exige declarar herramientas utilizadas | Equipo | ⏳ |
| Ejecutar la aplicación y las pruebas desde IntelliJ IDEA | [Integrante] | ⏳ |
| Uso manual de Swagger UI en navegador (*Authorize*, *Try it out*) | [Integrante] | ⏳ |
| Restaurar las carpetas de Windows en el `PATH` del sistema (o usar Git Bash) | [Integrante] | ⏳ |
| Opcional: probar la conexión con Supabase (pooler en modo *session*, SSL `verify-full`) | [Integrante] | ⏳ |
| Opcional: repetir la comprobación Docker + PostgreSQL con la versión HAL actual | [Integrante] | ⏳ |

## 13. Plan de trabajo hasta el 5 de enero de 2027

| Período | Actividad |
|---|---|
| Hasta el 8 de noviembre de 2026 | Contrastar con el enunciado oficial; verificar en IntelliJ y Swagger UI; corregir lo que surja |
| 9 al 22 de noviembre | Crear el repositorio público, publicar el código y revisar el README desde GitHub |
| 23 de noviembre al 6 de diciembre | Completar y exportar el informe a PDF; revisión cruzada entre integrantes |
| 7 al 20 de diciembre | Ensayar y grabar el video; revisar duración y audio |
| 21 de diciembre al 2 de enero de 2027 | Revisión final del checklist, ejecución completa de las pruebas sobre la versión entregada |
| 3 al 5 de enero de 2027 | Entrega (margen para imprevistos) |
