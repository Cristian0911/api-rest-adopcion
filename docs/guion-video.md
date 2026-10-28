# Guion del video de demostración

**Proyecto:** API-REST-AdopcionCodigoFacilito
**Duración objetivo:** 7 minutos (máximo 8)
**Presentador:** Cristian Gunther

## Preparación antes de grabar

1. Docker Desktop en ejecución.
2. Imagen construida: `docker build -t api-rest-adopcion .`
3. Contenedor recién iniciado con H2, para que los ids empiecen en 1:
   - Git Bash: `printf 'SPRING_PROFILES_ACTIVE=dev\n' > .env.h2`
   - PowerShell: `Set-Content -Path .env.h2 -Value "SPRING_PROFILES_ACTIVE=dev" -Encoding ascii`
   - `docker run -d --name adopcion-api --env-file .env.h2 -p 127.0.0.1:8080:8080 api-rest-adopcion`
4. Ejecutar antes de grabar, y dejar visibles los resúmenes, las dos suites de pruebas (tardan varios minutos):
   - `./mvnw clean verify` → resultado esperado: 110 + 73 pruebas, 0 fallos.
   - `./mvnw clean verify -Ppostgres` → resultado esperado: 110 + 21 pruebas, 0 fallos.
5. Abrir en pestañas: el repositorio (estructura de paquetes), `README.md`, `http://localhost:8080/swagger-ui.html`.
6. Terminal con fuente grande, en la carpeta raíz del proyecto. En PowerShell, usar `curl.exe` y `"@ejemplos/..."` entre comillas.
7. **Ensayar el uso de Swagger UI** (*Authorize* y *Try it out*): todavía no se verificó manualmente en un navegador. Hacer *Authorize* antes de *Try it out*; si el navegador muestra su propio diálogo de usuario y contraseña, cancelarlo.
8. Para repetir la demostración desde cero: `docker rm -f adopcion-api` y volver a levantar el contenedor.

Los comandos se muestran en sintaxis de Git Bash.

---

## Bloque 1 — Presentación (0:00 – 0:40)

**En pantalla:** título del proyecto y README.

> "Presentamos una API REST de adopción de mascotas construida con Java 21 y Spring Boot 4.1.2. Permite registrar mascotas y usuarios adoptantes, consultar las mascotas disponibles y registrar adopciones. Está protegida con HTTP Basic y dos roles, responde en formato HAL con hipervínculos, se documenta con Swagger y funciona con H2, con PostgreSQL mediante Flyway y dentro de un contenedor Docker."

## Bloque 2 — Arquitectura y modelo de datos (0:40 – 1:40)

**En pantalla:** árbol de paquetes en el IDE o en el repositorio; luego `entity/Adopcion.java`.

> "El proyecto está organizado en capas: los controllers solo manejan HTTP y delegan en los servicios; los servicios contienen las reglas de negocio y las transacciones; los repositories se limitan a persistencia. Usamos DTOs como records y no exponemos entidades JPA. Los ensambladores HATEOAS construyen los enlaces sin acceder a la base de datos."

> "El modelo tiene cuatro entidades: TipoMascota, Mascota, Usuario y Adopcion. Adopcion se relaciona con Mascota y con Usuario, y una restricción UNIQUE sobre la mascota garantiza en la base de datos que una mascota no pueda adoptarse dos veces."

## Bloque 3 — Ejecución y seguridad (1:40 – 2:30)

**En pantalla:** terminal.

```bash
docker ps --filter name=adopcion-api
curl http://localhost:8080/actuator/health
curl -i http://localhost:8080/api/mascotas
curl -i -u user:user123 -H "Content-Type: application/json" --data-binary @ejemplos/mascota.json http://localhost:8080/api/mascotas
```

> "La API corre en un contenedor con un usuario sin privilegios. El health responde UP sin detalles internos. Sin credenciales recibimos 401 con el formato de error de la API. Con la cuenta USER, registrar una mascota devuelve 403: esa operación requiere ADMIN."

## Bloque 4 — Flujo de adopción con HAL (2:30 – 4:30)

**En pantalla:** terminal; resaltar `_links` en cada respuesta.

