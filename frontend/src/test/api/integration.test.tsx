import { describe, it, expect, beforeEach, vi } from 'vitest';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import { 
  useWorkflows, 
  useWorkflow, 
  useCreateWorkflow, 
  useUpdateWorkflow, 
  useDeleteWorkflow,
  useExecuteWorkflow,
  useExecutionStatus,
  useExecutors,
} from '../../hooks/useWorkflowApi';
import type { Workflow, WorkflowExecution } from '../../api/workflowApi';

// ==================== Mock 数据 ====================

const mockWorkflows: Workflow[] = [
  {
    id: 'wf-1',
    name: '测试工作流 1',
    description: '测试描述 1',
    nodes: [
      { nodeId: 'node-1', type: 'INPUT', position: { x: 0, y: 0 }, config: {} },
    ],
    edges: [],
    published: false,
    createdBy: 'ben-test',
    createdAt: '2026-03-31T00:00:00Z',
    updatedAt: '2026-03-31T00:00:00Z',
  },
  {
    id: 'wf-2',
    name: '测试工作流 2',
    description: '测试描述 2',
    nodes: [
      { nodeId: 'node-1', type: 'INPUT', position: { x: 0, y: 0 }, config: {} },
    ],
    edges: [],
    published: true,
    createdBy: 'ben-test',
    createdAt: '2026-03-31T00:00:00Z',
    updatedAt: '2026-03-31T00:00:00Z',
  },
];

const mockExecution: WorkflowExecution = {
  id: 'exec-1',
  workflowId: 'wf-1',
  status: 'SUCCESS',
  nodeStates: {
    'node-1': {
      status: 'SUCCESS',
      result: { output: '测试输出' },
      startedAt: '2026-03-31T00:00:00Z',
      completedAt: '2026-03-31T00:00:01Z',
    },
  },
  startedAt: '2026-03-31T00:00:00Z',
  completedAt: '2026-03-31T00:00:01Z',
};

// ==================== MSW Server ====================

const server = setupServer(
  // GET /api/v1/workflows
  http.get('http://localhost:8080/api/v1/workflows', ({ request }) => {
    const url = new URL(request.url);
    const published = url.searchParams.get('published');
    
    let workflows = mockWorkflows;
    if (published === 'true') {
      workflows = mockWorkflows.filter(w => w.published);
    }
    
    return HttpResponse.json(workflows);
  }),

  // GET /api/v1/workflows/:id
  http.get('http://localhost:8080/api/v1/workflows/:id', ({ params }) => {
    const { id } = params;
    const workflow = mockWorkflows.find(w => w.id === id);
    
    if (!workflow) {
      return new HttpResponse(null, { status: 404 });
    }
    
    return HttpResponse.json(workflow);
  }),

  // POST /api/v1/workflows
  http.post('http://localhost:8080/api/v1/workflows', async ({ request }) => {
    const body = await request.json() as Partial<Workflow>;
    const newWorkflow: Workflow = {
      ...body,
      id: `wf-${Date.now()}`,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      createdBy: request.headers.get('X-User-Id') || 'anonymous',
    } as Workflow;
    
    return HttpResponse.json(newWorkflow, { status: 200 });
  }),

  // PUT /api/v1/workflows/:id
  http.put('http://localhost:8080/api/v1/workflows/:id', async ({ params, request }) => {
    const { id } = params;
    const body = await request.json() as Partial<Workflow>;
    const workflow = mockWorkflows.find(w => w.id === id);
    
    if (!workflow) {
      return new HttpResponse(null, { status: 404 });
    }
    
    const updatedWorkflow: Workflow = {
      ...workflow,
      ...body,
      updatedAt: new Date().toISOString(),
    };
    
    return HttpResponse.json(updatedWorkflow);
  }),

  // DELETE /api/v1/workflows/:id
  http.delete('http://localhost:8080/api/v1/workflows/:id', ({ params }) => {
    const { id } = params;
    const workflow = mockWorkflows.find(w => w.id === id);
    
    if (!workflow) {
      return new HttpResponse(null, { status: 404 });
    }
    
    return new HttpResponse(null, { status: 204 });
  }),

  // POST /api/v1/workflows/:id/execute
  http.post('http://localhost:8080/api/v1/workflows/:id/execute', ({ params }) => {
    const { id } = params;
    
    return HttpResponse.json({
      executionId: `exec-${Date.now()}`,
      status: 'RUNNING',
      workflowId: id,
    });
  }),

  // GET /api/v1/executions/:id
  http.get('http://localhost:8080/api/v1/executions/:id', ({ params }) => {
    const { id } = params;
    
    return HttpResponse.json({
      ...mockExecution,
      id,
    });
  }),

  // GET /api/v1/executors
  http.get('http://localhost:8080/api/v1/executors', () => {
    return HttpResponse.json([
      { id: 'executor-1', name: 'Python Executor', type: 'PYTHON', status: 'ACTIVE' },
      { id: 'executor-2', name: 'Node.js Executor', type: 'NODEJS', status: 'ACTIVE' },
    ]);
  }),
);

