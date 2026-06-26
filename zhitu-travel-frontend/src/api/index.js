import axios from 'axios'

const API_BASE_URL = process.env.NODE_ENV === 'production'
  ? '/api'
  : 'http://localhost:8123/api'

const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000
})

export const connectSSE = (url, params, onMessage, onError) => {
  const queryString = Object.keys(params)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')
  const fullUrl = `${API_BASE_URL}${url}?${queryString}`
  const eventSource = new EventSource(fullUrl)

  eventSource.onmessage = event => {
    let data = event.data
    if (data === '[DONE]') {
      if (onMessage) onMessage('[DONE]')
    } else {
      if (onMessage) onMessage(data)
    }
  }

  eventSource.onerror = error => {
    if (onError) onError(error)
    eventSource.close()
  }

  return eventSource
}

// 知途AI出行规划聊天
export const chatWithTravelPlanner = (message, chatId) => {
  return connectSSE('/ai/travel/chat/sse', { message, chatId })
}

// AI超级智能体聊天
export const chatWithManus = (message) => {
  return connectSSE('/ai/travel/agent/chat', { message })
}

export default {
  chatWithTravelPlanner,
  chatWithManus
}
