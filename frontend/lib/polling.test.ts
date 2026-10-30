import { describe, expect, it } from "vitest";

import { POLL_INTERVAL_MS, pollingInterval } from "@/lib/polling";

describe("pollingInterval", () => {
  it("有等待或处理中的任务时继续轮询", () => {
    expect(pollingInterval([{ status: "PENDING" }])).toBe(POLL_INTERVAL_MS);
    expect(pollingInterval([{ status: "SUCCEEDED" }, { status: "RUNNING" }])).toBe(
      POLL_INTERVAL_MS,
    );
  });

  it("全部进入终态后停止轮询", () => {
    expect(pollingInterval([{ status: "SUCCEEDED" }, { status: "FAILED" }])).toBe(false);
  });

  it("没有任务数据时不轮询", () => {
    expect(pollingInterval([])).toBe(false);
    expect(pollingInterval(undefined)).toBe(false);
  });
});
