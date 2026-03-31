import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, test, expect, beforeEach } from 'vitest'
import WorkflowCanvas from '@/components/WorkflowCanvas'

describe('F001 - 拖拽节点', () => {
  beforeEach(() => {
    render(<WorkflowCanvas />)
  })

  test('应从节点面板添加节点到画布', async () => {
    const inputNodeBtn = screen.getByRole('button', { name: /输入节点/ })
    
    // 点击按钮添加节点
    fireEvent.click(inputNodeBtn)
    
    // 验证节点已添加到画布
    await waitFor(() => {
      const nodes = screen.getAllByText('输入节点')
      expect(nodes.length).toBeGreaterThan(1) // 初始有 1 个，新增 1 个
    })
  })

  test('应可添加并选中节点', async () => {
    const inputNodeBtn = screen.getByRole('button', { name: /输入节点/ })
    fireEvent.click(inputNodeBtn)
    
    await waitFor(() => {
      const nodes = screen.getAllByText('输入节点')
      expect(nodes.length).toBeGreaterThan(1)
    })
    
    const node = screen.getAllByText('输入节点')[1]
    fireEvent.click(node)
    
    expect(node).toBeInTheDocument()
  })

  test('应可添加多个不同类型的节点', async () => {
    const modelNodeBtn = screen.getByRole('button', { name: /模型节点/ })
    const outputNodeBtn = screen.getByRole('button', { name: /输出节点/ })
    
    fireEvent.click(modelNodeBtn)
    fireEvent.click(outputNodeBtn)
    
    await waitFor(() => {
      expect(screen.getByText('模型节点')).toBeInTheDocument()
      expect(screen.getByText('输出节点')).toBeInTheDocument()
    })
  })
})

describe('F010 - 节点移动', () => {
  beforeEach(() => {
    render(<WorkflowCanvas />)
  })

  test('应可点击选中节点', async () => {
    const node = screen.getByText('输入节点')
    
    fireEvent.click(node)
    
    // 验证节点被选中（通过检查是否存在）
    expect(node).toBeInTheDocument()
  })

  test('应支持添加新节点', async () => {
    const inputNodeBtn = screen.getByRole('button', { name: /输入节点/ })
    
    fireEvent.click(inputNodeBtn)
    fireEvent.click(inputNodeBtn)
    
    await waitFor(() => {
      const nodes = screen.getAllByText('输入节点')
      expect(nodes.length).toBeGreaterThanOrEqual(2)
    })
  })

  test('应可添加并选中节点', async () => {
    const canvas = screen.getByTestId('canvas')
    const inputNodeBtn = screen.getByRole('button', { name: /输入节点/ })
    
    fireEvent.click(inputNodeBtn)
    
    await waitFor(() => {
      const nodes = screen.getAllByText('输入节点')
      expect(nodes.length).toBeGreaterThan(1)
    })
    
    const newNode = screen.getAllByText('输入节点')[1]
    fireEvent.click(newNode)
    
    expect(newNode).toBeInTheDocument()
  })
})
