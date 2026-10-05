import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import { ArrowLeft, Crown, Flag } from "lucide-react";
import { api } from "@/lib/api";
import { connectCampaignSocket } from "@/lib/ws";
import { findPath, reachableCells } from "@/lib/pathfinding";
import { CinematicIntro } from "@/components/intro/CinematicIntro";
import { ShopModal } from "@/components/shop/ShopModal";
import { Chronicle } from "@/components/Chronicle";
import { Spinner } from "@/components/ui/Spinner";
import { useGameStore } from "@/store/useGameStore";
import { Board } from "@/components/board/Board";
import { InitiativeTracker } from "@/components/InitiativeTracker";
import { CharacterSheet } from "@/components/CharacterSheet";
import { DmPanel } from "@/components/dm/DmPanel";
import { DiceTray } from "@/components/dice/DiceTray";
import { ActionBar } from "@/components/actions/ActionBar";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import type { Attack, Cell, GameEvent, PlayerActionPayload } from "@/types/game";

export function Game() {
  const { id } = useParams();
  const campaignId = Number(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const state = useQuery({
    queryKey: ["state", campaignId],
    queryFn: () => api.state(campaignId),
    refetchInterval: 4000,
  });

  const {
    selectedEntityId,
    selectEntity,
    pendingCheck,
    setPendingCheck,
    diceRequest,
    requestDice,
    clearDice,
    setDmThinking,
    setActiveEntity,
    addFloatingText,
    setLastAttack,
  } = useGameStore();

  const [mode, setMode] = useState<"move" | "attack">("move");
  const [selectedAttack, setSelectedAttack] = useState<Attack | undefined>();
  const [narration, setNarration] = useState<string | null>(null);
  const [started, setStarted] = useState(false);
  const [showShop, setShowShop] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const [pendingAttack, setPendingAttack] = useState<{ targetId: number; targetName: string } | null>(null);
  const [tab, setTab] = useState<"dm" | "party" | "chronicle">("dm");
  const [showIntro, setShowIntro] = useState(() => {
    const key = `intro-played-${campaignId}`;
    return !sessionStorage.getItem(key);
  });

  const intro = useQuery({
    queryKey: ["intro", campaignId],
    queryFn: () => api.intro(campaignId),
    enabled: showIntro,
  });

  const dismissIntro = useCallback(() => {
    sessionStorage.setItem(`intro-played-${campaignId}`, "1");
    setShowIntro(false);
  }, [campaignId]);

  const gameState = state.data;
  const provider = useQuery({
    queryKey: ["provider", campaignId],
    queryFn: async () => {
      const response = await fetch(`/api/campaigns/${campaignId}/provider`);
      return (await response.json()) as { aiProvider: string };
    },
  });

  // Auto-start the adventure once, so the DM gives the intro narration.
  useEffect(() => {
    if (!gameState || started || gameState.scene) return;
    setStarted(true);
    setDmThinking(true);
    api
      .startCampaign(campaignId)
      .then((result) => {
        if (result.narration) setNarration(result.narration);
        if (result.checkRequest) setPendingCheck(result.checkRequest);
        queryClient.invalidateQueries({ queryKey: ["state", campaignId] });
      })
      .finally(() => setDmThinking(false));
  }, [gameState, started, campaignId, queryClient, setPendingCheck, setDmThinking]);

  // Live updates from the WebSocket.
  useEffect(() => {
    const socket = connectCampaignSocket(campaignId, (_event: GameEvent) => {
      queryClient.invalidateQueries({ queryKey: ["state", campaignId] });
      queryClient.invalidateQueries({ queryKey: ["chronicle", campaignId] });
    }, () => {
      // Reconnection: resync full state to avoid divergence.
      queryClient.invalidateQueries({ queryKey: ["state", campaignId] });
      queryClient.invalidateQueries({ queryKey: ["chronicle", campaignId] });
    });
    return () => socket.close();
  }, [campaignId, queryClient]);

  // Track the entity whose turn it is.
  const combat = gameState?.combat ?? null;
  const currentParticipant = combat?.active ? combat.participants[combat.currentTurnIndex] : null;
  useEffect(() => {
    setActiveEntity(currentParticipant?.entityId ?? null);
  }, [currentParticipant?.entityId, setActiveEntity]);

  const myEntity = useMemo(() => {
    if (!gameState) return undefined;
    const mine = gameState.party.find((character) => character.predefined)?.id;
    return gameState.entities.find(
      (entity) => entity.kind === "PLAYER" && entity.data?.controller === "PLAYER",
    ) ?? gameState.entities.find((entity) => entity.kind === "PLAYER" && entity.refId === mine);
  }, [gameState]);

  const isMyTurn = !combat?.active || currentParticipant?.entityId === myEntity?.id;
  const canAct = Boolean(myEntity?.alive) && isMyTurn;

  const spotlightCharacter =
    !combat?.active && gameState && gameState.party.length > 0
      ? gameState.party[
          (((gameState.campaign.spotlightIndex ?? 0) % gameState.party.length) + gameState.party.length) %
            gameState.party.length
        ]
      : null;

  const selectedEntity = gameState?.entities.find((entity) => entity.id === selectedEntityId);
  const targetableIds = useMemo(() => {
    if (!gameState || mode !== "attack" || !gameState.combat?.active) return new Set<number>();
    return new Set(
      gameState.entities
        .filter((entity) => entity.kind === "ENEMY" && entity.alive)
        .map((entity) => entity.id),
    );
  }, [gameState, mode]);

  const act = useMutation({
    mutationFn: (action: PlayerActionPayload) => api.act(campaignId, action),
    onMutate: () => setDmThinking(true),
    onSuccess: (result) => {
      setDmThinking(false);
      if (!result.ok) {
        setToast(result.message ?? "No puedes hacer eso ahora mismo");
      }
      if (result.narration) setNarration(result.narration);
      if (result.checkRequest) setPendingCheck(result.checkRequest);

      if (result.roll && result.roll.expression) {
        requestDice(result.roll.expression, result.roll, undefined, () => {});
      }
      if (result.attack && result.attack.hit && result.attack.damage) {
        addFloatingText(result.attack.targetId, `-${result.attack.damage}`, "damage");
        setLastAttack(result.attack);
      }

      queryClient.invalidateQueries({ queryKey: ["state", campaignId] });
      if (result.adventureEnded) {
        setTimeout(() => queryClient.invalidateQueries({ queryKey: ["state", campaignId] }), 600);
      }
    },
    onError: () => setDmThinking(false),
  });

  const rollCheck = useCallback(
    (skill: string, dc: number) => {
      setPendingCheck(null);
      act.mutate({ type: "CHECK", skill, dc, actorEntityId: myEntity?.id });
    },
    [act, myEntity?.id, setPendingCheck],
  );

  const occupiedExcept = useCallback(
    (actorId: number) => {
      const set = new Set<string>();
      gameState?.entities.forEach((e) => {
        if (e.id !== actorId && e.alive && (e.kind === "PLAYER" || e.kind === "ENEMY")) {
          set.add(`${e.x},${e.y}`);
        }
      });
      return set;
    },
    [gameState],
  );

  const onCellClick = useCallback(
    (cell: Cell) => {
      if (!canAct || !myEntity || !gameState?.map) return;
      if (mode !== "move") {
        setToast("Estás en modo atacar. Cambia a Mover para desplazarte.");
        return;
      }
      const path = findPath(gameState.map, { x: myEntity.x, y: myEntity.y }, cell, occupiedExcept(myEntity.id));
      if (!path) {
        setToast("No puedes llegar ahí: el camino está bloqueado.");
        return;
      }
      if (path.length > myEntity.speed) {
        setToast(`No puedes moverte tan lejos: necesitas ${path.length} casillas y solo tienes ${myEntity.speed}.`);
        return;
      }
      act.mutate({ type: "MOVE", actorEntityId: myEntity.id, path });
    },
    [act, canAct, gameState, mode, myEntity, occupiedExcept],
  );

  const onEntityClick = useCallback(
    (entity: { id: number; kind: string }) => {
      selectEntity(entity.id);
    },
    [selectEntity],
  );

  const confirmAttack = useCallback(() => {
    if (!myEntity || !selectedEntity || !canAct) return;
    setPendingAttack({ targetId: selectedEntity.id, targetName: selectedEntity.name });
  }, [canAct, myEntity, selectedEntity]);

  const reachablePaths = useMemo(() => {
    if (!gameState?.map || !myEntity || !canAct || mode !== "move") {
      return new Map<string, Cell[]>();
    }
    return reachableCells(gameState.map, { x: myEntity.x, y: myEntity.y }, occupiedExcept(myEntity.id), myEntity.speed);
  }, [gameState, myEntity, canAct, mode, occupiedExcept]);

  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), 4200);
    return () => clearTimeout(timer);
  }, [toast]);

  // Derive the pending check from the event log so it survives reloads and reconnects.
  const derivedCheck = useMemo(() => {
    const events = gameState?.recentEvents ?? [];
    for (let i = events.length - 1; i >= 0; i--) {
      const event = events[i];
      if (event.type === "CHECK") return null; // already rolled
      if (event.type === "CHECK_REQUESTED") {
        try {
          const data = JSON.parse(event.dataJson) as { skill?: string; dc?: number };
          if (!data.skill || data.dc == null) return null;
          return { skill: data.skill, dc: data.dc, prompt: `Prueba de ${data.skill} (CD ${data.dc})` };
        } catch {
          return null;
        }
      }
    }
    return null;
  }, [gameState?.recentEvents]);

  const checkPending = derivedCheck ?? pendingCheck;
  const actionBusy = act.isPending;

  if (state.isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center gap-3 text-muted">
        <Spinner />
        <motion.p animate={{ opacity: [0.4, 1, 0.4] }} transition={{ duration: 1.6, repeat: Infinity }}>
          Despertando a la compañía...
        </motion.p>
      </div>
    );
  }

  if (!gameState) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <Button onClick={() => navigate("/")}>Volver</Button>
      </div>
    );
  }

  const finished = gameState.campaign.status === "FINISHED";

  return (
    <div className="flex h-screen flex-col">
      {/* Top bar */}
      <header className="flex items-center justify-between border-b border-parchment/10 bg-surface/70 px-4 py-2 backdrop-blur">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="sm" onClick={() => navigate("/")}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="title-display text-lg font-semibold text-parchment">{gameState.campaign.name}</h1>
            <p className="text-[11px] text-muted">{gameState.scene?.title ?? "Preparando..."}</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          {gameState.combat?.active && <Badge tone="crimson">Ronda {gameState.combat.round}</Badge>}
          <Badge tone={finished ? "gold" : "emerald"}>
            {finished ? "Aventura completada" : gameState.campaign.phase}
          </Badge>
        </div>
      </header>

      {/* Main grid */}
      <div className="grid min-h-0 flex-1 grid-cols-1 gap-3 p-3 lg:grid-cols-[minmax(0,1fr)_380px]">
        <div className="flex min-h-0 flex-col gap-3">
          {/* Scene banner */}
          <div className="panel flex items-center justify-between gap-3 p-3">
            <div className="flex items-center gap-2">
              <Flag className="h-4 w-4 text-gold" />
              <span className="text-sm text-muted">
                <span className="text-parchment">{gameState.objective}</span>
              </span>
            </div>
            {gameState.scene?.description && (
              <p className="hidden max-w-xl truncate text-xs italic text-muted xl:block">
                {gameState.scene.description}
              </p>
            )}
          </div>

          {/* Narrative spotlight: whose decision it is out of combat */}
          {spotlightCharacter && (
            <div className="panel-raised flex items-center justify-between gap-3 px-4 py-2.5">
              <div className="flex items-center gap-3">
                <span
                  className="title-display flex h-8 w-8 items-center justify-center rounded-full border text-sm font-bold"
                  style={{
                    borderColor: spotlightCharacter.color ?? "#C9A227",
                    color: spotlightCharacter.color ?? "#C9A227",
                  }}
                >
                  {spotlightCharacter.name.charAt(0)}
                </span>
                <span className="text-sm text-muted">
                  Es el turno de <span className="font-semibold text-ink">{spotlightCharacter.name}</span>. ¿Qué hace?
                </span>
              </div>
              <Button variant="secondary" size="sm" onClick={() => act.mutate({ type: "PASS" })}>
                Pasar turno
              </Button>
            </div>
          )}

          {/* Board */}
          <div className="panel relative min-h-0 flex-1 overflow-hidden">
            <AnimatePresence mode="wait">
              <motion.div
                key={gameState.scene?.id ?? "none"}
                initial={{ opacity: 0, scale: 1.03 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.98 }}
                transition={{ duration: 0.5 }}
                className="absolute inset-0"
              >
                {gameState.map ? (
                  <Board
                    map={gameState.map}
                    entities={gameState.entities}
                    activeEntityId={currentParticipant?.entityId ?? null}
                    reachablePaths={reachablePaths}
                    selectedEntityId={selectedEntityId}
                    targetableEntityIds={targetableIds}
                    onCellClick={onCellClick}
                    onEntityClick={onEntityClick}
                  />
                ) : (
                  <div className="flex h-full items-center justify-center text-muted">Sin tablero.</div>
                )}
              </motion.div>
            </AnimatePresence>
          </div>
        </div>

        {/* Right sidebar with tabs */}
        <div className="flex min-h-0 flex-col gap-2">
          <div className="flex gap-1 rounded-lg border border-[#2b2118] bg-[#100d0a] p-1">
            {(
              [
                ["dm", "Dungeon Master"],
                ["party", "Grupo"],
                ["chronicle", "Crónica"],
              ] as const
            ).map(([id, label]) => (
              <button
                key={id}
                onClick={() => setTab(id)}
                className={`flex-1 rounded-md px-2 py-1.5 text-xs font-medium transition ${
                  tab === id ? "bg-[#c9a227]/15 text-[#e0b84a]" : "text-muted hover:text-ink"
                }`}
              >
                {label}
              </button>
            ))}
          </div>

          <div className="min-h-0 flex-1 overflow-hidden">
            {tab === "dm" && (
              <DmPanel
                dmName={gameState.campaign.dmName}
                narration={narration}
                events={gameState.recentEvents}
                providerId={provider.data?.aiProvider}
                spotlightName={spotlightCharacter?.name}
                busy={act.isPending}
                pendingCheck={checkPending}
                onRollCheck={rollCheck}
                onSay={(text) => act.mutate({ type: "DIALOGUE", text, actorEntityId: myEntity?.id })}
              />
            )}

            {tab === "party" && (
              <div className="h-full space-y-3 overflow-y-auto pr-1">
                {gameState.combat?.active && (
                  <InitiativeTracker combat={gameState.combat} entities={gameState.entities} />
                )}
                {gameState.party.length === 0 && (
                  <p className="panel p-4 text-sm text-muted">Aún no hay personajes en la partida.</p>
                )}
                {gameState.party.map((character) => (
                  <CharacterSheet key={character.id} character={character} />
                ))}
              </div>
            )}

            {tab === "chronicle" && <Chronicle campaignId={campaignId} />}
          </div>
        </div>
      </div>

      {/* Action bar */}
      <div className="px-3 pb-3">
        <ActionBar
          character={gameState.party.find((c) => c.id === myEntity?.refId)}
          canAct={canAct}
          isMyTurn={isMyTurn}
          busy={actionBusy}
          checkPending={Boolean(checkPending)}
          selectedTargetName={mode === "attack" ? selectedEntity?.name : undefined}
          selectedAttack={selectedAttack}
          onSelectAttack={setSelectedAttack}
          onAttack={confirmAttack}
          onEndTurn={() => act.mutate({ type: "END_TURN", actorEntityId: myEntity?.id })}
          onDialogue={() => act.mutate({ type: "DIALOGUE", text: "El grupo interactúa con la escena." })}
          onQuickRoll={(expression) => act.mutate({ type: "DICE", expression })}
          onDiceRequest={() => act.mutate({ type: "DICE", expression: "1d20" })}
          onOpenShop={() => setShowShop(true)}
          shopOpen={gameState.campaign.shopOpen}
          mode={mode}
          onToggleMode={setMode}
        />
      </div>

      {/* Dice overlay */}
      {diceRequest && <DiceTray roll={diceRequest.result} onComplete={clearDice} />}

      {/* Opening cinematic */}
      {showIntro && intro.data && <CinematicIntro intro={intro.data} onDone={dismissIntro} />}

      {/* Shop */}
      {showShop && <ShopModal campaignId={campaignId} onClose={() => setShowShop(false)} />}

      {/* Attack confirmation */}
      <AnimatePresence>
        {pendingAttack && myEntity && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="absolute inset-0 z-40 flex items-center justify-center bg-black/70 p-4"
            onClick={() => setPendingAttack(null)}
          >
            <motion.div
              initial={{ scale: 0.94, y: 10 }}
              animate={{ scale: 1, y: 0 }}
              className="panel-raised w-[min(92vw,420px)] p-5"
              onClick={(event) => event.stopPropagation()}
            >
              <h3 className="title-display text-lg font-semibold text-ink">Confirmar ataque</h3>
              <p className="mt-3 text-sm text-muted">
                <span className="text-ink">{myEntity.name}</span> ataca a{" "}
                <span className="text-[#e06a7a]">{pendingAttack.targetName}</span>
              </p>
              <div className="mt-3 space-y-1.5 rounded-md border border-[#2b2118] bg-[#100d0a] p-3 text-sm">
                <div className="flex justify-between">
                  <span className="text-muted">Arma</span>
                  <span className="text-ink">{selectedAttack?.name ?? "Golpe"}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted">Bonificador</span>
                  <span className="text-ink">
                    {selectedAttack ? (selectedAttack.attackBonus >= 0 ? "+" : "") + selectedAttack.attackBonus : "+2"}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted">Daño</span>
                  <span className="text-ink">{selectedAttack?.damageExpression ?? "1d4"}</span>
                </div>
              </div>
              <div className="mt-4 flex justify-end gap-2">
                <Button variant="ghost" onClick={() => setPendingAttack(null)}>
                  Cancelar
                </Button>
                <Button
                  variant="danger"
                  onClick={() => {
                    act.mutate({
                      type: "ATTACK",
                      actorEntityId: myEntity.id,
                      targetEntityId: pendingAttack.targetId,
                      attack: selectedAttack?.name,
                    });
                    setPendingAttack(null);
                  }}
                >
                  Atacar
                </Button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Toast */}
      <AnimatePresence>
        {toast && (
          <motion.div
            initial={{ opacity: 0, y: -12 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -12 }}
            className="absolute left-1/2 top-16 z-50 -translate-x-1/2 rounded-lg border border-[#d0566a]/50 bg-[#2a1218] px-4 py-2 text-sm text-[#e8b7c0] shadow-lg"
          >
            {toast}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Completion overlay */}
      <AnimatePresence>
        {finished && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="absolute inset-0 z-50 flex items-center justify-center bg-black/80"
          >
            <motion.div
              initial={{ scale: 0.9, y: 20 }}
              animate={{ scale: 1, y: 0 }}
              className="panel max-w-lg p-8 text-center"
            >
              <Crown className="mx-auto mb-3 h-10 w-10 text-gold" />
              <h2 className="title-display text-3xl font-bold text-parchment text-glow-gold">
                ¡Aventura Completada!
              </h2>
              <p className="mt-3 text-sm text-muted">
                Habéis recuperado el Relicario de Valdros y sellado la cripta. La compañía vivirá para contarlo.
              </p>
              <div className="mt-6 flex justify-center gap-3">
                <Button variant="primary" onClick={() => navigate("/")}>
                  Volver a la sala
                </Button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
