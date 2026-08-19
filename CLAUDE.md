# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

Clawlett 是一个基于 **Spring AI Alibaba Agent Framework**（ReAct 模式）构建的"简版 Open Claw"式个人电脑管家 Agent。Agent 接管用户电脑处理任务：浏览器自动化、Python 代码执行、Shell 命令、文件读写。

- Java 17，Spring Boot 3.5.7，Spring AI 1.1.0，spring-ai-alibaba 1.1.2.0
- LLM 后端：**火山引擎方舟 Coding Plan 网关**（OpenAI 兼容协议，通过 `spring-ai-starter-model-openai` 接入，`ARK_API_KEY` 环境变量配置）
- Agent 状态保存在 `MemorySaver`（内存中，无持久化）

## 常用命令

```bash
# 编译
mvn clean package

# 运行（需先配置 ARK_API_KEY）
mvn spring-boot:run

# 测试
mvn test
mvn test -Dtest=SkillsAgentExampleApplicationTests#contextLoads   # 单个测试
```

应用启动于 `http://localhost:8080`，测试入口：

```bash
curl -X GET "http://localhost:8080/chat?message=整理桌面"
```

注意：首次请求 `/chat` 时懒加载构建 Agent（`ChatController.java:26`），且每次 JVM 重启后重新构建。

## 架构

核心入口 `src/main/java/.../skillsagentexample/`：

- **`agent/SkillsAgent.java`** — 核心装配类。构建 `ReactAgent`：
  - 注册工具：`python_tool`（PythonTool）、`browser_tool`（BrowserTool，非 headless）、文件工具（WriteFile/ReadFile/ListFiles）
  - 挂两个 hook：`SkillsAgentHook`（动态技能加载）和 `ShellToolAgentHook`（Shell 执行，工作目录为 `user.dir`，命令超时 10 秒）
  - `systemPrompt` 为硬编码中文提示词，要求 Agent 自主决策、能用 shell 装软件、能用 pip 补 Python 依赖、不频繁打扰用户
- **`agent/BrowserTool.java`** — Selenium + ChromeDriver（WebDriverManager 自动下载驱动）。actions: navigate/click/extract/screenshot/get_html/fill/scroll；selector 解析支持 `#id`、`.class`、tag 名、CSS 选择器
- **`agent/PythonTool.java`** — GraalVM polyglot 执行 Python（`allowAllAccess(true)`，非沙箱）。检测到 `ModuleNotFoundError` 时自动 `pip install` 后重试
- **`agent/ShellToolXs.java`** — `ShellTool2`（spring-ai-alibaba 提供）的 builder 封装，本项目未直接使用，可忽略或删除
- **`controller/ChatController.java`** — 唯一 REST 入口 `/chat?message=...`
- **模型接入** — 通过 `spring-ai-starter-model-openai` 以 OpenAI 兼容协议接入火山方舟 Coding Plan 网关，无自定义 bean（`SkillsAgentExampleApplication` 是纯启动类）。配置见 `application.yml`：`spring.ai.openai.base-url`（网关地址）、`spring.ai.openai.api-key`（`${ARK_API_KEY}`）、`spring.ai.openai.chat.options.model`（默认 `doubao-seed-2.0-code`，可环境变量 `ARK_MODEL` 覆盖，可选 `kimi-k2.5`、`ark-code-latest` 等）

## Skills 系统（重要）

- 技能注册中心 `FileSystemSkillRegistry` 指向 **用户目录 `~/.agents/skills`**（Windows: `C:\Users\<user>\.agents\skills`），不是项目内目录
- 项目根目录的 `skills/` 目录目前为空
- 如需新增技能：将其放入 `~/.agents/skills/`，Agent 通过 `SkillsAgentHook` 动态发现并加载
- 本机 `~/.agents/skills` 已有技能：defuddle、json-canvas、obsidian-bases、obsidian-cli、obsidian-markdown

## 注意事项

- 代码中大量注释为中文；SkillsAgent 中有一段注释掉的"用户目录全局 skills"逻辑（改用 `~/.agents/skills` 之前的方案）
- 模型切换：通过 `ARK_MODEL` 环境变量或在 `application.yml` 修改 `spring.ai.openai.chat.options.model`，无需改代码
- pom.xml 的 `project.build.sourceEncoding` 必须为 UTF-8（源码含中文注释，否则 Windows 下默认 GBK 解码报编译错）
- 工具执行非沙箱且可写可执行（Python 全权限、Shell 直接跑），属于该 Agent 产品的设计意图，不要"加固"成只读模式
