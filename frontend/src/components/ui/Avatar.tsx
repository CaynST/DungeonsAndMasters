import { useMemo, useState } from "react";
import { cn } from "@/lib/utils";

export type AvatarSize = "xs" | "sm" | "md" | "lg" | "xl";

const SIZES: Record<AvatarSize, string> = {
  xs: "h-7 w-7 text-[10px]",
  sm: "h-10 w-10 text-sm",
  md: "h-12 w-12 text-base",
  lg: "h-16 w-16 text-xl",
  xl: "h-20 w-20 text-2xl",
};

/**
 * Warm fallback palette used when a character has no (or a broken) portrait. The colour is
 * derived from the name so the same hero keeps the same token colour across screens.
 */
const FALLBACK_COLORS = ["#C9A227", "#A88CFF", "#3E9C6E", "#B0364B", "#D8C39A", "#5B8DB8"];

function initials(name: string): string {
  const words = name.trim().split(/\s+/).filter(Boolean);
  if (words.length === 0) {
    return "?";
  }
  if (words.length === 1) {
    return words[0].charAt(0).toUpperCase();
  }
  return (words[0].charAt(0) + words[words.length - 1].charAt(0)).toUpperCase();
}

function colorFor(name: string): string {
  let hash = 0;
  for (const char of name) {
    hash = (hash * 31 + char.charCodeAt(0)) >>> 0;
  }
  return FALLBACK_COLORS[hash % FALLBACK_COLORS.length];
}

interface AvatarProps {
  /** Character, NPC or player display name; also drives the initials fallback. */
  name: string;
  /** Existing local portrait or uploaded image. Falls back to code-drawn initials on error. */
  src?: string | null;
  /** Character token colour; overrides the deterministic fallback colour. */
  color?: string;
  size?: AvatarSize;
  className?: string;
  /** Accessible label. Decorative by default because names are usually shown next to it. */
  alt?: string;
}

/**
 * Single visual source of truth for character/NPC avatars. It renders the same circular token
 * everywhere and degrades to initials when the portrait is missing or fails to load, so broken
 * image paths never leave an empty hole in the UI.
 */
export function Avatar({ name, src, color, size = "md", className, alt = "" }: AvatarProps) {
  const [failedSrc, setFailedSrc] = useState<string | null>(null);
  const accent = useMemo(() => color ?? colorFor(name), [color, name]);
  const showImage = Boolean(src) && failedSrc !== src;

  return (
    <span
      className={cn(
        "relative inline-flex shrink-0 items-center justify-center overflow-hidden rounded-full border-2 font-bold",
        SIZES[size],
        className,
      )}
      style={{
        borderColor: accent,
        color: accent,
        background: `radial-gradient(circle at 30% 22%, ${accent}33, transparent 70%), linear-gradient(160deg, #241a12, #14110e)`,
      }}
    >
      {showImage ? (
        <img
          src={src!}
          alt={alt}
          className="h-full w-full object-cover"
          onError={() => setFailedSrc(src ?? null)}
        />
      ) : (
        <span className="title-display select-none leading-none" aria-hidden={alt === "" ? true : undefined}>
          {initials(name)}
        </span>
      )}
    </span>
  );
}
