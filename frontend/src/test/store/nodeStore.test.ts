import { describe, test, expect, beforeEach } from 'vitest'
import { useNodeStore } from '../../store/nodeStore'
import { Node } from '@xyflow/react'

describe('nodeStore 测试', () => {
  beforeEach(() => {
    // 重置 store 状态
    useNodeStore.setState({
      nodes: [],
      selectedNodeIds: [],
    })
  })

  const createMockNode = (id: string, position = { x: 0, y: 0 }): Node => ({
    id,
    position,
    data: { label: `Node ${id}` },
  })

  describe('addNode', () => {
    test('应添加单个节点', () => {
      const node = createMockNode('node-1')
      useNodeStore.getState().addNode(node)

      const state = useNodeStore.getState()
      expect(state.nodes).toHaveLength(1)
      expect(state.nodes[0].id).toBe('node-1')
    })

    test('应添加多个节点', () => {
      const node1 = createMockNode('node-1')
      const node2 = createMockNode('node-2')
      const node3 = createMockNode('node-3')

      useNodeStore.getState().addNode(node1)
      useNodeStore.getState().addNode(node2)
      useNodeStore.getState().addNode(node3)

      const state = useNodeStore.getState()
      expect(state.nodes).toHaveLength(3)
    })

    test('应保持节点的完整性', () => {
      const node = createMockNode('node-1', { x: 100, y: 200 })
      useNodeStore.getState().addNode(node)

      const state = useNodeStore.getState()
      expect(state.nodes[0].position).toEqual({ x: 100, y: 200 })
      expect(state.nodes[0].data.label).toBe('Node node-1')
    })
  })

  describe('removeNode', () => {
    test('应删除指定节点', () => {
      const node1 = createMockNode('node-1')
      const node2 = createMockNode('node-2')

      useNodeStore.getState().addNode(node1)
      useNodeStore.getState().addNode(node2)

      useNodeStore.getState().removeNode('node-1')

      const state = useNodeStore.getState()
      expect(state.nodes).toHaveLength(1)
      expect(state.nodes[0].id).toBe('node-2')
    })

    test('应同时从选中列表中移除', () => {
      const node1 = createMockNode('node-1')
      const node2 = createMockNode('node-2')

      useNodeStore.getState().addNode(node1)
      useNodeStore.getState().addNode(node2)
      useNodeStore.getState().selectNode('node-1')
      useNodeStore.getState().selectNode('node-2', true)

      useNodeStore.getState().removeNode('node-1')

      const state = useNodeStore.getState()
      expect(state.selectedNodeIds).not.toContain('node-1')
      expect(state.selectedNodeIds).toContain('node-2')
    })

    test('删除不存在的节点不应报错', () => {
      const node = createMockNode('node-1')
      useNodeStore.getState().addNode(node)

      expect(() => {
        useNodeStore.getState().removeNode('non-existent')
      }).not.toThrow()
    })
  })

  describe('updateNode', () => {
    test('应更新节点位置', () => {
      const node = createMockNode('node-1', { x: 0, y: 0 })
      useNodeStore.getState().addNode(node)

      useNodeStore.getState().updateNode('node-1', { position: { x: 100, y: 200 } })

      const state = useNodeStore.getState()
      expect(state.nodes[0].position).toEqual({ x: 100, y: 200 })
    })

    test('应更新节点数据', () => {
      const node = createMockNode('node-1')
      useNodeStore.getState().addNode(node)

      useNodeStore.getState().updateNode('node-1', {
        data: { label: 'Updated Label', custom: 'value' },
      })

      const state = useNodeStore.getState()
      expect(state.nodes[0].data.label).toBe('Updated Label')
      expect((state.nodes[0].data as any).custom).toBe('value')
    })

    test('应支持部分更新', () => {
      const node = createMockNode('node-1', { x: 0, y: 0 })
      useNodeStore.getState().addNode(node)

      useNodeStore.getState().updateNode('node-1', {
        data: { label: 'New Label' },
      })

      const state = useNodeStore.getState()
      expect(state.nodes[0].position).toEqual({ x: 0, y: 0 }) // 位置不变
      expect(state.nodes[0].data.label).toBe('New Label')
    })

    test('更新不存在的节点不应影响其他节点', () => {
      const node = createMockNode('node-1')
      useNodeStore.getState().addNode(node)

      useNodeStore.getState().updateNode('non-existent', { position: { x: 100, y: 100 } })

      const state = useNodeStore.getState()
      expect(state.nodes).toHaveLength(1)
      expect(state.nodes[0].position).toEqual({ x: 0, y: 0 })
    })
  })

  describe('selectNode', () => {
    test('应选择单个节点', () => {
      const node = createMockNode('node-1')
      useNodeStore.getState().addNode(node)

      useNodeStore.getState().selectNode('node-1')

      const state = useNodeStore.getState()
      expect(state.selectedNodeIds).toEqual(['node-1'])
    })

    test('应替换之前的选择（非多选）', () => {
      const node1 = createMockNode('node-1')
      const node2 = createMockNode('node-2')

      useNodeStore.getState().addNode(node1)
      useNodeStore.getState().addNode(node2)

      useNodeStore.getState().selectNode('node-1')
      useNodeStore.getState().selectNode('node-2')

      const state = useNodeStore.getState()
      expect(state.selectedNodeIds).toEqual(['node-2'])
    })

    test('应支持多选', () => {
      const node1 = createMockNode('node-1')
      const node2 = createMockNode('node-2')
      const node3 = createMockNode('node-3')

      useNodeStore.getState().addNode(node1)
      useNodeStore.getState().addNode(node2)
      useNodeStore.getState().addNode(node3)

      useNodeStore.getState().selectNode('node-1')
      useNodeStore.getState().selectNode('node-2', true)
      useNodeStore.getState().selectNode('node-3', true)

      const state = useNodeStore.getState()
      expect(state.selectedNodeIds).toEqual(['node-1', 'node-2', 'node-3'])
    })

    test('应支持重复选择同一节点（多选模式）', () => {
      const node = createMockNode('node-1')
      useNodeStore.getState().addNode(node)

      useNodeStore.getState().selectNode('node-1')
      useNodeStore.getState().selectNode('node-1', true)

      const state = useNodeStore.getState()
      expect(state.selectedNodeIds).toEqual(['node-1', 'node-1'])
    })
  })

  describe('deselectNode', () => {
    test('应取消选择指定节点', () => {
      const node1 = createMockNode('node-1')
      const node2 = createMockNode('node-2')

      useNodeStore.getState().addNode(node1)
      useNodeStore.getState().addNode(node2)

      useNodeStore.getState().selectNode('node-1')
      useNodeStore.getState().selectNode('node-2', true)

      useNodeStore.getState().deselectNode('node-1')

      const state = useNodeStore.getState()
      expect(state.selectedNodeIds).toEqual(['node-2'])
    })

    test('取消选择不存在的节点不应报错', () => {
      useNodeStore.getState().selectNode('node-1')

      expect(() => {
        useNodeStore.getState().deselectNode('non-existent')
      }).not.toThrow()
    })

    test('取消选择后列表应为空', () => {
      useNodeStore.getState().selectNode('node-1')
      useNodeStore.getState().deselectNode('node-1')

      const state = useNodeStore.getState()
      expect(state.selectedNodeIds).toEqual([])
    })
  })

  describe('clearSelection', () => {
    test('应清空所有选中', () => {
      const node1 = createMockNode('node-1')
      const node2 = createMockNode('node-2')
      const node3 = createMockNode('node-3')

      useNodeStore.getState().addNode(node1)
      useNodeStore.getState().addNode(node2)
      useNodeStore.getState().addNode(node3)

      useNodeStore.getState().selectNode('node-1')
      useNodeStore.getState().selectNode('node-2', true)
      useNodeStore.getState().selectNode('node-3', true)

      useNodeStore.getState().clearSelection()

      const state = useNodeStore.getState()
      expect(state.selectedNodeIds).toEqual([])
    })

    test('清空选择不应影响节点列表', () => {
      const node1 = createMockNode('node-1')
      const node2 = createMockNode('node-2')

      useNodeStore.getState().addNode(node1)
      useNodeStore.getState().addNode(node2)
      useNodeStore.getState().selectNode('node-1')

      useNodeStore.getState().clearSelection()

      const state = useNodeStore.getState()
      expect(state.nodes).toHaveLength(2)
      expect(state.selectedNodeIds).toEqual([])
    })
  })

  describe('removeSelectedNodes', () => {
    test('应删除所有选中的节点', () => {
      const node1 = createMockNode('node-1')
      const node2 = createMockNode('node-2')
      const node3 = createMockNode('node-3')

      useNodeStore.getState().addNode(node1)
      useNodeStore.getState().addNode(node2)
      useNodeStore.getState().addNode(node3)

      useNodeStore.getState().selectNode('node-1')
      useNodeStore.getState().selectNode('node-3', true)

      useNodeStore.getState().removeSelectedNodes()

      const state = useNodeStore.getState()
      expect(state.nodes).toHaveLength(1)
      expect(state.nodes[0].id).toBe('node-2')
    })

    test('应清空选中列表', () => {
      const node1 = createMockNode('node-1')
      const node2 = createMockNode('node-2')

      useNodeStore.getState().addNode(node1)
      useNodeStore.getState().addNode(node2)

      useNodeStore.getState().selectNode('node-1')
      useNodeStore.getState().selectNode('node-2', true)

      useNodeStore.getState().removeSelectedNodes()

      const state = useNodeStore.getState()
      expect(state.selectedNodeIds).toEqual([])
    })

    test('没有选中节点时不应删除任何节点', () => {
      const node1 = createMockNode('node-1')
      const node2 = createMockNode('node-2')

      useNodeStore.getState().addNode(node1)
      useNodeStore.getState().addNode(node2)

      useNodeStore.getState().removeSelectedNodes()

      const state = useNodeStore.getState()
      expect(state.nodes).toHaveLength(2)
    })
  })

  describe('状态管理', () => {
    test('应有正确的初始状态', () => {
      useNodeStore.setState({
        nodes: [],
        selectedNodeIds: [],
      })

      const state = useNodeStore.getState()
      expect(state.nodes).toEqual([])
      expect(state.selectedNodeIds).toEqual([])
    })

    test('应保持状态隔离', () => {
      const node1 = createMockNode('node-1')
      useNodeStore.getState().addNode(node1)

      const state1 = useNodeStore.getState()
      expect(state1.nodes).toHaveLength(1)

      // 创建新状态
      useNodeStore.setState({
        nodes: [],
        selectedNodeIds: [],
      })

      const state2 = useNodeStore.getState()
      expect(state2.nodes).toEqual([])
    })
  })

  describe('边缘情况', () => {
    test('应处理空节点 ID', () => {
      const node = createMockNode('')
      useNodeStore.getState().addNode(node)

      const state = useNodeStore.getState()
      expect(state.nodes).toHaveLength(1)
      expect(state.nodes[0].id).toBe('')
    })

    test('应处理特殊字符的节点 ID', () => {
      const node = createMockNode('node-with-special-chars-!@#$%')
      useNodeStore.getState().addNode(node)

      useNodeStore.getState().selectNode('node-with-special-chars-!@#$%')

      const state = useNodeStore.getState()
      expect(state.selectedNodeIds).toContain('node-with-special-chars-!@#$%')
    })

    test('应处理大量节点', () => {
      const nodes = Array.from({ length: 100 }, (_, i) => createMockNode(`node-${i}`))

      nodes.forEach(node => useNodeStore.getState().addNode(node))

      const state = useNodeStore.getState()
      expect(state.nodes).toHaveLength(100)
    })
  })
})
