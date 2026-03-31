import axios, { AxiosError, AxiosRequestConfig } from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/v1';

// 创建 axios 实例
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器 - 添加用户 ID
apiClient.interceptors.request.use(
  (config) => {
    const userId = localStorage.getItem('userId') || 'anonymous';
    if (userId) {
      config.headers['X-User-Id'] = userId;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器 - 统一错误处理
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    console.error('API 错误:', error.response?.status, error.message);
    
    // 统一错误处理
    if (error.response?.status === 404) {
      throw new Error('资源不存在');
    } else if (error.response?.status === 400) {
      throw new Error('请求参数错误');
    } else if (error.response?.status === 500) {
      throw new Error('服务器内部错误');
    } else if (error.response?.status === 503) {
      throw new Error('服务暂时不可用');
    }
    
    throw error;
  }
);

// ==================== 类型定义 ====================

export interface Workflow {
  id?: string;
  name: string;
  description: string;
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
  published?: boolean;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface WorkflowNode {
  nodeId: string;
  type: string;
  position: { x: number; y: number };
  config?: Record<string, any>;
  modelProvider?: string;
}

export interface WorkflowEdge {
  id: string;
  source: string;
  target: string;
  dataType?: string;
}

export interface WorkflowExecution {
  id: string;
  workflowId: string;
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'CANCELLED';
  nodeStates?: Record<string, NodeState>;
  inputs?: Record<string, any>;
  outputs?: Record<string, any>;
  startedAt?: string;
  completedAt?: string;
  error?: string;
}

export interface NodeState {
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED';
  result?: any;
  error?: string;
  startedAt?: string;
  completedAt?: string;
}

export interface Executor {
  id: string;
  name: string;
  type: string;
  status: 'ACTIVE' | 'INACTIVE';
}

// ==================== API 接口 ====================

export const workflowApi = {
  /**
   * 获取工作流列表
   */
  listWorkflows: async (params?: { published?: boolean; createdBy?: string }): Promise<Workflow[]> => {
    const response = await apiClient.get<Workflow[]>('/workflows', { params });
    return response.data;
  },

  /**
   * 创建工作流
   */
  createWorkflow: async (workflow: Omit<Workflow, 'id' | 'createdAt' | 'updatedAt'>): Promise<Workflow> => {
    const response = await apiClient.post<Workflow>('/workflows', workflow);
    return response.data;
  },

  /**
   * 获取工作流详情
   */
  getWorkflow: async (id: string): Promise<Workflow> => {
    const response = await apiClient.get<Workflow>(`/workflows/${id}`);
    return response.data;
  },

  /**
   * 更新工作流
   */
  updateWorkflow: async (id: string, workflow: Partial<Workflow>): Promise<Workflow> => {
    const response = await apiClient.put<Workflow>(`/workflows/${id}`, workflow);
    return response.data;
  },

  /**
   * 删除工作流
   */
  deleteWorkflow: async (id: string): Promise<void> => {
    await apiClient.delete(`/workflows/${id}`);
  },

  /**
   * 执行工作流
   */
  executeWorkflow: async (id: string, inputs?: Record<string, any>): Promise<{ executionId: string; status: string; workflowId: string }> => {
    const response = await apiClient.post(`/workflows/${id}/execute`, inputs || {});
    return response.data;
  },

  /**
   * 切换发布状态
   */
  togglePublished: async (id: string): Promise<Workflow> => {
    const response = await apiClient.post(`/workflows/${id}/publish`);
    return response.data;
  },
};

export const executionApi = {
  /**
   * 获取执行状态
   */
  getExecutionStatus: async (id: string): Promise<WorkflowExecution> => {
    const response = await apiClient.get<WorkflowExecution>(`/executions/${id}`);
    return response.data;
  },

  /**
   * 取消执行
   */
  cancelExecution: async (id: string): Promise<WorkflowExecution> => {
    const response = await apiClient.post(`/executions/${id}/cancel`);
    return response.data;
  },

  /**
   * 获取执行历史
   */
  getExecutionHistory: async (userId: string, limit: number = 20): Promise<WorkflowExecution[]> => {
    const response = await apiClient.get<WorkflowExecution[]>('/executions/history', {
      params: { userId, limit },
    });
    return response.data;
  },
};

export const executorApi = {
  /**
   * 获取执行器列表
   */
  listExecutors: async (): Promise<Executor[]> => {
    const response = await apiClient.get<Executor[]>('/executors');
    return response.data;
  },
};

export default apiClient;
