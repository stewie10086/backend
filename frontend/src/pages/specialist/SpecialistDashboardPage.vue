<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { api } from '@/api/client'

const pending = ref({ items: [], total: 0 })
const loading = ref(false)
const error = ref('')

async function load() {
  error.value = ''
  loading.value = true
  try {
    pending.value = await api.listBookingRequests({ status: 'Pending', pageSize: 20 })
  } catch (e) {
    error.value = e?.message || 'Failed to load'
    pending.value = { items: [], total: 0 }
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>Specialist Dashboard</h1>
    </header>

    <div v-if="error" class="banner banner--error" role="alert">{{ error }}</div>

    <div class="card">
      <div class="title">Pending Requests</div>
      <p v-if="loading" class="muted">Loading…</p>
      <p v-else class="stat">
        <span class="num">{{ pending.total ?? (pending.items || []).length }}</span>
        <span class="muted">items</span>
      </p>
      <RouterLink class="link" :to="{ name: 'specialist.requests' }">Go to Requests</RouterLink>
    </div>

    <ul v-if="(pending.items || []).length" class="preview">
      <li v-for="b in pending.items.slice(0, 5)" :key="b.id" class="preview__row">
        <span class="mono">{{ b.id }}</span>
        <span class="muted small">{{ b.customerName ?? b.customerId ?? '—' }}</span>
        <span class="small">{{ b.time ?? b.startTime }}</span>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.page__header h1 {
  margin: 0 0 6px;
  font-size: 22px;
}
.muted {
  opacity: 0.8;
}
.small {
  font-size: 12px;
}
.card {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
}
.title {
  font-weight: 700;
  margin-bottom: 8px;
}
.stat {
  margin: 0 0 10px;
}
.num {
  font-size: 28px;
  font-weight: 800;
}
.link {
  color: inherit;
  font-weight: 600;
  text-decoration: underline;
  text-underline-offset: 3px;
}
.preview {
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 8px;
}
.preview__row {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  font-size: 13px;
}
.mono {
  font-family: ui-monospace, monospace;
}
.banner {
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: 0;
  font-size: 13px;
}
.banner--error {
  border: 1px solid rgba(248, 113, 113, 0.45);
  background: rgba(248, 113, 113, 0.12);
  color: #991b1b;
}
@media (max-width: 640px) {
  .preview__row {
    grid-template-columns: 1fr;
  }
}
</style>
