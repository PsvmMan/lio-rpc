# 🚀 MyRPC —— 我的自研 Java RPC 框架

> 简洁、高效、可扩展的高性能 RPC 框架，适用于微服务架构下的服务通信场景。

[![Build Status](https://img.shields.io/badge/build-passing-green)](https://github.com/PsvmMan/lio-rpc)
[![License](https://img.shields.io/github/license/PsvmMan/lio-rpc)](LICENSE)

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

MyRPC 是一个轻量级的 Java 远程过程调用框架（Remote Procedure Call），旨在提供高性能、高可用、低侵入的服务间通信能力。支持多种协议、序列化方式以及注册中心集成，适用于构建分布式系统和微服务架构。

该项目是我个人学习与实践过程中逐步构建的 RPC 框架，目前具备基础功能并持续迭代中。

---

## 核心特性

- ✅ 支持多种通信协议：TCP、HTTP、LIO 等
- ✅ 支持多种序列化方式：JSON、Hessian、Protobuf、JDK 序列化等
- ✅ 负载均衡策略：随机、轮询、最少活跃调用等
- ✅ 服务注册与发现：Zookeeper、Nacos、Eureka、Consul 插件式支持
- ✅ 客户端容错机制：失败重试、熔断、降级
- ✅ 高性能 IO：基于 Netty 实现异步非阻塞通信
- ✅ 可插拔设计：模块解耦，易于二次开发与扩展
- ✅ Spring Boot Starter 集成，开箱即用

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