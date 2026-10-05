import { type ButtonHTMLAttributes, forwardRef } from "react";
import { cn } from "@/lib/utils";
import { Spinner } from "@/components/ui/Spinner";

type Variant = "primary" | "secondary" | "ghost" | "danger" | "arcane";
type Size = "sm" | "md" | "lg";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
}

const variants: Record<Variant, string> = {
  primary:
    "bg-gradient-to-b from-gold-soft to-gold text-[#241a06] font-semibold hover:brightness-110 shadow-glow",
  secondary:
    "bg-elevated text-ink border border-parchment/15 hover:border-gold/40 hover:bg-wood/70",
  ghost: "bg-transparent text-muted hover:text-ink hover:bg-white/5",
  danger: "bg-crimson text-white hover:bg-crimson-soft",
  arcane: "bg-arcane text-white hover:bg-arcane-soft shadow-[0_0_20px_-4px_rgba(124,92,255,0.6)]",
};

const sizes: Record<Size, string> = {
  sm: "h-8 px-3 text-xs rounded-md",
  md: "h-10 px-4 text-sm rounded-lg",
  lg: "h-12 px-6 text-base rounded-lg",
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = "secondary", size = "md", loading = false, disabled, children, ...props }, ref) => (
    <button
      ref={ref}
      disabled={disabled || loading}
      className={cn(
        "inline-flex items-center justify-center gap-2 font-medium tracking-wide transition-all",
        "disabled:cursor-not-allowed disabled:opacity-40 active:scale-[0.98]",
        variants[variant],
        sizes[size],
        className,
      )}
      {...props}
    >
      {loading && <Spinner className="h-4 w-4" />}
      {children}
    </button>
  ),
);
Button.displayName = "Button";
