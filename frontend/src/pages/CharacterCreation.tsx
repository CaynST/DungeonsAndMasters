import { useMutation, useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { ArrowLeft, ArrowRight, Check, Dices, Sparkles } from "lucide-react";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { cn, formatModifier } from "@/lib/utils";
import { PointBuyPanel } from "@/components/character/PointBuyPanel";
import type {
  AbilityScores,
  BackgroundView,
  ClassView,
  CreateCharacterPayload,
  RulesetView,
  SpeciesView,
} from "@/types/game";

type Step = "edition" | "species" | "class" | "background" | "abilities" | "skills" | "review";

const STEPS: { id: Step; label: string }[] = [
  { id: "edition", label: "Edición" },
  { id: "species", label: "Especie" },
  { id: "class", label: "Clase" },
  { id: "background", label: "Trasfondo" },
  { id: "abilities", label: "Características" },
  { id: "skills", label: "Habilidades" },
  { id: "review", label: "Revisión" },
];

const BASE: AbilityScores = {
  strength: 10,
  dexterity: 10,
  constitution: 10,
  intelligence: 10,
  wisdom: 10,
  charisma: 10,
};

export function CharacterCreation() {
  const navigate = useNavigate();
  const [step, setStep] = useState<Step>("edition");
  const [editionId, setEditionId] = useState("dnd-5e-2014");
  const [name, setName] = useState("");
  const [species, setSpecies] = useState<SpeciesView | null>(null);
  const [klass, setKlass] = useState<ClassView | null>(null);
  const [background, setBackground] = useState<BackgroundView | null>(null);
  const [scores, setScores] = useState<AbilityScores>(BASE);
  const [skills, setSkills] = useState<string[]>([]);

  const ruleset = useQuery({
    queryKey: ["ruleset", editionId],
    queryFn: () => api.ruleset(editionId),
  });

  const create = useMutation({
    mutationFn: (payload: CreateCharacterPayload) => api.createCharacter(payload),
    onSuccess: () => navigate("/"),
  });

  const stepIndex = STEPS.findIndex((item) => item.id === step);
  const canAdvance = useMemo(() => {
    switch (step) {
      case "edition":
        return Boolean(ruleset.data);
      case "species":
        return Boolean(species);
      case "class":
        return Boolean(klass);
      case "background":
        return Boolean(background);
      case "abilities":
        return pointsRemaining(scores) === 0;
      case "skills":
        return skills.length === (klass?.skillChoiceCount ?? 0);
      case "review":
        return name.trim().length > 0;
      default:
        return false;
    }
  }, [step, ruleset.data, species, klass, background, scores, skills, name]);

  const goNext = () => {
    if (stepIndex < STEPS.length - 1) {
      setStep(STEPS[stepIndex + 1].id);
    }
  };
  const goBack = () => {
    if (stepIndex > 0) setStep(STEPS[stepIndex - 1].id);
  };

  const submit = () => {
    if (!species || !klass || !background) return;
    create.mutate({
      name,
      edition: editionId,
      speciesId: species.id,
      classId: klass.id,
      backgroundId: background.id,
      scores,
      skills,
      armor: klass.startingArmor,
      weapon: klass.weapons[0],
    });
  };

  const toggleSkill = (skill: string) => {
    setSkills((current) => {
      if (current.includes(skill)) return current.filter((item) => item !== skill);
      if (klass && current.length >= klass.skillChoiceCount) return current;
      return [...current, skill];
    });
  };

  return (
    <div className="mx-auto max-w-5xl px-6 py-8">
      <header className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="title-display text-3xl font-bold text-parchment text-glow-gold">
            Forjar un aventurero
          </h1>
          <p className="mt-1 text-sm text-muted">Las reglas se aplican en el servidor (SRD).</p>
        </div>
        <Button variant="ghost" onClick={() => navigate("/")}>
          <ArrowLeft className="h-4 w-4" /> Cancelar
        </Button>
      </header>

      <Stepper current={stepIndex} />

      <div className="panel mt-6 min-h-[420px] p-6">
        {step === "edition" && (
          <EditionStep
            value={editionId}
            onChange={(id) => {
              setEditionId(id);
              setSpecies(null);
              setKlass(null);
              setBackground(null);
              setSkills([]);
            }}
          />
        )}

        {step === "species" && ruleset.data && (
          <SpeciesStep value={species} onChange={setSpecies} options={ruleset.data.species} edition={ruleset.data} />
        )}

        {step === "class" && ruleset.data && (
          <ClassStep value={klass} onChange={(value) => { setKlass(value); setSkills([]); }} options={ruleset.data.classes} />
        )}

        {step === "background" && ruleset.data && (
          <BackgroundStep value={background} onChange={setBackground} options={ruleset.data.backgrounds} />
        )}

        {step === "abilities" && (
          <AbilitiesStep scores={scores} onChange={setScores} klassId={klass?.id} />
        )}

        {step === "skills" && klass && (
          <SkillsStep klass={klass} selected={skills} onToggle={toggleSkill} />
        )}

        {step === "review" && species && klass && background && ruleset.data && (
          <ReviewStep
            name={name}
            setName={setName}
            species={species}
            klass={klass}
            background={background}
            scores={scores}
            skills={skills}
            edition={ruleset.data}
          />
        )}
      </div>

      <footer className="mt-4 flex items-center justify-between">
        <Button variant="ghost" onClick={goBack} disabled={stepIndex === 0}>
          <ArrowLeft className="h-4 w-4" /> Atrás
        </Button>
        {step === "review" ? (
          <Button
            variant="primary"
            size="lg"
            disabled={!canAdvance || create.isPending}
            onClick={submit}
          >
            <Sparkles className="h-4 w-4" />
            {create.isPending ? "Forjando..." : "Crear personaje"}
          </Button>
        ) : (
          <Button variant="primary" onClick={goNext} disabled={!canAdvance}>
            Siguiente <ArrowRight className="h-4 w-4" />
          </Button>
        )}
      </footer>

      {create.isError && (
        <p className="mt-3 rounded-md border border-crimson/30 bg-crimson/10 px-3 py-2 text-sm text-crimson-soft">
          {(create.error as Error).message}
        </p>
      )}
    </div>
  );
}

function Stepper({ current }: { current: number }) {
  return (
    <ol className="flex flex-wrap gap-2">
      {STEPS.map((item, index) => (
        <li key={item.id} className="flex items-center gap-2">
          <span
            className={cn(
              "flex h-7 w-7 items-center justify-center rounded-full border text-xs font-semibold",
              index < current
                ? "border-emerald/50 bg-emerald/20 text-emerald-soft"
                : index === current
                  ? "border-gold/60 bg-gold/15 text-gold-soft"
                  : "border-parchment/15 text-muted",
            )}
          >
            {index < current ? <Check className="h-3.5 w-3.5" /> : index + 1}
          </span>
          <span className={cn("text-xs", index === current ? "text-ink" : "text-muted")}>{item.label}</span>
          {index < STEPS.length - 1 && <span className="mx-1 h-px w-4 bg-parchment/15" />}
        </li>
      ))}
    </ol>
  );
}

function EditionStep({ value, onChange }: { value: string; onChange: (id: string) => void }) {
  const editions = [
    { id: "dnd-5e-2014", title: "D&D 5e (2014)", desc: "Las características vienen de la raza. Reglas clásicas del Manual del Jugador." },
    { id: "dnd-5e-2024", title: "D&D 5e (2024)", desc: "Especies y trasfondos actualizados; las características vienen del trasfondo." },
  ];
  return (
    <Section title="Elige la edición" subtitle="La campaña y la creación usan el mismo conjunto de reglas.">
      <div className="grid gap-4 sm:grid-cols-2">
        {editions.map((edition) => (
          <button
            key={edition.id}
            onClick={() => onChange(edition.id)}
            className={cn(
              "rounded-xl border p-5 text-left transition",
              value === edition.id ? "border-gold/60 bg-gold/10 shadow-glow" : "border-parchment/10 bg-surface/50 hover:border-gold/30",
            )}
          >
            <h3 className="title-display text-lg text-parchment">{edition.title}</h3>
            <p className="mt-2 text-sm text-muted">{edition.desc}</p>
          </button>
        ))}
      </div>
    </Section>
  );
}

function SpeciesStep({
  value,
  onChange,
  options,
  edition,
}: {
  value: SpeciesView | null;
  onChange: (species: SpeciesView) => void;
  options: SpeciesView[];
  edition: RulesetView;
}) {
  const is2024 = edition.edition === "DND_5E_2024";
  return (
    <Section title={is2024 ? "Elige tu especie" : "Elige tu raza"} subtitle="Otorga velocidad, visión y (en 2014) bonificadores de característica.">
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {options.map((species) => (
          <ChoiceCard
            key={species.id}
            selected={value?.id === species.id}
            onClick={() => onChange(species)}
            title={species.name}
            description={species.description}
          >
            <div className="mt-2 flex flex-wrap gap-1.5">
              <Badge tone="muted">{species.size}</Badge>
              <Badge tone="arcane">Vel {species.speed}</Badge>
              {species.darkvision && <Badge tone="gold">Visión oscura</Badge>}
              {Object.entries(species.abilityBonuses).map(([ability, bonus]) => (
                <Badge key={ability} tone="emerald">
                  {abilityLabel(ability)} {formatModifier(bonus)}
                </Badge>
              ))}
            </div>
          </ChoiceCard>
        ))}
      </div>
    </Section>
  );
}

function ClassStep({
  value,
  onChange,
  options,
}: {
  value: ClassView | null;
  onChange: (value: ClassView) => void;
  options: ClassView[];
}) {
  return (
    <Section title="Elige tu clase" subtitle="Define el dado de golpe, salvaciones, habilidades y equipo inicial.">
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {options.map((klass) => (
          <ChoiceCard
            key={klass.id}
            selected={value?.id === klass.id}
            onClick={() => onChange(klass)}
            title={klass.name}
            description={klass.description}
          >
            <div className="mt-2 flex flex-wrap gap-1.5">
              <Badge tone="crimson">Golpe {klass.hitDie}</Badge>
              <Badge tone="gold">{klass.skillChoiceCount} habilidades</Badge>
              <Badge tone="arcane">{klass.savingThrows.map(abilityShort).join(" / ")}</Badge>
            </div>
          </ChoiceCard>
        ))}
      </div>
    </Section>
  );
}

function BackgroundStep({
  value,
  onChange,
  options,
}: {
  value: BackgroundView | null;
  onChange: (value: BackgroundView) => void;
  options: BackgroundView[];
}) {
  return (
    <Section title="Elige tu trasfondo" subtitle="Aporta habilidades y, en 2024, los bonificadores de característica.">
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {options.map((background) => (
          <ChoiceCard
            key={background.id}
            selected={value?.id === background.id}
            onClick={() => onChange(background)}
            title={background.name}
            description={background.description}
          >
            <div className="mt-2 flex flex-wrap gap-1.5">
              {background.skills.map((skill) => (
                <Badge key={skill} tone="emerald">
                  {skillLabel(skill)}
                </Badge>
              ))}
              {Object.entries(background.abilityBonuses).map(([ability, bonus]) => (
                <Badge key={ability} tone="gold">
                  {abilityLabel(ability)} {formatModifier(bonus)}
                </Badge>
              ))}
            </div>
          </ChoiceCard>
        ))}
      </div>
    </Section>
  );
}

function AbilitiesStep({
  scores,
  onChange,
  klassId,
}: {
  scores: AbilityScores;
  onChange: (scores: AbilityScores) => void;
  klassId?: string;
}) {
  return (
    <Section title="Reparte las características" subtitle="Método estándar de compra de puntos: 27 puntos, valores de 8 a 15.">
      <PointBuyPanel scores={scores} onChange={onChange} primaryAbility={klassId ? primaryOf(klassId) : undefined} />
    </Section>
  );
}

function SkillsStep({
  klass,
  selected,
  onToggle,
}: {
  klass: ClassView;
  selected: string[];
  onToggle: (skill: string) => void;
}) {
  const remaining = klass.skillChoiceCount - selected.length;
  return (
    <Section
      title="Elige tus habilidades"
      subtitle={`Selecciona ${klass.skillChoiceCount} de la lista de ${klass.name}.`}
    >
      <p className="mb-3 text-sm">
        {remaining > 0 ? (
          <span className="text-muted">Te quedan <span className="font-semibold text-gold-soft">{remaining}</span> por elegir.</span>
        ) : (
          <span className="text-emerald-soft">Elección completa.</span>
        )}
      </p>
      <div className="flex flex-wrap gap-2">
        {klass.skillChoices.map((skill) => {
          const active = selected.includes(skill);
          return (
            <button
              key={skill}
              onClick={() => onToggle(skill)}
              className={cn(
                "rounded-lg border px-3 py-2 text-sm transition",
                active
                  ? "border-emerald/50 bg-emerald/15 text-emerald-soft"
                  : "border-parchment/15 bg-surface/50 text-muted hover:border-gold/30",
              )}
            >
              {skillLabel(skill)}
            </button>
          );
        })}
      </div>
    </Section>
  );
}

function ReviewStep({
  name,
  setName,
  species,
  klass,
  background,
  scores,
  skills,
  edition,
}: {
  name: string;
  setName: (name: string) => void;
  species: SpeciesView;
  klass: ClassView;
  background: BackgroundView;
  scores: AbilityScores;
  skills: string[];
  edition: RulesetView;
}) {
  // Preview of final scores with bonuses applied (mirrors backend logic).
  const final = applyBonuses(scores, species, background);
  return (
    <Section title="Revisa tu aventurero" subtitle="El servidor validará y calculará HP, CA, salvaciones y ataques.">
      <label className="mb-5 block max-w-sm">
        <span className="mb-1.5 block text-xs font-medium uppercase tracking-wider text-muted">Nombre</span>
        <input
          value={name}
          onChange={(event) => setName(event.target.value)}
          placeholder="Nombre del personaje"
          className="h-10 w-full rounded-lg border border-parchment/15 bg-black/35 px-3 text-ink outline-none focus:border-gold/50"
        />
      </label>

      <div className="grid gap-5 lg:grid-cols-2">
        <div className="space-y-3">
          <ReviewRow label="Edición" value={edition.displayName} />
          <ReviewRow label={edition.edition === "DND_5E_2024" ? "Especie" : "Raza"} value={species.name} />
          <ReviewRow label="Clase" value={`${klass.name} (${klass.hitDie})`} />
          <ReviewRow label="Trasfondo" value={background.name} />
          <ReviewRow label="Habilidades" value={skills.map(skillLabel).join(", ") || "—"} />
        </div>
        <div className="grid grid-cols-3 gap-2">
          {(["strength", "dexterity", "constitution", "intelligence", "wisdom", "charisma"] as const).map((ability) => (
            <div key={ability} className="rounded-lg border border-parchment/10 bg-black/30 p-3 text-center">
              <p className="text-[10px] uppercase tracking-widest text-muted">{abilityShort(ability)}</p>
              <p className="title-display text-2xl text-ink">{final[ability]}</p>
              <p className="text-xs text-gold-soft">{formatModifier(modifier(final[ability]))}</p>
            </div>
          ))}
        </div>
      </div>
    </Section>
  );
}

function Section({ title, subtitle, children }: { title: string; subtitle?: string; children: React.ReactNode }) {
  return (
    <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}>
      <h2 className="title-display text-xl text-parchment">{title}</h2>
      {subtitle && <p className="mb-4 mt-1 text-sm text-muted">{subtitle}</p>}
      {children}
    </motion.div>
  );
}

