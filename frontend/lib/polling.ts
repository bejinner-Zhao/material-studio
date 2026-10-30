import type { TaskStatus } from "@/lib/types";

export const POLL_INTERVAL_MS = 2000;

export function isActiveStatus(status: TaskStatus): boolean {
  return status === "PENDING" || status === "RUNNING";
}

/**
 * 轮询策略：只要还有未结束的任务就继续拉取，全部进入终态后停止。
 * 抽成纯函数，便于单独测试而不依赖 React Query。
 */
export function pollingInterval(
  tasks: ReadonlyArray<{ status: TaskStatus }> | undefined,
): number | false {
  if (!tasks || tasks.length === 0) {
    return false;
  }
  return tasks.some((task) => isActiveStatus(task.status)) ? POLL_INTERVAL_MS : false;
}
