# 🚀 Lio RPC 1.0.0

> 简洁、高效、可扩展的高性能 RPC 框架，适用于微服务架构下的服务通信场景。

[![Build Status](https://img.shields.io/badge/build-passing-green)](https://github.com/PsvmMan/lio-rpc)
[![License](https://img.shields.io/github/license/PsvmMan/lio-rpc.svg)](LICENSE)

---

## 📌 目录

1. [简介](#简介)
2. [核心特性](#核心特性)
3. [整体架构](#整体架构)
4. [模块划分](#模块划分)
5. [快速入门](#快速入门)
6. [功能详解](#功能详解)
7. [设计理念](#设计理念)
8. [开发计划](#开发计划)
9. [贡献代码](#贡献代码)
10. [许可证](#许可证)
11. [联系方式](#联系方式)
12. [致谢](#致谢)

---

## 简介

Lio RPC 是一个轻量级但功能强大的 Java 远程过程调用（Remote Procedure Call）框架，旨在为开发者提供高性能、高可用性以及低侵入性的服务间通信解决方案。该框架特别适用于构建分布式系统和微服务架构，提供了丰富的功能支持和服务治理能力。

该项目是我个人学习与实践过程中逐步构建的 RPC 框架，目前具备基础功能并持续迭代中。

---

## 核心特性

- ✅ 调用方式：同步调用、异步回调、单向调用、多路竞速调用
- ✅ 参数配置：配置文件(全局参数配置)、注解(接口级参数配置、方法级参数配置)
- ✅ 通信协议：提供了性能高效的lio通信协议，并且支持自定义扩展通信协议
- ✅ 序列化：提供了 Hessian、Kryo、JDK 等序列化方式，并且支持自定义扩展序列化方式
- ✅ 解压缩：对大数据场景提供了 gzip、zstd 等序列化方式，并且支持自定义扩展解压缩方式
- ✅ 负载均衡：提供了加权随机、一致性哈希等负载均衡策略，并且支持自定义扩展负载均衡策略
- ✅ 服务注册与发现：提供了 Zookeeper、Nacos 注册中心插件，并且支持自定义扩展注册中心插件
- ✅ 服务上下线动态感知：实时监控服务状态变化，确保服务列表的准确性与时效性。
- ✅ 客户端容错机制：支持降级处理、失败重试
- ✅ 流量控制：客户端支持滑动窗口、漏桶、令牌桶等限流方式，并且支持自定义扩展流量控制方式
- ✅ 集群策略：支持快速失败、失败重试、多路竞速等集群策略，并且支持自定义扩展集群策略
- ✅ 业务线程池：支持多业务线程池，保证业务处理隔离，并且支持自定义扩展业务线程池
- ✅ 高性能 IO：基于 Netty 实现异步非阻塞通信
- ✅ 可插拔设计：提供SPI机制，使系统功能模块解耦，易于二次开发与扩展
- ✅ 提供了对 Spring 和 Spring Boot 的原生集成支持

---

## 整体架构

![架构图](./docs/image/RPC.png)

该框架采用典型的 RPC 分层架构，主要包括以下几个部分：

- **注册中心**：服务注册与发现，主要作用就是发布服务、订阅服务、通知更新服务
- **消费者**：以接口为维度，订阅多注册中心的服务，并且可以获取实时的服务列表，并实现负载均衡与容错机制
- **提供者**：以接口为维度，发布服务到多注册中心

---

## 模块划分

本项目采用模块化设计，主要模块如下：

| 模块名                 | 描述                                               |
|---------------------|--------------------------------------------------|
| `lio-common`        | 公共工具类、常量定义、SPI机制                                 |
| `lio-config`        | 服务导出导入、配置中心模块，支持 YAML、Properties、配置Bean 等方式加载配置。 |                                 |
| `lio-protocol`      | 通信协议模块，支持自定义扩展通信协议                               |
| `lio-serialization` | 序列化模块，支持自定义扩展序列化方式                               |                                      |
| `lio-compression`   | 解压缩模块，支持自定义扩展解压缩方式                               |                                      |                                              |                                      |
| `lio-remote`        | 传输层模块，支持自定义传输层逻辑                                 |                                      |                                               |
| `lio-registry`      | 服务注册与发现模块，支持 Zookeeper、Nacos，支持自定义扩展注册中心         |
| `lio-cluster`       | 集群容错模块：负载均衡、故障转移                                 |
| `lio-limiter`       | 流量控制模块，支持自定义扩展流量控制方式                             |
| `lio-core`          | 集成模块，提供对Spring 和 Spring Boot 的原生集成支持                                          |

---

## 快速入门

### ✅ 添加依赖（Maven 示例）

```xml
<!-- Lio框架核心依赖 -->
<dependency>
    <groupId>com.gt.lio</groupId>
    <artifactId>lio-core</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- zookeeper作为注册中心 -->
<dependency>
    <groupId>com.gt.lio</groupId>
    <artifactId>lio-register-zookeeper</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 使用lio通信协议 -->
<dependency>
    <groupId>com.gt.lio</groupId>
    <artifactId>lio-protocol-lio</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 🛠️ 配置服务

接下来，需要对应用程序进行一些基本配置。可以在 application.yml 或 application.properties 文件中进行配置。这里以 application.yml 为例：
```yaml
#生产者
lio:
  #服务配置
  application:
    name: provider #服务名称
    version: 1.0.0 #服务版本
    group: dev #服务分组
  #通信协议配置
  protocol:
    name: lio #通信协议名称
    port: 20880 #通信端口
    serialization: hessian #序列化方式
  #注册中心配置
  registry:
    type: zookeeper #注册中心类型
    address: 192.168.204.130:2181 #注册中心地址
```

```yaml
#消费者
lio:
  #服务配置
  application:
    name: consumer #服务名称
    version: 1.0.0 #服务版本
    group: dev #服务分组
  #注册中心配置
  registry:
    type: zookeeper #注册中心类型
    address: 192.168.204.130:2181 #注册中心地址
```

### 📦 编写服务接口与实现
定义一个远程调用接口：
```java
public interface HelloService {
    String sayHello(String name);
}
```
在服务提供者中实现该接口：
```java
@LioService
public class HelloServiceImpl implements HelloService {
    @Override
    public String sayHello(String name) {
        return "Hello, " + name + "!";
    }
}
```

### 🔌 启动 RPC 服务
```java
@SpringBootApplication
@LioEnable
public class RpcProviderApplication {
    public static void main(String[] args) {
        SpringApplication.run(RpcProviderApplication.class, args);
    }
}
```

### 📞 调用远程服务
在消费者端注入远程服务并调用：
```java
@RestController
public class HelloController {

    @Reference
    private HelloService helloService;

    @GetMapping("/hello")
    public String hello(@RequestParam String name) {
        return helloService.sayHello(name);
    }
}
```
启动消费者服务：
```java
@SpringBootApplication
@LioEnable
public class RpcConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(RpcProviderApplication.class, args);
    }
}
```

### 🧪 运行测试
启动服务提供者和消费者后，访问：
```
http://localhost:8080/hello?name=Lio
```
你会看到输出：
```
Hello, Lio!
```