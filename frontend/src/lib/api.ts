import type {
  AuthResponse,
  Campaign,
  CampaignIntro,
  CreateCharacterPayload,
  GameEvent,
  GameState,
  PlayerActionPayload,
  PlayerCharacter,
  ActionResult,
  RulesetView,
  ShopView,
  User,
} from "@/types/game";

const TOKEN_KEY = "dnd-masters-token";
const USER_KEY = "dnd-masters-user";

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setSession(auth: AuthResponse) {
  localStorage.setItem(TOKEN_KEY, auth.token);
  localStorage.setItem(USER_KEY, JSON.stringify(auth.user));
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function getStoredUser(): User | null {
  const raw = localStorage.getItem(USER_KEY);
  return raw ? (JSON.parse(raw) as User) : null;
}

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(init.headers as Record<string, string> | undefined),
  };
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }
  const response = await fetch(path, { ...init, headers });
  const text = await response.text();
  const body = text ? JSON.parse(text) : null;
  if (!response.ok) {
    throw new ApiError(response.status, body?.error ?? "Error de red");
  }
  return body as T;
}

export const api = {
  register: (username: string, password: string, displayName?: string) =>
    request<AuthResponse>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ username, password, displayName }),
    }),

  login: (username: string, password: string) =>
    request<AuthResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ username, password }),
    }),

  me: () => request<User>("/api/auth/me"),

  campaigns: () => request<Campaign[]>("/api/campaigns"),

  campaign: (id: number) => request<Campaign>(`/api/campaigns/${id}`),

  createCampaign: (name: string, edition?: string) =>
    request<Campaign>("/api/campaigns", { method: "POST", body: JSON.stringify({ name, edition }) }),

  joinCampaign: (id: number, characterId?: number) =>
    request<Campaign>(`/api/campaigns/${id}/join`, {
      method: "POST",
      body: JSON.stringify({ characterId: characterId ?? null }),
    }),

  startCampaign: (id: number) =>
    request<ActionResult>(`/api/campaigns/${id}/start`, { method: "POST" }),

  state: (id: number) => request<GameState>(`/api/campaigns/${id}/state`),

  events: (id: number) => request<GameEvent[]>(`/api/campaigns/${id}/events`),

  act: (id: number, action: PlayerActionPayload) =>
    request<ActionResult>(`/api/campaigns/${id}/actions`, {
      method: "POST",
      body: JSON.stringify(action),
    }),

  characters: () => request<PlayerCharacter[]>("/api/characters"),

  createCharacter: (payload: CreateCharacterPayload) =>
    request<PlayerCharacter>("/api/characters", { method: "POST", body: JSON.stringify(payload) }),

  rulesets: () => request<RulesetView[]>("/api/rulesets"),

  ruleset: (edition: string) => request<RulesetView>(`/api/rulesets/${edition}`),

  intro: (id: number) => request<CampaignIntro>(`/api/campaigns/${id}/intro`),

  shop: (id: number) => request<ShopView>(`/api/campaigns/${id}/shop`),

  buy: (id: number, itemId: string, quantity = 1) =>
    request<PlayerCharacter>(`/api/campaigns/${id}/shop/buy`, {
      method: "POST",
      body: JSON.stringify({ itemId, quantity }),
    }),

  sell: (id: number, itemName: string, quantity = 1) =>
    request<PlayerCharacter>(`/api/campaigns/${id}/shop/sell`, {
      method: "POST",
      body: JSON.stringify({ itemName, quantity }),
    }),

  equip: (id: number, itemName: string) =>
    request<PlayerCharacter>(`/api/campaigns/${id}/shop/equip`, {
      method: "POST",
      body: JSON.stringify({ itemName }),
    }),

  unequip: (id: number, slot: string) =>
    request<PlayerCharacter>(`/api/campaigns/${id}/shop/unequip`, {
      method: "POST",
      body: JSON.stringify({ slot }),
    }),
};
