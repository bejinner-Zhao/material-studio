"use client";

import { ErrorBlock, SkeletonGrid } from "@/components/Feedback";
import { TaskBoard } from "@/components/TaskBoard";
import { useMe, useTasks } from "@/lib/queries";
import { isActiveTask } from "@/lib/types";

export default function HistoryPage() {
  const me = useMe();
  const tasksQuery = useTasks(me.data?.id);
  const tasks = tasksQuery.data ?? [];
  const activeCount = tasks.filter(isActiveTask).length;

  return (
    <div className="space-y-4">
      <div className="space-y-1">
        <h1 className="text-lg font-semibold">我的记录</h1>
        <p className="text-sm text-muted" aria-live="polite">
          {tasksQuery.isPending
            ? "加载中…"
            : activeCount > 0
              ? `共 ${tasks.length} 条记录，${activeCount} 个任务进行中`
              : `共 ${tasks.length} 条记录`}
        </p>
      </div>

      {me.isError ? (
        <ErrorBlock message="无法确认当前身份，请刷新页面重试" />
      ) : tasksQuery.isPending ? (
        <SkeletonGrid count={4} />
      ) : tasksQuery.isError && tasks.length === 0 ? (
        <ErrorBlock
          message={
            tasksQuery.error instanceof Error
              ? `记录加载失败：${tasksQuery.error.message}`
              : "记录加载失败"
          }
          onRetry={() => void tasksQuery.refetch()}
        />
      ) : (
        <TaskBoard
          tasks={tasks}
          emptyText="还没有制作记录，先去挑一个模板吧"
          isError={tasksQuery.isError}
          onRetry={() => void tasksQuery.refetch()}
        />
      )}
    </div>
  );
}
