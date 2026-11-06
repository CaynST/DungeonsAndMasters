-- Predefined Kael predates Fighting Style choices; give him Dueling so the demo showcases it.
-- New databases already seed this value from PredefinedCharacters.
UPDATE characters
SET creation_data_json = '{"fightingStyle":"dueling"}'
WHERE predefined = 1
  AND name = 'Kael'
  AND (creation_data_json IS NULL OR creation_data_json = '{}' OR creation_data_json = '');
