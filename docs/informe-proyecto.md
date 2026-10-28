# API REST de Adopción de Mascotas

**Proyecto final — Bootcamp de Backend con Java y Spring (Código Facilito)**

| | |
|---|---|
| Proyecto | API-REST-AdopcionCodigoFacilito |
| Integrantes | [Nombre 1] · [Nombre 2] · [Nombre 3] |
| Repositorio | [URL del repositorio público] |
| Versión documentada | commit `cafff03` (rama `main`) |
| Fecha | [Fecha de entrega] |

---

## 1. Resumen

API REST para gestionar la adopción de mascotas: registro de mascotas y de usuarios adoptantes, consulta de mascotas disponibles y registro de adopciones. Aplica reglas de negocio transaccionales y seguras ante solicitudes concurrentes, protege los endpoints con HTTP Basic y dos roles, responde en formato HAL con hipervínculos (HATEOAS), documenta la API con OpenAPI/Swagger UI y se ejecuta con H2 (desarrollo y pruebas), con PostgreSQL mediante migraciones Flyway, o dentro de un contenedor Docker.

El proyecto cuenta con 204 pruebas automatizadas distintas: unitarias, de controller y de integración con H2 y con PostgreSQL real en contenedores.

## 2. Objetivos

1. Exponer una API REST organizada en capas, con DTOs de entrada y salida y sin exponer entidades JPA.
2. Aplicar las reglas de adopción de forma consistente, incluso ante solicitudes concurrentes.
3. Proteger la API con autenticación HTTP Basic y autorización por roles.
4. Responder con códigos HTTP correctos y un formato de error único.
5. Exponer hipervínculos HAL que reflejen las acciones permitidas para cada recurso.
6. Documentar la API y monitorear su estado.
7. Verificar el comportamiento con pruebas automatizadas en varios niveles.
8. Funcionar con una base de datos distinta de H2 y ejecutarse en un contenedor.

## 3. Tecnologías

| Componente | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.2 |
| Spring Framework / Spring Security | 7.0.10 / 7.1.2 |
| Spring Data JPA / Hibernate ORM | 4.1.2 / 7.4.13.Final |
| Hibernate Validator (Jakarta Bean Validation) | 9.1.5.Final |
| Spring HATEOAS | 3.1.3 |
| springdoc-openapi (Swagger UI 5.34.0) | 3.2.0 |
| H2 Database | 2.4.240 |
| PostgreSQL (driver JDBC / imagen de pruebas) | 42.7.14 / `postgres:17-alpine` |
| Flyway | 12.4.0 |
| JUnit Jupiter / Mockito / Testcontainers | 6.0.3 / 5.23.0 / 2.0.5 |
| Maven (Maven Wrapper) | 3.9.16 |
| Imágenes Docker | `maven:3.9.16-eclipse-temurin-21` (compilación), `eclipse-temurin:21-jre-noble` (ejecución) |

## 4. Arquitectura

### 4.1 Capas

```
com.codigofacilito.adopcion
├── config       Seguridad, OpenAPI, Clock, propiedades de las cuentas de acceso
├── controller   HTTP: valida la entrada, delega en el servicio y arma la respuesta
│   └── hateoas  Ensambladores de enlaces HAL
├── service      Reglas de negocio y transacciones
├── repository   Interfaces Spring Data JPA (persistencia)
├── entity       Entidades JPA
├── dto          Records de request/response y formato de error
├── mapper       Conversión entidad <-> DTO
└── exception    Excepciones de negocio y manejo global de errores
```

Flujo de una solicitud:

```
Cliente ──HTTP──> SecurityFilterChain ──> Controller ──> Service ──> Repository ──> Base de datos
                                              │             │
                                              │             └── Mapper (entidad -> DTO)
                                              └── Assembler HATEOAS (DTO -> EntityModel con _links)
Excepciones ──> GlobalExceptionHandler ──> ErrorResponse (application/json)
```

