import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        background: "#0D0B09",
        surface: "#17130F",
        elevated: "#211A14",
        wood: "#2A1D14",
        parchment: "#D8C39A",
        ink: "#F0E9DC",
        muted: "#B3A793",
        gold: {
          DEFAULT: "#C9A227",
          soft: "#E0C56A",
          deep: "#8C6E1B",
        },
        crimson: {
          DEFAULT: "#B0364B",
          soft: "#E06A7A",
          deep: "#6E1F2D",
        },
        emerald: {
          DEFAULT: "#3E9C6E",
          soft: "#6FD3A0",
          deep: "#1F5A3E",
        },
        arcane: {
          DEFAULT: "#7C5CFF",
          soft: "#A88CFF",
          deep: "#3B2A80",
        },
      },
      fontFamily: {
        display: ["Sora", "system-ui", "sans-serif"],
        sans: ["Inter", "system-ui", "sans-serif"],
        epic: ["Cinzel", "Georgia", "serif"],
      },
      boxShadow: {
        panel: "0 12px 40px -12px rgba(0,0,0,0.8)",
        glow: "0 0 24px -4px rgba(201,162,39,0.45)",
      },
      keyframes: {
        shimmer: {
          "0%,100%": { opacity: "0.4" },
          "50%": { opacity: "1" },
        },
        flame: {
          "0%,100%": { transform: "scaleY(1) scaleX(1)", opacity: "0.9" },
          "50%": { transform: "scaleY(1.08) scaleX(0.96)", opacity: "1" },
        },
        float: {
          "0%,100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(-6px)" },
        },
        shake: {
          "0%,100%": { transform: "translateX(0)" },
          "25%": { transform: "translateX(-5px)" },
          "75%": { transform: "translateX(5px)" },
        },
      },
      animation: {
        shimmer: "shimmer 1.8s ease-in-out infinite",
        flame: "flame 0.8s ease-in-out infinite",
        float: "float 3s ease-in-out infinite",
        shake: "shake 0.35s ease-in-out",
      },
    },
  },
  plugins: [],
} satisfies Config;
