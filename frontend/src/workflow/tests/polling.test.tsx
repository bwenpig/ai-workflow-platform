import { describe, it, expect, beforeEach, vi } from 'vitest'
import { renderHook } from '@testing-library/react'
import { useNodeStatus, useFilteredLogs } from '../hooks/useExecutionPolling'

// Mock useWorkflowStore - 必须在顶部声明
const mockedUseWorkflowStore = vi.fn()
vi.mock('../store/useWorkflowStore', () => ({
  useWorkflowStore: (selector: any) => mockedUseWorkflowStore(selector),
}))

describe('useNodeStatus Hook', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('应返回节点状态信息', () => {
    mockedUseWorkflowStore.mockImplementation((selector) =>
      selector({
        executionState: {
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
        },
      })
    )

    const { result } = renderHook(() => useNodeStatus('node-1'))

    expect(result.current.status).toBe('success')
    expect(result.current.executionTime).toBe(1500)
    expect(result.current.retryCount).toBe(0)
  })

  it('应在无执行状态时返回默认值', () => {
    mockedUseWorkflowStore.mockImplementation((selector) =>
      selector({
        executionState: null,
      })
    )

    const { result } = renderHook(() => useNodeStatus('node-1'))

    expect(result.current.status).toBe('pending')
    expect(result.current.executionTime).toBeUndefined()
    expect(result.current.retryCount).toBe(0)
  })
})

describe('useFilteredLogs Hook', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('应返回过滤后的日志', () => {
    const mockLogs = [
      { id: '1', nodeId: 'node-1', level: 'INFO', message: '消息 1', timestamp: '2026-03-31T10:00:00Z' },
      { id: '2', nodeId: 'node-2', level: 'ERROR', message: '错误消息', timestamp: '2026-03-31T10:00:01Z' },
      { id: '3', nodeId: 'node-3', level: 'WARN', message: '警告消息', timestamp: '2026-03-31T10:00:02Z' },
    ]

    mockedUseWorkflowStore.mockImplementation((selector) =>
      selector({
        executionState: {
          workflowId: 'wf-1',
          executionId: 'exec-1',
          status: 'running',
          nodeStates: {},
          logs: mockLogs,
        },
        logFilter: 'ERROR',
        logSearchKeyword: '',
      })
    )

    const { result } = renderHook(() => useFilteredLogs())

    expect(result.current.length).toBe(1)
    expect(result.current[0].level).toBe('ERROR')
  })

  it('应支持搜索过滤', () => {
    const mockLogs = [
      { id: '1', nodeId: 'node-1', level: 'INFO', message: '成功完成', timestamp: '2026-03-31T10:00:00Z' },
      { id: '2', nodeId: 'node-2', level: 'INFO', message: '执行失败', timestamp: '2026-03-31T10:00:01Z' },
    ]

    mockedUseWorkflowStore.mockImplementation((selector) =>
      selector({
        executionState: {
          workflowId: 'wf-1',
          executionId: 'exec-1',
          status: 'running',
          nodeStates: {},
          logs: mockLogs,
        },
        logFilter: 'ALL',
        logSearchKeyword: '失败',
      })
    )

    const { result } = renderHook(() => useFilteredLogs())

    expect(result.current.length).toBe(1)
    expect(result.current[0].message).toBe('执行失败')
  })
})

// F029 轮询测试已移至集成测试，此处仅测试 hook 的基本功能