Todas las dependencias se inyectan por constructor.

### 4.2 Revisión de la separación de capas

| Consideración | Evidencia |
|---|---|
| Los controllers no acceden a repositories | `MascotaController`, `UsuarioController` y `AdopcionController` solo reciben por constructor su servicio y su ensamblador; ningún archivo de `controller` importa clases de `repository` ni de `entity`. |
| Las reglas de negocio y las transacciones están en los servicios | `@Transactional` aparece únicamente en `MascotaService`, `UsuarioService` y `AdopcionService` (por defecto `readOnly = true`; escritura en `registrar`, `actualizar`, `eliminar` y `adoptar`). Todas las excepciones de negocio (`RecursoNoEncontradoException`, `ConflictoException`) se lanzan desde los servicios. |
| Los repositories se limitan a persistencia | Solo declaran consultas: `findByDisponibleTrueOrderByIdAsc`, `findWithTipoById`, `findByIdForUpdate` (bloqueo pesimista), `existsByCorreoElectronico`, `existsByCorreoElectronicoAndIdNot`, `existsByMascotaId`, `existsByUsuarioId`, `findIdByMascotaId`, `findWithDetallesById`. No contienen reglas de negocio. |
| Los ensambladores HATEOAS no consultan repositories | `MascotaModelAssembler`, `UsuarioModelAssembler` y `AdopcionModelAssembler` solo dependen de las clases controller (para construir enlaces con `linkTo(methodOn(...))`) y de los DTOs. El estado que necesitan (si una mascota tiene adopción, si un usuario tiene adopciones) lo obtiene el controller a través de `MascotaService.adopcionDe` y `UsuarioService.tieneAdopciones`. |
| Los servicios no tienen responsabilidades HTTP | Ningún archivo de `service` importa `org.springframework.http`, `org.springframework.web`, `org.springframework.hateoas` ni `jakarta.servlet`. La traducción a códigos HTTP la hace `GlobalExceptionHandler`. |

Observaciones: el DTO `ErrorResponse` usa `HttpStatus` en su método de fábrica porque representa el contrato HTTP de los errores; y en `GET /api/mascotas/{id}` el controller invoca dos métodos de solo lectura del servicio (`obtener` y `adopcionDe`). Los enlaces reflejan el estado en ese momento y el servidor vuelve a validar cada acción.

## 5. Modelo de datos

```
TIPOS_MASCOTA 1 ──< N MASCOTAS 1 ──< 0..1 ADOPCIONES >── N : 1 USUARIOS
                                  (UNIQUE adopciones.mascota_id)
```

| Entidad / tabla | Campos | Restricciones |
|---|---|---|
| `TipoMascota` / `tipos_mascota` | `id`, `nombre` | `nombre` NOT NULL y UNIQUE |
| `Mascota` / `mascotas` | `id`, `nombre`, `tipo_mascota_id`, `edad`, `disponible` | `nombre` NOT NULL; FK a tipo NOT NULL; `edad` NOT NULL y `CHECK (edad >= 0)`; `disponible` NOT NULL, inicia en `true` |
| `Usuario` / `usuarios` | `id`, `nombre`, `correo_electronico`, `telefono` | todos NOT NULL; `correo_electronico` UNIQUE (`uk_usuario_correo`) |
| `Adopcion` / `adopciones` | `id`, `mascota_id`, `usuario_id`, `fecha_adopcion` | FKs NOT NULL sin borrado en cascada; `UNIQUE (mascota_id)` (`uk_adopcion_mascota`); `fecha_adopcion` asignada por el servidor |

- Relaciones `@ManyToOne` con carga diferida: `Mascota → TipoMascota`, `Adopcion → Mascota`, `Adopcion → Usuario`.
- La restricción UNIQUE sobre `adopciones.mascota_id` garantiza en la base de datos que una mascota tenga como máximo una adopción.
- Las claves foráneas sin cascada impiden, también en la base, borrar mascotas o usuarios con adopciones.
- Datos iniciales: tipos Perro, Gato, Conejo y Ave (`data.sql` en H2; migración Flyway V2 en PostgreSQL).

