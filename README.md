# Clawlett 🦞

基于 **Spring AI Alibaba Agent Framework**（ReAct 模式）的个人电脑管家 Agent——"简版 Open Claw"。Agent 接管你的电脑处理任务：浏览器自动化、Python 代码执行、Shell 命令、文件读写、MCP 工具。

## 功能特性

- **ReAct Agent**：LLM 自主决策 + 工具调用的完整闭环
- **工具集**：
  - `browser_tool` — Selenium 浏览器自动化（导航/点击/填表/截图/取 HTML）
  - `python_tool` — GraalVM Polyglot 执行 Python，`ModuleNotFoundError` 自动 pip install 后重试
  - Shell — 系统命令执行（工作目录 `user.dir`）
  - 文件读写、目录列表
  - **MCP 工具**（`mcp_` 前缀）— 通过 MCP 协议接入外部工具服务器
- **技能系统**：动态加载 `~/.agents/skills` 下的技能（Obsidian、浏览器等）
- **Web 控制台**：React + Vite 前端（OLED 深色风格），实时展示对话与任务活动

## 技术栈

- Java 17 · Spring Boot 3.5.7 · Spring AI 1.1.0 · spring-ai-alibaba 1.1.2.0
- LLM：火山引擎方舟 Coding Plan 网关（OpenAI 兼容协议）
- GraalVM Polyglot · Selenium · React + Vite + TypeScript

## 快速开始

### 1. 配置环境变量

```bash
export ARK_API_KEY=your_ark_api_key        # 方舟 API Key（必填）
export ARK_MODEL=doubao-seed-2.0-code      # 可选，默认 doubao-seed-2.0-code
                                           # 可选 kimi-k2.5 / ark-code-latest
```

### 2. 启动后端

```bash
mvn clean package
mvn spring-boot:run
```

应用启动于 `http://localhost:8080`。

### 3. 启动前端（可选）

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173（代理 /chat → 8080）
```

### 测试

```bash
curl -G "http://localhost:8080/chat" --data-urlencode "message=整理桌面"
```

## MCP 接入

在 `application.yml` 中配置 stdio 连接即可接入新的 MCP 服务器，Agent 自动发现其工具（统一 `mcp_` 前缀）：

```yaml
spring:
  ai:
    mcp:
      client:
        stdio:
          connections:
            filesystem:
              command: F:/Node/npx.cmd      # Windows 需 .cmd 全路径
              args: ["-y", "@modelcontextprotocol/server-filesystem", "C:/Users/27838"]
```

内置演示：MCP 官方文件系统服务器。

## 项目结构

```
src/main/java/.../skillsagentexample/
├── agent/SkillsAgent.java          # ReactAgent 装配：工具 + hooks + MCP + 系统提示词
├── agent/BrowserTool.java          # Selenium 浏览器自动化
├── agent/PythonTool.java           # GraalVM Python 执行 + 自动装依赖
├── controller/ChatController.java  # 唯一 REST 入口 /chat
└── McpToolPrefixConfig.java        # MCP 工具 mcp_ 前缀（避免与内置工具重名）
```

## 路线图

- [x] MCP 支持
- [x] Web 控制台
- [ ] 长期记忆（持久化对话历史）
- [ ] 流式响应（SSE）
- [ ] IM 集成（飞书/钉钉/Telegram）
- [ ] 定时任务
