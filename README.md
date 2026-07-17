# 云途智行 AI 出行系统（YunTu AI Travel Planner）

基于 Spring Boot 3 + Spring AI 的智能出行规划助手。

## 项目介绍

云途智行 AI 出行系统是一个基于大语言模型的智能出行规划助手。用户输入目的地和预算后，AI 通过 ReAct 模式自主完成需求分析、联网搜索景点/天气/交通信息、综合规划并生成行程方案文档。

### 核心能力

- **RAG 知识库问答**：基于 PGVector 向量数据库加载旅游攻略知识库，提供准确的景点推荐和出行建议
- **多工具调用**：通过 WebSearchTool、WebScrapingTool、FileOperationTool 等 7 种内置工具，实现联网搜索、网页抓取、文件导出等功能
- **ReAct Agent 自主规划**：基于 ReAct 模式的智能体，实现"思考→行动→观察"的自主决策闭环
- **SSE 流式输出**：对话流式实时推送，优化交互体验
- **MCP 协议**：支持 MCP 模型上下文协议，可扩展自定义工具服务
- **Redis 缓存**：缓存对话上下文，减少大模型重复调用

### 技术栈

| 技术 | 说明 |
|------|------|
| Java 21 + Spring Boot 3 | 应用框架 |
| Spring AI 1.0 | AI 集成框架 |
| PostgreSQL + PGVector | 关系数据库 + 向量存储 |
| Redis | 缓存 |
| Alibaba DashScope (qwen-plus) | 大模型（支持 Ollama 本地切换） |
| SSE | 流式响应 |
| MCP | 模型上下文协议 |
| Docker | 容器化部署 |

### 项目结构

```
zhitu-ai-travel/
├── zhitu-travel-frontend/        # Vue3 前端
└── src/main/java/com/zhitu/travel/
    ├── advisor/          # 自定义 Advisor（日志、推理增强）
    ├── agent/            # AI 智能体（BaseAgent → ToolCallAgent → ReActAgent → TravelPlannerAgent）
    ├── chatmemory/       # 对话记忆持久化
    ├── controller/       # REST API 控制器
    ├── rag/              # RAG 全链路（文档加载/切割/检索/查询增强）
    ├── service/          # 业务服务
    ├── tools/            # 7 种内置工具
    └── config/           # 全局配置
```

### 快速启动

1. 配置 `application.yml` 中的 `api-key`（阿里云百炼 DashScope API Key）
2. 可选：启动 PostgreSQL + PGVector（用于 RAG 知识库）
3. 启动后端：`mvn spring-boot:run`
4. 启动前端：`cd zhitu-travel-frontend && npm install && npm run dev`

### 环境要求

- JDK 21+
- Node.js 18+
- Maven 3.6+
- PostgreSQL 14+（可选，不使用 RAG 时可跳过）
- Redis（可选）
