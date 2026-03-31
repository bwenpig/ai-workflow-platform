import React from 'react'
import { renderHook, waitFor } from '@testing-library/react'
import { describe, test, expect, beforeEach, vi } from 'vitest'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useExecutionPolling, useStartExecution, useRetryNode, useNodeStatus, useFilteredLogs } from '@/workflow/hooks/useExecutionPolling'
import * as workflowStore from '@/workflow/store/useWorkflowStore'

// Mock workflow store
const mockUpdateNodeStatus = vi.fn()
const mockAddLogs = vi.fn()
const mockSetExecutionState = vi.fn()
const mockSetIsExecuting = vi.fn()

vi.mock('@/workflow/store/useWorkflowStore', () => ({
  useWorkflowStore: vi.fn((selector) => {
    if (selector.toString().includes('updateNodeStatus')) return mockUpdateNodeStatus
    if (selector.toString().includes('addLogs')) return mockAddLogs
    if (selector.toString().includes('setExecutionState')) return mockSetExecutionState
    if (selector.toString().includes('setIsExecuting')) return mockSetIsExecuting
    if (selector.toString().includes('executionState')) return {
      executionState: null,
      logs: [],
      logFilter: 'ALL',
      logSearchKeyword: '',
    }
    return {}
  }),
}))

// Mock fetch
global.fetch = vi.fn()

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })
  
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
}

