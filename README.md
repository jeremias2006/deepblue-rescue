# DeepBlue Rescue

## Integrantes

- Jeremias Esteban Parra Florez
- Aluna Soffia Perea Labastidas

## Descripción

Capa de persistencia de una plataforma de rescate de fauna marina, construida con **Java 21**, **Spring Boot 4.1**, **Spring Data JPA**, **Hibernate**, **Flyway** y **PostgreSQL**, probada íntegramente con **Testcontainers**.

El sistema modela el ciclo de vida de un rescate de fauna marina: un centro de rescate registra un caso, se identifica y registra al animal, se le abre un expediente médico, y especialistas con distintas áreas de experiencia le aplican tratamientos mientras el caso avanza por distintos estados de rehabilitación.

Este proyecto cubre **exclusivamente la capa de persistencia**: no incluye controllers, API REST, capa de servicio, DTOs, seguridad ni frontend.

## Modelo de datos

### Entidades

| Entidad | Descripción |
|---|---|
| `RescueCenter` | Centro de rescate (código, nombre, ciudad) |
| `RescueCase` | Caso de rescate (código, fecha, ubicación, estado) |
| `Animal` | Animal rescatado (código, nombre común/científico, sexo, dispositivo de rastreo) |
| `MedicalRecord` | Expediente médico del animal (peso inicial, condición, heridas, observaciones) |
| `Specialist` | Especialista veterinario (código profesional, nombre, email, estado activo) |
| `Expertise` | Catálogo de áreas de experiencia (Trauma, Rehabilitación, etc.) |
| `Treatment` | Tratamiento aplicado a un animal por un especialista |

### Relaciones

| Relación | Tipo | Implementación |
|---|---|---|
| `RescueCenter` → `RescueCase` | 1:N | FK simple en `rescue_cases.rescue_center_id` |
| `RescueCase` ↔ `Animal` | 1:1 | FK + UNIQUE en `animals.rescue_case_id` (dueño: `Animal`) |
| `Animal` ↔ `MedicalRecord` | 1:1 | FK + UNIQUE en `medical_records.animal_id` (dueño: `MedicalRecord`) |
| `Specialist` ↔ `Expertise` | N:M | Tabla intermedia `specialist_expertise` con PK compuesta |
| `Animal` → `Treatment` | 1:N | FK simple en `treatments.animal_id` |
| `Specialist` → `Treatment` | 1:N | FK simple en `treatments.specialist_id` |

## Requisitos previos

- JDK 21
- Maven (o el wrapper incluido `mvnw` / `mvnw.cmd`)
- Docker Desktop corriendo (necesario para Testcontainers)

## Cómo ejecutar los tests

```bash
# Windows (PowerShell)
.\mvnw clean test

# Linux / macOS
./mvnw clean test
```

Al ejecutar los tests, Testcontainers levanta automáticamente un contenedor de **PostgreSQL 18 (alpine)**, Flyway aplica las migraciones, y se ejecuta toda la suite de pruebas contra una base de datos real. El contenedor se destruye automáticamente al finalizar (gestionado por Ryuk).

No es necesario tener PostgreSQL instalado en el sistema para correr los tests — solo Docker.

## Flyway

El esquema de la base de datos es gestionado exclusivamente por Flyway (`spring.jpa.hibernate.ddl-auto=validate`); Hibernate nunca crea ni modifica tablas, solo valida que las entidades coincidan con el esquema real.

| Migración | Contenido |
|---|---|
| `V1__create_schema.sql` | Creación de las 8 tablas, constraints (PK, FK, UNIQUE, CHECK) e índices |
| `V2__insert_expertise_catalog.sql` | Carga del catálogo inicial de expertise (Marine Reptiles, Marine Mammals, Marine Birds, Trauma, Rehabilitation, Toxicology) |
| `V3__add_tracking_device_to_animal.sql` | Evolución del esquema: columna opcional `tracking_device_code` en `animals`, con constraint UNIQUE |

Las migraciones ya aplicadas nunca se modifican; cualquier cambio posterior se agrega como una nueva migración versionada.

## Testcontainers

Todas las pruebas de integración corren contra un PostgreSQL real dentro de un contenedor Docker (no se usa H2 ni ninguna base en memoria), garantizando que el comportamiento probado sea fiel al de producción.

```java
@Container
@ServiceConnection
static final PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:18-alpine")
                .withDatabaseName("deepblue_test")
                .withUsername("deepblue")
                .withPassword("deepblue");
```

