import { describe, test, expect, beforeEach } from 'vitest'
import { useEdgeStore } from '../../store/edgeStore'
import { Edge } from '@xyflow/react'

describe('edgeStore 测试', () => {
  beforeEach(() => {
    // 重置 store 状态
    useEdgeStore.setState({
      edges: [],
      selectedEdgeIds: [],
    })
  })

  const createMockEdge = (id: string, source: string, target: string): Edge => ({
    id,
    source,
    target,
  })

  describe('addEdge', () => {
    test('应添加单个边', () => {
      const edge = createMockEdge('e1', 'node-1', 'node-2')
      useEdgeStore.getState().addEdge(edge)

      const state = useEdgeStore.getState()
      expect(state.edges).toHaveLength(1)
      expect(state.edges[0].id).toBe('e1')
    })

    test('应添加多个边', () => {
      const edge1 = createMockEdge('e1', 'node-1', 'node-2')
      const edge2 = createMockEdge('e2', 'node-2', 'node-3')
      const edge3 = createMockEdge('e3', 'node-3', 'node-4')

      useEdgeStore.getState().addEdge(edge1)
      useEdgeStore.getState().addEdge(edge2)
      useEdgeStore.getState().addEdge(edge3)

      const state = useEdgeStore.getState()
      expect(state.edges).toHaveLength(3)
    })

    test('应保持边的完整性', () => {
      const edge = createMockEdge('e1', 'node-1', 'node-2')
      useEdgeStore.getState().addEdge(edge)

      const state = useEdgeStore.getState()
      expect(state.edges[0].source).toBe('node-1')
      expect(state.edges[0].target).toBe('node-2')
    })

    test('应支持带标签的边', () => {
      const edge: Edge = {
        id: 'e1',
        source: 'node-1',
        target: 'node-2',
        label: '测试边',
      }
      useEdgeStore.getState().addEdge(edge)

      const state = useEdgeStore.getState()
      expect((state.edges[0] as any).label).toBe('测试边')
    })
  })

  describe('removeEdge', () => {
    test('应删除指定边', () => {
      const edge1 = createMockEdge('e1', 'node-1', 'node-2')
      const edge2 = createMockEdge('e2', 'node-2', 'node-3')

      useEdgeStore.getState().addEdge(edge1)
      useEdgeStore.getState().addEdge(edge2)

      useEdgeStore.getState().removeEdge('e1')

      const state = useEdgeStore.getState()
      expect(state.edges).toHaveLength(1)
      expect(state.edges[0].id).toBe('e2')
    })

    test('应同时从选中列表中移除', () => {
      const edge1 = createMockEdge('e1', 'node-1', 'node-2')
      const edge2 = createMockEdge('e2', 'node-2', 'node-3')

      useEdgeStore.getState().addEdge(edge1)
      useEdgeStore.getState().addEdge(edge2)
      useEdgeStore.getState().selectEdge('e1')
      useEdgeStore.getState().selectEdge('e2', true)

      useEdgeStore.getState().removeEdge('e1')

      const state = useEdgeStore.getState()
      expect(state.selectedEdgeIds).not.toContain('e1')
      expect(state.selectedEdgeIds).toContain('e2')
    })

    test('删除不存在的边不应报错', () => {
      const edge = createMockEdge('e1', 'node-1', 'node-2')
      useEdgeStore.getState().addEdge(edge)

      expect(() => {
        useEdgeStore.getState().removeEdge('non-existent')
      }).not.toThrow()
    })
  })

  describe('边操作', () => {
    test('应通过 removeEdgesForNode 删除与节点相关的边', () => {
      const edge1 = createMockEdge('e1', 'node-1', 'node-2')
      const edge2 = createMockEdge('e2', 'node-2', 'node-3')
      const edge3 = createMockEdge('e3', 'node-3', 'node-4')

      useEdgeStore.getState().addEdge(edge1)
      useEdgeStore.getState().addEdge(edge2)
      useEdgeStore.getState().addEdge(edge3)

      useEdgeStore.getState().removeEdgesForNode('node-2')

      const state = useEdgeStore.getState()
      expect(state.edges).toHaveLength(1)
      expect(state.edges[0].id).toBe('e3')
    })
  })

  describe('selectEdge', () => {
    test('应选择单个边', () => {
      const edge = createMockEdge('e1', 'node-1', 'node-2')
      useEdgeStore.getState().addEdge(edge)

      useEdgeStore.getState().selectEdge('e1')

      const state = useEdgeStore.getState()
      expect(state.selectedEdgeIds).toEqual(['e1'])
    })

    test('应替换之前的选择（非多选）', () => {
      const edge1 = createMockEdge('e1', 'node-1', 'node-2')
      const edge2 = createMockEdge('e2', 'node-2', 'node-3')

      useEdgeStore.getState().addEdge(edge1)
      useEdgeStore.getState().addEdge(edge2)

      useEdgeStore.getState().selectEdge('e1')
      useEdgeStore.getState().selectEdge('e2')

      const state = useEdgeStore.getState()
      expect(state.selectedEdgeIds).toEqual(['e2'])
    })

    test('应支持多选', () => {
      const edge1 = createMockEdge('e1', 'node-1', 'node-2')
      const edge2 = createMockEdge('e2', 'node-2', 'node-3')
      const edge3 = createMockEdge('e3', 'node-3', 'node-4')

      useEdgeStore.getState().addEdge(edge1)
      useEdgeStore.getState().addEdge(edge2)
      useEdgeStore.getState().addEdge(edge3)

      useEdgeStore.getState().selectEdge('e1')
      useEdgeStore.getState().selectEdge('e2', true)
      useEdgeStore.getState().selectEdge('e3', true)

      const state = useEdgeStore.getState()
      expect(state.selectedEdgeIds).toEqual(['e1', 'e2', 'e3'])
    })
  })

  describe('deselectEdge', () => {
    test('应取消选择指定边', () => {
      const edge1 = createMockEdge('e1', 'node-1', 'node-2')
      const edge2 = createMockEdge('e2', 'node-2', 'node-3')

      useEdgeStore.getState().addEdge(edge1)
      useEdgeStore.getState().addEdge(edge2)

      useEdgeStore.getState().selectEdge('e1')
      useEdgeStore.getState().selectEdge('e2', true)

      useEdgeStore.getState().deselectEdge('e1')

      const state = useEdgeStore.getState()
      expect(state.selectedEdgeIds).toEqual(['e2'])
    })

    test('取消选择不存在的边不应报错', () => {
      useEdgeStore.getState().selectEdge('e1')

      expect(() => {
        useEdgeStore.getState().deselectEdge('non-existent')
      }).not.toThrow()
    })

    test('取消选择后列表应为空', () => {
      useEdgeStore.getState().selectEdge('e1')
      useEdgeStore.getState().deselectEdge('e1')

      const state = useEdgeStore.getState()
      expect(state.selectedEdgeIds).toEqual([])
    })
  })

  describe('clearSelection', () => {
    test('应清空所有选中', () => {
      const edge1 = createMockEdge('e1', 'node-1', 'node-2')
      const edge2 = createMockEdge('e2', 'node-2', 'node-3')
      const edge3 = createMockEdge('e3', 'node-3', 'node-4')

      useEdgeStore.getState().addEdge(edge1)
      useEdgeStore.getState().addEdge(edge2)
      useEdgeStore.getState().addEdge(edge3)

      useEdgeStore.getState().selectEdge('e1')
      useEdgeStore.getState().selectEdge('e2', true)
      useEdgeStore.getState().selectEdge('e3', true)

      useEdgeStore.getState().clearSelection()

      const state = useEdgeStore.getState()
      expect(state.selectedEdgeIds).toEqual([])
    })

    test('清空选择不应影响边列表', () => {
      const edge1 = createMockEdge('e1', 'node-1', 'node-2')
      const edge2 = createMockEdge('e2', 'node-2', 'node-3')

      useEdgeStore.getState().addEdge(edge1)
      useEdgeStore.getState().addEdge(edge2)
      useEdgeStore.getState().selectEdge('e1')

      useEdgeStore.getState().clearSelection()

      const state = useEdgeStore.getState()
      expect(state.edges).toHaveLength(2)
      expect(state.selectedEdgeIds).toEqual([])
    })
  })

  describe('removeSelectedEdges', () => {
    test('应删除所有选中的边', () => {
      const edge1 = createMockEdge('e1', 'node-1', 'node-2')
      const edge2 = createMockEdge('e2', 'node-2', 'node-3')
      const edge3 = createMockEdge('e3', 'node-3', 'node-4')

      useEdgeStore.getState().addEdge(edge1)
      useEdgeStore.getState().addEdge(edge2)
      useEdgeStore.getState().addEdge(edge3)

      useEdgeStore.getState().selectEdge('e1')
      useEdgeStore.getState().selectEdge('e3', true)

      useEdgeStore.getState().removeSelectedEdges()

      const state = useEdgeStore.getState()
      expect(state.edges).toHaveLength(1)
      expect(state.edges[0].id).toBe('e2')
    })

    test('应清空选中列表', () => {
      const edge1 = createMockEdge('e1', 'node-1', 'node-2')
      const edge2 = createMockEdge('e2', 'node-2', 'node-3')

      useEdgeStore.getState().addEdge(edge1)
      useEdgeStore.getState().addEdge(edge2)

      useEdgeStore.getState().selectEdge('e1')
      useEdgeStore.getState().selectEdge('e2', true)

      useEdgeStore.getState().removeSelectedEdges()

      const state = useEdgeStore.getState()
      expect(state.selectedEdgeIds).toEqual([])
    })

    test('没有选中边时不应删除任何边', () => {
      const edge1 = createMockEdge('e1', 'node-1', 'node-2')
      const edge2 = createMockEdge('e2', 'node-2', 'node-3')

      useEdgeStore.getState().addEdge(edge1)
      useEdgeStore.getState().addEdge(edge2)

      useEdgeStore.getState().removeSelectedEdges()

      const state = useEdgeStore.getState()
      expect(state.edges).toHaveLength(2)
    })
  })

  describe('边缘情况', () => {
    test('应处理空边 ID', () => {
      const edge = createMockEdge('', 'node-1', 'node-2')
      useEdgeStore.getState().addEdge(edge)

      const state = useEdgeStore.getState()
      expect(state.edges).toHaveLength(1)
      expect(state.edges[0].id).toBe('')
    })

    test('应处理自环边', () => {
      const edge = createMockEdge('e1', 'node-1', 'node-1')
      useEdgeStore.getState().addEdge(edge)

      const state = useEdgeStore.getState()
      expect(state.edges).toHaveLength(1)
      expect(state.edges[0].source).toBe('node-1')
      expect(state.edges[0].target).toBe('node-1')
    })

    test('应处理大量边', () => {
      const edges = Array.from({ length: 100 }, (_, i) =>
        createMockEdge(`e${i}`, `node-${i}`, `node-${i + 1}`)
      )

      edges.forEach(edge => useEdgeStore.getState().addEdge(edge))

      const state = useEdgeStore.getState()
      expect(state.edges).toHaveLength(100)
    })

    test('应处理重复的边', () => {
      const edge1 = createMockEdge('e1', 'node-1', 'node-2')
      const edge2 = createMockEdge('e1', 'node-1', 'node-2')

      useEdgeStore.getState().addEdge(edge1)
      useEdgeStore.getState().addEdge(edge2)

      const state = useEdgeStore.getState()
      expect(state.edges).toHaveLength(2)
    })
  })

  describe('状态管理', () => {
    test('应有正确的初始状态', () => {
      useEdgeStore.setState({
        edges: [],
        selectedEdgeIds: [],
      })

      const state = useEdgeStore.getState()
      expect(state.edges).toEqual([])
      expect(state.selectedEdgeIds).toEqual([])
    })

    test('应保持状态隔离', () => {
      const edge = createMockEdge('e1', 'node-1', 'node-2')
      useEdgeStore.getState().addEdge(edge)

      const state1 = useEdgeStore.getState()
      expect(state1.edges).toHaveLength(1)

      // 重置状态
      useEdgeStore.setState({
        edges: [],
        selectedEdgeIds: [],
      })

      const state2 = useEdgeStore.getState()
      expect(state2.edges).toEqual([])
    })
  })
})
