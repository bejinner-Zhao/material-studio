"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";

import { ErrorBlock } from "@/components/Feedback";
import { TaskBoard } from "@/components/TaskBoard";
import { UploadSlot, type UploadSlotState } from "@/components/UploadSlot";
import { ApiError } from "@/lib/api";
import { formatMegabytes, formatMime } from "@/lib/format";
import {
  deleteAsset,
  uploadAsset,
  useCreateTask,
  useMe,
  useTasks,
  useTemplate,
} from "@/lib/queries";
import type { Template } from "@/lib/types";

function emptySlots(count: number): UploadSlotState[] {
  return Array.from({ length: count }, () => ({ status: "empty" as const }));
}

function validateFile(file: File, template: Template): string | null {
  if (!template.acceptedMimeTypes.includes(file.type)) {
    return `仅支持 ${template.acceptedMimeTypes.map(formatMime).join(" / ")}`;
  }
  if (file.size > template.maxFileSizeBytes) {
    return `单张图片不能超过 ${formatMegabytes(template.maxFileSizeBytes)}`;
  }
  return null;
}

export default function TemplateDetailPage() {
  const params = useParams<{ id: string }>();
  const templateId = params.id ?? "";

  const templateQuery = useTemplate(templateId);
  const me = useMe();
  const tasksQuery = useTasks(me.data?.id);
  const createTask = useCreateTask(me.data?.id);

  const template = templateQuery.data;
  const [slots, setSlots] = useState<UploadSlotState[]>([]);

  // 上传是异步回调，需要读到最新的槽位状态
  const slotsRef = useRef<UploadSlotState[]>([]);
  slotsRef.current = slots;

  const requiredCount = template?.requiredImageCount;
  const loadedTemplateId = template?.id;

  useEffect(() => {
    if (!loadedTemplateId || !requiredCount) {
      return;
    }
    setSlots(emptySlots(requiredCount));
  }, [loadedTemplateId, requiredCount]);

  // 离开页面时释放本地预览地址
  useEffect(() => {
    return () => {
      slotsRef.current.forEach((slot) => {
        if (slot.previewUrl) {
          URL.revokeObjectURL(slot.previewUrl);
        }
      });
    };
  }, []);

  const updateSlot = useCallback((index: number, patch: Partial<UploadSlotState>) => {
    setSlots((current) =>
      current.map((slot, position) => (position === index ? { ...slot, ...patch } : slot)),
    );
  }, []);

  const handleSelect = useCallback(
    async (index: number, file: File) => {
      if (!template) {
        return;
      }
      const validationError = validateFile(file, template);
      if (validationError) {
        updateSlot(index, {
          status: "error",
          error: validationError,
          file: undefined,
          previewUrl: undefined,
          assetId: undefined,
        });
        return;
      }

      const previous = slotsRef.current[index];
      const previousAssetId = previous?.assetId;
      if (previous?.previewUrl) {
        URL.revokeObjectURL(previous.previewUrl);
      }

      const previewUrl = URL.createObjectURL(file);
      updateSlot(index, {
        status: "uploading",
        file,
        previewUrl,
        assetId: undefined,
        error: undefined,
      });

      try {
        const asset = await uploadAsset(template.id, file);
        // 上传期间用户又换了这一格：丢弃本次结果，避免张冠李戴
        if (slotsRef.current[index]?.previewUrl !== previewUrl) {
          void deleteAsset(asset.id).catch(() => undefined);
          return;
        }
        updateSlot(index, { status: "ready", assetId: asset.id });
        if (previousAssetId) {
          void deleteAsset(previousAssetId).catch(() => undefined);
        }
      } catch (error) {
        updateSlot(index, {
          status: "error",
          error: error instanceof Error ? error.message : "上传失败，请重试",
        });
      }
    },
    [template, updateSlot],
  );

  const handleRemove = useCallback(
    (index: number) => {
      const slot = slotsRef.current[index];
      if (slot?.assetId) {
        void deleteAsset(slot.assetId).catch(() => undefined);
      }
      if (slot?.previewUrl) {
        URL.revokeObjectURL(slot.previewUrl);
      }
      updateSlot(index, {
        status: "empty",
        file: undefined,
        previewUrl: undefined,
        assetId: undefined,
        error: undefined,
      });
    },
    [updateSlot],
  );

  const uploading = slots.some((slot) => slot.status === "uploading");
  const readyCount = slots.filter((slot) => slot.status === "ready").length;
  const allReady = slots.length > 0 && readyCount === slots.length;
  const canSubmit = allReady && !uploading && !createTask.isPending;

  const handleSubmit = () => {
    if (!template || !canSubmit) {
      return;
    }
    const assetIds = slots
      .map((slot) => slot.assetId)
      .filter((assetId): assetId is number => typeof assetId === "number");

    createTask.mutate(
      { templateId: template.id, assetIds },
      {
        onSuccess: () => {
          slotsRef.current.forEach((slot) => {
            if (slot.previewUrl) {
              URL.revokeObjectURL(slot.previewUrl);
            }
          });
          setSlots(emptySlots(template.requiredImageCount));
        },
      },
    );
  };

  if (templateQuery.isPending) {
    return <div className="h-72 animate-pulse rounded-card bg-panel" aria-hidden />;
  }

  if (templateQuery.isError) {
    const notFound =
      templateQuery.error instanceof ApiError && templateQuery.error.status === 404;
    return (
      <ErrorBlock
        message={notFound ? "模板不存在或已下线" : "模板加载失败，请稍后重试"}
        onRetry={notFound ? undefined : () => void templateQuery.refetch()}
      />
    );
  }

  if (!template) {
    return <ErrorBlock message="模板不存在" />;
  }

  const templateTasks = (tasksQuery.data ?? []).filter((task) => task.templateId === template.id);

  return (
    <div className="grid gap-5 lg:grid-cols-[minmax(0,360px)_minmax(0,1fr)]">
      <section className="card-surface flex flex-col gap-4 p-4">
        <Link href="/" className="text-xs text-muted hover:text-white">
          ‹ 返回模板列表
        </Link>

        <div className="overflow-hidden rounded-xl bg-panelSoft">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src={template.coverUrl}
            alt={`${template.title} 模板封面`}
            className="aspect-[4/5] w-full object-cover"
          />
        </div>

        <div className="space-y-1">
          <h1 className="text-base font-semibold">{template.title}</h1>
          <p className="text-xs leading-relaxed text-muted">{template.description}</p>
          <p className="text-[11px] text-white/35">
            需要 {template.requiredImageCount} 张 ·{" "}
            {template.acceptedMimeTypes.map(formatMime).join(" / ")} · 单张 ≤{" "}
            {formatMegabytes(template.maxFileSizeBytes)}
          </p>
        </div>

        <div className="space-y-2">
          <p className="text-xs font-medium text-white/70">Upload</p>
          <div className="grid grid-cols-2 gap-2">
            {slots.map((slot, index) => (
              <UploadSlot
                key={index}
                index={index}
                slot={slot}
                template={template}
                disabled={uploading || createTask.isPending}
                onSelect={(file) => void handleSelect(index, file)}
                onRemove={() => handleRemove(index)}
              />
            ))}
          </div>
          <p className="text-[11px] text-white/40" aria-live="polite">
            {uploading
              ? "正在上传素材…"
              : allReady
                ? "素材已就绪，可以开始制作"
                : `还需上传 ${slots.length - readyCount} 张`}
          </p>
        </div>

        <button
          type="button"
          onClick={handleSubmit}
          disabled={!canSubmit}
          className="mt-auto w-full rounded-xl bg-accent px-4 py-2.5 text-sm font-semibold text-black transition hover:brightness-105 disabled:cursor-not-allowed disabled:bg-panelSoft disabled:text-white/35"
        >
          {createTask.isPending ? "提交中…" : "Create Now"}
        </button>

        {createTask.isError ? (
          <p role="alert" className="text-xs text-danger">
            {createTask.error instanceof Error ? createTask.error.message : "提交失败，请重试"}
          </p>
        ) : null}
      </section>

      <section className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-medium">History</h2>
          <button
            type="button"
            onClick={() => void tasksQuery.refetch()}
            className="rounded-lg border border-line px-2.5 py-1 text-xs text-muted hover:border-white/30 hover:text-white"
          >
            手动刷新
          </button>
        </div>

        <TaskBoard
          tasks={templateTasks}
          emptyText={me.isError ? "无法获取任务状态，请刷新页面" : "这个模板还没有制作记录"}
          isError={tasksQuery.isError || me.isError}
          onRetry={() => void tasksQuery.refetch()}
        />
      </section>
    </div>
  );
}
