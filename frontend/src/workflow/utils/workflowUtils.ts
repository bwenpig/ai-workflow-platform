import { Node, Edge } from '@xyflow/react'
import { WorkflowNodeType, createDefaultNodeData } from '../nodes'
import { NodeStatus } from '../store/useWorkflowStore'

/**
 * 创建工作流节点
 */
export const createWorkflowNode = (
  type: WorkflowNodeType,
  position: { x: number; y: number },
  label?: string
): Node => {
  return {
    id: `node-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
    type: type,
    position,
    data: {
      ...createDefaultNodeData(type),
      label: label || getDefaultLabel(type),
    },
  }
}

/**
 * 获取默认节点标签
 */
const getDefaultLabel = (type: WorkflowNodeType): string => {
  switch (type) {
    case WorkflowNodeType.IF:
      return '条件分支'
    case WorkflowNodeType.PARALLEL:
      return '并行执行'
    case WorkflowNodeType.MERGE:
      return '合并节点'
    case WorkflowNodeType.SUB_WORKFLOW:
      return '子工作流'
    case WorkflowNodeType.TASK:
      return '任务节点'
    default:
      return '节点'
  }
}

/**
 * 计算节点执行状态颜色
 */
export const getNodeStatusColor = (status?: NodeStatus): string => {
  switch (status) {
    case 'success':
      return '#52c41a' // 绿色
    case 'failed':
      return '#ff4d4f' // 红色
    case 'running':
      return '#faad14' // 黄色
    case 'skipped':
      return '#d9d9d9' // 灰色
    default:
      return '#d9d9d9' // 灰色
  }
}

/**
 * 格式化执行时间
 */
export const formatExecutionTime = (ms?: number): string => {
  if (ms === undefined) return '-'
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  return `${(ms / 60000).toFixed(1)}m`
}

/**
 * 验证 IF 节点配置
 */
export const validateIfNode = (data: any): { valid: boolean; error?: string } => {
  if (!data.variable || data.variable.trim() === '') {
    return { valid: false, error: '请设置条件变量' }
  }
  if (!data.value || data.value.trim() === '') {
    return { valid: false, error: '请设置条件值' }
  }
  return { valid: true }
}

/**
 * 验证合并节点配置
 */
export const validateMergeNode = (data: any, incomingEdges: Edge[]): { valid: boolean; error?: string } => {
  if (incomingEdges.length < 2) {
    return { valid: false, error: '合并节点至少需要 2 个输入' }
  }
  return { valid: true }
}

/**
 * 获取节点输入边
 */
export const getNodeIncomingEdges = (nodeId: string, edges: Edge[]): Edge[] => {
  return edges.filter(edge => edge.target === nodeId)
}

/**
 * 获取节点输出边
 */
export const getNodeOutgoingEdges = (nodeId: string, edges: Edge[]): Edge[] => {
  return edges.filter(edge => edge.source === nodeId)
}

/**
 * 检查工作流是否有环
 */
export const hasCycle = (nodes: Node[], edges: Edge[]): boolean => {
  const graph = new Map<string, string[]>()
  
  // 构建邻接表
  nodes.forEach(node => {
    graph.set(node.id, [])
  })
  edges.forEach(edge => {
    const sources = graph.get(edge.source)
    if (sources) {
      sources.push(edge.target)
    }
  })
  
  // DFS 检测环
  const visited = new Set<string>()
  const recursionStack = new Set<string>()
  
  const dfs = (nodeId: string): boolean => {
    visited.add(nodeId)
    recursionStack.add(nodeId)
    
    const neighbors = graph.get(nodeId) || []
    for (const neighbor of neighbors) {
      if (!visited.has(neighbor)) {
        if (dfs(neighbor)) return true
      } else if (recursionStack.has(neighbor)) {
        return true
      }
    }
    
    recursionStack.delete(nodeId)
    return false
  }
  
  for (const nodeId of graph.keys()) {
    if (!visited.has(nodeId)) {
      if (dfs(nodeId)) return true
    }
  }
  
  return false
}

/**
 * 拓扑排序节点
 */
export const topologicalSort = (nodes: Node[], edges: Edge[]): string[] => {
  const inDegree = new Map<string, number>()
  const graph = new Map<string, string[]>()
  
  // 初始化
  nodes.forEach(node => {
    inDegree.set(node.id, 0)
    graph.set(node.id, [])
  })
  
  // 构建图
  edges.forEach(edge => {
    const targets = graph.get(edge.source)
    if (targets) {
      targets.push(edge.target)
    }
    const degree = inDegree.get(edge.target) || 0
    inDegree.set(edge.target, degree + 1)
  })
  
  // Kahn 算法
  const queue: string[] = []
  inDegree.forEach((degree, nodeId) => {
    if (degree === 0) {
      queue.push(nodeId)
    }
  })
  
  const result: string[] = []
  while (queue.length > 0) {
    const nodeId = queue.shift()!
    result.push(nodeId)
    
    const neighbors = graph.get(nodeId) || []
    for (const neighbor of neighbors) {
      const degree = inDegree.get(neighbor)! - 1
      inDegree.set(neighbor, degree)
      if (degree === 0) {
        queue.push(neighbor)
      }
    }
  }
  
  return result
}
