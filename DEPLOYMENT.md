# 脱敏工具项目 - 构建、部署与调试指南

## 1. 项目概述

本项目是一个支持敏感信息脱敏的通用工具，提供对日志、数据库等数据的自动脱敏能力。

### 1.1 项目结构

```
java_desensitizer/
├── desensitizer-core/          # 核心脱敏引擎
├── desensitizer-builtin/       # 内置脱敏器（姓名、手机号、身份证等）
├── desensitizer-logback/       # Logback 日志框架集成
├── desensitizer-log4j2/        # Log4j2 日志框架集成
├── desensitizer-spring-boot/   # Spring Boot 自动配置
├── test-console/               # 测试控制台应用
└── test-projects/              # 测试项目集合
```

### 1.2 核心功能

| 模块 | 功能 |
|------|------|
| core | 脱敏引擎核心、规则配置、监控统计 |
| builtin | 7种内置脱敏器（姓名、手机号、身份证、银行卡、邮箱、密码、地址） |
| logback | Logback Appender 集成，自动脱敏日志 |
| log4j2 | Log4j2 Appender 集成 |
| spring-boot | Spring Boot 自动配置、测试控制台 |

---

## 2. 环境要求

### 2.1 依赖版本

| 依赖 | 版本 | 说明 |
|------|------|------|
| Java | 17+ | 建议使用 OpenJDK 17 |
| Maven | 3.8+ | 构建工具 |
| Spring Boot | 2.7.x | 测试控制台使用 |

### 2.2 环境变量配置

```bash
# 设置 Java 环境（如果未全局配置）
export JAVA_HOME=/path/to/java17
export PATH=$JAVA_HOME/bin:$PATH

# 设置 Maven 环境
export MAVEN_HOME=/path/to/maven
export PATH=$MAVEN_HOME/bin:$PATH
```

---

## 3. 构建步骤

### 3.1 完整构建

```bash
# 进入项目根目录
cd /Users/bigxian/trae_projects/java_desensitizer

# 清理并构建整个项目（跳过测试）
mvn clean install -DskipTests -s settings.xml

# 构建并运行测试
mvn clean install -s settings.xml
```

### 3.2 单独构建某个模块

```bash
# 只构建核心模块
mvn clean install -pl desensitizer-core -am -DskipTests -s settings.xml

# 只构建 Spring Boot 模块
mvn clean install -pl desensitizer-spring-boot -am -DskipTests -s settings.xml
```

### 3.3 构建参数说明

| 参数 | 说明 |
|------|------|
| `-DskipTests` | 跳过单元测试，加速构建 |
| `-pl <module>` | 指定要构建的模块 |
| `-am` | 同时构建依赖模块 |
| `-s settings.xml` | 指定 Maven 配置文件 |
| `-q` | 静默模式，减少输出 |

### 3.4 构建产物

构建成功后，各模块的 JAR 包位于：
- `desensitizer-core/target/desensitizer-core-1.0.0-SNAPSHOT.jar`
- `desensitizer-builtin/target/desensitizer-builtin-1.0.0-SNAPSHOT.jar`
- `desensitizer-spring-boot/target/desensitizer-spring-boot-1.0.0-SNAPSHOT.jar`

---

## 4. 部署方式

### 4.1 运行测试控制台（开发模式）

```bash
# 进入项目根目录
cd /Users/bigxian/trae_projects/java_desensitizer

# 启动测试控制台
mvn spring-boot:run -pl test-console -DskipTests -s settings.xml
```

### 4.2 打包为可执行 JAR

```bash
# 打包测试控制台
mvn clean package -pl test-console -DskipTests -s settings.xml

# 运行打包后的 JAR（位于 test-console/target/）
java -jar test-console/target/test-console-1.0.0-SNAPSHOT.jar
```

### 4.3 作为依赖引入其他项目

```xml
<!-- pom.xml 中添加依赖 -->
<dependencies>
    <!-- 核心脱敏引擎 -->
    <dependency>
        <groupId>com.desensitizer</groupId>
        <artifactId>desensitizer-core</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    
    <!-- 内置脱敏器 -->
    <dependency>
        <groupId>com.desensitizer</groupId>
        <artifactId>desensitizer-builtin</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
    
    <!-- Spring Boot 自动配置 -->
    <dependency>
        <groupId>com.desensitizer</groupId>
        <artifactId>desensitizer-spring-boot</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

---

## 5. 启动与验证

### 5.1 启动服务

测试控制台启动后，默认监听端口 **8080**：

```bash
# 启动日志示例
...
Tomcat started on port(s): 8080 (http) 
Started TestConsoleApplication in 0.8 seconds
...
```

### 5.2 健康检查

```bash
# 检查服务是否正常运行
curl http://localhost:8080/desensitizer/health
```

响应示例：
```json
{
    "status": "UP",
    "timestamp": "2026-05-10T12:00:00Z"
}
```

### 5.3 访问测试报告

**HTML 报告**（推荐）：
```
http://localhost:8080/desensitizer/report/html
```

**JSON 报告**：
```bash
curl http://localhost:8080/desensitizer/report
```

### 5.4 测试脱敏接口

```bash
# 测试单个文本脱敏
curl "http://localhost:8080/desensitizer/test?text=张三 13800138000"

