<template>
  <div class="travel-container">
    <div class="header">
      <div class="back-button" @click="goBack">返回</div>
      <h1 class="title">知途AI出行规划</h1>
      <div class="chat-id">会话ID: {{ chatId }}</div>
    </div>

    <div class="content-wrapper">
      <div class="chat-area">
        <ChatRoom
          :messages="messages"
          :connection-status="connectionStatus"
          ai-type="travel"
          @send-message="sendMessage"
        />
      </div>
    </div>

    <div class="footer-container">
      <AppFooter />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useHead } from '@vueuse/head'
import ChatRoom from '../components/ChatRoom.vue'
import AppFooter from '../components/AppFooter.vue'
import { chatWithTravelPlanner } from '../api'

useHead({
  title: '知途AI出行规划 - AI智能助手平台',
  meta: [
    { name: 'description', content: '知途AI出行规划，帮你智能规划旅行方案，包括景点推荐、行程安排、预算规划等' },
    { name: 'keywords', content: 'AI出行规划,旅行规划,智能行程,旅游攻略,AI旅行助手' }
  ]
})

const router = useRouter()
const messages = ref([])
const chatId = ref('')
const connectionStatus = ref('disconnected')
let eventSource = null

const generateChatId = () => 'travel_' + Math.random().toString(36).substring(2, 10)

const addMessage = (content, isUser) => {
  messages.value.push({ content, isUser, time: new Date().getTime() })
}

const sendMessage = (message) => {
  addMessage(message, true)
  if (eventSource) eventSource.close()

  const aiMessageIndex = messages.value.length
  addMessage('', false)

  connectionStatus.value = 'connecting'
  eventSource = chatWithTravelPlanner(message, chatId.value)

  eventSource.onmessage = (event) => {
    const data = event.data
    if (data && data !== '[DONE]') {
      if (aiMessageIndex < messages.value.length) {
        messages.value[aiMessageIndex].content += data
      }
    }
    if (data === '[DONE]') {
      connectionStatus.value = 'disconnected'
      eventSource.close()
    }
  }

  eventSource.onerror = (error) => {
    console.error('SSE Error:', error)
    connectionStatus.value = 'error'
    eventSource.close()
  }
}

const goBack = () => router.push('/')

onMounted(() => {
  chatId.value = generateChatId()
  addMessage('你好！我是知途AI出行规划助手。你可以告诉我想去哪里、预算多少、玩几天，我来帮你规划行程。例如："帮我规划一个北京三日游，预算3000"', false)
})

onBeforeUnmount(() => {
  if (eventSource) eventSource.close()
})
</script>

<style scoped>
.travel-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #f0f7ff;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: linear-gradient(135deg, #2196F3, #00BCD4);
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 10;
}
.back-button { font-size: 16px; cursor: pointer; display: flex; align-items: center; transition: opacity 0.2s; }
.back-button:hover { opacity: 0.8; }
.back-button:before { content: '←'; margin-right: 8px; }
.title { font-size: 20px; font-weight: bold; margin: 0; }
.chat-id { font-size: 14px; opacity: 0.8; }
.content-wrapper { display: flex; flex-direction: column; flex: 1; }
.chat-area {
  flex: 1; padding: 16px; overflow: hidden; position: relative;
  min-height: calc(100vh - 56px - 180px); margin-bottom: 16px;
}
.footer-container { margin-top: auto; }
@media (max-width: 768px) {
  .header { padding: 12px 16px; }
  .title { font-size: 18px; }
  .chat-id { font-size: 12px; }
  .chat-area { padding: 12px; }
}
@media (max-width: 480px) {
  .header { padding: 10px 12px; }
  .back-button { font-size: 14px; }
  .title { font-size: 16px; }
  .chat-id { display: none; }
  .chat-area { padding: 8px; }
}
</style>
