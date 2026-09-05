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
// ======================== WebDriverManager（自动下载/管理驱动） ========================
import io.github.bonigarcia.wdm.WebDriverManager; // 驱动管理器，可自动下载匹配版本的 ChromeDriver
// ======================== Selenium（浏览器自动化核心） ========================
import org.openqa.selenium.*; // Selenium 核心 API（WebDriver、By、WebElement、JavascriptExecutor 等）
import org.openqa.selenium.chrome.ChromeDriver; // Chrome 浏览器驱动实现类
import org.openqa.selenium.chrome.ChromeOptions; // Chrome 浏览器启动选项配置
import org.openqa.selenium.support.ui.ExpectedConditions; // 显式等待条件集合（本文件暂未直接使用）
import org.openqa.selenium.support.ui.WebDriverWait; // 显式等待器，用于等待页面加载完成
// ======================== 日志框架（SLF4J） ========================
import org.slf4j.Logger; // SLF4J 日志接口
import org.slf4j.LoggerFactory; // SLF4J 日志工厂，用于创建 Logger 实例
// ======================== Spring AI 工具框架 ========================
import org.springframework.ai.chat.model.ToolContext; // 工具调用上下文对象（携带会话等信息）
import org.springframework.ai.tool.ToolCallback; // AI 工具回调接口，供模型以工具形式调用
import org.springframework.ai.tool.function.FunctionToolCallback; // 函数式工具回调构建器（Builder）
// ======================== JDK 标准库 ========================
import java.time.Duration; // 时长类型，用于设置等待/超时时间
import java.util.Base64; // Base64 编解码工具
import java.util.function.BiFunction; // 双参数函数式接口（本类实现它作为工具执行入口）

/**
 * Browser automation tool using Selenium WebDriver.
 * 
 * This tool provides web browser automation capabilities:
 * - Navigate to URLs
 * - Click elements
 * - Extract text content
 * - Take screenshots
 * - Get page HTML
 * - Fill input fields
 */
// 声明浏览器工具类：实现 BiFunction<请求对象, 工具上下文, 返回字符串>，由 AI 模型作为工具调用
public class BrowserTool implements BiFunction<BrowserTool.BrowserRequest, ToolContext, String> {

	// 工具描述常量：向 AI 模型说明本工具支持的动作、参数与用法示例
	public static final String DESCRIPTION = """
			Performs browser automation tasks using Selenium WebDriver.
			
			Usage:
			- action: The action to perform (navigate, click, extract, screenshot, get_html, fill, scroll)
			- url: The URL to navigate to (required for navigate action)
			- selector: CSS selector for element interaction (required for click/extract/fill actions)
			- value: Text value to input (required for fill action)
			- timeout: Maximum wait time in milliseconds (default: 30000)
			
			Actions supported:
			- navigate: Navigate to a URL (requires url parameter)
			- click: Click an element (requires selector parameter)
			- extract: Extract text from an element (requires selector parameter)
			- screenshot: Take a screenshot (returns base64 encoded image)
			- get_html: Get the full HTML content of current page
			- fill: Fill text into an input field (requires selector and value parameters)
			- scroll: Scroll to an element (requires selector parameter)
			
			Examples:
			- Navigate: action="navigate", url="https://www.google.com"
			- Click button: action="click", selector="#search-button"
			- Extract title: action="extract", selector="h1"
			- Fill form: action="fill", selector="#username", value="john_doe"
			- Screenshot: action="screenshot"
			"""; // 文本块结束（上述内容即 DESCRIPTION 的字符串内容）

	// 日志对象：用于输出调试/运行日志
	private static final Logger log = LoggerFactory.getLogger(BrowserTool.class);

	// WebDriver 实例：持有当前浏览器会话
	private WebDriver driver;
	// 是否使用无头（headless）模式运行（无 GUI 界面）
	private boolean headless;

