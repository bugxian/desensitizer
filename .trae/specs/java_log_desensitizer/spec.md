# Java 日志脱敏组件规范

## Why
在 Java 银行业务系统中，日志记录是排查问题的重要手段，但日志中可能包含客户敏感信息（如身份证号、手机号、银行卡号、账户信息等），直接记录日志会带来信息安全风险。需要一个轻量、灵活、可扩展的日志脱敏组件来自动检测和掩码敏感数据，且不影响用户原有使用的日志框架。

## What Changes
- 提供基于注解的字段脱敏能力，支持自定义脱敏规则
- 内置常见敏感数据类型（手机号、身份证、银行卡、邮箱、密码等）的脱敏实现
- 支持**字符串正则匹配**和**JSON 解析后字段脱敏**两种方式
- 采用装饰器模式自动包装 Appender，用户无需修改原有日志配置
- 通过 Spring 配置文件控制脱敏功能开关和敏感字段配置
- 支持正则检测器和 NLP 检测器两种模式，可配置切换
- **NLP 检测器支持离线部署，模型文件需预先下载，不依赖网络下载或更新**
- 支持与主流日志框架（Logback、Log4j2）无缝集成，用户 pom 引入即可使用
- **JSON 库自动检测**：自动识别 Jackson/Fastjson/Gson 并适配
- **异常处理**：脱敏失败时错误信息输出到专用日志文件，不影响主日志流

## Impact
- Affected specs: 日志脱敏能力
- Affected code: 核心脱敏引擎、注解定义、内置规则实现、日志框架集成适配器、Spring 自动配置、NLP 检测器

## TDD 开发方法论

本项目采用**测试驱动开发（TDD）** 方法，在实际编码之前先定义详尽的验收测试用例，确保组件的正确性和可验证性。

### 开发流程

```
┌─────────────────────────────────────────────────────────────┐
│  Phase 1: 测试项目搭建与验收测试定义                           │
│  - 创建 desensitizer-test-app 测试 Spring Boot 项目          │
│  - 定义所有验收测试用例（单元测试 + 集成测试）                 │
│  - 验证测试用例在实现前处于 RED 状态                           │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  Phase 2: 核心模块实现                                        │
│  - 实现 desensitizer-core 模块                               │
│  - 逐个运行测试，确保 GREEN 状态                              │
│  - 重构优化代码                                              │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│  Phase 3: 集成模块实现                                        │
│  - 实现日志框架集成模块（Logback/Log4j2）                    │
│  - 实现 Spring Boot 自动配置                                 │
│  - 运行集成测试验证                                          │
└─────────────────────────────────────────────────────────────┘
```

## 需求确认

| 维度 | 确认结果 |
|-----|---------|
| 日志消息格式 | 两者都有（纯字符串 + JSON） |
| 脱敏处理层级 | 两者都支持（字符串正则 + @Sensitive 注解） |
| Spring Boot 版本 | 2.x |
| JSON 库 | 自动检测（Jackson/Fastjson/Gson） |
| 异常处理策略 | 错误信息输出到专用日志文件，原日志正常输出 |
| 部署环境 | Kubernetes |
| 规则管理 | 静态加载（启动时加载，运行期不变） |
| 并发量 | 中等 (1000-10000 TPS) |

## ADDED Requirements

### Requirement: 敏感信息注解定义
系统 SHALL 提供 `@Sensitive` 注解，允许开发者标记需要脱敏的字段，并指定脱敏规则类型。

#### Scenario: 基础字段脱敏
- **WHEN** 开发者使用 `@Sensitive(type = SensitiveType.PHONE)` 标注 `String phone` 字段
- **THEN** 日志输出时，该字段显示为 `138****5678` 格式

#### Scenario: 嵌套对象脱敏
- **WHEN** 开发者使用 `@Sensitive` 标注嵌套对象字段
- **THEN** 系统递归遍历嵌套对象，对所有标注了 `@Sensitive` 的字段进行脱敏

### Requirement: 内置脱敏规则
系统 SHALL 提供内置脱敏规则，覆盖常见敏感数据类型：

