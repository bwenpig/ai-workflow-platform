import apiClient from './workflowApi';

// ==================== 类型定义 ====================

export interface SchedulerJob {
  id: string;
  workflowId: string;
  workflowName: string;
  cronExpression: string;
  status: 'RUNNING' | 'PAUSED' | 'COMPLETED';
  nextFireTime: string | null;
  lastFireTime: string | null;
  lastExecutionId: string | null;
  description: string | null;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateJobRequest {
  workflowId: string;
  cronExpression: string;
  description?: string;
}

// ==================== API ====================

export const schedulerApi = {
  /** 获取所有定时任务 */
  listJobs: async (): Promise<SchedulerJob[]> => {
    const response = await apiClient.get<SchedulerJob[]>('/scheduler/jobs');
    return response.data;
  },

  /** 获取单个定时任务 */
  getJob: async (id: string): Promise<SchedulerJob> => {
    const response = await apiClient.get<SchedulerJob>(`/scheduler/jobs/${id}`);
    return response.data;
  },

  /** 创建定时任务 */
  createJob: async (request: CreateJobRequest): Promise<SchedulerJob> => {
    const response = await apiClient.post<SchedulerJob>('/scheduler/jobs', request);
    return response.data;
  },

  /** 暂停定时任务 */
  pauseJob: async (id: string): Promise<SchedulerJob> => {
    const response = await apiClient.post<SchedulerJob>(`/scheduler/jobs/${id}/pause`);
    return response.data;
  },

  /** 恢复定时任务 */
  resumeJob: async (id: string): Promise<SchedulerJob> => {
    const response = await apiClient.post<SchedulerJob>(`/scheduler/jobs/${id}/resume`);
    return response.data;
  },

  /** 删除定时任务 */
  deleteJob: async (id: string): Promise<void> => {
    await apiClient.delete(`/scheduler/jobs/${id}`);
  },

  /** 更新 cron 表达式 */
  updateCron: async (id: string, cronExpression: string): Promise<SchedulerJob> => {
    const response = await apiClient.put<SchedulerJob>(`/scheduler/jobs/${id}/cron`, { cronExpression });
    return response.data;
  },
};
