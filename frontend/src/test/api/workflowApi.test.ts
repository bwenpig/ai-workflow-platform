/**
 * workflowApi 测试
 * 覆盖率提升：API 层测试
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import {
  workflowApi,
  executionApi,
  executorApi,
} from '../../api/workflowApi';

const API_BASE_URL = 'http://localhost:8080/api/v1';

// Mock 数据
const mockWorkflows = [
  {
    id: 'wf-1',
    name: '测试工作流 1',
    description: '测试描述',
    nodes: [],
    edges: [],
    published: false,
  },
];

const mockWorkflow = {
  id: 'wf-1',
  name: '测试工作流',
  description: '测试描述',
  nodes: [],
  edges: [],
};

const mockExecution = {
  id: 'exec-1',
  workflowId: 'wf-1',
  status: 'RUNNING' as const,
};

const mockExecutors = [
  { id: 'exec-1', name: 'Executor 1' },
  { id: 'exec-2', name: 'Executor 2' },
];

// 设置 MSW server
const server = setupServer(
  // 获取工作流列表
  http.get(`${API_BASE_URL}/workflows`, () => {
    return HttpResponse.json(mockWorkflows);
  }),

  // 获取单个工流
  http.get(`${API_BASE_URL}/workflows/:id`, ({ params }) => {
    const { id } = params;
    if (id === 'wf-1') {
      return HttpResponse.json(mockWorkflow);
    }
    return new HttpResponse(null, { status: 404 });
  }),

  // 创建工作流
  http.post(`${API_BASE_URL}/workflows`, async ({ request }) => {
    const body = await request.json();
    return HttpResponse.json({ ...mockWorkflow, ...body }, { status: 201 });
  }),

  // 更新工作流
  http.put(`${API_BASE_URL}/workflows/:id`, async ({ params, request }) => {
    const { id } = params;
    const body = await request.json();
    return HttpResponse.json({ ...mockWorkflow, id, ...body });
  }),

  // 删除工作流
  http.delete(`${API_BASE_URL}/workflows/:id`, ({ params }) => {
    const { id } = params;
    if (id === 'wf-1') {
      return new HttpResponse(null, { status: 204 });
    }
    return new HttpResponse(null, { status: 404 });
  }),

  // 执行工作流
  http.post(`${API_BASE_URL}/workflows/:id/execute`, ({ params }) => {
    const { id } = params;
    return HttpResponse.json({ ...mockExecution, workflowId: id as string }, { status: 202 });
  }),

  // 获取执行状态
  http.get(`${API_BASE_URL}/executions/:id`, ({ params }) => {
    const { id } = params;
    if (id === 'exec-1') {
      return HttpResponse.json(mockExecution);
    }
    return new HttpResponse(null, { status: 404 });
  }),

  // 取消执行
  http.post(`${API_BASE_URL}/executions/:id/cancel`, ({ params }) => {
    const { id } = params;
    if (id === 'exec-1') {
      return new HttpResponse(null, { status: 204 });
    }
    return new HttpResponse(null, { status: 404 });
  }),

  // 获取执行器列表
  http.get(`${API_BASE_URL}/executors`, () => {
    return HttpResponse.json(mockExecutors);
  }),
);

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('workflowApi.listWorkflows', () => {
  it('应获取工作流列表', async () => {
    const result = await workflowApi.listWorkflows();
    expect(result).toEqual(mockWorkflows);
    expect(result.length).toBe(1);
  });
});

describe('workflowApi.getWorkflow', () => {
  it('应获取单个工流', async () => {
    const result = await workflowApi.getWorkflow('wf-1');
    expect(result).toEqual(mockWorkflow);
    expect(result.id).toBe('wf-1');
  });

  it('应在工流不存在时抛出错误', async () => {
    await expect(workflowApi.getWorkflow('invalid-id'))
      .rejects.toThrow('资源不存在');
  });
});

describe('workflowApi.createWorkflow', () => {
  it('应创建工流', async () => {
    const newWorkflow = {
      name: '新工流',
      description: '新描述',
      nodes: [],
      edges: [],
    };

    const result = await workflowApi.createWorkflow(newWorkflow);
    expect(result.name).toBe('新工流');
  });
});

describe('workflowApi.updateWorkflow', () => {
  it('应更新工流', async () => {
    const updated = { name: '更新后的名称' };
    const result = await workflowApi.updateWorkflow('wf-1', updated);
    expect(result.name).toBe('更新后的名称');
  });
});

describe('workflowApi.deleteWorkflow', () => {
  it('应删除工流', async () => {
    await expect(workflowApi.deleteWorkflow('wf-1')).resolves.not.toThrow();
  });
});

describe('workflowApi.executeWorkflow', () => {
  it('应执行工流', async () => {
    const result = await workflowApi.executeWorkflow('wf-1');
    expect(result.workflowId).toBe('wf-1');
  });

  it('应支持带输入的执行', async () => {
    const result = await workflowApi.executeWorkflow('wf-1', { input: 'test' });
    expect(result.workflowId).toBe('wf-1');
  });
});

describe('executionApi.getExecutionStatus', () => {
  it('应获取执行状态', async () => {
    const result = await executionApi.getExecutionStatus('exec-1');
    expect(result.status).toBe('RUNNING');
  });
});

describe('executionApi.cancelExecution', () => {
  it('应取消执行', async () => {
    await expect(executionApi.cancelExecution('exec-1')).resolves.not.toThrow();
  });
});

describe('executorApi.listExecutors', () => {
  it('应获取执行器列表', async () => {
    const result = await executorApi.listExecutors();
    expect(result).toEqual(mockExecutors);
    expect(result.length).toBe(2);
  });
});