## 6. Endpoints

Todos los endpoints `/api/**` requieren HTTP Basic.

| Método | Ruta | Rol | Éxito | Errores |
|---|---|---|---|---|
| POST | `/api/mascotas` | ADMIN | 201 + `Location` | 400, 401, 403, 404 |
| GET | `/api/mascotas` | USER o ADMIN | 200 (solo disponibles) | 401 |
| GET | `/api/mascotas/{id}` | USER o ADMIN | 200 | 400, 401, 404 |
| PUT | `/api/mascotas/{id}` | ADMIN | 200 | 400, 401, 403, 404, 409 |
| DELETE | `/api/mascotas/{id}` | ADMIN | 204 | 401, 403, 404, 409 |
| POST | `/api/usuarios` | USER o ADMIN | 201 + `Location` | 400, 401, 409 |
| GET | `/api/usuarios` | USER o ADMIN | 200 | 401 |
| GET | `/api/usuarios/{id}` | USER o ADMIN | 200 | 400, 401, 404 |
| PUT | `/api/usuarios/{id}` | USER o ADMIN | 200 | 400, 401, 404, 409 |
| DELETE | `/api/usuarios/{id}` | ADMIN | 204 | 401, 403, 404, 409 |
| POST | `/api/adopciones` | USER o ADMIN | 201 + `Location` | 400, 401, 404, 409 |
| GET | `/api/adopciones/{id}` | USER o ADMIN | 200 | 400, 401, 404 |

### 6.1 CRUD de Usuario

| Operación | Endpoint | Regla relevante |
|---|---|---|
| Crear | `POST /api/usuarios` | correo único, sin distinguir mayúsculas (409 si existe) |
| Consultar | `GET /api/usuarios`, `GET /api/usuarios/{id}` | 404 si no existe |
| Actualizar | `PUT /api/usuarios/{id}` | el correo no puede pertenecer a otro usuario (409); el usuario puede conservar el suyo |
| Eliminar | `DELETE /api/usuarios/{id}` | solo ADMIN; 409 si el usuario tiene adopciones |

Implementación: `UsuarioController` (`registrar`, `listar`, `obtener`, `actualizar`, `eliminar`) y `UsuarioService`; los duplicados se detectan con `existsByCorreoElectronico` y `existsByCorreoElectronicoAndIdNot`, y las adopciones asociadas con `existsByUsuarioId`. La base de datos respalda ambas reglas con `uk_usuario_correo` y `fk_adopcion_usuario`.

## 7. Validaciones

- Jakarta Bean Validation en los DTOs de entrada y `@Valid` en los cinco cuerpos de request (POST/PUT de mascotas y usuarios, POST de adopciones).

| DTO | Validaciones |
|---|---|
| `MascotaRequest` / `MascotaUpdateRequest` | `nombre`: `@NotBlank`, `@Size(max=100)`; `tipoMascotaId`: `@NotNull`, `@Positive`; `edad`: `@NotNull`, `@PositiveOrZero`; `disponible` opcional en la actualización |
| `UsuarioRequest` | `nombre`: `@NotBlank`, `@Size(max=100)`; `correoElectronico`: `@NotBlank`, `@Email`, `@Size(max=150)`; `telefono`: `@NotBlank`, `@Pattern` (7 a 20 caracteres) |
| `AdopcionRequest` | `mascotaId` y `usuarioId`: `@NotNull`, `@Positive` |

- **Correo electrónico:** se eliminan los espacios iniciales y finales al construir el `UsuarioRequest`, antes de la validación y de la búsqueda de duplicados. Los espacios internos se rechazan (400). Se guarda en minúsculas.
- Errores de entrada: `400` con detalle por campo; JSON mal formado o id no numérico: `400`; `Content-Type` no JSON: `415`.
- Las restricciones de la base de datos (NOT NULL, UNIQUE, CHECK, FK) actúan como última barrera.

