"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

import { useMe, useSwitchIdentity } from "@/lib/queries";

const NAV = [
  { href: "/", label: "模板" },
  { href: "/history", label: "我的记录" },
];

export function SiteHeader() {
  const pathname = usePathname();
  const me = useMe();
  const switchIdentity = useSwitchIdentity();

  return (
    <header className="sticky top-0 z-10 border-b border-line bg-ink/85 backdrop-blur">
      <div className="mx-auto flex w-full max-w-6xl flex-wrap items-center gap-x-6 gap-y-2 px-4 py-3">
        <Link href="/" className="text-base font-semibold tracking-tight">
          Material<span className="text-accent">Studio</span>
        </Link>

        <nav aria-label="主导航" className="flex items-center gap-1 text-sm">
          {NAV.map((item) => {
            const active = item.href === "/" ? pathname === "/" : pathname.startsWith(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                aria-current={active ? "page" : undefined}
                className={`whitespace-nowrap rounded-full px-3 py-1.5 transition ${
                  active ? "bg-panelSoft text-white" : "text-muted hover:text-white"
                }`}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="ml-auto flex items-center gap-3 whitespace-nowrap">
          <p className="text-sm text-muted" title="匿名身份保存在 HttpOnly Cookie 中">
            {me.data ? me.data.nickname : "加载中…"}
          </p>
          <button
            type="button"
            onClick={() => switchIdentity.mutate()}
            disabled={switchIdentity.isPending || me.isPending}
            title="生成一个新的匿名身份。原身份的历史记录仍保留在数据库中，只是当前浏览器不再访问"
            className="whitespace-nowrap rounded-lg border border-line px-2.5 py-1 text-xs text-muted transition hover:border-white/30 hover:text-white disabled:cursor-not-allowed disabled:opacity-50"
          >
            {switchIdentity.isPending ? "切换中…" : "切换身份"}
          </button>
        </div>
      </div>
    </header>
  );
}
