---
name: coding-conventions
description: StoreOps code style and the checklist for adding an endpoint or module. Read before writing code.
---

# Coding conventions

## Style (enforced by `mvn checkstyle:check`, runs in `validate`)
- 120-column lines, braces always, no star or unused imports, newline at end of file, no tabs.
- Javadoc one-liner on every class, as in the existing code.

## Shape of code
- DTOs and entities are records (`Activity`, `CreateActivityRequest`, `UpdateActivityRequest`). Request validation via `jakarta.validation` (`@NotBlank`, `@NotNull`) with `@Valid` on the controller parameter.
- Service constructors are package-private; the class is `public` with `@Service`. Repository classes are package-private with `@Repository` and a `ConcurrentHashMap`.
- Domain values are enums (`ActivityStatus`, `ActivityPriority`, `ActivityCategory`, `ProgrammeRole`), never string literals such as `"OPEN"`.
- Optional request fields get defaults in the Service, not the controller (see `ActivityService.create`: priority MEDIUM, category GENERAL).
- Partial update: a null request field means leave unchanged (see `ActivityService.update`).
- IDs are `UUID.randomUUID().toString()` assigned in the Service.
- Controllers: `@ResponseStatus(HttpStatus.CREATED)` for POST creates, `NO_CONTENT` for DELETE.
- Errors: throw the AppError subclass from `common.error`. See `architecture-principles` G2.

## Adding an endpoint (all of these, same sprint)
1. Controller method (thin) -> Service method -> Repository method if needed.
2. Service publishes a `DomainEvent` via `EventBus` if it changes state.
3. Tests per `how-to-test`.
4. New row in the README endpoint table and in `app-context` if the endpoint list changes.

## Adding cross-module access
Add a method to the owning module's `*Reader` interface and implement it in that Service. Do not make a Repository public.

## Do not touch
`pom.xml` surefire `argLine` and the Mockito agent setup (fixes a macOS failure), `config/checkstyle.xml`, `config/spotbugs-exclude.xml`, `ArchitectureRulesTest.java`, and anything under `.harness/`.
