import { useEffect, useRef, useState } from 'react'
import { Bot, Cpu, SendHorizonal, Sparkles } from 'lucide-react'
import type { ChatMessage, ActivityEvent } from './hooks/useChat'
import { useChat } from './hooks/useChat'

const SKILLS = [
  'defuddle',
  'json-canvas',
  'obsidian-bases',
  'obsidian-cli',
  'obsidian-markdown',
]

interface ChatAreaProps {
  messages: ChatMessage[]
  activities: ActivityEvent[]
  loading: boolean
  error: string | null
  onSend: (text: string) => void
}

function SkillSidebar({ activities }: { activities: ActivityEvent[] }) {
  return (
    <aside className="sidebar" aria-label="技能列表">
      <div className="brand">
        <div className="brand-logo" aria-hidden="true">
          <Cpu size={18} />
        </div>
        <div>
          <div className="brand-title">Clawlett</div>
          <div className="brand-sub">电脑管家 Agent</div>
        </div>
      </div>

      <div className="section-title">已加载技能</div>
      <ul className="skill-list">
        {SKILLS.map((s) => (
          <li key={s} className="skill-item">
            <Sparkles size={14} aria-hidden="true" />
            {s}
          </li>
        ))}
      </ul>

      <div className="section-title">实时活动</div>
      <div className="activity-list">
        {activities.length === 0 ? (
          <div className="empty-state">
            <Cpu size={32} aria-hidden="true" />
            <span>暂无活动<br />发送消息开始</span>
          </div>
        ) : (
          activities.map((a) => (
            <div key={a.id} className={`activity-item${a.running ? ' running' : ''}`}>
              <div className="activity-header">
                <Cpu size={14} aria-hidden="true" />
                {a.label}
              </div>
              <div className="activity-meta">{a.time}</div>
              {a.detail && <div className="activity-detail">{a.detail}</div>}
            </div>
          ))
        )}
      </div>

      <div className="sidebar-footer">ARK · doubao-seed-2.0-code</div>
    </aside>
  )
}

function ChatArea({ messages, loading, error, onSend }: Omit<ChatAreaProps, 'activities'>) {
  const [input, setInput] = useState('')
  const bottomRef = useRef<HTMLDivElement>(null)
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [messages, loading])

  const submit = () => {
    const text = input.trim()
    if (!text || loading) return
    onSend(text)
    setInput('')
  }

  return (
    <main className="chat-area">
      <header className="chat-header">
        <span className="status-dot" aria-hidden="true" />
        <span className="chat-header-title">任务控制台</span>
        <span className="chat-header-status">{loading ? 'Agent 执行中…' : '在线'}</span>
      </header>

      <div className="chat-messages" aria-live="polite">
        {messages.length === 0 && !loading && (
          <div className="empty-state" style={{ marginTop: '10vh' }}>
            <Bot size={44} aria-hidden="true" />
            <span style={{ fontSize: 16, color: 'var(--color-foreground)' }}>
              我是你的电脑管家
            </span>
            <span>
              可以让我：整理文件 · 浏览网页 · 执行 Python · 安装软件<br />
              试试「帮我整理桌面」
            </span>
          </div>
        )}

        {messages.map((m) => (
          <div key={m.id} className={`message ${m.role}`}>
            <div
              className={`message-avatar ${m.role}`}
              aria-hidden="true"
            >
              {m.role === 'user' ? <SendHorizonal size={15} /> : <Bot size={15} />}
            </div>
            <div className="message-bubble">{m.text}</div>
          </div>
        ))}

        {loading && (
          <div className="message assistant">
            <div className="message-avatar assistant" aria-hidden="true">
              <Bot size={15} />
            </div>
            <div className="message-bubble">
              <div className="typing-indicator" aria-label="Agent 思考中">
                <span />
                <span />
                <span />
              </div>
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {error && (
        <div role="alert" className="error-banner" style={{ margin: '0 var(--space-5) var(--space-3)' }}>
          {error}
        </div>
      )}

      <footer className="chat-input-bar">
        <div className="chat-input-row">
          <textarea
            ref={textareaRef}
            className="chat-input"
            placeholder="告诉 Agent 要做什么…（Enter 发送，Shift+Enter 换行）"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault()
                submit()
              }
            }}
            rows={1}
            aria-label="消息输入"
          />
          <button
            className="send-btn"
            onClick={submit}
            disabled={loading || !input.trim()}
            aria-label="发送"
          >
            <SendHorizonal size={18} />
          </button>
        </div>
        <p className="chat-hint">Agent 可自主执行浏览器 / Python / Shell / 文件操作，非沙箱环境</p>
      </footer>
    </main>
  )
}

export default function App() {
  const { messages, activities, loading, error, send } = useChat()

  return (
    <div className="app-shell">
      <SkillSidebar activities={activities} />
      <ChatArea messages={messages} loading={loading} error={error} onSend={send} />
    </div>
  )
}
