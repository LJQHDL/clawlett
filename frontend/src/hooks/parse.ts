// 后端返回形如：
// AssistantMessage [messageType=ASSISTANT, toolCalls=[], textContent=你好, metadata={...}]
// 提取 textContent 内容（可能含换行）
export function parseAssistantText(raw: string): string {
  const match = raw.match(/textContent=((?:[^,}]|\{(?:[^{}]*|\{[^{}]*\})*\})*), metadata=/s)
  if (match && match[1]) {
    return match[1].trim()
  }
  return raw.trim()
}
