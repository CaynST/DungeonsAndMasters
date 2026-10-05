package com.dndmasters.adventure;

import com.dndmasters.ai.AIProvider;
import com.dndmasters.ai.AiRequest;
import com.dndmasters.content.AdventureDefinition;
import com.dndmasters.content.AdventureDefinition.EnemyDefinition;
import com.dndmasters.content.AdventureDefinition.ItemDefinition;
import com.dndmasters.content.AdventureDefinition.NpcDefinition;
import com.dndmasters.content.AdventureDefinition.Placement;
import com.dndmasters.content.AdventureDefinition.SceneDefinition;
import com.dndmasters.content.Bestiary;
import com.dndmasters.content.MapGenerator;
import com.dndmasters.domain.Cell;
import com.dndmasters.domain.GameMap;
import com.dndmasters.util.Json;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Produces a fresh adventure for every campaign. Prefers the AI Dungeon Master for the
 * narrative; falls back to a procedural generator when the AI is unavailable or returns
 * something unusable, so a new story is created every single time either way.
 */
@Service
public class AdventureGenerator {

    private static final Logger log = LoggerFactory.getLogger(AdventureGenerator.class);

    private static final String[] PLACES = {
            "la Cripta de Valdros", "el Bosque de Umbría", "las Ruinas de Khar", "el Pantano de Vael",
            "la Torre Quebrada", "las Cavernas de Zul", "el Monasterio Abandonado", "el Cementerio de Sal",
            "las Minas de Ferro", "el Templo Sumergido", "el Paso de los Susurros", "la Ciudadela Helada",
            "el Desierto de Ceniza", "la Garganta del Trueno", "el Templo de la Luna", "las Alcantarillas de Vess",
            "el Glaciar Eterno", "la Forja del Volcán", "la Aldea de Roble Gris", "la Taberna del Cuervo Cojo",
            "el Mercado de Sombras", "la Biblioteca Prohibida", "el Lago de Cristal", "las Catacumbas Reales"};
    private static final String[] VILLAINS = {
            "el Nigromante Sarthos", "la Reina Araña Zila", "el Guardián de Piedra", "el Dragón Carmesí",
            "el Culto del Eclipse", "el Señor de las Ratas", "la Bruja del Pantano", "el Caballero Caído",
            "el Devorador de Almas", "la Sombra del Trono"};
    private static final String[] RELICS = {
            "el Relicario de Plata", "la Corona Perdida", "el Grimorio Prohibido", "la Llama Eterna",
            "el Corazón de Cristal", "el Sello de Hierro", "la Lágrima de la Luna", "el Estandarte Roto"};

    private final AIProvider provider;

    public AdventureGenerator(AIProvider provider) {
        this.provider = provider;
    }

    public AdventurePlan generate(Random random) {
        if (provider != null && !"mock".equals(provider.id())) {
            try {
                AdventurePlan plan = fromAi(random);
                if (isUsable(plan)) {
                    log.info("Generated adventure with AI: {}", plan.title());
                    return normalize(plan, random);
                }
            } catch (RuntimeException e) {
                log.warn("AI adventure generation failed, using procedural fallback: {}", e.getMessage());
            }
        }
        AdventurePlan plan = procedural(random);
        log.info("Generated procedural adventure: {}", plan.title());
        return plan;
    }

    // ---- AI path ----

    private AdventurePlan fromAi(Random random) {
        StringBuilder bestiary = new StringBuilder();
        for (Bestiary.Monster m : Bestiary.all()) {
            bestiary.append(m.id()).append(" (").append(m.name()).append(", ").append(m.tier()).append("); ");
        }
        String system = """
                Eres un Dungeon Master experto en Dungeons & Dragons. Diseñas aventuras originales.
                Responde SOLO con un objeto JSON válido con esta forma:
                {
                  "title": "título épico y evocador",
                  "setting": "ambientación en una frase",
                  "description": "premisa de la aventura en 2-3 frases",
                  "objective": "objetivo principal en una frase",
                  "villain": "nombre del antagonista",
                  "premise": ["frase de introducción tipo 'Érase una vez...'", "otra frase", "otra frase"],
                  "scenes": [
                    { "title": "...", "description": "qué ven los jugadores", "kind": "SOCIAL|EXPLORATION|COMBAT|BOSS",
                      "objective": "qué deben lograr", "theme": "crypt|forest|cave|ruins|dungeon|swamp",
                      "enemyIds": ["id-del-bestiario"] }
                  ]
                }
                Reglas: exactamente 3 escenas (una introductoria sin enemigos, una de combate, y una final de jefe BOSS).
                Usa SOLO ids de este bestiario: %s
                Los temas válidos son: crypt, forest, cave, ruins, dungeon, swamp, desert, mountain,
                temple, sewer, ice, volcano, village, tavern.
                """.formatted(bestiary);

        AiRequest request = new AiRequest(system, "Genera una aventura nueva y original.", Map.of(), 1.0, 1200);
        String text = provider.complete(request).text();
        return Json.read(extractJson(text), AdventurePlan.class);
    }

