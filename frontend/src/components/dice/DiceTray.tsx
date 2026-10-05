import { useEffect, useMemo, useRef, useState } from "react";
import { Canvas, useFrame } from "@react-three/fiber";
import { Environment, Lightformer, RoundedBox } from "@react-three/drei";
import { motion } from "framer-motion";
import * as THREE from "three";
import type { DiceRoll } from "@/types/game";

interface DiceTrayProps {
  roll: DiceRoll;
  onComplete: () => void;
}

type Outcome = "crit" | "fail" | "normal" | "max";

function outcomeOf(roll: DiceRoll): Outcome {
  if (roll.natural20) return "crit";
  if (roll.natural1) return "fail";
  const allMax = roll.rolls.length > 0 && roll.rolls.every((value) => value === 20);
  return allMax ? "max" : "normal";
}

const OUTCOME_STYLE: Record<Outcome, { color: string; glow: string; label: string }> = {
  crit: { color: "#C9A227", glow: "#E0C56A", label: "¡CRÍTICO!" },
  fail: { color: "#B0364B", glow: "#E06A7A", label: "FALLO CRÍTICO" },
  max: { color: "#7C5CFF", glow: "#A88CFF", label: "MÁXIMO" },
  normal: { color: "#E8E1D5", glow: "#D8C39A", label: "" },
};

// Pip layouts for a standard d6, in local face space.
const PIP_LAYOUTS: Record<number, [number, number][]> = {
  1: [[0, 0]],
  2: [[-0.32, 0.32], [0.32, -0.32]],
  3: [[-0.32, 0.32], [0, 0], [0.32, -0.32]],
  4: [[-0.32, 0.32], [0.32, 0.32], [-0.32, -0.32], [0.32, -0.32]],
  5: [[-0.32, 0.32], [0.32, 0.32], [0, 0], [-0.32, -0.32], [0.32, -0.32]],
  6: [[-0.32, 0.32], [0.32, 0.32], [-0.32, 0], [0.32, 0], [-0.32, -0.32], [0.32, -0.32]],
};

/** Rotations that bring each pip face toward the camera, keyed by the shown value. */
const FACE_ROTATION: Record<number, [number, number, number]> = {
  1: [0, 0, 0],
  2: [0, Math.PI / 2, 0],
  3: [0, 0, Math.PI / 2],
  4: [0, 0, -Math.PI / 2],
  5: [0, -Math.PI / 2, 0],
  6: [Math.PI, 0, 0],
};

function Die({ face, color, rolling }: { face: number; color: string; rolling: boolean }) {
  const mesh = useRef<THREE.Group>(null);
  const [target] = useState(
    () => new THREE.Euler(...(FACE_ROTATION[Math.min(6, Math.max(1, face))] ?? [0, 0, 0])),
  );

  useFrame((state, delta) => {
    if (!mesh.current) return;
    if (rolling) {
      mesh.current.rotation.x += delta * 9;
      mesh.current.rotation.y += delta * 7;
      mesh.current.rotation.z += delta * 5;
      mesh.current.position.y = 0.2 + Math.abs(Math.sin(state.clock.elapsedTime * 4)) * 0.6;
    } else {
      mesh.current.rotation.x = THREE.MathUtils.lerp(mesh.current.rotation.x, target.x, 0.15);
      mesh.current.rotation.y = THREE.MathUtils.lerp(mesh.current.rotation.y, target.y, 0.15);
      mesh.current.rotation.z = THREE.MathUtils.lerp(mesh.current.rotation.z, target.z, 0.15);
      mesh.current.position.y = THREE.MathUtils.lerp(mesh.current.position.y, 0, 0.15);
    }
  });

  const pips = PIP_LAYOUTS[Math.min(6, Math.max(1, face))] ?? PIP_LAYOUTS[1];

  return (
    <group ref={mesh}>
      <RoundedBox args={[1.2, 1.2, 1.2]} radius={0.12} smoothness={4} castShadow>
        <meshStandardMaterial color={color} metalness={0.45} roughness={0.3} />
      </RoundedBox>
      {/* Front (value) face pips, nudged slightly forward. */}
      <group position={[0, 0, 0.615]}>
        {pips.map(([px, py], index) => (
          <mesh key={index} position={[px, py, 0]}>
            <circleGeometry args={[0.08, 16]} />
            <meshStandardMaterial color="#1a140d" />
          </mesh>
        ))}
      </group>
    </group>
  );
}

export function DiceTray({ roll, onComplete }: DiceTrayProps) {
  const [rolling, setRolling] = useState(true);
  const outcome = useMemo(() => outcomeOf(roll), [roll]);
  const style = OUTCOME_STYLE[outcome];
  const primaryFace = roll.rolls[0] ?? roll.total;

  useEffect(() => {
    setRolling(true);
    const stop = setTimeout(() => setRolling(false), 1200);
    const done = setTimeout(onComplete, outcome === "normal" ? 2000 : 2900);
    return () => {
      clearTimeout(stop);
      clearTimeout(done);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roll.expression, roll.total]);

  return (
    <div className="absolute inset-0 z-40 flex items-center justify-center bg-black/70 backdrop-blur-sm">
      <motion.div
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        className="relative flex w-[min(92vw,540px)] flex-col items-center"
      >
        <div
          className="w-full overflow-hidden rounded-2xl border p-1"
          style={{ borderColor: `${style.color}88`, boxShadow: `0 0 60px -10px ${style.glow}` }}
        >
          <div className="h-64 w-full rounded-xl bg-gradient-to-b from-[#17130F] to-[#0D0B09]">
            <Canvas shadows camera={{ position: [0, 1.4, 3.4], fov: 45 }}>
              <ambientLight intensity={0.5} />
              <directionalLight position={[3, 5, 3]} intensity={1.4} castShadow />
              <Environment resolution={64}>
                <Lightformer intensity={2} position={[0, 3, 2]} scale={5} color={style.glow} />
                <Lightformer intensity={1} position={[-3, -2, -2]} scale={5} color="#7C5CFF" />
              </Environment>
              <Die face={primaryFace} color={style.color} rolling={rolling} />
            </Canvas>
          </div>
        </div>

        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 1.2 }}
          className="mt-4 text-center"
        >
          <p className="title-display text-4xl font-bold" style={{ color: style.color }}>
            {roll.total}
          </p>
          <p className="mt-1 text-sm text-muted">
            {roll.expression} · [{roll.rolls.join(", ")}]
            {roll.modifier !== 0 && ` ${roll.modifier > 0 ? "+" : "−"} ${Math.abs(roll.modifier)}`}
          </p>
          {style.label && (
            <motion.p
              initial={{ scale: 0.7, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={{ delay: 1.4, type: "spring", stiffness: 300 }}
              className="title-display mt-3 text-2xl font-black tracking-widest text-glow-gold"
              style={{ color: style.glow }}
            >
              {style.label}
            </motion.p>
          )}
        </motion.div>
      </motion.div>
    </div>
  );
}
