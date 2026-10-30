"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { api } from "@/lib/api";
import { pollingInterval } from "@/lib/polling";
import type {
  Me,
  Task,
  Template,
  TemplateCatalog,
  UploadedAsset,
} from "@/lib/types";

export const queryKeys = {
  me: ["me"] as const,
  templates: (category: string | null) => ["templates", category] as const,
  template: (id: string) => ["template", id] as const,
  tasks: (userId: number | undefined) => ["tasks", userId] as const,
};

export function useMe() {
  return useQuery({
    queryKey: queryKeys.me,
    queryFn: () => api.get<Me>("/api/me"),
    staleTime: 5 * 60 * 1000,
  });
}

export function useSwitchIdentity() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => api.post<Me>("/api/me/reset", {}),
    onSuccess: () => {
      // 切换后旧缓存全部属于上一个身份，直接清空并重载页面，
      // 让所有在途请求与轮询随页面一起结束，不会出现旧数据覆盖新用户
      queryClient.clear();
      window.location.reload();
    },
  });
}

export function useTemplates(category: string | null) {
  const suffix = category ? `?category=${encodeURIComponent(category)}` : "";
  return useQuery({
    queryKey: queryKeys.templates(category),
    queryFn: () => api.get<TemplateCatalog>(`/api/templates${suffix}`),
    staleTime: 60 * 1000,
  });
}

export function useTemplate(id: string) {
  return useQuery({
    queryKey: queryKeys.template(id),
    queryFn: () => api.get<Template>(`/api/templates/${id}`),
    retry: 0,
    enabled: id.length > 0,
  });
}

/**
 * 任务列表同时承担「历史记录」与「轮询」两个职责：
 * 只要缓存里还有未结束的任务就 2 秒拉一次，全部结束立刻停止。
 * 查询失败时 TanStack Query 保留上一次成功的数据，界面不会把任务误标为失败。
 */
export function useTasks(userId: number | undefined) {
  return useQuery({
    queryKey: queryKeys.tasks(userId),
    queryFn: () => api.get<Task[]>("/api/tasks?page=1&size=50"),
    enabled: userId !== undefined,
    refetchInterval: (query) => pollingInterval(query.state.data),
    refetchIntervalInBackground: false,
  });
}

export function useCreateTask(userId: number | undefined) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: { templateId: string; assetIds: number[] }) =>
      api.post<Task>("/api/tasks", input),
    onSuccess: (task) => {
      // 立刻把受理结果插到列表头部，按钮随之恢复，历史区马上出现「处理中」
      queryClient.setQueryData<Task[]>(queryKeys.tasks(userId), (current) =>
        current ? [task, ...current.filter((item) => item.id !== task.id)] : [task],
      );
    },
  });
}

export async function uploadAsset(templateId: string, file: File): Promise<UploadedAsset> {
  const form = new FormData();
  form.append("templateId", templateId);
  form.append("file", file);
  return api.postForm<UploadedAsset>("/api/uploads", form);
}

export async function deleteAsset(assetId: number): Promise<void> {
  await api.delete(`/api/uploads/${assetId}`);
}
