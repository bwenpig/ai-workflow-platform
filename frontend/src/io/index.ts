/**
 * IO 模块 - 工作流导入导出
 * 
 * 功能:
 * - F035: 导出 JSON - 序列化工作流 + 文件下载
 * - F036: 导入 JSON - 文件上传 + 解析
 * - F037: 格式校验 - Schema 校验 + 错误提示
 * - F038: 覆盖确认 - 确认对话框
 */

// Schema 定义
export {
  WorkflowSchema,
  NodeDataSchema,
  EdgeDataSchema,
  ViewportSchema,
  type WorkflowData,
  type NodeData,
  type EdgeData,
  type ValidationError,
  type ValidationResult,
} from './schema';

// 导出功能
export {
  getWorkflowData,
  serializeWorkflow,
  downloadFile,
  exportWorkflow,
  exportWorkflowAsString,
  getExportPreview,
  type ExportOptions,
} from './export';

// 导入功能
export {
  parseJson,
  validateWorkflow,
  readFile,
  loadWorkflowToStore,
  importWorkflowFromFile,
  importWorkflowFromString,
  getFriendlyErrorMessage,
  type ImportResult,
} from './import';
