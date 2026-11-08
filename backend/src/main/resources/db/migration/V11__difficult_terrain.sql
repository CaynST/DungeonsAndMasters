-- Difficult terrain: cells that cost twice the normal movement to enter.
-- Existing maps keep the empty default, so no content changes are forced.
ALTER TABLE maps ADD COLUMN difficult_terrain_json TEXT NOT NULL DEFAULT '[]';
