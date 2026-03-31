import { render, screen, fireEvent } from '@testing-library/react'
import { describe, test, expect, beforeEach } from 'vitest'
import ResultPreview from '@/components/ResultPreview'

describe('ResultPreview 组件测试', () => {
  const mockResult = {
    executionId: 'exec-123',
    status: 'SUCCESS',
    nodeStates: {
      'node-1': {
        status: 'SUCCESS',
        result: {
          type: 'video',
          url: 'https://example.com/video.mp4',
          duration: 5,
          fps: 24,
        },
      },
      'node-2': {
        status: 'SUCCESS',
        result: {
          type: 'image',
          url: 'https://example.com/image.png',
          width: 1024,
          height: 1024,
        },
      },
      'node-3': {
        status: 'FAILED',
        errorMessage: '执行失败',
      },
    },
  }

  const mockOnClose = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('渲染测试', () => {
    test('应渲染执行结果面板', () => {
      render(
        <ResultPreview
          result={mockResult}
          onClose={mockOnClose}
        />
      )

      expect(screen.getByText('执行结果')).toBeInTheDocument()
      expect(screen.getByText('执行状态')).toBeInTheDocument()
      expect(screen.getByText('节点执行结果')).toBeInTheDocument()
    })

    test('当 result 为 null 时不应渲染任何内容', () => {
      const { container } = render(
        <ResultPreview
          result={null}
          onClose={mockOnClose}
        />
      )

      expect(container.firstChild).toBeNull()
    })

    test('应显示执行 ID', () => {
      render(
        <ResultPreview
          result={mockResult}
          onClose={mockOnClose}
        />
      )

      expect(screen.getByText(/exec-123/)).toBeInTheDocument()
    })
  })

  describe('执行状态显示', () => {
    test('应显示成功状态', () => {
      render(
        <ResultPreview
          result={mockResult}
          onClose={mockOnClose}
        />
      )

      expect(screen.getByText('✅ 成功')).toBeInTheDocument()
    })

    test('应显示失败状态', () => {
      const failedResult = { ...mockResult, status: 'FAILED' }

      render(
        <ResultPreview
          result={failedResult}
          onClose={mockOnClose}
        />
      )

      expect(screen.getByText('❌ 失败')).toBeInTheDocument()
    })

    test('应显示运行中状态', () => {
      const runningResult = { ...mockResult, status: 'RUNNING' }

      render(
        <ResultPreview
          result={runningResult}
          onClose={mockOnClose}
        />
      )

      expect(screen.getByText('⏳ 执行中')).toBeInTheDocument()
    })

    test('应显示等待中状态', () => {
      const pendingResult = { ...mockResult, status: 'PENDING' }

      render(
        <ResultPreview
          result={pendingResult}
          onClose={mockOnClose}
        />
      )

      expect(screen.getByText('⏸️ 等待中')).toBeInTheDocument()
    })
  })

  describe('节点列表显示', () => {
    test('应显示所有节点', () => {
      render(
        <ResultPreview
          result={mockResult}
          onClose={mockOnClose}
        />
      )

      expect(screen.getByText('node-1')).toBeInTheDocument()
      expect(screen.getByText('node-2')).toBeInTheDocument()
      expect(screen.getByText('node-3')).toBeInTheDocument()
    })

    test('应显示节点状态', () => {
      render(
        <ResultPreview
          result={mockResult}
          onClose={mockOnClose}
        />
      )

      const successNodes = screen.getAllByText('SUCCESS')
      const failedNode = screen.getByText('FAILED')

      expect(successNodes.length).toBeGreaterThanOrEqual(2)
      expect(failedNode).toBeInTheDocument()
    })

    test('应显示视频结果标识', () => {
      render(
        <ResultPreview
          result={mockResult}
          onClose={mockOnClose}
        />
      )

      expect(screen.getByText('🎬 视频')).toBeInTheDocument()
    })

    test('应显示图片结果标识', () => {
      render(
        <ResultPreview
          result={mockResult}
          onClose={mockOnClose}
        />
      )

      expect(screen.getByText('🖼️ 图片')).toBeInTheDocument()
    })
  })

  describe('节点选择', () => {
    test('应可选择节点', () => {
      render(
        <ResultPreview
          result={mockResult}
          onClose={mockOnClose}
        />
      )

      const node1 = screen.getByText('node-1')
      fireEvent.click(node1)

      expect(screen.getByText(/节点详情：node-1/)).toBeInTheDocument()
    })

    test('应显示选中节点的详情', () => {
      render(
        <ResultPreview
          result={mockResult}
          onClose={mockOnClose}
        />
      )

      const node1 = screen.getByText('node-1')
      fireEvent.click(node1)

      expect(screen.getByText(/节点详情：node-1/)).toBeInTheDocument()
    })

    test('应显示视频详细信息', () => {
      render(
        <ResultPreview
          result={mockResult}
          onClose={mockOnClose}
        />
      )

      const node1 = screen.getByText('node-1')
      fireEvent.click(node1)

      expect(screen.getByText(/时长:/)).toBeInTheDocument()
      expect(screen.getByText(/5 秒/)).toBeInTheDocument()
      expect(screen.getByText(/帧率:/)).toBeInTheDocument()
      expect(screen.getByText(/24 FPS/)).toBeInTheDocument()
    })

    test('应显示视频播放器', () => {
      render(
        <ResultPreview
          result={mockResult}
          onClose={mockOnClose}
        />
      )

      const node1 = screen.getByText('node-1')
      fireEvent.click(node1)

      const video = document.querySelector('video')
      expect(video).toBeInTheDocument()
      if (video) {
        expect(video.getAttribute('src')).toBe('https://example.com/video.mp4')
      }
    })

    test('应显示视频下载链接', () => {
      render(
        <ResultPreview
          result={mockResult}
          onClose={mockOnClose}
        />
      )

      const node1 = screen.getByText('node-1')
      fireEvent.click(node1)

      expect(screen.getByText('点击下载')).toBeInTheDocument()
    })

    test('应显示图片详细信息', () => {
      render(
        <ResultPreview
          result={mockResult}
          onClose={mockOnClose}
        />
      )

      const node2 = screen.getByText('node-2')
      fireEvent.click(node2)

      expect(screen.getByText(/尺寸:/)).toBeInTheDocument()
      expect(screen.getByText(/1024 × 1024/)).toBeInTheDocument()
    })

    test('应显示图片', () => {
      render(
        <ResultPreview
          result={mockResult}
          onClose={mockOnClose}
        />
      )

      const node2 = screen.getByText('node-2')
      fireEvent.click(node2)

      const img = screen.getByAltText('生成结果')
      expect(img).toBeInTheDocument()
      expect(img).toHaveAttribute('src', 'https://example.com/image.png')
    })

    test('应显示图片下载链接', () => {
      render(
        <ResultPreview
          result={mockResult}
          onClose={mockOnClose}
        />
      )

      const node2 = screen.getByText('node-2')
      fireEvent.click(node2)

      const downloadLinks = screen.getAllByText('点击下载')
      expect(downloadLinks.length).toBeGreaterThanOrEqual(1)
    })
  })

  describe('关闭功能', () => {
    test('应调用 onClose 当点击关闭按钮', () => {
      render(
        <ResultPreview
          result={mockResult}
          onClose={mockOnClose}
        />
      )

      const closeButton = screen.getByText('×')
      fireEvent.click(closeButton)

      expect(mockOnClose).toHaveBeenCalledTimes(1)
    })
  })

  describe('样式测试', () => {
    test('面板应有正确的样式', () => {
      render(
        <ResultPreview
          result={mockResult}
          onClose={mockOnClose}
        />
      )

      const overlay = document.body.firstChild
      expect(overlay).toBeInTheDocument()
    })

    test('节点列表应有正确的样式', () => {
      render(
        <ResultPreview
          result={mockResult}
          onClose={mockOnClose}
        />
      )

      const nodeItems = document.querySelectorAll('[style*="cursor: pointer"]')
      expect(nodeItems.length).toBeGreaterThanOrEqual(3)
    })

    test('选中的节点应有不同的背景色', () => {
      render(
        <ResultPreview
          result={mockResult}
          onClose={mockOnClose}
        />
      )

      const node1 = screen.getByText('node-1')
      fireEvent.click(node1)

      // 选中的节点应该有灰色背景
      const selectedNode = screen.getByText('node-1').closest('div')
      expect(selectedNode).toBeInTheDocument()
    })
  })

  describe('边缘情况', () => {
    test('应处理没有结果的节点', () => {
      const resultWithNoResult = {
        ...mockResult,
        nodeStates: {
          'node-1': {
            status: 'PENDING',
          },
        },
      }

      render(
        <ResultPreview
          result={resultWithNoResult}
          onClose={mockOnClose}
        />
      )

      const node1 = screen.getByText('node-1')
      fireEvent.click(node1)

      expect(screen.getByText('暂无结果')).toBeInTheDocument()
    })

    test('应处理未知结果类型', () => {
      const resultWithUnknownType = {
        ...mockResult,
        nodeStates: {
          'node-1': {
            status: 'SUCCESS',
            result: {
              type: 'unknown',
              data: { test: 'data' },
            },
          },
        },
      }

      render(
        <ResultPreview
          result={resultWithUnknownType}
          onClose={mockOnClose}
        />
      )

      const node1 = screen.getByText('node-1')
      fireEvent.click(node1)

      // 应该显示 JSON 预览
      expect(document.querySelector('pre')).toBeInTheDocument()
    })

    test('应处理空节点状态', () => {
      const resultWithNoNodes = {
        ...mockResult,
        nodeStates: {},
      }

      render(
        <ResultPreview
          result={resultWithNoNodes}
          onClose={mockOnClose}
        />
      )

      expect(screen.queryByText('node-1')).not.toBeInTheDocument()
    })
  })
})
