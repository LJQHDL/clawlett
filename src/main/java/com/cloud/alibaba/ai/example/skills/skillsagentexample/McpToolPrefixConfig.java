package com.cloud.alibaba.ai.example.skills.skillsagentexample; // 包声明：本配置类位于示例项目根包下

// ======================== MCP 规范与 Spring AI MCP 集成 ========================
import io.modelcontextprotocol.spec.McpSchema; // MCP 协议模式对象（描述一个 MCP 工具）
import org.springframework.ai.mcp.McpConnectionInfo; // MCP 连接信息（工具所属连接）
import org.springframework.ai.mcp.McpToolNamePrefixGenerator; // MCP 工具名前缀生成器接口
// ======================== Spring 容器注解 ========================
import org.springframework.context.annotation.Bean; // 声明一个由 Spring 容器管理的 Bean
import org.springframework.context.annotation.Configuration; // 声明该类为 Spring 配置类

/**
 * 为 MCP 工具强制添加 "mcp_" 前缀。
 * 默认前缀生成器在单连接时不加前缀，导致 MCP 文件系统的 write_file/read_file
 * 与 Agent 内置 WriteFileTool/ReadFileTool 同名冲突（Multiple tools with the same name）。
 */
// 声明该类为 Spring 配置类（会被组件扫描发现并加载其 Bean 定义）
@Configuration
public class McpToolPrefixConfig {

	// 注册一个 Bean：MCP 工具名前缀生成器，用于解决工具同名冲突
	@Bean
	public McpToolNamePrefixGenerator mcpToolNamePrefixGenerator() {
		return new McpToolNamePrefixGenerator() { // 创建匿名内部类实现该接口
			@Override
			public String prefixedToolName(McpConnectionInfo connectionInfo, McpSchema.Tool tool) { // 重写前缀生成逻辑
				return "mcp_" + tool.name(); // 在工具原名前强制拼接 "mcp_" 前缀并返回
			} // 匿名内部类方法结束
		}; // 匿名内部类结束
	} // Bean 方法结束
} // McpToolPrefixConfig 类结束
