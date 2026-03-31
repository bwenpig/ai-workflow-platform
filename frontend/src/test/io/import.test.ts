/**
 * 导入功能测试 (F036)
 * 测试用例：3 个
 */

import { describe, it, expect, beforeEach } from 'vitest';
import { 
  parseJson, 
  validateWorkflow, 
  importWorkflowFromString,
  getFriendlyErrorMessage,
} from '../../io/import';
import { useNodeStore } from '../../store/nodeStore';
import { useEdgeStore } from '../../store/edgeStore';

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
});