| 类型 | 示例输入 | 示例输出 | 脱敏规则 |
|------|---------|---------|---------|
| 手机号 | 13812345678 | 138\*\*\*\*5678 | 显示前3后4位 |
| 手机号(不足11位) | 138123456 | 138\*\*\*\*56 | 不足11位，显示前3后2位 |
| 手机号(不足7位) | 1381234 | 1381234 | 不足7位，不脱敏 |
| 身份证 | 110101199001011234 | 110101\*\*\*\*\*\*\*\*1234 | 显示前6后4位 |
| 身份证(15位) | 110101910101123 | 110101\*\*\*\*\*\*123 | 显示前6后3位 |
| 银行卡 | 6222021234567890123 | 622202\*\*\*\*\*\*\*\*0123 | 显示前6后4位 |
| 银行卡(19位) | 6222021234567890123 | 622202\*\*\*\*\*\*\*\*0123 | 显示前6后4位 |
| 银行卡(16位) | 6222021234567890 | 622202\*\*\*\*\*\*7890 | 显示前6后4位 |
| 邮箱 | test@example.com | t\*\*\*@example.com | 保留域名，首字母外匿名 |
| 邮箱(短用户名) | ab@example.com | \*\*@example.com | 用户名2字符及以下全掩 |
| 密码 | mySecretPassword | \*\*\*\*\*\*\* | 完全掩码 |
| 地址 | 北京市朝阳区某某路123号 | 北京市朝阳区\*\*\* | 显示省市区，详细地址掩码 |

**注**：银行业务特有敏感字段（如账号、客户号等）将由用户在后续提供具体定义。

### Requirement: 字符串正则脱敏
系统 SHALL 支持对日志消息中的纯字符串进行正则匹配脱敏：

#### Scenario: 手机号正则匹配
- **WHEN** 日志消息包含 `13812345678`
- **THEN** 脱敏后输出 `138****5678`

#### Scenario: 身份证正则匹配
- **WHEN** 日志消息包含 `110101199001011234`
- **THEN** 脱敏后输出 `110101********1234`

### Requirement: JSON 解析脱敏
系统 SHALL 支持 JSON 格式日志消息的解析和脱敏：

#### Scenario: JSON 对象脱敏
- **WHEN** 日志消息为 `{"name":"张三","phone":"13812345678","idCard":"110101199001011234"}`
- **THEN** 脱敏后输出 `{"name":"张三","phone":"138****5678","idCard":"110101********1234"}`

#### Scenario: JSON 数组脱敏
- **WHEN** 日志消息为 `[{"phone":"13812345678"},{"phone":"13987654321"}]`
- **THEN** 数组中所有 phone 字段均被脱敏

#### Scenario: 嵌套 JSON 脱敏
- **WHEN** 日志消息为 `{"user":{"phone":"13812345678"},"order":{"idCard":"110101199001011234"}}`
- **THEN** 递归遍历所有层级的字段并脱敏

### Requirement: 自动包装集成模式
系统 SHALL 采用装饰器模式自动包装 Appender，实现零侵入接入：

#### Scenario: 自动包装 Appender
- **WHEN** 用户在 Spring 配置文件中启用脱敏功能
- **THEN** 组件自动检测并包装 ConsoleAppender、FileAppender、RollingFileAppender 等常用 Appender
- **AND** 用户无需修改 logback.xml 或 log4j2.xml 配置文件

### Requirement: 日志框架集成
系统 SHALL 支持与主流 Java 日志框架集成：

#### Scenario: Logback 集成
- **WHEN** 用户使用 Logback 作为日志框架
- **THEN** 组件自动检测并包装 ConsoleAppender、FileAppender、RollingFileAppender 等常用 Appender

#### Scenario: Log4j2 集成
- **WHEN** 用户使用 Log4j2 作为日志框架
- **THEN** 组件自动检测并包装 ConsoleAppender、RollingFileAppender 等常用 Appender

### Requirement: Spring 配置管理
系统 SHALL 通过 Spring 配置文件管理脱敏功能：

#### Scenario: 配置启用/禁用
- **WHEN** 管理员在 Spring 配置文件中设置 `desensitizer.enabled=true`
- **THEN** 脱敏功能生效
- **WHEN** 管理员在 Spring 配置文件中设置 `desensitizer.enabled=false`
- **THEN** 脱敏功能关闭，日志正常输出

#### Scenario: 自定义敏感字段配置
- **WHEN** 管理员在 Spring 配置文件中添加自定义敏感字段定义
- **THEN** 系统加载并注册自定义脱敏规则

### Requirement: 自定义脱敏规则
系统 SHALL 提供扩展机制，允许用户实现自定义脱敏规则：

