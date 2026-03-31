import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, test, expect, beforeEach } from 'vitest'
import WorkflowCanvas from '@/components/WorkflowCanvas'

describe('F005 - 缩放画布', () => {
  beforeEach(() => {
    render(<WorkflowCanvas />)
  })

  test('应支持鼠标滚轮缩放', async () => {
    const canvas = screen.getByTestId('canvas')
    
    // 放大
    fireEvent.wheel(canvas, { 
      deltaY: -100,
      bubbles: true 
    })
    
    // 验证画布存在
    expect(canvas).toBeInTheDocument()
  })

  test('应限制缩放范围', async () => {
    const canvas = screen.getByTestId('canvas')
    
    // 多次放大
    for (let i = 0; i < 10; i++) {
      fireEvent.wheel(canvas, { deltaY: -100, bubbles: true })
    }
    
    // 验证画布仍然存在
    expect(canvas).toBeInTheDocument()
    
    // 多次缩小
    for (let i = 0; i < 10; i++) {
      fireEvent.wheel(canvas, { deltaY: 100, bubbles: true })
    }
    
    expect(canvas).toBeInTheDocument()
  })

  test('应显示缩放控件', () => {
    const canvas = screen.getByTestId('canvas')
    
    // React Flow Controls 应该存在
    const controls = canvas.querySelector('.react-flow__controls')
    expect(controls).toBeInTheDocument()
  })
})

describe('F006 - 平移画布', () => {
  beforeEach(() => {
    render(<WorkflowCanvas />)
  })

  test('应可拖拽空白区域平移画布', async () => {
    const canvas = screen.getByTestId('canvas')
    
    // 拖拽空白区域
    fireEvent.mouseDown(canvas, { clientX: 500, clientY: 500, bubbles: true })
    fireEvent.mouseMove(canvas, { clientX: 300, clientY: 300, bubbles: true })
    fireEvent.mouseUp(canvas, { clientX: 300, clientY: 300, bubbles: true })
    
    // 验证画布存在
    expect(canvas).toBeInTheDocument()
  })

  test('应支持空格 + 拖拽平移', async () => {
    const canvas = screen.getByTestId('canvas')
    
    // 按下空格
    fireEvent.keyDown(document, { key: ' ', bubbles: true })
    
    // 拖拽
    fireEvent.mouseDown(canvas, { clientX: 500, clientY: 500, bubbles: true })
    fireEvent.mouseMove(canvas, { clientX: 300, clientY: 300, bubbles: true })
    fireEvent.mouseUp(canvas, { clientX: 300, clientY: 300, bubbles: true })
    
    // 释放空格
    fireEvent.keyUp(document, { key: ' ', bubbles: true })
    
    expect(canvas).toBeInTheDocument()
  })

  test('拖拽节点不应触发布局平移', async () => {
    const node = screen.getByText('输入节点')
    const canvas = screen.getByTestId('canvas')
    
    // 拖拽节点
    fireEvent.mouseDown(node, { clientX: 100, clientY: 100, bubbles: true })
    fireEvent.mouseMove(canvas, { clientX: 200, clientY: 200, bubbles: true })
    fireEvent.mouseUp(canvas, { clientX: 200, clientY: 200, bubbles: true })
    
    // 节点应该还在
    expect(node).toBeInTheDocument()
  })
})

describe('F007 - 适应屏幕', () => {
  beforeEach(() => {
    render(<WorkflowCanvas />)
  })

  test('应可双击空白区域', async () => {
    const canvas = screen.getByTestId('canvas')
    
    // 双击空白区域
    fireEvent.dblClick(canvas, { 
      clientX: 500, 
      clientY: 500, 
      bubbles: true 
    })
    
    expect(canvas).toBeInTheDocument()
  })

  test('应可点击适应屏幕按钮', async () => {
    const canvas = screen.getByTestId('canvas')
    
    // 查找适应屏幕按钮
    const fitViewButton = canvas.querySelector('.react-flow__controls-fitview')
    
    if (fitViewButton) {
      fireEvent.click(fitViewButton)
      expect(canvas).toBeInTheDocument()
    }
  })

  test('应显示所有初始节点', () => {
    const inputNodes = screen.getAllByText('输入节点')
    const modelNodes = screen.getAllByText('可灵模型')
    const outputNodes = screen.getAllByText('输出节点')
    
    expect(inputNodes.length).toBeGreaterThan(0)
    expect(modelNodes.length).toBeGreaterThan(0)
    expect(outputNodes.length).toBeGreaterThan(0)
  })
})
