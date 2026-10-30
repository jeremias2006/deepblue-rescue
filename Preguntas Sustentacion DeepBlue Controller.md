# Preguntas de sustentación — DeepBlue Rescue (Capa Controller)

Respuestas a las 30 preguntas de la sección 113 del laboratorio, con ejemplos tomados del proyecto.

---

## Controller y arquitectura

### 1. ¿Cuál es la responsabilidad del Controller?

Traducir HTTP hacia la aplicación y la respuesta de la aplicación hacia HTTP. Se encarga de las URLs, los métodos HTTP, el body, los path variables, los query parameters, la validación de entrada, los códigos de estado y el cuerpo de la respuesta. No toma decisiones de negocio: recibe la petición, delega en el Service y devuelve el resultado.

### 2. ¿Qué diferencia existe entre Controller y Service?

El Controller responde a la pregunta **"¿cómo llega la petición?"** y el Service a **"¿está permitida la operación?"**.

- **Controller:** maneja el protocolo HTTP (rutas, JSON, status codes).
- **Service:** implementa las reglas de negocio (especialista activo, transiciones de estado, fechas válidas) y abre las transacciones.

Ejemplo: `TreatmentController.register()` solo recibe el JSON y devuelve `201 Created`; `TreatmentServiceImpl.register()` verifica que el animal exista, que la especialista esté activa y que el caso no esté `RELEASED`.

### 3. ¿Por qué Controller no debería usar Repository directamente?

Porque se saltaría la capa donde viven las reglas de negocio y las transacciones. Si el Controller llama al Repository:

- conoce detalles de persistencia que no le corresponden;
- termina duplicando o mezclando reglas de negocio;
- tiende a devolver entidades en lugar de DTOs;
- se vuelve difícil de probar, porque ya no se puede aislar con un Service mock.

El flujo correcto es `Controller → Service → Repository`.

---

## Anotaciones de Spring MVC

### 4. ¿Qué hace `@RestController`?

Marca la clase como un controlador REST. Combina `@Controller` y `@ResponseBody`: Spring registra la clase para atender peticiones HTTP y convierte automáticamente lo que retornan sus métodos a JSON en el body de la respuesta, sin buscar vistas HTML.

### 5. ¿Qué hace `@RequestMapping`?

Define la ruta base (y opcionalmente el método HTTP) que atiende un controlador o un método. En el proyecto se usa a nivel de clase, por ejemplo `@RequestMapping("/api/rescue-cases")`, y los métodos agregan su parte con `@GetMapping`, `@PostMapping` o `@PatchMapping`.

### 6. ¿Qué diferencia existe entre `@PathVariable` y `@RequestParam`?

| | `@PathVariable` | `@RequestParam` |
|---|---|---|
| Dónde viaja | Dentro de la ruta | En el query string (`?clave=valor`) |
| Uso típico | Identificar un recurso concreto | Filtrar o parametrizar una consulta |
| Ejemplo | `GET /api/rescue-cases/RES-2026-001` | `GET /api/rescue-cases?status=IN_REHABILITATION` |
| Obligatorio | Sí, es parte de la URL | Por defecto sí, pero puede hacerse opcional |

### 7. ¿Qué hace `@RequestBody`?

Toma el cuerpo JSON de la petición y lo convierte (deserializa) a un objeto Java, normalmente un DTO de request como `CreateTreatmentRequest` o `ChangeRescueStatusRequest`. Si el JSON está mal formado o contiene un valor de enum inexistente, Spring lanza `HttpMessageNotReadableException`.

### 8. ¿Qué hace `@Valid`?

Activa Bean Validation sobre el objeto recibido. Spring revisa las anotaciones del DTO (`@NotBlank`, `@NotNull`, `@Size`, `@PastOrPresent`) antes de ejecutar el método del Controller. Si alguna falla, lanza `MethodArgumentNotValidException` y el método nunca se ejecuta, por lo que el Service no se llega a invocar.

---

## Validación y reglas de negocio

### 9. ¿Qué diferencia existe entre validación de entrada y regla de negocio?

- **Validación de entrada:** comprueba que el request esté bien formado, sin consultar el estado del sistema. Se resuelve en el DTO con Bean Validation. Ejemplos: `animalCode` vacío, `status` nulo, descripción de menos de 10 caracteres, fecha futura. Respuesta: **400**.
- **Regla de negocio:** comprueba si la operación está permitida según el estado del sistema. Se resuelve en el Service. Ejemplos: animal inexistente, especialista inactivo, caso `RELEASED`, transición `ADMITTED → RELEASED`. Respuesta: **404** o **409**.

Un request puede ser válido en su forma y aun así violar una regla de negocio.

---

## Métodos HTTP

### 10. ¿Cuándo utilizar GET?

Para **consultar** recursos. No debe modificar el estado del sistema. Ejemplo: `GET /api/animals/AN-001`.

### 11. ¿Cuándo utilizar POST?

Para **crear** un nuevo recurso. Ejemplo: `POST /api/treatments` registra un tratamiento nuevo y responde `201 Created`.

### 12. ¿Cuándo utilizar PATCH?

Para **modificar parcialmente** un recurso, es decir, cambiar solo algunos de sus campos sin reemplazarlo completo. Ejemplo: `PATCH /api/rescue-cases/RES-001/status` modifica únicamente el `status` del caso.

---

## Códigos de estado HTTP

### 13. ¿Qué significa 200?

**OK.** La operación fue exitosa (una consulta o una actualización correcta) y la respuesta lleva el resultado.

### 14. ¿Qué significa 201?

**Created.** La operación creó un nuevo recurso. Se usa en el `POST /api/treatments`.