    private String extractJson(String text) {
        if (text == null) {
            throw new IllegalStateException("Empty AI response");
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalStateException("No JSON in AI response");
        }
        return text.substring(start, end + 1);
    }

    private boolean isUsable(AdventurePlan plan) {
        return plan != null && plan.title() != null && !plan.title().isBlank()
                && plan.scenes() != null && !plan.scenes().isEmpty();
    }

    /** Repairs a plan so it always has a boss scene and valid themes/enemies. */
    private AdventurePlan normalize(AdventurePlan plan, Random random) {
        List<AdventurePlan.ScenePlan> scenes = new ArrayList<>();
        for (AdventurePlan.ScenePlan scene : plan.scenes()) {
            String kind = scene.kind() == null ? "COMBAT" : scene.kind().toUpperCase();
            String theme = scene.theme() == null || !isTheme(scene.theme()) ? MapGenerator.randomTheme(random) : scene.theme();
            List<String> enemies = validEnemyIds(scene.enemyIds());
            if ("BOSS".equals(kind) && enemies.isEmpty()) {
                enemies = List.of(pickBoss(random).id());
            }
            scenes.add(new AdventurePlan.ScenePlan(scene.title(), scene.description(), kind, scene.objective(), theme, enemies));
        }
        boolean hasBoss = scenes.stream().anyMatch(s -> "BOSS".equals(s.kind()));
        if (!hasBoss) {
            scenes.add(new AdventurePlan.ScenePlan("El enfrentamiento final",
                    "El camino desemboca en la guarida del villano.", "BOSS",
                    "Derrota a " + plan.villain() + ".", MapGenerator.randomTheme(random),
                    List.of(pickBoss(random).id())));
        }
        List<String> premise = plan.premise() == null || plan.premise().isEmpty()
                ? List.of("Érase una vez una historia que aún no se ha contado...") : plan.premise();
        return new AdventurePlan(plan.title(), plan.setting(), plan.description(), plan.objective(),
                plan.villain() == null ? "una amenaza olvidada" : plan.villain(), premise, scenes);
    }

    private List<String> validEnemyIds(List<String> ids) {
        if (ids == null) {
            return List.of();
        }
        List<String> valid = new ArrayList<>();
        for (String id : ids) {
            try {
                valid.add(Bestiary.byId(id).id());
            } catch (RuntimeException ignored) {
                // Unknown id from the model: skip it.
            }
        }
        return valid;
    }

    private boolean isTheme(String theme) {
        return List.of("crypt", "forest", "cave", "ruins", "dungeon", "swamp",
                "desert", "mountain", "temple", "sewer", "ice", "volcano", "village", "tavern").contains(theme);
    }

    private Bestiary.Monster pickBoss(Random random) {
        List<Bestiary.Monster> bosses = Bestiary.byTier(Bestiary.Tier.BOSS);
        return bosses.get(random.nextInt(bosses.size()));
    }

    // ---- Procedural fallback ----

    private AdventurePlan procedural(Random random) {
        String place = pick(PLACES, random);
        String villain = pick(VILLAINS, random);
        String relic = pick(RELICS, random);
        String themeA = MapGenerator.randomTheme(random);
        String themeB = MapGenerator.randomTheme(random);
        String themeC = MapGenerator.randomTheme(random);

        List<Bestiary.Monster> skirmish = Bestiary.pick(Bestiary.Tier.MINION, 3, random);
        Bestiary.Monster boss = pickBoss(random);

        List<AdventurePlan.ScenePlan> scenes = List.of(
                new AdventurePlan.ScenePlan("La llegada a " + place,
                        "El grupo llega al umbral de " + place + ". Algo no va bien: el silencio pesa demasiado.",
                        "SOCIAL", "Descubrir qué ha ocurrido aquí.", themeA, List.of()),
                new AdventurePlan.ScenePlan("Guardianes del camino",
                        "Criaturas al servicio de " + villain + " bloquean el paso.",
                        "COMBAT", "Abrirse paso entre los guardianes.", themeB,
                        skirmish.stream().map(Bestiary.Monster::id).toList()),
                new AdventurePlan.ScenePlan("El corazón de " + place,
                        villain + " aguarda junto a " + relic + ". No piensa entregarlo sin lucha.",
                        "BOSS", "Derrotar a " + villain + " y recuperar " + relic + ".", themeC, List.of(boss.id())));

        return new AdventurePlan(
                "La Sombra de " + capitalize(stripArticle(place)),
                "Un rincón del mundo donde la luz no llega.",
                "Una compañía de aventureros acude a " + place + " tras la pista de " + relic
                        + ". Lo que encuentre allí pondrá a prueba algo más que sus espadas.",
                "Recuperar " + relic + " y detener a " + villain + ".",
                villain,
                List.of(
                        "Érase una vez un lugar llamado " + place + ", del que nadie regresaba para contarlo.",
                        "Se decía que " + villain + " custodiaba " + relic + " en su interior.",
                        "Y se decía, también, que solo unos pocos valientes se atreverían a entrar."),
                scenes);
    }

