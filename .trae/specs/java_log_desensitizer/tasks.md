# Tasks - Java 日志脱敏组件 (TDD 开发流程)

## Phase 1: 测试项目搭建与验收测试定义

### 测试项目初始化

- [ ] Task 1.1: 创建 desensitizer-test-app Maven 项目
  - [ ] 创建测试项目 pom.xml，依赖 Spring Boot 2.7.x + JUnit 5 + AssertJ
  - [ ] 创建项目目录结构
  - [ ] 配置 Maven 依赖：spring-boot-starter-logging、logback-classic、log4j-api

### 验收测试定义 - 核心脱敏引擎

- [ ] Task 1.2: 定义 DesensitizerCoreTest 测试用例
  - [ ] 测试 `String desensitize(String value)` 基本字符串脱敏
  - [ ] 测试对象字段遍历和脱敏
  - [ ] 测试嵌套对象递归脱敏
  - [ ] 测试对象为 null 时的处理
  - [ ] 测试字段值为 null 时的处理
  - [ ] 测试多种敏感类型组合脱敏

### 验收测试定义 - 内置脱敏规则

- [ ] Task 1.3: 定义 BuiltinDesensitizersTest 测试用例

#### 手机号脱敏 (PhoneDesensitizer)
  - [ ] 11位手机号：13812345678 → 138****5678
  - [ ] 不足11位（9位）：138123456 → 138****56
  - [ ] 不足7位：1381234 → 1381234（不脱敏）
  - [ ] null输入 → null
  - [ ] 空字符串 → 空字符串
  - [ ] 非手机号格式 → 原样返回

#### 身份证脱敏 (IdCardDesensitizer)
  - [ ] 18位身份证：110101199001011234 → 110101********1234
  - [ ] 15位身份证：110101910101123 → 110101******123
  - [ ] 不足15位：110101910101 → 110101910101（不脱敏）
  - [ ] null输入 → null

#### 银行卡脱敏 (BankCardDesensitizer)
  - [ ] 19位银行卡：6222021234567890123 → 622202********0123
  - [ ] 16位银行卡：6222021234567890 → 622202******7890
  - [ ] 不足16位：622202123456 → 622202123456（不脱敏）
  - [ ] null输入 → null

#### 邮箱脱敏 (EmailDesensitizer)
  - [ ] 标准邮箱：test@example.com → t***@example.com
  - [ ] 短用户名(2字符)：ab@example.com → **@example.com
  - [ ] 单字符用户名：a@example.com → *@example.com
  - [ ] null输入 → null

#### 密码脱敏 (PasswordDesensitizer)
  - [ ] 任意密码：mySecretPassword → *******
  - [ ] 空字符串 → 空字符串
  - [ ] null输入 → null

#### 地址脱敏 (AddressDesensitizer)
  - [ ] 标准地址：北京市朝阳区某某路123号 → 北京市朝阳区***
  - [ ] 仅省市：北京市 → 北京市
  - [ ] null输入 → null

### 验收测试定义 - 日志框架集成

- [ ] Task 1.4: 定义 LogbackIntegrationTest 测试用例
  - [ ] 测试 ConsoleAppender 自动包装后日志脱敏
  - [ ] 测试 FileAppender 自动包装后日志脱敏
  - [ ] 测试 RollingFileAppender 自动包装后日志脱敏
  - [ ] 测试原有日志格式保留
  - [ ] 测试关闭脱敏后日志正常输出

- [ ] Task 1.5: 定义 Log4j2IntegrationTest 测试用例
  - [ ] 测试 ConsoleAppender 自动包装后日志脱敏
  - [ ] 测试 RollingFileAppender 自动包装后日志脱敏
  - [ ] 测试原有日志格式保留

### 验收测试定义 - Spring Boot 集成

- [ ] Task 1.6: 定义 SpringBootIntegrationTest 测试用例
  - [ ] 测试 desensitizer.enabled=true 时脱敏生效
  - [ ] 测试 desensitizer.enabled=false 时脱敏关闭
  - [ ] 测试自定义脱敏规则加载
  - [ ] 测试 @Sensitive 注解类级别应用

