# Arquitectura: partidas autoalojadas (host + amigos)

**Estado:** propuesta, **no implementada**. Guardada el 2026-11-05 para retomarla después de las
brechas de reglas actuales (R-10/R-11).

## Objetivo

Convertir la aplicación en una experiencia donde **un jugador hostea la partida** y los demás se
unen mediante un enlace, una IP o un código. El host es la autoridad del juego; no hay P2P puro ni
consenso distribuido.

## Decisión de arquitectura

- **Host-autoritativo**, no P2P: el backend Spring Boot sigue validando reglas, estado, dados,
  combate, persistencia y WebSocket.
- **Mismo origen** siempre que sea posible: el backend sirve el frontend compilado, la API y el
  WebSocket desde un único puerto. Así `api.ts` (rutas relativas) y `ws.ts` (`location.host`)
  funcionan sin configuración extra y sin CORS.
- **LAN primero**: enlace/IP/QR. Internet después, mediante túnel gestionado; el port forwarding no
  será la opción principal.
- **Escritorio después**: la app de escritorio es una comodidad, no un requisito para validar el
  flujo.

## Fases

### Fase 1 — Host en LAN (MVP)

1. Compilar el frontend y servirlo desde Spring Boot (`frontend/dist` → `static/`).
2. Añadir fallback de SPA para que `/login`, `/campaign/{id}/lobby` y `/campaign/{id}/play`
   devuelvan `index.html`.
3. Arrancar en `0.0.0.0` con puerto configurable; script `start-host.sh` que compile, arranque y
   muestre la IP local.
4. Panel de host: URL, QR, enlace de campaña y estado del servidor.
5. Enlace de campaña: `http://IP:PUERTO/campaign/{id}/lobby`; el amigo se registra y entra.

**Criterios de aceptación:** dos navegadores en la misma LAN pueden registrarse, unirse a la
campaña, jugar y recibir eventos WebSocket sin proxy ni CORS manual.

**Huecos actuales del código:**

- `frontend/dist` existe, pero `WebConfig` solo sirve `/uploads/**`.
- No hay fallback de SPA; un deep link de React Router devolvería 404.
- `scripts/*.sh` y `application.yml` de HEAD usan `8080`; el árbol local del usuario
  puede sobrescribirlo a `8081` (cambio no commiteado).
- No hay panel de host, QR ni enlace de campaña.
- `SecurityConfig` permite CORS solo en `localhost`; para mismo origen no importa.
- `WebSocketConfig` permite `*`; conviene restringirlo en producción.

### Fase 2 — Pantalla “Unirse a servidor” (cliente fino/escritorio)

- Ruta `/join`: pedir URL/IP, normalizar (`http://`, sin barra final), probar `GET /api/health`,
  guardar `serverUrl` en `localStorage`.
- `api.ts` y `ws.ts` usan `serverUrl` cuando exista; por defecto, mismo origen.
- Útil sobre todo si el cliente no es un navegador con barra de direcciones.
- Manejo de errores: servidor caído, versión incompatible, URL inválida.

### Fase 3 — Internet sin port forwarding

- Integrar un túnel gestionado (Cloudflare Tunnel o Tailscale) con un botón “Crear enlace remoto”.
- Compartir la URL HTTPS del túnel; funciona detrás de CGNAT.
- Endurecer antes de exponer:
  - Código de invitación del servidor para el registro.
  - Rate limiting y límites de subida.
  - HTTPS (el túnel lo aporta).
  - Restricción de orígenes WebSocket.
  - Secretos JWT por host, backups de SQLite, logs sin datos sensibles.
  - Límites/cuotas para la IA del DM (la clave la paga el host).

### Fase 4 — App de escritorio

- Modo **Host**: arranca el backend (JRE incluido o native image) y sirve el frontend.
- Modo **Unirse**: webview o cliente que apunta a la URL del host.
- Empaquetado: Tauri (más ligero, requiere JRE/native image) o Electron (más directo, más pesado).
- Auto-update y firma de código si se distribuye fuera del círculo de amigos.

### Fase 5 — Código corto (opcional)

- Un código corto global **no puede resolver una IP por sí solo**: necesita un servicio de
  rendezvous que mapee código → URL del host/túnel.
- Alternativas: mDNS en LAN, QR/enlace directo, URL del túnel como “código”.
- Solo implementar si el producto lo exige; añade infraestructura pequeña (Cloudflare Worker,
  Supabase, VPS mínimo).

## Ahorro de infraestructura

- **Sí** para el servidor de partida: no hay VPS/DB central; el host pone CPU, ancho de banda y
  almacenamiento.
- **No del todo** para un producto general: distribución, updates, firma, rendezvous, relay/TURN,
  backups, soporte y cumplimiento siguen existiendo.
- Para 4–8 amigos el ahorro es real; para miles de usuarios el coste se traslada al soporte y a la
  fiabilidad del host.

## Riesgos

- CGNAT, firewall, IP dinámica y redes corporativas.
- El host debe estar encendido; no hay failover si se cae.
- Exponer el backend actual a Internet sin hardening es inseguro.
- La IA del DM usa la clave del host; riesgo de coste/abuso.
- SQLite: activar WAL, backups y export/import de partida.
- Empaquetar Java (JRE/native image) y firmar la app añade complejidad.

## No objetivos

- P2P real/WebRTC con consenso distribuido.
- Port forwarding manual como experiencia principal.
- Cuentas centrales o guardado en la nube en la primera fase.
- Matchmaking público en la primera fase.

## Preguntas abiertas

- ¿LAN primero o Internet desde el inicio?
- ¿Clientes por navegador, escritorio o ambos?
- ¿Túnel aceptado (Cloudflare/Tailscale) o cero terceros?
- ¿Invitación por enlace, código de servidor o aprobación del host?
- ¿Quién paga la IA del DM?