	/**
	 * Create a new BrowserTool instance.
	 * @param headless true to run browser in headless mode (no GUI)
	 */
	// 有参构造方法：根据 headless 参数创建浏览器工具实例
	public BrowserTool(boolean headless) {
		this.headless = headless; // 保存无头模式标志
		initializeDriver(); // 初始化浏览器驱动
	} // 有参构造方法结束

	/**
	 * Create a new BrowserTool instance with headless mode enabled.
	 */
	// 无参构造方法：默认启用无头模式
	public BrowserTool() {
		this(true); // Default to headless mode // 委托给有参构造，默认使用无头模式
	}

	// 初始化 WebDriver 驱动（配置驱动管理器与 Chrome 启动参数）
	private void initializeDriver() {
		try { // 进入 try 块，统一捕获初始化阶段的异常
			log.info("Initializing Chrome WebDriver..."); // 打印初始化开始日志

			// Setup WebDriver Manager for automatic driver download
			WebDriverManager.chromedriver().setup(); // 自动下载并配置与浏览器匹配的 ChromeDriver

			// Configure Chrome options
			ChromeOptions options = new ChromeOptions(); // 创建 Chrome 启动选项对象
			if (headless) { // 如果启用了无头模式
				options.addArguments("--headless=new"); // 添加新版无头模式启动参数
			}
			options.addArguments("--disable-gpu"); // 禁用 GPU 硬件加速（无头/容器环境更稳定）
			options.addArguments("--no-sandbox"); // 禁用沙箱模式（容器或 CI 环境必需）
			options.addArguments("--disable-dev-shm-usage"); // 禁用 /dev/shm 共享内存（避免资源不足崩溃）
			options.addArguments("--window-size=1920,1080"); // 设置浏览器窗口尺寸为 1920x1080

			// Create driver instance
			this.driver = new ChromeDriver(options); // 使用上述选项创建 Chrome 驱动实例
			this.driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10)); // 设置全局隐式等待 10 秒

