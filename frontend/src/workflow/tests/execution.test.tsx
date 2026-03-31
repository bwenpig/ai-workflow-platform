import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { TaskNode } from '../../nodes/TaskNode'
import { useWorkflowStore } from '../../../store/useWorkflowStore'
import { ReactFlowProvider } from '@xyflow/react'

vi.mock('../../../store/useWorkflowStore', () => ({
  useWorkflowStore: vi.fn(),
}))

describe('F028 - 状态颜色', () => {
  const mockTaskProps = {
    id: 'test-task-node',
    data: {
      label: '测试任务',
      taskType: 'script' as const,
    },
    selected: false,
    type: 'task',
    position: { x: 0, y: 0 },
    width: 220,
    height: 200,
    zIndex: 1,
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('应在成功状态显示绿色边框', () => {
    vi.mocked(useWorkflowStore).mockReturnValue({
      updateNodeStatus: vi.fn(),
      expandedSubWorkflowId: null,
      setExpandedSubWorkflowId: vi.fn(),
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'success',
        nodeStates: {
          'test-task-node': { status: 'success' },
        },
        logs: [],
      },
      logDrawerOpen: false,
      logFilter: 'ALL',
      logSearchKeyword: '',
      nodes: [],
      edges: [],
      isExecuting: false,
      selectedNodeId: null,
      setNodes: vi.fn(),
      setEdges: vi.fn(),
      updateNodeExecutionTime: vi.fn(),
      addLog: vi.fn(),
      addLogs: vi.fn(),
      clearLogs: vi.fn(),
      setExecutionState: vi.fn(),
      setIsExecuting: vi.fn(),
      retryNode: vi.fn(),
      selectNode: vi.fn(),
      setLogDrawerOpen: vi.fn(),
      setLogFilter: vi.fn(),
      setLogSearchKeyword: vi.fn(),
      clearExecutionState: vi.fn(),
    })

    render(
      <ReactFlowProvider>
        <TaskNode {...mockTaskProps} />
      </ReactFlowProvider>
    )

    // 验证成功状态图标
    expect(screen.getByTestId('check-circle')).toBeInTheDocument()
  })

  it('应在失败状态显示红色边框', () => {
    vi.mocked(useWorkflowStore).mockReturnValue({
      updateNodeStatus: vi.fn(),
      expandedSubWorkflowId: null,
      setExpandedSubWorkflowId: vi.fn(),
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'failed',
        nodeStates: {
          'test-task-node': { status: 'failed' },
        },
        logs: [],
      },
      logDrawerOpen: false,
      logFilter: 'ALL',
      logSearchKeyword: '',
      nodes: [],
      edges: [],
      isExecuting: false,
      selectedNodeId: null,
      setNodes: vi.fn(),
      setEdges: vi.fn(),
      updateNodeExecutionTime: vi.fn(),
      addLog: vi.fn(),
      addLogs: vi.fn(),
      clearLogs: vi.fn(),
      setExecutionState: vi.fn(),
      setIsExecuting: vi.fn(),
      retryNode: vi.fn(),
      selectNode: vi.fn(),
      setLogDrawerOpen: vi.fn(),
      setLogFilter: vi.fn(),
      setLogSearchKeyword: vi.fn(),
      clearExecutionState: vi.fn(),
    })

    render(
      <ReactFlowProvider>
        <TaskNode {...mockTaskProps} />
      </ReactFlowProvider>
    )

    // 验证失败状态图标
    expect(screen.getByTestId('close-circle')).toBeInTheDocument()
  })

  it('应在运行状态显示黄色边框', () => {
    vi.mocked(useWorkflowStore).mockReturnValue({
      updateNodeStatus: vi.fn(),
      expandedSubWorkflowId: null,
      setExpandedSubWorkflowId: vi.fn(),
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'running',
        nodeStates: {
          'test-task-node': { status: 'running' },
        },
        logs: [],
      },
      logDrawerOpen: false,
      logFilter: 'ALL',
      logSearchKeyword: '',
      nodes: [],
      edges: [],
      isExecuting: false,
      selectedNodeId: null,
      setNodes: vi.fn(),
      setEdges: vi.fn(),
      updateNodeExecutionTime: vi.fn(),
      addLog: vi.fn(),
      addLogs: vi.fn(),
      clearLogs: vi.fn(),
      setExecutionState: vi.fn(),
      setIsExecuting: vi.fn(),
      retryNode: vi.fn(),
      selectNode: vi.fn(),
      setLogDrawerOpen: vi.fn(),
      setLogFilter: vi.fn(),
      setLogSearchKeyword: vi.fn(),
      clearExecutionState: vi.fn(),
    })

    render(
      <ReactFlowProvider>
        <TaskNode {...mockTaskProps} />
      </ReactFlowProvider>
    )

    // 验证运行状态图标
    expect(screen.getByTestId('sync')).toBeInTheDocument()
  })
})

