"use client";

import { useEffect, useState } from "react";

import { CategoryTabs } from "@/components/CategoryTabs";
import { EmptyBlock, ErrorBlock, SkeletonGrid } from "@/components/Feedback";
import { TemplateCard } from "@/components/TemplateCard";
import { useTemplates } from "@/lib/queries";

export default function TemplatesPage() {
  const [category, setCategory] = useState<string | null>(null);
  const templatesQuery = useTemplates(category);

  // 切换分类会换 queryKey，新缓存没有 data。分类本身与筛选无关，单独留存避免标签闪烁
  const [categories, setCategories] = useState<string[]>([]);
  useEffect(() => {
    if (templatesQuery.data) {
      setCategories(templatesQuery.data.categories);
    }
  }, [templatesQuery.data]);

  return (
    <div className="space-y-5">
      <div className="space-y-1">
        <h1 className="text-lg font-semibold">模板</h1>
        <p className="text-sm text-muted">选择一个模板，上传图片后即可开始制作</p>
      </div>

      <CategoryTabs categories={categories} value={category} onChange={setCategory} />

      {templatesQuery.isPending ? (
        <SkeletonGrid />
      ) : templatesQuery.isError ? (
        <ErrorBlock
          message={
            templatesQuery.error instanceof Error
              ? `模板加载失败：${templatesQuery.error.message}`
              : "模板加载失败"
          }
          onRetry={() => void templatesQuery.refetch()}
        />
      ) : templatesQuery.data.templates.length === 0 ? (
        <EmptyBlock message="该分类下暂时没有模板" />
      ) : (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
          {templatesQuery.data.templates.map((template) => (
            <TemplateCard key={template.id} template={template} />
          ))}
        </div>
      )}
    </div>
  );
}
