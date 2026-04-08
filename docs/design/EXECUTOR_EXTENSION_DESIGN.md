# Executor 扩展架构设计方案

## 概述

本文档描述如何设计一个可扩展的 Executor 架构，支持通过 SPI 和配置方式动态注册新的节点执行器。

## 现有架构分析

### 当前 Executor 结构

```
executor/
├── ConditionalExecutor.java    # 条件分支
├── LoopExecutor.java           # 循环
├── HttpRequestExecutor.java    # HTTP 请求
└── LLMNodeExecutor.java        # LLM 调用
```

### 现有 ExecutorRegistry

```java
@Component
public class ExecutorRegistry {
    private final Map<NodeType, NodeExecutor> executors = new HashMap<>();
    
    @PostConstruct
    public void init() {
        // 硬编码注册
        executors.put(NodeType.CONDITIONAL, new ConditionalExecutor());
        executors.put(NodeType.LOOP, new LoopExecutor());
        // ...
    }
    
    public NodeExecutor get(NodeType type) {
        return executors.get(type);
    }
}
```

## 设计目标

1. **可扩展**: 新增 Executor 无需修改核心代码
2. **配置化**: 通过配置文件声明式注册
3. **SPI 支持**: 支持 Java SPI 机制自动发现
4. **生命周期管理**: 支持 Executor 的初始化和销毁
5. **元数据声明**: 支持声明式配置（名称、描述、参数schema）

## 核心设计

### 1. Executor 接口定义

```java
public interface NodeExecutor {
    
    /**
     * 执行节点
     */
    NodeResult execute(NodeExecutionContext context);
    
    /**
     * 获取节点类型
     */
    NodeType getType();
    
    /**
     * 获取元数据
     */
    default ExecutorMetadata getMetadata() {
        return ExecutorMetadata.builder()
            .type(getType())
            .name(getType().name())
            .description("No description")
            .build();
    }
    
    /**
     * 初始化（可选）
     */
    default void initialize(ExecutorConfig config) {
        // 默认空实现
    }
    
    /**
     * 销毁（可选）
     */
    default void destroy() {
        // 默认空实现
    }
    
    /**
     * 校验配置（可选）
     */
    default void validate(NodeConfig config) throws ValidationException {
        // 默认空实现
    }
}
```

### 2. 元数据定义

```java
@Data
@Builder
public class ExecutorMetadata {
    private NodeType type;
    private String name;
    private String description;
    private String category;           // 分类: logic, integration, ai, etc.
    private String icon;              // 图标
    private List<ParameterSchema> inputParams;
    private List<ParameterSchema> outputParams;
    private Map<String, Object> capabilities;
    private boolean experimental;
    private String version;
}

@Data
@Builder
public class ParameterSchema {
    private String name;
    private String type;               // string, number, boolean, object, array
    private String label;
    private String description;
    private boolean required;
    private Object defaultValue;
    private Map<String, Object> constraints;  // min, max, pattern, etc.
    private Map<String, Object> options;      // 下拉选项
}
```

### 3. 配置化注册机制

```java
@Component
public class ConfigDrivenExecutorRegistry {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private final Map<NodeType, NodeExecutor> executors = new ConcurrentHashMap<>();
    private final Map<NodeType, ExecutorMetadata> metadataMap = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void loadExecutors() {
        // 1. 加载配置文件
        List<ExecutorRegistration> registrations = loadFromConfig();
        
        // 2. 加载 SPI 发现
        List<ExecutorRegistration> spiRegistrations = loadFromSPI();
        
        // 3. 合并（配置优先于 SPI）
        Map<String, ExecutorRegistration> all = new HashMap<>();
        spiRegistrations.forEach(r -> all.put(r.getType(), r));
        registrations.forEach(r -> all.put(r.getType(), r));
        
        // 4. 注册
        all.values().forEach(this::registerExecutor);
    }
    
    private void registerExecutor(ExecutorRegistration registration) {
        try {
            // 实例化
            NodeExecutor executor = createExecutor(registration);
            
            // 初始化
            if (registration.getConfig() != null) {
                executor.initialize(registration.getConfig());
            }
            
            // 注册
            NodeType type = NodeType.valueOf(registration.getType());
            executors.put(type, executor);
            metadataMap.put(type, executor.getMetadata());
            
            log.info("Registered executor: {} -> {}", registration.getType(), 
                registration.getClassName());
            
        } catch (Exception e) {
            log.error("Failed to register executor: {}", registration.getType(), e);
        }
    }
}
```

### 4. 注册配置格式

