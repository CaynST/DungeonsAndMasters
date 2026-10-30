-- Campaigns generated before the asset fix stored the English filename, which never existed.
-- Keep this migration idempotent so it is safe to run on already-correct databases.
UPDATE npc_templates
SET portrait = '/images/npcs/archivista.png'
WHERE portrait = '/images/npcs/archivist.png';
