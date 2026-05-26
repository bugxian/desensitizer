# Java Desensitizer 项目规则

日志敏感信息脱敏工具，通过包装 Logback Appender 自动脱敏（姓名、手机号、身份证、银行卡、邮箱、密码、地址）。

## 项目结构

- `desensitizer-core` - 核心脱敏引擎、监控统计
- `desensitizer-builtin` - 7种内置脱敏器
- `desensitizer-logback` / `desensitizer-log4j2` - 日志框架集成
- `desensitizer-spring-boot` - Spring Boot 自动配置、测试控制台
- `test-projects/test-core` - 单元测试
- `test-console` - Spring Boot 测试应用（入口，端口8080）

## 构建

```bash
mvn clean install -DskipTests -s settings.xml          # 完整构建
mvn clean install -pl desensitizer-spring-boot -am -DskipTests -s settings.xml  # 单模块构建
```

**重要**：修改代码后必须 `mvn clean install`，不能只用 `mvn compile`，因为 test-console 通过 Maven 依赖引用其他模块 jar。

## 单元测试

```bash
mvn test -pl test-projects/test-core -s settings.xml
mvn test -pl test-projects/test-core -Dtest=BuiltinDesensitizersTest -s settings.xml
```

## 启动与测试

```bash
mvn spring-boot:run -pl test-console -DskipTests -s settings.xml  # 启动
lsof -ti:8080 | xargs kill -9                                      # 停止
curl http://localhost:8080/desensitizer/test/excel                  # 触发Excel测试(1343行×5字段=6715用例)
```

浏览器访问 `http://localhost:8080/desensitizer/report/html` 查看报告。启动时不会自动加载Excel，需手动触发。

## 关键架构

- **Appender包装**：CONSOLE和FILE两个Appender都被包装，但只有第一个(CONSOLE)的`recordToMetrics=true`，避免重复计数
- **递归防护**：`append()`中精确匹配框架内部logger name跳过（不能用`contains("Desensitizer")`，Controller包名也包含）
- **监控暂停**：`getReport()`生成报告时暂停监控，但`getReportHtml()`中`generateHtmlReport()`在恢复后执行
- **性能指标**：`totalProcessingTime`以纳秒存储，计算时转毫秒保留4位小数；吞吐量基于滑动窗口(最近1分钟)

## 关键文件

- `desensitizer-core/.../DesensitizationEngine.java` - 脱敏引擎
- `desensitizer-core/.../DesensitizationMonitor.java` - 监控统计
- `desensitizer-spring-boot/.../DesensitizerAutoConfiguration.java` - 自动配置+AppenderWrapper
- `desensitizer-spring-boot/.../DesensitizerConsoleController.java` - REST控制器+报告生成

## 项目实时进度
每次执行任务前阅读 .trae/custom/progress.md，了解已完成、进行中、待办任务。每完成一轮会话更新此文件中本次任务的完成情况。