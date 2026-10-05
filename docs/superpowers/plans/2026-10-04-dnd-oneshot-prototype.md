# La Cripta de Valdros — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (Native)
> to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a playable vertical slice of an AI-DM D&D platform demoing the one-shot "La Cripta de Valdros".

**Architecture:** Spring Boot backend is the single authority over game state; a pure rules `engine` does all math; a decoupled `AIProvider` (Mock by default) emits structured actions validated by the engine; React/TS frontend renders Home → Lobby → VTT with an SVG board and R3F 3D dice.

**Tech Stack:** Java 21, Spring Boot 3, Maven, Spring Security + JWT (jjwt), JPA/Hibernate, Flyway, SQLite, STOMP WebSocket. React 18 + TypeScript + Vite, Tailwind, shadcn/ui primitives, Zustand, TanStack Query, Three.js/R3F/drei, Framer Motion, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-04-dnd-oneshot-prototype-design.md`

## Global Constraints

- Java 21, Spring Boot 3.3.x, Maven; no Gradle.
- SQLite only; no PostgreSQL/Redis/Docker/microservices.
- All game math in `backend ...engine`; AI never touches DB or computes results.
- No secrets in code; AI/image config via env vars (`AI_PROVIDER`, `AI_API_KEY`, `AI_MODEL`, `IMAGE_PROVIDER`).
- Mock providers must make the full flow work with zero external services.
- Frontend: only the listed libraries; board starts as SVG.
- Palette and fonts exactly as in the spec (Cinzel + Inter); 70% modern gaming / 30% fantasy.
- Conventional Commits; no automatic remote push; run tests/lint/build before each commit.

## Review Focus

- **Movement into walls/obstacles/occupied tiles** — must be rejected with a clear reason, not partially applied.
- **Attacks declared out of turn / by dead entities** — engine must reject, not mutate HP.
- **Dice expressions with malformed input** (e.g. `2d`, `1d20 +`, `abc`) — must fail loudly, never return 0.
- **Enemy turn when the active enemy is already dead** — turn must skip, not hang combat.
- **Network loss / reconnection mid-combat** — client must resync full state, not diverge.

---

### Task 0: Repository skeleton, docs, git

**Files:**
- Create: `.gitignore`, `README.md`, `AGENTS.md`, `CHANGELOG.md`
- Create: `docs/architecture.md`, `docs/game-engine.md`, `docs/ai-dm.md`, `docs/database.md`, `docs/decisions.md`, `docs/roadmap.md`

- [ ] Write `.gitignore` (Java `target/`, Node `node_modules/`, `dist/`, `*.db`, `.env`).
- [ ] Write README (what it is, how to run backend+frontend, env vars, demo credentials).
- [ ] Write AGENTS.md (conventions, commands, where rules/AI live).
- [ ] Write docs listed above.
- [ ] `git add -A && git commit -m "docs: bootstrap project documentation"`.

### Task 1: Backend scaffold + SQLite + Flyway

**Files:**
- Create: `backend/pom.xml`, `backend/src/main/java/com/dndmasters/DndMastersApplication.java`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/resources/db/migration/V1__initial_schema.sql`
- Test: `backend/src/test/java/com/dndmasters/DndMastersApplicationTests.java`

**Interfaces:** Produces a bootable app on :8080 with `/api/health`.

- [ ] `pom.xml` with deps: spring-boot-starter-web, data-jpa, security, validation, websocket, flyway-core, sqlite-jdbc, hibernate-community-dialects, jjwt, lombok, spring-boot-starter-test.
- [ ] `application.yml`: datasource `jdbc:sqlite:./data/dnd.db`, dialect `org.hibernate.community.dialect.SQLiteDialect`, `ddl-auto: validate`, Flyway enabled.
- [ ] `V1__initial_schema.sql`: users, characters, campaigns, campaign_members, scenes, maps, board_entities, npc_templates, enemy_templates, items, game_events, dice_rolls, combat_states, combat_participants.
- [ ] Create `HealthController` returning `{"status":"ok"}`.
- [ ] Test: context loads. Run `mvn -q test`. Commit `feat: scaffold backend with sqlite and flyway`.

### Task 2: Auth — users, BCrypt, JWT

**Files:**
- Create: `security/JwtService.java`, `security/JwtAuthFilter.java`, `security/SecurityConfig.java`, `security/UserDetailsServiceImpl.java`
- Create: `domain/User.java` (entity), `persistence/UserRepository.java`
- Create: `web/AuthController.java`, `web/dto/AuthDtos.java`
- Test: `web/AuthControllerTest.java` (register, login, bad password, protected route).