describe('F033 - 执行时间', () => {
  const mockTaskProps = {
    id: 'test-task-node',
    data: {
      label: '测试任务',
      taskType: 'script' as const,
    },
    selected: false,
    type: 'task',
    position: { x: 0, y: 0 },
    width: 220,
    height: 200,
    zIndex: 1,
  }

  it('应显示节点执行耗时', () => {
    vi.mocked(useWorkflowStore).mockReturnValue({
      updateNodeStatus: vi.fn(),
      expandedSubWorkflowId: null,
      setExpandedSubWorkflowId: vi.fn(),
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'success',
        nodeStates: {
          'test-task-node': { 
            status: 'success',
            executionTime: 2500
          },
        },
        logs: [],
      },
      logDrawerOpen: false,
      logFilter: 'ALL',
      logSearchKeyword: '',
      nodes: [],
      edges: [],
      isExecuting: false,
      selectedNodeId: null,
      setNodes: vi.fn(),
      setEdges: vi.fn(),
      updateNodeExecutionTime: vi.fn(),
      addLog: vi.fn(),
      addLogs: vi.fn(),
      clearLogs: vi.fn(),
      setExecutionState: vi.fn(),
      setIsExecuting: vi.fn(),
      retryNode: vi.fn(),
      selectNode: vi.fn(),
      setLogDrawerOpen: vi.fn(),
      setLogFilter: vi.fn(),
      setLogSearchKeyword: vi.fn(),
      clearExecutionState: vi.fn(),
    })

    render(
      <ReactFlowProvider>
        <TaskNode {...mockTaskProps} />
      </ReactFlowProvider>
    )

    expect(screen.getByText('⏱️ 耗时：2500ms')).toBeInTheDocument()
  })

  it('应格式化长时间显示', () => {
    vi.mocked(useWorkflowStore).mockReturnValue({
      ...vi.mocked(useWorkflowStore).mockReturnValue,
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'success',
        nodeStates: {
          'test-task-node': { 
            status: 'success',
            executionTime: 65000
          },
        },
        logs: [],
      },
    })

    render(
      <ReactFlowProvider>
        <TaskNode {...mockTaskProps} />
      </ReactFlowProvider>
    )

    expect(screen.getByText('⏱️ 耗时：65000ms')).toBeInTheDocument()
  })

  it('应在未执行时不显示时间', () => {
    vi.mocked(useWorkflowStore).mockReturnValue({
      updateNodeStatus: vi.fn(),
      expandedSubWorkflowId: null,
      setExpandedSubWorkflowId: vi.fn(),
      executionState: null,
      logDrawerOpen: false,
      logFilter: 'ALL',
      logSearchKeyword: '',
      nodes: [],
      edges: [],
      isExecuting: false,
      selectedNodeId: null,
      setNodes: vi.fn(),
      setEdges: vi.fn(),
      updateNodeExecutionTime: vi.fn(),
      addLog: vi.fn(),
      addLogs: vi.fn(),
      clearLogs: vi.fn(),
      setExecutionState: vi.fn(),
      setIsExecuting: vi.fn(),
      retryNode: vi.fn(),
      selectNode: vi.fn(),
      setLogDrawerOpen: vi.fn(),
      setLogFilter: vi.fn(),
      setLogSearchKeyword: vi.fn(),
      clearExecutionState: vi.fn(),
    })

    render(
      <ReactFlowProvider>
        <TaskNode {...mockTaskProps} />
      </ReactFlowProvider>
    )

    expect(screen.queryByText(/⏱️/)).not.toBeInTheDocument()
  })
})

