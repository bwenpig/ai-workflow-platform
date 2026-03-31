/**
 * 工作流导入功能
 * F036: 导入 JSON - 文件上传 + 解析
 * F037: 格式校验 - Schema 校验 + 错误提示
 */

import { WorkflowSchema, type WorkflowData, type ValidationResult, type ValidationError } from './schema';
import { useNodeStore } from '../store/nodeStore';
import { useEdgeStore } from '../store/edgeStore';
import { useViewportStore } from '../store/viewportStore';

/**
 * 导入结果类型
 */
export interface ImportResult {
  success: boolean;
  error?: string;
  errors?: ValidationError[];
  data?: WorkflowData;
}

/**
 * 解析 JSON 字符串
 */
export function parseJson(content: string): { success: boolean; data?: any; error?: string } {
  try {
    const data = JSON.parse(content);
    return { success: true, data };
  } catch (error) {
    return {
      success: false,
      error: error instanceof Error ? error.message : 'JSON 解析失败',
    };
  }
}

/**
 * 校验工作流数据
 * F037 核心功能
 */
export function validateWorkflow(data: any): ValidationResult {
  const result = WorkflowSchema.safeParse(data);
  
  if (result.success) {
    return {
      success: true,
      errors: [],
      data: result.data,
    };
  }
  
  // 转换 Zod 错误为自定义错误格式 (Zod v4 使用 issues 而不是 errors)
  const errors: ValidationError[] = (result.error.issues || []).map(err => ({
    path: err.path.join('.'),
    message: err.message,
    code: err.code as string,
  }));
  
  return {
    success: false,
    errors,
  };
}

/**
 * 读取文件内容
 */
export function readFile(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    
    reader.onload = (event) => {
      const content = event.target?.result;
      if (typeof content === 'string') {
        resolve(content);
      } else {
        reject(new Error('文件读取失败'));
      }
    };
    
    reader.onerror = () => {
      reject(new Error('文件读取失败'));
    };
    
    reader.readAsText(file);
  });
}

/**
 * 将工作流数据加载到 store
 */
export function loadWorkflowToStore(data: WorkflowData) {
  const nodeStore = useNodeStore.getState();
  const edgeStore = useEdgeStore.getState();
  const viewportStore = useViewportStore.getState();
  
  // 清空现有数据
  nodeStore.nodes.forEach(node => nodeStore.removeNode(node.id));
  edgeStore.edges.forEach(edge => edgeStore.removeEdge(edge.id));
  
  // 加载节点
  data.nodes.forEach(node => {
    nodeStore.addNode({
      id: node.id,
      type: node.type,
      position: node.position,
      data: node.data,
      selected: node.selected,
      zIndex: node.zIndex,
    });
  });
  
  // 加载边
  data.edges.forEach(edge => {
    edgeStore.addEdge({
      id: edge.id,
      source: edge.source,
      target: edge.target,
      sourceHandle: edge.sourceHandle || undefined,
      targetHandle: edge.targetHandle || undefined,
      type: edge.type,
      label: edge.label,
      selected: edge.selected,
      zIndex: edge.zIndex,
    });
  });
  
  // 加载视口
  if (data.viewport) {
    viewportStore.updateViewport({
      zoom: data.viewport.zoom,
      pan: { x: data.viewport.x, y: data.viewport.y },
    });
  }
}

/**
 * 从文件导入工作流
 * F036 核心功能
 */
export async function importWorkflowFromFile(file: File): Promise<ImportResult> {
  try {
    // 检查文件类型
    if (!file.name.endsWith('.json')) {
      return {
        success: false,
        error: '仅支持 JSON 格式文件',
      };
    }
    
    // 读取文件
    const content = await readFile(file);
    
    // 解析 JSON
    const parseResult = parseJson(content);
    if (!parseResult.success) {
      return {
        success: false,
        error: parseResult.error,
      };
    }
    
    // 校验数据
    const validationResult = validateWorkflow(parseResult.data);
    if (!validationResult.success) {
      return {
        success: false,
        errors: validationResult.errors,
        error: `格式校验失败：${validationResult.errors.map(e => e.message).join(', ')}`,
      };
    }
    
    // 加载到 store
    loadWorkflowToStore(validationResult.data!);
    
    return {
      success: true,
      data: validationResult.data,
    };
  } catch (error) {
    console.error('导入工作流失败:', error);
    return {
      success: false,
      error: error instanceof Error ? error.message : '导入失败',
    };
  }
}

/**
 * 从字符串导入工作流（用于分享链接）
 */
export async function importWorkflowFromString(content: string): Promise<ImportResult> {
  try {
    // 解析 JSON
    const parseResult = parseJson(content);
    if (!parseResult.success) {
      return {
        success: false,
        error: parseResult.error,
      };
    }
    
    // 校验数据
    const validationResult = validateWorkflow(parseResult.data);
    if (!validationResult.success) {
      return {
        success: false,
        errors: validationResult.errors,
        error: `格式校验失败：${validationResult.errors.map(e => e.message).join(', ')}`,
      };
    }
    
    // 加载到 store
    loadWorkflowToStore(validationResult.data!);
    
    return {
      success: true,
      data: validationResult.data,
    };
  } catch (error) {
    console.error('导入工作流失败:', error);
    return {
      success: false,
      error: error instanceof Error ? error.message : '导入失败',
    };
  }
}

/**
 * 获取友好的错误提示信息
 */
export function getFriendlyErrorMessage(error: string | ValidationError[]): string {
  if (typeof error === 'string') {
    return error;
  }
  
  if (Array.isArray(error)) {
    const messages = error.map(e => {
      if (e.path.includes('nodes')) {
        return `节点错误：${e.message}`;
      }
      if (e.path.includes('edges')) {
        return `连接错误：${e.message}`;
      }
      return e.message;
    });
    return messages.join('; ');
  }
  
  return '未知错误';
}
