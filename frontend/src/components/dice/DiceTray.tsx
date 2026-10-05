import { useEffect, useMemo, useRef, useState } from "react";
import { Canvas, useFrame } from "@react-three/fiber";
import { Billboard, Environment, Lightformer } from "@react-three/drei";
import { motion } from "framer-motion";
import * as THREE from "three";
import type { DiceRoll } from "@/types/game";

interface DiceTrayProps {
  roll: DiceRoll;
  onComplete: () => void;
}

type Outcome = "crit" | "fail" | "max" | "normal";

const OUTCOME_STYLE: Record<Outcome, { color: string; label: string }> = {
  crit: { color: "#E0B84A", label: "¡CRÍTICO!" },
  fail: { color: "#D0566A", label: "FALLO CRÍTICO" },
  max: { color: "#9B7CFF", label: "MÁXIMO" },
  normal: { color: "#E8E1D5", label: "" },
};

function outcomeOf(roll: DiceRoll): Outcome {
  if (roll.natural20) return "crit";
  if (roll.natural1) return "fail";
  const allMax = roll.rolls.length > 0 && roll.rolls.every((value) => value === 20);
  return allMax ? "max" : "normal";
}

/** Extracts the number of sides from the first die in an expression like "2d6+3". */
export function primarySides(expression: string): number {
  const match = /(\d*)d(\d+)/i.exec(expression ?? "");
  return match ? Number(match[2]) : 20;
}

function geometryFor(sides: number): THREE.BufferGeometry | null {
  switch (sides) {
    case 4:
      return new THREE.TetrahedronGeometry(1.15);
    case 6:
      return new THREE.BoxGeometry(1.25, 1.25, 1.25);
    case 8:
      return new THREE.OctahedronGeometry(1.15);
    case 12:
      return new THREE.DodecahedronGeometry(1.1);
    case 20:
      return new THREE.IcosahedronGeometry(1.1);
    default:
      return null; // d10 handled as a bipyramid
  }
}

function DieBody({ sides, color }: { sides: number; color: string }) {
  const geometry = useMemo(() => geometryFor(sides), [sides]);
  const material = useMemo(
    () => new THREE.MeshStandardMaterial({ color, metalness: 0.25, roughness: 0.45, flatShading: true }),
    [color],
  );
  const coneUp = useMemo(() => new THREE.ConeGeometry(1.0, 1.15, 5, 1), []);
  const coneDown = useMemo(() => new THREE.ConeGeometry(1.0, 1.15, 5, 1), []);

  if (sides === 10) {
    return (
      <group>
        <mesh geometry={coneUp} material={material} position={[0, 0.55, 0]} />
        <mesh geometry={coneDown} material={material} position={[0, -0.55, 0]} rotation={[Math.PI, 0, 0]} />
      </group>
    );
  }
  return <mesh geometry={geometry ?? undefined} material={material} />;
}

function RollingDie({ sides, color, rolling }: { sides: number; color: string; rolling: boolean }) {
  const group = useRef<THREE.Group>(null);
  useFrame((state, delta) => {
    if (!group.current) return;
    if (rolling) {
      group.current.rotation.x += delta * 8;
      group.current.rotation.y += delta * 6;
      group.current.rotation.z += delta * 4;
      group.current.position.y = Math.abs(Math.sin(state.clock.elapsedTime * 5)) * 0.5;
    } else {
      group.current.position.y = THREE.MathUtils.lerp(group.current.position.y, 0, 0.2);
      group.current.rotation.x = THREE.MathUtils.lerp(group.current.rotation.x, 0.3, 0.1);
      group.current.rotation.y = THREE.MathUtils.lerp(group.current.rotation.y, 0.5, 0.1);
      group.current.rotation.z = THREE.MathUtils.lerp(group.current.rotation.z, 0, 0.1);
    }
  });
  return (
    <group ref={group}>
      <DieBody sides={sides} color={color} />
    </group>
  );
}

