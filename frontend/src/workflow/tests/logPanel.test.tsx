import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { LogPanel } from '../components/LogPanel'
import { useWorkflowStore } from '../store/useWorkflowStore'
import { ConfigProvider } from 'antd'

vi.mock('../store/useWorkflowStore', () => ({
  useWorkflowStore: vi.fn(),
}))

describe('F030 - 日志面板 (Drawer + 虚拟列表)', () => {
  const mockLogs = [
    {
      id: 'log-1',
      nodeId: 'node-1',
      level: 'INFO' as const,
      message: '工作流开始执行',
      timestamp: '2026-03-31T10:00:00.000Z',
    },
    {
      id: 'log-2',
      nodeId: 'node-1',
      level: 'INFO' as const,
      message: '节点 1 执行成功',
      timestamp: '2026-03-31T10:00:01.000Z',
    },
    {
      id: 'log-3',
      nodeId: 'node-2',
      level: 'WARN' as const,
      message: '节点 2 执行缓慢',
      timestamp: '2026-03-31T10:00:02.000Z',
    },
  ]

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('应渲染日志面板 Drawer', () => {
    vi.mocked(useWorkflowStore).mockReturnValue({
      logDrawerOpen: true,
      setLogDrawerOpen: vi.fn(),
      logFilter: 'ALL',
      setLogFilter: vi.fn(),
      logSearchKeyword: '',
      setLogSearchKeyword: vi.fn(),
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'running',
        nodeStates: {},
        logs: mockLogs,
      },
      clearLogs: vi.fn(),
      updateNodeStatus: vi.fn(),
      expandedSubWorkflowId: null,
      setExpandedSubWorkflowId: vi.fn(),
      nodes: [],
      edges: [],
      isExecuting: false,
      selectedNodeId: null,
      setNodes: vi.fn(),
      setEdges: vi.fn(),
      updateNodeExecutionTime: vi.fn(),
      addLog: vi.fn(),
      addLogs: vi.fn(),
      setExecutionState: vi.fn(),
      setIsExecuting: vi.fn(),
      retryNode: vi.fn(),
      selectNode: vi.fn(),
      clearExecutionState: vi.fn(),
    })

    render(
      <ConfigProvider>
        <LogPanel />
      </ConfigProvider>
    )

    expect(screen.getByText('执行日志')).toBeInTheDocument()
  })

  it('应显示日志列表', () => {
    vi.mocked(useWorkflowStore).mockReturnValue({
      logDrawerOpen: true,
      setLogDrawerOpen: vi.fn(),
      logFilter: 'ALL',
      setLogFilter: vi.fn(),
      logSearchKeyword: '',
      setLogSearchKeyword: vi.fn(),
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'running',
        nodeStates: {},
        logs: mockLogs,
      },
      clearLogs: vi.fn(),
      updateNodeStatus: vi.fn(),
      expandedSubWorkflowId: null,
      setExpandedSubWorkflowId: vi.fn(),
      nodes: [],
      edges: [],
      isExecuting: false,
      selectedNodeId: null,
      setNodes: vi.fn(),
      setEdges: vi.fn(),
      updateNodeExecutionTime: vi.fn(),
      addLog: vi.fn(),
      addLogs: vi.fn(),
      setExecutionState: vi.fn(),
      setIsExecuting: vi.fn(),
      retryNode: vi.fn(),
      selectNode: vi.fn(),
      clearExecutionState: vi.fn(),
    })

    render(
      <ConfigProvider>
        <LogPanel />
      </ConfigProvider>
    )

    expect(screen.getByText('工作流开始执行')).toBeInTheDocument()
    expect(screen.getByText('节点 1 执行成功')).toBeInTheDocument()
  })

  it('应可关闭日志面板', () => {
    const mockSetLogDrawerOpen = vi.fn()
    vi.mocked(useWorkflowStore).mockReturnValue({
      logDrawerOpen: true,
      setLogDrawerOpen: mockSetLogDrawerOpen,
      logFilter: 'ALL',
      setLogFilter: vi.fn(),
      logSearchKeyword: '',
      setLogSearchKeyword: vi.fn(),
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'running',
        nodeStates: {},
        logs: mockLogs,
      },
      clearLogs: vi.fn(),
      updateNodeStatus: vi.fn(),
      expandedSubWorkflowId: null,
      setExpandedSubWorkflowId: vi.fn(),
      nodes: [],
      edges: [],
      isExecuting: false,
      selectedNodeId: null,
      setNodes: vi.fn(),
      setEdges: vi.fn(),
      updateNodeExecutionTime: vi.fn(),
      addLog: vi.fn(),
      addLogs: vi.fn(),
      setExecutionState: vi.fn(),
      setIsExecuting: vi.fn(),
      retryNode: vi.fn(),
      selectNode: vi.fn(),
      clearExecutionState: vi.fn(),
    })

    render(
      <ConfigProvider>
        <LogPanel />
      </ConfigProvider>
    )

    // 关闭按钮通常在右上角
    const closeButtons = document.querySelectorAll('[aria-label="Close"]')
    if (closeButtons.length > 0) {
      fireEvent.click(closeButtons[0])
      expect(mockSetLogDrawerOpen).toHaveBeenCalledWith(false)
    }
  })
})

