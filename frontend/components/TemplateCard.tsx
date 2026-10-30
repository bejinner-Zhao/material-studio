import Link from "next/link";

import type { Template } from "@/lib/types";

export function TemplateCard({ template }: { template: Template }) {
  return (
    <Link
      href={`/templates/${template.id}`}
      className="group block overflow-hidden rounded-card border border-line bg-panel transition hover:border-white/25"
    >
      <div className="relative aspect-[4/5] overflow-hidden bg-panelSoft">
        {/* 服务端返回的是本地 SVG 示例图，不确定尺寸，直接用 img 避免额外的图片配置 */}
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src={template.coverUrl}
          alt={`${template.title} 模板封面`}
          className="h-full w-full object-cover transition duration-300 group-hover:scale-[1.03]"
          loading="lazy"
        />
        <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/85 via-black/30 to-transparent p-3">
          <p className="text-sm font-medium">{template.title}</p>
          <p className="mt-0.5 text-xs text-white/60">
            {template.category} · 需 {template.requiredImageCount} 张图
          </p>
        </div>
      </div>
    </Link>
  );
}