			log.info("Chrome WebDriver initialized successfully"); // 打印初始化成功日志
		} catch (Exception e) { // 捕获初始化过程中的任意异常
			log.error("Failed to initialize WebDriver", e); // 记录错误日志及堆栈
			throw new RuntimeException("Failed to initialize WebDriver", e); // 包装后抛出运行时异常，终止初始化
		}
	} // initializeDriver 方法结束

	/**
	 * Create a ToolCallback for the Browser tool.
	 */
	// 静态工厂方法：创建浏览器工具的 ToolCallback（默认无头模式）
	public static ToolCallback createBrowserToolCallback(String description) {
		return FunctionToolCallback.builder("browser_tool", new BrowserTool()) // 构建名为 browser_tool 的回调，实例默认无头
				.description(description) // 设置工具对外描述
				.inputType(BrowserRequest.class) // 指定请求参数类型为 BrowserRequest
				.build(); // 完成构建并返回 ToolCallback
	} // 工厂方法结束

	/**
	 * Create a ToolCallback for the Browser tool with custom headless setting.
	 */
	// 静态工厂方法：创建浏览器工具的 ToolCallback（可自定义无头模式）
	public static ToolCallback createBrowserToolCallback(String description, boolean headless) {
		return FunctionToolCallback.builder("browser_tool", new BrowserTool(headless)) // 构建回调并传入无头标志
				.description(description) // 设置工具对外描述
				.inputType(BrowserRequest.class) // 指定请求参数类型为 BrowserRequest
				.build(); // 完成构建并返回 ToolCallback
	} // 工厂方法结束

	// 工具核心执行方法：由 AI 模型调用，根据动作分发到具体处理逻辑
	@Override
	public String apply(BrowserRequest request, ToolContext toolContext) {
		if (request.action == null || request.action.trim().isEmpty()) { // 校验动作参数是否为空或空白
			return "Error: Browser action cannot be empty"; // 非法参数时返回错误提示
		}

		try { // 进入 try 块，统一捕获执行阶段的异常
			log.debug("Executing browser action: {} | URL: {} | Selector: {}", 
					request.action, request.url, request.selector); // 打印调试日志（动作、URL、选择器）

			// 根据动作类型分发到对应的处理方法（Java 14+ switch 箭头表达式）
			String result = switch (request.action.toLowerCase()) { // 将动作转小写后进行匹配
				case "navigate" -> handleNavigate(request); // 导航：跳转到指定 URL
				case "click" -> handleClick(request); // 点击：点击指定元素
				case "extract" -> handleExtract(request); // 提取：获取元素文本
				case "screenshot" -> handleScreenshot(request); // 截图：截取当前页面
				case "get_html" -> handleGetHtml(request); // 获取 HTML：读取页面源码
				case "fill" -> handleFill(request); // 填充：向输入框写入文本
				case "scroll" -> handleScroll(request); // 滚动：滚动到指定元素
				default -> "Error: Unknown action '" + request.action + 
						"'. Supported: navigate, click, extract, screenshot, get_html, fill, scroll"; // 未知动作提示支持范围
			}; // switch 表达式结束

			return result; // 返回动作执行结果字符串
		}
		catch (Exception e) { // 捕获执行过程中的任意异常
			log.error("Error executing browser action: " + request.action, e); // 记录错误日志
			return "Error executing browser action: " + e.getMessage(); // 返回包含异常信息的错误提示
		}
	} // apply 方法结束

	// 处理 navigate 动作：导航到 URL 并等待页面加载完成
	private String handleNavigate(BrowserRequest request) {
		if (request.url == null || request.url.trim().isEmpty()) { // 校验 URL 参数是否为空
			return "Error: URL is required for navigate action"; // 为空则返回错误提示
		}
		
		try { // 进入 try 块
			long timeout = request.timeout != null ? request.timeout : 30000; // 获取超时时间，未指定时默认 30000 毫秒
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(timeout)); // 创建显式等待器
			
			log.info("Navigating to: {}", request.url); // 打印导航日志
			driver.get(request.url); // 驱动执行页面跳转
			
			// Wait for page to load
			wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete")); // 循环等待直到页面 readyState 为 complete
			
			String title = driver.getTitle(); // 获取跳转后页面的标题
			return "Successfully navigated to: " + request.url + "\nPage Title: " + title; // 返回导航成功信息及页面标题
		}
		catch (Exception e) { // 捕获跳转过程中的异常
			return "Failed to navigate to " + request.url + ": " + e.getMessage(); // 返回导航失败信息
		}
	} // handleNavigate 方法结束

	// 处理 click 动作：点击指定 CSS 选择器对应的元素
	private String handleClick(BrowserRequest request) {
		if (request.selector == null || request.selector.trim().isEmpty()) { // 校验选择器参数是否为空
			return "Error: CSS selector is required for click action"; // 为空则返回错误提示
		}
		
		try { // 进入 try 块
			WebElement element = findElement(request.selector); // 根据选择器查找目标元素
			element.click(); // 执行点击操作
			return "Successfully clicked element: " + request.selector; // 返回点击成功信息
		}
		catch (NoSuchElementException e) { // 捕获元素不存在异常
			return "Element not found with selector: " + request.selector; // 返回未找到元素的提示
		}
		catch (Exception e) { // 捕获其他异常
			return "Failed to click element " + request.selector + ": " + e.getMessage(); // 返回点击失败信息
		}
	} // handleClick 方法结束

	// 处理 extract 动作：提取指定元素的可见文本内容
	private String handleExtract(BrowserRequest request) {
		if (request.selector == null || request.selector.trim().isEmpty()) { // 校验选择器参数是否为空
			return "Error: CSS selector is required for extract action"; // 为空则返回错误提示
		}
		
		try { // 进入 try 块
			WebElement element = findElement(request.selector); // 根据选择器查找目标元素
			String text = element.getText(); // 获取元素的可见文本
			return "Extracted from " + request.selector + ":\n" + text; // 返回提取到的文本内容
		}
		catch (NoSuchElementException e) { // 捕获元素不存在异常
			return "Element not found with selector: " + request.selector; // 返回未找到元素的提示
		}
		catch (Exception e) { // 捕获其他异常
			return "Failed to extract from " + request.selector + ": " + e.getMessage(); // 返回提取失败信息
		}
	} // handleExtract 方法结束

	// 处理 screenshot 动作：对当前页面进行截图
	private String handleScreenshot(BrowserRequest request) {
		try { // 进入 try 块
			TakesScreenshot screenshotDriver = (TakesScreenshot) driver; // 将驱动对象转换为截图接口类型
			byte[] screenshotBytes = screenshotDriver.getScreenshotAs(OutputType.BYTES); // 截取屏幕并输出为字节数组
			String base64 = Base64.getEncoder().encodeToString(screenshotBytes); // 将截图字节编码为 Base64 字符串
			// 不要返回完整的 data URI 格式，避免 AI 误点
			return "Screenshot captured successfully!\n" + // 返回截图成功提示
					"Size: " + screenshotBytes.length + " bytes\n" + // 附带截图字节大小
					"Format: PNG (Base64 encoded)\n" + // 附带截图格式说明
					"Note: Screenshot data is available but too large to display. " + // 提示截图数据过大不便直接展示
					"You can save it by encoding the binary data to base64."; // 提示可将二进制数据自行保存
		}
		catch (Exception e) { // 捕获截图过程中的异常
			return "Failed to take screenshot: " + e.getMessage(); // 返回截图失败信息
		}
	} // handleScreenshot 方法结束

	// 处理 get_html 动作：获取当前页面的完整 HTML 源码
	private String handleGetHtml(BrowserRequest request) {
		try { // 进入 try 块
			String html = driver.getPageSource(); // 获取当前页面源码字符串
			int length = html.length(); // 计算 HTML 源码长度
			if (length > 5000) { // 若源码超过 5000 字符
				return "HTML retrieved (" + length + " chars)\nFirst 500 chars:\n" + // 返回截断提示及总长度
						html.substring(0, 500) + "\n...[truncated]"; // 仅返回前 500 字符并标注已截断
			}
			return "HTML (" + length + " chars):\n" + html; // 源码较短时返回完整 HTML
		}
		catch (Exception e) { // 捕获获取源码过程中的异常
			return "Failed to get HTML: " + e.getMessage(); // 返回获取失败信息
		}
	} // handleGetHtml 方法结束

	// 处理 fill 动作：向输入框清空并写入文本
	private String handleFill(BrowserRequest request) {
		if (request.selector == null || request.selector.trim().isEmpty()) { // 校验选择器参数是否为空
			return "Error: CSS selector is required for fill action"; // 为空则返回错误提示
		}
		if (request.value == null) { // 校验填充文本值是否为空
			return "Error: Value is required for fill action"; // 为空则返回错误提示
		}
		
		try { // 进入 try 块
			WebElement element = findElement(request.selector); // 根据选择器查找目标输入框元素
			element.clear(); // 清空输入框原有内容
			element.sendKeys(request.value); // 向输入框键入新的文本内容
			return "Successfully filled '" + request.value + "' into: " + request.selector; // 返回填充成功信息
		}
		catch (NoSuchElementException e) { // 捕获元素不存在异常
			return "Element not found with selector: " + request.selector; // 返回未找到元素的提示
		}
		catch (Exception e) { // 捕获其他异常
			return "Failed to fill element " + request.selector + ": " + e.getMessage(); // 返回填充失败信息
		}
	} // handleFill 方法结束

	// 处理 scroll 动作：滚动页面使指定元素进入可视区域
	private String handleScroll(BrowserRequest request) {
		if (request.selector == null || request.selector.trim().isEmpty()) { // 校验选择器参数是否为空
			return "Error: CSS selector is required for scroll action"; // 为空则返回错误提示
		}
		
		try { // 进入 try 块
			WebElement element = findElement(request.selector); // 根据选择器查找目标元素
			((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element); // 通过 JS 将元素滚动到视口顶部
			return "Successfully scrolled to element: " + request.selector; // 返回滚动成功信息
		}
		catch (NoSuchElementException e) { // 捕获元素不存在异常
			return "Element not found with selector: " + request.selector; // 返回未找到元素的提示
		}
		catch (Exception e) { // 捕获其他异常
			return "Failed to scroll to element " + request.selector + ": " + e.getMessage(); // 返回滚动失败信息
		}
	} // handleScroll 方法结束

	// 根据选择器字符串查找并返回页面元素
	private WebElement findElement(String selector) {
		By by = parseSelector(selector); // 将选择器字符串解析为 Selenium By 定位器
		return driver.findElement(by); // 调用驱动按定位器查找元素
	} // findElement 方法结束

	// 解析选择器字符串：自动识别 ID、类名、标签名或 CSS 选择器
	private By parseSelector(String selector) {
		// Support various selector formats
		if (selector.startsWith("#")) { // 若以 # 开头：视为 ID 选择器
			return By.id(selector.substring(1)); // 去掉 # 后按元素 ID 定位
		} else if (selector.startsWith(".")) { // 若以 . 开头：视为类名选择器
			return By.className(selector.substring(1)); // 去掉 . 后按 CSS 类名定位
		} else if (selector.matches("^[a-zA-Z][\\w-]*$")) { // 若符合纯标签名正则（字母开头，含字母/数字/下划线/连字符）
			return By.tagName(selector); // 按 HTML 标签名定位
		} else { // 其余情况
			return By.cssSelector(selector); // 一律按标准 CSS 选择器解析
		}
	} // parseSelector 方法结束

	/**
	 * Close the browser and cleanup resources.
	 */
	// 关闭浏览器并释放相关资源
	public void close() {
		if (driver != null) { // 若驱动实例非空
			log.info("Closing browser..."); // 打印关闭日志
			driver.quit(); // 退出并彻底关闭浏览器会话
		}
	} // close 方法结束

	/**
	 * Request structure for the Browser tool.
	 */
	// 请求参数结构体：定义本工具可接收的输入参数（供 JSON 反序列化）
	public static class BrowserRequest {

		@JsonProperty(required = true) // 声明该字段为必填参数
		@JsonPropertyDescription("The browser action to perform") // 参数描述：要执行的浏览器动作
		public String action; // 动作类型（navigate/click/extract/screenshot/get_html/fill/scroll）

		@JsonProperty(required = false) // 声明该字段为可选参数
		@JsonPropertyDescription("The URL to navigate to (for navigate action)") // 参数描述：待导航的 URL
		public String url; // 目标 URL（navigate 动作使用）

		@JsonProperty(required = false) // 声明该字段为可选参数
		@JsonPropertyDescription("CSS selector for element interaction") // 参数描述：元素交互选择器
		public String selector; // CSS 选择器（click/extract/fill/scroll 动作使用）

		@JsonProperty(required = false) // 声明该字段为可选参数
		@JsonPropertyDescription("Text value to input (for fill action)") // 参数描述：待输入的文本
		public String value; // 填充文本内容（fill 动作使用）

		@JsonProperty(required = false) // 声明该字段为可选参数
		@JsonPropertyDescription("Timeout in milliseconds (default: 30000)") // 参数描述：超时毫秒数
		public Long timeout; // 超时时间（毫秒），默认 30000

		public BrowserRequest() { // 无参构造方法（Jackson 反序列化必需）
		} // 无参构造结束

		public BrowserRequest(String action, String url, String selector, String value, Long timeout) { // 全参构造方法
			this.action = action; // 赋值动作类型
			this.url = url; // 赋值目标 URL
			this.selector = selector; // 赋值选择器
			this.value = value; // 赋值填充文本
			this.timeout = timeout; // 赋值超时时间
		} // 全参构造结束
	} // BrowserRequest 静态内部类结束
} // BrowserTool 类结束
