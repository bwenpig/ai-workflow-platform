# 架构可视化

## 当前架构

```mermaid
graph TB
    subgraph ai-workflow
        Engine[DagWorkflowEngine]
        PyExec[PythonScriptExecutor]
        Mock[mockModelResult<br/>硬编码模型逻辑]
        
        Engine --> PyExec
        Engine --> Mock
        
        subgraph MockModels
            Kling[ Kling Mock]
            Wan[ Wan Mock]
            Seedance[ Seedance Mock]
            NanoBanana[ NanoBanana Mock]
        end
        
        Mock --> Kling
        Mock --> Wan
        Mock --> Seedance
        Mock --> NanoBanana
    end
    
    style Mock fill:#ff6b6b
    style Kling fill:#ffe66d
    style Wan fill:#ffe66d
    style Seedance fill:#ffe66d
    style NanoBanana fill:#ffe66d
```

**问题：**
- 🔴 模型逻辑硬编码在引擎中
- 🔴 新增模型需要修改引擎代码
- 🔴 无法运行时扩展
- 🟡 测试覆盖率低

---

## 重构后架构（方案 A）

```mermaid
graph TB
    subgraph ai-workflow["ai-workflow (业务层)"]
        Engine[DagWorkflowEngine]
        Registry[ExecutorRegistry<br/>SPI 注册中心]
        
        Engine --> Registry
    end
    
    subgraph dag-scheduler["dag-scheduler (SPI 框架)"]
        SPI[NodeExecutor 接口]
        
        subgraph Executors["执行器实现"]
            PyExec[PythonScriptExecutor]
            Kling[KlingExecutor]
            Wan[WanExecutor]
            Seedance[SeedanceExecutor]
            NanoBanana[NanoBananaExecutor]
        end
        
        SPI <|-- PyExec
        SPI <|-- Kling
        SPI <|-- Wan
        SPI <|-- Seedance
        SPI <|-- NanoBanana
        
        Registry --> PyExec
        Registry --> Kling
        Registry --> Wan
        Registry --> Seedance
        Registry --> NanoBanana
    end
    
    dag-scheduler -.->|依赖 | ai-workflow
    
    style Engine fill:#4ecdc4
    style Registry fill:#4ecdc4
    style SPI fill:#95e1d3
    style PyExec fill:#a8e6cf
    style Kling fill:#a8e6cf
    style Wan fill:#a8e6cf
    style Seedance fill:#a8e6cf
    style NanoBanana fill:#a8e6cf
```

**优势：**
- ✅ 统一 SPI 接口标准
- ✅ 新增模型无需修改引擎
- ✅ 支持运行时扩展
- ✅ 高测试覆盖率（95.7%）

---

## 数据流对比

### 当前：直接调用

```mermaid
sequenceDiagram
    participant Client
    participant Engine as DagWorkflowEngine
    participant Mock as mockModelResult
    
    Client->>Engine: execute(workflow)
    Engine->>Engine: topologicalSort()
    
    loop 每个节点
        Engine->>Engine: switch(nodeType)
        Engine->>Mock: case "MODEL"
        Mock-->>Engine: 硬编码结果
    end
    
    Engine-->>Client: instanceId
```

### 重构后：SPI 调用

```mermaid
sequenceDiagram
    participant Client
    participant Engine as DagWorkflowEngine
    participant Registry as ExecutorRegistry
    participant Executor as NodeExecutor
    
    Client->>Engine: execute(workflow)
    Engine->>Engine: topologicalSort()
    
    loop 每个节点
        Engine->>Registry: getExecutor(nodeType)
        Registry-->>Engine: NodeExecutor
        
        Engine->>Engine: new NodeExecutionContext()
        Engine->>Executor: execute(context)
        Executor-->>Engine: NodeExecutionResult
    end
    
    Engine-->>Client: instanceId
```

---

## 扩展流程对比

### 当前：添加新模型

```mermaid
flowchart TD
    A[需求：新增模型] --> B[修改 DagWorkflowEngine]
    B --> C[添加 case 分支]
    C --> D[实现 mockModelResult 逻辑]
    D --> E[修改单元测试]
    E --> F[回归测试]
    F --> G[部署]
    
    style B fill:#ff6b6b
    style C fill:#ff6b6b
    style D fill:#ff6b6b
```

**问题：** 需要修改核心引擎代码，风险高

---

### 重构后：添加新模型

```mermaid
flowchart TD
    A[需求：新增模型] --> B[创建 NewModelExecutor]
    B --> C[实现 NodeExecutor 接口]
    C --> D[添加 @NodeComponent 注解]
    D --> E[编写单元测试]
    E --> F[自动注册]
    F --> G[部署]
    
    style B fill:#a8e6cf
    style C fill:#a8e6cf
    style D fill:#a8e6cf
    style E fill:#a8e6cf
```

**优势：** 新增文件，无需修改现有代码

---

## 模块依赖关系

```mermaid
graph LR
    subgraph 业务层
        A[ai-workflow]
    end
    
    subgraph SPI 层
        B[dag-scheduler]
    end
    
    subgraph 框架层
        C[Spring Boot 3.2.3]
    end
    
    subgraph 基础层
        D[Java 17]
    end
    
    A --> B
    B --> C
    C --> D
    
    style A fill:#4ecdc4
    style B fill:#95e1d3
    style C fill:#a8e6cf
    style D fill:#d8f3dc
```

**关键：** dag-scheduler 不依赖 ai-workflow，保持 SPI 层独立性

---

## 执行器注册流程

```mermaid
sequenceDiagram
    participant Spring
    participant AutoDiscovery as ExecutorAutoDiscovery
    participant Registry as ExecutorRegistry
    participant Executor as @NodeComponent
    
    Spring->>AutoDiscovery: 启动扫描
    AutoDiscovery->>AutoDiscovery: 扫描 @NodeComponent
    
    loop 每个执行器
        AutoDiscovery->>Executor: 实例化
        Executor->>Executor: init()
        Executor->>Registry: register(type, executor)
        Registry-->>AutoDiscovery: 注册成功
    end
    
    AutoDiscovery-->>Spring: 扫描完成
```

---

## 节点执行流程

```mermaid
stateDiagram-v2
    [*] --> PENDING: 创建任务
    PENDING --> RUNNING: 开始执行
    RUNNING --> SUCCESS: 执行成功
    RUNNING --> FAILED: 执行失败
    RUNNING --> CANCELLED: 用户取消
    RUNNING --> TIMEOUT: 超时
    
    SUCCESS --> [*]
    FAILED --> [*]
    CANCELLED --> [*]
    TIMEOUT --> [*]
```

---

*图表使用 Mermaid 语法，可在支持 Mermaid 的编辑器中查看*
