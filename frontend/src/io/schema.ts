/**
 * 工作流导入导出 Schema 定义
 * 使用 Zod 进行数据校验
 */

import { z } from 'zod';

/**
 * 节点数据 Schema
 */
export const NodeDataSchema = z.object({
  id: z.string(),
  type: z.string(),
  position: z.object({
    x: z.number(),
    y: z.number(),
  }),
  data: z.object({
    label: z.string(),
    script: z.string().optional(),
    timeout: z.number().optional(),
    requirements: z.array(z.string()).optional(),
    pythonVersion: z.string().optional(),
    inputs: z.array(z.object({
      id: z.string(),
      label: z.string(),
      type: z.enum(['any', 'text', 'image', 'video', 'audio', 'json']),
    })).optional(),
    outputs: z.array(z.object({
      id: z.string(),
      label: z.string(),
      type: z.enum(['any', 'text', 'image', 'video', 'audio', 'json']),
    })).optional(),
  }).passthrough(),
  selected: z.boolean().optional(),
  zIndex: z.number().optional(),
}).passthrough();

/**
 * 边数据 Schema
 */
export const EdgeDataSchema = z.object({
  id: z.string(),
  source: z.string(),
  target: z.string(),
  sourceHandle: z.string().nullable().optional(),
  targetHandle: z.string().nullable().optional(),
  type: z.string().optional(),
  label: z.string().optional(),
  selected: z.boolean().optional(),
  zIndex: z.number().optional(),
}).passthrough();

/**
 * 视口 Schema
 */
export const ViewportSchema = z.object({
  x: z.number(),
  y: z.number(),
  zoom: z.number(),
});

/**
 * 工作流完整 Schema
 */
export const WorkflowSchema = z.object({
  version: z.string(),
  name: z.string().optional(),
  description: z.string().optional(),
  createdAt: z.string().datetime().optional(),
  updatedAt: z.string().datetime().optional(),
  nodes: z.array(NodeDataSchema),
  edges: z.array(EdgeDataSchema),
  viewport: ViewportSchema.optional(),
  metadata: z.object({
    author: z.string().optional(),
    tags: z.array(z.string()).optional(),
  }).optional(),
});

/**
 * 导出类型
 */
export type WorkflowData = z.infer<typeof WorkflowSchema>;
export type NodeData = z.infer<typeof NodeDataSchema>;
export type EdgeData = z.infer<typeof EdgeDataSchema>;

/**
 * 校验错误类型
 */
export interface ValidationError {
  path: string;
  message: string;
  code: string;
}

/**
 * 校验结果类型
 */
export interface ValidationResult {
  success: boolean;
  errors: ValidationError[];
  data?: WorkflowData;
}