describe('useExecutionPolling Hook 测试', () => {
  const mockExecutionData = {
    executionId: 'exec-123',
    workflowId: 'wf-456',
    status: 'running' as const,
    nodeStates: {
      'node-1': {
        status: 'running' as const,
        startTime: '2024-01-01T00:00:00Z',
      },
      'node-2': {
        status: 'pending' as const,
      },
    },
    logs: [
      { level: 'info', message: '执行开始', timestamp: '2024-01-01T00:00:00Z' },
    ],
    startTime: '2024-01-01T00:00:00Z',
  }

  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(global.fetch).mockResolvedValue({
      ok: true,
      json: async () => mockExecutionData,
    } as any)
  })

  describe('轮询启动', () => {
    test('当 executionId 和 enabled 都为 true 时应启动轮询', () => {
      const { result } = renderHook(
        () => useExecutionPolling('exec-123', true),
        { wrapper: createWrapper() }
      )

      expect(result.current.isPolling).toBe(true)
    })

    test('当 executionId 为 null 时不应启动轮询', () => {
      const { result } = renderHook(
        () => useExecutionPolling(null, true),
        { wrapper: createWrapper() }
      )

      expect(result.current.isPolling).toBe(false)
    })

    test('当 enabled 为 false 时不应启动轮询', () => {
      const { result } = renderHook(
        () => useExecutionPolling('exec-123', false),
        { wrapper: createWrapper() }
      )

      expect(result.current.isPolling).toBe(false)
    })
  })

  describe('轮询停止', () => {
    test('当 enabled 变为 false 时应停止轮询', () => {
      const { result, rerender } = renderHook(
        ({ id, enabled }) => useExecutionPolling(id, enabled),
        { 
          wrapper: createWrapper(),
          initialProps: { id: 'exec-123', enabled: true }
        }
      )

      expect(result.current.isPolling).toBe(true)

      rerender({ id: 'exec-123', enabled: false })

      expect(result.current.isPolling).toBe(false)
    })

    test('当 executionId 变为 null 时应停止轮询', () => {
      const { result, rerender } = renderHook(
        ({ id, enabled }) => useExecutionPolling(id, enabled),
        { 
          wrapper: createWrapper(),
          initialProps: { id: 'exec-123', enabled: true }
        }
      )

      expect(result.current.isPolling).toBe(true)

      rerender({ id: null, enabled: true })

      expect(result.current.isPolling).toBe(false)
    })
  })

  describe('状态更新', () => {
    test('应同步节点状态到 Store', async () => {
      renderHook(
        () => useExecutionPolling('exec-123', true),
        { wrapper: createWrapper() }
      )

      await waitFor(() => {
        expect(mockSetExecutionState).toHaveBeenCalledWith({
          workflowId: 'wf-456',
          executionId: 'exec-123',
          status: 'running',
          nodeStates: mockExecutionData.nodeStates,
          logs: mockExecutionData.logs,
          startTime: '2024-01-01T00:00:00Z',
          endTime: undefined,
        })
      })
    })

    test('应更新每个节点的状态', async () => {
      renderHook(
        () => useExecutionPolling('exec-123', true),
        { wrapper: createWrapper() }
      )

      await waitFor(() => {
        expect(mockUpdateNodeStatus).toHaveBeenCalledWith(
          'node-1',
          'running',
          expect.objectContaining({
            startTime: '2024-01-01T00:00:00Z',
          })
        )
      })
    })

    test('应添加日志到 Store', async () => {
      renderHook(
        () => useExecutionPolling('exec-123', true),
        { wrapper: createWrapper() }
      )

      await waitFor(() => {
        expect(mockAddLogs).toHaveBeenCalledWith(mockExecutionData.logs)
      })
    })

    test('应设置执行中状态为 true', async () => {
      renderHook(
        () => useExecutionPolling('exec-123', true),
        { wrapper: createWrapper() }
      )

      await waitFor(() => {
        expect(mockSetIsExecuting).toHaveBeenCalledWith(true)
      })
    })

    test('执行完成时应设置执行中状态为 false', async () => {
      vi.mocked(global.fetch).mockResolvedValue({
        ok: true,
        json: async () => ({
          ...mockExecutionData,
          status: 'success' as const,
        }),
      } as any)

      renderHook(
        () => useExecutionPolling('exec-123', true),
        { wrapper: createWrapper() }
      )

      await waitFor(() => {
        expect(mockSetIsExecuting).toHaveBeenCalledWith(false)
      })
    })
  })

  describe('错误处理', () => {
    test('API 调用失败时应返回错误', async () => {
      vi.mocked(global.fetch).mockResolvedValue({
        ok: false,
        statusText: 'Not Found',
      } as any)

      const { result } = renderHook(
        () => useExecutionPolling('exec-123', true),
        { wrapper: createWrapper() }
      )

      await waitFor(() => {
        expect(result.current.error).toBeDefined()
      })
    })

    test('网络错误时应返回错误', async () => {
      vi.mocked(global.fetch).mockRejectedValue(new Error('Network error'))

      const { result } = renderHook(
        () => useExecutionPolling('exec-123', true),
        { wrapper: createWrapper() }
      )

      await waitFor(() => {
        expect(result.current.error).toBeDefined()
      })
    })
  })

  describe('超时处理', () => {
    test('应在 3 秒后自动轮询', async () => {
      vi.useFakeTimers()
      
      renderHook(
        () => useExecutionPolling('exec-123', true),
        { wrapper: createWrapper() }
      )

      expect(global.fetch).toHaveBeenCalledTimes(1)

      vi.advanceTimersByTime(3000)
      
      await waitFor(() => {
        expect(global.fetch).toHaveBeenCalledTimes(2)
      })

      vi.useRealTimers()
    })
  })

  describe('React Query 集成', () => {
    test('应使用正确的 queryKey', () => {
      const { result } = renderHook(
        () => useExecutionPolling('exec-123', true),
        { wrapper: createWrapper() }
      )

      expect(result.current.data).toBeDefined()
    })

    test('应返回 isLoading 状态', () => {
      const { result } = renderHook(
        () => useExecutionPolling('exec-123', true),
        { wrapper: createWrapper() }
      )

      expect(result.current.isLoading).toBeDefined()
    })

    test('应提供 refetch 方法', () => {
      const { result } = renderHook(
        () => useExecutionPolling('exec-123', true),
        { wrapper: createWrapper() }
      )

      expect(result.current.refetch).toBeDefined()
      expect(typeof result.current.refetch).toBe('function')
    })
  })
})

