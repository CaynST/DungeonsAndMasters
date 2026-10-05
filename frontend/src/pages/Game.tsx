import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import { ArrowLeft, Crown, Flag } from "lucide-react";
import { api } from "@/lib/api";
import { connectCampaignSocket } from "@/lib/ws";
import { CinematicIntro } from "@/components/intro/CinematicIntro";
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
    }, () => {
      // Reconnection: resync full state to avoid divergence.
      queryClient.invalidateQueries({ queryKey: ["state", campaignId] });
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
      if (result.narration) setNarration(result.narration);
      if (result.checkRequest) setPendingCheck(result.checkRequest);

      if (result.roll) {
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

  const onCellClick = useCallback(
    (cell: Cell) => {
      if (!canAct || !myEntity || mode !== "move") return;
      act.mutate({ type: "MOVE", actorEntityId: myEntity.id, path: [cell] });
    },
    [act, canAct, mode, myEntity],
  );

  const onEntityClick = useCallback(
    (entity: { id: number; kind: string }) => {
      selectEntity(entity.id);
      if (mode === "attack" && entity.kind === "ENEMY") {
        selectEntity(entity.id);
      }
    },
    [mode, selectEntity],
  );

  const confirmAttack = useCallback(() => {
    if (!myEntity || !selectedEntity || !canAct) return;
    act.mutate({
      type: "ATTACK",
      actorEntityId: myEntity.id,
      targetEntityId: selectedEntity.id,
      attack: selectedAttack?.name,
    });
  }, [act, canAct, myEntity, selectedAttack?.name, selectedEntity]);

  const reachablePaths = useMemo(() => new Map<string, Cell[]>(), []);

  if (state.isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center text-muted">
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

          {/* Initiative + party */}
          <div className="grid gap-3 lg:grid-cols-2">
            {gameState.combat?.active && (
              <InitiativeTracker combat={gameState.combat} entities={gameState.entities} />
            )}
            {myEntity && gameState.party.find((c) => c.id === myEntity.refId) && (
              <CharacterSheet
                character={gameState.party.find((c) => c.id === myEntity.refId)!}
                compact={gameState.combat?.active}
              />
            )}
          </div>
        </div>

        {/* DM panel */}
        <div className="min-h-0">
          <DmPanel
            dmName={gameState.campaign.dmName}
            narration={narration}
            events={gameState.recentEvents}
            providerId={provider.data?.aiProvider}
            onRollCheck={rollCheck}
          />
        </div>
      </div>

      {/* Action bar */}
      <div className="px-3 pb-3">
        <ActionBar
          character={gameState.party.find((c) => c.id === myEntity?.refId)}
          canAct={canAct}
          isMyTurn={isMyTurn}
          selectedTargetName={mode === "attack" ? selectedEntity?.name : undefined}
          selectedAttack={selectedAttack}
          onSelectAttack={setSelectedAttack}
          onAttack={confirmAttack}
          onEndTurn={() => act.mutate({ type: "END_TURN", actorEntityId: myEntity?.id })}
          onDialogue={() => act.mutate({ type: "DIALOGUE", text: "El grupo interactúa con la escena." })}
          onQuickRoll={(expression) => act.mutate({ type: "DICE", expression })}
          onDiceRequest={() => act.mutate({ type: "DICE", expression: "1d20" })}
          mode={mode}
          onToggleMode={setMode}
        />
      </div>

      {/* Dice overlay */}
      {diceRequest && <DiceTray roll={diceRequest.result} onComplete={clearDice} />}

      {/* Opening cinematic */}
      {showIntro && intro.data && <CinematicIntro intro={intro.data} onDone={dismissIntro} />}

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
