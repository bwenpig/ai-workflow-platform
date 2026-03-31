/**
 * 覆盖确认对话框测试 (F038)
 * 测试用例：3 个
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ImportExportModal } from '../../components/ImportExportModal';
import { useNodeStore } from '../../store/nodeStore';
import { useEdgeStore } from '../../store/edgeStore';

// Mock antd message
vi.mock('antd', async () => {
  const actual = await vi.importActual('antd');
  return {
    ...actual,
    message: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn(),
    },
  };
});

describe('F038 - 覆盖确认对话框', () => {
  beforeEach(() => {
    // 清空 store
    const nodeState = useNodeStore.getState();
    const edgeState = useEdgeStore.getState();
    [...nodeState.nodes].forEach(n => nodeState.removeNode(n.id));
    [...edgeState.edges].forEach(e => edgeState.removeEdge(e.id));
  });

  it('应显示导出对话框并展示预览信息', async () => {
    // 添加测试节点
    useNodeStore.getState().addNode({
      id: 'node-1',
      type: 'python',
      position: { x: 100, y: 100 },
      data: { label: '测试节点' },
    });

    render(
      <ImportExportModal
        open={true}
        mode="export"
        onClose={vi.fn()}
      />
    );

    // 验证对话框标题
    expect(screen.getByText(/导出工作流/)).toBeInTheDocument();
    
    // 验证预览信息
    await waitFor(() => {
      expect(screen.getByText(/节点：1 个/)).toBeInTheDocument();
    });
    
    // 验证节点信息
    expect(screen.getByText(/测试节点/)).toBeInTheDocument();
  });

  it('应显示导入对话框并提供文件上传选项', () => {
    const onCloseMock = vi.fn();
    
    render(
      <ImportExportModal
        open={true}
        mode="import"
        onClose={onCloseMock}
      />
    );

    // 验证对话框标题
    expect(screen.getByText(/导入工作流/)).toBeInTheDocument();
    
    // 验证文件上传按钮
    expect(screen.getByText(/选择文件/)).toBeInTheDocument();
    
    // 验证提示信息
    expect(screen.getByText(/支持格式：JSON/)).toBeInTheDocument();
    expect(screen.getByText(/导入将覆盖当前工作流/)).toBeInTheDocument();
  });

  it('应支持关闭对话框', async () => {
    const onCloseMock = vi.fn();
    
    render(
      <ImportExportModal
        open={true}
        mode="export"
        onClose={onCloseMock}
      />
    );

    // 点击关闭按钮 (X)
    const closeButton = screen.getByRole('button', { name: /Close/i });
    fireEvent.click(closeButton);

    // 验证关闭回调被调用
    await waitFor(() => {
      expect(onCloseMock).toHaveBeenCalled();
    });
  });
});

describe('导入确认对话框 - 覆盖警告', () => {
  it('应显示覆盖警告提示', () => {
    render(
      <ImportExportModal
        open={true}
        mode="import"
        onClose={vi.fn()}
      />
    );

    // 验证警告图标和提示
    expect(screen.getByText(/注意：导入将覆盖当前工作流/)).toBeInTheDocument();
  });

  it('应在空工作流时显示提示', () => {
    // 确保 store 为空
    const nodeState = useNodeStore.getState();
    [...nodeState.nodes].forEach(n => nodeState.removeNode(n.id));

    render(
      <ImportExportModal
        open={true}
        mode="export"
        onClose={vi.fn()}
      />
    );

    // 验证空工作流提示
    expect(screen.getByText(/当前工作流为空/)).toBeInTheDocument();
  });

  it('应显示文件格式限制', () => {
    render(
      <ImportExportModal
        open={true}
        mode="import"
        onClose={vi.fn()}
      />
    );

    // 验证格式限制
    expect(screen.getByText(/支持格式：JSON/)).toBeInTheDocument();
    expect(screen.getByText(/最大 10MB/)).toBeInTheDocument();
  });
});