describe('useStartExecution Hook 测试', () => {
  beforeEach(() => {
    vi.mocked(global.fetch).mockResolvedValue({
      ok: true,
      json: async () => ({ executionId: 'exec-123' }),
    } as any)
  })

  test('应启动工作流执行', async () => {
    const { result } = renderHook(
      () => useStartExecution(),
      { wrapper: createWrapper() }
    )

    result.current.mutate({ workflowId: 'wf-456' })

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        '/api/v1/workflows/wf-456/execute',
        expect.objectContaining({
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
        })
      )
    })
  })

  test('应支持传递配置参数', async () => {
    const { result } = renderHook(
      () => useStartExecution(),
      { wrapper: createWrapper() }
    )

    const config = { param1: 'value1', param2: 'value2' }
    result.current.mutate({ workflowId: 'wf-456', config })

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        expect.any(String),
        expect.objectContaining({
          body: JSON.stringify(config),
        })
      )
    })
  })
})

describe('useRetryNode Hook 测试', () => {
  beforeEach(() => {
    vi.mocked(global.fetch).mockResolvedValue({
      ok: true,
      json: async () => ({}),
    } as any)
  })

  test('应重试节点执行', async () => {
    const { result } = renderHook(
      () => useRetryNode('exec-123'),
      { wrapper: createWrapper() }
    )

    result.current.mutate({ nodeId: 'node-1' })

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        '/api/v1/executions/exec-123/nodes/node-1/retry',
        expect.objectContaining({
          method: 'POST',
        })
      )
    })
  })

  test('执行成功后应刷新查询', async () => {
    const { result } = renderHook(
      () => useRetryNode('exec-123'),
      { wrapper: createWrapper() }
    )

    result.current.mutate({ nodeId: 'node-1' })

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalled()
    })
  })
})

describe('useNodeStatus Hook 测试', () => {
  beforeEach(() => {
    vi.mocked(workflowStore.useWorkflowStore).mockImplementation((selector: any) => {
      if (selector.toString().includes('executionState')) {
        return {
          executionState: {
            nodeStates: {
              'node-1': {
                status: 'success' as const,
                executionTime: 1000,
                error: undefined,
                retryCount: 0,
                result: { data: 'test' },
              },
            },
          },
        }
      }
      return {}
    })
  })

  test('应返回节点状态', () => {
    const { result } = renderHook(
      () => useNodeStatus('node-1'),
      { wrapper: createWrapper() }
    )

    expect(result.current.status).toBe('success')
    expect(result.current.executionTime).toBe(1000)
  })

  test('当没有执行状态时应返回默认值', () => {
    vi.mocked(workflowStore.useWorkflowStore).mockImplementation((selector: any) => {
      if (selector.toString().includes('executionState')) {
        return { executionState: null }
      }
      return {}
    })

    const { result } = renderHook(
      () => useNodeStatus('node-1'),
      { wrapper: createWrapper() }
    )

    expect(result.current.status).toBe('pending')
    expect(result.current.retryCount).toBe(0)
  })

  test('应返回错误信息', () => {
    vi.mocked(workflowStore.useWorkflowStore).mockImplementation((selector: any) => {
      if (selector.toString().includes('executionState')) {
        return {
          executionState: {
            nodeStates: {
              'node-1': {
                status: 'failed' as const,
                error: '执行失败',
                retryCount: 2,
              },
            },
          },
        }
      }
      return {}
    })

    const { result } = renderHook(
      () => useNodeStatus('node-1'),
      { wrapper: createWrapper() }
    )

    expect(result.current.status).toBe('failed')
    expect(result.current.error).toBe('执行失败')
    expect(result.current.retryCount).toBe(2)
  })
})

