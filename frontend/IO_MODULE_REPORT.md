# 导入导出模块实现报告 (F035-F038)

## 概述

已完成工作流导入导出功能的完整实现，包括 4 个 P0 功能点和 25 个测试用例。

## 功能实现

### F035 - 导出 JSON
**位置:** `src/io/export.ts`

**核心功能:**
- `getWorkflowData()` - 从 Zustand store 获取完整工作流数据
- `serializeWorkflow()` - 使用 Zod Schema 校验后序列化为 JSON
- `downloadFile()` - 创建 Blob 并触发浏览器下载
- `exportWorkflow()` - 完整导出流程（异步）
- `exportWorkflowAsString()` - 导出为字符串（用于分享链接）
- `getExportPreview()` - 获取导出预览数据

**技术要点:**
- 支持 pretty/compact 两种 JSON 格式
- 导出前自动进行 Schema 校验
- 自动生成文件名：`workflow-{timestamp}.json`

### F036 - 导入 JSON
**位置:** `src/io/import.ts`

**核心功能:**
- `readFile()` - 使用 FileReader 读取文件内容
- `parseJson()` - 解析 JSON 字符串，捕获解析错误
- `importWorkflowFromFile()` - 从文件导入完整流程
- `importWorkflowFromString()` - 从字符串导入（分享链接）
- `loadWorkflowToStore()` - 将数据加载到 Zustand store

**技术要点:**
- 文件类型检查（仅支持 .json）
- 分步处理：读取 → 解析 → 校验 → 加载
- 导入前清空现有数据

### F037 - 格式校验
**位置:** `src/io/schema.ts` + `src/io/import.ts`

**Schema 定义:**
- `WorkflowSchema` - 工作流完整结构
- `NodeDataSchema` - 节点数据结构
- `EdgeDataSchema` - 边数据结构
- `ViewportSchema` - 视口数据

**校验功能:**
- `validateWorkflow()` - 使用 Zod safeParse 进行校验
- `getFriendlyErrorMessage()` - 转换技术错误为友好提示
- 支持错误路径、错误码、错误消息

**校验规则:**
- version: 必需，string
- nodes: 必需，array
  - id: 必需，string
  - type: 必需，string
  - position: 必需，{x: number, y: number}
  - data: 必需，{label: string, ...}
- edges: 必需，array
  - id: 必需，string
  - source: 必需，string
  - target: 必需，string
- viewport: 可选，{x: number, y: number, zoom: number}
- metadata: 可选，{author?: string, tags?: string[]}

### F038 - 覆盖确认
**位置:** `src/components/ImportExportModal.tsx`

**导出对话框:**
- 显示节点数量、连接数量
- 显示前 5 个节点预览
- 空工作流警告提示
- 导出格式说明

**导入对话框:**
- 文件上传按钮（AntD Upload）
- 文件格式限制提示
- 覆盖警告提示
- 文件大小限制（10MB）

**UI 组件:**
- AntD Modal - 对话框
- AntD Upload - 文件上传
- AntD Button - 操作按钮
- AntD Tag - 标签展示
- AntD Typography - 文本展示
- AntD message - 消息提示

## 测试用例

### export.test.ts (4 个测试)
1. ✅ 应正确序列化包含节点和边的工作流
2. ✅ 应生成有效的 JSON 格式且可通过 Schema 校验
3. ✅ 应支持 pretty 和 compact 两种格式
4. ✅ 应创建并触发下载链接

### import.test.ts (6 个测试)
1. ✅ 应正确解析有效的 JSON 字符串
2. ✅ 应正确处理无效的 JSON 格式
3. ✅ 应解析包含特殊字符的 JSON 内容
4. ✅ 应成功导入完整的工作流数据
5. ✅ 应拒绝缺少必需字段的工作流
6. ✅ 应拒绝节点格式错误的工作流

### schema.test.ts (9 个测试)
1. ✅ 应通过包含完整字段的工作流校验
2. ✅ 应拒绝缺少必需字段的工作流并提供错误信息
3. ✅ 应拒绝节点位置格式错误的工作流
4. ✅ 应将技术错误转换为友好的提示信息
5. ✅ 应处理字符串类型的错误信息
6. ✅ 应区分节点错误和边错误
7. ✅ 应接受空节点和边数组
8. ✅ 应接受包含可选字段的简化工作流
9. ✅ 应拒绝节点 ID 类型错误的工作流

