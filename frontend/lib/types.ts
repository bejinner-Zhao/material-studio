export type TaskStatus = "PENDING" | "RUNNING" | "SUCCEEDED" | "FAILED";

export type TaskStage = "MATERIAL_CHECK" | "GENERATING" | "FINALIZING";

export interface Me {
  id: number;
  nickname: string;
}

export interface Template {
  id: string;
  title: string;
  category: string;
  coverUrl: string;
  description: string;
  requiredImageCount: number;
  acceptedMimeTypes: string[];
  maxFileSizeBytes: number;
}

export interface TemplateCatalog {
  categories: string[];
  templates: Template[];
}

export interface UploadedAsset {
  id: number;
  url: string;
  mimeType: string;
  sizeBytes: number;
  originalName: string;
}

export interface Task {
  id: number;
  templateId: string;
  templateTitle: string;
  templateCoverUrl: string;
  status: TaskStatus;
  stage: TaskStage | null;
  errorStage: TaskStage | null;
  errorMessage: string | null;
  resultUrl: string | null;
  createdAt: string;
  startedAt: string | null;
  finishedAt: string | null;
}

export const STAGE_LABELS: Record<TaskStage, string> = {
  MATERIAL_CHECK: "素材检查",
  GENERATING: "模拟生成",
  FINALIZING: "结果整理",
};

export function isActiveTask(task: Task): boolean {
  return task.status === "PENDING" || task.status === "RUNNING";
}