function ChoiceCard({
  selected,
  onClick,
  title,
  description,
  children,
}: {
  selected: boolean;
  onClick: () => void;
  title: string;
  description: string;
  children?: React.ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      className={cn(
        "rounded-lg border p-4 text-left transition",
        selected ? "border-gold/60 bg-gold/10 shadow-glow" : "border-parchment/10 bg-surface/50 hover:border-gold/30",
      )}
    >
      <h3 className="title-display text-base text-parchment">{title}</h3>
      <p className="mt-1 text-xs leading-relaxed text-muted">{description}</p>
      {children}
    </button>
  );
}

function ReviewRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center justify-between rounded-lg border border-parchment/10 bg-black/25 px-3 py-2 text-sm">
      <span className="text-muted">{label}</span>
      <span className="text-ink">{value}</span>
    </div>
  );
}

// ---- helpers mirrored from the backend for a live preview ----

const POINT_COST: Record<number, number> = { 8: 0, 9: 1, 10: 2, 11: 3, 12: 4, 13: 5, 14: 7, 15: 9 };

export function pointsRemaining(scores: AbilityScores): number {
  return 27 - Object.values(scores).reduce((total, score) => total + (POINT_COST[score] ?? 0), 0);
}

export const POINT_BUY_COST = POINT_COST;

