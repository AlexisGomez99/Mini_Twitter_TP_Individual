# Mini_Twitter_TP_Individual

Backend de un mini clon de Twitter (usuarios, tweets, retweets y follows) hecho con Spring Boot.

## Tecnologías utilizadas

### Lenguaje y build

- **Java 24** — lenguaje en el que está escrito todo el backend.
- **Maven** (con Maven Wrapper `mvnw`/`mvnw.cmd`) — gestiona las dependencias y el ciclo de build (compilar, testear, empaquetar, correr la app), sin necesitar tener Maven instalado globalmente. El `pom.xml` no usa `spring-boot-starter-parent`: las versiones de Spring Boot se importan como BOM (`dependencyManagement`), para poder fijar a mano versiones puntuales como la de Hibernate o JUnit.

### Framework

- **Spring Boot** — framework principal de la aplicación. Levanta el servidor web y arma el contenedor de dependencias (inyección de dependencias).
- **Spring Web (MVC)** (`spring-boot-starter-webmvc`) — expone la API REST (`@RestController`) y levanta el servidor embebido (Tomcat).
- **Spring Boot Validation** (`spring-boot-starter-validation`) — permite validar datos de entrada usando anotaciones.

> **Sin Spring Data JPA:** a propósito, no se usa `spring-boot-starter-data-jpa` ni autoconfiguración de JPA. El `EntityManagerFactory` se arma a mano (`EmfBuilder`, con `jakarta.persistence.PersistenceConfiguration`) y los repositorios trabajan directo contra `EntityManager`, para entender JPA/Hibernate sin la magia de Spring Data.

### Persistencia / Base de datos

- **Hibernate** — implementación de JPA. Es el ORM que convierte objetos Java en filas de la base de datos y genera las sentencias SQL (`create table`, `insert`, etc.).
- **PostgreSQL** — base de datos relacional real que usa la app en desarrollo (contenedor Docker), donde se guardan usuarios, tweets y follows de forma persistente.
- **H2** (`com.h2database`) — base de datos en memoria, usada por los tests (se recrea el esquema en cada corrida).
- **pgAdmin** — interfaz web para administrar PostgreSQL visualmente (ver tablas, correr queries, etc.) sin usar la consola.

### Infraestructura

- **Docker / Docker Compose** (`docker-compose.yml`) — levanta PostgreSQL y pgAdmin en contenedores aislados, con un solo comando (`docker compose up -d`), sin instalar nada de eso directamente en Windows.

### Utilidades de código

- **Lombok** — genera automáticamente código repetitivo (getters, setters, constructores) a partir de anotaciones (`@Getter`, `@Setter`, `@NoArgsConstructor`), evitando escribirlo a mano en las entidades.

### Testing

- **JUnit 5** — framework para escribir y correr los tests unitarios e de integración.
- **AssertJ** — librería de aserciones fluida (`assertThat(...)`) usada en los tests para verificar resultados y excepciones de forma legible.
- **MockMvc** (`spring-boot-starter-webmvc-test`) — usado para testear los controllers REST de punta a punta (request HTTP → respuesta JSON) sin levantar un servidor real.
- **JaCoCo** — mide el porcentaje de cobertura de código de los tests (líneas ejecutadas vs. no ejecutadas) y falla el build si baja de un umbral mínimo configurado (90%).

## Arquitectura

El backend está organizado en capas, sin usar Spring Data JPA:

```
web/            → Controllers REST, DTOs de request y manejo global de excepciones
service/        → TwitterService: una única fachada para toda la lógica de la app
repositorios/   → Interfaz + implementación Jpa*Repository (EntityManager puro) por entidad
model/          → Entidades JPA (User, Tweet/OriginalTweet/Retweet, Follow) y sus reglas de negocio
dto/            → Records de solo lectura (*Info) que devuelve el servicio, sin exponer las entidades
```

### Modelo de datos

- `Tweet` es una jerarquía (`SINGLE_TABLE`): `OriginalTweet` (con contenido propio) y `Retweet` (que apunta a otro `Tweet` como origen).
- **Los borrados son soft delete, no `DELETE` físico.** Tanto `User` como `Tweet` tienen un flag `deleted`: al "borrar" una cuenta o un tweet, la fila se conserva marcada como eliminada en vez de removerse, y el texto del tweet también se conserva en la base (solo se oculta al mostrarlo). Esto evita que un retweet de otro usuario quede con una referencia rota: si el tweet o la cuenta original desaparecen, el retweet se sigue mostrando, pero con el contenido reemplazado por `"Publicación no disponible"` y/o el autor por `"Cuenta eliminada"`.
- Al borrar un usuario, sus propios tweets se ocultan en cascada (a nivel de modelo Java) y sus relaciones de "follow" (en ambos sentidos) sí se eliminan de verdad, ya que no tienen valor sin una cuenta activa.

### Búsquedas por id, no por username

Salvo un caso puntual, toda la API trabaja con **ids** (`Integer`) en vez de usernames, porque el username puede cambiar con el tiempo y el id no:

