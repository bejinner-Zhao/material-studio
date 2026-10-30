import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { TaskBoard } from "@/components/TaskBoard";
import type { Task } from "@/lib/types";

function makeTask(overrides: Partial<Task>): Task {
  return {
    id: 1,
    templateId: "demo-postcard",
    templateTitle: "旅行明信片",
    templateCoverUrl: "/demo/covers/postcard.svg",
    status: "PENDING",
    stage: null,
    errorStage: null,
    errorMessage: null,
    resultUrl: null,
    createdAt: "2026-10-29T15:20:11",
    startedAt: null,
    finishedAt: null,
    ...overrides,
  };
}

describe("TaskBoard", () => {
  it("多个任务各自展示自己的状态，互不串扰", () => {
    const tasks = [
      makeTask({ id: 1, status: "RUNNING", stage: "GENERATING" }),
      makeTask({ id: 2, status: "SUCCEEDED", resultUrl: "/files/results/2.svg" }),
      makeTask({
        id: 3,
        status: "FAILED",
        errorMessage: "模拟失败：模拟生成阶段被配置为失败",
        errorStage: "GENERATING",
      }),
    ];

    render(<TaskBoard tasks={tasks} emptyText="暂无记录" />);

    expect(screen.getByText("Processing…")).toBeInTheDocument();
    expect(screen.getByText("正在模拟生成")).toBeInTheDocument();

    const result = screen.getByAltText("旅行明信片 的制作结果");
    expect(result).toHaveAttribute("src", "/files/results/2.svg");

    expect(screen.getByText("制作失败")).toBeInTheDocument();
    expect(screen.getByText("模拟失败：模拟生成阶段被配置为失败")).toBeInTheDocument();
    expect(screen.getByText("失败于「模拟生成」")).toBeInTheDocument();
  });

  it("查询异常时只提示取不到状态，不会把进行中的任务标成失败", () => {
    const tasks = [makeTask({ id: 1, status: "RUNNING", stage: "MATERIAL_CHECK" })];
    const onRetry = vi.fn();

    render(<TaskBoard tasks={tasks} emptyText="暂无记录" isError onRetry={onRetry} />);

    expect(screen.getByRole("alert")).toHaveTextContent("暂时无法获取任务状态");
    expect(screen.queryByText("制作失败")).toBeNull();
    expect(screen.getByText("正在素材检查")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "重新获取" }));
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it("空列表展示占位文案", () => {
    render(<TaskBoard tasks={[]} emptyText="这个模板还没有制作记录" />);

    expect(screen.getByText("这个模板还没有制作记录")).toBeInTheDocument();
  });
});