#### Scenario: 自定义脱敏类型
- **WHEN** 开发者实现 `Desensitizer` 接口并注册自定义脱敏器
- **THEN** 可以通过 `@Sensitive(type = CustomType.XXX)` 使用自定义脱敏规则

### Requirement: 异常处理
系统 SHALL 实现安全的异常处理策略：

#### Scenario: 脱敏异常处理
- **WHEN** 脱敏过程中发生异常（如正则超时、JSON 解析失败、反射异常）
- **THEN** 错误信息记录到专用日志文件（默认 `logs/desensitizer-error.log`）
- **AND** 主日志流正常输出，不受影响
- **AND** 不抛出异常，不中断日志输出流程

### Requirement: JSON 库自动检测
系统 SHALL 自动检测并适配不同的 JSON 库：

#### Scenario: Jackson 检测
- **WHEN** classpath 中存在 Jackson
- **THEN** 使用 Jackson 进行 JSON 解析和序列化

#### Scenario: Fastjson 检测
- **WHEN** classpath 中存在 Fastjson 但不存在 Jackson
- **THEN** 使用 Fastjson 进行 JSON 解析和序列化

#### Scenario: Gson 检测
- **WHEN** classpath 中存在 Gson 但不存在 Jackson 和 Fastjson
- **THEN** 使用 Gson 进行 JSON 解析和序列化

### Requirement: NLP 扩展接口（离线部署）
系统 SHALL 预留 NLP 扩展接口，支持未来引入 NLP 小模型辅助处理复杂文本：

#### Scenario: NLP 扩展点
- **WHEN** 未来需要使用 NLP 模型辅助脱敏复杂文本（如自由格式地址、混合敏感信息）
- **THEN** 开发者可以通过实现 `SensitiveDetector` 接口扩展 NLP 能力
- **AND** 架构上支持热插拔，不影响现有正则脱敏流程

#### Scenario: 离线部署约束
- **WHEN** NLP 功能在生产环境部署时
- **THEN** 所有模型文件必须预先下载并打包到应用中
- **AND** 运行时不得联网下载或更新模型
- **AND** 模型文件存放于应用类路径或指定本地目录（支持 Kubernetes ConfigMap 挂载）

## MODIFIED Requirements

无

## REMOVED Requirements

无

## Technical Design

### 敏感信息检测器架构

```
┌─────────────────────────────────────────────────────────────┐
│              SensitiveDetector SPI 接口                      │
│         (可插拔，支持正则/NLP/混合多种实现)                    │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│ RegexDetector │   │ NlpDetector   │   │HybridDetector │
│   (V1实现)    │   │   (V2实现)    │   │   (V2实现)    │
└───────────────┘   └───────────────┘   └───────────────┘
```

### 核心接口设计

```java
/**
 * 敏感信息检测器接口（SPI）
 */
public interface SensitiveDetector {

    /**
     * 检测文本中的敏感信息
     * @param text 待检测文本
     * @return 敏感信息片段列表
     */
    List<SensitiveMatch> detect(String text);

    /**
     * 检测器名称
     */
    String name();

    /**
     * 是否启用
     */
    default boolean enabled() {
        return true;
    }
}

/**
 * 敏感信息匹配结果
 */
public class SensitiveMatch {
    private int start;
    private int end;
    private String matchedText;
    private String sensitiveType;
    private float confidence;
}

/**
 * 脱敏器接口
 */
public interface Desensitizer {
    String desensitize(String value);
}

/**
 * 敏感类型枚举
 */
public enum SensitiveType {
    PHONE, ID_CARD, BANK_CARD, EMAIL, PASSWORD, ADDRESS, CUSTOM
}

/**
 * 敏感字段注解
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {
    SensitiveType type() default SensitiveType.CUSTOM;
    Class<? extends Desensitizer> customType() default NoOpDesensitizer.class;
    String pattern() default "";
    int keepLeft() default 0;
    int keepRight() default 0;
}
```

### 模块结构

