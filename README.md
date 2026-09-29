# Template de API por capas

API de ejemplo en Spring Boot 4.1, Java 17, Maven, PostgreSQL, Flyway y JWT. Sirve para copiar la estructura cuando arranques un servicio nuevo. La arquitectura es por capas: cada tipo de clase vive en un solo paquete, y un recurso nuevo se agrega ahí, no en un paquete propio.

## Reglas

- `controller` habla HTTP. Depende de la interfaz del servicio (`IUserService`, `IAuthService`) y solo ve DTOs.
- `service/impl` aplica la regla de negocio, abre la transacción y devuelve DTOs. La clase implementa la interfaz de `service/interfaces`.
- `repository` y `entity` son la persistencia. La entidad no sale del servicio ni del repositorio.
- `dto/request` y `dto/response` son el contrato HTTP. `mapper` hace la traducción con MapStruct. Alta y actualización usan `UserCreateRequest` y `UserUpdateRequest`.
- `exception` concentra las excepciones de negocio y el manejador.
- `shared` guarda seguridad (`JwtAuthFilter`, `JwtUtil`), configuración (CORS, OpenAPI) y constantes.
- Un caso de negocio nuevo agrega un `code` (`USER_ALREADY_EXISTS`). No agrega una clase de excepción por caso.
- La autorización de rutas vive en `SecurityConfig`. Una operación solo de admin tiene que declararse ahí.
- El esquema lo cambia Flyway. Hibernate solo valida que las entidades coincidan.

```text
com.company.template
  TemplateApplication.java
  controller
  service
    interfaces
    impl
  repository
  entity
  dto
    request
    response
  mapper
  exception
  shared
    config
    security
    constants
```

## Al copiarlo para un proyecto

Elige esta API o la hexagonal, no las dos, para el mismo sistema. El ejemplo usa un gimnasio. Copia la carpeta y deja este template como está.

| Qué | Ejemplo |
|---|---|
| Carpeta y artefacto | `gym-system-api` |
| Base | `gym-db` |
| Cliente | `gym-system-client`, con `API_URL=http://localhost:8080/api/v1` |

Archivos:

| Archivo | Qué cambiar |
|---|---|
| Nombre de la carpeta | `spring-layered-template` → `gym-system-api` |
| `pom.xml` | `artifactId` y `name`: `gym-system-api`. `groupId` si quieres otro, por ejemplo `com.gym` |
| Paquete `com.company.template` | Opcional. Si lo cambias, mueve la carpeta `src/main/java/com/company/template` y la de test, y reemplaza el paquete en los `.java` |
| `docker-compose.yml` | `POSTGRES_DB` y el healthcheck: `gym-db`. `container_name`, por ejemplo `gym-system-api-db` |
| `src/main/resources/application-local.properties` | `spring.datasource.url` a `jdbc:postgresql://localhost:5433/gym-db` |

Si el contenedor ya corrió con `template_db`, el volumen conserva ese nombre. Cambia también el volumen en `docker-compose.yml` (el nombre `pgdata`) o borra el volumen viejo antes de `docker compose up -d`.

El puerto `5433` es el de esta plantilla. La hexagonal usa `5434` para poder convivir con esta en la máquina de desarrollo. En el proyecto copiado puedes dejar `5433`.

## Errores

Todas las respuestas de error usan la misma forma:

```json
{
  "title": "Conflict",
  "status": 409,
  "detail": "A user with this name already exists",
  "code": "USER_ALREADY_EXISTS"
}
```

| Situación | HTTP | Clase | Ejemplo de code |
|---|---|---|---|
| Body mal formado | 400 | Bean Validation | `VALIDATION_ERROR` |
| Credencial o token inválido | 401 | `UnauthorizedException` o el filtro JWT | `INVALID_CREDENTIALS`, `UNAUTHORIZED`, `INVALID_TOKEN` |
| Autenticado sin rol | 403 | Spring Security | `FORBIDDEN` |
| No existe | 404 | `NotFoundException` | `USER_NOT_FOUND` |
| Choca con datos existentes | 409 | `ConflictException` | `USER_ALREADY_EXISTS` |
| Regla de estado | 422 | `BusinessRuleException` | `USER_INACTIVE` |

Si dos requests pasan el `if` al mismo tiempo, la constraint de Postgres responde el mismo 409.

## Seguridad

La cuenta de acceso (`username`, hash, roles) es distinta del `User` de negocio (nombre, email, teléfono).