# 测试 Excel 数据批量脱敏
curl http://localhost:8080/desensitizer/test/excel

# 测试批量脱敏
curl http://localhost:8080/desensitizer/test/batch
```

---

## 6. 调试方法

### 6.1 查看日志

**应用日志**：
```bash
tail -f /Users/bigxian/trae_projects/java_desensitizer/test-console/logs/desensitizer.log
```

**脱敏监控日志**：
```bash
# 查看脱敏统计
curl http://localhost:8080/desensitizer/monitor/metrics
```

### 6.2 常用 API 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/desensitizer/health` | GET | 健康检查 |
| `/desensitizer/report` | GET | JSON 格式测试报告 |
| `/desensitizer/report/html` | GET | HTML 格式测试报告 |
| `/desensitizer/test?text=xxx` | GET | 测试单个文本脱敏 |
| `/desensitizer/test/excel` | GET | 测试 Excel 数据批量脱敏 |
| `/desensitizer/test/batch` | GET | 批量测试 |
| `/desensitizer/test/cases` | GET | 查看测试用例列表 |
| `/desensitizer/test/failed-cases` | GET | 查看失败用例 |

### 6.3 调整测试延迟

默认情况下，Excel 测试会在每条数据间添加 50ms 延迟，避免日志刷屏：

```java
// DesensitizerConsoleController.java - testExcelDataDesensitization() 方法
logger.info("Excel测试数据[" + (i + 1) + "]: " + logString);
Thread.sleep(50);  // 可根据需要调整或移除
```

**移除延迟以提高测试速度**：
```java
// 移除 Thread.sleep(50) 行
logger.info("Excel测试数据[" + (i + 1) + "]: " + logString);
```

### 6.4 性能测试

```bash
# 测试 Excel 数据处理性能
curl -s "http://localhost:8080/desensitizer/test/excel" | python3 -c "
import json, time
start = time.time()
data = json.load(open('/dev/stdin'))
elapsed = time.time() - start
print(f'处理量: {data[\"totalIncrease\"]}')
print(f'耗时: {elapsed:.2f}s')
print(f'吞吐量: {data[\"totalIncrease\"]/elapsed:.1f} 条/秒')
"
```

---

## 7. 测试验证

### 7.1 运行单元测试

```bash
# 运行所有单元测试
mvn test -pl test-projects/test-core -s settings.xml

# 运行特定测试类
mvn test -pl test-projects/test-core -Dtest=BuiltinDesensitizersTest -s settings.xml
```

### 7.2 测试用例说明

项目包含 **6715 条** 测试用例，来自 Excel 测试数据文件：

| 敏感类型 | 用例数 | 说明 |
|----------|--------|------|
| NAME | 1343 | 姓名脱敏测试 |
| PHONE | 1343 | 手机号脱敏测试 |
| ID_CARD | 1343 | 身份证号脱敏测试 |
| BANK_CARD | 1343 | 银行卡号脱敏测试 |
| ADDRESS | 1343 | 地址脱敏测试 |
| **总计** | **6715** | |

### 7.3 测试数据文件

测试数据位于：
```
desensitizer-spring-boot/src/main/resources/test-data/
└── 赛题4-支持敏感信息脱敏的通用工具-测试数据v0.3.xlsx
```

---

## 8. 基于 Excel 的测试流程

### 8.1 Excel 文件结构

测试数据文件包含 **12 列**：

| 列号 | 字段 | 说明 |
|------|------|------|
| 1-6 | `name/phone/idCard/bankCard/address/country` | **原始数据** |
| 7-12 | `xxxDesensitized` | **预期脱敏结果** |

### 8.2 测试执行机制

**重要说明**：**应用启动时不会自动加载 Excel 进行测试！**

#### 8.2.1 Excel 加载时机

Excel 文件在**首次访问相关 API 时**才会加载：

```
启动日志示例：
Started TestConsoleApplication in 0.8 seconds  ← 只初始化，不加载Excel

首次访问 /report 时：
Loading 1345 rows from Excel file                ← 此时才加载
Successfully loaded 1343 log entries
```

#### 8.2.2 测试触发方式

