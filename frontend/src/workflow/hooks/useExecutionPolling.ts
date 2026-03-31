import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useCallback, useMemo } from 'react'
import { useWorkflowStore, NodeStatus, LogEntry } from '../store/useWorkflowStore'

// API 响应类型
export interface WorkflowExecutionResponse {
  executionId: string
  workflowId: string
  status: 'pending' | 'running' | 'success' | 'failed'
  nodeStates: Record<string, {
    status: NodeStatus
    executionTime?: number
    startTime?: string
    endTime?: string
    error?: string
    retryCount?: number
    result?: any
  }>
  logs: LogEntry[]
  startTime?: string
  endTime?: string
}

// 获取执行状态的 API
const fetchExecutionStatus = async (executionId: string): Promise<WorkflowExecutionResponse> => {
  const response = await fetch(`/api/v1/executions/${executionId}`)
  if (!response.ok) {
    throw new Error(`Failed to fetch execution status: ${response.statusText}`)
  }
  return response.json()
}

// 重试节点的 API
const retryNodeExecution = async (executionId: string, nodeId: string): Promise<void> => {
  const response = await fetch(`/api/v1/executions/${executionId}/nodes/${nodeId}/retry`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
  })
  if (!response.ok) {
    throw new Error(`Failed to retry node: ${response.statusText}`)
  }
}

// 开始执行的 API
const startWorkflowExecution = async (workflowId: string, config?: any): Promise<{ executionId: string }> => {
  const response = await fetch(`/api/v1/workflows/${workflowId}/execute`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(config || {}),
  })
  if (!response.ok) {
    throw new Error(`Failed to start execution: ${response.statusText}`)
  }
  return response.json()
}

/**
 * F029 - 实时状态轮询 Hook
 * 使用 React Query 每 3 秒轮询执行状态
 */
export const useExecutionPolling = (executionId: string | null, enabled: boolean = true) => {
  const queryClient = useQueryClient()
  const updateNodeStatus = useWorkflowStore((state) => state.updateNodeStatus)
  const addLogs = useWorkflowStore((state) => state.addLogs)
  const setExecutionState = useWorkflowStore((state) => state.setExecutionState)
  const setIsExecuting = useWorkflowStore((state) => state.setIsExecuting)
  
  // 轮询查询
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['execution', executionId],
    queryFn: () => fetchExecutionStatus(executionId!),
    enabled: enabled && !!executionId,
    refetchInterval: 3000, // F029 - 3 秒轮询
    retry: 3,
    staleTime: 2000,
  })
  
  // 同步状态到 Store
  useCallback(() => {
    if (!data) return
    
    // 更新执行状态
    setExecutionState({
      workflowId: data.workflowId,
      executionId: data.executionId,
      status: data.status,
      nodeStates: data.nodeStates,
      logs: data.logs,
      startTime: data.startTime,
      endTime: data.endTime,
    })
    
    // 更新节点状态
    Object.entries(data.nodeStates).forEach(([nodeId, nodeState]) => {
      updateNodeStatus(nodeId, nodeState.status, {
        executionTime: nodeState.executionTime,
        startTime: nodeState.startTime,
        endTime: nodeState.endTime,
        error: nodeState.error,
        retryCount: nodeState.retryCount,
        result: nodeState.result,
      })
    })
    
    // 添加新日志
    if (data.logs.length > 0) {
      addLogs(data.logs)
    }
    
    // 更新执行中状态
    setIsExecuting(data.status === 'running' || data.status === 'pending')
  }, [data, setExecutionState, updateNodeStatus, addLogs, setIsExecuting])
  
  return {
    data,
    isLoading,
    error,
    refetch,
    isPolling: enabled && !!executionId,
  }
}

/**
 * 开始执行工作流 Hook
 */
export const useStartExecution = () => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: ({ workflowId, config }: { workflowId: string; config?: any }) =>
      startWorkflowExecution(workflowId, config),
    onSuccess: (data) => {
      // 预取执行状态
      queryClient.prefetchQuery({
        queryKey: ['execution', data.executionId],
        queryFn: () => fetchExecutionStatus(data.executionId),
      })
    },
  })
}

/**
 * 重试节点 Hook
 */
export const useRetryNode = (executionId: string | null) => {
  const queryClient = useQueryClient()
  
  return useMutation({
    mutationFn: ({ nodeId }: { nodeId: string }) =>
      retryNodeExecution(executionId!, nodeId),
    onSuccess: () => {
      // 立即刷新状态
      if (executionId) {
        queryClient.invalidateQueries({ queryKey: ['execution', executionId] })
      }
    },
  })
}

/**
 * 获取节点状态 Hook
 */
export const useNodeStatus = (nodeId: string) => {
  const executionState = useWorkflowStore((state) => state.executionState)
  
  if (!executionState || !executionState.nodeStates) {
    return {
      status: 'pending' as NodeStatus,
      executionTime: undefined,
      error: undefined,
      retryCount: 0,
    }
  }
  
  const nodeState = executionState.nodeStates[nodeId]
  
  return {
    status: nodeState?.status || 'pending',
    executionTime: nodeState?.executionTime,
    error: nodeState?.error,
    retryCount: nodeState?.retryCount || 0,
    result: nodeState?.result,
  }
}

/**
 * 获取过滤后的日志 Hook (F031, F032)
 */
export const useFilteredLogs = () => {
  const executionState = useWorkflowStore((state) => state.executionState)
  const logFilter = useWorkflowStore((state) => state.logFilter)
  const logSearchKeyword = useWorkflowStore((state) => state.logSearchKeyword)
  
  return useMemo(() => {
    if (!executionState) return []
    
    let logs = executionState.logs
    
    // 按级别过滤 (F031)
    if (logFilter !== 'ALL') {
      logs = logs.filter(log => log.level === logFilter)
    }
    
    // 按关键词搜索 (F032)
    if (logSearchKeyword) {
      logs = logs.filter(log => 
        log.message.toLowerCase().includes(logSearchKeyword.toLowerCase()) ||
        log.nodeId?.toLowerCase().includes(logSearchKeyword.toLowerCase())
      )
    }
    
    // 按时间倒序
    return logs.sort((a, b) => 
      new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
    )
  }, [executionState, logFilter, logSearchKeyword])
}
