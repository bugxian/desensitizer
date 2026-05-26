# Checklist - Java 日志脱敏组件 (TDD 验收清单)

## Phase 1: 测试项目搭建与验收测试定义

### Task 1.1: 测试项目创建

- [ ] desensitizer-test-app/pom.xml 正确配置 Spring Boot 2.7.x 依赖
- [ ] JUnit 5 + AssertJ 测试框架依赖正确
- [ ] spring-boot-starter-logging 依赖正确
- [ ] logback-classic 依赖正确
- [ ] log4j-api 依赖正确
- [ ] 项目目录结构符合 Maven 标准

### Task 1.2: DesensitizerCoreTest 测试用例定义

- [ ] 测试方法 `desensitizePhone()` 正确定义，断言 13812345678 → 138****5678
- [ ] 测试方法 `desensitizeIdCard()` 正确定义，断言身份证脱敏
- [ ] 测试方法 `desensitizeBankCard()` 正确定义，断言银行卡脱敏
- [ ] 测试方法 `desensitizeEmail()` 正确定义，断言邮箱脱敏
- [ ] 测试方法 `desensitizePassword()` 正确定义，断言密码脱敏
- [ ] 测试方法 `desensitizeAddress()` 正确定义，断言地址脱敏
- [ ] 测试方法 `desensitizeNull()` 正确定义，断言 null 输入返回 null
- [ ] 测试方法 `desensitizeEmptyString()` 正确定义，断言空字符串返回空字符串
- [ ] 测试方法 `desensitizeInvalidFormat()` 正确定义，断言无效格式原样返回
- [ ] 测试方法 `desensitizeNestedObject()` 正确定义，断言嵌套对象递归脱敏

### Task 1.3: BuiltinDesensitizersTest 测试用例定义

#### 手机号脱敏测试
- [ ] 11位手机号测试用例正确定义
- [ ] 不足11位测试用例正确定义
- [ ] 不足7位测试用例正确定义
- [ ] null 输入测试用例正确定义
- [ ] 空字符串测试用例正确定义
- [ ] 非手机号格式测试用例正确定义

#### 身份证脱敏测试
- [ ] 18位身份证测试用例正确定义
- [ ] 15位身份证测试用例正确定义
- [ ] 不足15位测试用例正确定义
- [ ] null 输入测试用例正确定义

#### 银行卡脱敏测试
- [ ] 19位银行卡测试用例正确定义
- [ ] 16位银行卡测试用例正确定义
- [ ] 不足16位测试用例正确定义
- [ ] null 输入测试用例正确定义

#### 邮箱脱敏测试
- [ ] 标准邮箱测试用例正确定义
- [ ] 短用户名邮箱测试用例正确定义
- [ ] 单字符用户名邮箱测试用例正确定义
- [ ] null 输入测试用例正确定义

#### 密码脱敏测试
- [ ] 密码测试用例正确定义
- [ ] 空字符串测试用例正确定义
- [ ] null 输入测试用例正确定义

#### 地址脱敏测试
- [ ] 标准地址测试用例正确定义
- [ ] 仅省市地址测试用例正确定义
- [ ] null 输入测试用例正确定义

### Task 1.4: LogbackIntegrationTest 测试用例定义

- [ ] ConsoleAppender 脱敏测试用例正确定义
- [ ] FileAppender 脱敏测试用例正确定义
- [ ] RollingFileAppender 脱敏测试用例正确定义
- [ ] 日志格式保留测试用例正确定义
- [ ] 脱敏关闭测试用例正确定义

### Task 1.5: Log4j2IntegrationTest 测试用例定义

- [ ] ConsoleAppender 脱敏测试用例正确定义
- [ ] RollingFileAppender 脱敏测试用例正确定义
- [ ] 日志格式保留测试用例正确定义

### Task 1.6: SpringBootIntegrationTest 测试用例定义