## 8. Seguridad

- **HTTP Basic** sobre todos los endpoints `/api/**`, con `SecurityFilterChain` (sin APIs obsoletas) y sesión `STATELESS`.
- **Roles:** `USER` consulta, registra y actualiza usuarios y registra adopciones; `ADMIN` además crea, actualiza y elimina mascotas y elimina usuarios. Permisos insuficientes: `403`.
- **Cuentas de acceso separadas de los adoptantes:** las cuentas autentican a quien opera la API; la entidad `Usuario` representa adoptantes y no tiene contraseña.
- **Credenciales por variables de entorno** (`APP_USER_*`, `APP_ADMIN_*`). Los valores de demostración existen solo en el perfil `dev`; fuera de él, la aplicación no arranca si faltan.
- **Contraseñas** codificadas con BCrypt (`PasswordEncoder` delegante).
- **401 y 403** con el mismo formato JSON de error y `WWW-Authenticate: Basic`.
- **Endpoints públicos:** Swagger UI, `/v3/api-docs`, `/actuator/health` y `/actuator/info`. Ningún otro endpoint de Actuator está expuesto.
- **Consola H2:** solo en el perfil `dev`, con su propia cadena de seguridad, rol ADMIN y frames del mismo origen limitados a sus rutas.
- **CSRF:** desactivado en `/api/**` porque los clientes previstos envían `Authorization` explícitamente (curl, Postman, Swagger UI con *Authorize*, otro backend). `STATELESS` no elimina por sí solo el riesgo: si un navegador guarda credenciales Basic introducidas en su diálogo nativo, las reenvía automáticamente. Por eso la API no debe usarse desde un navegador con ese diálogo; un frontend web requeriría reactivar CSRF o usar tokens explícitos.
- **HTTPS:** HTTP Basic solo codifica las credenciales en Base64; fuera del entorno local es obligatorio usar HTTPS.

## 9. Formato de error

Todos los errores son `application/json`:

```json
{
  "timestamp": "2026-10-27T17:46:09.267772249Z",
  "status": 409,
  "error": "Conflict",
  "message": "La mascota con id 1 ya fue adoptada o no está disponible",
  "path": "/api/adopciones",
  "details": []
}
```

`details` contiene los errores por campo en las validaciones (400). No se exponen trazas ni información interna; un error inesperado responde `500` con un mensaje genérico.

## 10. HATEOAS (HAL)

Las respuestas exitosas son `application/hal+json`. Cada recurso incluye `_links`; las listas usan `_embedded.mascotas` o `_embedded.usuarios`.

### 10.1 Relaciones

| Recurso | Relación | Destino | Condición |
|---|---|---|---|
| Mascota | `self` | GET `/api/mascotas/{id}` | siempre |
| | `mascotas-disponibles` | GET `/api/mascotas` | siempre |
| | `adoptar` | POST `/api/adopciones` | disponible y sin adopción |
| | `adopcion` | GET `/api/adopciones/{id}` | tiene adopción |
| | `actualizar` | PUT `/api/mascotas/{id}` | cuenta ADMIN |
| | `eliminar` | DELETE `/api/mascotas/{id}` | cuenta ADMIN y sin adopción |
| Usuario | `self`, `usuarios`, `actualizar` | GET / GET lista / PUT | siempre |
| | `eliminar` | DELETE `/api/usuarios/{id}` | cuenta ADMIN y sin adopciones (solo en el detalle) |
| Adopción | `self`, `mascota`, `usuario` | GET | siempre |
| Listas | `self` | GET de la lista | siempre |

No se generan enlaces a operaciones inexistentes. **Los enlaces no reemplazan la autorización ni las reglas de negocio**: el servidor vuelve a verificar cada solicitud (`403` sin el rol, `409` si el estado cambió).