| 触发方式 | 说明 |
|----------|------|
| **访问 HTML 报告** | `http://localhost:8080/desensitizer/report/html` |
| **调用 Excel 测试 API** | `curl http://localhost:8080/desensitizer/test/excel` |
| **调用批量测试 API** | `curl http://localhost:8080/desensitizer/test/batch` |

#### 8.2.3 测试数据转换

每一行 Excel 数据会被转换为 **5 条字段级测试用例**：

```
1343 行 × 5 个敏感字段 = 6715 条测试用例
```

### 8.3 测试执行步骤

#### 步骤 1：启动测试控制台

```bash
cd /Users/bigxian/trae_projects/java_desensitizer
mvn spring-boot:run -pl test-console -s settings.xml
```

#### 步骤 2：手动触发测试

```bash
# 方式1：访问可视化报告（推荐）
open http://localhost:8080/desensitizer/report/html

# 方式2：调用 API
curl http://localhost:8080/desensitizer/test/excel
```

#### 步骤 3：查看测试结果

```json
{
    "totalRows": 1343,
    "desensitizedRows": 1343,
    "beforeCount": 10,
    "afterCount": 2696,
    "totalIncrease": 2686,
    "success": true
}
```

### 8.4 配置启动时自动测试

如需在启动时自动执行测试，可添加 `@PostConstruct` 方法：

```java
// 在 DesensitizerConsoleController.java 中添加
@PostConstruct
public void init() {
    logger.info("=== 启动时自动执行脱敏测试 ===");
    
    // 计算准确率
    getAccuracyMetrics();
    
    // 执行 Excel 数据测试
    testExcelDataDesensitization();
    
    logger.info("=== 自动测试完成 ===");
}
```

### 8.5 测试流程图

```
应用启动
    │
    ▼
初始化完成（不加载Excel）
    │
    ├──► [手动] 访问 /report/html ──► 加载Excel → 计算准确率
    │
    ├──► [手动] 访问 /test/excel ──► 加载Excel → 执行脱敏
    │
    └──► [可选] @PostConstruct ──► 启动时自动执行
```

---

## 9. 常见问题

### 9.1 构建失败

**问题**：Maven 依赖下载失败
```bash
# 解决：检查网络或配置代理
# 编辑 settings.xml 添加镜像或代理配置
```

**问题**：Java 版本不兼容
```bash
# 解决：确认 Java 版本
java -version  # 应显示 17.x
```

### 9.2 启动失败

**问题**：端口被占用
```bash
# 查找占用进程
lsof -i :8080

# 杀死进程
kill -9 <PID>
```

**问题**：Excel 文件找不到
```bash
# 确认文件存在
ls desensitizer-spring-boot/src/main/resources/test-data/
```

### 9.3 脱敏结果不符合预期

**问题**：准确率低于预期
```bash
# 查看失败用例
curl http://localhost:8080/desensitizer/test/failed-cases

# 检查脱敏规则配置
# 规则文件位置：classpath:META-INF/sensitive-rules.json
```

---

## 9. 配置说明

### 9.1 application.yml 关键配置

```yaml
# test-console/src/main/resources/application.yml
server:
  port: 8080

desensitizer:
  enabled: true
  monitor:
    enabled: true
    log-path: logs/desensitizer.log

spring:
  application:
    name: desensitizer-test-console
```

### 9.2 脱敏器配置

脱敏规则通过 `sensitive-rules.json` 配置，支持自定义脱敏模式。

---

## 10. CI/CD 建议

### 10.1 构建脚本

```bash
#!/bin/bash
# build.sh
set -e

echo "=== 开始构建脱敏工具项目 ==="

# 检查环境
java -version
mvn -version

# 清理构建
mvn clean install -DskipTests -s settings.xml -q

echo "=== 构建完成 ==="
ls -la */target/*.jar
```

### 10.2 部署脚本

```bash
#!/bin/bash
# deploy.sh
set -e

echo "=== 停止旧服务 ==="
pkill -f test-console-1.0.0-SNAPSHOT.jar || true
sleep 2

echo "=== 启动新服务 ==="
nohup java -jar test-console/target/test-console-1.0.0-SNAPSHOT.jar > /dev/null 2>&1 &

echo "=== 服务已启动 ==="
sleep 5
curl -s http://localhost:8080/desensitizer/health
```

---

## 附录：命令速查

| 操作 | 命令 |
|------|------|
| 完整构建 | `mvn clean install -DskipTests -s settings.xml` |
| 启动测试控制台 | `mvn spring-boot:run -pl test-console -s settings.xml` |
| 查看报告 | `curl http://localhost:8080/desensitizer/report` |
| 查看日志 | `tail -f test-console/logs/desensitizer.log` |
| 运行单元测试 | `mvn test -pl test-projects/test-core -s settings.xml` |

---

**文档版本**：v1.0  
**生成日期**：2026-05-17  
**项目版本**：1.0.0-SNAPSHOT