function modifier(score: number): number {
  return Math.floor((score - 10) / 2);
}

function applyBonuses(scores: AbilityScores, species: SpeciesView, background: BackgroundView): AbilityScores {
  const result: AbilityScores = { ...scores };
  const add = (bonuses: Record<string, number>) => {
    for (const [ability, bonus] of Object.entries(bonuses)) {
      result[ability as keyof AbilityScores] += bonus;
    }
  };
  add(species.abilityBonuses);
  add(background.abilityBonuses);
  return result;
}

function primaryOf(classId: string): keyof AbilityScores {
  const map: Record<string, keyof AbilityScores> = {
    fighter: "strength",
    barbarian: "strength",
    paladin: "strength",
    ranger: "dexterity",
    rogue: "dexterity",
    monk: "dexterity",
    wizard: "intelligence",
    cleric: "wisdom",
    druid: "wisdom",
    bard: "charisma",
    sorcerer: "charisma",
    warlock: "charisma",
  };
  return map[classId] ?? "strength";
}

const ABILITY_LABELS: Record<string, string> = {
  strength: "Fuerza",
  dexterity: "Destreza",
  constitution: "Constitución",
  intelligence: "Inteligencia",
  wisdom: "Sabiduría",
  charisma: "Carisma",
};

const ABILITY_SHORT: Record<string, string> = {
  strength: "FUE",
  dexterity: "DES",
  constitution: "CON",
  intelligence: "INT",
  wisdom: "SAB",
  charisma: "CAR",
};

const SKILL_LABELS: Record<string, string> = {
  acrobatics: "Acrobacias",
  animal_handling: "Trato con animales",
  arcana: "Arcanos",
  athletics: "Atletismo",
  deception: "Engaño",
  history: "Historia",
  insight: "Perspicacia",
  intimidation: "Intimidación",
  investigation: "Investigación",
  medicine: "Medicina",
  nature: "Naturaleza",
  perception: "Percepción",
  performance: "Interpretación",
  persuasion: "Persuasión",
  religion: "Religión",
  sleight_of_hand: "Juego de manos",
  stealth: "Sigilo",
  survival: "Supervivencia",
};

export function abilityLabel(ability: string): string {
  return ABILITY_LABELS[ability] ?? ability;
}

export function abilityShort(ability: string): string {
  return ABILITY_SHORT[ability] ?? ability.slice(0, 3).toUpperCase();
}

export function skillLabel(skill: string): string {
  return SKILL_LABELS[skill] ?? skill;
}
