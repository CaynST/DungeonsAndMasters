# TODO

Estados: `pending` · `in_progress` · `blocked` · `done`.
Prioridad: P0 (bloquea la demo) · P1 (fidelidad D&D) · P2 (pulido).

## Reglas (compatibilidad con el PHB)

| ID | Tarea | Prioridad | Estado |
|----|-------|-----------|--------|
| R-01 | Tiradas de salvación contra muerte (3 éxitos/fallos, 1 = 2 fallos, 20 = 1 PG, estabilizar, daño masivo) | P1 | done |
| R-02 | Economía de acciones completa: acción adicional, reacción, ataque de oportunidad, interacción gratuita | P1 | done |
| R-03 | Acciones de combate: Carrera, Retirada, Esquivar, Ayudar, Ocultarse, Preparado, Buscar, Usar objeto | P1 | done |
| R-04 | Condiciones del Apéndice A (cegado, derribado, apresado, asustado, etc.) | P1 | done |
| R-05 | Cobertura (media +2 / tres cuartos +5 / total) | P1 | done |
| R-06 | Rango de ataque (normal/largo, desventaja) y alcance cuerpo a cuerpo | P0 | done |
| R-07 | Movimiento diagonal: no atravesar esquinas de muros | P0 | done |
| R-08 | Terreno difícil (coste doble) | P2 | done |
| R-09 | Sorpresa | P2 | done |
| R-10 | Conjuros: trucos, espacios, concentración, componentes | P1 | done |
| R-11 | Rasgos de clase (Estilo de combate, Segundo aliento, Ataque furtivo, Furia, Inspiración, Canalizar divinidad, etc.) | P1 | in_progress |
| R-12 | Trasfondos PHB faltantes: Charlatán, Artista, Artesano gremial, Ermitaño, Forastero, Marinero | P2 | done |
| R-13 | Competencias de armadura/arma/herramienta e idiomas | P2 | done |
| R-14 | Descanso corto/largo y dados de golpe | P2 | pending |
| R-15 | Peso/equipamiento y herramientas: peso por objeto, carga total, capacidad FUE×15 y aviso de sobrecarga en ficha/tienda (sin efecto en velocidad) | P3 | done |
| R-16 | Ventaja/desventaja expuesta en la UI (el motor ya la soporta) | P2 | done |
| R-17 | d100 en la mesa de dados | P3 | done |
| R-18 | Consumo de munición en combate (flechas/virotes) y penalización de carga en el movimiento | P3 | pending |

## Producto / UX

| ID | Tarea | Prioridad | Estado |
|----|-------|-----------|--------|
| U-01 | Tablero a pantalla completa (hoy queda con márgenes) | P2 | done |
| U-02 | Retratos/consonancia visual: componente `Avatar` reutilizable con retrato local y fallback de iniciales | P2 | done |
| U-03 | Segunda pasada visual: Home, Lobby, Crónica y estados vacíos | P2 | done |
| U-04 | Revisión de accesibilidad (contraste AA, foco, aria en iconos) | P2 | pending |
| U-05 | Responsive del juego (sidebar fija, barra horizontal) | P2 | done |
| U-06 | Paginación y filtro de estado en Home (sin borrar campañas) | P3 | done |
| U-07 | Generación dinámica de escenas por el DM a mitad de aventura | P2 | pending |
| U-08 | Modo host autoalojado: servir frontend desde Spring, SPA fallback, enlace/QR y enlace de campaña | P2 | pending |
| U-09 | Pantalla "Unirse a servidor" y URL configurable de API/WebSocket | P2 | pending |
| U-10 | Acceso remoto por túnel gestionado (Cloudflare/Tailscale) + hardening e invitaciones | P2 | pending |
| U-11 | App de escritorio con modos Host/Unirse (Tauri/Electron) | P3 | pending |
| U-12 | Código corto/rendezvous (solo si el producto lo exige) | P3 | pending |

## Hecho en esta sesión

| ID | Tarea | Estado |
|----|-------|--------|
| D-01 | Imágenes SVG→PNG reales (retratos, fondos, enemigos) | done |
| D-02 | Proxy de `/images` y `/uploads` en Vite | done |
| D-03 | No exponer el razonamiento del modelo | done |
| D-04 | Barra de acciones con el personaje del usuario | done |
| D-05 | Dirección de arte "mesa de taberna" + campaña destacada | done |
| D-06 | 14 fondos por temática para campañas/escenas | done |
| D-07 | Fichas del tablero con retrato | done |
| D-08 | Intro con imagen, viñeta y letterbox | done |
| D-09 | Rango/alcance de ataque + desventaja a larga distancia | done |
| D-10 | Movimiento diagonal sin atravesar esquinas | done |
| D-11 | Muerte y salvaciones de muerte (moribundo/estable/muerto) | done |
| D-12 | Economía de turno: Dash/Dodge/Disengage y ataques de oportunidad | done |
| D-13 | Condiciones 5e: modelo, efectos en ataque/prueba/movimiento, acciones del DM y UI | done |
| D-14 | Cobertura 5e: línea de visión, +2/+5 CA y bloqueo total | done |
| D-15 | Rasgos de clase: Segundo aliento, Furia y Ataque furtivo | done |
| D-18 | Conjuros: trucos, espacios, ataques, salvaciones y curación | done |
| D-19 | Concentración y Bendición (+1d4 a ataques/salvaciones, rotura por daño) | done |
| D-21 | Conjuros de área: Onda de trueno (cubo, salvación de CON, 2d8) | done |
| D-22 | Componentes materiales de conjuros: foco/bolsa, metadata V/S/M y UI | done |
| D-23 | Estilo de combate: Defensa, Arquería y Duelo (+ selección en creación) | done |
| D-24 | Inspiración bárdica: d6, usos por CAR, acción adicional y uso en tiradas | done |
| D-25 | Terreno difícil: celdas persistidas y coste doble de movimiento | done |
| D-26 | Sorpresa: la criatura marcada pierde su primer turno de combate | done |
| D-27 | Disparador de sorpresa: acción validada del DM (`SET_SURPRISED`) | done |
| D-28 | Tablero sin anillo vacío + layout del juego adaptativo (móvil/tablet) | done |
| D-29 | Ventaja/desventaja visible en el registro de ataques del DM | done |
| D-30 | d100 en el menú de dados y en la mesa 3D | done |
| D-31 | Trasfondos PHB 2014 restantes: Charlatán, Artista, Artesano gremial, Ermitaño, Forastero y Marinero | done |
| D-32 | Idiomas y competencias de armadura/arma/herramienta derivados y visibles en la ficha | done |
| D-33 | `Avatar` reutilizable con retrato local y fallback de iniciales (Home, Lobby, tarjetas e iniciativa) | done |
| D-34 | Listado paginado/filtrable de campañas y controles en Home sin borrar datos | done |
| D-35 | Peso de inventario, capacidad de carga, aviso de sobrecarga y herramientas/munición iniciales | done |
| D-36 | Acciones R-02/R-03: Ayudar, Ocultarse, Preparado, Buscar, Usar objeto, interacción y reacciones | done |
| D-37 | Segunda pasada visual: Home, Lobby, Crónica, estados vacíos y de carga | done |
| D-38 | Gran arma: repetición por dado de 1/2 con arma a dos manos y bonus en el evento de ataque | done |
| D-39 | Protección: reacción automática acotada que impone desventaja a ataques contra aliados a 5 pies | done |
