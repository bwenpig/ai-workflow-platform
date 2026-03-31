import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { IfNode } from '../../nodes/IfNode'
import { useWorkflowStore } from '../../../store/useWorkflowStore'
import { ReactFlowProvider } from '@xyflow/react'

// Mock useWorkflowStore
vi.mock('../../../store/useWorkflowStore', () => ({
  useWorkflowStore: vi.fn(),
}))

describe('F024 - 条件分支 (IF Node)', () => {
  const mockNodeProps = {
    id: 'test-if-node',
    data: {
      label: '测试条件分支',
      variable: 'input.value',
      operator: 'equals' as const,
      value: 'true',
    },
    selected: false,
    type: 'if',
    position: { x: 0, y: 0 },
    width: 220,
    height: 200,
    zIndex: 1,
  }

  beforeEach(() => {
    vi.clearAllMocks()
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
  })

  it('应渲染 IF 节点并显示两个输出端口 (True/False)', () => {
    render(
      <ReactFlowProvider>
        <IfNode {...mockNodeProps} />
      </ReactFlowProvider>
    )

    expect(screen.getByText('测试条件分支')).toBeInTheDocument()
    // 检查 True/False 标签
    expect(screen.getByText('True')).toBeInTheDocument()
    expect(screen.getByText('False')).toBeInTheDocument()
  })

  it('应允许编辑条件变量和值', () => {
    const mockSetNodes = vi.fn()
    vi.mocked(useWorkflowStore).mockReturnValue({
      ...vi.mocked(useWorkflowStore).mockReturnValue,
      setNodes: mockSetNodes,
    })

    render(
      <ReactFlowProvider>
        <IfNode {...mockNodeProps} />
      </ReactFlowProvider>
    )

    const variableInput = screen.getByPlaceholderText('例如：input.value')
    fireEvent.change(variableInput, { target: { value: 'new.variable' } })

    expect(mockSetNodes).toHaveBeenCalled()
  })

  it('应根据状态显示不同的边框颜色', () => {
    // 测试成功状态（绿色）
    vi.mocked(useWorkflowStore).mockReturnValue({
      ...vi.mocked(useWorkflowStore).mockReturnValue,
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'success',
        nodeStates: {
          'test-if-node': { status: 'success' },
        },
        logs: [],
      },
    })

    render(
      <ReactFlowProvider>
        <IfNode {...mockNodeProps} />
      </ReactFlowProvider>
    )

    // 验证节点渲染成功状态图标
    expect(screen.getByTestId('check-circle')).toBeInTheDocument()
  })
})

describe('F025 - 并行执行 (Parallel Node)', () => {
  const mockParallelProps = {
    id: 'test-parallel-node',
    data: {
      label: '测试并行执行',
      parallelCount: 3,
      branchCount: 3,
    },
    selected: false,
    type: 'parallel',
    position: { x: 0, y: 0 },
    width: 200,
    height: 180,
    zIndex: 1,
  }

  beforeEach(() => {
    vi.clearAllMocks()
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
  })

  it('应渲染并行节点并显示并行数量', () => {
    const { ParallelNode } = require('../../nodes/ParallelNode')
    render(
      <ReactFlowProvider>
        <ParallelNode {...mockParallelProps} />
      </ReactFlowProvider>
    )

    expect(screen.getByText('测试并行执行')).toBeInTheDocument()
    expect(screen.getByText('3 路并行')).toBeInTheDocument()
  })

  it('应在执行中显示进度条', () => {
    vi.mocked(useWorkflowStore).mockReturnValue({
      ...vi.mocked(useWorkflowStore).mockReturnValue,
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'running',
        nodeStates: {
          'test-parallel-node': { 
            status: 'running',
            result: { progress: 60 }
          },
        },
        logs: [],
      },
    })

    const { ParallelNode } = require('../../nodes/ParallelNode')
    render(
      <ReactFlowProvider>
        <ParallelNode {...mockParallelProps} />
      </ReactFlowProvider>
    )

    expect(screen.getByText('60%')).toBeInTheDocument()
  })

  it('应显示执行时间', () => {
    vi.mocked(useWorkflowStore).mockReturnValue({
      ...vi.mocked(useWorkflowStore).mockReturnValue,
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'success',
        nodeStates: {
          'test-parallel-node': { 
            status: 'success',
            executionTime: 1500
          },
        },
        logs: [],
      },
    })

    const { ParallelNode } = require('../../nodes/ParallelNode')
    render(
      <ReactFlowProvider>
        <ParallelNode {...mockParallelProps} />
      </ReactFlowProvider>
    )

    expect(screen.getByText('⏱️ 1500ms')).toBeInTheDocument()
  })
})

describe('F026 - 合并节点 (Merge Node)', () => {
  const mockMergeProps = {
    id: 'test-merge-node',
    data: {
      label: '测试合并节点',
      mergeStrategy: 'all' as const,
      inputCount: 3,
      totalInputs: 3,
    },
    selected: false,
    type: 'merge',
    position: { x: 0, y: 0 },
    width: 200,
    height: 200,
    zIndex: 1,
  }

  beforeEach(() => {
    vi.clearAllMocks()
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
  })

  it('应渲染合并节点并显示汇聚策略', () => {
    const { MergeNode } = require('../../nodes/MergeNode')
    render(
      <ReactFlowProvider>
        <MergeNode {...mockMergeProps} />
      </ReactFlowProvider>
    )

    expect(screen.getByText('测试合并节点')).toBeInTheDocument()
    expect(screen.getByText('等待全部')).toBeInTheDocument()
  })

  it('应显示多个输入端口', () => {
    const { MergeNode } = require('../../nodes/MergeNode')
    render(
      <ReactFlowProvider>
        <MergeNode {...mockMergeProps} />
      </ReactFlowProvider>
    )

    // 检查输入端口的 Handle
    const handles = document.querySelectorAll('[data-handleid^="input-"]')
    expect(handles.length).toBeGreaterThanOrEqual(2)
  })

  it('应显示输入完成状态', () => {
    vi.mocked(useWorkflowStore).mockReturnValue({
      ...vi.mocked(useWorkflowStore).mockReturnValue,
      executionState: {
        workflowId: 'test',
        executionId: 'test',
        status: 'running',
        nodeStates: {
          'test-merge-node': { 
            status: 'running',
            result: { completedInputs: 2 }
          },
        },
        logs: [],
      },
    })

    const { MergeNode } = require('../../nodes/MergeNode')
    render(
      <ReactFlowProvider>
        <MergeNode {...mockMergeProps} />
      </ReactFlowProvider>
    )

    expect(screen.getByText('已汇聚 2/3')).toBeInTheDocument()
  })
})
