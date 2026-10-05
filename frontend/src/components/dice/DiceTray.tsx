import { useEffect, useMemo, useRef, useState } from "react";
import { Canvas, useFrame } from "@react-three/fiber";
import { Environment, Lightformer } from "@react-three/drei";
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
  normal: { color: "#B9AE97", label: "" },
};

function outcomeOf(roll: DiceRoll): Outcome {
  if (roll.natural20) return "crit";
  if (roll.natural1) return "fail";
  const allMax = roll.rolls.length > 0 && roll.rolls.every((value) => value === 20);
  return allMax ? "max" : "normal";
}

export function primarySides(expression: string): number {
  const match = /(\d*)d(\d+)/i.exec(expression ?? "");
  return match ? Number(match[2]) : 20;
}

interface Face {
  normal: THREE.Vector3;
  centroid: THREE.Vector3;
}

/** Builds the correct polyhedron for each die type. */
function buildGeometry(sides: number): THREE.BufferGeometry {
  switch (sides) {
    case 4:
      return new THREE.TetrahedronGeometry(1.25);
    case 6:
      return new THREE.BoxGeometry(1.5, 1.5, 1.5);
    case 8:
      return new THREE.OctahedronGeometry(1.35);
    case 10: {
      const up = new THREE.ConeGeometry(1.05, 1.25, 5, 1, true);
      const down = new THREE.ConeGeometry(1.05, 1.25, 5, 1, true);
      up.translate(0, 0.625, 0);
      down.rotateX(Math.PI);
      down.translate(0, -0.625, 0);
      return mergeGeometries(up, down);
    }
    case 12:
      return new THREE.DodecahedronGeometry(1.25);
    default:
      return new THREE.IcosahedronGeometry(1.25);
  }
}

/** Minimal merge so we avoid pulling in BufferGeometryUtils. */
function mergeGeometries(a: THREE.BufferGeometry, b: THREE.BufferGeometry): THREE.BufferGeometry {
  const an = a.toNonIndexed();
  const bn = b.toNonIndexed();
  const pa = an.getAttribute("position").array as Float32Array;
  const pb = bn.getAttribute("position").array as Float32Array;
  const merged = new Float32Array(pa.length + pb.length);
  merged.set(pa, 0);
  merged.set(pb, pa.length);
  const geometry = new THREE.BufferGeometry();
  geometry.setAttribute("position", new THREE.BufferAttribute(merged, 3));
  geometry.computeVertexNormals();
  return geometry;
}

/** Groups the triangles of a convex polyhedron into logical faces. */
function extractFaces(geometry: THREE.BufferGeometry): Face[] {
  const geo = geometry.index ? geometry.toNonIndexed() : geometry;
  const position = geo.getAttribute("position");
  const groups = new Map<string, { normal: THREE.Vector3; centroid: THREE.Vector3; count: number }>();

  for (let i = 0; i < position.count; i += 3) {
    const a = new THREE.Vector3().fromBufferAttribute(position, i);
    const b = new THREE.Vector3().fromBufferAttribute(position, i + 1);
    const c = new THREE.Vector3().fromBufferAttribute(position, i + 2);
    const normal = new THREE.Vector3()
      .subVectors(b, a)
      .cross(new THREE.Vector3().subVectors(c, a))
      .normalize();
    const centroid = new THREE.Vector3().add(a).add(b).add(c).divideScalar(3);
    const key = `${normal.x.toFixed(2)},${normal.y.toFixed(2)},${normal.z.toFixed(2)}`;
    const existing = groups.get(key);
    if (existing) {
      existing.centroid.add(centroid);
      existing.count += 1;
    } else {
      groups.set(key, { normal: normal.clone(), centroid, count: 1 });
    }
  }
  return [...groups.values()].map((group) => ({
    normal: group.normal,
    centroid: group.centroid.divideScalar(group.count),
  }));
}

const TEXTURE_CACHE = new Map<number, THREE.CanvasTexture>();

