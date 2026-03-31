import { describe, test, expect, beforeEach } from 'vitest'
import { useViewportStore } from '../../store/viewportStore'

describe('viewportStore 测试', () => {
  beforeEach(() => {
    // 重置 store 状态
    useViewportStore.setState({
      zoom: 1,
      pan: { x: 0, y: 0 },
    })
  })

  describe('初始状态', () => {
    test('应有正确的初始缩放值', () => {
      const state = useViewportStore.getState()
      expect(state.zoom).toBe(1)
    })

    test('应有正确的初始平移值', () => {
      const state = useViewportStore.getState()
      expect(state.pan).toEqual({ x: 0, y: 0 })
    })
  })

  describe('setZoom', () => {
    test('应设置缩放值', () => {
      useViewportStore.getState().setZoom(1.5)

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(1.5)
    })

    test('应限制最小缩放值为 0.1', () => {
      useViewportStore.getState().setZoom(0.05)

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(0.1)
    })

    test('应限制最大缩放值为 2', () => {
      useViewportStore.getState().setZoom(3)

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(2)
    })

    test('应允许边界值 0.1', () => {
      useViewportStore.getState().setZoom(0.1)

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(0.1)
    })

    test('应允许边界值 2', () => {
      useViewportStore.getState().setZoom(2)

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(2)
    })

    test('应处理负数缩放值', () => {
      useViewportStore.getState().setZoom(-1)

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(0.1)
    })

    test('应支持多次缩放', () => {
      useViewportStore.getState().setZoom(1.2)
      useViewportStore.getState().setZoom(1.5)
      useViewportStore.getState().setZoom(0.8)

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(0.8)
    })
  })

  describe('setPan', () => {
    test('应设置平移值', () => {
      useViewportStore.getState().setPan({ x: 100, y: 200 })

      const state = useViewportStore.getState()
      expect(state.pan).toEqual({ x: 100, y: 200 })
    })

    test('应支持负数平移', () => {
      useViewportStore.getState().setPan({ x: -50, y: -100 })

      const state = useViewportStore.getState()
      expect(state.pan).toEqual({ x: -50, y: -100 })
    })

    test('应支持零平移', () => {
      useViewportStore.getState().setPan({ x: 0, y: 0 })

      const state = useViewportStore.getState()
      expect(state.pan).toEqual({ x: 0, y: 0 })
    })

    test('应支持大数值平移', () => {
      useViewportStore.getState().setPan({ x: 10000, y: 20000 })

      const state = useViewportStore.getState()
      expect(state.pan).toEqual({ x: 10000, y: 20000 })
    })

    test('应支持小数平移', () => {
      useViewportStore.getState().setPan({ x: 10.5, y: 20.7 })

      const state = useViewportStore.getState()
      expect(state.pan).toEqual({ x: 10.5, y: 20.7 })
    })
  })

  describe('updateViewport', () => {
    test('应同时更新缩放和平移', () => {
      useViewportStore.getState().updateViewport({
        zoom: 1.5,
        pan: { x: 100, y: 200 },
      })

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(1.5)
      expect(state.pan).toEqual({ x: 100, y: 200 })
    })

    test('应只更新缩放', () => {
      useViewportStore.getState().updateViewport({
        zoom: 1.2,
        pan: { x: 0, y: 0 },
      })

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(1.2)
      expect(state.pan).toEqual({ x: 0, y: 0 })
    })

    test('应只更新平移', () => {
      useViewportStore.getState().updateViewport({
        zoom: 1,
        pan: { x: 50, y: 100 },
      })

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(1)
      expect(state.pan).toEqual({ x: 50, y: 100 })
    })

    test('应支持大数值更新', () => {
      useViewportStore.getState().updateViewport({
        zoom: 1.8,
        pan: { x: 1000, y: 2000 },
      })

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(1.8)
      expect(state.pan).toEqual({ x: 1000, y: 2000 })
    })
  })

  describe('fitView', () => {
    test('应重置缩放为 1', () => {
      useViewportStore.getState().setZoom(1.5)
      useViewportStore.getState().fitView()

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(1)
    })

    test('应重置平移为 0', () => {
      useViewportStore.getState().setPan({ x: 100, y: 200 })
      useViewportStore.getState().fitView()

      const state = useViewportStore.getState()
      expect(state.pan).toEqual({ x: 0, y: 0 })
    })

    test('应同时重置缩放和平移', () => {
      useViewportStore.getState().setZoom(1.5)
      useViewportStore.getState().setPan({ x: 100, y: 200 })
      useViewportStore.getState().fitView()

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(1)
      expect(state.pan).toEqual({ x: 0, y: 0 })
    })
  })

  describe('resetViewport', () => {
    test('应重置缩放为 1', () => {
      useViewportStore.getState().setZoom(1.5)
      useViewportStore.getState().resetViewport()

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(1)
    })

    test('应重置平移为 0', () => {
      useViewportStore.getState().setPan({ x: 100, y: 200 })
      useViewportStore.getState().resetViewport()

      const state = useViewportStore.getState()
      expect(state.pan).toEqual({ x: 0, y: 0 })
    })

    test('应同时重置缩放和平移', () => {
      useViewportStore.getState().setZoom(1.5)
      useViewportStore.getState().setPan({ x: 100, y: 200 })
      useViewportStore.getState().resetViewport()

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(1)
      expect(state.pan).toEqual({ x: 0, y: 0 })
    })

    test('fitView 和 resetViewport 应有相同效果', () => {
      useViewportStore.getState().setZoom(1.5)
      useViewportStore.getState().setPan({ x: 100, y: 200 })
      useViewportStore.getState().fitView()

      const fitState = useViewportStore.getState()

      useViewportStore.getState().setZoom(1.5)
      useViewportStore.getState().setPan({ x: 100, y: 200 })
      useViewportStore.getState().resetViewport()

      const resetState = useViewportStore.getState()

      expect(fitState.zoom).toBe(resetState.zoom)
      expect(fitState.pan).toEqual(resetState.pan)
    })
  })

  describe('状态管理', () => {
    test('应保持状态响应式', () => {
      const { setZoom, setPan } = useViewportStore.getState()

      setZoom(1.2)
      expect(useViewportStore.getState().zoom).toBe(1.2)

      setPan({ x: 50, y: 100 })
      expect(useViewportStore.getState().pan).toEqual({ x: 50, y: 100 })
    })

    test('应支持多次连续更新', () => {
      const { updateViewport } = useViewportStore.getState()

      updateViewport({ zoom: 1.2, pan: { x: 10, y: 20 } })
      updateViewport({ zoom: 1.4, pan: { x: 30, y: 40 } })
      updateViewport({ zoom: 1.6, pan: { x: 50, y: 60 } })

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(1.6)
      expect(state.pan).toEqual({ x: 50, y: 60 })
    })
  })

  describe('边缘情况', () => {
    test('应处理极小的缩放值', () => {
      useViewportStore.getState().setZoom(0.001)

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(0.1)
    })

    test('应处理极大的缩放值', () => {
      useViewportStore.getState().setZoom(1000)

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(2)
    })

    test('应处理 Infinity 缩放值', () => {
      useViewportStore.getState().setZoom(Infinity)

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(2)
    })

    test('应处理-Infinity 缩放值', () => {
      useViewportStore.getState().setZoom(-Infinity)

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(0.1)
    })
  })

  describe('并发更新', () => {
    test('应正确处理快速连续的缩放操作', () => {
      const zoomValues = [0.5, 1.0, 1.5, 2.0, 0.1]
      zoomValues.forEach(value => {
        useViewportStore.getState().setZoom(value)
      })

      const state = useViewportStore.getState()
      expect(state.zoom).toBe(0.1)
    })

    test('应正确处理快速连续的平移操作', () => {
      const panValues = [
        { x: 10, y: 20 },
        { x: 30, y: 40 },
        { x: 50, y: 60 },
      ]
      panValues.forEach(value => {
        useViewportStore.getState().setPan(value)
      })

      const state = useViewportStore.getState()
      expect(state.pan).toEqual({ x: 50, y: 60 })
    })
  })
})
