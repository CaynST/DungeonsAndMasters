# Walkthrough — La Cripta de Valdros

Guion para probar la vertical slice de punta a punta.

## Preparación

```bash
# Terminal 1
cd backend && mvn spring-boot:run

# Terminal 2
cd frontend && pnpm install && pnpm dev
```

Abrir `http://localhost:5173`.

## Recorrido

1. **Login** — crea una cuenta (p. ej. `dm_demo` / `secret123`).
2. **Home** — "Sala de Aventuras": verás la lista de héroes predefinidos
   (Kael, Lyra, Aria, Thorin). Pulsa **Crear campaña**.
3. **Lobby** — estética de taberna. Elige un héroe y pulsa **Unirme**, luego
   **Comenzar aventura**.
4. **Intro** — el Dungeon Master narra la llegada a la cripta y pide una
   *prueba de Percepción*. Pulsa **Tirar dado** en el panel del DM.
5. **Exploración** — habla con el Archivista (**Hablar**) y realiza la
   *prueba de Investigación* (CD 14). Al superarla, se abre la puerta y
   comienza el combate.
6. **Combate** — el tracker de iniciativa muestra el orden. En tu turno:
   - **Mover**: clic en una casilla adyacente libre.
   - **Atacar**: activa *Atacar*, clic en un goblin, elige arma y confirma.
   - Observa el dado 3D, el daño flotante y la barra de HP del enemigo.
   - Pulsa **Terminar turno**; los enemigos y aliados actúan automáticamente.
7. **Jefe** — tras vencer a los goblins, la escena cambia al Sello de Valdros.
   El Guardián tiene más HP, CA alta y múltiples ataques.
8. **Victoria** — al caer el Guardián se otorga el *Relicario de Valdros* y
   aparece el resumen de aventura completada.

## Notas

- Sin API configurada, el DM responde con el guion del `MockAIProvider`
  (mismo flujo, narración predefinida pero consciente del estado).
- Los aliados no elegidos son controlados por el DM y actúan en su turno.
- El badge junto al nombre del DM muestra el proveedor de IA activo (`mock`).