// ==================== 测试辅助函数 ====================

const createWrapper = () => {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });
  
  return ({ children }: { children: React.ReactNode }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
};

// ==================== 测试用例 ====================

describe('API 集成测试', () => {
  beforeAll(() => server.listen());
  afterEach(() => server.resetHandlers());
  afterAll(() => server.close());

  describe('工作流列表 API', () => {
    it('应该正确获取工作流列表', async () => {
      const { result } = renderHook(() => useWorkflows(), { wrapper: createWrapper() });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(result.current.data).toHaveLength(2);
      expect(result.current.data?.[0].name).toBe('测试工作流 1');
    });

    it('应该支持按发布状态筛选', async () => {
      const { result } = renderHook(() => useWorkflows({ published: true }), { 
        wrapper: createWrapper() 
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(result.current.data).toHaveLength(1);
      expect(result.current.data?.[0].published).toBe(true);
    });

    it('应该处理加载状态', () => {
      const { result } = renderHook(() => useWorkflows(), { wrapper: createWrapper() });

      expect(result.current.isLoading).toBe(true);
    });
  });

  describe('工作流详情 API', () => {
    it('应该正确获取工作流详情', async () => {
      const { result } = renderHook(() => useWorkflow('wf-1'), { wrapper: createWrapper() });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(result.current.data?.id).toBe('wf-1');
      expect(result.current.data?.name).toBe('测试工作流 1');
    });

    it('应该处理 404 错误', async () => {
      const { result } = renderHook(() => useWorkflow('non-existent'), { 
        wrapper: createWrapper() 
      });

      await waitFor(() => expect(result.current.isError).toBe(true));

      expect(result.current.error).toBeDefined();
    });
  });

  describe('创建工作流 API', () => {
    it('应该成功创建工作流', async () => {
      const { result } = renderHook(() => useCreateWorkflow(), { wrapper: createWrapper() });

      const newWorkflow = {
        name: '新工作流',
        description: '新描述',
        nodes: [],
        edges: [],
      };

      result.current.mutate(newWorkflow);

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(result.current.data?.name).toBe('新工作流');
      expect(result.current.data?.id).toBeDefined();
    });
  });

  describe('更新工作流 API', () => {
    it('应该成功更新工作流', async () => {
      const { result } = renderHook(() => useUpdateWorkflow(), { wrapper: createWrapper() });

      result.current.mutate({
        id: 'wf-1',
        workflow: { name: '更新后的名称' },
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(result.current.data?.name).toBe('更新后的名称');
    });
  });

  describe('删除工作流 API', () => {
    it('应该成功删除工作流', async () => {
      const { result } = renderHook(() => useDeleteWorkflow(), { wrapper: createWrapper() });

      result.current.mutate('wf-1');

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
    });
  });

  describe('执行工作流 API', () => {
    it('应该成功执行工作流', async () => {
      const { result } = renderHook(() => useExecuteWorkflow(), { wrapper: createWrapper() });

      result.current.mutate({ id: 'wf-1', inputs: { test: 'input' } });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(result.current.data?.executionId).toBeDefined();
      expect(result.current.data?.status).toBe('RUNNING');
    });
  });

  describe('执行状态 API（轮询）', () => {
    it('应该正确获取执行状态', async () => {
      const { result } = renderHook(() => useExecutionStatus('exec-1'), { 
        wrapper: createWrapper() 
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(result.current.data?.status).toBe('SUCCESS');
      expect(result.current.data?.nodeStates).toBeDefined();
    });

    it('应该支持轮询（3 秒间隔）', async () => {
      const { result } = renderHook(() => useExecutionStatus('exec-1', { refetchInterval: 3000 }), { 
        wrapper: createWrapper() 
      });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));
      
      // 验证初始获取
      expect(result.current.data?.id).toBe('exec-1');
    });
  });

  describe('执行器列表 API', () => {
    it('应该正确获取执行器列表', async () => {
      const { result } = renderHook(() => useExecutors(), { wrapper: createWrapper() });

      await waitFor(() => expect(result.current.isSuccess).toBe(true));

      expect(result.current.data).toHaveLength(2);
      expect(result.current.data?.[0].name).toBe('Python Executor');
    });
  });
});
