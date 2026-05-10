# Java 日志脱敏工具

## 1. 项目简介

Java 日志脱敏工具是一个用于日志脱敏的 Java 依赖组件，支持自动识别和脱敏日志中的敏感信息，如手机号、身份证、银行卡、邮箱、密码、地址、姓名等。

## 2. 核心特性

- **自动脱敏**：配置后，打日志时自动脱敏，接口返回时不需要脱敏
- **多日志框架支持**：支持 Logback 和 Log4j2
- **细粒度配置**：支持按名称、类型指定需要脱敏的 Appender，按文件路径或模式指定需要脱敏的日志文件
- **内置脱敏规则**：支持手机号、身份证、银行卡、邮箱、密码、地址、姓名等类型的脱敏
- **多种脱敏方式**：支持注解方式、Map 字段映射、字符串自动探测等多种脱敏方式
- **监控与统计**：提供脱敏次数、错误次数和类型分布统计
- **NLP 可扩展点**：预留 NLP 敏感信息识别能力，支持本地部署模型文件

## 3. 项目结构

- **desensitizer-core**：核心脱敏引擎，提供脱敏器接口、处理器和配置加载
- **desensitizer-builtin**：内置脱敏规则，支持手机号、身份证、银行卡、邮箱、密码、地址、姓名等类型
- **desensitizer-logback**：Logback 日志框架集成，包含 SensitiveLogger 和消息转换器
- **desensitizer-log4j2**：Log4j2 日志框架集成
- **desensitizer-spring-boot**：Spring Boot 自动配置
- **test-projects**：测试工程目录
  - **test-core**：核心测试应用，用于验证核心功能和集成测试
  - **test-project-1**：Spring Boot 2.7.x + JDK 8 + Logback
  - **test-project-2**：Spring Boot 3.2.x + JDK 17 + Log4j2
  - **test-project-3**：传统 Java 项目 + JDK 11 + 混合日志框架

## 4. 快速开始

### 4.1 基于源码构建与安装

如果用户拿到项目源码，需要先构建部署二方包，然后再引用使用。具体步骤如下：

1. **克隆项目源码**
   ```bash
   git clone <项目仓库地址>
   cd java_desensitizer
   ```

2. **构建项目**
   ```bash
   mvn -s settings.xml clean install
   ```
   这会将项目构建并安装到本地 Maven 仓库。

3. **部署到 Maven 私服（可选）**
   如果需要部署到公司内部的 Maven 私服，需要修改 `pom.xml` 中的发布配置，添加以下内容：
   
   ```xml
   <distributionManagement>
       <repository>
           <id>maven-releases</id>
           <name>Maven Releases Repository</name>
           <url>https://your-nexus-server/repository/maven-releases/</url>
       </repository>
       <snapshotRepository>
           <id>maven-snapshots</id>
           <name>Maven Snapshots Repository</name>
           <url>https://your-nexus-server/repository/maven-snapshots/</url>
       </snapshotRepository>
   </distributionManagement>
   ```
   
   然后在 `settings.xml` 文件中添加私服的认证信息：
   
   ```xml
   <servers>
       <server>
           <id>maven-releases</id>
           <username>your-username</username>
           <password>your-password</password>
       </server>
       <server>
           <id>maven-snapshots</id>
           <username>your-username</username>
           <password>your-password</password>
       </server>
   </servers>
   ```
   
   最后执行部署命令：
   ```bash
   mvn -s settings.xml clean deploy
   ```

### 4.2 引用依赖

在项目的 `pom.xml` 文件中添加以下依赖：

```xml
<dependency>
    <groupId>com.desensitizer</groupId>
    <artifactId>desensitizer-spring-boot</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

或者单独引用 Logback 模块：

```xml
<dependency>
    <groupId>com.desensitizer</groupId>
    <artifactId>desensitizer-logback</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 5. 使用方式

### 5.1 Spring Boot 配置

在 `application.yml` 中添加配置：

#### 5.1.1 默认配置

如果不进行任何配置，脱敏工具会自动启用并对所有日志进行脱敏处理，具体行为如下：

