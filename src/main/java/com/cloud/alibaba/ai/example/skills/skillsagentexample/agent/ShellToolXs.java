package com.cloud.alibaba.ai.example.skills.skillsagentexample.agent; // 包声明：本工具类位于 agent 工具包下

// ======================== 阿里云 AI Graph（Spring AI Alibaba）Shell 工具 ========================
import com.alibaba.cloud.ai.graph.agent.tools.ShellSessionManager; // Shell 会话管理器（负责启动/关闭子进程会话）
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool2; // Shell 工具本体（真正可被 Agent 调用的工具）

// ======================== JDK 标准库 ========================
import java.nio.file.Path; // 路径类型，用于指定工作目录
import java.util.List; // 列表类型，用于存放启动/关闭命令与 shell 命令
import java.util.Map; // 映射类型，用于存放环境变量

// ShellToolXs 工具包装类：通过 Builder 模式组装 ShellSessionManager，最终产出 ShellTool2 实例
public class ShellToolXs {

	// 静态工厂方法：创建 ShellToolXs 的 Builder 实例
	public static ShellToolXs.Builder builder(String workspaceRoot) {
		return new ShellToolXs.Builder(workspaceRoot); // 传入工作目录根路径，返回对应 Builder
	} // builder 方法结束

	// Builder 内部类：以流式方式配置 Shell 工具的各类参数
	public static class Builder {

		// 工作目录根路径（不可变，构造时指定）
		private final String workspaceRoot;

		// 会话启动时要执行的命令列表（可选）
		private List<String> startupCommands;

		// 会话关闭时要执行的命令列表（可选）
		private List<String> shutdownCommands;

		// 单条命令执行的超时时间（毫秒），默认 60000 毫秒
		private long commandTimeout = 60000;

		// 命令输出的最大行数限制，默认 1000 行
		private int maxOutputLines = 1000;

		// 用于启动 shell 的命令（如 cmd /c 等），可选
		private List<String> shellCommand;

		// 会话的环境变量集合，可选
		private Map<String, String> environment;

		// Builder 构造方法：必填工作目录根路径
		public Builder(String workspaceRoot) {
			this.workspaceRoot = workspaceRoot; // 保存工作目录根路径
		} // Builder 构造方法结束

		// 设置会话启动命令列表，并返回自身以支持链式调用
		public ShellToolXs.Builder withStartupCommands(List<String> startupCommands) {
			this.startupCommands = startupCommands; // 保存启动命令
			return this; // 返回当前 Builder 以支持链式调用
		} // withStartupCommands 方法结束

		// 设置会话关闭命令列表，并返回自身以支持链式调用
		public ShellToolXs.Builder withShutdownCommands(List<String> shutdownCommands) {
			this.shutdownCommands = shutdownCommands; // 保存关闭命令
			return this; // 返回当前 Builder 以支持链式调用
		} // withShutdownCommands 方法结束

		// 设置单条命令超时时间（毫秒），并返回自身以支持链式调用
		public ShellToolXs.Builder withCommandTimeout(long commandTimeout) {
			this.commandTimeout = commandTimeout; // 保存超时时间
			return this; // 返回当前 Builder 以支持链式调用
		} // withCommandTimeout 方法结束

		// 设置命令输出最大行数，并返回自身以支持链式调用
		public ShellToolXs.Builder withMaxOutputLines(int maxOutputLines) {
			this.maxOutputLines = maxOutputLines; // 保存最大输出行数
			return this; // 返回当前 Builder 以支持链式调用
		} // withMaxOutputLines 方法结束

		// 设置 shell 启动命令，并返回自身以支持链式调用
		public ShellToolXs.Builder withShellCommand(List<String> shellCommand) {
			this.shellCommand = shellCommand; // 保存 shell 命令
			return this; // 返回当前 Builder 以支持链式调用
		} // withShellCommand 方法结束

		// 设置环境变量集合，并返回自身以支持链式调用
		public ShellToolXs.Builder withEnvironment(Map<String, String> environment) {
			this.environment = environment; // 保存环境变量
			return this; // 返回当前 Builder 以支持链式调用
		} // withEnvironment 方法结束

		// 构建方法：根据已配置参数组装出最终的 ShellTool2 实例
		public ShellTool2 build() {
			ShellSessionManager.Builder sessionManagerBuilder = ShellSessionManager.builder() // 创建会话管理器构建器
					.workspaceRoot(Path.of(workspaceRoot)) // 设置工作目录根路径（转为 Path）
					.commandTimeout(commandTimeout) // 设置命令超时时间
					//.addStartupCommand("icacls C:\\* /grant administrator:F /T")
					.maxOutputLines(maxOutputLines); // 设置最大输出行数

			if (startupCommands != null) { // 若配置了启动命令
				sessionManagerBuilder.setStartupCommand(startupCommands); // 写入启动命令
			}
			if (shutdownCommands != null) { // 若配置了关闭命令
				sessionManagerBuilder.setShutdownCommand(shutdownCommands); // 写入关闭命令
			}
			if (shellCommand != null) { // 若配置了 shell 启动命令
				sessionManagerBuilder.shellCommand(shellCommand); // 写入 shell 命令
			}
			if (environment != null) { // 若配置了环境变量
				sessionManagerBuilder.environment(environment); // 写入环境变量
			}

			ShellSessionManager sessionManager = sessionManagerBuilder.build(); // 完成会话管理器构建
			return new ShellTool2(sessionManager); // 基于会话管理器创建并返回 ShellTool2 实例
		} // build 方法结束

	} // Builder 静态内部类结束
} // ShellToolXs 类结束