**Interfaces:**
- `POST /api/auth/register {username,password,displayName}` → `{token,user}`
- `POST /api/auth/login {username,password}` → `{token,user}`
- `GET /api/auth/me` (Bearer) → user.

- [ ] Register with BCrypt hash; reject duplicate username (409).
- [ ] Login returns signed JWT (subject=userId, expiry 24h).
- [ ] Filter validates Bearer token; `/api/auth/**` and `/api/health` public, rest authenticated.
- [ ] Test the four cases. `mvn test`. Commit `feat: add jwt authentication`.

### Task 3: Dice engine (pure, TDD)

**Files:**
- Create: `engine/dice/DiceExpression.java`, `engine/dice/DiceRollResult.java`, `engine/dice/DiceEngine.java`
- Create: `engine/dice/DiceParseException.java`
- Test: `engine/dice/DiceEngineTest.java`

**Interfaces:**
- `DiceEngine.roll(String expr)` → `DiceRollResult{ expression, rolls, modifier, total, natural20, natural1 }`
- Supports `NdM`, `NdM+K`, `NdM-K`, bare constant.

- [ ] Failing tests: `1d20` in range; `1d20+5` total; `2d6+3` has 2 rolls; `1d1` = 1; critical flags; malformed (`2d`, `1d20+`, `abc`) throws `DiceParseException`.
- [ ] Implement parser + roller with injected `Random` for determinism in tests.
- [ ] `mvn test`. Commit `feat: implement dice engine`.

### Task 4: Rules engine — stats, checks, attack, damage, initiative, HP

**Files:**
- Create: `engine/rules/Ability.java`, `engine/rules/Modifier.java`, `engine/rules/Combatant.java`
- Create: `engine/rules/CheckResolver.java`, `engine/rules/AttackResolver.java`, `engine/rules/DamageResolver.java`, `engine/rules/InitiativeEngine.java`
- Test: `engine/rules/*Test.java`

**Interfaces:**
- `Modifier.of(score)`; `CheckResolver.resolve(combatant, skill, dc, advantage)` → `CheckResult{roll,total,dc,success,critical}`.
- `AttackResolver.resolve(attacker, target, attack)` → `AttackResult{hit, critical, attackRoll, total, ac}`.
- `DamageResolver.apply(attack, AttackResult)` → `DamageResult{diceTotal, modifier, total, targetHpAfter, dead}`.
- `InitiativeEngine.order(combatants)` → sorted list desc by `d20+dex`.

- [ ] TDD each: modifier table (−1 for 8/9, +0 10/11, +2 14/15); check success/fail at DC boundary; nat 20 always hits + crit doubles damage dice; nat 1 always misses; damage floors at 0 and marks dead; initiative ordering with tiebreak by DEX then id.
- [ ] `mvn test`. Commit `feat: implement rules engine`.

### Task 5: Persistence model + repositories + mappers

**Files:**
- Create: entities under `persistence/entity/` (Campaign, Character, CampaignMember, Scene, GameMap, BoardEntity, NpcTemplate, EnemyTemplate, Item, GameEvent, DiceRollLog, CombatState, CombatParticipant).
- Create repositories under `persistence/repository/`.
- Create `persistence/mapper/*Mapper.java`.
- Test: `persistence/RepositorySmokeTest.java` (save/load campaign with entities).

- [ ] JPA entities aligned with `V1__initial_schema.sql`; `ddl-auto: validate` must pass.
- [ ] Repositories extend `JpaRepository`.
- [ ] Mappers convert entity ⇄ `domain` model.
- [ ] `mvn test`. Commit `feat: add persistence model and repositories`.

### Task 6: Adventure seeder + predefined characters/enemies

**Files:**
- Create: `service/AdventureSeeder.java` (CommandLineRunner), `domain/CharacterTemplateFactory.java`
- Create: `resources/adventure/valdros.json` (scenes, maps, NPCs, enemies, quests)
- Test: `service/AdventureSeederTest.java`

**Interfaces:** seeds 4 characters, the "La Cripta de Valdros" campaign template, maps, NPC (Archivista), enemies (Goblin xN, Goblin Archer, Valdros Guardian), items.

- [ ] Character factory with exact stats from the spec (Kael AC16 HP28; Lyra; Aria; Thorin) and attacks/inventory.
- [ ] Load `valdros.json` and persist templates.
- [ ] Test counts + idempotency (running twice does not duplicate). `mvn test`. Commit `feat: seed valdros one-shot content`.

### Task 7: Game state + engine services (combat, turns, movement, events)

