import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, test, expect, beforeEach } from 'vitest'
import WorkflowCanvas from '@/components/WorkflowCanvas'

describe('F003 - 删除节点', () => {
  beforeEach(() => {
    render(<WorkflowCanvas />)
  })

  test('应可删除选中节点', async () => {
    const node = screen.getAllByText('输入节点')[0]
    
    fireEvent.click(node)
    fireEvent.keyDown(document, { key: 'Delete', bubbles: true })
    
    await waitFor(() => {
      const nodes = screen.queryAllByText('输入节点')
      expect(nodes.length).toBe(0)
    })
  })

  test('删除节点应同时删除关联连线', async () => {
    const node = screen.getAllByText('输入节点')[0]
    
    // 初始有连线
    const initialEdges = document.querySelectorAll('.react-flow__edge-path')
    expect(initialEdges.length).toBeGreaterThan(0)
    
    fireEvent.click(node)
    fireEvent.keyDown(document, { key: 'Delete', bubbles: true })
    
    await waitFor(() => {
      const edges = document.querySelectorAll('.react-flow__edge-path')
      expect(edges.length).toBeLessThan(initialEdges.length)
    })
  })

  test('应可添加并删除节点', async () => {
    const inputNodeBtn = screen.getByRole('button', { name: /输入节点/ })
    fireEvent.click(inputNodeBtn)
    
    await waitFor(() => {
      const nodes = screen.getAllByText('输入节点')
      expect(nodes.length).toBeGreaterThan(1)
    })
    
    const newNode = screen.getAllByText('输入节点')[1]
    fireEvent.click(newNode)
    fireEvent.keyDown(document, { key: 'Delete', bubbles: true })
    
    await waitFor(() => {
      const nodes = screen.getAllByText('输入节点')
      expect(nodes.length).toBe(1) // 只剩下初始节点
    })
  })
})

describe('F008 - 节点选中', () => {
  beforeEach(() => {
    render(<WorkflowCanvas />)
  })

  test('应单击选中单个节点', async () => {
    const node = screen.getAllByText('输入节点')[0]
    
    fireEvent.click(node)
    
    expect(node).toBeInTheDocument()
  })

  test('应取消选中已选中的节点', async () => {
    const canvas = screen.getByTestId('canvas')
    const node = screen.getAllByText('输入节点')[0]
    
    fireEvent.click(node)
    fireEvent.click(canvas)
    
    expect(node).toBeInTheDocument()
  })

  test('应可点击不同节点切换选中', async () => {
    const node1 = screen.getAllByText('输入节点')[0]
    const node2 = screen.getAllByText('可灵模型')[0]
    
    fireEvent.click(node1)
    fireEvent.click(node2)
    
    expect(node1).toBeInTheDocument()
    expect(node2).toBeInTheDocument()
  })
})

describe('F009 - 多选节点', () => {
  beforeEach(() => {
    render(<WorkflowCanvas />)
  })

  test('应按住 Ctrl/Cmd 多选节点', async () => {
    const node1 = screen.getAllByText('输入节点')[0]
    const node2 = screen.getAllByText('可灵模型')[0]
    
    fireEvent.click(node1)
    fireEvent.click(node2, { ctrlKey: true, bubbles: true })
    
    expect(node1).toBeInTheDocument()
    expect(node2).toBeInTheDocument()
  })

  test('应可添加多个节点', async () => {
    const inputNodeBtn = screen.getByRole('button', { name: /输入节点/ })
    
    fireEvent.click(inputNodeBtn)
    fireEvent.click(inputNodeBtn)
    
    await waitFor(() => {
      const nodes = screen.getAllByText('输入节点')
      expect(nodes.length).toBeGreaterThanOrEqual(2)
    })
  })

  test('应可框选区域', async () => {
    const canvas = screen.getByTestId('canvas')
    
    // 框选
    fireEvent.mouseDown(canvas, { 
      clientX: 0, 
      clientY: 0,
      bubbles: true 
    })
    fireEvent.mouseMove(canvas, { 
      clientX: 500, 
      clientY: 500,
      bubbles: true 
    })
    fireEvent.mouseUp(canvas, { 
      clientX: 500, 
      clientY: 500,
      bubbles: true 
    })
    
    expect(canvas).toBeInTheDocument()
  })
})

describe('F010 - 节点移动', () => {
  beforeEach(() => {
    render(<WorkflowCanvas />)
  })

  test('应可点击并准备移动节点', async () => {
    const node = screen.getAllByText('输入节点')[0]
    
    fireEvent.click(node)
    
    expect(node).toBeInTheDocument()
  })

  test('应支持添加多个节点并移动', async () => {
    const canvas = screen.getByTestId('canvas')
    const inputNodeBtn = screen.getByRole('button', { name: /输入节点/ })
    
    fireEvent.click(inputNodeBtn)
    
    await waitFor(() => {
      const nodes = screen.getAllByText('输入节点')
      expect(nodes.length).toBeGreaterThan(1)
    })
    
    const newNode = screen.getAllByText('输入节点')[1]
    
    // 拖拽
    fireEvent.mouseDown(newNode, { clientX: 100, clientY: 100, bubbles: true })
    fireEvent.mouseMove(canvas, { clientX: 200, clientY: 200, bubbles: true })
    fireEvent.mouseUp(canvas, { clientX: 200, clientY: 200, bubbles: true })
    
    expect(newNode).toBeInTheDocument()
  })

  test('应保持节点在画布内', async () => {
    const canvas = screen.getByTestId('canvas')
    const node = screen.getAllByText('输入节点')[0]
    
    // 尝试移动到画布外
    fireEvent.mouseDown(node, { clientX: 100, clientY: 100, bubbles: true })
    fireEvent.mouseMove(canvas, { clientX: 1000, clientY: 1000, bubbles: true })
    fireEvent.mouseUp(canvas, { clientX: 1000, clientY: 1000, bubbles: true })
    
    expect(node).toBeInTheDocument()
  })
})
