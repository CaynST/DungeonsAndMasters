-- Board entities copy the portrait at placement time, so existing tokens need the same fix.
UPDATE board_entities
SET portrait = '/images/npcs/archivista.png'
WHERE portrait = '/images/npcs/archivist.png';