describe('F034 - 重试按钮', () => {
  const mockTaskProps = {
    id: 'test-task-node',
    data: {
      label: '测试任务',
      taskType: 'script' as const,
    },
    selected: false,
    type: 'task',
    position: { x: 0, y: 0 },
    width: 220,
    height: 200,
    zIndex: 1,
  }

  it('应在失败节点显示重试按钮', () => {
    vi.mocked(useWorkflowStore).mockReturnValue({
      updateNodeStatus: vi.fn(),
      expandedSubWorkflowId: null,
      setExpandedSubWorkflowId: vi.fn(),
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'failed',
        nodeStates: {
          'test-task-node': { 
            status: 'failed',
            error: '执行失败'
          },
        },
        logs: [],
      },
      logDrawerOpen: false,
      logFilter: 'ALL',
      logSearchKeyword: '',
      nodes: [],
      edges: [],
      isExecuting: false,
      selectedNodeId: null,
      setNodes: vi.fn(),
      setEdges: vi.fn(),
      updateNodeExecutionTime: vi.fn(),
      addLog: vi.fn(),
      addLogs: vi.fn(),
      clearLogs: vi.fn(),
      setExecutionState: vi.fn(),
      setIsExecuting: vi.fn(),
      retryNode: vi.fn(),
      selectNode: vi.fn(),
      setLogDrawerOpen: vi.fn(),
      setLogFilter: vi.fn(),
      setLogSearchKeyword: vi.fn(),
      clearExecutionState: vi.fn(),
    })

    render(
      <ReactFlowProvider>
        <TaskNode {...mockTaskProps} />
      </ReactFlowProvider>
    )

    expect(screen.getByText('重试')).toBeInTheDocument()
  })

  it('应点击重试按钮调用重试函数', () => {
    const mockRetryNode = vi.fn()
    vi.mocked(useWorkflowStore).mockReturnValue({
      updateNodeStatus: vi.fn(),
      expandedSubWorkflowId: null,
      setExpandedSubWorkflowId: vi.fn(),
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'failed',
        nodeStates: {
          'test-task-node': { 
            status: 'failed',
            error: '执行失败'
          },
        },
        logs: [],
      },
      logDrawerOpen: false,
      logFilter: 'ALL',
      logSearchKeyword: '',
      nodes: [],
      edges: [],
      isExecuting: false,
      selectedNodeId: null,
      setNodes: vi.fn(),
      setEdges: vi.fn(),
      updateNodeExecutionTime: vi.fn(),
      addLog: vi.fn(),
      addLogs: vi.fn(),
      clearLogs: vi.fn(),
      setExecutionState: vi.fn(),
      setIsExecuting: vi.fn(),
      retryNode: mockRetryNode,
      selectNode: vi.fn(),
      setLogDrawerOpen: vi.fn(),
      setLogFilter: vi.fn(),
      setLogSearchKeyword: vi.fn(),
      clearExecutionState: vi.fn(),
    })

    render(
      <ReactFlowProvider>
        <TaskNode {...mockTaskProps} />
      </ReactFlowProvider>
    )

    const retryButton = screen.getByText('重试')
    fireEvent.click(retryButton)

    expect(mockRetryNode).toHaveBeenCalledWith('test-task-node')
  })

  it('应在非失败状态不显示重试按钮', () => {
    vi.mocked(useWorkflowStore).mockReturnValue({
      updateNodeStatus: vi.fn(),
      expandedSubWorkflowId: null,
      setExpandedSubWorkflowId: vi.fn(),
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'success',
        nodeStates: {
          'test-task-node': { status: 'success' },
        },
        logs: [],
      },
      logDrawerOpen: false,
      logFilter: 'ALL',
      logSearchKeyword: '',
      nodes: [],
      edges: [],
      isExecuting: false,
      selectedNodeId: null,
      setNodes: vi.fn(),
      setEdges: vi.fn(),
      updateNodeExecutionTime: vi.fn(),
      addLog: vi.fn(),
      addLogs: vi.fn(),
      clearLogs: vi.fn(),
      setExecutionState: vi.fn(),
      setIsExecuting: vi.fn(),
      retryNode: vi.fn(),
      selectNode: vi.fn(),
      setLogDrawerOpen: vi.fn(),
      setLogFilter: vi.fn(),
      setLogSearchKeyword: vi.fn(),
      clearExecutionState: vi.fn(),
    })

    render(
      <ReactFlowProvider>
        <TaskNode {...mockTaskProps} />
      </ReactFlowProvider>
    )

    expect(screen.queryByText('重试')).not.toBeInTheDocument()
  })
})