    // ---- Materialisation ----

    /** Turns a plan into the persisted adventure shape (maps, enemies, placements, items). */
    public AdventureDefinition materialize(AdventurePlan plan, Random random) {
        Map<String, EnemyDefinition> enemies = new LinkedHashMap<>();
        List<NpcDefinition> npcs = List.of(new NpcDefinition("El Guía",
                "Un lugareño nervioso que conoce los rumores del lugar.", "/images/npcs/archivist.png",
                List.of("No deberíais haber venido... pero ya es tarde.", "El villano no perdona. Nadie vuelve.")));
        List<ItemDefinition> items = List.of(
                new ItemDefinition(stripArticle(plan.villain() == null ? "el botín" : plan.villain()) + " trofeo",
                        "Prueba de vuestra victoria.", "TROPHY", "gem"),
                new ItemDefinition("Poción de Curación", "Recupera 2d4+2 HP.", "POTION", "flask"));

        List<SceneDefinition> scenes = new ArrayList<>();
        int order = 0;
        for (AdventurePlan.ScenePlan scenePlan : plan.scenes()) {
            int width = "BOSS".equals(scenePlan.kind()) ? 13 : "COMBAT".equals(scenePlan.kind()) ? 14 : 12;
            int height = "BOSS".equals(scenePlan.kind()) ? 13 : 10;
            MapGenerator.Layout layout = MapGenerator.generate(scenePlan.theme(), width, height, random);
            GameMap map = layout.map();

            List<Placement> placements = new ArrayList<>();
            int spawnIndex = 0;
            for (String enemyId : scenePlan.enemyIds()) {
                if (spawnIndex >= layout.enemySpawns().size()) {
                    break;
                }
                Bestiary.Monster monster = Bestiary.byId(enemyId);
                enemies.putIfAbsent(monster.id(), toEnemy(monster));
                Cell spawn = layout.enemySpawns().get(spawnIndex++);
                placements.add(new Placement("ENEMY", monster.name(), spawn.x(), spawn.y()));
            }
            if ("SOCIAL".equals(scenePlan.kind())) {
                Cell npcCell = layout.partySpawn().get(0).plus(3, 0);
                placements.add(new Placement("NPC", "El Guía", npcCell.x(), npcCell.y()));
            }
            if ("BOSS".equals(scenePlan.kind())) {
                Cell itemCell = layout.enemySpawns().get(layout.enemySpawns().size() - 1).plus(0, 0);
                placements.add(new Placement("ITEM", "Poción de Curación", itemCell.x(), itemCell.y()));
            }

            scenes.add(new SceneDefinition(order++, scenePlan.title(), scenePlan.description(), scenePlan.kind(),
                    scenePlan.description(), map, layout.partySpawn(), placements));
        }

        return new AdventureDefinition(
                "generated",
                plan.title(),
                plan.setting(),
                plan.description(),
                "El Dungeon Master",
                plan.villain(),
                plan.premise(),
                plan.objective(),
                new ArrayList<>(enemies.values()),
                npcs,
                items,
                scenes);
    }

    private EnemyDefinition toEnemy(Bestiary.Monster m) {
        return new EnemyDefinition(m.name(), m.hp(), m.ac(), m.speed(), m.stats(), m.attacks(),
                m.behavior(), m.color(), m.icon(), m.portrait());
    }

    private static String pick(String[] pool, Random random) {
        return pool[random.nextInt(pool.length)];
    }

    private static String stripArticle(String value) {
        if (value == null) {
            return "";
        }
        for (String article : List.of("el ", "la ", "los ", "las ")) {
            if (value.toLowerCase().startsWith(article)) {
                return value.substring(article.length());
            }
        }
        return value;
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