**Files:**
- Create: `engine/combat/CombatEngine.java`, `engine/combat/TurnManager.java`, `engine/board/MovementValidator.java`, `engine/board/GridMath.java`
- Create: `service/GameStateService.java`, `service/GameEventService.java`, `service/ActionProcessor.java`
- Test: `engine/combat/CombatEngineTest.java`, `engine/board/MovementValidatorTest.java`, `service/ActionProcessorTest.java`

**Interfaces:**
- `MovementValidator.validate(board, entity, path, occupied)` → `MoveResult{ok, reason, cost}`.
- `CombatEngine.startInitiative(campaignId)`, `nextTurn(campaignId)`, `endCombat(campaignId)`.
- `ActionProcessor.process(campaignId, PlayerAction)` → `ActionResult{narration?,events,updatedState}`; `PlayerAction` types: MOVE, ATTACK, CHECK, END_TURN, USE_ITEM, DIALOGUE.

- [ ] Tests: movement blocked by wall/obstacle/occupied → `ok=false` with reason; valid path applies; attacking out of turn rejected; killing last enemy ends combat; enemy turn skips dead enemies.
- [ ] Every mutation logs a `GameEvent` and broadcasts via `GameEventService`.
- [ ] `mvn test`. Commit `feat: implement combat, turns and movement`.

### Task 8: AI layer — AIProvider, DM context, structured output, validator

**Files:**
- Create: `ai/AIProvider.java`, `ai/MockAIProvider.java`, `ai/OpenAiCompatibleProvider.java`, `ai/AiConfig.java`
- Create: `ai/dm/DmContext.java`, `ai/dm/DmResponse.java`, `ai/dm/DungeonMasterService.java`, `ai/dm/DmActionValidator.java`
- Test: `ai/dm/DungeonMasterServiceTest.java`, `ai/dm/DmActionValidatorTest.java`

**Interfaces:**
- `AIProvider.complete(AiRequest)` → `AiResponse`.
- `DungeonMasterService.respond(DmContext)` → `DmResponse{narration, actions[], sceneChanged, imagePrompt}`.
- Actions: `NARRATE, REQUEST_CHECK, START_COMBAT, SPAWN_ENEMY, SHIFT_SCENE, GRANT_REWARD, END_ADVENTURE`.
- `DmActionValidator` rejects actions referencing unknown entities or invalid DCs (1..30).

- [ ] Mock provider returns scripted, state-aware responses keyed by scene/phase so the whole one-shot plays without an API.
- [ ] Config selects provider from `AI_PROVIDER` env (default `mock`).
- [ ] Test: mock DM at intro requests no invalid action; validator rejects unknown entity id; env override selects stub.
- [ ] `mvn test`. Commit `feat: add decoupled ai dungeon master`.

### Task 9: Game REST API + WebSocket

**Files:**
- Create: `web/CampaignController.java`, `web/GameController.java`, `web/DiceController.java`, `web/CharacterController.java`, `web/dto/GameDtos.java`
- Create: `web/ws/WebSocketConfig.java`, `web/ws/GameEventBroadcaster.java` (@SendTo `/topic/campaign/{id}`)
- Test: `web/GameControllerTest.java` (MockMvc), `web/ws/GameEventBroadcasterTest.java`

**Interfaces (key):**
- `GET /api/campaigns`, `POST /api/campaigns`, `POST /api/campaigns/{id}/join`, `POST /api/campaigns/{id}/start`
- `GET /api/campaigns/{id}/state`
- `POST /api/campaigns/{id}/actions` (PlayerAction) → ActionResult
- `POST /api/campaigns/{id}/roll` → dice result
- `GET /api/characters`, `POST /api/characters`

- [ ] STOMP endpoint `/ws`, topic `/topic/campaign/{id}`.
- [ ] GameEventService publishes to broadcaster.
- [ ] DTOs keep domain internal (no JPA entities in responses).
- [ ] `mvn test`. Commit `feat: expose game rest api and websocket`.

### Task 10: Image provider + placeholder art

**Files:**
- Create: `image/ImageProvider.java`, `image/PlaceholderImageProvider.java`, `image/ImageConfig.java`, static placeholders in `resources/static/images/`.
- Test: `image/PlaceholderImageProviderTest.java`

- [ ] Real provider stub selected by `IMAGE_PROVIDER`; default returns local placeholder path for a prompt hash.
- [ ] Seeded campaigns/scenes reference placeholder images.
- [ ] `mvn test`. Commit `feat: add image provider abstraction`.

### Task 11: Frontend scaffold + design system

