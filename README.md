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
- ✅ 客户端容错机制：支持降级处理、失败重试
- ✅ 流量控制：客户端支持滑动窗口、漏桶、令牌桶等限流方式，并且支持自定义扩展流量控制方式
- ✅ 集群策略：支持快速失败、失败重试、多路竞速等集群策略，并且支持自定义扩展集群策略
- ✅ 业务线程池：支持多业务线程池，保证业务处理隔离，并且支持自定义扩展业务线程池
- ✅ 高性能 IO：基于 Netty 实现异步非阻塞通信
- ✅ 可插拔设计：提供SPI机制，使系统功能模块解耦，易于二次开发与扩展
- ✅ 提供了对 Spring 和 Spring Boot 的原生集成支持

---

## 整体架构

![架构图](docs/architecture.png)

该框架采用典型的 RPC 分层架构，主要包括以下几个部分：

- **Consumer（消费者）**：发起远程调用的服务
- **Provider（提供者）**：提供服务实现的服务
- **Registry（注册中心）**：用于服务注册与发现
- **Cluster（集群管理）**：负责负载均衡与故障转移
- **Protocol（协议层）**：定义通信格式与交互规则
- **Transport（传输层）**：基于 Netty 的网络通信实现

---

## 模块划分

本项目采用模块化设计，主要模块如下：

| 模块名 | 描述 |
|--------|------|
| `myrpc-common` | 公共工具类、常量定义 |
| `myrpc-core` | 核心逻辑：代理生成、协议编解码、通信模型 |
| `myrpc-protocol` | 协议抽象层，支持 LIO、HTTP、Dubbo 协议等 |
| `myrpc-serialize` | 序列化模块，支持 JSON、Hessian、Protobuf 等 |
| `myrpc-transport` | 基于 Netty 的网络通信实现 |
| `myrpc-registry` | 服务注册与发现模块，支持 Zookeeper、Nacos 等 |
| `myrpc-cluster` | 集群容错模块：负载均衡、故障转移 |
| `myrpc-spring-boot-starter` | Spring Boot 自动装配模块 |

---

## 快速入门

### 添加依赖（Maven 示例）

```xml
<dependency>
    <groupId>com.myrpc</groupId>
    <artifactId>myrpc-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>