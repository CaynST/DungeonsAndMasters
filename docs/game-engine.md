# Game Engine

Todo el cálculo vive en el backend, en `com.dndmasters.engine`, y es determinista y
testeable con un `Random` inyectable.

## Dados

Expresiones soportadas: `NdM`, `NdM+K`, `NdM-K`, constante.

```
1d20        → un d20
1d20 + 5    → d20 + 5
2d6 + 3     → 2d6 + 3
1d8 + 3     → d8 + 3
```

`DiceEngine.roll(expr)` devuelve:

```json
{ "expression": "1d20+5", "rolls": [17], "modifier": 5, "total": 22,
  "natural20": false, "natural1": false }
```

Entrada malformada lanza `DiceParseException` (nunca devuelve 0 silenciosamente).

## Modificadores

`modifier = floor((score - 10) / 2)`.

| Score | Mod |
|-------|-----|
| 8–9 | −1 |
| 10–11 | 0 |
| 12–13 | +1 |
| 14–15 | +2 |
| 16–17 | +3 |
| 18–19 | +4 |

## Checks

`CheckResolver.resolve(combatant, skill, dc)`:

- `total = d20 + mod(skill)`.
- Éxito si `total >= dc`.
- **20 natural** = éxito crítico; **1 natural** = fallo crítico.
- `advantage`: tira 2d20 y toma el mayor; `disadvantage`: el menor.

## Ataques y daño

`AttackResolver.resolve(attacker, target, attack)`:

- `attackRoll = d20 + attackBonus`.
- Impacta si `attackRoll >= target.ac`, salvo **20 natural** (siempre impacta) o
  **1 natural** (siempre falla).
- Crítico natural 20: se doblan los dados de daño (no el modificador).

`DamageResolver` aplica el daño, nunca por debajo de 0 HP, y marca `dead` cuando
`hp <= 0`.

## Iniciativa, turnos y rondas

- Iniciativa = `d20 + DEX mod`; desempate por mayor DEX y luego por id.
- El turno avanza por el orden de iniciativa. Al cerrar el último participante
  empieza una nueva ronda.
- Los participantes muertos se **saltan** automáticamente.
- El combate termina cuando solo queda un bando (o por acción explícita).

## Movimiento

- Grid con coste por casilla (por defecto 1).
- La ruta se valida casilla a casilla: **pared** u **obstáculo** bloquean; una casilla
  **ocupada** por otra criatura bloquea.
- El coste total no puede superar la velocidad restante del turno.
- Un movimiento inválido devuelve `ok=false` con un `reason`; no se aplica parcialmente.

## Flujo de estado de combate

```
NOT_STARTED ──startInitiative──► ACTIVE ──lastEnemyDown──► FINISHED
                                   │
                                   ├─ nextTurn (skip muertos)
                                   └─ new round al cerrar la iniciativa
```
