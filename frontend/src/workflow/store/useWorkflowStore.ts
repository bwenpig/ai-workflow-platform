import { create } from 'zustand'
import { Node, Edge } from '@xyflow/react'

// 节点状态枚举
export type NodeStatus = 'pending' | 'running' | 'success' | 'failed' | 'skipped'

// 日志级别
export type LogLevel = 'INFO' | 'WARN' | 'ERROR' | 'DEBUG'

// 日志条目
export interface LogEntry {
  id: string
  nodeId: string
  level: LogLevel
  message: string
  timestamp: string
  details?: any
}

// 节点执行信息
export interface NodeExecutionInfo {
  status: NodeStatus
  executionTime?: number // 执行耗时 (ms)
  startTime?: string
  endTime?: string
  error?: string
  retryCount?: number
  result?: any
}

// 工作流执行状态
export interface WorkflowExecutionState {
  workflowId: string
  executionId: string
  status: 'pending' | 'running' | 'success' | 'failed'
  nodeStates: Record<string, NodeExecutionInfo>
  logs: LogEntry[]
  startTime?: string
  endTime?: string
}

// 工作流 Store 状态
interface WorkflowState {
  // 节点相关
  nodes: Node[]
  edges: Edge[]
  
  // 执行状态
  executionState: WorkflowExecutionState | null
  isExecuting: boolean
  selectedNodeId: string | null
  
  // 日志面板
  logDrawerOpen: boolean
  logFilter: LogLevel | 'ALL'
  logSearchKeyword: string
  
  // 子工作流
  expandedSubWorkflowId: string | null
  
  // Actions
  setNodes: (nodes: Node[]) => void
  setEdges: (edges: Edge[]) => void
  updateNodeStatus: (nodeId: string, status: NodeStatus, info?: Partial<NodeExecutionInfo>) => void
  updateNodeExecutionTime: (nodeId: string, executionTime: number) => void
  addLog: (log: LogEntry) => void
  addLogs: (logs: LogEntry[]) => void
  clearLogs: (nodeId?: string) => void
  
  // 执行控制
  setExecutionState: (state: WorkflowExecutionState | null) => void
  setIsExecuting: (executing: boolean) => void
  retryNode: (nodeId: string) => Promise<void>
  
  // 选择
  selectNode: (nodeId: string | null) => void
  
  // 日志面板
  setLogDrawerOpen: (open: boolean) => void
  setLogFilter: (filter: LogLevel | 'ALL') => void
  setLogSearchKeyword: (keyword: string) => void
  
  // 子工作流
  setExpandedSubWorkflowId: (id: string | null) => void
  
  // 清空
  clearExecutionState: () => void
}

// 创建初始执行状态
const createInitialExecutionState = (workflowId: string, executionId: string): WorkflowExecutionState => ({
  workflowId,
  executionId,
  status: 'pending',
  nodeStates: {},
  logs: [],
})

export const useWorkflowStore = create<WorkflowState>((set, get) => ({
  // 初始状态
  nodes: [],
  edges: [],
  executionState: null,
  isExecuting: false,
  selectedNodeId: null,
  logDrawerOpen: false,
  logFilter: 'ALL',
  logSearchKeyword: '',
  expandedSubWorkflowId: null,
  
  // Set nodes
  setNodes: (nodes) => set({ nodes }),
  
  // Set edges
  setEdges: (edges) => set({ edges }),
  
  // 更新节点状态
  updateNodeStatus: (nodeId, status, info = {}) => {
    set((state) => {
      const currentNodeState = state.executionState?.nodeStates[nodeId] || {}
      
      const updatedNodeState: NodeExecutionInfo = {
        ...currentNodeState,
        status,
        ...info,
      }
      
      // 自动设置执行时间
      if (status === 'running' && !currentNodeState.startTime) {
        updatedNodeState.startTime = new Date().toISOString()
      }
      if ((status === 'success' || status === 'failed') && !currentNodeState.endTime) {
        updatedNodeState.endTime = new Date().toISOString()
        if (updatedNodeState.startTime) {
          updatedNodeState.executionTime = Math.floor(
            (new Date(updatedNodeState.endTime).getTime() - new Date(updatedNodeState.startTime).getTime())
          )
        }
      }
      
      return {
        executionState: state.executionState ? {
          ...state.executionState,
          nodeStates: {
            ...state.executionState.nodeStates,
            [nodeId]: updatedNodeState,
          },
        } : null,
      }
    })
  },
  
  // 更新节点执行时间
  updateNodeExecutionTime: (nodeId, executionTime) => {
    set((state) => {
      if (!state.executionState) return state
      
      const currentNodeState = state.executionState.nodeStates[nodeId] || {}
      
      return {
        executionState: {
          ...state.executionState,
          nodeStates: {
            ...state.executionState.nodeStates,
            [nodeId]: {
              ...currentNodeState,
              executionTime,
            },
          },
        },
      }
    })
  },
  
  // 添加日志
  addLog: (log) => {
    set((state) => {
      if (!state.executionState) return state
      
      return {
        executionState: {
          ...state.executionState,
          logs: [...state.executionState.logs, log],
        },
      }
    })
  },
  
  // 批量添加日志
  addLogs: (logs) => {
    set((state) => {
      if (!state.executionState) return state
      
      return {
        executionState: {
          ...state.executionState,
          logs: [...state.executionState.logs, ...logs],
        },
      }
    })
  },
  
  // 清空日志
  clearLogs: (nodeId) => {
    set((state) => {
      if (!state.executionState) return state
      
      return {
        executionState: {
          ...state.executionState,
          logs: nodeId
            ? state.executionState.logs.filter(log => log.nodeId !== nodeId)
            : [],
        },
      }
    })
  },
  
  // 设置执行状态
  setExecutionState: (state) => set({ executionState: state }),
  
  // 设置执行中状态
  setIsExecuting: (isExecuting) => set({ isExecuting }),
  
  // 重试节点
  retryNode: async (nodeId) => {
    const state = get()
    if (!state.executionState) return
    
    const nodeState = state.executionState.nodeStates[nodeId]
    if (!nodeState || nodeState.status !== 'failed') return
    
    // 更新状态为 pending
    get().updateNodeStatus(nodeId, 'pending', {
      retryCount: (nodeState.retryCount || 0) + 1,
      error: undefined,
    })
    
    // TODO: 调用后端 API 重试节点
    console.log(`Retrying node ${nodeId}, attempt ${nodeState.retryCount || 0 + 1}`)
  },
  
  // 选择节点
  selectNode: (nodeId) => set({ selectedNodeId: nodeId }),
  
  // 设置日志面板开关
  setLogDrawerOpen: (open) => set({ logDrawerOpen: open }),
  
  // 设置日志过滤
  setLogFilter: (filter) => set({ logFilter: filter }),
  
  // 设置日志搜索关键词
  setLogSearchKeyword: (keyword) => set({ logSearchKeyword: keyword }),
  
  // 设置展开的子工作流
  setExpandedSubWorkflowId: (id) => set({ expandedSubWorkflowId: id }),
  
  // 清空执行状态
  clearExecutionState: () => set({
    executionState: null,
    isExecuting: false,
    logDrawerOpen: false,
    logSearchKeyword: '',
    logFilter: 'ALL',
  }),
}))