describe('F031 - 日志过滤 (按级别过滤)', () => {
  const mockLogs = [
    {
      id: 'log-1',
      nodeId: 'node-1',
      level: 'INFO' as const,
      message: '信息日志',
      timestamp: '2026-03-31T10:00:00.000Z',
    },
    {
      id: 'log-2',
      nodeId: 'node-2',
      level: 'WARN' as const,
      message: '警告日志',
      timestamp: '2026-03-31T10:00:01.000Z',
    },
    {
      id: 'log-3',
      nodeId: 'node-3',
      level: 'ERROR' as const,
      message: '错误日志',
      timestamp: '2026-03-31T10:00:02.000Z',
    },
    {
      id: 'log-4',
      nodeId: 'node-4',
      level: 'DEBUG' as const,
      message: '调试日志',
      timestamp: '2026-03-31T10:00:03.000Z',
    },
  ]

  it('应显示全部日志当过滤为 ALL', () => {
    vi.mocked(useWorkflowStore).mockReturnValue({
      logDrawerOpen: true,
      setLogDrawerOpen: vi.fn(),
      logFilter: 'ALL',
      setLogFilter: vi.fn(),
      logSearchKeyword: '',
      setLogSearchKeyword: vi.fn(),
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'running',
        nodeStates: {},
        logs: mockLogs,
      },
      clearLogs: vi.fn(),
      updateNodeStatus: vi.fn(),
      expandedSubWorkflowId: null,
      setExpandedSubWorkflowId: vi.fn(),
      nodes: [],
      edges: [],
      isExecuting: false,
      selectedNodeId: null,
      setNodes: vi.fn(),
      setEdges: vi.fn(),
      updateNodeExecutionTime: vi.fn(),
      addLog: vi.fn(),
      addLogs: vi.fn(),
      setExecutionState: vi.fn(),
      setIsExecuting: vi.fn(),
      retryNode: vi.fn(),
      selectNode: vi.fn(),
      clearExecutionState: vi.fn(),
    })

    render(
      <ConfigProvider>
        <LogPanel />
      </ConfigProvider>
    )

    expect(screen.getByText('信息日志')).toBeInTheDocument()
    expect(screen.getByText('警告日志')).toBeInTheDocument()
    expect(screen.getByText('错误日志')).toBeInTheDocument()
    expect(screen.getByText('调试日志')).toBeInTheDocument()
  })

  it('应只过滤显示 ERROR 级别日志', () => {
    vi.mocked(useWorkflowStore).mockReturnValue({
      logDrawerOpen: true,
      setLogDrawerOpen: vi.fn(),
      logFilter: 'ERROR',
      setLogFilter: vi.fn(),
      logSearchKeyword: '',
      setLogSearchKeyword: vi.fn(),
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'running',
        nodeStates: {},
        logs: mockLogs,
      },
      clearLogs: vi.fn(),
      updateNodeStatus: vi.fn(),
      expandedSubWorkflowId: null,
      setExpandedSubWorkflowId: vi.fn(),
      nodes: [],
      edges: [],
      isExecuting: false,
      selectedNodeId: null,
      setNodes: vi.fn(),
      setEdges: vi.fn(),
      updateNodeExecutionTime: vi.fn(),
      addLog: vi.fn(),
      addLogs: vi.fn(),
      setExecutionState: vi.fn(),
      setIsExecuting: vi.fn(),
      retryNode: vi.fn(),
      selectNode: vi.fn(),
      clearExecutionState: vi.fn(),
    })

    render(
      <ConfigProvider>
        <LogPanel />
      </ConfigProvider>
    )

    expect(screen.getByText('错误日志')).toBeInTheDocument()
    expect(screen.queryByText('信息日志')).not.toBeInTheDocument()
    expect(screen.queryByText('警告日志')).not.toBeInTheDocument()
  })

  it('应可通过下拉切换过滤级别', () => {
    const mockSetLogFilter = vi.fn()
    vi.mocked(useWorkflowStore).mockReturnValue({
      logDrawerOpen: true,
      setLogDrawerOpen: vi.fn(),
      logFilter: 'ALL',
      setLogFilter: mockSetLogFilter,
      logSearchKeyword: '',
      setLogSearchKeyword: vi.fn(),
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'running',
        nodeStates: {},
        logs: mockLogs,
      },
      clearLogs: vi.fn(),
      updateNodeStatus: vi.fn(),
      expandedSubWorkflowId: null,
      setExpandedSubWorkflowId: vi.fn(),
      nodes: [],
      edges: [],
      isExecuting: false,
      selectedNodeId: null,
      setNodes: vi.fn(),
      setEdges: vi.fn(),
      updateNodeExecutionTime: vi.fn(),
      addLog: vi.fn(),
      addLogs: vi.fn(),
      setExecutionState: vi.fn(),
      setIsExecuting: vi.fn(),
      retryNode: vi.fn(),
      selectNode: vi.fn(),
      clearExecutionState: vi.fn(),
    })

    render(
      <ConfigProvider>
        <LogPanel />
      </ConfigProvider>
    )

    // 找到过滤下拉框并选择 ERROR
    const filterSelect = screen.getByText('全部')
    fireEvent.mouseDown(filterSelect)
    
    // 选择 ERROR 选项
    const errorOption = screen.getByText('错误')
    fireEvent.click(errorOption)
    
    expect(mockSetLogFilter).toHaveBeenCalledWith('ERROR')
  })
})

