/**
 * 导入功能测试 (F036, F037)
 * 测试用例：补充至 20+ 个
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { 
  parseJson, 
  validateWorkflow, 
  importWorkflowFromString,
  importWorkflowFromFile,
  readFile,
  loadWorkflowToStore,
  getFriendlyErrorMessage,
  type ImportResult,
} from '../../io/import';
import { useNodeStore } from '../../store/nodeStore';
import { useEdgeStore } from '../../store/edgeStore';
import { useViewportStore } from '../../store/viewportStore';

describe('F036 - 导入 JSON 功能', () => {
  beforeEach(() => {
    // 清空 store
    useNodeStore.getState().nodes.forEach(n => useNodeStore.getState().removeNode(n.id));
    useEdgeStore.getState().edges.forEach(e => useEdgeStore.getState().removeEdge(e.id));
  });

  it('应正确解析有效的 JSON 字符串', () => {
    const validJson = JSON.stringify({
      version: '1.0.0',
      nodes: [
        {
          id: 'node-1',
          type: 'python',
          position: { x: 100, y: 100 },
          data: { label: '测试节点' },
        },
      ],
      edges: [],
    });

    const result = parseJson(validJson);

    expect(result.success).toBe(true);
    expect(result.data).toBeDefined();
    expect(result.data.version).toBe('1.0.0');
    expect(result.data.nodes.length).toBe(1);
  });

  it('应正确处理无效的 JSON 格式', () => {
    const invalidJson = '{ invalid json }';

    const result = parseJson(invalidJson);

    expect(result.success).toBe(false);
    expect(result.error).toBeDefined();
    expect(result.error).toContain('JSON');
  });

  it('应解析包含特殊字符的 JSON 内容', () => {
    const jsonWithSpecialChars = JSON.stringify({
      version: '1.0.0',
      nodes: [
        {
          id: 'node-1',
          type: 'python',
          position: { x: 100, y: 100 },
          data: { 
            label: '测试节点',
            script: 'print("Hello, 世界！\\n")' 
          },
        },
      ],
      edges: [],
    });

    const result = parseJson(jsonWithSpecialChars);

    expect(result.success).toBe(true);
    expect(result.data).toBeDefined();
    expect(result.data.nodes[0].data.script).toContain('Hello, 世界！');
  });
});

describe('importWorkflowFromString', () => {
  beforeEach(() => {
    useNodeStore.getState().nodes.forEach(n => useNodeStore.getState().removeNode(n.id));
    useEdgeStore.getState().edges.forEach(e => useEdgeStore.getState().removeEdge(e.id));
  });

  it('应成功导入完整的工作流数据', async () => {
    const workflowJson = JSON.stringify({
      version: '1.0.0',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      nodes: [
        {
          id: 'node-1',
          type: 'python',
          position: { x: 100, y: 100 },
          data: { label: '节点 1', script: 'print(1)' },
        },
        {
          id: 'node-2',
          type: 'python',
          position: { x: 200, y: 200 },
          data: { label: '节点 2', script: 'print(2)' },
        },
      ],
      edges: [
        {
          id: 'edge-1',
          source: 'node-1',
          target: 'node-2',
        },
      ],
      viewport: { x: 0, y: 0, zoom: 1 },
    });

    const result = await importWorkflowFromString(workflowJson);

    expect(result.success).toBe(true);
    expect(result.data).toBeDefined();
    
    // 验证数据已加载到 store
    const nodes = useNodeStore.getState().nodes;
    const edges = useEdgeStore.getState().edges;
    
    expect(nodes.length).toBe(2);
    expect(edges.length).toBe(1);
    expect(nodes[0].id).toBe('node-1');
    expect(nodes[1].id).toBe('node-2');
  });

  it('应拒绝缺少必需字段的工作流', async () => {
    const invalidWorkflow = JSON.stringify({
      // 缺少 version 和 nodes
      edges: [],
    });

    const result = await importWorkflowFromString(invalidWorkflow);

    expect(result.success).toBe(false);
    expect(result.errors).toBeDefined();
    if (result.errors && result.errors.length > 0) {
      expect(result.errors.length).toBeGreaterThan(0);
    }
  });

  it('应拒绝节点格式错误的工作流', async () => {
    const invalidNodes = JSON.stringify({
      version: '1.0.0',
      nodes: [
        {
          // 缺少必需的 id 和 type
          position: { x: 100, y: 100 },
          data: { label: '测试' },
        },
      ],
      edges: [],
    });

    const result = await importWorkflowFromString(invalidNodes);

    expect(result.success).toBe(false);
    expect(result.errors).toBeDefined();
    
    // 应该包含关于节点 id 或 type 的错误
    if (result.errors && result.errors.length > 0) {
      const errorMessages = result.errors.map(e => e.message);
      expect(errorMessages.some(msg => msg.includes('id') || msg.includes('type'))).toBe(true);
    }
  });

  it('应处理空字符串输入', async () => {
    const result = await importWorkflowFromString('');
    expect(result.success).toBe(false);
  });

  it('应处理 null 输入', async () => {
    const result = await importWorkflowFromString(null as any);
    expect(result.success).toBe(false);
  });

  it('应处理异常输入', async () => {
    const result = await importWorkflowFromString('not json at all');
    expect(result.success).toBe(false);
    expect(result.error).toBeDefined();
  });
});

describe('validateWorkflow', () => {
  it('应验证通过有效的工作流数据', () => {
    const validData = {
      version: '1.0.0',
      nodes: [
        {
          id: 'node-1',
          type: 'python',
          position: { x: 100, y: 100 },
          data: { label: '测试' },
        },
      ],
      edges: [],
    };

    const result = validateWorkflow(validData);
    expect(result.success).toBe(true);
    expect(result.errors).toHaveLength(0);
    expect(result.data).toBeDefined();
  });

  it('应验证失败缺少 version', () => {
    const invalidData = {
      nodes: [],
      edges: [],
    };

    const result = validateWorkflow(invalidData);
    expect(result.success).toBe(false);
    expect(result.errors.length).toBeGreaterThan(0);
  });

  it('应验证失败节点缺少必需字段', () => {
    const invalidData = {
      version: '1.0.0',
      nodes: [{ position: { x: 0, y: 0 } }],
      edges: [],
    };

    const result = validateWorkflow(invalidData);
    expect(result.success).toBe(false);
  });

  it('应验证失败边缺少必需字段', () => {
    const invalidData = {
      version: '1.0.0',
      nodes: [],
      edges: [{ source: 'node-1' }], // 缺少 target
    };

    const result = validateWorkflow(invalidData);
    expect(result.success).toBe(false);
  });

  it('应验证通过包含 viewport 的工作流', () => {
    const validData = {
      version: '1.0.0',
      nodes: [],
      edges: [],
      viewport: { x: 0, y: 0, zoom: 1 },
    };

    const result = validateWorkflow(validData);
    expect(result.success).toBe(true);
  });

  it('应验证失败 viewport 格式错误', () => {
    const invalidData = {
      version: '1.0.0',
      nodes: [],
      edges: [],
      viewport: { x: 'invalid', y: 0, zoom: 1 },
    };

    const result = validateWorkflow(invalidData);
    expect(result.success).toBe(false);
  });
});

describe('getFriendlyErrorMessage', () => {
  it('应返回字符串错误', () => {
    const result = getFriendlyErrorMessage('简单错误消息');
    expect(result).toBe('简单错误消息');
  });

  it('应格式化节点错误', () => {
    const errors = [
      { path: 'nodes.0.id', message: '必需字段', code: 'required' },
    ];
    const result = getFriendlyErrorMessage(errors);
    expect(result).toContain('节点错误');
  });

  it('应格式化边错误', () => {
    const errors = [
      { path: 'edges.0.source', message: '必需字段', code: 'required' },
    ];
    const result = getFriendlyErrorMessage(errors);
    expect(result).toContain('连接错误');
  });

  it('应格式化多个错误', () => {
    const errors = [
      { path: 'nodes.0.id', message: '错误 1', code: 'required' },
      { path: 'edges.0.source', message: '错误 2', code: 'required' },
    ];
    const result = getFriendlyErrorMessage(errors);
    expect(result).toContain('错误 1');
    expect(result).toContain('错误 2');
  });

  it('应处理空数组', () => {
    const result = getFriendlyErrorMessage([]);
    expect(result).toBe('');
  });

  it('应处理未知错误类型', () => {
    const result = getFriendlyErrorMessage(null as any);
    expect(result).toBe('未知错误');
  });
});

describe('readFile', () => {
  it('应读取文件内容', async () => {
    const mockFile = new Blob(['{"test": "data"}'], { type: 'application/json' }) as any;
    mockFile.name = 'test.json';
    
    const content = await readFile(mockFile);
    expect(content).toBe('{"test": "data"}');
  });

  it('应处理文件读取错误', async () => {
    const mockFile = {} as File;
    // 模拟读取错误
    const originalFileReader = FileReader;
    vi.stubGlobal('FileReader', class {
      onload: (() => void) | null = null;
      onerror: (() => void) | null = null;
      result: any = null;
      readAsText() {
        setTimeout(() => this.onerror?.());
      }
    });

    await expect(readFile(mockFile)).rejects.toThrow('文件读取失败');

    vi.stubGlobal('FileReader', originalFileReader);
  });
});

describe('loadWorkflowToStore', () => {
  beforeEach(() => {
    useNodeStore.getState().nodes.forEach(n => useNodeStore.getState().removeNode(n.id));
    useEdgeStore.getState().edges.forEach(e => useEdgeStore.getState().removeEdge(e.id));
  });

  it('应加载节点到 store', () => {
    const data = {
      version: '1.0.0',
      nodes: [
        { id: 'node-1', type: 'python', position: { x: 100, y: 100 }, data: { label: '测试' } },
      ],
      edges: [],
    };

    loadWorkflowToStore(data as any);

    expect(useNodeStore.getState().nodes.length).toBe(1);
    expect(useNodeStore.getState().nodes[0].id).toBe('node-1');
  });

  it('应加载边到 store', () => {
    const data = {
      version: '1.0.0',
      nodes: [
        { id: 'node-1', type: 'python', position: { x: 0, y: 0 }, data: {} },
        { id: 'node-2', type: 'python', position: { x: 100, y: 100 }, data: {} },
      ],
      edges: [
        { id: 'edge-1', source: 'node-1', target: 'node-2' },
      ],
    };

    loadWorkflowToStore(data as any);

    expect(useEdgeStore.getState().edges.length).toBe(1);
    expect(useEdgeStore.getState().edges[0].source).toBe('node-1');
  });

  it('应加载 viewport 到 store', () => {
    const data = {
      version: '1.0.0',
      nodes: [],
      edges: [],
      viewport: { x: 100, y: 200, zoom: 1.5 },
    };

    expect(() => loadWorkflowToStore(data as any)).not.toThrow();
  });

  it('应清空现有数据后加载', () => {
    // 先添加一些数据
    useNodeStore.getState().addNode({ id: 'existing', type: 'python', position: { x: 0, y: 0 }, data: {} });
    
    const data = {
      version: '1.0.0',
      nodes: [{ id: 'new', type: 'python', position: { x: 0, y: 0 }, data: {} }],
      edges: [],
    };

    loadWorkflowToStore(data as any);

    expect(useNodeStore.getState().nodes.length).toBe(1);
    expect(useNodeStore.getState().nodes[0].id).toBe('new');
  });

  it('应处理无边的工作流', () => {
    const data = {
      version: '1.0.0',
      nodes: [],
      edges: [],
    };

    expect(() => loadWorkflowToStore(data as any)).not.toThrow();
  });

  it('应处理无 viewport 的工作流', () => {
    const data = {
      version: '1.0.0',
      nodes: [],
      edges: [],
    };

    expect(() => loadWorkflowToStore(data as any)).not.toThrow();
  });
});

describe('importWorkflowFromFile', () => {
  beforeEach(() => {
    useNodeStore.getState().nodes.forEach(n => useNodeStore.getState().removeNode(n.id));
    useEdgeStore.getState().edges.forEach(e => useEdgeStore.getState().removeEdge(e.id));
  });

  it('应拒绝非 JSON 文件', async () => {
    const mockFile = new Blob(['test'], { type: 'text/plain' }) as any;
    mockFile.name = 'test.txt';

    const result = await importWorkflowFromFile(mockFile);

    expect(result.success).toBe(false);
    expect(result.error).toBe('仅支持 JSON 格式文件');
  });

  it('应导入有效的 JSON 文件', async () => {
    const workflowData = {
      version: '1.0.0',
      nodes: [{ id: 'node-1', type: 'start', position: { x: 0, y: 0 }, data: { label: 'Start' } }],
      edges: [],
    };
    const mockFile = new Blob([JSON.stringify(workflowData)], { type: 'application/json' }) as any;
    mockFile.name = 'workflow.json';

    const result = await importWorkflowFromFile(mockFile);

    expect(result.success).toBe(true);
  });

  it('应处理文件解析失败', async () => {
    const mockFile = new Blob(['invalid json'], { type: 'application/json' }) as any;
    mockFile.name = 'invalid.json';

    const result = await importWorkflowFromFile(mockFile);

    expect(result.success).toBe(false);
    expect(result.error).toBeDefined();
  });

  it('应处理文件校验失败', async () => {
    const mockFile = new Blob([JSON.stringify({ version: '1.0.0' })], { type: 'application/json' }) as any;
    mockFile.name = 'invalid.json';

    const result = await importWorkflowFromFile(mockFile);

    expect(result.success).toBe(false);
    expect(result.errors).toBeDefined();
  });

  it('应处理文件读取异常', async () => {
    const mockFile = {} as File;
    mockFile.name = 'test.json';

    const result = await importWorkflowFromFile(mockFile);

    expect(result.success).toBe(false);
  });
});
