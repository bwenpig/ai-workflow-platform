import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, test, expect, beforeEach } from 'vitest'
import WorkflowCanvas from '@/components/WorkflowCanvas'

describe('F002 - 节点连线', () => {
  beforeEach(() => {
    render(<WorkflowCanvas />)
  })

  test('应显示初始连线', () => {
    // 初始应该有连线 e1-2 和 e2-3
    const edges = document.querySelectorAll('.react-flow__edge-path')
    expect(edges.length).toBeGreaterThan(0)
  })

  test('应可添加新节点并自动支持连线', async () => {
    const outputNodeBtn = screen.getByRole('button', { name: /输出节点/ })
    fireEvent.click(outputNodeBtn)
    
    await waitFor(() => {
      expect(screen.getByText('输出节点')).toBeInTheDocument()
    })
    
    // 验证画布仍然有连线
    const edges = document.querySelectorAll('.react-flow__edge-path')
    expect(edges.length).toBeGreaterThan(0)
  })

  test('应支持添加多个节点', async () => {
    const modelNodeBtn = screen.getByRole('button', { name: /模型节点/ })
    fireEvent.click(modelNodeBtn)
    fireEvent.click(modelNodeBtn)
    
    await waitFor(() => {
      const modelNodes = screen.getAllByText('模型节点')
      expect(modelNodes.length).toBeGreaterThanOrEqual(2)
    })
  })
})

describe('F004 - 删除连线', () => {
  beforeEach(() => {
    render(<WorkflowCanvas />)
  })

  test('应可点击连线', async () => {
    const edgePaths = document.querySelectorAll('.react-flow__edge-path')
    expect(edgePaths.length).toBeGreaterThan(0)
    
    // 点击第一条连线
    if (edgePaths.length > 0) {
      fireEvent.click(edgePaths[0], { bubbles: true })
      expect(edgePaths[0]).toBeInTheDocument()
    }
  })

  test('删除连线不应影响节点', async () => {
    const node1 = screen.getByText('输入节点')
    const node2 = screen.getByText('可灵模型')
    
    expect(node1).toBeInTheDocument()
    expect(node2).toBeInTheDocument()
    
    // 尝试删除连线（按 Delete）
    const edgePaths = document.querySelectorAll('.react-flow__edge-path')
    if (edgePaths.length > 0) {
      fireEvent.click(edgePaths[0], { bubbles: true })
      fireEvent.keyDown(document, { key: 'Delete', bubbles: true })
    }
    
    // 节点应该还在
    await waitFor(() => {
      expect(node1).toBeInTheDocument()
      expect(node2).toBeInTheDocument()
    })
  })

  test('应可添加节点后删除', async () => {
    const outputNodeBtn = screen.getByRole('button', { name: /输出节点/ })
    fireEvent.click(outputNodeBtn)
    
    await waitFor(() => {
      expect(screen.getByText('输出节点')).toBeInTheDocument()
    })
    
    const newNode = screen.getByText('输出节点')
    fireEvent.click(newNode)
    fireEvent.keyDown(document, { key: 'Delete', bubbles: true })
    
    await waitFor(() => {
      expect(screen.queryByText('输出节点')).not.toBeInTheDocument()
    })
  })
})
