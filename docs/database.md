# Base de datos

SQLite + JPA/Hibernate + Flyway. La capa de persistencia está aislada tras
repositorios y mappers, de modo que migrar a otro motor sea un cambio local.

## Configuración

```yaml
spring:
  datasource:
    url: jdbc:sqlite:./data/dnd.db
    driver-class-name: org.sqlite.JDBC
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate.dialect: org.hibernate.community.dialect.SQLiteDialect
  flyway:
    enabled: true
```

`ddl-auto: validate` garantiza que el esquema gestionado por Flyway y el modelo JPA
no divergen.

## Tablas principales

| Tabla | Contenido |
|-------|-----------|
| `users` | Cuenta, hash BCrypt, display name, id único |
| `characters` | Personajes jugables (stats, HP, AC, ataques, inventario) |
| `campaigns` | Campaña, ambientación, fase, escena activa, DM |
| `campaign_members` | Relación usuario↔campaña↔personaje |
| `scenes` | Escenas narrativas con `image_prompt` |
| `maps` | Grid, dimensiones, JSON de paredes/puertas/obstáculos |
| `board_entities` | Posición de jugadores/enemigos/NPCs/objetos por escena |
| `npc_templates` | Plantillas de NPC |
| `enemy_templates` | Goblin, Goblin Archer, Valdros Guardian |
| `items` | Objetos e inventario |
| `combat_states` | Estado de combate (ronda, turno, activo) |
| `combat_participants` | Orden de iniciativa por entidad |
| `game_events` | Historial de eventos (fuente de verdad narrativa) |
| `dice_rolls` | Registro de cada tirada |

## Migraciones

`backend/src/main/resources/db/migration/V*.sql`. Nunca editar una migración ya
aplicada: crear una nueva versión.

## Decisiones

- SQLite por ser un prototipo personal y de un solo proceso (sin despliegue).
- Persistencia desacoplada: los servicios dependen de repositorios/mappers, no de SQL.
- Los eventos de juego son apéndices inmutables; el estado "actual" se puede reconstruir
  a partir de ellos en principio.
