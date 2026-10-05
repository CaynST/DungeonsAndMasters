# Roadmap

El prototipo cubre la vertical slice del one-shot. Lo siguiente, por prioridad.

## Corto plazo (pulido del prototipo)
- [ ] Proveedor de IA real verificado (OpenAI-compatible) y prompt del DM pulido.
- [ ] Generación real de imágenes por escena (`ImageProvider` real).
- [ ] Más contenido de una sola escena: puzzles y loot.

## Medio plazo (producto)
- [ ] Amigos y presencia (lista de amigos, invitaciones).
- [ ] Varias campañas activas por usuario con progresión persistente.
- [ ] Editor de mapas básico.
- [ ] Inventario y equipo con efectos mecánicos.
- [ ] Selección de edición/sistema de D&D (5e, OSR, homebrew).

## Largo plazo
- [ ] Fog of war y línea de visión.
- [ ] Condiciones de estado (envenenado, derribado…), hechizos con áreas.
- [ ] Sesiones largas con memoria narrativa del DM.
- [ ] Despliegue (Docker/cloud) — actualmente fuera de alcance por diseño.

## Deuda técnica consciente
- `DmActionValidator` cubre los casos del one-shot, no todas las combinaciones.
- Persistencia de movimiento/paredes en JSON; si crece, normalizar.
- Sin i18n (contenido en español; UI puede migrar a textos clave).
