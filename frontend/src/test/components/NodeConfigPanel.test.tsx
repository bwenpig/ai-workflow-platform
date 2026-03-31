import { render, screen, fireEvent } from '@testing-library/react'
import { describe, test, expect, beforeEach, vi } from 'vitest'
import NodeConfigPanel from '@/components/NodeConfigPanel'

describe('NodeConfigPanel 组件测试', () => {
  const mockNode = {
    nodeId: 'node-1',
    type: 'input',
    config: {
      prompt: '测试提示词',
    },
  }

  const mockOnClose = vi.fn()
  const mockOnSave = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('表单渲染', () => {
    test('应渲染节点配置面板', () => {
      render(
        <NodeConfigPanel
          node={mockNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      expect(screen.getByText('节点配置')).toBeInTheDocument()
      expect(screen.getByText('基本信息')).toBeInTheDocument()
      expect(screen.getByText('参数配置')).toBeInTheDocument()
    })

    test('当 node 为 null 时不应渲染任何内容', () => {
      const { container } = render(
        <NodeConfigPanel
          node={null}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      expect(container.firstChild).toBeNull()
    })

    test('应显示节点 ID', () => {
      render(
        <NodeConfigPanel
          node={mockNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const nodeIdInput = screen.getByDisplayValue('node-1')
      expect(nodeIdInput).toBeInTheDocument()
      expect(nodeIdInput).toBeDisabled()
    })

    test('应显示节点类型', () => {
      render(
        <NodeConfigPanel
          node={mockNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const typeInput = screen.getByDisplayValue('input')
      expect(typeInput).toBeInTheDocument()
      expect(typeInput).toBeDisabled()
    })

    test('应显示保存按钮', () => {
      render(
        <NodeConfigPanel
          node={mockNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      expect(screen.getByRole('button', { name: '保存' })).toBeInTheDocument()
    })

    test('应显示取消按钮', () => {
      render(
        <NodeConfigPanel
          node={mockNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      expect(screen.getByRole('button', { name: '取消' })).toBeInTheDocument()
    })
  })

  describe('input 类型节点配置', () => {
    test('应渲染提示词输入框', () => {
      render(
        <NodeConfigPanel
          node={mockNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const promptInput = screen.getByPlaceholderText('输入提示词...')
      expect(promptInput).toBeInTheDocument()
    })

    test('应显示现有提示词值', () => {
      render(
        <NodeConfigPanel
          node={mockNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const promptInput = screen.getByDisplayValue('测试提示词')
      expect(promptInput).toBeInTheDocument()
    })

    test('应支持修改提示词', () => {
      render(
        <NodeConfigPanel
          node={mockNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const promptInput = screen.getByPlaceholderText('输入提示词...')
      fireEvent.change(promptInput, { target: { value: '新的提示词' } })

      expect(promptInput).toHaveValue('新的提示词')
    })
  })

  describe('model 类型节点配置', () => {
    const modelNode = {
      nodeId: 'node-2',
      type: 'model',
      modelProvider: 'kling',
      config: {
        prompt: '生成视频',
        duration: 5,
        fps: 24,
      },
    }

    test('应渲染模型选择下拉框', () => {
      render(
        <NodeConfigPanel
          node={modelNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const selects = screen.getAllByRole('combobox')
      expect(selects.length).toBeGreaterThanOrEqual(1)
    })

    test('应显示所有模型选项', () => {
      render(
        <NodeConfigPanel
          node={modelNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      expect(screen.getByText('🎬 可灵 (Kling) - 视频生成')).toBeInTheDocument()
      expect(screen.getByText('🎨 万相 (Wan) - 图片生成')).toBeInTheDocument()
      expect(screen.getByText('🎬 Seedance - 视频生成')).toBeInTheDocument()
      expect(screen.getByText('🎨 NanoBanana - 图片生成')).toBeInTheDocument()
    })

    test('应显示提示词输入框', () => {
      render(
        <NodeConfigPanel
          node={modelNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const promptInput = screen.getByPlaceholderText('描述你想生成的内容...')
      expect(promptInput).toBeInTheDocument()
    })

    test('视频模型应显示时长输入框', () => {
      render(
        <NodeConfigPanel
          node={modelNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const durationInputs = screen.getAllByRole('spinbutton')
      expect(durationInputs.length).toBeGreaterThan(0)
    })

    test('视频模型应显示帧率选择框', () => {
      render(
        <NodeConfigPanel
          node={modelNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const selects = screen.getAllByRole('combobox')
      expect(selects.length).toBeGreaterThanOrEqual(2)
    })

    test('应支持修改视频时长', () => {
      render(
        <NodeConfigPanel
          node={modelNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const durationInput = screen.getAllByRole('spinbutton')[0]
      fireEvent.change(durationInput, { target: { value: '10' } })

      expect(durationInput).toHaveValue(10)
    })

    test('应支持修改帧率', () => {
      render(
        <NodeConfigPanel
          node={modelNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const selects = screen.getAllByRole('combobox')
      const fpsSelect = selects[selects.length - 1]
      fireEvent.change(fpsSelect, { target: { value: '30' } })

      expect(fpsSelect).toHaveValue('30')
    })

    test('图片模型应显示尺寸选择框', () => {
      const wanNode = {
        ...modelNode,
        modelProvider: 'wan',
        config: { prompt: '生成图片', size: '1024x1024' },
      }

      render(
        <NodeConfigPanel
          node={wanNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const selects = screen.getAllByRole('combobox')
      expect(selects.length).toBeGreaterThanOrEqual(2)
    })

    test('图片模型不应显示时长和帧率选项', () => {
      const wanNode = {
        ...modelNode,
        modelProvider: 'wan',
        config: { prompt: '生成图片', size: '1024x1024' },
      }

      render(
        <NodeConfigPanel
          node={wanNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      expect(screen.queryByRole('spinbutton')).not.toBeInTheDocument()
    })

    test('未选择模型时应只显示模型选择框', () => {
      const emptyModelNode = {
        ...modelNode,
        modelProvider: '',
        config: {},
      }

      render(
        <NodeConfigPanel
          node={emptyModelNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      expect(screen.queryByRole('spinbutton')).not.toBeInTheDocument()
    })
  })

  describe('process 类型节点配置', () => {
    const processNode = {
      nodeId: 'node-3',
      type: 'process',
      config: {
        processType: 'upscale',
      },
    }

    test('应渲染处理类型选择框', () => {
      render(
        <NodeConfigPanel
          node={processNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const select = screen.getByRole('combobox')
      expect(select).toBeInTheDocument()
    })

    test('应显示所有处理类型选项', () => {
      render(
        <NodeConfigPanel
          node={processNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      expect(screen.getByText('🔍 高清放大')).toBeInTheDocument()
      expect(screen.getByText('🎨 风格转换')).toBeInTheDocument()
      expect(screen.getByText('✏️ 局部重绘')).toBeInTheDocument()
      expect(screen.getByText('✂️ 去除背景')).toBeInTheDocument()
    })

    test('应支持修改处理类型', () => {
      render(
        <NodeConfigPanel
          node={processNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const select = screen.getByRole('combobox')
      fireEvent.change(select, { target: { value: 'style' } })

      expect(select).toHaveValue('style')
    })
  })

  describe('未知类型节点配置', () => {
    const unknownNode = {
      nodeId: 'node-4',
      type: 'unknown',
      config: {},
    }

    test('应显示提示消息', () => {
      render(
        <NodeConfigPanel
          node={unknownNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      expect(screen.getByText('该节点类型暂无配置项')).toBeInTheDocument()
    })
  })

  describe('保存操作', () => {
    test('点击保存按钮应调用 onSave', () => {
      render(
        <NodeConfigPanel
          node={mockNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const saveButton = screen.getByRole('button', { name: '保存' })
      fireEvent.click(saveButton)

      expect(mockOnSave).toHaveBeenCalledWith({
        ...mockNode,
        config: { prompt: '测试提示词' },
      })
    })

    test('保存时应包含修改后的配置', () => {
      render(
        <NodeConfigPanel
          node={mockNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const promptInput = screen.getByPlaceholderText('输入提示词...')
      fireEvent.change(promptInput, { target: { value: '新的提示词' } })

      const saveButton = screen.getByRole('button', { name: '保存' })
      fireEvent.click(saveButton)

      expect(mockOnSave).toHaveBeenCalledWith({
        ...mockNode,
        config: { prompt: '新的提示词' },
      })
    })
  })

  describe('取消操作', () => {
    test('点击取消按钮应调用 onClose', () => {
      render(
        <NodeConfigPanel
          node={mockNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const cancelButton = screen.getByRole('button', { name: '取消' })
      fireEvent.click(cancelButton)

      expect(mockOnClose).toHaveBeenCalled()
    })

    test('点击关闭按钮应调用 onClose', () => {
      render(
        <NodeConfigPanel
          node={mockNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const closeButton = screen.getByText('×')
      fireEvent.click(closeButton)

      expect(mockOnClose).toHaveBeenCalled()
    })
  })

  describe('字段验证', () => {
    test('节点 ID 字段应为只读', () => {
      render(
        <NodeConfigPanel
          node={mockNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const nodeIdInput = screen.getByDisplayValue('node-1')
      expect(nodeIdInput).toBeDisabled()
    })

    test('节点类型字段应为只读', () => {
      render(
        <NodeConfigPanel
          node={mockNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const typeInput = screen.getByDisplayValue('input')
      expect(typeInput).toBeDisabled()
    })

    test('时长输入框应为数字类型', () => {
      const modelNode = {
        nodeId: 'node-2',
        type: 'model',
        modelProvider: 'kling',
        config: { prompt: '生成视频', duration: 5, fps: 24 },
      }

      render(
        <NodeConfigPanel
          node={modelNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const durationInput = screen.getAllByRole('spinbutton')[0]
      expect(durationInput).toHaveAttribute('type', 'number')
    })

    test('时长输入框应有合理范围', () => {
      const modelNode = {
        nodeId: 'node-2',
        type: 'model',
        modelProvider: 'kling',
        config: { prompt: '生成视频', duration: 5, fps: 24 },
      }

      render(
        <NodeConfigPanel
          node={modelNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const durationInput = screen.getAllByRole('spinbutton')[0]
      expect(durationInput).toHaveAttribute('min', '1')
      expect(durationInput).toHaveAttribute('max', '30')
    })
  })

  describe('错误提示', () => {
    test('空配置时应能保存', () => {
      const emptyNode = {
        nodeId: 'node-5',
        type: 'input',
        config: {},
      }

      render(
        <NodeConfigPanel
          node={emptyNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const saveButton = screen.getByRole('button', { name: '保存' })
      fireEvent.click(saveButton)

      expect(mockOnSave).toHaveBeenCalledWith({
        ...emptyNode,
        config: {},
      })
    })

    test('配置变更时应立即更新内部状态', () => {
      render(
        <NodeConfigPanel
          node={mockNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const promptInput = screen.getByPlaceholderText('输入提示词...')
      fireEvent.change(promptInput, { target: { value: '新值' } })

      expect(promptInput).toHaveValue('新值')
    })

    test('node 变化时应重新初始化配置', () => {
      const { rerender } = render(
        <NodeConfigPanel
          node={mockNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const newNode = {
        nodeId: 'node-6',
        type: 'input',
        config: { prompt: '不同的提示词' },
      }

      rerender(
        <NodeConfigPanel
          node={newNode}
          onClose={mockOnClose}
          onSave={mockOnSave}
        />
      )

      const promptInput = screen.getByPlaceholderText('输入提示词...')
      expect(promptInput).toHaveValue('不同的提示词')
    })
  })
})