### 验收测试 - RED 状态验证

- [ ] Task 1.7: 验证所有测试在实现前处于 RED 状态
  - [ ] 执行 mvn test，确保所有测试编译失败或断言失败
  - [ ] 记录预期失败原因

## Phase 2: 核心模块实现

- [ ] Task 2.1: 实现 desensitizer-core 模块
  - [ ] 实现 SensitiveType 枚举
  - [ ] 实现 @Sensitive 注解
  - [ ] 实现 Desensitizer 接口
  - [ ] 实现 SensitiveDetector SPI 接口
  - [ ] 实现 SensitiveMatch 匹配结果类
  - [ ] 实现 RegexDetector 正则检测器
  - [ ] 实现 DesensitizerRegistry
  - [ ] 实现 DesensitizationEngine 核心引擎

- [ ] Task 2.2: 实现 desensitizer-builtin 模块
  - [ ] 实现 PhoneDesensitizer
  - [ ] 实现 IdCardDesensitizer
  - [ ] 实现 BankCardDesensitizer
  - [ ] 实现 EmailDesensitizer
  - [ ] 实现 PasswordDesensitizer
  - [ ] 实现 AddressDesensitizer

- [ ] Task 2.3: 逐个运行测试，确保 GREEN 状态
  - [ ] 运行 DesensitizerCoreTest
  - [ ] 运行 BuiltinDesensitizersTest
  - [ ] 修复失败的测试

## Phase 3: 集成模块实现

- [ ] Task 3.1: 实现 desensitizer-logback 模块
  - [ ] 实现 DesensitizingLayoutDecorator
  - [ ] 实现 DesensitizingAppender 包装器
  - [ ] 自动检测并包装 ConsoleAppender、FileAppender、RollingFileAppender

- [ ] Task 3.2: 实现 desensitizer-log4j2 模块
  - [ ] 实现 DesensitizingLayoutWrapper
  - [ ] 实现 DesensitizingAppender 包装器

- [ ] Task 3.3: 实现 desensitizer-spring-boot 模块
  - [ ] 实现 DesensitizerProperties
  - [ ] 实现 DesensitizerAutoConfiguration
  - [ ] 实现 Appender 自动包装逻辑

- [ ] Task 3.4: 运行集成测试验证
  - [ ] 运行 LogbackIntegrationTest
  - [ ] 运行 Log4j2IntegrationTest
  - [ ] 运行 SpringBootIntegrationTest
  - [ ] 修复失败的测试

## Phase 4: 最终验证

- [ ] Task 4.1: 执行完整测试套件
  - [ ] mvn clean test
  - [ ] 确保所有测试通过

- [ ] Task 4.2: 验证构建
  - [ ] mvn clean install
  - [ ] 生成可供分发的 JAR 文件

## NLP 扩展预留设计（V2，非本期任务）

- [ ] Task V2.1: 设计 NlpDetector SPI 接口
  - [ ] 定义 NlpDetector 接口，继承 SensitiveDetector
  - [ ] 设计模型加载器接口
  - [ ] 设计离线模型打包规范

- [ ] Task V2.2: 实现本地模型推理
  - [ ] 集成 ONNX Runtime
  - [ ] 实现 ALBERT-Tiny 模型加载
  - [ ] 实现 NER 推理逻辑

- [ ] Task V2.3: 实现 HybridDetector 混合检测
  - [ ] 实现正则优先、NLP 兜底的检测策略
  - [ ] 实现异步批处理优化

## Task Dependencies

```
Phase 1 (测试项目搭建)
├── Task 1.1 (创建测试项目)
├── Task 1.2 (核心引擎测试用例) ──┐
├── Task 1.3 (内置规则测试用例) ──┤
├── Task 1.4 (Logback测试用例) ───┤
├── Task 1.5 (Log4j2测试用例) ────┤
├── Task 1.6 (SpringBoot测试用例)─┤
└── Task 1.7 (RED状态验证) ◄──────┘

Phase 2 (核心实现) ──► Phase 3 (集成实现) ──► Phase 4 (最终验证)
```