La anotación `@ServiceConnection` conecta automáticamente Spring Boot a ese contenedor, sin configuración manual de URLs.

## Repositories y consultas implementadas

### Métodos heredados (`JpaRepository`)
`save`, `saveAndFlush`, `findById`, `existsById`, `count`, `deleteById`, entre otros — disponibles en los 7 repositories sin código adicional.

### Query Methods

| Repository | Método | Descripción |
|---|---|---|
| `RescueCenterRepository` | `findByCode` | Busca un centro por su código |
| `RescueCaseRepository` | `findByCaseCode` | Busca un caso por su código |
| `RescueCaseRepository` | `findByStatusOrderByRescueDateAsc` | Casos filtrados por estado, ordenados por fecha |
| `RescueCaseRepository` | `findByRescueCenterCode` | Casos de un centro (navega `RescueCase → rescueCenter → code`) |
| `RescueCaseRepository` | `findByRescueDateAfterOrderByRescueDateDesc` | Casos posteriores a una fecha, más recientes primero |
| `AnimalRepository` | `findByAnimalCode` | Busca un animal por su código |
| `AnimalRepository` | `findByCommonNameContainingIgnoreCase` | Búsqueda parcial por nombre común |
| `AnimalRepository` | `findByRescueCaseStatus` | Animales filtrados por el estado de su caso |
| `AnimalRepository` | `findByRescueCaseRescueCenterCode` | Animales de un centro (navega 2 relaciones) |
| `ExpertiseRepository` | `findByNameIgnoreCase` | Busca una expertise por nombre |
| `TreatmentRepository` | `findByAnimalIdOrderByPerformedAtAsc` | Tratamientos de un animal en orden cronológico |

### Consultas JPQL (`@Query`)

| Repository | Método | Descripción |
|---|---|---|
| `SpecialistRepository` | `findActiveByExpertise` | Especialistas activos con cierta expertise (JOIN + parámetro nombrado) |
| `TreatmentRepository` | `findBetweenDates` | Tratamientos dentro de un rango de fechas |
| `TreatmentRepository` | `findByCenterCode` | Tratamientos de animales de un centro (navega 3 relaciones) |
| `TreatmentRepository` | `findByExpertise` | Tratamientos hechos por especialistas con cierta expertise (JOIN sobre relación N:M) |
| `AnimalRepository` | `findInRehabilitationTreatedByExpertise` | **Reto sin guía**: animales en rehabilitación tratados por un especialista con cierta expertise — combina filtro directo (`a.rescueCase.status`) con navegación de colecciones (`join a.treatments`, `join t.specialist`, `join s.expertiseAreas`) |

## Criterio de elección: Query Method vs. JPQL

- **Método heredado**: cuando la operación ya existe en `JpaRepository` (guardar, buscar por ID, contar, etc.).
- **Query Method**: cuando la consulta es una condición simple sobre uno o pocos atributos, incluso navegando relaciones directas (`@ManyToOne`, `@OneToOne`), y el nombre resultante sigue siendo legible.
- **`@Query` + JPQL**: cuando la consulta cruza varias relaciones combinadas, involucra colecciones (`@OneToMany`, `@ManyToMany`), requiere lógica de texto (`lower`, `like`), o el nombre del Query Method se volvería demasiado largo o difícil de leer.

## Suite de pruebas

`PersistenceIntegrationTest` contiene 15 pruebas de integración que cubren:

- Verificación de que Flyway ejecutó las 3 migraciones correctamente
- Operaciones CRUD heredadas
- Las relaciones 1:N, 1:1 (×2) y N:M
- Query Methods simples y con navegación de relaciones
- Consultas JPQL (por expertise, por rango de fechas, por relaciones múltiples)
- Constraints reales de PostgreSQL (violación de `UNIQUE` capturada como `DataIntegrityViolationException`)
- Un escenario integrador completo (centro → caso → animal → expediente → especialista → tratamientos) con las 8 consultas del negocio
- La consulta combinada del reto sin guía, validando tanto el caso positivo como los casos que deben quedar excluidos

## Estructura del proyecto

```
src/main/java/com/deepblue/rescue/
├── domain/          Entidades JPA y enums
├── repository/      Interfaces JpaRepository con Query Methods y JPQL
└── DeepblueRescueApplication.java

src/main/resources/
├── application.properties
└── db/migration/    Migraciones de Flyway (V1, V2, V3)

src/test/java/com/deepblue/rescue/
└── PersistenceIntegrationTest.java
```
