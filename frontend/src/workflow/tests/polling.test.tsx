import { describe, it, expect, beforeEach, vi } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useExecutionPolling, useNodeStatus, useFilteredLogs } from '../hooks/useExecutionPolling'
import { useWorkflowStore } from '../store/useWorkflowStore'

vi.mock('../store/useWorkflowStore', () => ({
  useWorkflowStore: vi.fn(),
}))

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  })
  
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  )
}

describe('F029 - 实时状态 (React Query 3s 轮询)', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('应每 3 秒轮询执行状态', async () => {
    const mockFetch = vi.fn()
      .mockResolvedValueOnce({
        json: () => Promise.resolve({
          executionId: 'exec-1',
          workflowId: 'wf-1',
          status: 'running',
          nodeStates: {},
          logs: [],
        }),
      })
      .mockResolvedValueOnce({
        json: () => Promise.resolve({
          executionId: 'exec-1',
          workflowId: 'wf-1',
          status: 'success',
          nodeStates: {},
          logs: [],
        }),
      })

    global.fetch = mockFetch as any

    const { result } = renderHook(
      () => useExecutionPolling('exec-1', true),
      { wrapper: createWrapper() }
    )

    // 等待初始查询
    await waitFor(() => expect(result.current.data).toBeDefined())
    expect(mockFetch).toHaveBeenCalledTimes(1)

    // 快进 3 秒
    vi.advanceTimersByTime(3000)

    // 等待第二次查询
    await waitFor(() => expect(mockFetch).toHaveBeenCalledTimes(2))
  })

  it('应在 enabled 为 false 时停止轮询', async () => {
    const mockFetch = vi.fn()
      .mockResolvedValue({
        json: () => Promise.resolve({
          executionId: 'exec-1',
          workflowId: 'wf-1',
          status: 'running',
          nodeStates: {},
          logs: [],
        }),
      })

    global.fetch = mockFetch as any

    const { result, rerender } = renderHook(
      ({ enabled }) => useExecutionPolling('exec-1', enabled),
      { wrapper: createWrapper(), initialProps: { enabled: true } }
    )

    // 等待初始查询
    await waitFor(() => expect(result.current.data).toBeDefined())
    const firstCallCount = mockFetch.mock.calls.length

    // 禁用轮询
    rerender({ enabled: false })

    // 快进 3 秒
    vi.advanceTimersByTime(3000)
    await vi.runAllTimersAsync()

    // 不应有新的请求
    expect(mockFetch).toHaveBeenCalledTimes(firstCallCount)
  })

  it('应同步状态到 Store', async () => {
    const mockUpdateNodeStatus = vi.fn()
    const mockAddLogs = vi.fn()
    const mockSetExecutionState = vi.fn()
    const mockSetIsExecuting = vi.fn()

    vi.mocked(useWorkflowStore).mockImplementation((selector: any) => {
      const state = {
        updateNodeStatus: mockUpdateNodeStatus,
        addLogs: mockAddLogs,
        setExecutionState: mockSetExecutionState,
        setIsExecuting: mockSetIsExecuting,
      }
      return selector(state)
    })

    const mockFetch = vi.fn().mockResolvedValue({
      json: () => Promise.resolve({
        executionId: 'exec-1',
        workflowId: 'wf-1',
        status: 'running',
        nodeStates: {
          'node-1': { status: 'success', executionTime: 1000 },
          'node-2': { status: 'running' },
        },
        logs: [
          { id: 'log-1', nodeId: 'node-1', level: 'INFO', message: '完成', timestamp: new Date().toISOString() }
        ],
      }),
    })

    global.fetch = mockFetch as any

    renderHook(
      () => useExecutionPolling('exec-1', true),
      { wrapper: createWrapper() }
    )

    await waitFor(() => expect(mockFetch).toHaveBeenCalled())

    // 验证状态同步
    expect(mockSetExecutionState).toHaveBeenCalled()
    expect(mockUpdateNodeStatus).toHaveBeenCalled()
    expect(mockAddLogs).toHaveBeenCalled()
  })
})

describe('useNodeStatus Hook', () => {
  it('应返回节点状态信息', () => {
    vi.mocked(useWorkflowStore).mockReturnValue({
      workflowId: 'wf-1',
      executionId: 'exec-1',
      status: 'running',
      nodeStates: {
        'node-1': {
          status: 'success',
          executionTime: 1500,
          error: undefined,
          retryCount: 0,
          result: { data: 'test' },
        },
      },
      logs: [],
    } as any)

    const { result } = renderHook(() => useNodeStatus('node-1'))

    expect(result.current.status).toBe('success')
    expect(result.current.executionTime).toBe(1500)
    expect(result.current.retryCount).toBe(0)
  })

  it('应在无执行状态时返回默认值', () => {
    vi.mocked(useWorkflowStore).mockReturnValue({
      executionState: null,
    } as any)

    const { result } = renderHook(() => useNodeStatus('node-1'))

    expect(result.current.status).toBe('pending')
    expect(result.current.executionTime).toBeUndefined()
    expect(result.current.retryCount).toBe(0)
  })
})

describe('useFilteredLogs Hook', () => {
  it('应返回过滤后的日志', () => {
    const mockLogs = [
      { id: '1', nodeId: 'node-1', level: 'INFO', message: '消息 1', timestamp: '2026-03-31T10:00:00Z' },
      { id: '2', nodeId: 'node-2', level: 'ERROR', message: '错误消息', timestamp: '2026-03-31T10:00:01Z' },
      { id: '3', nodeId: 'node-3', level: 'WARN', message: '警告消息', timestamp: '2026-03-31T10:00:02Z' },
    ]

    vi.mocked(useWorkflowStore).mockImplementation((selector: any) => {
      const state = {
        executionState: {
          workflowId: 'wf-1',
          executionId: 'exec-1',
          status: 'running',
          nodeStates: {},
          logs: mockLogs,
        },
        logFilter: 'ERROR',
        logSearchKeyword: '',
      }
      return selector(state)
    })

    const { result } = renderHook(() => useFilteredLogs())

    expect(result.current.length).toBe(1)
    expect(result.current[0].level).toBe('ERROR')
  })

  it('应支持搜索过滤', () => {
    const mockLogs = [
      { id: '1', nodeId: 'node-1', level: 'INFO', message: '成功完成', timestamp: '2026-03-31T10:00:00Z' },
      { id: '2', nodeId: 'node-2', level: 'INFO', message: '执行失败', timestamp: '2026-03-31T10:00:01Z' },
    ]

    vi.mocked(useWorkflowStore).mockImplementation((selector: any) => {
      const state = {
        executionState: {
          workflowId: 'wf-1',
          executionId: 'exec-1',
          status: 'running',
          nodeStates: {},
          logs: mockLogs,
        },
        logFilter: 'ALL',
        logSearchKeyword: '失败',
      }
      return selector(state)
    })

    const { result } = renderHook(() => useFilteredLogs())

    expect(result.current.length).toBe(1)
    expect(result.current[0].message).toBe('执行失败')
  })
})