describe('useFilteredLogs Hook 测试', () => {
  const mockLogs = [
    { level: 'info', message: '开始执行', timestamp: '2024-01-01T00:00:00Z', nodeId: 'node-1' },
    { level: 'error', message: '执行失败', timestamp: '2024-01-01T00:00:01Z', nodeId: 'node-2' },
    { level: 'info', message: '重试中', timestamp: '2024-01-01T00:00:02Z', nodeId: 'node-1' },
    { level: 'success', message: '执行完成', timestamp: '2024-01-01T00:00:03Z', nodeId: 'node-1' },
  ]

  beforeEach(() => {
    vi.mocked(workflowStore.useWorkflowStore).mockImplementation((selector: any) => {
      if (selector.toString().includes('executionState')) {
        return { executionState: { logs: mockLogs } }
      }
      if (selector.toString().includes('logFilter')) {
        return 'ALL'
      }
      if (selector.toString().includes('logSearchKeyword')) {
        return ''
      }
      return {}
    })
  })

  test('应返回所有日志当过滤器为 ALL', () => {
    vi.mocked(workflowStore.useWorkflowStore).mockImplementation((selector: any) => {
      if (selector.toString().includes('executionState')) {
        return { 
          executionState: { 
            logs: mockLogs,
            nodeStates: {},
          },
          logs: mockLogs,
        }
      }
      if (selector.toString().includes('logFilter')) {
        return 'ALL'
      }
      if (selector.toString().includes('logSearchKeyword')) {
        return ''
      }
      return {}
    })

    const { result } = renderHook(
      () => useFilteredLogs(),
      { wrapper: createWrapper() }
    )

    expect(result.current.length).toBe(4)
  })

  test('应按级别过滤日志', () => {
    vi.mocked(workflowStore.useWorkflowStore).mockImplementation((selector: any) => {
      if (selector.toString().includes('executionState')) {
        return { 
          executionState: { 
            logs: mockLogs,
            nodeStates: {},
          },
          logs: mockLogs,
        }
      }
      if (selector.toString().includes('logFilter')) {
        return 'error'
      }
      if (selector.toString().includes('logSearchKeyword')) {
        return ''
      }
      return {}
    })

    const { result } = renderHook(
      () => useFilteredLogs(),
      { wrapper: createWrapper() }
    )

    expect(result.current.length).toBe(1)
    expect(result.current[0].level).toBe('error')
  })

  test('应按关键词搜索日志', () => {
    vi.mocked(workflowStore.useWorkflowStore).mockImplementation((selector: any) => {
      if (selector.toString().includes('executionState')) {
        return { 
          executionState: { 
            logs: mockLogs,
            nodeStates: {},
          },
          logs: mockLogs,
        }
      }
      if (selector.toString().includes('logFilter')) {
        return 'ALL'
      }
      if (selector.toString().includes('logSearchKeyword')) {
        return 'node-1'
      }
      return {}
    })

    const { result } = renderHook(
      () => useFilteredLogs(),
      { wrapper: createWrapper() }
    )

    expect(result.current.length).toBe(3)
  })

  test('应按时间倒序排列', () => {
    vi.mocked(workflowStore.useWorkflowStore).mockImplementation((selector: any) => {
      if (selector.toString().includes('executionState')) {
        return { 
          executionState: { 
            logs: mockLogs,
            nodeStates: {},
          },
          logs: mockLogs,
        }
      }
      if (selector.toString().includes('logFilter')) {
        return 'ALL'
      }
      if (selector.toString().includes('logSearchKeyword')) {
        return ''
      }
      return {}
    })

    const { result } = renderHook(
      () => useFilteredLogs(),
      { wrapper: createWrapper() }
    )

    expect(result.current[0].timestamp).toBe('2024-01-01T00:00:03Z')
    expect(result.current[result.current.length - 1].timestamp).toBe('2024-01-01T00:00:00Z')
  })
})