### 10.2 Ejemplos

Mascota recién registrada, vista por ADMIN:

```json
{
  "_links": {
    "self": { "href": "http://localhost:8080/api/mascotas/1" },
    "mascotas-disponibles": { "href": "http://localhost:8080/api/mascotas" },
    "adoptar": { "href": "http://localhost:8080/api/adopciones" },
    "actualizar": { "href": "http://localhost:8080/api/mascotas/1" },
    "eliminar": { "href": "http://localhost:8080/api/mascotas/1" }
  },
  "id": 1,
  "nombre": "Firulais",
  "tipoMascota": { "id": 1, "nombre": "Perro" },
  "edad": 3,
  "disponible": true
}
```

Lista de mascotas disponibles, vista por USER:

```json
{
  "_embedded": {
    "mascotas": [
      {
        "_links": {
          "self": { "href": "http://localhost:8080/api/mascotas/1" },
          "mascotas-disponibles": { "href": "http://localhost:8080/api/mascotas" },
          "adoptar": { "href": "http://localhost:8080/api/adopciones" }
        },
        "id": 1,
        "nombre": "Firulais",
        "tipoMascota": { "id": 1, "nombre": "Perro" },
        "edad": 3,
        "disponible": true
      }
    ]
  },
  "_links": { "self": { "href": "http://localhost:8080/api/mascotas" } }
}
```

Adopción registrada:

```json
{
  "_links": {
    "self": { "href": "http://localhost:8080/api/adopciones/1" },
    "mascota": { "href": "http://localhost:8080/api/mascotas/1" },
    "usuario": { "href": "http://localhost:8080/api/usuarios/1" }
  },
  "id": 1,
  "fechaAdopcion": "2026-10-27",
  "mascota": { "id": 1, "nombre": "Firulais", "tipoMascota": { "id": 1, "nombre": "Perro" }, "edad": 3, "disponible": false },
  "usuario": { "id": 1, "nombre": "Ana Pérez", "correoElectronico": "ana.perez@mail.com", "telefono": "+54 11 5555-1234" }
}
```

Mascota adoptada, vista por ADMIN (enlaza su adopción; ya no ofrece `adoptar` ni `eliminar`):

```json
{
  "_links": {
    "self": { "href": "http://localhost:8080/api/mascotas/1" },
    "mascotas-disponibles": { "href": "http://localhost:8080/api/mascotas" },
    "adopcion": { "href": "http://localhost:8080/api/adopciones/1" },
    "actualizar": { "href": "http://localhost:8080/api/mascotas/1" }
  },
  "id": 1,
  "nombre": "Firulais",
  "tipoMascota": { "id": 1, "nombre": "Perro" },
  "edad": 3,
  "disponible": false
}
```

### 10.3 Colecciones vacías

Siguiendo HAL, una lista vacía omite `_embedded` y conserva el enlace a sí misma. El cliente debe interpretar la ausencia de `_embedded` como lista vacía:

```json
{
  "_links": { "self": { "href": "http://localhost:8080/api/mascotas" } }
}
```

## 11. Transacciones, rollback y concurrencia

- **Adopción atómica:** `AdopcionService.adoptar` registra la adopción y marca la mascota como no disponible en un único método `@Transactional`; ante cualquier fallo se revierten ambos cambios.
- **Bloqueo pesimista:** la mascota se obtiene con `findByIdForUpdate` (`@Lock(PESSIMISTIC_WRITE)`, `SELECT … FOR UPDATE`). Una segunda solicitud concurrente espera el bloqueo y, al obtenerlo, encuentra `disponible = false`: responde `409`. Actualizar y eliminar mascotas toman el mismo bloqueo, por lo que un `PUT` concurrente no puede reactivar una mascota recién adoptada.
- **Segunda barrera en la base:** la restricción `UNIQUE (mascota_id)`. Se usa `saveAndFlush` para que una violación ocurra dentro de la transacción y se traduzca a `409`.
- **Espera acotada en PostgreSQL:** `lock_timeout` configurable (`DB_LOCK_TIMEOUT`, 5 s por defecto); si se supera, la API responde `409` sin cambios.
- **Fecha del servidor:** se obtiene de un `Clock` inyectado, lo que permite fijarla en las pruebas.

