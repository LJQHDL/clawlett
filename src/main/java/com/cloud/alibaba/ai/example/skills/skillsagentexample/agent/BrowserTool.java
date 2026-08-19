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
package com.cloud.alibaba.ai.example.skills.skillsagentexample.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.time.Duration;
import java.util.Base64;
import java.util.function.BiFunction;

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
public class BrowserTool implements BiFunction<BrowserTool.BrowserRequest, ToolContext, String> {

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
			""";
	
	private static final Logger log = LoggerFactory.getLogger(BrowserTool.class);
	
	private WebDriver driver;
	private boolean headless;

	/**
	 * Create a new BrowserTool instance.
	 * @param headless true to run browser in headless mode (no GUI)
	 */
	public BrowserTool(boolean headless) {
		this.headless = headless;
		initializeDriver();
	}
	
	/**
	 * Create a new BrowserTool instance with headless mode enabled.
	 */
	public BrowserTool() {
		this(true); // Default to headless mode
	}

	private void initializeDriver() {
		try {
			log.info("Initializing Chrome WebDriver...");
			
			// Setup WebDriver Manager for automatic driver download
			WebDriverManager.chromedriver().setup();
			
			// Configure Chrome options
			ChromeOptions options = new ChromeOptions();
			if (headless) {
				options.addArguments("--headless=new");
			}
			options.addArguments("--disable-gpu");
			options.addArguments("--no-sandbox");
			options.addArguments("--disable-dev-shm-usage");
			options.addArguments("--window-size=1920,1080");
			
			// Create driver instance
			this.driver = new ChromeDriver(options);
			this.driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
			
			log.info("Chrome WebDriver initialized successfully");
		} catch (Exception e) {
			log.error("Failed to initialize WebDriver", e);
			throw new RuntimeException("Failed to initialize WebDriver", e);
		}
	}

	/**
	 * Create a ToolCallback for the Browser tool.
	 */
	public static ToolCallback createBrowserToolCallback(String description) {
		return FunctionToolCallback.builder("browser_tool", new BrowserTool())
				.description(description)
				.inputType(BrowserRequest.class)
				.build();
	}
	
	/**
	 * Create a ToolCallback for the Browser tool with custom headless setting.
	 */
	public static ToolCallback createBrowserToolCallback(String description, boolean headless) {
		return FunctionToolCallback.builder("browser_tool", new BrowserTool(headless))
				.description(description)
				.inputType(BrowserRequest.class)
				.build();
	}

	@Override
	public String apply(BrowserRequest request, ToolContext toolContext) {
		if (request.action == null || request.action.trim().isEmpty()) {
			return "Error: Browser action cannot be empty";
		}

		try {
			log.debug("Executing browser action: {} | URL: {} | Selector: {}", 
					request.action, request.url, request.selector);

			String result = switch (request.action.toLowerCase()) {
				case "navigate" -> handleNavigate(request);
				case "click" -> handleClick(request);
				case "extract" -> handleExtract(request);
				case "screenshot" -> handleScreenshot(request);
				case "get_html" -> handleGetHtml(request);
				case "fill" -> handleFill(request);
				case "scroll" -> handleScroll(request);
				default -> "Error: Unknown action '" + request.action + 
						"'. Supported: navigate, click, extract, screenshot, get_html, fill, scroll";
			};

			return result;
		}
		catch (Exception e) {
			log.error("Error executing browser action: " + request.action, e);
			return "Error executing browser action: " + e.getMessage();
		}
	}

	private String handleNavigate(BrowserRequest request) {
		if (request.url == null || request.url.trim().isEmpty()) {
			return "Error: URL is required for navigate action";
		}
		
		try {
			long timeout = request.timeout != null ? request.timeout : 30000;
			WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(timeout));
			
			log.info("Navigating to: {}", request.url);
			driver.get(request.url);
			
			// Wait for page to load
			wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
			
			String title = driver.getTitle();
			return "Successfully navigated to: " + request.url + "\nPage Title: " + title;
		}
		catch (Exception e) {
			return "Failed to navigate to " + request.url + ": " + e.getMessage();
		}
	}

	private String handleClick(BrowserRequest request) {
		if (request.selector == null || request.selector.trim().isEmpty()) {
			return "Error: CSS selector is required for click action";
		}
		
		try {
			WebElement element = findElement(request.selector);
			element.click();
			return "Successfully clicked element: " + request.selector;
		}
		catch (NoSuchElementException e) {
			return "Element not found with selector: " + request.selector;
		}
		catch (Exception e) {
			return "Failed to click element " + request.selector + ": " + e.getMessage();
		}
	}

	private String handleExtract(BrowserRequest request) {
		if (request.selector == null || request.selector.trim().isEmpty()) {
			return "Error: CSS selector is required for extract action";
		}
		
		try {
			WebElement element = findElement(request.selector);
			String text = element.getText();
			return "Extracted from " + request.selector + ":\n" + text;
		}
		catch (NoSuchElementException e) {
			return "Element not found with selector: " + request.selector;
		}
		catch (Exception e) {
			return "Failed to extract from " + request.selector + ": " + e.getMessage();
		}
	}

	private String handleScreenshot(BrowserRequest request) {
		try {
			TakesScreenshot screenshotDriver = (TakesScreenshot) driver;
			byte[] screenshotBytes = screenshotDriver.getScreenshotAs(OutputType.BYTES);
			String base64 = Base64.getEncoder().encodeToString(screenshotBytes);
			// 不要返回完整的 data URI 格式，避免 AI 误点
			return "Screenshot captured successfully!\n" +
					"Size: " + screenshotBytes.length + " bytes\n" +
					"Format: PNG (Base64 encoded)\n" +
					"Note: Screenshot data is available but too large to display. " +
					"You can save it by encoding the binary data to base64.";
		}
		catch (Exception e) {
			return "Failed to take screenshot: " + e.getMessage();
		}
	}

	private String handleGetHtml(BrowserRequest request) {
		try {
			String html = driver.getPageSource();
			int length = html.length();
			if (length > 5000) {
				return "HTML retrieved (" + length + " chars)\nFirst 500 chars:\n" + 
						html.substring(0, 500) + "\n...[truncated]";
			}
			return "HTML (" + length + " chars):\n" + html;
		}
		catch (Exception e) {
			return "Failed to get HTML: " + e.getMessage();
		}
	}

	private String handleFill(BrowserRequest request) {
		if (request.selector == null || request.selector.trim().isEmpty()) {
			return "Error: CSS selector is required for fill action";
		}
		if (request.value == null) {
			return "Error: Value is required for fill action";
		}
		
		try {
			WebElement element = findElement(request.selector);
			element.clear();
			element.sendKeys(request.value);
			return "Successfully filled '" + request.value + "' into: " + request.selector;
		}
		catch (NoSuchElementException e) {
			return "Element not found with selector: " + request.selector;
		}
		catch (Exception e) {
			return "Failed to fill element " + request.selector + ": " + e.getMessage();
		}
	}

	private String handleScroll(BrowserRequest request) {
		if (request.selector == null || request.selector.trim().isEmpty()) {
			return "Error: CSS selector is required for scroll action";
		}
		
		try {
			WebElement element = findElement(request.selector);
			((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
			return "Successfully scrolled to element: " + request.selector;
		}
		catch (NoSuchElementException e) {
			return "Element not found with selector: " + request.selector;
		}
		catch (Exception e) {
			return "Failed to scroll to element " + request.selector + ": " + e.getMessage();
		}
	}

	private WebElement findElement(String selector) {
		By by = parseSelector(selector);
		return driver.findElement(by);
	}

	private By parseSelector(String selector) {
		// Support various selector formats
		if (selector.startsWith("#")) {
			return By.id(selector.substring(1));
		} else if (selector.startsWith(".")) {
			return By.className(selector.substring(1));
		} else if (selector.matches("^[a-zA-Z][\\w-]*$")) {
			return By.tagName(selector);
		} else {
			return By.cssSelector(selector);
		}
	}

	/**
	 * Close the browser and cleanup resources.
	 */
	public void close() {
		if (driver != null) {
			log.info("Closing browser...");
			driver.quit();
		}
	}

	/**
	 * Request structure for the Browser tool.
	 */
	public static class BrowserRequest {

		@JsonProperty(required = true)
		@JsonPropertyDescription("The browser action to perform")
		public String action;

		@JsonProperty(required = false)
		@JsonPropertyDescription("The URL to navigate to (for navigate action)")
		public String url;

		@JsonProperty(required = false)
		@JsonPropertyDescription("CSS selector for element interaction")
		public String selector;

		@JsonProperty(required = false)
		@JsonPropertyDescription("Text value to input (for fill action)")
		public String value;

		@JsonProperty(required = false)
		@JsonPropertyDescription("Timeout in milliseconds (default: 30000)")
		public Long timeout;

		public BrowserRequest() {
		}

		public BrowserRequest(String action, String url, String selector, String value, Long timeout) {
			this.action = action;
			this.url = url;
			this.selector = selector;
			this.value = value;
			this.timeout = timeout;
		}
	}
}
