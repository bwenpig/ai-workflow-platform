import { describe, it, expect } from 'vitest'
import {
  createWorkflowNode,
  getNodeStatusColor,
  formatExecutionTime,
  validateIfNode,
  validateMergeNode,
  getNodeIncomingEdges,
  getNodeOutgoingEdges,
  hasCycle,
  topologicalSort,
} from '../utils/workflowUtils'
import { WorkflowNodeType } from '../nodes'
import { Node, Edge } from '@xyflow/react'

describe('Workflow Utils', () => {
  describe('createWorkflowNode', () => {
    it('应创建 IF 节点', () => {
      const node = createWorkflowNode(WorkflowNodeType.IF, { x: 100, y: 100 })
      
      expect(node.id).toMatch(/node-\d+-\w+/)
      expect(node.type).toBe('if')
      expect(node.position).toEqual({ x: 100, y: 100 })
      expect(node.data.label).toBe('条件分支')
      expect(node.data.operator).toBe('equals')
    })

    it('应创建并行节点', () => {
      const node = createWorkflowNode(WorkflowNodeType.PARALLEL, { x: 200, y: 200 })
      
      expect(node.type).toBe('parallel')
      expect(node.data.label).toBe('并行执行')
      expect(node.data.parallelCount).toBe(2)
    })

    it('应创建合并节点', () => {
      const node = createWorkflowNode(WorkflowNodeType.MERGE, { x: 300, y: 300 })
      
      expect(node.type).toBe('merge')
      expect(node.data.label).toBe('合并节点')
      expect(node.data.mergeStrategy).toBe('all')
    })

    it('应创建子工作流节点', () => {
      const node = createWorkflowNode(WorkflowNodeType.SUB_WORKFLOW, { x: 400, y: 400 })
      
      expect(node.type).toBe('subWorkflow')
      expect(node.data.label).toBe('子工作流')
    })

    it('应创建任务节点', () => {
      const node = createWorkflowNode(WorkflowNodeType.TASK, { x: 500, y: 500 })
      
      expect(node.type).toBe('task')
      expect(node.data.label).toBe('任务节点')
      expect(node.data.taskType).toBe('script')
    })
  })

  describe('getNodeStatusColor', () => {
    it('应返回成功状态绿色', () => {
      expect(getNodeStatusColor('success')).toBe('#52c41a')
    })

    it('应返回失败状态红色', () => {
      expect(getNodeStatusColor('failed')).toBe('#ff4d4f')
    })

    it('应返回运行状态黄色', () => {
      expect(getNodeStatusColor('running')).toBe('#faad14')
    })

    it('应返回默认状态灰色', () => {
      expect(getNodeStatusColor('pending')).toBe('#d9d9d9')
      expect(getNodeStatusColor('skipped')).toBe('#d9d9d9')
      expect(getNodeStatusColor(undefined)).toBe('#d9d9d9')
    })
  })

  describe('formatExecutionTime', () => {
    it('应格式化毫秒', () => {
      expect(formatExecutionTime(500)).toBe('500ms')
    })

    it('应格式化秒', () => {
      expect(formatExecutionTime(2500)).toBe('2.5s')
    })

    it('应格式化分钟', () => {
      expect(formatExecutionTime(120000)).toBe('2.0m')
    })

    it('应处理未定义值', () => {
      expect(formatExecutionTime(undefined)).toBe('-')
    })
  })

  describe('validateIfNode', () => {
    it('应验证有效的 IF 节点配置', () => {
      const result = validateIfNode({
        variable: 'input.value',
        value: 'true',
      })
      
      expect(result.valid).toBe(true)
      expect(result.error).toBeUndefined()
    })

    it('应拒绝空变量', () => {
      const result = validateIfNode({
        variable: '',
        value: 'true',
      })
      
      expect(result.valid).toBe(false)
      expect(result.error).toBe('请设置条件变量')
    })

    it('应拒绝空值', () => {
      const result = validateIfNode({
        variable: 'input.value',
        value: '',
      })
      
      expect(result.valid).toBe(false)
      expect(result.error).toBe('请设置条件值')
    })
  })

  describe('validateMergeNode', () => {
    it('应验证有效的合并节点配置', () => {
      const edges: Edge[] = [
        { id: 'e1', source: 'node1', target: 'merge1' },
        { id: 'e2', source: 'node2', target: 'merge1' },
      ]
      
      const result = validateMergeNode({}, edges)
      
      expect(result.valid).toBe(true)
    })

    it('应拒绝少于 2 个输入的合并节点', () => {
      const edges: Edge[] = [
        { id: 'e1', source: 'node1', target: 'merge1' },
      ]
      
      const result = validateMergeNode({}, edges)
      
      expect(result.valid).toBe(false)
      expect(result.error).toBe('合并节点至少需要 2 个输入')
    })
  })

  describe('getNodeIncomingEdges', () => {
    it('应返回节点的输入边', () => {
      const edges: Edge[] = [
        { id: 'e1', source: 'node1', target: 'node2' },
        { id: 'e2', source: 'node3', target: 'node2' },
        { id: 'e3', source: 'node2', target: 'node4' },
      ]
      
      const incoming = getNodeIncomingEdges('node2', edges)
      
      expect(incoming.length).toBe(2)
      expect(incoming.map(e => e.id)).toEqual(['e1', 'e2'])
    })
  })

  describe('getNodeOutgoingEdges', () => {
    it('应返回节点的输出边', () => {
      const edges: Edge[] = [
        { id: 'e1', source: 'node1', target: 'node2' },
        { id: 'e2', source: 'node1', target: 'node3' },
        { id: 'e3', source: 'node2', target: 'node4' },
      ]
      
      const outgoing = getNodeOutgoingEdges('node1', edges)
      
      expect(outgoing.length).toBe(2)
      expect(outgoing.map(e => e.id)).toEqual(['e1', 'e2'])
    })
  })

  describe('hasCycle', () => {
    it('应检测无环图', () => {
      const nodes: Node[] = [
        { id: 'node1', position: { x: 0, y: 0 }, data: {} },
        { id: 'node2', position: { x: 0, y: 0 }, data: {} },
        { id: 'node3', position: { x: 0, y: 0 }, data: {} },
      ]
      
      const edges: Edge[] = [
        { id: 'e1', source: 'node1', target: 'node2' },
        { id: 'e2', source: 'node2', target: 'node3' },
      ]
      
      expect(hasCycle(nodes, edges)).toBe(false)
    })

    it('应检测有环图', () => {
      const nodes: Node[] = [
        { id: 'node1', position: { x: 0, y: 0 }, data: {} },
        { id: 'node2', position: { x: 0, y: 0 }, data: {} },
        { id: 'node3', position: { x: 0, y: 0 }, data: {} },
      ]
      
      const edges: Edge[] = [
        { id: 'e1', source: 'node1', target: 'node2' },
        { id: 'e2', source: 'node2', target: 'node3' },
        { id: 'e3', source: 'node3', target: 'node1' },
      ]
      
      expect(hasCycle(nodes, edges)).toBe(true)
    })
  })

  describe('topologicalSort', () => {
    it('应正确排序无环图', () => {
      const nodes: Node[] = [
        { id: 'node1', position: { x: 0, y: 0 }, data: {} },
        { id: 'node2', position: { x: 0, y: 0 }, data: {} },
        { id: 'node3', position: { x: 0, y: 0 }, data: {} },
      ]
      
      const edges: Edge[] = [
        { id: 'e1', source: 'node1', target: 'node2' },
        { id: 'e2', source: 'node1', target: 'node3' },
      ]
      
      const sorted = topologicalSort(nodes, edges)
      
      expect(sorted[0]).toBe('node1')
      expect(sorted).toContain('node2')
      expect(sorted).toContain('node3')
    })

    it('应处理独立节点', () => {
      const nodes: Node[] = [
        { id: 'node1', position: { x: 0, y: 0 }, data: {} },
        { id: 'node2', position: { x: 0, y: 0 }, data: {} },
      ]
      
      const edges: Edge[] = []
      
      const sorted = topologicalSort(nodes, edges)
      
      expect(sorted.length).toBe(2)
      expect(sorted).toContain('node1')
      expect(sorted).toContain('node2')
    })
  })
})
