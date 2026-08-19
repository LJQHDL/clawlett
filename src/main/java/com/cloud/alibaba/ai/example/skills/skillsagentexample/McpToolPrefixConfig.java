package com.cloud.alibaba.ai.example.skills.skillsagentexample;

import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.McpConnectionInfo;
import org.springframework.ai.mcp.McpToolNamePrefixGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 为 MCP 工具强制添加 "mcp_" 前缀。
 * 默认前缀生成器在单连接时不加前缀，导致 MCP 文件系统的 write_file/read_file
 * 与 Agent 内置 WriteFileTool/ReadFileTool 同名冲突（Multiple tools with the same name）。
 */
@Configuration
public class McpToolPrefixConfig {

    @Bean
    public McpToolNamePrefixGenerator mcpToolNamePrefixGenerator() {
        return new McpToolNamePrefixGenerator() {
            @Override
            public String prefixedToolName(McpConnectionInfo connectionInfo, McpSchema.Tool tool) {
                return "mcp_" + tool.name();
            }
        };
    }
}
