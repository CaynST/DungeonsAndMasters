-- La Cripta de Valdros — initial schema (SQLite)

CREATE TABLE users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    display_name  TEXT NOT NULL,
    avatar        TEXT,
    created_at    TEXT NOT NULL
);

CREATE TABLE characters (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_id       INTEGER REFERENCES users(id),
    name           TEXT NOT NULL,
    cls            TEXT NOT NULL,
    race           TEXT NOT NULL,
    level          INTEGER NOT NULL DEFAULT 1,
    hp             INTEGER NOT NULL,
    max_hp         INTEGER NOT NULL,
    ac             INTEGER NOT NULL,
    strength       INTEGER NOT NULL,
    dexterity      INTEGER NOT NULL,
    constitution   INTEGER NOT NULL,
    intelligence   INTEGER NOT NULL,
    wisdom         INTEGER NOT NULL,
    charisma       INTEGER NOT NULL,
    speed          INTEGER NOT NULL DEFAULT 6,
    color          TEXT,
    icon           TEXT,
    portrait       TEXT,
    attacks_json   TEXT NOT NULL DEFAULT '[]',
    inventory_json TEXT NOT NULL DEFAULT '[]',
    predefined     INTEGER NOT NULL DEFAULT 0,
    created_at     TEXT NOT NULL
);

CREATE TABLE campaigns (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    name            TEXT NOT NULL,
    setting         TEXT,
    description     TEXT,
    dm_name         TEXT NOT NULL DEFAULT 'El Dungeon Master',
    dm_avatar       TEXT,
    image           TEXT,
    status          TEXT NOT NULL DEFAULT 'LOBBY',
    phase           TEXT NOT NULL DEFAULT 'INTRO',
    active_scene_id INTEGER,
    created_by      INTEGER REFERENCES users(id),
    created_at      TEXT NOT NULL,
    updated_at      TEXT NOT NULL
);

CREATE TABLE campaign_members (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id  INTEGER NOT NULL REFERENCES campaigns(id),
    user_id      INTEGER NOT NULL REFERENCES users(id),
    character_id INTEGER REFERENCES characters(id),
    role         TEXT NOT NULL DEFAULT 'PLAYER',
    ready        INTEGER NOT NULL DEFAULT 0,
    joined_at    TEXT NOT NULL,
    UNIQUE (campaign_id, user_id)
);

CREATE TABLE scenes (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id  INTEGER NOT NULL REFERENCES campaigns(id),
    order_index  INTEGER NOT NULL,
    title        TEXT NOT NULL,
    description  TEXT,
    kind         TEXT NOT NULL DEFAULT 'NARRATIVE',
    image_prompt TEXT,
    image_url    TEXT
);

CREATE TABLE maps (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id    INTEGER NOT NULL REFERENCES campaigns(id),
    name           TEXT NOT NULL,
    width          INTEGER NOT NULL,
    height         INTEGER NOT NULL,
    tile_size      INTEGER NOT NULL DEFAULT 48,
    walls_json     TEXT NOT NULL DEFAULT '[]',
    doors_json     TEXT NOT NULL DEFAULT '[]',
    obstacles_json TEXT NOT NULL DEFAULT '[]',
    decor_json     TEXT NOT NULL DEFAULT '[]'
);

CREATE TABLE board_entities (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    scene_id   INTEGER NOT NULL REFERENCES scenes(id),
    map_id     INTEGER NOT NULL REFERENCES maps(id),
    kind       TEXT NOT NULL,
    ref_id     INTEGER,
    name       TEXT NOT NULL,
    x          INTEGER NOT NULL,
    y          INTEGER NOT NULL,
    hp         INTEGER NOT NULL,
    max_hp     INTEGER NOT NULL,
    ac         INTEGER NOT NULL,
    speed      INTEGER NOT NULL DEFAULT 6,
    color      TEXT,
    icon       TEXT,
    portrait   TEXT,
    alive      INTEGER NOT NULL DEFAULT 1,
    data_json  TEXT NOT NULL DEFAULT '{}'
);

CREATE TABLE npc_templates (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id INTEGER NOT NULL REFERENCES campaigns(id),
    name        TEXT NOT NULL,
    description TEXT,
    portrait    TEXT,
    disposition TEXT NOT NULL DEFAULT 'NEUTRAL',
    dialogue_json TEXT NOT NULL DEFAULT '[]'
);

CREATE TABLE enemy_templates (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id  INTEGER NOT NULL REFERENCES campaigns(id),
    name         TEXT NOT NULL,
    hp           INTEGER NOT NULL,
    max_hp       INTEGER NOT NULL,
    ac           INTEGER NOT NULL,
    speed        INTEGER NOT NULL DEFAULT 6,
    attacks_json TEXT NOT NULL DEFAULT '[]',
    behavior     TEXT NOT NULL DEFAULT 'MELEE',
    xp           INTEGER NOT NULL DEFAULT 0,
    portrait     TEXT
);

CREATE TABLE items (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id  INTEGER REFERENCES campaigns(id),
    name         TEXT NOT NULL,
    description  TEXT,
    kind         TEXT NOT NULL DEFAULT 'MISC',
    icon         TEXT,
    data_json    TEXT NOT NULL DEFAULT '{}'
);

CREATE TABLE combat_states (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id        INTEGER NOT NULL REFERENCES campaigns(id),
    scene_id           INTEGER REFERENCES scenes(id),
    active             INTEGER NOT NULL DEFAULT 0,
    round              INTEGER NOT NULL DEFAULT 0,
    current_turn_index INTEGER NOT NULL DEFAULT 0,
    status             TEXT NOT NULL DEFAULT 'NOT_STARTED',
    started_at         TEXT,
    ended_at           TEXT
);

CREATE TABLE combat_participants (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    combat_id   INTEGER NOT NULL REFERENCES combat_states(id),
    entity_id   INTEGER NOT NULL REFERENCES board_entities(id),
    initiative  INTEGER NOT NULL,
    order_index INTEGER NOT NULL,
    alive       INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE game_events (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id INTEGER NOT NULL REFERENCES campaigns(id),
    scene_id    INTEGER REFERENCES scenes(id),
    sequence    INTEGER NOT NULL,
    type        TEXT NOT NULL,
    actor       TEXT,
    message     TEXT NOT NULL,
    data_json   TEXT NOT NULL DEFAULT '{}',
    created_at  TEXT NOT NULL
);

CREATE TABLE dice_rolls (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    campaign_id INTEGER REFERENCES campaigns(id),
    user_id     INTEGER REFERENCES users(id),
    expression  TEXT NOT NULL,
    rolls_json  TEXT NOT NULL DEFAULT '[]',
    modifier    INTEGER NOT NULL DEFAULT 0,
    total       INTEGER NOT NULL,
    natural20   INTEGER NOT NULL DEFAULT 0,
    natural1    INTEGER NOT NULL DEFAULT 0,
    purpose     TEXT,
    created_at  TEXT NOT NULL
);

CREATE INDEX idx_characters_owner ON characters(owner_id);
CREATE INDEX idx_members_campaign ON campaign_members(campaign_id);
CREATE INDEX idx_scenes_campaign ON scenes(campaign_id, order_index);
CREATE INDEX idx_maps_campaign ON maps(campaign_id);
CREATE INDEX idx_entities_scene ON board_entities(scene_id);
CREATE INDEX idx_events_campaign ON game_events(campaign_id, sequence);
CREATE INDEX idx_combat_campaign ON combat_states(campaign_id);
CREATE INDEX idx_participants_combat ON combat_participants(combat_id, order_index);
CREATE INDEX idx_rolls_campaign ON dice_rolls(campaign_id);
