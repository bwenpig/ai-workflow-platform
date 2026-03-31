# 架构对比：当前 vs 重构后

## 当前架构（自研引擎）

```
┌─────────────────────────────────────────────────────────────────┐
│                        ai-workflow                               │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    DagWorkflowEngine                       │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │  executeNode() - switch(nodeType)                    │  │  │
│  │  │    case "INPUT":     → 直接返回输入                   │  │  │
│  │  │    case "MODEL":     → mockModelResult() (硬编码)    │  │  │
│  │  │    case "PROCESS":   → processInput()                │  │  │
│  │  │    case "PYTHON_SCRIPT": → pythonExecutor.execute()  │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  │                                                            │  │
│  │  ┌────────────────┐    ┌────────────────┐                 │  │
│  │  │PythonScript    │    │ModelProvider   │                 │  │
│  │  │Executor        │    │(接口，无实现)   │                 │  │
│  │  │(内联脚本执行)   │    │                │                 │  │
│  │  └────────────────┘    └────────────────┘                 │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

问题：
❌ 模型执行逻辑硬编码在 mockModelResult() 中
❌ 新增模型需要修改 DagWorkflowEngine 的 switch 语句
❌ PythonScriptExecutor 与引擎强耦合
❌ 无统一的执行器接口标准
❌ 无法运行时动态扩展
```

## 重构后架构（深度集成 dag-scheduler）

```
┌─────────────────────────────────────────────────────────────────┐
│                        ai-workflow                               │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    DagWorkflowEngine                       │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │  executeNodeWithSPI()                                │  │  │
│  │  │    1. executorRegistry.getExecutor(nodeType)         │  │  │
│  │  │    2. executor.execute(context)                      │  │  │
│  │  │    3. return result.getOutputs()                     │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  │                            ↓ 依赖                           │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │              ExecutorRegistry (SPI 注册中心)           │  │  │
│  │  │  Map<String, NodeExecutor> executors                 │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                                ↑ 使用
                                │
┌───────────────────────────────┼───────────────────────────────┐
│                    dag-scheduler (SPI 框架)                    │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │                   NodeExecutor 接口                      │  │
│  │  • getType() / getName() / getDescription()             │  │
│  │  • execute(NodeExecutionContext)                        │  │
│  │  • init() / destroy() / isAvailable()                   │  │
│  └─────────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │                   执行器实现                              │  │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐        │  │
│  │  │PythonScript │ │KlingExecutor│ │WanExecutor  │        │  │
│  │  │Executor     │ │             │ │             │        │  │
│  │  │(类型:python │ │(类型:kling) │ │(类型:wan)   │        │  │
│  │  │_script)     │ │             │ │             │        │  │
│  │  └─────────────┘ └─────────────┘ └─────────────┘        │  │
│  │  ┌─────────────┐ ┌─────────────┐                        │  │
│  │  │Seedance     │ │NanoBanana   │                        │  │
│  │  │Executor     │ │Executor     │                        │  │
│  │  │(类型:seedance)│(类型:nanobanana)                      │  │
│  │  └─────────────┘ └─────────────┘                        │  │
│  └─────────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │  @NodeComponent 自动发现  │  REST API  │  配置管理       │  │
│  └─────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘

优势：
✅ 统一 SPI 接口标准
✅ 新增模型无需修改引擎代码
✅ 执行器可独立开发、测试、部署
✅ 支持运行时动态注册/注销
✅ 自动获得 REST API 管理能力
✅ 复用 dag-scheduler 高测试覆盖率
```

## 代码对比

### 当前：添加新模型需要修改引擎

```java
// DagWorkflowEngine.java
private Object mockModelResult(WorkflowNode node) {
    String modelProvider = node.getModelProvider();
    
    switch (modelProvider != null ? modelProvider : "") {
        case "kling":
            // 硬编码 Kling 逻辑
            result.put("url", "https://...");
            break;
        case "wan":
            // 硬编码 Wan 逻辑
            result.put("url", "https://...");
            break;
        // ❌ 新增模型需要修改这里
        case "new_model":
            result.put("url", "https://...");
            break;
        default:
            result.put("url", "https://placeholder.com");
    }
    return result;
}
```

### 重构后：添加新模型只需实现 SPI

```java
// NewModelExecutor.java（新文件，无需修改现有代码）
@Component
@NodeComponent(
    value = "new_model", 
    name = "New Model Executor",
    description = "新模型执行器"
)
public class NewModelExecutor implements NodeExecutor {
    
    @Override
    public String getType() {
        return "new_model";
    }
    
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        // 实现新模型逻辑
        // ✅ 无需修改 DagWorkflowEngine
        // ✅ 无需修改现有执行器
        // ✅ 自动被 ExecutorRegistry 发现
    }
}
```

## 扩展性对比

| 场景 | 当前架构 | 重构后架构 |
|------|----------|------------|
| 新增模型提供者 | 修改 DagWorkflowEngine | 新增执行器类 |
| 修改模型 API | 修改 DagWorkflowEngine | 修改对应执行器 |
| 运行时禁用模型 | 不支持 | REST API 禁用 |
| 动态加载新模型 | 不支持 | 支持（注册 API） |
| 单元测试 | 需 mock 整个引擎 | 可独立测试执行器 |
| 代码复用 | 低（硬编码） | 高（SPI 标准） |

## 迁移风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| API 对接复杂度 | 中 | 先实现 1 个执行器验证流程 |
| 异步任务处理 | 中 | 使用 dag-scheduler 的超时机制 |
| 性能影响 | 低 | SPI 调用开销可忽略 |
| 团队学习成本 | 低 | dag-scheduler 文档完善 |
| 回归测试 | 中 | 保持原有测试，新增 SPI 测试 |

---

*辩论准备完毕！*