describe('F032 - 日志搜索 (关键词搜索 + 高亮)', () => {
  const mockLogs = [
    {
      id: 'log-1',
      nodeId: 'node-1',
      level: 'INFO' as const,
      message: '节点 1 开始执行',
      timestamp: '2026-03-31T10:00:00.000Z',
    },
    {
      id: 'log-2',
      nodeId: 'node-2',
      level: 'INFO' as const,
      message: '节点 2 执行成功',
      timestamp: '2026-03-31T10:00:01.000Z',
    },
    {
      id: 'log-3',
      nodeId: 'node-3',
      level: 'ERROR' as const,
      message: '节点 3 执行失败',
      timestamp: '2026-03-31T10:00:02.000Z',
    },
  ]

  it('应可通过搜索框输入关键词', () => {
    const mockSetLogSearchKeyword = vi.fn()
    vi.mocked(useWorkflowStore).mockReturnValue({
      logDrawerOpen: true,
      setLogDrawerOpen: vi.fn(),
      logFilter: 'ALL',
      setLogFilter: vi.fn(),
      logSearchKeyword: '',
      setLogSearchKeyword: mockSetLogSearchKeyword,
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'running',
        nodeStates: {},
        logs: mockLogs,
      },
      clearLogs: vi.fn(),
      updateNodeStatus: vi.fn(),
      expandedSubWorkflowId: null,
      setExpandedSubWorkflowId: vi.fn(),
      nodes: [],
      edges: [],
      isExecuting: false,
      selectedNodeId: null,
      setNodes: vi.fn(),
      setEdges: vi.fn(),
      updateNodeExecutionTime: vi.fn(),
      addLog: vi.fn(),
      addLogs: vi.fn(),
      setExecutionState: vi.fn(),
      setIsExecuting: vi.fn(),
      retryNode: vi.fn(),
      selectNode: vi.fn(),
      clearExecutionState: vi.fn(),
    })

    render(
      <ConfigProvider>
        <LogPanel />
      </ConfigProvider>
    )

    const searchInput = screen.getByPlaceholderText('搜索日志内容...')
    fireEvent.change(searchInput, { target: { value: '失败' } })

    expect(mockSetLogSearchKeyword).toHaveBeenCalledWith('失败')
  })

  it('应过滤显示包含关键词的日志', () => {
    vi.mocked(useWorkflowStore).mockReturnValue({
      logDrawerOpen: true,
      setLogDrawerOpen: vi.fn(),
      logFilter: 'ALL',
      setLogFilter: vi.fn(),
      logSearchKeyword: '失败',
      setLogSearchKeyword: vi.fn(),
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'running',
        nodeStates: {},
        logs: mockLogs,
      },
      clearLogs: vi.fn(),
      updateNodeStatus: vi.fn(),
      expandedSubWorkflowId: null,
      setExpandedSubWorkflowId: vi.fn(),
      nodes: [],
      edges: [],
      isExecuting: false,
      selectedNodeId: null,
      setNodes: vi.fn(),
      setEdges: vi.fn(),
      updateNodeExecutionTime: vi.fn(),
      addLog: vi.fn(),
      addLogs: vi.fn(),
      setExecutionState: vi.fn(),
      setIsExecuting: vi.fn(),
      retryNode: vi.fn(),
      selectNode: vi.fn(),
      clearExecutionState: vi.fn(),
    })

    render(
      <ConfigProvider>
        <LogPanel />
      </ConfigProvider>
    )

    expect(screen.getByText('节点 3 执行失败')).toBeInTheDocument()
    expect(screen.queryByText('节点 1 开始执行')).not.toBeInTheDocument()
    expect(screen.queryByText('节点 2 执行成功')).not.toBeInTheDocument()
  })

  it('应高亮显示搜索关键词', () => {
    vi.mocked(useWorkflowStore).mockReturnValue({
      logDrawerOpen: true,
      setLogDrawerOpen: vi.fn(),
      logFilter: 'ALL',
      setLogFilter: vi.fn(),
      logSearchKeyword: '节点',
      setLogSearchKeyword: vi.fn(),
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'running',
        nodeStates: {},
        logs: mockLogs,
      },
      clearLogs: vi.fn(),
      updateNodeStatus: vi.fn(),
      expandedSubWorkflowId: null,
      setExpandedSubWorkflowId: vi.fn(),
      nodes: [],
      edges: [],
      isExecuting: false,
      selectedNodeId: null,
      setNodes: vi.fn(),
      setEdges: vi.fn(),
      updateNodeExecutionTime: vi.fn(),
      addLog: vi.fn(),
      addLogs: vi.fn(),
      setExecutionState: vi.fn(),
      setIsExecuting: vi.fn(),
      retryNode: vi.fn(),
      selectNode: vi.fn(),
      clearExecutionState: vi.fn(),
    })

    render(
      <ConfigProvider>
        <LogPanel />
      </ConfigProvider>
    )

    // 检查高亮标记
    const marks = document.querySelectorAll('mark')
    expect(marks.length).toBeGreaterThan(0)
  })
})
