import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./app/**/*.{ts,tsx}", "./components/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#0b0c0f",
        panel: "#14161b",
        panelSoft: "#1b1e25",
        line: "#262a33",
        muted: "#8b93a3",
        accent: "#b9c96a",
        accentSoft: "#3a4221",
        danger: "#e06666",
      },
      borderRadius: {
        card: "16px",
      },
    },
  },
  plugins: [],
};

export default config;
