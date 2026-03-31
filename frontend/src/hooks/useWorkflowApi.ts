import { useQuery, useMutation, useQueryClient, UseQueryOptions, UseMutationOptions } from '@tanstack/react-query';
import { workflowApi, executionApi, executorApi, Workflow, WorkflowExecution, Executor } from '../api/workflowApi';

// ==================== Query Keys ====================

export const workflowKeys = {
  all: ['workflows'] as const,
  lists: () => [...workflowKeys.all, 'list'] as const,
  list: (params: { published?: boolean; createdBy?: string }) => [...workflowKeys.lists(), params] as const,
  details: () => [...workflowKeys.all, 'detail'] as const,
  detail: (id: string) => [...workflowKeys.details(), id] as const,
};

export const executionKeys = {
  all: ['executions'] as const,
  lists: () => [...executionKeys.all, 'list'] as const,
  history: (userId: string, limit: number) => [...executionKeys.lists(), 'history', userId, limit] as const,
  details: () => [...executionKeys.all, 'detail'] as const,
  detail: (id: string) => [...executionKeys.details(), id] as const,
};

export const executorKeys = {
  all: ['executors'] as const,
  lists: () => [...executorKeys.all, 'list'] as const,
  list: () => [...executorKeys.lists()] as const,
};

// ==================== Workflow Hooks ====================

/**
 * 获取工作流列表
 */
export function useWorkflows(params?: { published?: boolean; createdBy?: string }, options?: UseQueryOptions<Workflow[], Error>) {
  return useQuery({
    queryKey: workflowKeys.list(params || {}),
    queryFn: () => workflowApi.listWorkflows(params),
    ...options,
  });
}

/**
 * 获取工作流详情
 */
export function useWorkflow(id: string | null, options?: UseQueryOptions<Workflow, Error>) {
  return useQuery({
    queryKey: workflowKeys.detail(id!),
    queryFn: () => workflowApi.getWorkflow(id!),
    enabled: !!id,
    ...options,
  });
}

/**
 * 创建工作流
 */
export function useCreateWorkflow(options?: UseMutationOptions<Workflow, Error, Omit<Workflow, 'id' | 'createdAt' | 'updatedAt'>>) {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: workflowApi.createWorkflow,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: workflowKeys.all });
      options?.onSuccess?.(data);
    },
    ...options,
  });
}

/**
 * 更新工作流
 */
export function useUpdateWorkflow(options?: UseMutationOptions<Workflow, Error, { id: string; workflow: Partial<Workflow> }>) {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ id, workflow }) => workflowApi.updateWorkflow(id, workflow),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: workflowKeys.detail(variables.id) });
      queryClient.invalidateQueries({ queryKey: workflowKeys.all });
      options?.onSuccess?.(data, variables);
    },
    ...options,
  });
}

/**
 * 删除工作流
 */
export function useDeleteWorkflow(options?: UseMutationOptions<void, Error, string>) {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: workflowApi.deleteWorkflow,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: workflowKeys.all });
      queryClient.removeQueries({ queryKey: workflowKeys.detail(variables) });
      options?.onSuccess?.(data, variables);
    },
    ...options,
  });
}

/**
 * 执行工作流
 */
export function useExecuteWorkflow(options?: UseMutationOptions<{ executionId: string; status: string; workflowId: string }, Error, { id: string; inputs?: Record<string, any> }>) {
  return useMutation({
    mutationFn: ({ id, inputs }) => workflowApi.executeWorkflow(id, inputs),
    ...options,
  });
}

/**
 * 切换发布状态
 */
export function useTogglePublished(options?: UseMutationOptions<Workflow, Error, string>) {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: workflowApi.togglePublished,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: workflowKeys.detail(variables) });
      options?.onSuccess?.(data, variables);
    },
    ...options,
  });
}

// ==================== Execution Hooks ====================

/**
 * 获取执行状态（支持轮询）
 */
export function useExecutionStatus(id: string | null, options?: { refetchInterval?: number } & UseQueryOptions<WorkflowExecution, Error>) {
  return useQuery({
    queryKey: executionKeys.detail(id!),
    queryFn: () => executionApi.getExecutionStatus(id!),
    enabled: !!id,
    refetchInterval: options?.refetchInterval || false,
    ...options,
  });
}

/**
 * 取消执行
 */
export function useCancelExecution(options?: UseMutationOptions<WorkflowExecution, Error, string>) {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: executionApi.cancelExecution,
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: executionKeys.detail(variables) });
      options?.onSuccess?.(data, variables);
    },
    ...options,
  });
}

/**
 * 获取执行历史
 */
export function useExecutionHistory(userId: string | null, limit: number = 20, options?: UseQueryOptions<WorkflowExecution[], Error>) {
  return useQuery({
    queryKey: executionKeys.history(userId!, limit),
    queryFn: () => executionApi.getExecutionHistory(userId!, limit),
    enabled: !!userId,
    ...options,
  });
}

// ==================== Executor Hooks ====================

/**
 * 获取执行器列表
 */
export function useExecutors(options?: UseQueryOptions<Executor[], Error>) {
  return useQuery({
    queryKey: executorKeys.list(),
    queryFn: executorApi.listExecutors,
    ...options,
  });
}