```yaml
# executors.yaml
executors:
  # 内置 Executor
  - type: conditional
    class: com.ben.workflow.executor.ConditionalExecutor
    enabled: true
    config:
      defaultTimeout: 30000
  
  - type: loop
    class: com.ben.workflow.executor.LoopExecutor
    enabled: true
    config:
      maxIterations: 1000
  
  # 第三方/扩展 Executor
  - type: email_smtp
    class: com.ben.workflow.executor.EmailExecutor
    enabled: true
    config:
      defaultSmtpHost: smtp.gmail.com
      defaultSmtpPort: 587
      poolSize: 5
  
  - type: slack_message
    class: com.ben.workflow.executor.SlackExecutor
    enabled: true
    config:
      defaultChannel: "#workflows"
  
  # 禁用某些内置
  - type: http_request
    enabled: false
```

```json
// executors.json
{
  "executors": [
    {
      "type": "conditional",
      "class": "com.ben.workflow.executor.ConditionalExecutor",
      "enabled": true,
      "config": {
        "defaultTimeout": 30000
      }
    }
  ]
}
```

### 5. SPI 机制

#### META-INF/services/com.ben.workflow.executor.NodeExecutor

```
# 第三方 Executor 只需要创建此文件
com.ben.workflow.executor.EmailExecutor
com.ben.workflow.executor.SlackExecutor
com.ben.workflow.executor.DatabaseExecutor
```

#### ServiceLoader 加载

```java
private List<ExecutorRegistration> loadFromSPI() {
    List<ExecutorRegistration> registrations = new ArrayList<>();
    
    ServiceLoader.load(NodeExecutor.class).forEach(executor -> {
        ExecutorMetadata metadata = executor.getMetadata();
        
        ExecutorRegistration registration = ExecutorRegistration.builder()
            .type(metadata.getType().name())
            .className(executor.getClass().getName())
            .enabled(true)
            .metadata(metadata)
            .build();
        
        registrations.add(registration);
    });
    
    return registrations;
}
```

### 6. Executor 基类

```java
public abstract class BaseExecutor implements NodeExecutor {
    
    protected ExecutorConfig config;
    protected ExecutorRegistry registry;
    
    @Override
    public void initialize(ExecutorConfig config) {
        this.config = config;
        doInitialize();
    }
    
    protected void doInitialize() {
        // 子类可覆盖
    }
    
    @Override
    public void destroy() {
        doDestroy();
    }
    
    protected void doDestroy() {
        // 子类可覆盖
    }
    
    @Override
    public void validate(NodeConfig config) {
        // 默认校验
        if (config == null) {
            throw new ValidationException("Node config cannot be null");
        }
    }
    
    protected Object resolveExpression(String expression, NodeExecutionContext context) {
        // 复用现有实现
    }
}
```

## 优先实现的 Executor 列表

### Tier 1: 核心集成 (高优先级)

| Executor | 用途 | 依赖 |
|----------|------|------|
| **EmailExecutor** | 发送邮件 | JavaMail / Apache Commons Email |
| **SlackExecutor** | Slack 消息 | Slack SDK |
| **TelegramExecutor** | Telegram 消息 | Telegram Bot API |
| **HttpRequestExecutor** | HTTP 请求 | 已存在 |

### Tier 2: 数据处理 (中优先级)

| Executor | 用途 | 依赖 |
|----------|------|------|
| **DatabaseExecutor** | 数据库操作 | JDBC / Spring Data |
| **FileExecutor** | 文件读写 | Java NIO |
| **JsonTransformExecutor** | JSON 转换 | Jackson |
| **CsvExecutor** | CSV 处理 | OpenCSV |

### Tier 3: AI 增强 (中优先级)

| Executor | 用途 | 依赖 |
|----------|------|------|
| **VectorStoreExecutor** | 向量存储 | Pinecone Client |
| **EmbeddingExecutor** | 向量嵌入 | OpenAI/HuggingFace |
| **MemoryExecutor** | 对话记忆 | 自研 |

### Tier 4: 高级功能 (低优先级)

| Executor | 用途 | 依赖 |
|----------|------|------|
| **WebsocketExecutor** | WebSocket | Java WebSocket |
| **CronExecutor** | 定时触发 | Quartz |
| **RabbitMQExecutor** | 消息队列 | AMQP |

## 完整示例: EmailExecutor

