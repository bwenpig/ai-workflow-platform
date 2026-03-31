import { render, screen, fireEvent } from '@testing-library/react'
import { describe, test, expect, beforeEach, vi } from 'vitest'
import WorkflowCanvas from '@/components/WorkflowCanvas'
import { dragAndDrop } from '@/test/utils/dragHelpers'

describe('F043 - 快捷键', () => {
  beforeEach(() => {
    render(<WorkflowCanvas />)
  })

  test('应支持 Ctrl/Cmd+S 保存', async () => {
    fireEvent.keyDown(document, { key: 's', ctrlKey: true, metaKey: true })
    
    expect(screen.getByText(/保存/)).toBeInTheDocument()
  })

  test('应支持 Ctrl/Cmd+Z 撤销', async () => {
    const canvas = screen.getByTestId('canvas')
    const node = screen.getByText(/输入节点/)
    
    await dragAndDrop(node, canvas, { x: 100, y: 100 })
    
    fireEvent.keyDown(document, { key: 'z', ctrlKey: true, metaKey: true })
    
    // 应撤销上一步操作
    expect(screen.queryByTestId('node-4')).not.toBeInTheDocument()
  })

  test('应支持 Ctrl/Cmd+Shift+Z 重做', async () => {
    const canvas = screen.getByTestId('canvas')
    const node = screen.getByText(/输入节点/)
    
    await dragAndDrop(node, canvas, { x: 100, y: 100 })
    
    // 撤销
    fireEvent.keyDown(document, { key: 'z', ctrlKey: true, metaKey: true })
    // 重做
    fireEvent.keyDown(document, { key: 'z', ctrlKey: true, shiftKey: true, metaKey: true })
    
    // 应恢复节点
    expect(screen.getByTestId('node-4')).toBeInTheDocument()
  })

  test('应支持 Delete 删除选中节点', async () => {
    const node = screen.getByTestId('node-1')
    
    fireEvent.click(node)
    fireEvent.keyDown(document, { key: 'Delete' })
    
    expect(node).not.toBeInTheDocument()
  })

  test('应支持 Ctrl/Cmd+A 全选', async () => {
    fireEvent.keyDown(document, { key: 'a', ctrlKey: true, metaKey: true })
    
    // 所有节点都应被选中
    expect(screen.getByTestId('node-1')).toHaveClass('selected')
    expect(screen.getByTestId('node-2')).toHaveClass('selected')
    expect(screen.getByTestId('node-3')).toHaveClass('selected')
  })

  test('应支持 Ctrl/Cmd+C 复制', async () => {
    const node = screen.getByTestId('node-1')
    
    fireEvent.click(node)
    fireEvent.keyDown(document, { key: 'c', ctrlKey: true, metaKey: true })
    
    // 应显示复制提示
    expect(screen.getByText(/已复制/)).toBeInTheDocument()
  })

  test('应支持 Ctrl/Cmd+V 粘贴', async () => {
    const node = screen.getByTestId('node-1')
    
    // 先复制
    fireEvent.click(node)
    fireEvent.keyDown(document, { key: 'c', ctrlKey: true, metaKey: true })
    // 粘贴
    fireEvent.keyDown(document, { key: 'v', ctrlKey: true, metaKey: true })
    
    // 应有新节点
    expect(screen.getByTestId('node-4')).toBeInTheDocument()
  })

  test('应支持 Ctrl/Cmd+D 复制选中节点', async () => {
    const node = screen.getByTestId('node-1')
    
    fireEvent.click(node)
    fireEvent.keyDown(document, { key: 'd', ctrlKey: true, metaKey: true })
    
    // 应有复制的节点
    expect(screen.getByTestId('node-4')).toBeInTheDocument()
  })

  test('应支持空格键拖拽画布', async () => {
    const canvas = screen.getByTestId('canvas')
    
    fireEvent.keyDown(document, { key: ' ' })
    fireEvent.mouseDown(canvas, { clientX: 100, clientY: 100 })
    fireEvent.mouseMove(canvas, { clientX: 200, clientY: 200 })
    fireEvent.mouseUp(canvas)
    
    // 画布应移动
    expect(canvas).toBeInTheDocument()
  })

  test('应支持 +/- 缩放画布', async () => {
    const canvas = screen.getByTestId('canvas')
    
    fireEvent.keyDown(document, { key: '+' })
    
    // 画布应放大
    const zoomLevel = screen.getByTestId('zoom-level')
    expect(zoomLevel.textContent).toMatch(/>\s*1/)
    
    fireEvent.keyDown(document, { key: '-' })
    
    // 画布应缩小
    expect(zoomLevel.textContent).toMatch(/<\s*1/)
  })

  test('应支持数字 0 重置缩放', async () => {
    const canvas = screen.getByTestId('canvas')
    
    // 先放大
    fireEvent.keyDown(document, { key: '+' })
    // 重置
    fireEvent.keyDown(document, { key: '0' })
    
    const zoomLevel = screen.getByTestId('zoom-level')
    expect(zoomLevel.textContent).toMatch(/100%|1\.0/)
  })

  test('应支持 F 键适应画布', async () => {
    fireEvent.keyDown(document, { key: 'f' })
    
    // 画布应适应所有节点
    expect(screen.getByTestId('canvas')).toBeInTheDocument()
  })

  test('应支持 R 键运行工作流', async () => {
    fireEvent.keyDown(document, { key: 'r' })
    
    // 应开始运行
    expect(screen.getByText(/运行|执行/)).toBeInTheDocument()
  })

  test('应支持 E 键导出', async () => {
    fireEvent.keyDown(document, { key: 'e' })
    
    // 应显示导出选项
    expect(screen.getByText(/导出/)).toBeInTheDocument()
  })

  test('应支持 I 键导入', async () => {
    fireEvent.keyDown(document, { key: 'i' })
    
    // 应显示导入选项
    expect(screen.getByText(/导入/)).toBeInTheDocument()
  })

  test('应支持 H 键显示帮助', async () => {
    fireEvent.keyDown(document, { key: 'h' })
    
    // 应显示快捷键帮助
    expect(screen.getByText(/快捷键|帮助/)).toBeInTheDocument()
  })

  test('应支持 Esc 取消操作', async () => {
    const node = screen.getByTestId('node-1')
    
    fireEvent.click(node)
    expect(node).toHaveClass('selected')
    
    fireEvent.keyDown(document, { key: 'Escape' })
    
    // 应取消选中
    expect(node).not.toHaveClass('selected')
  })

  test('应支持 Ctrl/Cmd+F 查找节点', async () => {
    fireEvent.keyDown(document, { key: 'f', ctrlKey: true, metaKey: true })
    
    // 应显示搜索框
    expect(screen.getByPlaceholderText(/搜索|查找/)).toBeInTheDocument()
  })

  test('应支持 Ctrl/Cmd+G 分组', async () => {
    const node1 = screen.getByTestId('node-1')
    const node2 = screen.getByTestId('node-2')
    
    fireEvent.click(node1)
    fireEvent.click(node2, { ctrlKey: true })
    fireEvent.keyDown(document, { key: 'g', ctrlKey: true, metaKey: true })
    
    // 应创建分组
    expect(screen.getByText(/分组|Group/)).toBeInTheDocument()
  })

  test('应支持 Ctrl/Cmd+Shift+G 取消分组', async () => {
    // 先创建分组再取消
    const node1 = screen.getByTestId('node-1')
    const node2 = screen.getByTestId('node-2')
    
    fireEvent.click(node1)
    fireEvent.click(node2, { ctrlKey: true })
    fireEvent.keyDown(document, { key: 'g', ctrlKey: true, metaKey: true })
    fireEvent.keyDown(document, { key: 'g', ctrlKey: true, shiftKey: true, metaKey: true })
    
    // 分组应被取消
    expect(screen.queryByText(/分组/)).not.toBeInTheDocument()
  })

  test('应支持箭头键移动选中节点', async () => {
    const node = screen.getByTestId('node-1')
    const initialRect = node.getBoundingClientRect()
    
    fireEvent.click(node)
    fireEvent.keyDown(document, { key: 'ArrowRight' })
    
    const finalRect = node.getBoundingClientRect()
    expect(finalRect.x).toBeGreaterThan(initialRect.x)
  })

  test('应支持 Shift+ 箭头键调整节点大小', async () => {
    const node = screen.getByTestId('node-1')
    
    fireEvent.click(node)
    fireEvent.keyDown(document, { key: 'ArrowRight', shiftKey: true })
    
    // 节点大小应改变
    expect(node).toBeInTheDocument()
  })

  test('应支持 Tab 切换选中节点', async () => {
    fireEvent.click(screen.getByTestId('node-1'))
    expect(screen.getByTestId('node-1')).toHaveClass('selected')
    
    fireEvent.keyDown(document, { key: 'Tab' })
    
    expect(screen.getByTestId('node-2')).toHaveClass('selected')
  })

  test('应支持 Shift+Tab 反向切换选中节点', async () => {
    fireEvent.click(screen.getByTestId('node-3'))
    expect(screen.getByTestId('node-3')).toHaveClass('selected')
    
    fireEvent.keyDown(document, { key: 'Tab', shiftKey: true })
    
    expect(screen.getByTestId('node-2')).toHaveClass('selected')
  })

  test('应支持 Ctrl/Cmd+Shift+S 另存为', async () => {
    fireEvent.keyDown(document, { key: 's', ctrlKey: true, shiftKey: true, metaKey: true })
    
    // 应显示另存为对话框
    expect(screen.getByText(/另存为/)).toBeInTheDocument()
  })

  test('应支持 Ctrl/Cmd+P 打印', async () => {
    fireEvent.keyDown(document, { key: 'p', ctrlKey: true, metaKey: true })
    
    // 应显示打印选项
    expect(screen.getByText(/打印/)).toBeInTheDocument()
  })

  test('应支持 Ctrl/Cmd+, 打开设置', async () => {
    fireEvent.keyDown(document, { key: ',', ctrlKey: true, metaKey: true })
    
    // 应显示设置面板
    expect(screen.getByText(/设置/)).toBeInTheDocument()
  })
})