Verificación automatizada:

| Comportamiento | Prueba (H2) | Prueba (PostgreSQL) |
|---|---|---|
| Rollback ante un fallo posterior al INSERT | `AdopcionRollbackIT` (spy sobre `AdopcionMapper`) | — |
| Rollback ante violación de la restricción UNIQUE | `AdopcionIT` | `AdopcionPostgresIT` |
| Dos adopciones simultáneas: exactamente un 201 y un 409 (5 repeticiones) | `AdopcionConcurrenciaIT` | `ConcurrenciaPostgresIT` |
| Una adopción espera mientras otra transacción bloquea la mascota | `AdopcionConcurrenciaIT` | `ConcurrenciaPostgresIT` |
| Bloqueo que supera `lock_timeout` → 409 sin cambios | — | `ConcurrenciaPostgresIT` |

Las pruebas de concurrencia usan el servidor HTTP real, sincronización explícita con `CountDownLatch`, esperas con límite de tiempo y comprueban el estado final directamente en la base de datos.

## 12. Persistencia: H2, PostgreSQL y Flyway

| Perfil | Uso | Base | Esquema |
|---|---|---|---|
| `dev` | Desarrollo local (por defecto) | H2 en memoria `jdbc:h2:mem:adopciondb` | Hibernate `create-drop` + `data.sql` |
| `test` | Pruebas automatizadas | H2 en memoria, nombre aleatorio por contexto | Hibernate `create-drop` + `data.sql` |
| `postgres` | Base persistente | PostgreSQL (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) | Migraciones Flyway + `ddl-auto=validate` |

Perfil `postgres`:

- Schema propio `adopcion`, configurado de forma consistente en JPA (`hibernate.default_schema`) y Flyway (`default-schema`, `schemas`).
- Migraciones versionadas: `V1__crear_esquema.sql` (tablas, restricciones con nombre, índices), `V2__tipos_mascota_iniciales.sql` (`ON CONFLICT DO NOTHING`), `V3__restringir_acceso_externo.sql` (revoca el acceso de los roles `anon`/`authenticated` si existen y activa RLS sin políticas).
- `ddl-auto=validate`: Hibernate no crea ni modifica tablas; una diferencia entre entidades y migraciones impide el arranque.
- `data.sql` no se ejecuta (`spring.sql.init.mode=never`).
- Variables opcionales: `DB_POOL_SIZE`, `DB_LOCK_TIMEOUT` y `SPRING_FLYWAY_URL`/`USER`/`PASSWORD` para migrar por otra conexión.

**Supabase:** la configuración está preparada para el pooler compartido en modo *session*, con `sslmode=verify-full` y el certificado CA montado como archivo de solo lectura. **No se probó contra un proyecto real de Supabase; la conexión y el SSL con Supabase no están verificados.**

## 13. Docker

- `Dockerfile` multietapa: compilación con Maven y JDK 21 (las pruebas se ejecutan por separado, no durante la construcción de la imagen); ejecución con JRE 21, usuario sin privilegios (`adopcion`, uid 10001), `EXPOSE 8080` y `ENTRYPOINT` en formato exec.
- La imagen no contiene credenciales ni archivos `.env`; `.dockerignore` excluye `.git`, `target`, archivos `.env*`, certificados y claves.
- El perfil se selecciona con `SPRING_PROFILES_ACTIVE` y la configuración se pasa con `--env-file` (archivos locales ignorados por Git).
- Dentro del contenedor, `localhost` es el propio contenedor: para una base en el equipo con Docker Desktop se usa `host.docker.internal`.

