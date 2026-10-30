import { formatDateTime } from "@/lib/format";
import { STAGE_LABELS, type Task, type TaskStatus } from "@/lib/types";

const STATUS_TEXT: Record<TaskStatus, string> = {
  PENDING: "等待处理",
  RUNNING: "处理中",
  SUCCEEDED: "已完成",
  FAILED: "制作失败",
};

const STATUS_STYLE: Record<TaskStatus, string> = {
  PENDING: "bg-white/10 text-white/70",
  RUNNING: "bg-accentSoft text-accent",
  SUCCEEDED: "bg-emerald-500/15 text-emerald-300",
  FAILED: "bg-danger/15 text-danger",
};

export function TaskCard({ task }: { task: Task }) {
  return (
    <article className="overflow-hidden rounded-card border border-line bg-panel">
      <div className="relative aspect-[3/4] bg-panelSoft">
        {renderBody(task)}
        <span
          className={`absolute left-2 top-2 rounded-full px-2 py-0.5 text-[11px] ${STATUS_STYLE[task.status]}`}
        >
          {STATUS_TEXT[task.status]}
        </span>
      </div>
      <div className="flex items-center justify-between gap-2 px-3 py-2">
        <span className="truncate text-xs text-white/55">{formatDateTime(task.createdAt)}</span>
        <span className="truncate text-xs text-white/40">{task.templateTitle}</span>
      </div>
    </article>
  );
}

function renderBody(task: Task) {
  if (task.status === "SUCCEEDED" && task.resultUrl) {
    return (
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={task.resultUrl}
        alt={`${task.templateTitle} 的制作结果`}
        className="h-full w-full object-cover"
        loading="lazy"
      />
    );
  }

  if (task.status === "FAILED") {
    return (
      <div className="flex h-full flex-col items-center justify-center gap-2 px-4 text-center">
        <span className="line-clamp-3 text-xs text-danger">
          {task.errorMessage ?? "未知原因"}
        </span>
        {task.errorStage ? (
          <span className="text-[11px] text-white/35">
            失败于「{STAGE_LABELS[task.errorStage]}」
          </span>
        ) : null}
      </div>
    );
  }

  return (
    <div className="flex h-full flex-col items-center justify-center gap-2 text-center">
      <span className="animate-pulse text-sm text-white/75">Processing…</span>
      <span className="text-xs text-white/45">
        {task.status === "PENDING"
          ? "排队等待处理"
          : task.stage
            ? `正在${STAGE_LABELS[task.stage]}`
            : "准备开始"}
      </span>
    </div>
  );
}
