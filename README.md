# API-REST-AdopcionCodigoFacilito

API REST de adopción de mascotas con Spring Boot 4.1.2, JPA y H2 (Java 21, Maven).

## Abrir en IntelliJ IDEA
1. Descomprimí el .zip.
2. **File → Open** → elegí la carpeta `API-REST-AdopcionCodigoFacilito` (o el `pom.xml`) → *Open as Project*.
3. Verificá que el SDK del proyecto sea Java 21 (File → Project Structure → SDK).
4. Esperá a que Maven descargue las dependencias.
5. Ejecutá `AdopcionApplication` (botón ▶ junto al `main`).

La API queda en `http://localhost:8080`. Consola H2: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:adopciondb`, usuario `sa`).

## Endpoints
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/mascotas` | Listar (filtros: `?especie=perro`, `?disponibles=true`) |
| GET | `/api/mascotas/{id}` | Obtener una |
| POST | `/api/mascotas` | Crear |
| PUT | `/api/mascotas/{id}` | Actualizar |
| DELETE | `/api/mascotas/{id}` | Eliminar |
| POST | `/api/mascotas/{id}/adoptar` | Adoptar (`{"adoptante":"Juan"}`) |

Ejemplo:
```bash
curl -X POST localhost:8080/api/mascotas -H "Content-Type: application/json" \
  -d '{"nombre":"Luna","especie":"Gato","raza":"Persa","edad":1}'
```