function numberTexture(value: number): THREE.CanvasTexture {
  const cached = TEXTURE_CACHE.get(value);
  if (cached) return cached;
  const canvas = document.createElement("canvas");
  canvas.width = 128;
  canvas.height = 128;
  const ctx = canvas.getContext("2d");
  if (ctx) {
    ctx.clearRect(0, 0, 128, 128);
    ctx.font = "bold 84px Inter, system-ui, sans-serif";
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.lineWidth = 10;
    ctx.strokeStyle = "rgba(10,8,6,0.85)";
    ctx.strokeText(String(value), 64, 70);
    ctx.fillStyle = "#f6efe1";
    ctx.fillText(String(value), 64, 70);
  }
  const texture = new THREE.CanvasTexture(canvas);
  texture.anisotropy = 4;
  TEXTURE_CACHE.set(value, texture);
  return texture;
}

function FaceNumber({ face, value }: { face: Face; value: number }) {
  const texture = useMemo(() => numberTexture(value), [value]);
  const quaternion = useMemo(
    () => new THREE.Quaternion().setFromUnitVectors(new THREE.Vector3(0, 0, 1), face.normal.clone().normalize()),
    [face],
  );
  const position = useMemo(
    () => face.centroid.clone().add(face.normal.clone().normalize().multiplyScalar(0.035)),
    [face],
  );
  return (
    <mesh position={position} quaternion={quaternion}>
      <planeGeometry args={[0.62, 0.62]} />
      <meshBasicMaterial map={texture} transparent depthWrite={false} />
    </mesh>
  );
}

function Die({ sides, color, faceValue, rolling }: { sides: number; color: string; faceValue: number; rolling: boolean }) {
  const group = useRef<THREE.Group>(null);
  const geometry = useMemo(() => buildGeometry(sides), [sides]);
  const faces = useMemo(() => extractFaces(geometry), [geometry]);
  const material = useMemo(
    () => new THREE.MeshStandardMaterial({ color, metalness: 0.2, roughness: 0.5, flatShading: true }),
    [color],
  );

  const target = useMemo(() => {
    const index = Math.min(faces.length - 1, Math.max(0, (faceValue - 1) % faces.length));
    const normal = faces[index]?.normal.clone().normalize() ?? new THREE.Vector3(0, 0, 1);
    const tilt = new THREE.Quaternion().setFromEuler(new THREE.Euler(0.22, -0.18, 0.05));
    return new THREE.Quaternion()
      .setFromUnitVectors(normal, new THREE.Vector3(0, 0, 1))
      .premultiply(tilt);
  }, [faces, faceValue]);

  useFrame((state, delta) => {
    if (!group.current) return;
    if (rolling) {
      group.current.rotation.x += delta * 9;
      group.current.rotation.y += delta * 7;
      group.current.rotation.z += delta * 5;
      group.current.position.y = Math.abs(Math.sin(state.clock.elapsedTime * 5)) * 0.55;
    } else {
      group.current.quaternion.slerp(target, 0.16);
      group.current.position.y = THREE.MathUtils.lerp(group.current.position.y, 0, 0.2);
    }
  });

  return (
    <group ref={group}>
      <mesh geometry={geometry} material={material} />
      {faces.map((face, index) => (
        <FaceNumber key={index} face={face} value={index + 1} />
      ))}
    </group>
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
    const stop = setTimeout(() => setRolling(false), 1000);
    const done = setTimeout(onComplete, outcome === "normal" ? 2200 : 3000);
    return () => {
      clearTimeout(stop);
      clearTimeout(done);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roll.expression, roll.total]);

  return (
    <div className="absolute inset-0 z-40 flex items-center justify-center bg-black/75">
      <motion.div initial={{ opacity: 0, scale: 0.94 }} animate={{ opacity: 1, scale: 1 }} className="w-[min(92vw,460px)]">
        <div className="panel-raised overflow-hidden p-1" style={{ borderColor: `${style.color}66` }}>
          <div className="h-60 w-full rounded-md bg-[#0c0a08]">
            <Canvas shadows camera={{ position: [0, 0.6, 3.6], fov: 42 }}>
              <ambientLight intensity={0.7} />
              <directionalLight position={[3, 5, 4]} intensity={1.5} castShadow />
              <directionalLight position={[-3, -2, -3]} intensity={0.5} />
              <Environment resolution={64}>
                <Lightformer intensity={1.4} position={[0, 3, 2]} scale={5} color={style.color} />
              </Environment>
              <Die sides={sides} color={style.color} faceValue={face} rolling={rolling} />
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
            <span className="text-xs font-semibold uppercase tracking-[0.2em] text-muted">d{sides}</span>
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
