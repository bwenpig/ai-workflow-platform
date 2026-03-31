# ai-workflow 重构方案

## 方案文档

| 文档 | 说明 |
|------|------|
| [PLAN_A_DEEP_INTEGRATION.md](./PLAN_A_DEEP_INTEGRATION.md) | 方案 A 详细设计（深度集成 dag-scheduler） |
| [ARCHITECTURE_COMPARISON.md](./ARCHITECTURE_COMPARISON.md) | 架构对比：当前 vs 重构后 |
| [DEBATE_PREP.md](./DEBATE_PREP.md) | 辩论准备材料 |

---

## 方案 A 速览

### 核心思想
**深度集成 dag-scheduler SPI**，将 PythonScriptExecutor 和模型适配器迁移到统一标准。

### 架构图
```
ai-workflow (业务层)
    ↓ 使用
dag-scheduler (SPI 框架)
    ├─ NodeExecutor 接口
    ├─ ExecutorRegistry 注册中心
    └─ 执行器实现
        ├─ PythonScriptExecutor
        ├─ KlingExecutor
        ├─ WanExecutor
        ├─ SeedanceExecutor
        └─ NanoBananaExecutor
```

### 实施步骤
1. 将 PythonScriptExecutor 迁移到 dag-scheduler（2-3 天）
2. 创建 Kling/Wan/Seedance/NanoBanana 执行器（4-6 天）
3. 修改 DagWorkflowEngine 使用 SPI 执行（1-2 天）
4. 补充单元测试（2-3 天）

**总计：9-14 天**

### 核心优势
1. ✅ **避免重复造轮子** - dag-scheduler 已有成熟 SPI 框架
2. ✅ **统一 SPI 标准** - 所有执行器实现 NodeExecutor 接口
3. ✅ **扩展性强** - 新增模型只需实现 SPI，无需修改引擎
4. ✅ **测试复用** - dag-scheduler 已有 95.7% 测试覆盖率

### 关键代码

**执行器示例：**
```java
@Component
@NodeComponent(value = "kling", name = "Kling Video Generator")
public class KlingExecutor implements NodeExecutor {
    
    @Override
    public NodeExecutionResult execute(NodeExecutionContext context) {
        String prompt = context.getInput("prompt", String.class);
        // 调用 Kling API...
        return NodeExecutionResult.success(nodeId, outputs, start, end);
    }
}
```

**引擎调用：**
```java
NodeExecutor executor = executorRegistry.getExecutor("kling");
NodeExecutionResult result = executor.execute(context);
```

---

## 辩论要点

### 方案 A 立场
- 长期可维护性 > 短期便利
- 统一标准 > 各自为政
- 测试驱动 > 事后补救

### 预期反驳
- "迁移成本太高" → ROI 计算：3 个月后盈利
- "SPI 太复杂" → 核心接口只有 1 个方法
- "性能损耗" → <5%，可忽略

---

## 决策矩阵

| 评估维度 | 权重 | 方案 A | 方案 B |
|----------|------|--------|--------|
| 扩展性 | 30% | ⭐⭐⭐⭐⭐ | ? |
| 代码质量 | 25% | ⭐⭐⭐⭐⭐ | ? |
| 迁移成本 | 20% | ⭐⭐⭐ | ? |
| 学习曲线 | 15% | ⭐⭐⭐⭐ | ? |
| 测试覆盖 | 10% | ⭐⭐⭐⭐⭐ | ? |

---

## 下一步

1. 与龙傲天 B 辩论
2. 确定最终方案
3. 创建详细实施计划
4. 开始编码

---

*最后更新：2026-03-31*