- `GET /users/{id}`, `DELETE /users/{id}`, `GET /tweets?userId=`, `POST /follows` con `followerId`/`followedId`, etc.
- La única excepción es `GET /users?username=`, pensada para el caso puntual en que todavía no se conoce el id de la persona.

### Autenticación

- `POST /login` valida usuario y contraseña y responde con una cookie `token` (JWT, `HttpOnly`, 3 minutos). El cuerpo de la respuesta va vacío.
- `POST /logout` expira la cookie `token` (`Max-Age=0`). Lo hace el backend porque la cookie es `HttpOnly`. No exige estar logueado. El JWT no se revoca del lado del servidor (es stateless): solo se borra de la cookie del navegador.
- Todos los endpoints requieren esa cookie, **salvo** `POST /login`, `POST /logout`, `POST /registrar` y `POST /users`.
- Sin cookie, con un token inválido o con credenciales incorrectas, la API responde **401** con `{ "message": "..." }`. El resto de los errores de negocio siguen siendo **400**.
- Como la cookie es `HttpOnly` el frontend no puede leerla; para saber quién está logueado consulta `GET /me`.
- **CORS:** el backend acepta pedidos con credenciales desde `http://localhost:5173` (el frontend con Vite). El origen se cambia en `WebConfig`.

### Paginación

`GET /tweets/feed` (20 por página) y `GET /tweets?userId=` (10 por página) devuelven una página en vez de una lista. El tamaño es fijo (constantes en `TwitterService`); el cliente solo elige `page` (por defecto 0). Una `page` negativa o que no sea un número responde **400**; una página fuera de rango devuelve `content` vacío.

```json
{ "content": [ ... ], "page": 0, "totalPages": 3, "totalElements": 57, "hasNext": true }
```

Se usa `LIMIT/OFFSET` (la página N saltea N × tamaño filas) más un `count` para el total, ordenando por fecha y desempatando por id para que el orden sea estable.

## Endpoints principales

| Método | Ruta                       | Descripción                                   |
|--------|----------------------------|------------------------------------------------|
| POST   | `/login`                   | Inicia sesión (`{ username, password }`) y devuelve la cookie `token` |
| POST   | `/logout`                  | Cierra sesión: expira la cookie `token`        |
| POST   | `/registrar`               | Registra un usuario nuevo (`{ username, password }`). Es pública: no requiere sesión. Responde 400 si el username ya existe, no tiene entre 5 y 25 caracteres o la contraseña está vacía |
| GET    | `/me`                      | Usuario logueado según la cookie `token`       |
| POST   | `/users`                   | Crea un usuario                                |
| GET    | `/users`                   | Lista todos los usuarios                       |
| GET    | `/users?username=`         | Busca un usuario por username (caso puntual)   |
| GET    | `/users/{id}`              | Busca un usuario por id                        |
| DELETE | `/users/{id}`              | Borra (soft delete) un usuario                 |
| POST   | `/tweets`                  | Crea un tweet (`{ userId, content }`)          |
| GET    | `/tweets?userId=&page=`    | Tweets propios de un usuario, **paginados de a 10** (`page` empieza en 0) |
| GET    | `/tweets/feed?page=`       | Feed de inicio: tweets y retweets propios y de los usuarios que sigue quien está logueado, del más nuevo al más viejo, **paginado de a 20** (`page` empieza en 0) |
| DELETE | `/tweets/{id}`             | Borra (soft delete) un tweet propio; si es de otra persona responde 403 |
| POST   | `/retweets`                | Crea un retweet (`{ userId, originTweetId }`)  |
| GET    | `/retweets?userId=`        | Retweets de un usuario                         |
| DELETE | `/retweets/{id}`           | Borra (soft delete) un retweet                 |
| POST   | `/follows`                 | Sigue a un usuario (`{ followerId, followedId }`) |
| DELETE | `/follows?followerId=&followedId=` | Deja de seguir a un usuario            |
| GET    | `/follows/is-following?followerId=&followedId=` | Consulta si sigue a otro usuario |
| GET    | `/follows/followers?userId=` | Seguidores de un usuario                     |
| GET    | `/follows/following?userId=`  | A quiénes sigue un usuario                   |

## Estructura del proyecto

```
mini-twitter/
├── backend/          # API en Spring Boot (Java 24 + PostgreSQL)
│   ├── src/main/     # Código fuente (web, service, repositorios, model, dto, config)
│   ├── src/test/     # Tests unitarios e de integración (JUnit 5 + AssertJ + MockMvc)
│   └── docker-compose.yml   # PostgreSQL + pgAdmin
└── frontend/         # (pendiente)
```

## Cómo correr el proyecto

1. Levantar la base de datos:
   ```bash
   cd backend
   docker compose up -d
   ```
2. Correr la app:
   ```bash
   ./mvnw.cmd spring-boot:run
   ```
   La API queda disponible en `http://localhost:8080`.
3. Correr los tests (con reporte de cobertura JaCoCo, usan H2 en memoria):
   ```bash
   ./mvnw.cmd clean test
   ```

**Nota:** el proyecto requiere JDK 24. Si tu `JAVA_HOME`/`PATH` apunta a otra versión, seteala antes de correr los comandos de Maven.
