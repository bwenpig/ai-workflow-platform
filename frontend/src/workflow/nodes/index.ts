import { IfNode } from './IfNode'
import { ParallelNode } from './ParallelNode'
import { MergeNode } from './MergeNode'
import { SubWorkflowNode } from './SubWorkflowNode'
import { TaskNode } from './TaskNode'
import HttpRequestNode from './HttpRequestNode'
import ConditionalNode from './ConditionalNode'
import LoopNode from './LoopNode'
import EmailNode from './EmailNode'
import WxPushNode from './WxPushNode'

export { IfNode, ParallelNode, MergeNode, SubWorkflowNode, TaskNode }
export { HttpRequestNode, ConditionalNode, LoopNode, EmailNode, WxPushNode }

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
  email: EmailNode,
  wx_push: WxPushNode,
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
  EMAIL = 'email',
  WX_PUSH = 'wx_push',
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
    case WorkflowNodeType.EMAIL:
      return {
        label: '邮件发送',
        to: [],
        cc: [],
        subject: '',
        body: '',
        from: '',
        attachments: [],
      }
    case WorkflowNodeType.WX_PUSH:
      return {
        label: '微信推送',
        to: '',
        content: '',
        silent: false,
      }
    default:
      return { label: '节点' }
  }
}
