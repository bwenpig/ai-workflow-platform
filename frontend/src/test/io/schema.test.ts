/**
 * Schema 校验功能测试 (F037)
 * 测试用例：3 个
 */

import { describe, it, expect } from 'vitest';
import { validateWorkflow, getFriendlyErrorMessage } from '../../io/import';
import { WorkflowSchema } from '../../io/schema';

describe('F037 - 格式校验功能', () => {
  it('应通过包含完整字段的工作流校验', () => {
    const validWorkflow = {
      version: '1.0.0',
      name: '测试工作流',
      description: '这是一个测试',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      nodes: [
        {
          id: 'node-1',
          type: 'python',
          position: { x: 100, y: 100 },
          data: {
            label: '节点 1',
            script: 'print("hello")',
            timeout: 30,
            requirements: ['requests'],
          },
        },
      ],
      edges: [
        {
          id: 'edge-1',
          source: 'node-1',
          target: 'node-2',
          sourceHandle: 'output-1',
          targetHandle: 'input-1',
        },
      ],
      viewport: {
        x: 0,
        y: 0,
        zoom: 1,
      },
      metadata: {
        author: 'Test User',
        tags: ['test', 'demo'],
      },
    };

    const result = validateWorkflow(validWorkflow);

    expect(result.success).toBe(true);
    expect(result.errors.length).toBe(0);
    expect(result.data).toBeDefined();
    expect(result.data!.name).toBe('测试工作流');
  });

  it('应拒绝缺少必需字段的工作流并提供错误信息', () => {
    const invalidWorkflow = {
      // 缺少 version
      nodes: [
        {
          // 缺少 id 和 type
          position: { x: 100, y: 100 },
          data: { label: '测试' },
        },
      ],
      edges: [],
    };

    const result = validateWorkflow(invalidWorkflow);

    expect(result.success).toBe(false);
    expect(result.errors).toBeDefined();
    expect(Array.isArray(result.errors)).toBe(true);
    
    // 验证错误信息包含具体字段
    if (result.errors.length > 0) {
      const errorPaths = result.errors.map(e => e.path);
      expect(errorPaths.some(p => p.includes('version') || p.includes('nodes'))).toBe(true);
    }
  });

  it('应拒绝节点位置格式错误的工作流', () => {
    const invalidPosition = {
      version: '1.0.0',
      nodes: [
        {
          id: 'node-1',
          type: 'python',
          position: { x: 'invalid' as any, y: 100 }, // x 应该是 number
          data: { label: '测试' },
        },
      ],
      edges: [],
    };

    const result = validateWorkflow(invalidPosition);

    expect(result.success).toBe(false);
    expect(result.errors).toBeDefined();
    
    // 应该包含关于 position 的错误
    if (result.errors.length > 0) {
      const errorMessages = result.errors.map(e => e.message);
      expect(errorMessages.some(msg => msg.includes('position') || msg.includes('number'))).toBe(true);
    }
  });
});

describe('getFriendlyErrorMessage', () => {
  it('应将技术错误转换为友好的提示信息', () => {
    const validationErrors = [
      {
        path: 'nodes.0.id',
        message: '必需',
        code: 'invalid_type',
      },
      {
        path: 'nodes.0.type',
        message: '必需',
        code: 'invalid_type',
      },
    ];

    const friendlyMessage = getFriendlyErrorMessage(validationErrors);

    expect(friendlyMessage).toContain('节点错误');
    expect(friendlyMessage).toContain('必需');
  });

  it('应处理字符串类型的错误信息', () => {
    const errorMessage = '文件为空或格式错误';
    
    const friendlyMessage = getFriendlyErrorMessage(errorMessage);
    
    expect(friendlyMessage).toBe(errorMessage);
  });

  it('应区分节点错误和边错误', () => {
    const validationErrors = [
      {
        path: 'nodes.0.data.script',
        message: '脚本不能为空',
        code: 'too_small',
      },
      {
        path: 'edges.0.source',
        message: '源节点无效',
        code: 'invalid_string',
      },
    ];

    const friendlyMessage = getFriendlyErrorMessage(validationErrors);

    expect(friendlyMessage).toContain('节点错误');
    expect(friendlyMessage).toContain('连接错误');
  });
});

describe('WorkflowSchema 边界测试', () => {
  it('应接受空节点和边数组', () => {
    const emptyWorkflow = {
      version: '1.0.0',
      nodes: [],
      edges: [],
    };

    const result = WorkflowSchema.safeParse(emptyWorkflow);

    expect(result.success).toBe(true);
  });

  it('应接受包含可选字段的简化工作流', () => {
    const minimalWorkflow = {
      version: '1.0.0',
      nodes: [
        {
          id: 'node-1',
          type: 'python',
          position: { x: 0, y: 0 },
          data: { label: '最小节点' },
        },
      ],
      edges: [],
    };

    const result = WorkflowSchema.safeParse(minimalWorkflow);

    expect(result.success).toBe(true);
  });

  it('应拒绝无效的枚举值', () => {
    const invalidTypeWorkflow = {
      version: '1.0.0',
      nodes: [
        {
          id: 'node-1',
          type: 'python',
          position: { x: 0, y: 0 },
          data: { 
            label: '测试',
            inputs: [
              {
                id: 'input-1',
                label: '输入',
                type: 'invalid_type' as any, // 无效的枚举值
              },
            ],
          },
        },
      ],
      edges: [],
    };

    const result = WorkflowSchema.safeParse(invalidTypeWorkflow);

    expect(result.success).toBe(false);
    if (result.success === false) {
      expect(result.error.errors).toBeDefined();
      expect(result.error.errors.length).toBeGreaterThan(0);
    }
  });
});
