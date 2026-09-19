<script setup>
import { ref, onMounted } from 'vue'

const messages = ref([])
const content = ref('')
const loading = ref(false)
const error = ref('')

async function load() {
  error.value = ''
  try {
    const res = await fetch('/api/messages')
    if (!res.ok) throw new Error('HTTP ' + res.status)
    messages.value = await res.json()
  } catch (e) {
    error.value = '加载失败：' + e.message
  }
}

async function add() {
  if (!content.value.trim()) return
  loading.value = true
  error.value = ''
  try {
    const res = await fetch('/api/messages', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ content: content.value.trim() })
    })
    if (!res.ok) throw new Error('HTTP ' + res.status)
    content.value = ''
    await load()
  } catch (e) {
    error.value = '提交失败：' + e.message
  } finally {
    loading.value = false
  }
}

function fmt(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').slice(0, 19)
}

onMounted(load)
</script>

<template>
  <div class="wrap">
    <header>
      <h1>K8s 全栈演示站点<span class="badge">Ingress → Service → Pod → DB</span></h1>
      <p>Vue 前端 + Spring Boot 后端 + MySQL，数据经 K8s Secret 注入、NFS PVC 持久化</p>
    </header>

    <div class="card">
      <div class="form">
        <input v-model="content" @keyup.enter="add" placeholder="输入一条留言，回车或点击提交" />
        <button :disabled="loading" @click="add">{{ loading ? '提交中…' : '提交' }}</button>
      </div>
      <p v-if="error" class="err">{{ error }}</p>
    </div>

    <div class="card">
      <div class="status">共 {{ messages.length }} 条留言</div>
      <ul class="list">
        <li v-for="m in messages" :key="m.id">
          <span>{{ m.content }}</span>
          <time>{{ fmt(m.createdAt) }}</time>
        </li>
      </ul>
      <p v-if="!messages.length" class="status">暂无数据，先在上面新增一条吧。</p>
    </div>
  </div>
</template>
