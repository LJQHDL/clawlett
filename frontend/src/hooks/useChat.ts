import { useCallback, useState } from 'react'
import { parseAssistantText } from './parse'

export interface ActivityEvent {
  id: number
  label: string
  detail: string
  running: boolean
  time: string
}

export interface ChatMessage {
  id: number
  role: 'user' | 'assistant'
  text: string
  raw?: string
}

let msgId = 0
let actId = 0

export function useChat() {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [activities, setActivities] = useState<ActivityEvent[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const addActivity = useCallback((label: string, detail: string, running = true) => {
    const id = ++actId
    setActivities((prev) => [
      { id, label, detail, running, time: new Date().toLocaleTimeString('zh-CN', { hour12: false }) },
      ...prev,
    ])
    return id
  }, [])

  const updateActivity = useCallback((id: number, patch: Partial<ActivityEvent>) => {
    setActivities((prev) => prev.map((a) => (a.id === id ? { ...a, ...patch } : a)))
  }, [])

  const send = useCallback(
    async (text: string) => {
      const userMsg: ChatMessage = { id: ++msgId, role: 'user', text }
      setMessages((prev) => [...prev, userMsg])
      setLoading(true)
      setError(null)

      const actId_ = addActivity('agent 调用', text)

      try {
        const resp = await fetch(`/chat?message=${encodeURIComponent(text)}`)
        if (!resp.ok) {
          throw new Error(`HTTP ${resp.status}`)
        }
        const raw = await resp.text()
        const parsed = parseAssistantText(raw)
        setMessages((prev) => [...prev, { id: ++msgId, role: 'assistant', text: parsed, raw }])
        updateActivity(actId_, { running: false, label: 'agent 完成', detail: parsed.slice(0, 200) })
      } catch (e) {
        const errMsg = e instanceof Error ? e.message : String(e)
        setError(`请求失败：${errMsg}（请确认后端 8080 已启动）`)
        updateActivity(actId_, { running: false, label: 'agent 失败', detail: errMsg })
      } finally {
        setLoading(false)
      }
    },
    [addActivity, updateActivity],
  )

  return { messages, activities, loading, error, send }
}
