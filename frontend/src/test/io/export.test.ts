/**
 * 导出功能测试 (F035)
 * 测试用例：补充至 15+ 个
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { 
  getExportPreview,
  exportWorkflowAsString,
  exportWorkflow,
  downloadFile,
} from '../../io/export';
import { WorkflowSchema } from '../../io/schema';
import { useNodeStore } from '../../store/nodeStore';
import { useEdgeStore } from '../../store/edgeStore';
import { useViewportStore } from '../../store/viewportStore';

describe('F035 - 导出 JSON 功能', () => {
  beforeEach(() => {
    // 清空 store
    const nodeState = useNodeStore.getState();
    const edgeState = useEdgeStore.getState();
    [...nodeState.nodes].forEach(n => nodeState.removeNode(n.id));
    [...edgeState.edges].forEach(e => edgeState.removeEdge(e.id));
  });

  it('应正确序列化包含节点和边的工作流', () => {
    // 添加测试节点
    useNodeStore.getState().addNode({
      id: 'node-1',
      type: 'python',
      position: { x: 100, y: 100 },
      data: { label: '测试节点', script: 'print("hello")' },
    });

    // 添加测试边
    useEdgeStore.getState().addEdge({
      id: 'edge-1',
      source: 'node-1',
      target: 'node-2',
    });

    const preview = getExportPreview();
    
    expect(preview.nodeCount).toBe(1);
    expect(preview.edgeCount).toBe(1);
    expect(preview.nodes[0].id).toBe('node-1');
    expect(preview.nodes[0].type).toBe('python');
  });

  it('应生成有效的 JSON 格式且可通过 Schema 校验', () => {
    // 添加测试数据
    useNodeStore.getState().addNode({
      id: 'node-1',
      type: 'python',
      position: { x: 200, y: 200 },
      data: { label: '节点 1' },
    });

    const workflowString = exportWorkflowAsString({ pretty: false });
    
    // 验证是有效的 JSON
    const parsed = JSON.parse(workflowString);
    
    // 验证通过 Schema 校验
    const result = WorkflowSchema.safeParse(parsed);
    expect(result.success).toBe(true);
    
    if (result.success) {
      expect(result.data.version).toBe('1.0.0');
      expect(result.data.nodes.length).toBe(1);
      expect(result.data.nodes[0].id).toBe('node-1');
    }
  });

  it('应支持 pretty 和 compact 两种格式', () => {
    useNodeStore.getState().addNode({
      id: 'node-1',
      type: 'python',
      position: { x: 100, y: 100 },
      data: { label: '测试' },
    });

    const prettyJson = exportWorkflowAsString({ pretty: true });
    const compactJson = exportWorkflowAsString({ pretty: false });

    // Pretty 格式应该包含换行和缩进
    expect(prettyJson).toContain('\n');
    expect(prettyJson).toContain('  ');
    
    // Compact 格式应该是单行
    expect(compactJson).not.toContain('\n');
    
    // 两者解析后应该相等（忽略时间戳的毫秒差异）
    const prettyObj = JSON.parse(prettyJson);
    const compactObj = JSON.parse(compactJson);
    
    // 比较除了时间戳之外的所有字段
    expect(prettyObj.version).toEqual(compactObj.version);
    expect(prettyObj.nodes).toEqual(compactObj.nodes);
    expect(prettyObj.edges).toEqual(compactObj.edges);
    expect(prettyObj.metadata).toEqual(compactObj.metadata);
    expect(prettyObj.viewport).toEqual(compactObj.viewport);
  });
});

describe('downloadFile 工具函数', () => {
  it('应创建并触发下载链接', () => {
    const mockLink = {
      href: '',
      download: '',
      style: {} as any,
      click: vi.fn(),
    };
    
    const createElementSpy = vi.spyOn(document, 'createElement').mockReturnValue(mockLink as any);
    const appendChildSpy = vi.spyOn(document.body, 'appendChild').mockImplementation(() => mockLink as any);
    const removeChildSpy = vi.spyOn(document.body, 'removeChild').mockImplementation(() => mockLink as any);

    downloadFile('test content', 'test.json', 'application/json');

    expect(createElementSpy).toHaveBeenCalledWith('a');
    expect(mockLink.download).toBe('test.json');
    expect(mockLink.click).toHaveBeenCalled();
    expect(removeChildSpy).toHaveBeenCalled();

    createElementSpy.mockRestore();
    appendChildSpy.mockRestore();
    removeChildSpy.mockRestore();
  });

  it('应处理默认 MIME 类型', () => {
    const mockLink = {
      href: '',
      download: '',
      style: {} as any,
      click: vi.fn(),
    };
    
    vi.spyOn(document, 'createElement').mockReturnValue(mockLink as any);
    vi.spyOn(document.body, 'appendChild').mockImplementation(() => mockLink as any);
    vi.spyOn(document.body, 'removeChild').mockImplementation(() => mockLink as any);

    downloadFile('test content', 'test.json');

    expect(mockLink.href).toBeDefined();
    expect(mockLink.download).toBe('test.json');
  });
});



describe('exportWorkflow', () => {
  beforeEach(() => {
    const nodeState = useNodeStore.getState();
    const edgeState = useEdgeStore.getState();
    [...nodeState.nodes].forEach(n => nodeState.removeNode(n.id));
    [...edgeState.edges].forEach(e => edgeState.removeEdge(e.id));
  });

  it('应成功导出工作流', async () => {
    useNodeStore.getState().addNode({
      id: 'node-1',
      type: 'python',
      position: { x: 100, y: 100 },
      data: { label: '测试' },
    });

    const result = await exportWorkflow({ pretty: true });

    expect(result.success).toBe(true);
  });

  it('应处理空工作流导出', async () => {
    const result = await exportWorkflow({ pretty: true });

    expect(result.success).toBe(true);
  });

  it('应处理导出异常', async () => {
    // 模拟异常情况
    vi.spyOn(JSON, 'stringify').mockImplementation(() => {
      throw new Error('序列化失败');
    });

    const result = await exportWorkflow({ pretty: true });

    expect(result.success).toBe(false);
    expect(result.error).toBeDefined();

    vi.restoreAllMocks();
  });
});

describe('getExportPreview', () => {
  beforeEach(() => {
    const nodeState = useNodeStore.getState();
    const edgeState = useEdgeStore.getState();
    [...nodeState.nodes].forEach(n => nodeState.removeNode(n.id));
    [...edgeState.edges].forEach(e => edgeState.removeEdge(e.id));
  });

  it('应返回空工作流预览', () => {
    const preview = getExportPreview();
    expect(preview.nodeCount).toBe(0);
    expect(preview.edgeCount).toBe(0);
    expect(preview.nodes).toHaveLength(0);
  });

  it('应返回多个节点的预览', () => {
    useNodeStore.getState().addNode({
      id: 'node-1',
      type: 'python',
      position: { x: 0, y: 0 },
      data: { label: '节点 1' },
    });
    useNodeStore.getState().addNode({
      id: 'node-2',
      type: 'python',
      position: { x: 100, y: 100 },
      data: { label: '节点 2' },
    });
    useNodeStore.getState().addNode({
      id: 'node-3',
      type: 'condition',
      position: { x: 200, y: 200 },
      data: { label: '节点 3' },
    });

    const preview = getExportPreview();
    expect(preview.nodeCount).toBe(3);
    expect(preview.nodes.length).toBe(3);
    expect(preview.nodes[0].label).toBe('节点 1');
  });

  it('应返回边的预览', () => {
    useNodeStore.getState().addNode({
      id: 'node-1',
      type: 'python',
      position: { x: 0, y: 0 },
      data: {},
    });
    useNodeStore.getState().addNode({
      id: 'node-2',
      type: 'python',
      position: { x: 100, y: 100 },
      data: {},
    });
    useEdgeStore.getState().addEdge({
      id: 'edge-1',
      source: 'node-1',
      target: 'node-2',
    });

    const preview = getExportPreview();
    expect(preview.edgeCount).toBe(1);
  });

  it('应包含节点类型信息', () => {
    useNodeStore.getState().addNode({
      id: 'node-1',
      type: 'condition',
      position: { x: 0, y: 0 },
      data: { label: '条件节点' },
    });

    const preview = getExportPreview();
    expect(preview.nodes[0].type).toBe('condition');
  });

  it('应处理大量节点', () => {
    for (let i = 0; i < 100; i++) {
      useNodeStore.getState().addNode({
        id: `node-${i}`,
        type: 'python',
        position: { x: i * 10, y: i * 10 },
        data: { label: `节点${i}` },
      });
    }

    const preview = getExportPreview();
    expect(preview.nodeCount).toBe(100);
    expect(preview.nodes.length).toBe(100);
  });
});

describe('exportWorkflowAsString', () => {
  beforeEach(() => {
    const nodeState = useNodeStore.getState();
    const edgeState = useEdgeStore.getState();
    [...nodeState.nodes].forEach(n => nodeState.removeNode(n.id));
    [...edgeState.edges].forEach(e => edgeState.removeEdge(e.id));
  });

  it('应导出包含 viewport 的工作流', () => {
    useViewportStore.getState().updateViewport({ zoom: 1.5, pan: { x: 100, y: 200 } });

    const result = exportWorkflowAsString({ pretty: false });
    const parsed = JSON.parse(result);

    expect(parsed.viewport).toBeDefined();
  });

  it('应处理特殊字符', () => {
    useNodeStore.getState().addNode({
      id: 'node-1',
      type: 'python',
      position: { x: 0, y: 0 },
      data: { label: '测试\n换行', script: 'print("Hello, 世界！")' },
    });

    const result = exportWorkflowAsString({ pretty: false });
    const parsed = JSON.parse(result);

    expect(parsed.nodes[0].data.script).toContain('世界');
  });

  it('应处理空数据', () => {
    const result = exportWorkflowAsString({ pretty: false });
    const parsed = JSON.parse(result);

    expect(parsed.nodes).toHaveLength(0);
    expect(parsed.edges).toHaveLength(0);
  });
});