### confirm.test.tsx (6 个测试)
1. ✅ 应显示导出对话框并展示预览信息
2. ✅ 应显示导入对话框并提供文件上传选项
3. ✅ 应支持关闭对话框
4. ✅ 应显示覆盖警告提示
5. ✅ 应在空工作流时显示提示
6. ✅ 应显示文件格式限制

**总计:** 25 个测试，全部通过 ✅

## 文件结构

```
src/
├── io/
│   ├── index.ts           # 模块导出
│   ├── schema.ts          # Zod Schema 定义
│   ├── export.ts          # 导出功能实现
│   └── import.ts          # 导入功能实现
├── components/
│   └── ImportExportModal.tsx  # 导入导出对话框
└── test/io/
    ├── export.test.ts     # 导出测试
    ├── import.test.ts     # 导入测试
    ├── schema.test.ts     # Schema 校验测试
    └── confirm.test.tsx   # 对话框测试
```

## 依赖

- **zod@4.3.6** - Schema 校验库
- **antd@6.3.4** - UI 组件库
- **@ant-design/icons** - 图标库
- **zustand** - 状态管理

## Git 提交

```
commit e0abc7f
Author: ben
Date: Tue Mar 31 2026

feat: 实现工作流导入导出功能 (F035-F038)

- F035: 导出 JSON - 序列化工作流 + 文件下载
- F036: 导入 JSON - 文件上传 + 解析
- F037: 格式校验 - Schema 校验 + 错误提示
- F038: 覆盖确认 - 确认对话框
- 测试用例：25 个测试全部通过
```

## 使用示例

### 导出工作流

```typescript
import { exportWorkflow } from '@/io';

// 导出为 JSON 文件
const result = await exportWorkflow({ pretty: true });
if (result.success) {
  message.success('导出成功');
}

// 获取导出预览
const preview = getExportPreview();
console.log(`节点：${preview.nodeCount}, 连接：${preview.edgeCount}`);
```

### 导入工作流

```typescript
import { importWorkflowFromFile } from '@/io';

// 从文件导入
const file = fileInput.files[0];
const result = await importWorkflowFromFile(file);

if (result.success) {
  message.success('导入成功');
} else {
  const errorMsg = getFriendlyErrorMessage(result.errors || result.error!);
  message.error(errorMsg);
}
```

### 使用对话框组件

```typescript
import { ImportExportModal } from '@/components/ImportExportModal';

function App() {
  const [modalOpen, setModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<'import' | 'export'>('export');

  return (
    <>
      <Button onClick={() => { setModalMode('export'); setModalOpen(true); }}>
        导出
      </Button>
      <Button onClick={() => { setModalMode('import'); setModalOpen(true); }}>
        导入
      </Button>
      
      <ImportExportModal
        open={modalOpen}
        mode={modalMode}
        onClose={() => setModalOpen(false)}
        onImportSuccess={() => console.log('导入成功')}
        onExportSuccess={() => console.log('导出成功')}
      />
    </>
  );
}
```

## 注意事项

1. **Zod v4 API:** 使用 `error.issues` 而不是 `error.errors`
2. **Viewport 格式:** store 中使用 `{pan: {x, y}, zoom}`，导出使用 `{x, y, zoom}`
3. **文件限制:** 导入仅支持 .json 格式，最大 10MB
4. **覆盖行为:** 导入会清空当前工作流并替换为新数据
5. **浏览器兼容:** 下载功能依赖 Blob 和 URL.createObjectURL

## 后续优化

- [ ] 支持导出为图片/PDF（F041-F042）
- [ ] 支持分享链接（Base64 编码）
- [ ] 支持批量导出
- [ ] 支持版本历史
- [ ] 支持增量导入（合并而非覆盖）

---

**完成时间:** 2026-03-31  
**开发者:** 龙傲天 🐲  
**状态:** ✅ 已完成
