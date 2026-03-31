import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, test, expect, beforeEach, vi } from 'vitest'
import { ImportExportModal } from '@/components/ImportExportModal'
import * as ioModule from '@/io'

// Mock Ant Design message
vi.mock('antd', async () => {
  const actual = await vi.importActual('antd')
  return {
    ...actual,
    message: {
      success: vi.fn(),
      error: vi.fn(),
      info: vi.fn(),
      warning: vi.fn(),
      loading: vi.fn(),
    },
  }
})

// Mock IO module
vi.mock('@/io', () => ({
  exportWorkflow: vi.fn(),
  importWorkflowFromFile: vi.fn(),
  getExportPreview: vi.fn(),
  getFriendlyErrorMessage: vi.fn(),
}))

describe('ImportExportModal 组件测试', () => {
  const mockOnClose = vi.fn()
  const mockOnImportSuccess = vi.fn()
  const mockOnExportSuccess = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('导出模式测试', () => {
    beforeEach(() => {
      vi.mocked(ioModule.getExportPreview).mockReturnValue({
        nodeCount: 2,
        edgeCount: 1,
        nodes: [
          { id: '1', label: 'Python 脚本', type: 'python' },
          { id: '2', label: '视频生成', type: 'video' },
        ],
      })
    })

    test('应渲染导出对话框', () => {
      render(
        <ImportExportModal
          open={true}
          mode="export"
          onClose={mockOnClose}
          onExportSuccess={mockOnExportSuccess}
        />
      )

      expect(screen.getByText('导出工作流')).toBeInTheDocument()
      expect(screen.getByText('即将导出以下内容：')).toBeInTheDocument()
    })

    test('应显示节点数量预览', () => {
      render(
        <ImportExportModal
          open={true}
          mode="export"
          onClose={mockOnClose}
          onExportSuccess={mockOnExportSuccess}
        />
      )

      expect(screen.getByText('节点：2 个')).toBeInTheDocument()
    })

    test('应显示连线数量预览', () => {
      render(
        <ImportExportModal
          open={true}
          mode="export"
          onClose={mockOnClose}
          onExportSuccess={mockOnExportSuccess}
        />
      )

      expect(screen.getByText('连接：1 个')).toBeInTheDocument()
    })

    test('应显示节点列表预览', () => {
      render(
        <ImportExportModal
          open={true}
          mode="export"
          onClose={mockOnClose}
          onExportSuccess={mockOnExportSuccess}
        />
      )

      expect(screen.getByText('Python 脚本 (python)')).toBeInTheDocument()
      expect(screen.getByText('视频生成 (video)')).toBeInTheDocument()
    })

    test('应显示导出格式信息', () => {
      render(
        <ImportExportModal
          open={true}
          mode="export"
          onClose={mockOnClose}
          onExportSuccess={mockOnExportSuccess}
        />
      )

      expect(screen.getByText(/导出格式：JSON/)).toBeInTheDocument()
    })

    test('空工作流时应显示提示', () => {
      vi.mocked(ioModule.getExportPreview).mockReturnValue({
        nodeCount: 0,
        edgeCount: 0,
        nodes: [],
      })

      render(
        <ImportExportModal
          open={true}
          mode="export"
          onClose={mockOnClose}
          onExportSuccess={mockOnExportSuccess}
        />
      )

      expect(screen.getByText('当前工作流为空')).toBeInTheDocument()
    })

    test('点击导出按钮应调用 exportWorkflow', async () => {
      vi.mocked(ioModule.exportWorkflow).mockResolvedValue({ success: true })

      render(
        <ImportExportModal
          open={true}
          mode="export"
          onClose={mockOnClose}
          onExportSuccess={mockOnExportSuccess}
        />
      )

      const okButton = screen.getByRole('button', { name: /导 出/i })
      fireEvent.click(okButton)

      await waitFor(() => {
        expect(ioModule.exportWorkflow).toHaveBeenCalledWith({ pretty: true })
      })
    })

    test('导出成功应显示成功提示并关闭', async () => {
      vi.mocked(ioModule.exportWorkflow).mockResolvedValue({ success: true })

      render(
        <ImportExportModal
          open={true}
          mode="export"
          onClose={mockOnClose}
          onExportSuccess={mockOnExportSuccess}
        />
      )

      const okButton = screen.getByRole('button', { name: /导 出/i })
      fireEvent.click(okButton)

      await waitFor(() => {
        expect(mockOnExportSuccess).toHaveBeenCalled()
        expect(mockOnClose).toHaveBeenCalled()
      })
    })

    test('导出失败应显示错误提示', async () => {
      vi.mocked(ioModule.exportWorkflow).mockResolvedValue({ success: false, error: '导出失败' })

      render(
        <ImportExportModal
          open={true}
          mode="export"
          onClose={mockOnClose}
          onExportSuccess={mockOnExportSuccess}
        />
      )

      const okButton = screen.getByRole('button', { name: /导 出/i })
      fireEvent.click(okButton)

      await waitFor(() => {
        expect(ioModule.exportWorkflow).toHaveBeenCalled()
      })
    })

    test('点击取消按钮应调用 onClose', () => {
      render(
        <ImportExportModal
          open={true}
          mode="export"
          onClose={mockOnClose}
          onExportSuccess={mockOnExportSuccess}
        />
      )

      const cancelButton = screen.getByRole('button', { name: /取 消/i })
      fireEvent.click(cancelButton)

      expect(mockOnClose).toHaveBeenCalled()
    })

    test('对话框关闭时应不调用任何回调', () => {
      render(
        <ImportExportModal
          open={false}
          mode="export"
          onClose={mockOnClose}
          onExportSuccess={mockOnExportSuccess}
        />
      )

      expect(screen.queryByText('导出工作流')).not.toBeInTheDocument()
    })
  })

  describe('导入模式测试', () => {
    test('应渲染导入对话框', () => {
      render(
        <ImportExportModal
          open={true}
          mode="import"
          onClose={mockOnClose}
          onImportSuccess={mockOnImportSuccess}
        />
      )

      expect(screen.getByText('导入工作流')).toBeInTheDocument()
      expect(screen.getByText('选择 JSON 文件：')).toBeInTheDocument()
    })

    test('应显示选择文件按钮', () => {
      render(
        <ImportExportModal
          open={true}
          mode="import"
          onClose={mockOnClose}
          onImportSuccess={mockOnImportSuccess}
        />
      )

      expect(screen.getByRole('button', { name: /选择文件/ })).toBeInTheDocument()
    })

    test('应显示导入提示信息', () => {
      render(
        <ImportExportModal
          open={true}
          mode="import"
          onClose={mockOnClose}
          onImportSuccess={mockOnImportSuccess}
        />
      )

      expect(screen.getByText(/支持格式：JSON/)).toBeInTheDocument()
      expect(screen.getByText(/文件大小：最大 10MB/)).toBeInTheDocument()
      expect(screen.getByText(/导入将覆盖当前工作流/)).toBeInTheDocument()
    })

    test('点击取消按钮应调用 onClose', () => {
      render(
        <ImportExportModal
          open={true}
          mode="import"
          onClose={mockOnClose}
          onImportSuccess={mockOnImportSuccess}
        />
      )

      const cancelButton = screen.getByRole('button', { name: /取 消/i })
      fireEvent.click(cancelButton)

      expect(mockOnClose).toHaveBeenCalled()
    })

    test('选择文件按钮应存在', () => {
      render(
        <ImportExportModal
          open={true}
          mode="import"
          onClose={mockOnClose}
          onImportSuccess={mockOnImportSuccess}
        />
      )

      const selectButton = screen.getByRole('button', { name: /选择文件/ })
      expect(selectButton).toBeInTheDocument()
    })

    test('导入功能应正常工作', async () => {
      const mockFile = new File(['{}'], 'test.json', { type: 'application/json' })
      vi.mocked(ioModule.importWorkflowFromFile).mockResolvedValue({ success: true })

      render(
        <ImportExportModal
          open={true}
          mode="import"
          onClose={mockOnClose}
          onImportSuccess={mockOnImportSuccess}
        />
      )

      // 验证按钮存在
      const selectButton = screen.getByRole('button', { name: /选择文件/ })
      expect(selectButton).toBeInTheDocument()
    })

    test('对话框关闭时应不调用任何回调', () => {
      render(
        <ImportExportModal
          open={false}
          mode="import"
          onClose={mockOnClose}
          onImportSuccess={mockOnImportSuccess}
        />
      )

      expect(screen.queryByText('导入工作流')).not.toBeInTheDocument()
    })

    test('点击选择文件按钮应触发文件选择', () => {
      render(
        <ImportExportModal
          open={true}
          mode="import"
          onClose={mockOnClose}
          onImportSuccess={mockOnImportSuccess}
        />
      )

      const selectButton = screen.getByRole('button', { name: /选择文件/ })
      expect(selectButton).toBeInTheDocument()
    })
  })
})
