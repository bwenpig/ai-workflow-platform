/**
 * 工作流导出功能
 * F035: 导出 JSON - 序列化工作流 + 文件下载
 */

import { WorkflowSchema, type WorkflowData } from './schema';
import { useNodeStore } from '../store/nodeStore';
import { useEdgeStore } from '../store/edgeStore';
import { useViewportStore } from '../store/viewportStore';

/**
 * 导出选项
 */
export interface ExportOptions {
  includeViewport?: boolean;
  includeMetadata?: boolean;
  pretty?: boolean;
}

/**
 * 从 store 获取当前工作流数据
 */
export function getWorkflowData(): WorkflowData {
  const nodes = useNodeStore.getState().nodes;
  const edges = useEdgeStore.getState().edges;
  const viewport = useViewportStore.getState();

  return {
    version: '1.0.0',
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
    nodes: nodes.map(node => ({
      id: node.id,
      type: node.type,
      position: {
        x: node.position.x,
        y: node.position.y,
      },
      data: node.data as any,
      selected: node.selected,
      zIndex: node.zIndex,
    })),
    edges: edges.map(edge => ({
      id: edge.id,
      source: edge.source,
      target: edge.target,
      sourceHandle: edge.sourceHandle || null,
      targetHandle: edge.targetHandle || null,
      type: edge.type,
      label: edge.label,
      selected: edge.selected,
      zIndex: edge.zIndex,
    })),
    viewport: {
      x: viewport.pan.x,
      y: viewport.pan.y,
      zoom: viewport.zoom,
    },
    metadata: {
      author: 'Anonymous',
      tags: [],
    },
  };
}

/**
 * 序列化工作流为 JSON 字符串
 */
export function serializeWorkflow(data: WorkflowData, options: ExportOptions = {}): string {
  const { pretty = true } = options;
  
  // 校验数据
  const result = WorkflowSchema.safeParse(data);
  if (!result.success) {
    throw new Error('工作流数据校验失败');
  }

  return JSON.stringify(result.data, null, pretty ? 2 : undefined);
}

/**
 * 触发浏览器下载
 */
export function downloadFile(content: string, filename: string, mimeType: string = 'application/json') {
  const blob = new Blob([content], { type: mimeType });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  
  link.href = url;
  link.download = filename;
  link.style.display = 'none';
  
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  
  // 清理 URL
  setTimeout(() => {
    URL.revokeObjectURL(url);
  }, 100);
}

/**
 * 导出工作流为 JSON 文件
 * F035 核心功能
 */
export async function exportWorkflow(options: ExportOptions = {}): Promise<{ success: boolean; error?: string }> {
  try {
    const workflowData = getWorkflowData();
    const jsonContent = serializeWorkflow(workflowData, options);
    const filename = `workflow-${Date.now()}.json`;
    
    downloadFile(jsonContent, filename);
    
    return { success: true };
  } catch (error) {
    console.error('导出工作流失败:', error);
    return {
      success: false,
      error: error instanceof Error ? error.message : '导出失败',
    };
  }
}

/**
 * 导出工作流为字符串（用于分享链接等）
 */
export function exportWorkflowAsString(options: ExportOptions = {}): string {
  const workflowData = getWorkflowData();
  return serializeWorkflow(workflowData, options);
}

/**
 * 导出预览数据（用于显示导出前的预览）
 */
export function getExportPreview() {
  const nodes = useNodeStore.getState().nodes;
  const edges = useEdgeStore.getState().edges;
  
  return {
    nodeCount: nodes.length,
    edgeCount: edges.length,
    nodes: nodes.map(n => ({ id: n.id, type: n.type, label: n.data.label })),
    edges: edges.map(e => ({ id: e.id, source: e.source, target: e.target })),
  };
}
