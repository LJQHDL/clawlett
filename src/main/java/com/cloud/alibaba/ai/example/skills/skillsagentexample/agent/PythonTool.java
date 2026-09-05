/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.alibaba.ai.example.skills.skillsagentexample.agent; // 包声明：本工具类位于 agent 工具包下

// ======================== Jackson 注解（JSON 参数序列化） ========================
import com.fasterxml.jackson.annotation.JsonProperty; // 注解：将字段映射为 JSON 参数名
import com.fasterxml.jackson.annotation.JsonPropertyDescription; // 注解：为字段生成参数说明描述
// ======================== GraalVM Polyglot（在 JVM 中执行 Python） ========================
import org.graalvm.polyglot.Context; // Polyglot 执行上下文（隔离的脚本运行环境）
import org.graalvm.polyglot.Engine; // 共享引擎（可被多个 Context 复用，提升性能）
import org.graalvm.polyglot.PolyglotException; // Polyglot 执行时抛出的异常类型
import org.graalvm.polyglot.Value; // 语言互操作值对象（Python 执行结果的封装）
// ======================== 日志框架（SLF4J） ========================
import org.slf4j.Logger; // SLF4J 日志接口
import org.slf4j.LoggerFactory; // SLF4J 日志工厂，用于创建 Logger 实例
// ======================== Spring AI 工具框架 ========================
import org.springframework.ai.chat.model.ToolContext; // 工具调用上下文对象
import org.springframework.ai.tool.ToolCallback; // AI 工具回调接口
import org.springframework.ai.tool.function.FunctionToolCallback; // 函数式工具回调构建器（Builder）
// ======================== JDK 标准库 ========================
import java.util.function.BiFunction; // 双参数函数式接口（本类实现它作为工具执行入口）

/**
 * xstest
 * Tool for executing Python code using GraalVM polyglot.
 *
 * This tool allows the agent to execute Python code snippets and get results.
 * It uses GraalVM's polyglot API to run Python code in a sandboxed environment.
 */
// 声明 Python 工具类：实现 BiFunction<请求对象, 工具上下文, 返回字符串>，供 AI 模型执行 Python 代码
public class PythonTool implements BiFunction<PythonTool.PythonRequest, ToolContext, String> {

	// 工具描述常量：向 AI 模型说明本工具的用法、示例与安全性说明
	public static final String DESCRIPTION = """
			Executes Python code and returns the result.
			
			Usage:
			- The code parameter must be valid Python code
			- The tool will execute the code and return the output
			- If the code produces a result, it will be returned as a string
			- Errors will be caught and returned as error messages
			- The execution is sandboxed for security
			
			Examples:
			- Simple calculation: code = "2 + 2" returns "4"
			- String operations: code = "'Hello, ' + 'World'" returns "Hello, World"
			- List operations: code = "[1, 2, 3][0]" returns "1"
			"""; // 文本块结束（上述内容即 DESCRIPTION 的字符串内容）

	// 日志对象：用于输出调试/运行日志
	private static final Logger log = LoggerFactory.getLogger(PythonTool.class);
	// 共享 Polyglot 引擎：被多个执行上下文复用，避免重复初始化开销
	private final Engine engine;

	// 无参构造方法：初始化共享引擎
	public PythonTool() {
		// Create a shared engine for better performance
		this.engine = Engine.newBuilder() // 创建引擎构建器
				.option("engine.WarnInterpreterOnly", "false") // 关闭"仅解释器模式"告警（避免误报性能提示）
				.build(); // 完成引擎构建
	}

	/**
	 * Create a ToolCallback for the Python tool.
	 */
	// 静态工厂方法：创建 Python 工具的 ToolCallback
	public static ToolCallback createPythonToolCallback(String description) {
		return FunctionToolCallback.builder("python_tool", new PythonTool()) // 构建名为 python_tool 的回调
				.description(description) // 设置工具对外描述
				.inputType(PythonRequest.class) // 指定请求参数类型为 PythonRequest
				.build(); // 完成构建并返回 ToolCallback
	}

