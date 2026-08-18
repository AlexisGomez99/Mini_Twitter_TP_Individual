# Mini_Twitter_TP_Individual

Backend de un mini clon de Twitter (usuarios, tweets, retweets y follows) hecho con Spring Boot.

## Tecnologías utilizadas

### Lenguaje y build

- **Java 24** — lenguaje en el que está escrito todo el backend.
- **Maven** (con Maven Wrapper `mvnw`/`mvnw.cmd`) — gestiona las dependencias y el ciclo de build (compilar, testear, empaquetar, correr la app), sin necesitar tener Maven instalado globalmente.

### Framework

- **Spring Boot** — framework principal de la aplicación. Levanta el servidor web, arma el contenedor de dependencias (inyección de dependencias) y expone la app como un ejecutable standalone.
- **Spring Web (MVC)** (`spring-boot-starter-webmvc`) — permite crear controladores REST (endpoints HTTP) y levanta el servidor embebido (Tomcat).
- **Spring Data JPA** (`spring-boot-starter-data-jpa`) — capa de acceso a datos. Traduce las entidades Java (`User`, `Tweet`, `Follow`) a tablas SQL y da repositorios listos para usar sin escribir SQL a mano.
- **Spring Boot Validation** (`spring-boot-starter-validation`) — permite validar datos de entrada (por ejemplo, campos obligatorios o con formato específico) usando anotaciones.

### Persistencia / Base de datos

- **Hibernate** — implementación de JPA que usa Spring Data JPA por debajo. Es el ORM que convierte objetos Java en filas de la base de datos y genera las sentencias SQL (`create table`, `insert`, etc.).
- **PostgreSQL** — base de datos relacional real que usa la app en desarrollo (contenedor Docker), donde se guardan usuarios, tweets y follows de forma persistente.
- **H2** (`com.h2database`) — base de datos en memoria, liviana y descartable. Útil como alternativa rápida sin depender de Docker/Postgres levantado.
- **pgAdmin** — interfaz web para administrar PostgreSQL visualmente (ver tablas, correr queries, etc.) sin usar la consola.

### Infraestructura

- **Docker / Docker Compose** (`docker-compose.yml`) — levanta PostgreSQL y pgAdmin en contenedores aislados, con un solo comando (`docker compose up -d`), sin instalar nada de eso directamente en Windows.

### Utilidades de código

- **Lombok** — genera automáticamente código repetitivo (getters, setters, constructores) a partir de anotaciones (`@Getter`, `@Setter`, `@NoArgsConstructor`), evitando escribirlo a mano en las entidades.

### Testing

- **JUnit 5** — framework para escribir y correr los tests unitarios (`UserTest`, `TweetTest`, `FollowTest`, etc.).
- **AssertJ** — librería de aserciones fluida (`assertThat(...)`) usada en los tests para verificar resultados y excepciones de forma legible.
- **Mockito** — librería para crear mocks (objetos simulados), incluida por los starters de test de Spring Boot para tests que necesiten aislar dependencias.
- **JaCoCo** — mide el porcentaje de cobertura de código de los tests (líneas ejecutadas vs. no ejecutadas) y falla el build si baja de un umbral mínimo configurado (90%).

## Estructura del proyecto

```
mini-twitter/
├── backend/          # API en Spring Boot (Java 24 + PostgreSQL)
│   ├── src/main/     # Código fuente (modelo, excepciones, configuración)
│   ├── src/test/     # Tests unitarios (JUnit 5 + AssertJ)
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
3. Correr los tests (con reporte de cobertura JaCoCo):
   ```bash
   ./mvnw.cmd clean test
   ```

**Nota:** el proyecto requiere JDK 24. Si tu `JAVA_HOME`/`PATH` apunta a otra versión, seteala antes de correr los comandos de Maven.
