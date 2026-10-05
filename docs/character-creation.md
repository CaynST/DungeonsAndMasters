# Reglas y creación de personajes

El backend es la autoridad de las reglas. El cliente solo elige opciones; **nunca calcula**
HP, CA, bonificadores ni ataques. La IA tampoco: solo narra.

## Ediciones soportadas

La campaña fija una edición (`campaigns.edition`), y la creación de personaje respeta ese mismo
conjunto de reglas. Catálogo servido por `GET /api/rulesets/{edition}`.

| Edición | Id | Diferencias clave |
|---------|----|-------------------|
| D&D 5e (2014) | `dnd-5e-2014` | "Raza". Bonificadores de característica fijos por raza. |
| D&D 5e (2024) | `dnd-5e-2024` | "Especie". Bonificadores vienen del **trasfondo**. Trasfondos nuevos (Guía, Viajero…). |

Ambas ediciones incluyen 8 especies, 12 clases y 7 trasfondos. Los ids desconocidos resuelven
a 2014 de forma permisiva.

## Flujo de creación

```
Edición → Especie/Raza → Clase → Trasfondo → Puntos (27) → Habilidades → Revisión → Servidor
```

1. **Especie** aporta velocidad, tamaño, visión en la oscuridad y bonificadores (2014).
2. **Clase** aporta dado de golpe, salvaciones competentes, lista de habilidades y equipo.
3. **Trasfondo** aporta 2 habilidades y, en 2024, los bonificadores de característica.
4. **Puntos:** 27 puntos, valores 8–15, coste escalonado (13→14 y 14→15 cuestan 2).
5. **Habilidades:** se eligen N de la lista de la clase (el trasfondo se añade solo).

## Cálculos del motor (`engine/character/CharacterBuilder`)

| Valor | Fórmula |
|-------|---------|
| Modificador | `floor((score − 10) / 2)` |
| HP nivel 1 | dado de golpe de la clase + mod CON |
| CA sin armadura | 10 + mod DES |
| CA cuero | 11 + mod DES |
| CA cota de mallas | 16 (fija) |
| Ataque | mod de la característica del arma + competencia (+2 a nivel 1) |
| Daño | dado del arma + mod de la característica |

### Validaciones (rechazan con 400)

- Distribución de puntos inválida (no suma 27) o valores fuera de 8–15.
- Habilidad no permitida por la clase, o más habilidades de las debidas.
- Especie/clase/trasfondo desconocidos.

## Persistencia

El personaje guarda las **elecciones** (`creation_data_json`: edición, especie, clase, trasfondo,
habilidades, salvaciones, dado de golpe, puntos base) junto a los valores derivados. Guardar las
elecciones permite recalcular o migrar reglas después sin perder información.

## API

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/rulesets` | Todas las ediciones |
| GET | `/api/rulesets/{edition}` | Catálogo de una edición |
| POST | `/api/characters` | Crea un personaje validando reglas |
| POST | `/api/campaigns` | Crea campaña con `{ name, edition }` |

## Tests

`PointBuyTest`, `RuleSetRegistryTest`, `CharacterBuilderTest` (unitarios del motor) y
`CharacterCreationApiTest` (integración HTTP). El builder verifica bonificadores por edición,
HP, CA por tipo de armadura, ataques y rechazo de builds inválidos.