	// 工具核心执行方法：执行 Python 代码并返回结果；若缺模块则自动尝试 pip 安装后重试
	@Override
	public String apply(PythonRequest request, ToolContext toolContext) {
		if (request.code == null || request.code.trim().isEmpty()) { // 校验代码参数是否为空
			return "Error: Python code cannot be empty"; // 为空则返回错误提示
		}

		try (Context context = Context.newBuilder("python") // 构建 Python 语言执行上下文（try-with-resources 自动关闭）
				.engine(engine) // 复用共享引擎
				.allowAllAccess(true) // 允许完全访问以支持 pip 安装
				.allowIO(true) // 启用文件 I/O
				.allowNativeAccess(true) // 启用原生访问
				.allowCreateProcess(true) // 允许创建进程（用于 pip）
				.allowHostAccess(true) // 允许访问主机对象
				.build()) { // 完成上下文构建

			log.debug("Executing Python code: {}", request.code); // 打印待执行的 Python 代码（调试日志）

			// 先尝试执行代码
			try { // 内层 try：捕获 Polyglot 执行异常
				Value result = context.eval("python", request.code); // 在 Python 上下文中求值执行代码
				return convertResultToString(result); // 将执行结果转换为字符串返回
			} catch (PolyglotException e) { // 捕获 Python 执行抛出的异常
				String errorMsg = e.getMessage(); // 获取异常信息文本
				
				// 检查是否是模块缺失错误
				if (errorMsg.contains("ModuleNotFoundError") || errorMsg.contains("No module named")) { // 判断是否为缺模块异常
					// 提取模块名
					String moduleName = extractModuleName(errorMsg); // 从异常信息中解析出缺失的模块名
					if (moduleName != null) { // 若成功解析出模块名
						log.info("Missing module detected: {}, attempting to install...", moduleName); // 打印安装尝试日志
						
						// 尝试通过 pip 安装
						try { // 内层 try：捕获安装过程异常
							ProcessBuilder pb = new ProcessBuilder("pip", "install", moduleName, "--quiet"); // 构造 pip 安装命令
							Process process = pb.start(); // 启动 pip 子进程
							int exitCode = process.waitFor(); // 等待安装进程结束并获取退出码
							
							if (exitCode == 0) { // 若安装成功（退出码为 0）
								log.info("Successfully installed module: {}, retrying execution...", moduleName); // 打印重试日志
								// 重新执行代码
								Value result = context.eval("python", request.code); // 安装后重新求值原代码
								return "Module '" + moduleName + "' was missing and has been installed. Result:\n" + convertResultToString(result); // 返回安装说明与执行结果
							} else { // 若安装失败
								log.warn("Failed to install module: {} via pip", moduleName); // 打印安装失败警告
							}
						} catch (Exception installEx) { // 捕获 pip 安装过程中的异常
							log.error("Error installing module: {}", moduleName, installEx); // 记录安装错误日志
						}
					}
				}
				
				return "Error executing Python code: " + errorMsg; // 返回原始执行错误信息
			}
		}
		catch (Exception e) { // 捕获外层（上下文创建等）异常
			log.error("Unexpected error executing Python code", e); // 记录意外错误日志
			return "Unexpected error: " + e.getMessage(); // 返回意外错误信息
		}
	} // apply 方法结束

	/**
	 * Extract module name from ModuleNotFoundError message.
	 */
	// 从 ModuleNotFoundError 错误信息中提取缺失的模块名
	private String extractModuleName(String errorMsg) {
		try { // 进入 try 块
			// Pattern: No module named 'xxx' or ModuleNotFoundError: No module named 'xxx'
			int start = errorMsg.indexOf("'"); // 查找第一个单引号位置
			if (start != -1) { // 若存在单引号
				int end = errorMsg.indexOf("'", start + 1); // 从其后查找配对的下一个单引号
				if (end != -1) { // 若找到闭合引号
					return errorMsg.substring(start + 1, end); // 截取两个引号之间的模块名并返回
				}
			}
		} catch (Exception e) { // 捕获解析过程中的异常
			log.warn("Failed to extract module name from error: {}", errorMsg); // 记录解析失败警告
		}
		return null; // 解析失败时返回 null
	} // extractModuleName 方法结束

	/**
	 * Convert Python result to Java string.
	 */
	// 将 Python 执行结果（Value 对象）转换为 Java 字符串
	private String convertResultToString(Value result) {
		if (result.isNull()) { // 若结果为 null
			return "Execution completed with no return value"; // 返回"无返回值"提示
		}

		// Handle different result types
		if (result.isString()) { // 若结果为字符串类型
			return result.asString(); // 直接转为 Java 字符串返回
		}
		else if (result.isNumber()) { // 若结果为数字类型
			return String.valueOf(result.as(Object.class)); // 转为 Object 后再转字符串返回
		}
		else if (result.isBoolean()) { // 若结果为布尔类型
			return String.valueOf(result.asBoolean()); // 转为布尔值后再转字符串返回
		}
		else if (result.hasArrayElements()) { // 若结果为数组/列表类型
			// Convert array/list to string representation
			StringBuilder sb = new StringBuilder("["); // 创建字符串构建器，以 "[" 开头
			long size = result.getArraySize(); // 获取数组元素个数
			for (long i = 0; i < size; i++) { // 遍历数组每个元素
				if (i > 0) { // 若非首元素
					sb.append(", "); // 追加逗号分隔符
				}
				Value element = result.getArrayElement(i); // 获取第 i 个元素
				sb.append(element.toString()); // 追加元素字符串表示
			}
			sb.append("]"); // 追加闭合括号 "]"
			return sb.toString(); // 返回数组字符串表示
		}
		else {
			// For other types, use toString()
			return result.toString(); // 其他类型直接调用 toString() 返回
		}
	} // convertResultToString 方法结束

	/**
	 * Request structure for the Python tool.
	 */
	// 请求参数结构体：定义本工具可接收的输入参数
	public static class PythonRequest {

		@JsonProperty(required = true) // 声明该字段为必填参数
		@JsonPropertyDescription("The Python code to execute") // 参数描述：待执行的 Python 代码
		public String code; // 待执行的 Python 代码字符串

		public PythonRequest() { // 无参构造方法（Jackson 反序列化必需）
		} // 无参构造结束

		public PythonRequest(String code) { // 有参构造方法
			this.code = code; // 赋值待执行代码
		} // 有参构造结束
	} // PythonRequest 静态内部类结束
} // PythonTool 类结束
