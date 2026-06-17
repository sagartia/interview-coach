<template>
  <div class="container">
    <header>
      <h1>AI 面試教練</h1>
      <p>模擬技術面試，即時獲得評分與建議</p>
    </header>

    <!-- 輸入職位畫面 -->
    <div v-if="stage === 'input'" class="start-screen">
      <div class="start-card">
        <h2>你要面試什麼職位？</h2>
        <input
          v-model="role"
          @keyup.enter="startInterview"
          placeholder="例如：Java工程師、前端工程師、全端工程師"
        />
        <button @click="startInterview" :disabled="!role.trim() || loading">
          {{ loading ? '準備中...' : '開始面試' }}
        </button>
      </div>
    </div>

    <!-- 面試進行畫面 -->
    <div v-if="stage === 'interview'" class="interview-screen">
      <div class="chat-box" ref="chatBox">
        <div
          v-for="(msg, index) in messages"
          :key="index"
          :class="['message', msg.role]"
        >
          <div class="bubble">{{ msg.content }}</div>
        </div>
        <div v-if="loading" class="message assistant">
          <div class="bubble loading">評分中...</div>
        </div>
      </div>

      <div class="input-area">
        <textarea
          v-model="userInput"
          @keydown.enter.ctrl="submitAnswer"
          placeholder="輸入你的回答... （Ctrl + Enter 送出）"
          :disabled="loading"
          rows="3"
        />
        <button @click="submitAnswer" :disabled="loading || !userInput.trim()">
          {{ loading ? '評分中...' : '送出回答' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'

const stage = ref('input')
const role = ref('')
const loading = ref(false)
const messages = ref([])
const userInput = ref('')
const sessionId = ref('')
const currentQuestion = ref('')
const chatBox = ref(null)

async function startInterview() {
  if (!role.value.trim() || loading.value) return
  loading.value = true

  try {
    const response = await fetch('http://localhost:8080/api/interview/start', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(role.value.trim())
    })
    const text = await response.text()
    const [id, ...rest] = text.split('|||')
    sessionId.value = id
    const content = rest.join('|||')
    currentQuestion.value = content
    messages.value.push({ role: 'assistant', content })
    stage.value = 'interview'
  } catch (e) {
    alert('連線失敗，請確認後端是否啟動')
  } finally {
    loading.value = false
  }
}

async function submitAnswer() {
  const text = userInput.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  userInput.value = ''
  loading.value = true

  await nextTick()
  chatBox.value.scrollTop = chatBox.value.scrollHeight

  try {
    const response = await fetch('http://localhost:8080/api/interview/answer', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId: sessionId.value,
        answer: text,
        currentQuestion: currentQuestion.value
      })
    })
    const reply = await response.text()
    currentQuestion.value = reply
    messages.value.push({ role: 'assistant', content: reply })
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '發生錯誤，請稍後再試。' })
  } finally {
    loading.value = false
    await nextTick()
    chatBox.value.scrollTop = chatBox.value.scrollHeight
  }
}
</script>

<style scoped>
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px 16px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

header h1 {
  font-size: 1.5rem;
  color: #1a1a1a;
}

header p {
  font-size: 0.9rem;
  color: #666;
  margin-top: 4px;
}

.start-screen {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.start-card {
  background: white;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.08);
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  max-width: 480px;
}

.start-card h2 {
  font-size: 1.2rem;
  color: #1a1a1a;
}

.start-card input {
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
  outline: none;
}

.start-card input:focus {
  border-color: #6c63ff;
}

.start-card button {
  padding: 12px;
  background: #6c63ff;
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  cursor: pointer;
}

.start-card button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.interview-screen {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

.chat-box {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: #f5f5f5;
  border-radius: 12px;
}

.message {
  display: flex;
}

.message.user {
  justify-content: flex-end;
}

.message.assistant {
  justify-content: flex-start;
}

.bubble {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 0.95rem;
  line-height: 1.6;
  white-space: pre-wrap;
}

.message.user .bubble {
  background: #6c63ff;
  color: white;
  border-bottom-right-radius: 4px;
}

.message.assistant .bubble {
  background: white;
  color: #1a1a1a;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

.loading {
  color: #999;
  font-style: italic;
}

.input-area {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

textarea {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #ddd;
  border-radius: 12px;
  font-size: 0.95rem;
  outline: none;
  resize: none;
  font-family: inherit;
  line-height: 1.5;
}

textarea:focus {
  border-color: #6c63ff;
}

textarea:disabled {
  background: #f9f9f9;
}

.input-area button {
  padding: 12px 20px;
  background: #6c63ff;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 0.95rem;
  cursor: pointer;
  white-space: nowrap;
}

.input-area button:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style>