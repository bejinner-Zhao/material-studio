"use client";

interface CategoryTabsProps {
  categories: string[];
  value: string | null;
  onChange: (category: string | null) => void;
}

export function CategoryTabs({ categories, value, onChange }: CategoryTabsProps) {
  const items: Array<{ key: string | null; label: string }> = [
    { key: null, label: "全部" },
    ...categories.map((category) => ({ key: category, label: category })),
  ];

  return (
    <div role="group" aria-label="模板分类" className="flex flex-wrap gap-2">
      {items.map((item) => {
        const active = item.key === value;
        return (
          <button
            key={item.label}
            type="button"
            aria-pressed={active}
            onClick={() => onChange(item.key)}
            className={`rounded-full border px-4 py-1.5 text-sm transition ${
              active
                ? "border-accent/60 bg-accentSoft text-accent"
                : "border-line bg-panel text-muted hover:border-white/25 hover:text-white"
            }`}
          >
            {item.label}
          </button>
        );
      })}
    </div>
  );
}