```java
@ExecutorMetadata(
    type = NodeType.EMAIL,
    name = "Send Email",
    description = "Send email via SMTP",
    category = "integration",
    icon = "📧",
    inputParams = {
        @ParameterSchema(name = "to", type = "array", label = "To", 
            description = "Recipients", required = true),
        @ParameterSchema(name = "cc", type = "array", label = "CC"),
        @ParameterSchema(name = "subject", type = "string", label = "Subject",
            required = true),
        @ParameterSchema(name = "body", type = "string", label = "Body",
            required = true, defaultValue = ""),
        @ParameterSchema(name = "attachment", type = "array", label = "Attachments")
    }
)
@Component
public class EmailExecutor extends BaseExecutor {
    
    private JavaMailSender mailSender;
    
    @Override
    public NodeType getType() {
        return NodeType.EMAIL;
    }
    
    @Override
    protected void doInitialize() {
        Map<String, Object> cfg = config.getParams();
        
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(getString(cfg, "smtpHost", "localhost"));
        sender.setPort(getInt(cfg, "smtpPort", 25));
        sender.setUsername(getString(cfg, "username", ""));
        sender.setPassword(getString(cfg, "password", ""));
        
        this.mailSender = sender;
    }
    
    @Override
    public NodeResult execute(NodeExecutionContext context) {
        NodeConfig nodeConfig = context.getNode().getConfig();
        
        try {
            List<String> to = resolveList(nodeConfig.getString("to"), context);
            String subject = resolveString(nodeConfig.getString("subject"), context);
            String body = resolveString(nodeConfig.getString("body"), context);
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(to.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(body, true);
            
            // 处理附件
            List<String> attachments = resolveList(nodeConfig.getString("attachment"), context);
            for (String path : attachments) {
                helper.addAttachment(new File(path).getName(), new File(path));
            }
            
            mailSender.send(message);
            
            return NodeResult.success(Map.of("sent", true, "recipients", to));
            
        } catch (Exception e) {
            return NodeResult.failure("Email failed: " + e.getMessage());
        }
    }
}
```

## 动态注册 API

```java
@RestController
@RequestMapping("/api/executors")
public class ExecutorController {
    
    @Autowired
    private ConfigDrivenExecutorRegistry registry;
    
    @GetMapping
    public List<ExecutorMetadata> listExecutors() {
        return registry.getAllMetadata();
    }
    
    @GetMapping("/{type}")
    public ExecutorMetadata getExecutor(@PathVariable String type) {
        return registry.getMetadata(NodeType.valueOf(type));
    }
    
    @PostMapping
    public ResponseEntity<?> registerExecutor(@RequestBody ExecutorRegistration registration) {
        registry.registerExecutor(registration);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{type}")
    public ResponseEntity<?> unregisterExecutor(@PathVariable String type) {
        registry.unregister(NodeType.valueOf(type));
        return ResponseEntity.ok().build();
    }
}
```

## 测试策略

### 1. 接口测试

```java
@Test
void testExecutorInterface() {
    NodeExecutor executor = new ConditionalExecutor();
    
    assertNotNull(executor.getType());
    assertNotNull(executor.getMetadata());
    assertNotNull(executor.getMetadata().getName());
}
```

### 2. 注册机制测试

```java
@Test
void testConfigDrivenRegistration() {
    ExecutorRegistration registration = ExecutorRegistration.builder()
        .type("test_executor")
        .className(TestExecutor.class.getName())
        .enabled(true)
        .build();
    
    registry.registerExecutor(registration);
    
    assertNotNull(registry.get(NodeType.valueOf("test_executor")));
}
```

### 3. SPI 发现测试

```java
@Test
void testSPIDiscovery() {
    List<ExecutorRegistration> spiRegistrations = loadFromSPI();
    
    assertTrue(spiRegistrations.size() > 0);
    assertTrue(spiRegistrations.stream()
        .anyMatch(r -> r.getType().equals("email")));
}
```

### 4. 生命周期测试

```java
@Test
void testExecutorLifecycle() {
    TestExecutor executor = new TestExecutor();
    
    executor.initialize(ExecutorConfig.builder()
        .param("key", "value")
        .build());
    
    assertTrue(executor.isInitialized());
    
    executor.destroy();
    
    assertFalse(executor.isInitialized());
}
```

## 迁移指南

### 从硬编码迁移

1. 创建 `executors.yaml` 配置文件
2. 为每个现有 Executor 添加配置项
3. 修改 `ExecutorRegistry` 继承 `ConfigDrivenExecutorRegistry`
4. 保持向后兼容

### 添加新 Executor

1. 继承 `BaseExecutor`
2. 实现 `getType()` 和 `execute()`
3. 添加 `@ExecutorMetadata` 注解（或实现 `getMetadata()`）
4. 在 `executors.yaml` 中注册（或使用 SPI）
5. 前端添加节点类型支持

## 总结

| 特性 | 实现方式 |
|------|----------|
| **配置化** | YAML/JSON 配置文件 |
| **SPI** | META-INF/services |
| **元数据** | @ExecutorMetadata 注解 |
| **生命周期** | initialize()/destroy() |
| **动态注册** | REST API |