/** A number drawn on a canvas, shown as a camera-facing badge over the die. */
function ResultBadge({ value, color }: { value: number; color: string }) {
  const texture = useMemo(() => {
    const canvas = document.createElement("canvas");
    canvas.width = 128;
    canvas.height = 128;
    const ctx = canvas.getContext("2d");
    if (ctx) {
      ctx.fillStyle = "#0c0a08";
      ctx.beginPath();
      ctx.arc(64, 64, 58, 0, Math.PI * 2);
      ctx.fill();
      ctx.lineWidth = 4;
      ctx.strokeStyle = color;
      ctx.stroke();
      ctx.fillStyle = color;
      ctx.font = "bold 78px Inter, system-ui, sans-serif";
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.fillText(String(value), 64, 70);
    }
    const result = new THREE.CanvasTexture(canvas);
    result.anisotropy = 4;
    return result;
  }, [value, color]);

  return (
    <Billboard position={[0, 0, 1.9]}>
      <mesh>
        <planeGeometry args={[1.0, 1.0]} />
        <meshBasicMaterial map={texture} transparent />
      </mesh>
    </Billboard>
  );
}

export function DiceTray({ roll, onComplete }: DiceTrayProps) {
  const [rolling, setRolling] = useState(true);
  const outcome = useMemo(() => outcomeOf(roll), [roll]);
  const style = OUTCOME_STYLE[outcome];
  const sides = primarySides(roll.expression);
  const face = roll.rolls[0] ?? roll.total;

  useEffect(() => {
    setRolling(true);
    const stop = setTimeout(() => setRolling(false), 1100);
    const done = setTimeout(onComplete, outcome === "normal" ? 1900 : 2700);
    return () => {
      clearTimeout(stop);
      clearTimeout(done);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roll.expression, roll.total]);

  return (
    <div className="absolute inset-0 z-40 flex items-center justify-center bg-black/75">
      <motion.div
        initial={{ opacity: 0, scale: 0.94 }}
        animate={{ opacity: 1, scale: 1 }}
        className="w-[min(92vw,460px)]"
      >
        <div className="panel-raised overflow-hidden p-1" style={{ borderColor: `${style.color}66` }}>
          <div className="h-56 w-full rounded-md bg-[#0c0a08]">
            <Canvas shadows camera={{ position: [0, 1.1, 3.4], fov: 42 }}>
              <ambientLight intensity={0.55} />
              <directionalLight position={[3, 5, 3]} intensity={1.5} castShadow />
              <Environment resolution={64}>
                <Lightformer intensity={1.6} position={[0, 3, 2]} scale={5} color={style.color} />
                <Lightformer intensity={0.8} position={[-3, -2, -2]} scale={5} color="#7C5CFF" />
              </Environment>
              <RollingDie sides={sides} color={style.color} rolling={rolling} />
              {!rolling && <ResultBadge value={face} color={style.color} />}
            </Canvas>
          </div>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.9 }}
          className="mt-3 text-center"
        >
          <div className="flex items-baseline justify-center gap-3">
            <span className="text-xs font-semibold uppercase tracking-[0.2em] text-muted">
              d{sides}
            </span>
            <span className="title-display text-4xl font-bold" style={{ color: style.color }}>
              {roll.total}
            </span>
          </div>
          <p className="mt-1 text-sm text-muted">
            {roll.expression} · [{roll.rolls.join(", ")}]
            {roll.modifier !== 0 && ` ${roll.modifier > 0 ? "+" : "−"} ${Math.abs(roll.modifier)}`}
          </p>
          {style.label && (
            <motion.p
              initial={{ scale: 0.8, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={{ delay: 1.1, type: "spring", stiffness: 320 }}
              className="title-display mt-2 text-xl font-extrabold tracking-widest"
              style={{ color: style.color }}
            >
              {style.label}
            </motion.p>
          )}
        </motion.div>
      </motion.div>
    </div>
  );
}
