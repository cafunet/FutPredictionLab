# Patrones de diseno implementados en el backend

Este documento resume los 3 patrones de diseno aplicados en el proyecto y como estudiarlos en contexto real del codigo.

## 1) Repository Pattern

### Que es
Abstrae el acceso a datos para que la logica de negocio no dependa de SQL directo ni de detalles de persistencia.

### Donde esta implementado
- `src/main/java/com/futprediction/auth/repository/UsuarioRepository.java`
- `src/main/java/com/futprediction/teams/repository/EquipoRepository.java`
- `src/main/java/com/futprediction/match/repository/PartidoRepository.java`
- `src/main/java/com/futprediction/match/repository/ResultadoRepository.java`

### Como se usa
Los servicios invocan metodos del repositorio (`findById`, `save`, `existsBy...`) y no escriben queries manuales en la capa de negocio.

### Beneficio
- Menor acoplamiento con la base de datos.
- Mayor mantenibilidad y testabilidad.

---

## 2) Inyeccion de Dependencias (Dependency Injection)

### Que es
El contenedor de Spring crea e inyecta dependencias automaticamente en clases anotadas con `@Service`, `@Component`, etc.

### Donde esta implementado
- `src/main/java/com/futprediction/auth/service/AuthService.java`
- `src/main/java/com/futprediction/match/service/MatchService.java`
- `src/main/java/com/futprediction/shared/security/SecurityConfig.java`

### Como se usa
Se aplica inyeccion por constructor, por ejemplo en `AuthService`:
- `AuthenticationManager`
- `JwtService`
- `UsuarioRepository`
- `PasswordEncoder`

### Beneficio
- Facilita pruebas unitarias (mock de dependencias).
- Reduce acoplamiento entre clases.
- Claridad sobre lo que cada clase necesita para funcionar.

---

## 3) Strategy Pattern (nuevo)

### Que es
Permite encapsular reglas/algoritmos intercambiables bajo una misma interfaz.
En lugar de poner todas las validaciones en un solo metodo largo, cada regla vive en su propia clase.

### Donde esta implementado
- Interfaz:
  - `src/main/java/com/futprediction/match/service/rules/MatchSchedulingRule.java`
- Estrategias concretas:
  - `src/main/java/com/futprediction/match/service/rules/DifferentTeamsRule.java`
  - `src/main/java/com/futprediction/match/service/rules/TeamsMustExistRule.java`
- Orquestador:
  - `src/main/java/com/futprediction/match/service/MatchService.java` (`scheduleMatch`)

### Flujo
1. `MatchService` recibe `List<MatchSchedulingRule>` por inyeccion.
2. En `scheduleMatch`, itera y ejecuta `rule.validate(request)`.
3. Si una regla falla, lanza excepcion y se corta el flujo.
4. Si todas pasan, se crea el partido.

### Beneficio
- Regla nueva = clase nueva (sin tocar demasiado el servicio principal).
- Cumple mejor con el principio Open/Closed.
- Codigo mas limpio y extensible para reglas futuras.

---

## Guia rapida para estudiar este cambio

1. Leer `MatchService.scheduleMatch`.
2. Revisar la interfaz `MatchSchedulingRule`.
3. Comparar cada estrategia concreta (`DifferentTeamsRule`, `TeamsMustExistRule`).
4. Ver como Spring inyecta la lista de estrategias automaticamente.
5. Identificar como Repository + DI + Strategy trabajan juntos en el mismo caso de uso.

---

## Idea de siguiente paso academico

Si quieres profundizar patrones, el siguiente natural aqui seria `Factory Method` para centralizar la construccion de DTOs de respuesta (por ejemplo en auth y match) o `Facade` para unificar operaciones complejas de varios modulos.