- **全局启用**：脱敏功能默认开启，包括正则表达式匹配
- **日志框架集成**：默认同时启用 Logback 和 Log4j2 集成
- **Appender 覆盖**：默认对所有 Appender 应用脱敏规则，不排除任何 Appender
- **文件覆盖**：默认对所有日志文件应用脱敏规则，不排除任何日志文件
- **脱敏规则**：默认对所有 Appender 和日志文件应用所有内置脱敏规则（手机号、身份证、银行卡、邮箱、密码、地址、姓名）
- **监控功能**：默认禁用监控统计功能

#### 5.1.2 自定义配置

在 `application.yml` 中添加配置：

```yaml
desensitizer:
  enabled: true
  regex:
    enabled: true
  log:
    enabled: true
    logback: true
    log4j2: true
  appenders:
    include:
      - name: CONSOLE
      - type: FILE
    exclude:
      - name: ERROR
  files:
    include:
      - pattern: "**/app.log"
    exclude:
      - pattern: "**/error.log"
  rules:
    appenders:
      CONSOLE:
        types:
          - PHONE
          - ID_CARD
    files:
      "**/app.log":
        types:
          - EMAIL
          - BANK_CARD
  monitoring:
    enabled: true
    statsInterval: 60
```

### 5.2 传统 Java 项目

#### 5.2.1 Logback 配置

在 `logback.xml` 中添加：

```xml
<appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
        <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>

<appender name="DESENSITIZED_CONSOLE" class="com.desensitizer.logback.DesensitizingLogbackAppender">
    <appender-ref ref="CONSOLE" />
</appender>

<root level="info">
    <appender-ref ref="DESENSITIZED_CONSOLE" />
</root>
```

#### 5.2.2 Log4j2 配置

在 `log4j2.xml` 中添加：

```xml
<Appenders>
    <Console name="CONSOLE" target="SYSTEM_OUT">
        <PatternLayout pattern="%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n" />
    </Console>

    <DesensitizingLog4j2Appender name="DESENSITIZED_CONSOLE">
        <AppenderRef ref="CONSOLE" />
    </DesensitizingLog4j2Appender>
</Appenders>

<Loggers>
    <Root level="info">
        <AppenderRef ref="DESENSITIZED_CONSOLE" />
    </Root>
</Loggers>
```

### 5.3 注解方式脱敏

通过 `@Sensitive` 注解标记需要脱敏的字段：

```java
public class User {
    
    @Sensitive(type = SensitiveType.NAME)
    private String name;
    
    @Sensitive(type = SensitiveType.PHONE)
    private String phone;
    
    @Sensitive(type = SensitiveType.ID_CARD)
    private String idCard;
    
    private String address;
}
```

使用 `SensitiveLog` 进行脱敏：

```java
User user = new User("张三", "13800138000", "110101199001011234", "北京市");
String desensitized = SensitiveLog.desensitize(user).toString();
// 输出: User{name='张*', phone='138****8000', idCard='110101********1234', address='北京市'}
```

### 5.4 Map 字段映射脱敏

通过 YAML 配置文件 `sensitive-config.yaml` 定义字段名到敏感类型的映射：

```yaml
sensitive:
  enabled: true
  rules:
    - type: PHONE
      fieldNames:
        - phoneNo
        - sjh
        - phone
    - type: NAME
      fieldNames:
        - name
        - xm
        - 姓名
```

使用 `SensitiveLog` 进行脱敏：

```java
Map<String, Object> map = new HashMap<>();
map.put("phone", "13800138000");
map.put("name", "李四");

Map<String, Object> desensitized = SensitiveLog.desensitize(map);
```

### 5.5 字符串自动探测脱敏

直接对字符串进行脱敏，系统会自动识别敏感信息：

```java
String content = "联系人: 张三, 电话: 13800138000, 身份证: 110101199001011234";
String desensitized = SensitiveLog.desensitize(content);
// 输出: 联系人: 张*, 电话: 138****8000, 身份证: 110101********1234
```

### 5.6 使用 SensitiveLogger

`SensitiveLogger` 是对 SLF4J Logger 的包装，会自动对日志参数进行脱敏：