### 15. ¿Qué significa 400?

**Bad Request.** El request es inválido: campos que no pasan la validación, JSON mal formado, un enum inexistente o un query parameter con un valor no válido.

### 16. ¿Qué significa 404?

**Not Found.** El recurso solicitado no existe. Ejemplo: `GET /api/animals/AN-999`. El Service lanza `ResourceNotFoundException` y el handler la convierte en 404.

### 17. ¿Qué significa 409?

**Conflict.** El request es válido, pero la operación entra en conflicto con una regla de negocio o con el estado actual del recurso. Ejemplo: registrar un tratamiento a un animal con caso `RELEASED`, o una transición de estado inválida.

### 18. ¿Qué significa 500?

**Internal Server Error.** Ocurrió un error inesperado en el servidor. El cliente no debe recibir detalles internos (stack trace, SQL, secretos), solo un mensaje genérico.

---

## Manejo de errores

### 19. ¿Para qué sirve `ResponseEntity`?

Para controlar la respuesta HTTP completa: el **código de estado**, los **headers** y el **body**. Permite devolver `200 OK`, `201 Created` u otros códigos de forma explícita, en lugar de depender del valor por defecto.

### 20. ¿Qué problema resuelve `@RestControllerAdvice`?

Centraliza el manejo de excepciones de todos los controllers en una sola clase (`GlobalExceptionHandler`). Evita repetir bloques `try/catch` en cada endpoint y garantiza que cada tipo de excepción se traduzca siempre al mismo código HTTP y al mismo formato de respuesta.

### 21. ¿Por qué conviene tener un `ErrorResponse` común?

Porque todos los errores tienen la misma estructura (`timestamp`, `status`, `error`, `message`, `details`). Así el cliente que consume la API sabe siempre cómo interpretar un error, el código es más consistente y los tests pueden verificar el mismo contrato en todos los casos.

### 22. ¿Para qué sirve `details`?

Para dar información adicional del error. En los errores de validación contiene un mapa campo → mensaje, por ejemplo:

```json
{
  "animalCode": "Animal code is required",
  "description": "Description is required"
}
```

En los errores sin información adicional (404, 409, 500) va vacío (`Map.of()`).

### 23. ¿Qué diferencia existe entre `MethodArgumentNotValidException` y `BusinessRuleException`?

| | `MethodArgumentNotValidException` | `BusinessRuleException` |
|---|---|---|
| Quién la lanza | Spring, al fallar `@Valid` | El Service, al violar una regla |
| Cuándo | Antes de entrar al Controller | Durante la lógica de negocio |
| Causa | El request está mal formado | El request es válido pero la operación no está permitida |
| HTTP | **400 Bad Request** | **409 Conflict** |
| Incluye `details` | Sí, un mapa por campo | No (vacío) |

---

## Testing

### 24. ¿Qué prueba `@WebMvcTest`?

Prueba únicamente la **capa web**: levanta el Controller indicado, el `GlobalExceptionHandler` importado y la infraestructura de Spring MVC (conversión JSON, validación, mapeo de rutas). No carga Services reales, Repositories ni base de datos.

### 25. ¿Por qué utilizamos un Service mock?

Para aislar el Controller. Queremos verificar el contrato HTTP (URL, método, JSON, status y manejo de errores) sin que influya cómo funciona el Service por dentro. Con `@MockitoBean` definimos lo que el Service devuelve o lanza en cada test, lo que permite simular fácilmente casos como un 404 o un 409. La lógica del Service ya se prueba aparte en sus propios tests.

### 26. ¿Qué permite probar `MockMvc`?

Simular peticiones HTTP contra el Controller sin levantar un servidor real. Permite verificar la URL y el método, enviar JSON, comprobar el código de estado, y validar el cuerpo de la respuesta con `jsonPath`.

### 27. ¿Por qué el test de Controller no necesita PostgreSQL?

Porque el Controller no accede a datos: solo habla con el Service, y en el test el Service es un mock. Como `@WebMvcTest` no carga Repositories ni JPA, no hay conexión a ninguna base de datos. Por eso estos tests corren rápido, sin Docker ni Testcontainers.

### 28. ¿Por qué `verify(..., never())` es útil en validaciones?

Porque demuestra que, cuando el request es inválido, **el Service nunca fue invocado**. Comprueba que la validación detuvo la petición en la capa web y que no se ejecutó ninguna lógica de negocio con datos incorrectos. Ejemplo: tras un `PATCH` con body `{}`, `verify(service, never()).changeStatus(anyString(), any())`.

---

## Responsabilidades por capa

### 29. ¿Qué capa debe abrir transacciones?

La capa **Service**. Ahí está el `@Transactional`, porque una operación de negocio puede involucrar varias consultas y escrituras que deben tener éxito o fallar juntas. En el proyecto, `TreatmentServiceImpl` y `RescueCaseServiceImpl` usan `@Transactional(readOnly = true)` a nivel de clase y `@Transactional` en los métodos que escriben (`register`, `changeStatus`). El Controller y el Repository no abren transacciones.

### 30. ¿Qué capa debe decidir una transición de estado?

La capa **Service**, porque es una regla de negocio. En el proyecto, `RescueCaseServiceImpl.isValidTransition()` define qué cambios de estado se permiten (por ejemplo, `IN_REHABILITATION` solo puede pasar a `READY_FOR_RELEASE`). El Controller solo recibe el nuevo estado y delega.

---

> **Idea principal:** el Controller traduce HTTP hacia la aplicación; el Service decide qué está permitido por el negocio; el Repository accede a la persistencia; y el `GlobalExceptionHandler` traduce las excepciones a un contrato HTTP consistente.
