import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { SubWorkflowNode } from '../../nodes/SubWorkflowNode'
import { useWorkflowStore } from '../../../store/useWorkflowStore'
import { ReactFlowProvider } from '@xyflow/react'

vi.mock('../../../store/useWorkflowStore', () => ({
  useWorkflowStore: vi.fn(),
}))

describe('F027 - 子工作流 (嵌套工作流 + 展开折叠)', () => {
  const mockSubWorkflowProps = {
    id: 'test-subworkflow-node',
    data: {
      label: '测试子工作流',
      subWorkflowId: 'sub-workflow-123',
      subWorkflowName: '数据处理子流程',
    },
    selected: false,
    type: 'subWorkflow',
    position: { x: 0, y: 0 },
    width: 220,
    height: 200,
    zIndex: 1,
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('应渲染子工作流节点', () => {
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
        <SubWorkflowNode {...mockSubWorkflowProps} />
      </ReactFlowProvider>
    )

    expect(screen.getByText('测试子工作流')).toBeInTheDocument()
    expect(screen.getByText('数据处理子流程')).toBeInTheDocument()
  })

  it('应显示展开/折叠按钮', () => {
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
        <SubWorkflowNode {...mockSubWorkflowProps} />
      </ReactFlowProvider>
    )

    expect(screen.getByText('展开子流程')).toBeInTheDocument()
  })

  it('应可展开显示子工作流内容', () => {
    const mockSetExpandedSubWorkflowId = vi.fn()
    vi.mocked(useWorkflowStore).mockReturnValue({
      updateNodeStatus: vi.fn(),
      expandedSubWorkflowId: null,
      setExpandedSubWorkflowId: mockSetExpandedSubWorkflowId,
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
        <SubWorkflowNode {...mockSubWorkflowProps} />
      </ReactFlowProvider>
    )

    const expandButton = screen.getByText('展开子流程')
    fireEvent.click(expandButton)

    expect(mockSetExpandedSubWorkflowId).toHaveBeenCalledWith('test-subworkflow-node')
  })

  it('应显示已展开的子工作流预览', () => {
    vi.mocked(useWorkflowStore).mockReturnValue({
      updateNodeStatus: vi.fn(),
      expandedSubWorkflowId: 'test-subworkflow-node',
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
        <SubWorkflowNode {...mockSubWorkflowProps} />
      </ReactFlowProvider>
    )

    expect(screen.getByText('收起子流程')).toBeInTheDocument()
    expect(screen.getByText('📋 子工作流节点预览')).toBeInTheDocument()
    expect(screen.getByText('• 子节点 1: 数据预处理')).toBeInTheDocument()
  })

  it('应可收起已展开的子工作流', () => {
    const mockSetExpandedSubWorkflowId = vi.fn()
    vi.mocked(useWorkflowStore).mockReturnValue({
      updateNodeStatus: vi.fn(),
      expandedSubWorkflowId: 'test-subworkflow-node',
      setExpandedSubWorkflowId: mockSetExpandedSubWorkflowId,
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
        <SubWorkflowNode {...mockSubWorkflowProps} />
      </ReactFlowProvider>
    )

    const collapseButton = screen.getByText('收起子流程')
    fireEvent.click(collapseButton)

    expect(mockSetExpandedSubWorkflowId).toHaveBeenCalledWith(null)
  })
})