```java
import com.desensitizer.logback.SensitiveLoggerFactory;
import org.slf4j.Logger;

public class Service {
    
    private static final Logger logger = SensitiveLoggerFactory.getSLF4JLogger(Service.class);
    
    public void process(User user) {
        // 自动脱敏对象参数
        logger.info("用户信息: {}", user);
        
        // 自动脱敏字符串参数
        logger.info("手机号: {}, 姓名: {}", "13800138000", "张三");
        
        // 自动脱敏 Map 参数
        Map<String, Object> data = new HashMap<>();
        data.put("phone", "13900139000");
        logger.info("数据: {}", data);
    }
}
```

### 5.7 Logback 消息转换器

使用 `SensitiveMessageConverter` 对日志消息进行脱敏：

```xml
<encoder>
    <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %sensitiveMsg%n</pattern>
    <conversionRule conversionWord="sensitiveMsg" 
                    converterClass="com.desensitizer.logback.SensitiveMessageConverter" />
</encoder>
```

## 6. 脱敏规则

| 类型 | 说明 | 示例 | 脱敏结果 |
|------|------|------|----------|
| PHONE | 手机号 | 13812345678 | 138****5678 |
| ID_CARD | 身份证号 | 110101199001011234 | 110101********1234 |
| BANK_CARD | 银行卡号 | 6222021234567890123 | 622202**********0123 |
| EMAIL | 邮箱 | user@example.com | u***@example.com |
| PASSWORD | 密码 | password123 | ********** |
| ADDRESS | 地址 | 北京市朝阳区建国路88号 | 北京市朝阳区**** |
| NAME | 姓名 | 张三 | 张* |

## 7. 配置文件说明

### 7.1 sensitive-config.yaml

```yaml
sensitive:
  enabled: true
  rules:
    - type: PHONE
      fieldNames:
        - phoneNo
        - sjh
        - phone
        - 手机号
    - type: ID_CARD
      fieldNames:
        - idCard
        - sfz
        - identityCard
        - 身份证
    - type: NAME
      fieldNames:
        - name
        - xm
        - 姓名
        - 联系人
```

## 8. 监控与统计

### 8.1 启用监控

在 `application.yml` 中启用监控：

```yaml
desensitizer:
  monitoring:
    enabled: true  # 启用监控
    statsInterval: 60  # 统计间隔（秒）
```

### 8.2 监控内容

启用监控后，系统会定期输出脱敏统计信息，包括：

- 脱敏次数：总共脱敏的次数
- 错误次数：脱敏过程中发生错误的次数
- 各类型脱敏分布：不同类型敏感信息的脱敏次数

### 8.3 查看监控结果

监控信息会通过日志输出，默认使用 `INFO` 级别，输出到应用的日志系统中，具体输出位置取决于应用的日志配置：

- 如果应用配置了控制台输出（如 Logback 的 ConsoleAppender），监控信息会输出到标准输出
- 如果应用配置了文件输出（如 Logback 的 FileAppender），监控信息会输出到指定的日志文件
- 如果应用配置了其他输出目标（如数据库、消息队列等），监控信息会输出到相应的目标

示例输出：

```
INFO  c.d.core.monitor.DesensitizationMonitor - Desensitization stats: total=100, errors=0, types={PHONE=40, ID_CARD=30, BANK_CARD=20, EMAIL=10, NAME=10}
```

## 9. 测试工程

项目包含三个测试工程，用于验证不同环境下的脱敏功能：

1. **test-project-1**：Spring Boot 2.7.x + JDK 8 + Logback
2. **test-project-2**：Spring Boot 3.2.x + JDK 17 + Log4j2
3. **test-project-3**：传统 Java 项目 + JDK 11 + 混合日志框架

## 10. 注意事项

- **NLP 模型**：NLP 模型需要下载到本地，使用过程中不能联网下载或更新，因为最终的应用环境是隔绝互联网的
- **性能考虑**：脱敏操作会增加日志处理的开销，建议根据实际情况调整配置
- **自定义规则**：可以通过实现 `Desensitizer` 接口来自定义脱敏规则
- **配置文件**：`sensitive-config.yaml` 需放置在 classpath 根目录下

## 11. 许可证

MIT License