```bash
curl -i -u user:user123 -H "Content-Type: application/json" --data-binary @ejemplos/usuario.json http://localhost:8080/api/usuarios
curl -i -u admin:admin123 -H "Content-Type: application/json" --data-binary @ejemplos/mascota.json http://localhost:8080/api/mascotas
curl -u user:user123 http://localhost:8080/api/mascotas
```

> "Registramos un usuario y, como ADMIN, una mascota. Ambas respuestas son 201 con el header Location y el tipo application/hal+json. La mascota trae enlaces: self, mascotas-disponibles, adoptar y, por ser ADMIN, actualizar y eliminar. En la lista vista por USER aparecen dentro de _embedded y sin las acciones de administrador."

```bash
curl -i -u user:user123 -H "Content-Type: application/json" --data-binary @ejemplos/adopcion.json http://localhost:8080/api/adopciones
curl -u user:user123 http://localhost:8080/api/mascotas
curl -i -u user:user123 -H "Content-Type: application/json" --data-binary @ejemplos/adopcion.json http://localhost:8080/api/adopciones
curl -u admin:admin123 http://localhost:8080/api/mascotas/1
```

> "Adoptamos la mascota usando el enlace adoptar. La adopción enlaza a la mascota y al usuario. Ahora la lista de disponibles está vacía: en HAL una colección vacía no incluye _embedded, solo el enlace self. Si intentamos adoptarla otra vez, el servidor responde 409. Y la mascota, vista por ADMIN, ahora enlaza su adopción y ya no ofrece adoptar ni eliminar."

> "Los enlaces orientan al cliente, pero no reemplazan la autorización: el servidor vuelve a verificar permisos y reglas en cada solicitud."

## Bloque 5 — Reglas de negocio y CRUD de Usuario (4:30 – 5:15)

```bash
curl -i -u admin:admin123 -X DELETE http://localhost:8080/api/usuarios/1
curl -i -u admin:admin123 -X PUT -H "Content-Type: application/json" --data-binary @ejemplos/mascota-reactivar.json http://localhost:8080/api/mascotas/1
curl -i -u user:user123 -H "Content-Type: application/json" --data-binary @ejemplos/usuario-invalido.json http://localhost:8080/api/usuarios
```

> "El CRUD de usuarios incluye crear, listar, consultar, actualizar y eliminar. No se puede eliminar un usuario con adopciones ni reactivar una mascota adoptada: ambos casos responden 409. Las validaciones responden 400 con el detalle por campo. Los correos se normalizan eliminando espacios en los extremos antes de validar y de buscar duplicados."

## Bloque 6 — Swagger UI (5:15 – 6:00)

**En pantalla:** `http://localhost:8080/swagger-ui.html`.

> "La documentación OpenAPI describe todos los endpoints, el rol requerido, los esquemas HAL y las respuestas de error. Con Authorize cargamos las credenciales y probamos una operación."

Acciones: *Authorize* → `user` / `user123` → abrir `GET /api/mascotas` → *Try it out* → *Execute*.

## Bloque 7 — Transacciones, PostgreSQL y pruebas (6:00 – 7:00)

**En pantalla:** resúmenes de las dos ejecuciones de Maven preparadas antes de grabar; luego `db/migration/postgresql`.

> "La adopción registra la adopción y cambia la disponibilidad en una sola transacción, con bloqueo pesimista sobre la mascota. Lo comprobamos con pruebas automatizadas: un fallo dentro de la transacción revierte ambos cambios, y dos solicitudes simultáneas producen exactamente un 201 y un 409."

> "Además de H2, la API funciona con PostgreSQL: Flyway crea el schema adopcion y Hibernate solo valida el esquema. Probamos el perfil con PostgreSQL 17 en contenedores. En total ejecutamos 183 pruebas con H2 y 131 con el perfil PostgreSQL: 204 pruebas distintas, todas aprobadas."

## Cierre (7:00 – 7:20)

> "En resumen: una API en capas, segura, transaccional, con hipervínculos HAL, documentada, probada y lista para ejecutarse en Docker con H2 o PostgreSQL. Gracias."

---

## Notas para la grabación

- No presentar como comprobadas la conexión con Supabase ni la ejecución desde IntelliJ: no forman parte de lo verificado.
- Si un comando falla durante la grabación, detener el contenedor, volver a levantarlo y repetir el bloque desde el inicio (los ids dependen de una base recién iniciada).
- Al terminar: `docker stop adopcion-api` y `docker rm adopcion-api`.
