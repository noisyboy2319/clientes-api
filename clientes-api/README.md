# API REST de Clientes — Spring Boot

**Asignatura:** IS388 Construcción y Evolución de Software
**Laboratorio Nº 04:** Gestión Ágil Avanzada (Scrum en la construcción de software)
**Escuela Profesional:** Ingeniería de Sistemas (plan 2018 R) — UNSCH
**Docente:** Mtro. Ing. Oscar Meneses Yaranga
**Semestre académico:** 2026-II
**Autor:** _[Apellidos y nombres del estudiante]_

---

## Tabla de contenidos

1. [Visión del producto](#1-visión-del-producto)
2. [Requisitos técnicos de arquitectura](#2-requisitos-técnicos-de-arquitectura)
3. [Historias de usuario y criterios de aceptación (BDD)](#3-historias-de-usuario-y-criterios-de-aceptación-bdd)
4. [Contrato de API (endpoints REST)](#4-contrato-de-api-endpoints-rest)
5. [Lista de cotejo: Definition of Ready (DoR)](#5-lista-de-cotejo-definition-of-ready-dor)
6. [Lista de cotejo: Definition of Done (DoD)](#6-lista-de-cotejo-definition-of-done-dod)
7. [Refinamiento técnico y estimación](#7-refinamiento-técnico-y-estimación)
8. [Plantilla de Pull Request](#8-plantilla-de-pull-request)
9. [Ejecución local](#9-ejecución-local)

---

## 1. Visión del producto

Servicio backend que expone una **API REST** para administrar el registro de clientes de una organización. Permite crear, consultar, actualizar y eliminar clientes, así como ubicarlos rápidamente por **DNI** (búsqueda exacta) o por **nombre** (coincidencia parcial, sin distinguir mayúsculas/minúsculas).

| Rol | Necesidad |
|---|---|
| Analista de Datos | Mantener actualizada la base de datos operativa de clientes (HU-01). |
| Operador del Sistema | Ubicar la información de un cliente con rapidez (HU-02). |

---

## 2. Requisitos técnicos de arquitectura

### 2.1 Stack tecnológico

| Aspecto | Decisión |
|---|---|
| Lenguaje | Java 21 (o 25) |
| Framework | Spring Boot 3.x |
| Módulos Spring | Spring Web, Spring Data JPA, Validation |
| Persistencia temporal | H2 en memoria: `jdbc:h2:mem:clientesdb` |
| Inspección de datos | Consola web de H2 activa (solo perfil local/pruebas) |
| Manejo de errores | `@RestControllerAdvice` con respuestas `ProblemDetail` (RFC 7807) |
| Pruebas | JUnit 5 + Mockito (unitarias), `@SpringBootTest` (integración) |
| Documentación de API | Swagger UI / OpenAPI (springdoc-openapi) |
| Control de versiones | Git + GitHub (ramas por historia, Pull Requests) |
| CI/CD y calidad | GitHub Actions + Codacy / SonarQube |

### 2.2 Arquitectura en capas

```
Cliente HTTP (Postman / Swagger UI)
        │
        ▼
┌───────────────────────────┐
│  Controller (REST)        │  ClienteController
│  - Valida entrada (@Valid)│
│  - Mapea códigos HTTP     │
└─────────────┬─────────────┘
              ▼
┌───────────────────────────┐
│  Service (negocio)        │  ClienteService
│  - Regla: DNI único       │
│  - Búsqueda DNI / nombre  │
└─────────────┬─────────────┘
              ▼
┌───────────────────────────┐
│  Repository (JPA)         │  ClienteRepository
└─────────────┬─────────────┘
              ▼
        H2 en memoria (clientesdb)

Transversal: GlobalExceptionHandler (@RestControllerAdvice → ProblemDetail)
```

### 2.3 Estructura de paquetes propuesta

```
src/main/java/pe/edu/unsch/clientes
├── ClientesApplication.java
├── controller/   ClienteController.java
├── service/      ClienteService.java
├── repository/   ClienteRepository.java
├── model/        Cliente.java
├── dto/          ClienteRequestDTO.java, ClienteResponseDTO.java
└── exception/    ClienteNoEncontradoException.java,
                  DniDuplicadoException.java,
                  GlobalExceptionHandler.java
src/test/java/... ClienteServiceTest.java, ClienteControllerIT.java
```

### 2.4 Configuración base (`application.properties`)

```properties
spring.datasource.url=jdbc:h2:mem:clientesdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=true

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

spring.mvc.problemdetails.enabled=true
```

### 2.5 Reglas de validación del modelo

| Campo | Regla |
|---|---|
| `dni` | Obligatorio, exactamente 8 dígitos numéricos, **único** |
| `nombre` | Obligatorio, no vacío |
| `email` | Obligatorio, formato de correo válido |

### 2.6 Manejo global de excepciones

| Excepción | HTTP | Cuándo ocurre |
|---|---|---|
| `MethodArgumentNotValidException` | 400 Bad Request | Payload con DNI, nombre o email inválidos |
| `DniDuplicadoException` | 409 Conflict | Se intenta registrar un DNI ya existente |
| `ClienteNoEncontradoException` | 404 Not Found | El ID solicitado no existe |

Ejemplo de respuesta estandarizada (RFC 7807):

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Ya existe un cliente registrado con el DNI 12345678",
  "instance": "/api/v1/clientes"
}
```

### 2.7 Requisitos no funcionales

- **Mantenibilidad:** separación estricta en capas; el controlador no contiene lógica de negocio.
- **Calidad:** cobertura de código mínima acordada por el equipo y análisis estático sin vulnerabilidades.
- **Trazabilidad:** cada cambio ingresa mediante Pull Request pequeño y atómico.
- **Documentación:** contrato de API visible en Swagger UI (`/swagger-ui.html`).

---

## 3. Historias de usuario y criterios de aceptación (BDD)

### HU-01: Gestión CRUD de clientes

> **Como** Analista de Datos, **quiero** registrar, consultar, actualizar y eliminar clientes, **para** mantener actualizada la base de datos operativa.

**Criterios de aceptación (Given / When / Then)**

**Escenario 1 — Creación exitosa**
- **Given** que envío una solicitud `POST /api/v1/clientes` con un payload válido (DNI único, nombre, email)
- **When** la API procesa el registro
- **Then** responde con `HTTP 201 Created`, incluye el header `Location` y el cliente generado con su ID

**Escenario 2 — Validación de DNI duplicado**
- **Given** que ya existe un cliente registrado con el DNI `"12345678"`
- **When** intento registrar otro cliente con el mismo DNI `"12345678"`
- **Then** la API responde con `HTTP 409 Conflict` indicando el fallo de unicidad

**Escenarios complementarios (propuestos para cubrir el CRUD completo)**

**Escenario 3 — Datos inválidos**
- **Given** que envío un `POST` con DNI de 7 dígitos o email mal formado
- **When** la API valida el payload
- **Then** responde con `HTTP 400 Bad Request` y un `ProblemDetail` que identifica los campos inválidos

**Escenario 4 — Consulta por ID**
- **Given** que existe un cliente con ID `1`
- **When** realizo `GET /api/v1/clientes/1`
- **Then** responde con `HTTP 200 OK` y los datos del cliente; si el ID no existe, responde `404 Not Found`

**Escenario 5 — Actualización**
- **Given** que existe un cliente con ID `1`
- **When** realizo `PUT /api/v1/clientes/1` con un payload válido
- **Then** responde con `HTTP 200 OK` y el cliente actualizado; si el ID no existe, responde `404 Not Found`

**Escenario 6 — Eliminación**
- **Given** que existe un cliente con ID `1`
- **When** realizo `DELETE /api/v1/clientes/1`
- **Then** responde con `HTTP 204 No Content`; si el ID no existe, responde `404 Not Found`

---

### HU-02: Búsqueda y filtrado por DNI o nombre

> **Como** Operador del Sistema, **quiero** realizar consultas por DNI o por coincidencia de nombre, **para** ubicar rápidamente la información del cliente.

**Criterios de aceptación (Given / When / Then)**

**Escenario 1 — Búsqueda exacta por DNI**
- **Given** que realizo una petición `GET /api/v1/clientes?dni=12345678`
- **When** la API ejecuta la consulta en H2
- **Then** retorna `HTTP 200 OK` con los datos del cliente encontrado

**Escenario 2 — Búsqueda parcial por nombre**
- **Given** que realizo una petición `GET /api/v1/clientes?nombre=Carlos`
- **When** la API busca coincidencias parciales sin diferenciar mayúsculas/minúsculas (case-insensitive)
- **Then** retorna `HTTP 200 OK` con el listado de clientes coincidentes

---

## 4. Contrato de API (endpoints REST)

| Método | Endpoint | Descripción | Parámetros / Body | HTTP esperado |
|---|---|---|---|---|
| POST | `/api/v1/clientes` | Crear cliente | Body: `ClienteRequestDTO` | 201 Created / 400 Bad Request / 409 Conflict |
| GET | `/api/v1/clientes` | Listar / buscar | Query opcionales: `dni`, `nombre` | 200 OK |
| GET | `/api/v1/clientes/{id}` | Obtener por ID | Path: `id` | 200 OK / 404 Not Found |
| PUT | `/api/v1/clientes/{id}` | Actualizar cliente | Path: `id`; Body: `ClienteRequestDTO` | 200 OK / 400 / 404 Not Found |
| DELETE | `/api/v1/clientes/{id}` | Eliminar cliente | Path: `id` | 204 No Content / 404 Not Found |

**`ClienteRequestDTO`**

```json
{
  "dni": "12345678",
  "nombre": "Carlos Quispe",
  "email": "carlos.quispe@correo.com"
}
```

**`ClienteResponseDTO`**

```json
{
  "id": 1,
  "dni": "12345678",
  "nombre": "Carlos Quispe",
  "email": "carlos.quispe@correo.com"
}
```

---

## 5. Lista de cotejo: Definition of Ready (DoR)

Acuerdo entre Product Owner y desarrolladores para que una historia pueda entrar al Sprint. Se marca **antes** de mover la tarjeta a *To Do / In Progress*.

### 5.1 DoR general (criterios de entrada)

| Categoría | Criterio | Verificación |
|---|---|---|
| Marco INVEST | La historia es independiente, negociable, aporta valor, estimable, pequeña y testeable | [ ] |
| Criterios de aceptación | Redactados de forma explícita y verificable (BDD: Given / When / Then) | [ ] |
| Contratos de API e interfaces | Arquitectura, esquemas de BD y contratos de integración pre-diseñados y acordados | [ ] |
| Gestión de dependencias | Dependencias de infraestructura, servicios externos o tareas previas resueltas | [ ] |
| Estimación técnica | Story Points asignados por el equipo (Planning Poker / Fibonacci) | [ ] |

### 5.2 DoR específica de esta tarea

| Criterio | HU-01 | HU-02 |
|---|---|---|
| La historia cumple INVEST (independiente, pequeña y testeable) | [x] | [x] |
| El contrato de API (DTOs, rutas y respuestas HTTP) está pre-diseñado y acordado (sección 4) | [x] | [x] |
| Las reglas de validación (DNI obligatorio de 8 dígitos, formato de email) están documentadas (sección 2.5) | [x] | [x] |
| La estimación en Story Points fue asignada por el equipo técnico (sección 7) | [x] | [x] |

> Nota: la estimación se realizó de forma individual como simulación del Planning Poker. En un equipo real debe votarse en conjunto.

---

## 6. Lista de cotejo: Definition of Done (DoD)

Compromiso de calidad que debe cumplirse **antes de hacer merge** a la rama principal. El revisor la usa como puerta de calidad en el Pull Request.

### 6.1 DoD general (criterios de salida)

| Categoría | Criterio | Verificación |
|---|---|---|
| Revisión de código | PR revisado y aprobado por al menos un Developer Senior o par | [ ] |
| Tamaño atómico del PR | Cambio conciso y enfocado (idealmente 200–400 líneas) | [ ] |
| Pruebas y CI/CD | Pruebas unitarias e integración pasan en el pipeline automatizado | [ ] |
| Análisis estático | Codacy / SonarQube sin vulnerabilidades ni code smells graves | [ ] |
| Documentación técnica | API, contratos y README actualizados | [ ] |
| Despliegue | Código desplegado y verificado en Staging/QA | [ ] |

### 6.2 DoD específica de esta tarea

- [ ] Código funcional con la base de datos H2 en memoria configurada.
- [ ] Suite de pruebas unitarias (JUnit 5 + Mockito) e integración (`@SpringBootTest`) ejecutadas con éxito en el pipeline de CI/CD.
- [ ] Pull Request atómico (< 300 líneas) revisado y aprobado por un desarrollador senior.
- [ ] Análisis estático libre de vulnerabilidades y con cobertura de código mínima aprobada.
- [ ] Documentación interactiva habilitada en Swagger / OpenAPI.

### 6.3 Clasificación de comentarios en Code Review

| Etiqueta | Cuándo usarla | Bloquea el merge |
|---|---|---|
| **[BLOQUEANTE]** | Error de lógica, falla de seguridad, fallo de arquitectura o violación de la DoD | Sí |
| **[SUGERENCIA]** (nitpick) | Estilo, nombres, mejoras opcionales | No |

Los comentarios se redactan sobre el **código**, no sobre la persona.

---

## 7. Refinamiento técnico y estimación

Estimación relativa en Story Points (Fibonacci: 1, 2, 3, 5, 8, 13). Valores propuestos en la simulación individual.

| Historia | Story Points | Justificación |
|---|---|---|
| HU-01 Gestión CRUD de clientes | 8 | Cinco endpoints, validaciones, regla de unicidad y manejo global de errores |
| HU-02 Búsqueda por DNI o nombre | 3 | Consultas derivadas en el repositorio y un parámetro opcional en el `GET` |

**Desglose de HU-01 en tareas técnicas (< 1–2 días cada una)**

| # | Tarea | Duración estimada |
|---|---|---|
| T1 | Crear proyecto (Spring Initializr) y configurar H2 | 2 h |
| T2 | Entidad `Cliente` y `ClienteRepository` | 2 h |
| T3 | DTOs con Bean Validation | 2 h |
| T4 | `ClienteService` con regla de DNI único | 3 h |
| T5 | `GlobalExceptionHandler` con `ProblemDetail` | 3 h |
| T6 | `ClienteController`: POST, GET, PUT, DELETE | 4 h |
| T7 | Pruebas unitarias (Mockito) e integración (`@SpringBootTest`) | 5 h |
| T8 | Configurar Swagger/OpenAPI y actualizar README | 2 h |

**Desglose de HU-02**

| # | Tarea | Duración estimada |
|---|---|---|
| T9 | Métodos `findByDni` y `findByNombreContainingIgnoreCase` | 2 h |
| T10 | Parámetros `dni` y `nombre` en `GET /api/v1/clientes` | 2 h |
| T11 | Pruebas de ambos escenarios | 3 h |

---

## 8. Plantilla de Pull Request

```markdown
## Resumen de cambios
<!-- Qué se hizo y por qué. Historia asociada: HU-0X -->

## Plan de pruebas
- [ ] Pruebas unitarias ejecutadas
- [ ] Pruebas de integración ejecutadas
- [ ] Probado manualmente en Swagger UI / Postman

## Checklist de la DoD
- [ ] PR atómico (< 300 líneas)
- [ ] CI en verde
- [ ] Análisis estático sin vulnerabilidades
- [ ] Documentación / README actualizados
- [ ] Revisión aprobada por un desarrollador senior o par
```

---

## 9. Ejecución local

**Requisitos:** JDK 21 o superior, Maven 3.9+ y Git.

```bash
git clone https://github.com/<usuario>/<repositorio>.git
cd <repositorio>
mvn spring-boot:run
```

| Recurso | URL |
|---|---|
| API | `http://localhost:8080/api/v1/clientes` |
| Consola H2 | `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:clientesdb`) |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |

Ejecutar pruebas:

```bash
mvn test
```

---

## Referencias

- Martin, R. C. (2008). *Clean Code: A Handbook of Agile Software Craftsmanship*. Pearson.
- Khorikov, V. (2020). *Unit Testing Principles, Practices, and Patterns*. Manning.
- Fowler, M. (2015). *Yagni*. martinfowler.com.
- IETF. (2016). *RFC 7807: Problem Details for HTTP APIs*.
