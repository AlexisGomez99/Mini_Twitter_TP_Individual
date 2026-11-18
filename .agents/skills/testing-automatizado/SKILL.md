---
name: testing-automatizado
description: Guía para escribir tests automatizados con JUnit 5.13 en este proyecto Java. Usá este skill cuando vayas a crear o modificar tests en src/test/java/.
---

## Contexto del Proyecto

- Framework: JUnit 5.13
- Ubicación: `src/test/java/`
- Tests unitarios en memoria (código real, sin mocks)
- Tests de integración con base de datos
- Comandos de consola disponibles:
    - `./mvnw test`
    - `./mvnw verify` (ya configura fallo de build si cobertura < 90%)

## Flujo de Ejecución y Cobertura

Cuando se genera o modifica código en `/backend/src/main/java`:

1. Ejecutar `./mvnw test` por consola.
2. Ejecutar `./mvnw verify` por consola para revisar cobertura.
3. Si fallan los tests, realiza las modificaciones necesarias y vuelve a ejecutar los tests (no detenerse por ese motivo).
4. Si los tests pasan pero falla cobertura (< 90%), realiza las modificaciones necesarias y vuelve a ejecutar los tests hasta cumplir el umbral.

## 1. Nomenclatura Clara y Descriptiva

### Patrón de Nombre del Método

Usa el patrón: **`cuestionATestear_resultadoEsperado`**

### Anotación @DisplayName

```java
@Test
@DisplayName("Agregar contacto lanza excepción cuando es duplicado")
void agregarContacto_lanzaExcepcionCuandoEsDuplicado() { ... }
```

## 2. Sin Mocks, Stubs ni Fakes

- Los tests unitarios corren **en memoria ejecutando código real**
- **NO uses mocks, stubs o fakes** - complican la lectura
- Excepción: Solo para servicios externos (pagos online, emails, etc.)

## 3. Estructura del Test (Setup-Ejercitación-Verificación)

Cada test debe seguir esta estructura con **comentarios explícitos**:

```java
@Test
@DisplayName("Agregar producto incrementa el total del carrito")
void agregarProducto_incrementaTotal() {
    // Setup: Preparar el escenario
    var carrito = new Carrito();
    var producto = new Producto("Laptop", 1000.0);
    
    // Ejercitación: Ejecutar la acción a probar
    carrito.agregar(producto);
    
    // Verificación: Verificar el resultado esperado
    assertEquals(1000.0, carrito.total(), "El total debe ser 1000.0");
}
```

## 4. Un Solo Caso de Prueba por Test

- Cada test evalúa **un único caso de prueba**
- Si necesitas evaluar múltiples casos, crea **un test separado** para cada uno

## 5. Asserts Claros y Descriptivos

Utiliza aserciones con **mensajes descriptivos**:

```java
assertEquals(expectedValue, actualValue, 
    "El valor esperado no coincide con el valor actual");
```

## 6. Probar Casos Límite

Siempre incluye tests para:

- ✅ Valores nulos (`null`)
- ✅ Listas vacías o inputs vacíos
- ✅ Números negativos o fuera de rango
- ✅ Estados inválidos o excepciones esperadas

## 7. Verificación de Excepciones

Usa `assertThrows` y **las constantes del código real** para los mensajes de error:

```java
@Test
@DisplayName("Crear cliente lanza excepción cuando nombre es null")
void crearCliente_lanzaExcepcionCuandoNombreEsNull() {
    // Setup: Preparar el escenario
    // (no se necesita setup adicional)
    
    // Ejercitación y Verificación
    var ex = assertThrows(RuntimeException.class, () -> {
        new Cliente(null, 25);
    });
    assertEquals(Cliente.ERROR_NOMBRE_INVALIDO, ex.getMessage(),
        "Debe lanzar el error correcto para nombre nulo");
}
```

❌ **Evitar strings hardcodeados:**
```java
assertEquals("El nombre no puede ser nulo", ex.getMessage());
```

✅ **Usar constantes del código real:**
```java
assertEquals(Cliente.ERROR_NOMBRE_INVALIDO, ex.getMessage());
```

## 8. Testing de Integración

### Setup Inicial

- Siempre incluir `@BeforeEach` con truncate:

```java
@BeforeEach
void beforeEach() {
    emf.getSchemaManager().truncate();
}
```

### Separación de Responsabilidades

**NO incluyas en tests de integración casos que pueden ser tests unitarios.**

## Checklist de Test

- [ ] Nombre sigue patrón `cuestionATestear_resultadoEsperado`
- [ ] Tiene `@DisplayName` descriptivo
- [ ] Tiene comentarios de estructura (Setup, Ejercitación, Verificación)
- [ ] Prueba un solo caso
- [ ] Asserts con mensajes descriptivos
- [ ] Usa constantes del código real para mensajes de error
- [ ] Si es test de integración, tiene `@BeforeEach` con truncate
- [ ] No usa mocks innecesarios