```
java-desensitizer/
├── desensitizer-core/              # 核心脱敏引擎（零外部依赖）
│   ├── annotation/               # @Sensitive 注解定义
│   ├── api/                      # Desensitizer、SensitiveDetector 接口定义
│   ├── engine/                   # DesensitizationEngine 核心引擎
│   ├── detector/                 # RegexDetector 正则检测器实现
│   ├── registry/                 # DesensitizerRegistry 脱敏器注册表
│   ├── json/                     # JSON 解析器抽象和自动检测
│   └── util/                     # 工具类
├── desensitizer-builtin/          # 内置脱敏规则实现
├── desensitizer-logback/         # Logback 集成（自动包装 Appender）
├── desensitizer-log4j2/          # Log4j2 集成（自动包装 Appender）
├── desensitizer-spring-boot/     # Spring Boot 自动配置
├── desensitizer-nlp/             # NLP 检测器扩展（V2 实现）
│   ├── detector/                # NlpDetector 实现
│   ├── model/                    # 模型加载器
│   └── runtime/                  # ONNX Runtime 集成
└── desensitizer-test-app/       # TDD 测试验证用 Spring Boot 项目
```

### 装饰器模式设计

```
                    ┌─────────────────────┐
                    │    LogEvent         │
                    └─────────────────────┘
                              │
                              ▼
                    ┌─────────────────────┐
                    │  DesensitizingLayout │
                    │  (装饰器包装)        │
                    └─────────────────────┘
                              │
                              ▼
                    ┌─────────────────────┐
                    │    OriginalLayout   │
                    │  (原有 Layout)      │
                    └─────────────────────┘
```

### 异常处理设计

```
┌─────────────────────────────────────────────────────────────┐
│                    脱敏流程                                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────────┐
                    │   RegexDetector     │
                    │   敏感信息检测       │
                    └─────────────────────┘
                              │
                              ▼
                    ┌─────────────────────┐
                    │   JSON 解析（如需）  │
                    └─────────────────────┘
                              │
                              ▼
                    ┌─────────────────────┐
                    │   Desensitizer      │
                    │   执行脱敏          │
                    └─────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              │ 检测到异常？                   │
              └───────────────┬───────────────┘
                      Yes    │    No
        ┌─────────────────────┐
        ▼                     ▼
┌─────────────────────┐   ┌─────────────────────┐
│ DesensitizerLogger  │   │  返回脱敏结果        │
│ 记录错误到专用日志   │   └─────────────────────┘
│ (desensitizer-error.log) │
└─────────────────────┘
        │
        ▼
┌─────────────────────┐
│ 返回原始日志消息    │  ──► 主日志流不受影响
└─────────────────────┘
```

### JSON 库自动检测

```java
public class JsonDetector {

    private static final JsonParser JSON_PARSER;

    static {
        if (ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", null)) {
            JSON_PARSER = new JacksonParser();
        } else if (ClassUtils.isPresent("com.alibaba.fastjson.JSON", null)) {
            JSON_PARSER = new FastjsonParser();
        } else if (ClassUtils.isPresent("com.google.gson.Gson", null)) {
            JSON_PARSER = new GsonParser();
        } else {
            JSON_PARSER = new NoOpParser();
        }
    }
}
```

### NLP 离线部署架构