**Files:**
- Create Vite app in `frontend/`; `tailwind.config.ts`, `src/index.css` tokens, `src/lib/api.ts`, `src/lib/ws.ts`, `src/store/useGameStore.ts`, router, layout.
- Create shadcn-style primitives: `components/ui/{button,card,badge,tabs,progress,tooltip,dialog}.tsx`.
- Test: `src/test/setup.ts`, `src/App.test.tsx`.

- [ ] `pnpm create vite frontend --template react-ts`; add Tailwind, shadcn deps, TanStack Query, Zustand, R3F/drei, Framer Motion, Vitest.
- [ ] Design tokens for the exact palette; Cinzel + Inter via Google Fonts.
- [ ] Router: `/`, `/login`, `/campaign/:id/lobby`, `/campaign/:id/play`.
- [ ] `pnpm build` + `pnpm test`. Commit `feat: scaffold frontend and design system`.

### Task 12: Home + Login + Lobby

**Files:**
- Create: `src/pages/Home.tsx`, `src/pages/Login.tsx`, `src/pages/Lobby.tsx`
- Create: `src/components/{CampaignCard,CharacterCard,PlayerSlot}.tsx`
- Test: `Home.test.tsx`, `Lobby.test.tsx`

- [ ] Home = "sala de aventuras": campaign cards (image, name, players, progress, setting), character list, create/enter.
- [ ] Login registers/logs in, stores JWT in Zustand + localStorage.
- [ ] Lobby = tavern mood (warm light, subtle particles/fire with Framer Motion), shows DM, players, characters, START.
- [ ] `pnpm test`. Commit `feat: add home, login and lobby`.

### Task 13: Game VTT — board, initiative, character panel

**Files:**
- Create: `src/pages/Game.tsx`
- Create: `src/components/board/{Board,Grid,Token}.tsx` (SVG, pan/zoom)
- Create: `src/components/{InitiativeTracker,CharacterSheet,TopBar}.tsx`
- Test: `Board.test.tsx`, `InitiativeTracker.test.tsx`

- [ ] SVG board: grid, walls, doors, obstacles, tokens (players/enemies/NPC/items), selection, movement highlight, zoom/pan.
- [ ] Initiative tracker highlights the active turn; enemies show HP bars.
- [ ] Character sheet card with stats/attacks/inventory per spec.
- [ ] `pnpm test`. Commit `feat: add game board and initiative`.

### Task 14: DM panel + 3D dice + actions/animations

**Files:**
- Create: `src/components/dm/{DmPanel,NarrationBlock,CheckPrompt}.tsx`
- Create: `src/components/dice/{DiceTray,Dice3D}.tsx` (R3F), `useDiceRoll.ts`
- Create: `src/components/actions/ActionBar.tsx`
- Test: `DmPanel.test.tsx`, `DiceTray.test.tsx`, `ActionBar.test.tsx`

- [ ] DM panel: avatar, narration, events, roll/check prompts, "The Dungeon Master is weaving the story…" loading state.
- [ ] R3F die: appears, rolls with physics-ish animation, stops, shows face + result; special styling for crit/crit-fail.
- [ ] Attack flow: select target → highlight → roll → damage → HP animation; damage shake/flash; death fade.
- [ ] Turn highlight + scene transitions with Framer Motion.
- [ ] `pnpm test` + `pnpm build`. Commit `feat: add dungeon master panel and 3d dice`.

### Task 15: Integration, polish, docs

**Files:**
- Modify: README, docs/*, CHANGELOG.
- Create: `docs/walkthrough.md` (end-to-end demo script).

- [ ] Play the full one-shot manually against the Mock DM; fix blockers.
- [ ] Add reconnection resync (fetch full state on socket reconnect) — covers Review Focus item.
- [ ] Run backend `mvn test` and frontend `pnpm test`/`pnpm build`; update docs; final commit.

---

## Self-Review

- **Spec coverage:** auth(JWT)→T2; dice→T3; rules→T4; persistence→T5; one-shot content→T6; combat/turns/movement→T7; AI DM→T8; REST/WS→T9; images→T10; design system→T11; Home/Lobby→T12; board/initiative→T13; DM panel/3D dice/animations→T14; tests/docs→T0,T15. All spec sections covered. Friends/map editor explicitly roadmap (T0 docs).
- **Placeholders:** tasks name concrete files, interfaces and tests; implementation details are resolved during execution per the spec.
- **Type consistency:** `DiceRollResult`, `AttackResult`, `ActionResult`, `DmResponse`, `PlayerAction` names are used consistently across T3–T14.
- **Review Focus:** wall/occupied movement + out-of-turn attack + dead-enemy skip are tested in T7; malformed dice in T3; reconnect resync in T15.