## 14. Documentación y monitoreo

- **OpenAPI 3** en `/v3/api-docs` y **Swagger UI** en `/swagger-ui.html`, con esquema de seguridad HTTP Basic aplicado a todos los endpoints de negocio, rol requerido por operación, esquemas HAL (`EntityModel…`, `CollectionModel…`) y respuestas de error con `ErrorResponse`.
- **Actuator:** solo `health` (`{"status":"UP"}`, sin detalles) e `info` (nombre, descripción y versión) expuestos por HTTP.
- **README** con instalación, ejecución, perfiles, endpoints, ejemplos HAL y comandos para PowerShell y Git Bash.

## 15. Pruebas y resultados

### 15.1 Organización

| Tipo | Clases | Pruebas |
|---|---|---|
| Unitarias (JUnit + Mockito, sin Spring) | `MascotaServiceTest` (12), `UsuarioServiceTest` (12), `AdopcionServiceTest` (7), `UsuarioRequestTest` (6) | 37 |
| Controllers (`@WebMvcTest`, seguridad real, servicios simulados) | `MascotaControllerTest` (31), `UsuarioControllerTest` (29), `AdopcionControllerTest` (13) | 73 |
| Integración con H2 (`*IT`) | `AdopcionIT` (7), `AdopcionRollbackIT` (1), `AdopcionConcurrenciaIT` (6), `MascotaIT` (5), `UsuarioIT` (10), `PersistenciaIT` (7), `HateoasIT` (6), `DocumentacionYMonitoreoIT` (31) | 73 |
| Integración con PostgreSQL 17 en Testcontainers (`*PostgresIT`) | `EsquemaPostgresIT` (6), `RestriccionesPostgresIT` (5), `AdopcionPostgresIT` (3), `ConcurrenciaPostgresIT` (7) | 21 |

Las pruebas de integración con H2 usan servicios y repositorios reales; `AdopcionRollbackIT` agrega un spy (`@MockitoSpyBean` sobre `AdopcionMapper`) para provocar un fallo controlado dentro de la transacción. Las de PostgreSQL usan el perfil `postgres` real, sin mocks ni spies.

### 15.2 Resultados

Ejecutados sobre el código del commit `cafff03`:

| Comando | Surefire | Failsafe | Total | Fallos / errores / omitidas |
|---|---|---|---|---|
| `mvnw clean verify` (H2) | 110 | 73 | **183** | 0 / 0 / 0 |
| `mvnw clean verify -Ppostgres` | 110 | 21 | **131** | 0 / 0 / 0 |
| **Pruebas distintas** | | | **204** | |

Las 110 pruebas de Surefire son las mismas en ambos comandos; por eso el total de pruebas distintas es 110 + 73 + 21 = 204.

### 15.3 Alcance de lo verificado

| Aspecto | Verificado con | No verificado |
|---|---|---|
| HAL, enlaces por estado y rol, navegación siguiendo los enlaces, colecciones vacías | Pruebas de controller y de integración con H2 (`HateoasIT`); demostración manual con la imagen Docker y el perfil `dev` (H2) | Navegación HAL específica contra PostgreSQL. La suite de PostgreSQL comprueba el formato `_embedded` de la lista de mascotas, pero no recorre los enlaces. |
| Reglas de negocio, restricciones, rollback y concurrencia en PostgreSQL | Suite `-Ppostgres` (PostgreSQL 17 en Testcontainers) | — |
| Perfil `postgres` con variables de entorno | Ejecución del JAR contra un PostgreSQL local temporal: migraciones V1–V3, persistencia tras reiniciar y conexión separada para Flyway | — |
| API contenerizada con PostgreSQL | Contenedor de la API contra un PostgreSQL temporal mediante `host.docker.internal`: migraciones aplicadas, flujo de adopción y datos conservados al recrear el contenedor. Realizado con la versión previa a HATEOAS (commit `a6aa759`). | Repetición con la versión HAL actual |
| Supabase y SSL con certificado real | — | **No probado** |
| Ejecución desde IntelliJ IDEA | — | Pendiente |
| Uso manual de Swagger UI en un navegador (*Authorize*, *Try it out*) | Swagger UI y `/v3/api-docs` comprobados por HTTP y con pruebas automatizadas | Pendiente |