```
┌─────────────────────────────────────────────────────────────┐
│                    离线部署架构                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────────────────────────────────────────┐   │
│   │                  应用包 (JAR/WAR)                   │   │
│   │  ┌─────────────────────────────────────────────┐   │   │
│   │  │           模型文件 (预下载)                   │   │   │
│   │  │   - albert_tiny_model.onnx (~4MB)           │   │   │
│   │  │   - vocabulary.txt                          │   │   │
│   │  │   - label_map.json                           │   │   │
│   │  └─────────────────────────────────────────────┘   │   │
│   │                                                     │   │
│   │  ┌─────────────────────────────────────────────┐   │   │
│   │  │           NLP 检测器模块                     │   │   │
│   │  │   - ONNX Runtime (本地推理，无网络依赖)       │   │   │
│   │  │   - NlpDetector (SPI 实现)                   │   │   │
│   │  └─────────────────────────────────────────────┘   │   │
│   └─────────────────────────────────────────────────────┘   │
│                              │                              │
│                              │ (禁止网络)                    │
│                              │                              │
│                    ┌─────────┴─────────┐                   │
│                    │   生产环境网络      │                   │
│                    │   (完全隔离)       │                   │
│                    └───────────────────┘                   │
│                                                             │
│   Kubernetes 部署：支持从 ConfigMap 挂载模型文件              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### NLP 模型选型（V2 实现）

| 模型 | 规模 | 推理延迟 | 中文支持 | 推荐场景 |
|-----|------|---------|---------|---------|
| ALBERT-Tiny | ~4MB | ~20ms | ✅ | **首选**，轻量级离线部署 |
| BiLSTM-CRF | ~10MB | ~10ms | ✅ | 高性能场景 |
| BERT-base + ONNX | ~400MB | ~30ms | ✅ | 高精度场景 |

**选型理由**：ALBERT-Tiny 体积最小，适合离线部署的银行环境，同时推理速度可满足批处理场景。

### NLP 配置设计

```yaml
desensitizer:
  enabled: true

  # 正则检测器配置（V1 默认启用）
  regex:
    enabled: true
    patterns:
      phone: "1[3-9]\\d{9}"
      idCard: "[1-9]\\d{5}(\\d{4})\\d{4}(\\d{3}[\\dXx])"
      bankCard: "[621789]\\d{15,19}"
      email: "\\w+@\\w+\\.\\w+"
      password: "(?i)(password|pwd|密码)\\s*[:=]\\s*\\S+"

  # JSON 配置
  json:
    enabled: true
    # JSON 库自动检测，无需配置

  # 错误日志配置
  errorLog:
    enabled: true
    path: "logs/desensitizer-error.log"

  # NLP 检测器配置（V2 可选启用）
  # 重要约束：模型文件必须预先下载到本地，运行时不得联网
  nlp:
    enabled: false
    # 模型来源：本地类路径或本地文件系统（支持 Kubernetes ConfigMap 挂载）
    modelPath: "classpath:/nlp-model/albert_tiny/"
    # 备选：本地绝对路径
    # modelPath: "file:/opt/desensitizer/nlp-model/"
    # Kubernetes ConfigMap 挂载路径示例
    # modelPath: "file:/etc/desensitizer/nlp-model/"
    provider: "onnx"                    # onnx / remote (remote 模式下调用内部 NLP 服务)
    confidenceThreshold: 0.8
    batchSize: 32
    timeout: 100
```

### 依赖管理
- `desensitizer-core`：零外部依赖，仅使用 JDK
- `desensitizer-builtin`：依赖 desensitizer-core
- `desensitizer-logback`：依赖 desensitizer-core + logback
- `desensitizer-log4j2`：依赖 desensitizer-core + log4j2-api
- `desensitizer-spring-boot`：依赖 desensitizer-core + spring-boot-autoconfigure
- `desensitizer-nlp`：依赖 desensitizer-core + onnxruntime (V2 实现)
- `desensitizer-test-app`：依赖所有模块 + spring-boot-starter-logging + spring-boot-starter-test
- JSON 库依赖（自动检测，不强制引入）：
  - Jackson（com.fasterxml.jackson.core）
  - Fastjson（com.alibaba.fastjson）
  - Gson（com.google.code.gson）
- 使用 Maven 进行依赖管理和构建

### Spring 配置属性

```properties
# 脱敏功能开关
desensitizer.enabled=true

# 正则检测器配置
desensitizer.regex.enabled=true

# JSON 处理配置
desensitizer.json.enabled=true

# 错误日志配置
desensitizer.errorLog.enabled=true
desensitizer.errorLog.path=logs/desensitizer-error.log

# NLP 检测器配置（V2）
# 重要：模型必须预先下载到本地
desensitizer.nlp.enabled=false
desensitizer.nlp.modelPath=classpath:/nlp-model/albert_tiny/
desensitizer.nlp.confidenceThreshold=0.8

# 自定义脱敏规则包扫描路径
desensitizer.custom-packages=com.yourcompany.desensitizer

# 脱敏日志输出级别（仅当日志级别 >=此级别时脱敏）
desensitizer.min-level=DEBUG
```

### 实现计划

#### V1（本期实现）
- RegexDetector：正则表达式检测
- JSON 解析和脱敏支持
- JSON 库自动检测
- DesensitizerErrorLogger：错误日志专用输出
- 装饰器模式集成日志框架

#### V2（下期实现）
- NlpDetector SPI 实现
- 本地模型推理（ALBERT-Tiny + ONNX Runtime）
- HybridDetector 混合检测（正则优先，NLP 兜底）
- 批处理优化

### 部署约束

1. **模型文件预下载**：NLP 模型必须在开发/测试环境下载完毕，打包到应用 JAR 中
2. **禁止运行时下载**：生产环境网络完全隔离，禁止运行时联网下载模型
3. **模型版本锁定**：模型文件需与代码版本匹配，建议在配置中指定模型版本号
4. **Kubernetes 支持**：模型文件可通过 ConfigMap 或 PersistentVolumeClaim 挂载
