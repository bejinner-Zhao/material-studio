import type { Task } from "@/lib/types";

import { TaskCard } from "./TaskCard";

interface TaskBoardProps {
  tasks: Task[];
  emptyText: string;
  /** 请求异常：只提示“取不到状态”，不代表任务失败 */
  isError?: boolean;
  onRetry?: () => void;
}

export function TaskBoard({ tasks, emptyText, isError, onRetry }: TaskBoardProps) {
  return (
    <div className="space-y-3">
      {isError ? (
        <div
          role="alert"
          className="flex flex-wrap items-center gap-2 rounded-xl border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-xs text-amber-200"
        >
          <span>暂时无法获取任务状态，任务本身不受影响。</span>
          {onRetry ? (
            <button
              type="button"
              onClick={onRetry}
              className="rounded-md border border-amber-300/40 px-2 py-1 font-medium text-amber-100 hover:bg-amber-400/10"
            >
              重新获取
            </button>
          ) : null}
        </div>
      ) : null}

      {tasks.length === 0 ? (
        <p className="rounded-xl border border-dashed border-line px-4 py-10 text-center text-sm text-muted">
          {emptyText}
        </p>
      ) : (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          {tasks.map((task) => (
            <TaskCard key={task.id} task={task} />
          ))}
        </div>
      )}
    </div>
  );
}