- `POST /api/v1/auth/login` con `username` y `password` devuelve un access token de 15 minutos y un refresh token de 7 días.
- `POST /api/v1/auth/refresh` rota el refresh token. El anterior deja de servir.
- `POST /api/v1/auth/logout` con `{ "refreshToken" }` revoca ese refresh token y responde 204. Si el token ya no sirve, también responde 204.
- El resto de la API exige `Authorization: Bearer …`.
- `GET /users?page=0&size=10` responde `content`, `totalElements`, `totalPages`, `number` y `size`.
- `DELETE /users/{id}`, activar y desactivar exigen rol `ADMIN`.

No hay sesión ni CSRF: el cliente manda el token en cada request.

## Configuración previa

Hace falta esto instalado:

- Java 17
- Maven
- Docker, con el daemon en marcha

No hay que crear la base ni las tablas a mano. `docker-compose.yml` levanta Postgres y, al arrancar la API, Flyway aplica `src/main/resources/db/migration`. Hibernate no crea esquema: `ddl-auto=validate` solo comprueba que las entidades coincidan con lo que migró Flyway.

El perfil por defecto es `local` (`spring.profiles.default=local`). Ese perfil trae valores de desarrollo en `application-local.properties`. Coinciden con el contenedor:

| | Valor local |
|---|---|
| Host | `localhost` |
| Puerto | `5433` (el contenedor escucha 5432 por dentro; 5433 evita chocar con otro Postgres) |
| Base | `template_db` |
| Usuario | `admin` |
| Contraseña | `12345` |
| JDBC | `jdbc:postgresql://localhost:5433/template_db` |

Esas claves, el secreto JWT `local-dev-only-secret-key-min-32b` y las cuentas de abajo son solo para la máquina local. Se pueden pisar con variables de entorno sin editar los properties.

| Variable | Para qué sirve | Default en `local` |
|---|---|---|
| `DB_URL` | JDBC | `jdbc:postgresql://localhost:5433/template_db` |
| `DB_USERNAME` | Usuario de Postgres | `admin` |
| `DB_PASSWORD` | Contraseña de Postgres | `12345` |
| `JWT_SECRET` | Firma HMAC del token. Mínimo 32 bytes | `local-dev-only-secret-key-min-32b` |
| `APP_ADMIN_USERNAME` / `APP_ADMIN_PASSWORD` | Cuenta admin del seed | `admin` / `admin` |
| `APP_USER_USERNAME` / `APP_USER_PASSWORD` | Cuenta user del seed | `user` / `user` |

`application.properties` fija lo que no cambia entre perfiles: context path `/api/v1`, Flyway activo, `open-in-view=false` y la duración de los tokens (access 15 minutos, refresh 7 días).

En `prod` (`application-prod.properties`) no hay defaults. `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET` tienen que existir en el entorno, y el seed de cuentas queda apagado.

## Cómo levantar el ambiente

Desde la raíz del template:

```bash
docker compose up -d
mvn spring-boot:run
```

`docker compose up -d` deja el contenedor `spring-layered-template-db` corriendo. La API escucha en `http://localhost:8080/api/v1`.

En IntelliJ: abre la carpeta como proyecto Maven, espera a que importe el `pom.xml` y ejecuta `TemplateApplication`. No hace falta elegir perfil: entra `local`. Docker tiene que estar arriba antes de esa ejecución.

Cuentas que se crean solo con el perfil `local`, la primera vez que arranca la API:

| Username | Password | Rol |
|---|---|---|
| `admin` | `admin` | `ADMIN` |
| `user` | `user` | `USER` |

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"admin\"}"
```

La documentación queda en `http://localhost:8080/api/v1/swagger-ui/index.html`. CORS permite cualquier origen porque el cliente manda el JWT en el header, no en una cookie.

Para producción:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Pruebas

```bash
mvn test
```

- Unitarias: servicio de usuarios, servicio de auth, mapper y JWT. No levantan Spring ni Postgres.
- De capa web: `ApiIT` llama los endpoints con MockMvc, incluyendo 401, 403 y el CRUD.
- De persistencia: `UserRepositoryTest` comprueba las constraints únicas contra Postgres.

Las dos últimas usan Testcontainers y se omiten si Docker no está disponible.

## Fuera de este template

Login social, verificación de email, MFA y servidor OAuth. La variante hexagonal está en `spring-hexagonal-template` y usa estas mismas reglas de error, migraciones y pruebas.
