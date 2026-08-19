package com.cloud.alibaba.ai.example.skills.skillsagentexample.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.ListFilesTool;
import com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.ReadFileTool;
import com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.WriteFileTool;
import com.alibaba.cloud.ai.graph.agent.hook.shelltool.ShellToolAgentHook;
import com.alibaba.cloud.ai.graph.agent.hook.skills.SkillsAgentHook;

import java.io.IOException;
import java.util.List;

import com.alibaba.cloud.ai.graph.agent.tools.ShellTool2;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.skills.registry.filesystem.FileSystemSkillRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;

@Service
public class SkillsAgent {
    private static final Logger logger = LoggerFactory.getLogger(SkillsAgent.class);
    private static final String SKILLS_DIR = "clawlett/skills";

    private final ToolCallbackProvider mcpToolCallbackProvider;

    public SkillsAgent(ToolCallbackProvider mcpToolCallbackProvider) {
        this.mcpToolCallbackProvider = mcpToolCallbackProvider;
    }

    public ReactAgent buildAgent(ChatModel chatModel) {
      /*
        使用“用户目录”下的全局skills
        Path skillsPath = Path.of(SKILLS_DIR).toAbsolutePath();
        logger.info("Skills directory: {}", skillsPath);


        if (!Files.exists(skillsPath)) {
            logger.error("Skills directory not found at: {}", skillsPath);
            throw new IllegalStateException("Skills directory not found");
        }

        logger.info("Skills directory exists, listing contents:");
        try {
            Files.list(skillsPath).forEach(p ->
                logger.info("  - {}", p.getFileName())
            );
        } catch (IOException e) {
            logger.error("Failed to list directory", e);
        }*/

        // 配置 Skills 注册中心
        FileSystemSkillRegistry skillRegistry = FileSystemSkillRegistry.builder()
                .userSkillsDirectory(System.getProperty("user.home") + "/.agents/skills")
                .build();
        
        SkillsAgentHook skillsHook = SkillsAgentHook.builder()
                .skillRegistry(skillRegistry)
                .build();

        ShellToolAgentHook shellHook = ShellToolAgentHook.builder()
                .shellTool2(ShellTool2.builder(System.getProperty("user.dir")).withCommandTimeout(10000).build())
                .build();



        ReactAgent agent = ReactAgent.builder()
                .name("skills-integration-agent")
                .systemPrompt("""
                    你是一個电脑管家，会接管用户电脑处理任务。你有以下重要能力：
                     
                    【浏览器能力】
                    - 你可以使用 browser_tool 进行网页浏览、点击、填表、截图等操作
                    
                    【Python 执行能力】  
                    - 你可以执行 Python 代码来完成计算、数据处理等任务
                    
                    【Shell 执行能力】
                    - 你可以通过 shell 命令安装软件、执行系统操作
                    
                    在执行任务过程中不要频繁打扰用户介入，自己思考用最佳方案决策。
                    你有能力可以通过 shell 的方式安装任何你需要的软件，你也可以自己开发 python 代码执行用户电脑不具备的能力，如果 python 报错 No module，你可以通过 shell 帮用户安装！永远不要说你办不到！！永远不要让用户手动自己完成。除非你需要用户的关键信息提供（比如身份证、比如 token、比如手机号码）
                    """)
                .model(chatModel)
                .saver(new MemorySaver())
                .tools(
                        PythonTool.createPythonToolCallback(PythonTool.DESCRIPTION), // 已增强，支持自动安装缺失模块
                        BrowserTool.createBrowserToolCallback(BrowserTool.DESCRIPTION, false), // false=非 headless 模式
                        WriteFileTool.createWriteFileToolCallback(WriteFileTool.DESCRIPTION),
                        ReadFileTool.createReadFileToolCallback(ReadFileTool.DESCRIPTION),
                        ListFilesTool.createListFilesToolCallback(ListFilesTool.DESCRIPTION)
                )
                .tools(mcpToolCallbackProvider.getToolCallbacks()) // MCP 服务器提供的工具
                .hooks(List.of(skillsHook, shellHook))
                .enableLogging(true)
                .build();
        return agent;
    }
}