## 16. Comandos reproducibles

Requisitos: JDK 21; Docker Desktop solo para el contenedor y las pruebas de PostgreSQL. En Windows, si `mvnw.cmd` no encuentra PowerShell, agregar temporalmente las carpetas del sistema al `PATH` de la sesión (ver README).

| Acción | PowerShell | Git Bash |
|---|---|---|
| Pruebas unitarias y de controller | `.\mvnw.cmd clean test` | `./mvnw clean test` |
| Todas las pruebas con H2 | `.\mvnw.cmd clean verify` | `./mvnw clean verify` |
| Pruebas con PostgreSQL (Docker) | `.\mvnw.cmd clean verify -Ppostgres` | `./mvnw clean verify -Ppostgres` |
| Ejecutar la API (H2, perfil `dev`) | `.\mvnw.cmd spring-boot:run` | `./mvnw spring-boot:run` |
| Construir la imagen | `docker build -t api-rest-adopcion .` | `docker build -t api-rest-adopcion .` |
| Archivo de configuración H2 | `Set-Content -Path .env.h2 -Value "SPRING_PROFILES_ACTIVE=dev" -Encoding ascii` | `printf 'SPRING_PROFILES_ACTIVE=dev\n' > .env.h2` |
| Levantar el contenedor | `docker run -d --name adopcion-api --env-file .env.h2 -p 127.0.0.1:8080:8080 api-rest-adopcion` | igual |
| Estado | `curl.exe http://localhost:8080/actuator/health` | `curl http://localhost:8080/actuator/health` |
| Consulta autenticada | `curl.exe -u user:user123 http://localhost:8080/api/mascotas` | `curl -u user:user123 http://localhost:8080/api/mascotas` |
| Logs / detener / eliminar | `docker logs -f adopcion-api` · `docker stop adopcion-api` · `docker rm adopcion-api` | igual |

Credenciales de demostración (solo perfil `dev`, uso local): `user`/`user123` (USER) y `admin`/`admin123` (ADMIN). Swagger UI: `http://localhost:8080/swagger-ui.html`.

## 17. Decisiones de diseño

| Decisión | Motivo |
|---|---|
| DTOs como `record` y mappers manuales | Contratos inmutables y conversión explícita, sin generadores de código |
| Bloqueo pesimista + restricción UNIQUE | El conflicto de adopción es real y debe responder con un mensaje claro; la restricción protege aunque se omita el servicio |
| `open-in-view=false` | Toda la conversión a DTO ocurre dentro de la transacción del servicio |
| Cuentas de acceso separadas de los adoptantes | Los adoptantes son datos del dominio; quien opera la API se autentica con otra identidad |
| Flyway + `validate` solo en PostgreSQL | Esquema versionado y controlado en la base persistente; H2 se mantiene simple para desarrollo y pruebas |
| Schema `adopcion` y V3 restrictiva | Evita exponer datos por servicios externos que publican el schema `public` |
| HAL estándar con enlaces condicionados | El cliente descubre las acciones válidas; el servidor sigue siendo la autoridad |
| Errores siempre `application/json` | Un único formato de error, independiente del formato HAL de las respuestas exitosas |

## 18. Limitaciones y trabajo futuro

- Verificar la conexión con Supabase (pooler en modo *session*, SSL `verify-full`).
- Reemplazar HTTP Basic por tokens (JWT u OAuth2) si la API se consume desde un navegador.
- Paginación en las listas.
- HAL-FORMS para describir método y campos de cada acción.
