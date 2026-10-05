export interface Stats {
  strength: number;
  dexterity: number;
  constitution: number;
  intelligence: number;
  wisdom: number;
  charisma: number;
}

export interface Attack {
  name: string;
  attackBonus: number;
  damageExpression: string;
  range: number;
  ranged: boolean;
}

export interface InventoryItem {
  name: string;
  description?: string;
  icon?: string;
  quantity: number;
}

export interface PlayerCharacter {
  id: number;
  ownerId?: number;
  name: string;
  cls: string;
  race: string;
  level: number;
  hp: number;
  maxHp: number;
  ac: number;
  stats: Stats;
  speed: number;
  color?: string;
  icon?: string;
  portrait?: string;
  attacks: Attack[];
  inventory: InventoryItem[];
  predefined: boolean;
  creationData?: Record<string, unknown>;
  gold?: number;
  silver?: number;
  copper?: number;
}

export type EntityKind = "PLAYER" | "ENEMY" | "NPC" | "ITEM";

export interface GameEntity {
  id: number;
  sceneId: number;
  mapId: number;
  kind: EntityKind;
  refId?: number;
  name: string;
  x: number;
  y: number;
  hp: number;
  maxHp: number;
  ac: number;
  speed: number;
  color?: string;
  icon?: string;
  portrait?: string;
  alive: boolean;
  data: Record<string, unknown>;
}

export interface Cell {
  x: number;
  y: number;
}

export interface Door extends Cell {
  open: boolean;
}

export interface Decor extends Cell {
  icon: string;
  kind: string;
}

export interface GameMap {
  id: number;
  name: string;
  width: number;
  height: number;
  tileSize: number;
  theme?: string;
  walls: Cell[];
  doors: Door[];
  obstacles: Cell[];
  decor: Decor[];
}

export interface Scene {
  id: number;
  campaignId: number;
  orderIndex: number;
  title: string;
  description: string;
  kind: string;
  imagePrompt?: string;
  imageUrl?: string;
  mapId?: number;
}

export interface CombatParticipant {
  id?: number;
  entityId: number;
  name: string;
  kind: EntityKind;
  initiative: number;
  orderIndex: number;
  alive: boolean;
}

export interface CombatState {
  id?: number;
  campaignId: number;
  sceneId?: number;
  active: boolean;
  round: number;
  currentTurnIndex: number;
  status: string;
  participants: CombatParticipant[];
}

export type CampaignStatus = "LOBBY" | "ACTIVE" | "FINISHED";
export type GamePhase = "INTRO" | "EXPLORATION" | "SOCIAL" | "COMBAT" | "BOSS" | "COMPLETE";

export interface CampaignMember {
  id: number;
  userId: number;
  displayName: string;
  characterId?: number;
  characterName?: string;
  role: string;
  ready: boolean;
}

export interface Campaign {
  id: number;
  name: string;
  setting?: string;
  description?: string;
  dmName: string;
  dmAvatar?: string;
  image?: string;
  status: CampaignStatus;
  phase: GamePhase;
  activeSceneId?: number;
  createdBy?: number;
  edition?: string;
  premise?: string;
  villain?: string;
  objective?: string;
  spotlightIndex?: number;
  shopOpen?: boolean;
  members: CampaignMember[];
}

export interface GameEvent {
  id: number;
  campaignId: number;
  sceneId?: number;
  sequence: number;
  type: string;
  actor?: string;
  message: string;
  dataJson: string;
  createdAt: string;
}

export interface GameState {
  campaign: Campaign;
  scene: Scene | null;
  map: GameMap | null;
  entities: GameEntity[];
  party: PlayerCharacter[];
  combat: CombatState | null;
  recentEvents: GameEvent[];
  objective?: string;
}

export interface CheckRequest {
  skill: string;
  dc: number;
  actorEntityId?: number;
  prompt?: string;
}

export interface DiceRoll {
  expression: string;
  rolls: number[];
  modifier: number;
  total: number;
  natural20: boolean;
  natural1: boolean;
}

export interface AttackOutcome {
  attackerId: number;
  attackerName: string;
  targetId: number;
  targetName: string;
  attack: string;
  roll: number;
  total: number;
  ac: number;
  hit: boolean;
  critical: boolean;
  fumble: boolean;
  damage?: number;
  damageRolls?: number[];
  hpAfter?: number;
  dead?: boolean;
}

export interface ActionResult {
  ok: boolean;
  message?: string;
  narration?: string;
  events: GameEvent[];
  checkRequest?: CheckRequest | null;
  roll?: DiceRoll | null;
  attack?: AttackOutcome | null;
  dmActions: string[];
  adventureEnded: boolean;
}

export interface User {
  id: number;
  username: string;
  displayName: string;
  avatar?: string;
}

// ---- Character creation (SRD) ----

export interface SpeciesView {
  id: string;
  name: string;
  speed: number;
  size: string;
  abilityBonuses: Record<string, number>;
  darkvision: boolean;
  description: string;
}

export interface ClassView {
  id: string;
  name: string;
  hitDie: string;
  skillChoiceCount: number;
  skillChoices: string[];
  savingThrows: string[];
  startingArmor: string;
  weapons: string[];
  primaryAbility: string;
  description: string;
  subclasses: string[];
}

export interface BackgroundView {
  id: string;
  name: string;
  skills: string[];
  abilityBonuses: Record<string, number>;
  description: string;
}

export interface RulesetView {
  edition: string;
  displayName: string;
  species: SpeciesView[];
  classes: ClassView[];
  backgrounds: BackgroundView[];
}

export interface AbilityScores {
  strength: number;
  dexterity: number;
  constitution: number;
  intelligence: number;
  wisdom: number;
  charisma: number;
}

export interface CreateCharacterPayload {
  name: string;
  edition: string;
  speciesId: string;
  classId: string;
  subclass?: string;
  backgroundId: string;
  scores: AbilityScores;
  skills: string[];
  armor?: string;
  weapon?: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface ShopItem {
  id: string;
  name: string;
  kind: string;
  price: number;
  icon: string;
  description: string;
}

export interface ShopView {
  characterId: number;
  gold: number;
  silver: number;
  copper: number;
  items: ShopItem[];
  inventory: InventoryItem[];
}

export interface CampaignIntro {
  title: string;
  setting?: string;
  villain?: string;
  objective?: string;
  premise: string[];
  party: { name: string; race: string; cls: string; color?: string; portrait?: string }[];
  spotlight?: string;
}

export interface PlayerActionPayload {
  type: string;
  actorEntityId?: number;
  targetEntityId?: number;
  path?: Cell[];
  attack?: string;
  skill?: string;
  dc?: number;
  expression?: string;
  text?: string;
  item?: string;
}