- [ ] 启用脱敏测试用例正确定义
- [ ] 关闭脱敏测试用例正确定义
- [ ] 自定义规则加载测试用例正确定义
- [ ] 类级别注解测试用例正确定义

### Task 1.7: RED 状态验证

- [ ] mvn test 执行后所有测试编译失败或断言失败
- [ ] 预期失败原因已记录

## Phase 2: 核心模块实现

### Task 2.1: desensitizer-core 模块实现

- [ ] SensitiveType 枚举编译通过，包含所有定义的类型
- [ ] @Sensitive 注解编译通过，属性定义正确
- [ ] Desensitizer 接口编译通过
- [ ] SensitiveDetector SPI 接口编译通过
- [ ] SensitiveMatch 匹配结果类编译通过
- [ ] RegexDetector 正则检测器编译通过
- [ ] DesensitizerRegistry 实现编译通过
- [ ] DesensitizationEngine 核心引擎编译通过

### Task 2.2: desensitizer-builtin 模块实现

- [ ] PhoneDesensitizer 编译通过
- [ ] IdCardDesensitizer 编译通过
- [ ] BankCardDesensitizer 编译通过
- [ ] EmailDesensitizer 编译通过
- [ ] PasswordDesensitizer 编译通过
- [ ] AddressDesensitizer 编译通过

### Task 2.3: GREEN 状态验证

- [ ] DesensitizerCoreTest 所有测试通过
- [ ] BuiltinDesensitizersTest 所有测试通过

## Phase 3: 集成模块实现

### Task 3.1: desensitizer-logback 模块实现

- [ ] DesensitizingLayoutDecorator 编译通过
- [ ] DesensitizingAppender 编译通过
- [ ] ConsoleAppender 自动包装功能正常
- [ ] FileAppender 自动包装功能正常
- [ ] RollingFileAppender 自动包装功能正常

### Task 3.2: desensitizer-log4j2 模块实现

- [ ] DesensitizingLayoutWrapper 编译通过
- [ ] DesensitizingAppender 编译通过
- [ ] ConsoleAppender 自动包装功能正常
- [ ] RollingFileAppender 自动包装功能正常

### Task 3.3: desensitizer-spring-boot 模块实现

- [ ] DesensitizerProperties 编译通过
- [ ] DesensitizerAutoConfiguration 编译通过
- [ ] Appender 自动包装逻辑正常
- [ ] spring.factories 或 META-INF 配置正确

### Task 3.4: GREEN 状态验证

- [ ] LogbackIntegrationTest 所有测试通过
- [ ] Log4j2IntegrationTest 所有测试通过
- [ ] SpringBootIntegrationTest 所有测试通过

## Phase 4: 最终验证

### Task 4.1: 完整测试套件

- [ ] mvn clean test 全部通过
- [ ] 无测试跳过
- [ ] 无测试失败

### Task 4.2: 构建验证

- [ ] mvn clean install 成功
- [ ] 所有 JAR 文件正确生成
- [ ] 生成的 JAR 文件可被其他项目依赖

## NLP 扩展预留检查点（V2，非本期）

### Task V2.1: NlpDetector SPI 接口设计
- [ ] NlpDetector 接口继承 SensitiveDetector
- [ ] 模型加载器接口设计合理
- [ ] 离线模型打包规范文档完整

### Task V2.2: 本地模型推理实现
- [ ] ONNX Runtime 集成正确
- [ ] ALBERT-Tiny 模型加载正常
- [ ] NER 推理结果准确

### Task V2.3: HybridDetector 混合检测实现
- [ ] 正则优先策略正确实现
- [ ] NLP 兜底检测正确实现
- [ ] 异步批处理性能达标

## 离线部署验证

- [ ] NLP 模型文件可打包到应用 JAR 中
- [ ] 模型路径支持 classpath: 前缀
- [ ] 模型路径支持 file: 前缀
- [ ] 运行时禁止联网下载模型（网络完全隔离环境下正常运行）
