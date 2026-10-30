"use client";

import { useId } from "react";

import type { Template } from "@/lib/types";

export interface UploadSlotState {
  file?: File;
  previewUrl?: string;
  assetId?: number;
  status: "empty" | "uploading" | "ready" | "error";
  error?: string;
}

interface UploadSlotProps {
  index: number;
  slot: UploadSlotState;
  template: Template;
  disabled: boolean;
  onSelect: (file: File) => void;
  onRemove: () => void;
}

export function UploadSlot({
  index,
  slot,
  template,
  disabled,
  onSelect,
  onRemove,
}: UploadSlotProps) {
  const inputId = useId();
  const label = `上传第 ${index + 1} 张图片`;

  const fileInput = (
    <input
      id={inputId}
      type="file"
      accept={template.acceptedMimeTypes.join(",")}
      className="sr-only"
      disabled={disabled}
      aria-label={label}
      onChange={(event) => {
        const file = event.target.files?.[0];
        // 清空 value，保证再次选择同一个文件也会触发 change
        event.target.value = "";
        if (file) {
          onSelect(file);
        }
      }}
    />
  );

  if (slot.status === "ready" && slot.previewUrl) {
    return (
      <div className="group relative aspect-square overflow-hidden rounded-xl border border-line bg-panelSoft">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img src={slot.previewUrl} alt={`第 ${index + 1} 张素材预览`} className="h-full w-full object-cover" />
        <div className="absolute inset-x-0 bottom-0 flex items-center justify-between gap-2 bg-black/70 px-2 py-1.5 opacity-0 transition group-hover:opacity-100 group-focus-within:opacity-100">
          <label
            htmlFor={inputId}
            className="cursor-pointer rounded px-1.5 py-0.5 text-[11px] text-white/80 hover:bg-white/15"
          >
            替换
          </label>
          <button
            type="button"
            onClick={onRemove}
            className="rounded px-1.5 py-0.5 text-[11px] text-white/80 hover:bg-white/15"
          >
            移除
          </button>
        </div>
        {fileInput}
      </div>
    );
  }

  if (slot.status === "uploading") {
    return (
      <div
        aria-busy="true"
        className="relative aspect-square overflow-hidden rounded-xl border border-line bg-panelSoft"
      >
        {slot.previewUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={slot.previewUrl} alt="" className="h-full w-full object-cover opacity-40" />
        ) : null}
        <span className="absolute inset-0 flex items-center justify-center text-xs text-white/80">
          上传中…
        </span>
      </div>
    );
  }

  if (slot.status === "error") {
    return (
      <label
        htmlFor={inputId}
        className="flex aspect-square cursor-pointer flex-col items-center justify-center gap-1 rounded-xl border border-dashed border-danger/60 bg-danger/5 px-2 text-center hover:bg-danger/10"
      >
        <span className="text-xs text-danger">上传失败</span>
        <span className="line-clamp-2 text-[11px] text-white/50">{slot.error ?? "点击重试"}</span>
        {fileInput}
      </label>
    );
  }

  return (
    <label
      htmlFor={inputId}
      className="flex aspect-square cursor-pointer flex-col items-center justify-center gap-1.5 rounded-xl border border-dashed border-line bg-panelSoft/60 text-center transition hover:border-white/30 hover:bg-panelSoft"
    >
      <span aria-hidden className="text-lg text-white/40">
        +
      </span>
      <span className="text-xs text-white/70">点击上传</span>
      <span className="text-[11px] text-white/35">第 {index + 1} 张</span>
      {fileInput}
    </label>
  );
}
