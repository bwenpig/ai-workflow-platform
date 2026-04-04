export { IfNode } from './IfNode'
export { ParallelNode } from './ParallelNode'
export { MergeNode } from './MergeNode'
export { SubWorkflowNode } from './SubWorkflowNode'
export { TaskNode } from './TaskNode'
export { default as HttpRequestNode } from './HttpRequestNode'
export { default as ConditionalNode } from './ConditionalNode'
export { default as LoopNode } from './LoopNode'

// 节点类型映射
export const nodeTypes = {
  if: IfNode,
  parallel: ParallelNode,
  merge: MergeNode,
  subWorkflow: SubWorkflowNode,
  task: TaskNode,
  http_request: HttpRequestNode,
  conditional: ConditionalNode,
  loop: LoopNode,
}

// 节点类型枚举
export enum WorkflowNodeType {
  IF = 'if',
  PARALLEL = 'parallel',
  MERGE = 'merge',
  SUB_WORKFLOW = 'subWorkflow',
  TASK = 'task',
  HTTP_REQUEST = 'http_request',
  CONDITIONAL = 'conditional',
  LOOP = 'loop',
}

// 创建默认节点数据
export const createDefaultNodeData = (type: WorkflowNodeType) => {
  switch (type) {
    case WorkflowNodeType.IF:
      return {
        label: '条件分支',
        condition: '',
        operator: 'equals' as const,
        variable: '',
        value: '',
      }
    case WorkflowNodeType.PARALLEL:
      return {
        label: '并行执行',
        parallelCount: 2,
        branchCount: 2,
      }
    case WorkflowNodeType.MERGE:
      return {
        label: '合并节点',
        mergeStrategy: 'all' as const,
        inputCount: 2,
      }
    case WorkflowNodeType.SUB_WORKFLOW:
      return {
        label: '子工作流',
        subWorkflowId: '',
        subWorkflowName: '',
      }
    case WorkflowNodeType.TASK:
      return {
        label: '任务节点',
        taskType: 'script' as const,
      }
    case WorkflowNodeType.HTTP_REQUEST:
      return {
        label: 'HTTP 请求',
        url: 'https://api.example.com',
        method: 'GET',
        timeout: 5000,
        headers: {},
      }
    case WorkflowNodeType.CONDITIONAL:
      return {
        label: '条件判断',
        expression: '',
        value: '',
      }
    case WorkflowNodeType.LOOP:
      return {
        label: '循环处理',
        items: [],
        itemVar: 'item',
        indexVar: 'index',
        concurrency: 1,
        maxIterations: 100,
      }
    default:
      return { label: '节点' }
  }
}
