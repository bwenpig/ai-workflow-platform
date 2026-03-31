import { describe, it, expect, beforeEach } from 'vitest'
import { useWorkflowStore } from '../store/useWorkflowStore'

describe('useWorkflowStore', () => {
  beforeEach(() => {
    // 重置 store 状态
    useWorkflowStore.setState({
      nodes: [],
      edges: [],
      executionState: null,
      isExecuting: false,
      selectedNodeId: null,
      logDrawerOpen: false,
      logFilter: 'ALL',
      logSearchKeyword: '',
      expandedSubWorkflowId: null,
    })
  })

  describe('节点管理', () => {
    it('应设置节点', () => {
      const mockNodes = [
        { id: 'node1', position: { x: 0, y: 0 }, data: {} },
        { id: 'node2', position: { x: 100, y: 100 }, data: {} },
      ]

      useWorkflowStore.getState().setNodes(mockNodes as any)
      expect(useWorkflowStore.getState().nodes).toHaveLength(2)
    })

    it('应设置边', () => {
      const mockEdges = [
        { id: 'e1', source: 'node1', target: 'node2' },
      ]

      useWorkflowStore.getState().setEdges(mockEdges as any)
      expect(useWorkflowStore.getState().edges).toHaveLength(1)
    })
  })

  describe('F028 - 节点状态更新', () => {
    it('应更新节点状态', () => {
      const executionState = {
        workflowId: 'wf-1',
        executionId: 'exec-1',
        status: 'running' as const,
        nodeStates: {},
        logs: [],
      }
      useWorkflowStore.getState().setExecutionState(executionState)

      useWorkflowStore.getState().updateNodeStatus('node1', 'running')
      
      const state = useWorkflowStore.getState()
      expect(state.executionState?.nodeStates['node1'].status).toBe('running')
    })

    it('应自动设置执行时间', () => {
      const executionState = {
        workflowId: 'wf-1',
        executionId: 'exec-1',
        status: 'running' as const,
        nodeStates: {},
        logs: [],
      }
      useWorkflowStore.getState().setExecutionState(executionState)

      // 设置为运行状态
      useWorkflowStore.getState().updateNodeStatus('node1', 'running')
      
      // 等待一小段时间
      const startTime = Date.now()
      
      // 设置为成功状态
      setTimeout(() => {
        useWorkflowStore.getState().updateNodeStatus('node1', 'success')
        
        const nodeState = useWorkflowStore.getState().executionState?.nodeStates['node1']
        expect(nodeState?.status).toBe('success')
        expect(nodeState?.executionTime).toBeGreaterThan(0)
      }, 100)
    })

    it('应更新节点执行时间', () => {
      const executionState = {
        workflowId: 'wf-1',
        executionId: 'exec-1',
        status: 'success' as const,
        nodeStates: {},
        logs: [],
      }
      useWorkflowStore.getState().setExecutionState(executionState)

      useWorkflowStore.getState().updateNodeExecutionTime('node1', 2500)
      
      const nodeState = useWorkflowStore.getState().executionState?.nodeStates['node1']
      expect(nodeState?.executionTime).toBe(2500)
    })
  })

  describe('F030-F032 - 日志管理', () => {
    it('应添加单条日志', () => {
      const executionState = {
        workflowId: 'wf-1',
        executionId: 'exec-1',
        status: 'running' as const,
        nodeStates: {},
        logs: [],
      }
      useWorkflowStore.getState().setExecutionState(executionState)

      useWorkflowStore.getState().addLog({
        id: 'log-1',
        nodeId: 'node1',
        level: 'INFO',
        message: '测试日志',
        timestamp: new Date().toISOString(),
      })

      expect(useWorkflowStore.getState().executionState?.logs).toHaveLength(1)
    })

    it('应批量添加日志', () => {
      const executionState = {
        workflowId: 'wf-1',
        executionId: 'exec-1',
        status: 'running' as const,
        nodeStates: {},
        logs: [],
      }
      useWorkflowStore.getState().setExecutionState(executionState)

      useWorkflowStore.getState().addLogs([
        { id: 'log-1', nodeId: 'node1', level: 'INFO', message: '日志 1', timestamp: new Date().toISOString() },
        { id: 'log-2', nodeId: 'node2', level: 'ERROR', message: '日志 2', timestamp: new Date().toISOString() },
      ])

      expect(useWorkflowStore.getState().executionState?.logs).toHaveLength(2)
    })

    it('应清空日志', () => {
      const executionState = {
        workflowId: 'wf-1',
        executionId: 'exec-1',
        status: 'running' as const,
        nodeStates: {},
        logs: [
          { id: 'log-1', nodeId: 'node1', level: 'INFO', message: '日志 1', timestamp: new Date().toISOString() },
        ],
      }
      useWorkflowStore.getState().setExecutionState(executionState)

      useWorkflowStore.getState().clearLogs()
      expect(useWorkflowStore.getState().executionState?.logs).toHaveLength(0)
    })

    it('应清空指定节点的日志', () => {
      const executionState = {
        workflowId: 'wf-1',
        executionId: 'exec-1',
        status: 'running' as const,
        nodeStates: {},
        logs: [
          { id: 'log-1', nodeId: 'node1', level: 'INFO', message: '日志 1', timestamp: new Date().toISOString() },
          { id: 'log-2', nodeId: 'node2', level: 'INFO', message: '日志 2', timestamp: new Date().toISOString() },
        ],
      }
      useWorkflowStore.getState().setExecutionState(executionState)

      useWorkflowStore.getState().clearLogs('node1')
      expect(useWorkflowStore.getState().executionState?.logs).toHaveLength(1)
      expect(useWorkflowStore.getState().executionState?.logs[0].nodeId).toBe('node2')
    })
  })

  describe('F031 - 日志过滤', () => {
    it('应设置日志过滤级别', () => {
      useWorkflowStore.getState().setLogFilter('ERROR')
      expect(useWorkflowStore.getState().logFilter).toBe('ERROR')
    })

    it('应重置为 ALL', () => {
      useWorkflowStore.getState().setLogFilter('ERROR')
      useWorkflowStore.getState().setLogFilter('ALL')
      expect(useWorkflowStore.getState().logFilter).toBe('ALL')
    })
  })

  describe('F032 - 日志搜索', () => {
    it('应设置搜索关键词', () => {
      useWorkflowStore.getState().setLogSearchKeyword('错误')
      expect(useWorkflowStore.getState().logSearchKeyword).toBe('错误')
    })

    it('应清空搜索关键词', () => {
      useWorkflowStore.getState().setLogSearchKeyword('测试')
      useWorkflowStore.getState().setLogSearchKeyword('')
      expect(useWorkflowStore.getState().logSearchKeyword).toBe('')
    })
  })

  describe('F034 - 重试节点', () => {
    it('应可调用重试函数', async () => {
      const executionState = {
        workflowId: 'wf-1',
        executionId: 'exec-1',
        status: 'failed' as const,
        nodeStates: {
          'node1': { status: 'failed', retryCount: 0 },
        },
        logs: [],
      }
      useWorkflowStore.getState().setExecutionState(executionState)

      await useWorkflowStore.getState().retryNode('node1')
      
      const nodeState = useWorkflowStore.getState().executionState?.nodeStates['node1']
      expect(nodeState?.retryCount).toBe(1)
      expect(nodeState?.status).toBe('pending')
    })
  })

  describe('F027 - 子工作流展开折叠', () => {
    it('应设置展开的子工作流 ID', () => {
      useWorkflowStore.getState().setExpandedSubWorkflowId('sub-1')
      expect(useWorkflowStore.getState().expandedSubWorkflowId).toBe('sub-1')
    })

    it('应收起子工作流', () => {
      useWorkflowStore.getState().setExpandedSubWorkflowId('sub-1')
      useWorkflowStore.getState().setExpandedSubWorkflowId(null)
      expect(useWorkflowStore.getState().expandedSubWorkflowId).toBeNull()
    })
  })

  describe('执行状态管理', () => {
    it('应设置执行状态', () => {
      const executionState = {
        workflowId: 'wf-1',
        executionId: 'exec-1',
        status: 'running' as const,
        nodeStates: {},
        logs: [],
      }
      useWorkflowStore.getState().setExecutionState(executionState)
      expect(useWorkflowStore.getState().executionState).toEqual(executionState)
    })

    it('应设置执行中状态', () => {
      useWorkflowStore.getState().setIsExecuting(true)
      expect(useWorkflowStore.getState().isExecuting).toBe(true)
    })

    it('应清空执行状态', () => {
      const executionState = {
        workflowId: 'wf-1',
        executionId: 'exec-1',
        status: 'running' as const,
        nodeStates: {},
        logs: [],
      }
      useWorkflowStore.getState().setExecutionState(executionState)
      useWorkflowStore.getState().setIsExecuting(true)
      useWorkflowStore.getState().setLogDrawerOpen(true)

      useWorkflowStore.getState().clearExecutionState()
      
      expect(useWorkflowStore.getState().executionState).toBeNull()
      expect(useWorkflowStore.getState().isExecuting).toBe(false)
      expect(useWorkflowStore.getState().logDrawerOpen).toBe(false)
    })
  })

  describe('节点选择', () => {
    it('应选择节点', () => {
      useWorkflowStore.getState().selectNode('node1')
      expect(useWorkflowStore.getState().selectedNodeId).toBe('node1')
    })

    it('应取消选择', () => {
      useWorkflowStore.getState().selectNode('node1')
      useWorkflowStore.getState().selectNode(null)
      expect(useWorkflowStore.getState().selectedNodeId).toBeNull()
    })
  })

  describe('日志面板', () => {
    it('应打开日志面板', () => {
      useWorkflowStore.getState().setLogDrawerOpen(true)
      expect(useWorkflowStore.getState().logDrawerOpen).toBe(true)
    })

    it('应关闭日志面板', () => {
      useWorkflowStore.getState().setLogDrawerOpen(true)
      useWorkflowStore.getState().setLogDrawerOpen(false)
      expect(useWorkflowStore.getState().logDrawerOpen).toBe(false)
    })
  })
})
