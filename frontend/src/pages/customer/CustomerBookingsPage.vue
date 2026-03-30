<script setup>
import { onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { api } from '@/api/client'

const status = ref('')
const page = ref({ items: [], total: 0, page: 1, pageSize: 10 })
const loading = ref(false)
const error = ref('')

async function load() {
  error.value = ''
  loading.value = true
  try {
    const params = {}
    if (status.value) params.status = status.value
    page.value = await api.listMyBookings(params)
  } catch (e) {
    error.value = e?.message || 'Failed to load'
    page.value = { items: [], total: 0, page: 1, pageSize: 10 }
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(status, () => load())
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>My Bookings</h1>
    </header>

    <div class="toolbar card">
      <label class="field">
        <span class="label">Status</span>
        <select v-model="status" class="input">
          <option value="">All</option>
          <option value="Pending">Pending</option>
          <option value="Confirmed">Confirmed</option>
          <option value="Cancelled">Cancelled</option>
          <option value="Completed">Completed</option>
          <option value="Rejected">Rejected</option>
        </select>
      </label>
      <button type="button" class="btn" :disabled="loading" @click="load">Refresh</button>
    </div>

    <div v-if="error" class="banner banner--error" role="alert">{{ error }}</div>

    <div v-if="loading && !(page.items || []).length" class="card muted">Loading…</div>

    <div v-else-if="!(page.items || []).length && !error" class="empty">
      <div class="empty__title">No bookings yet</div>
      <p class="muted">Create a booking from a specialist detail page.</p>
    </div>

    <ul v-else class="list">
      <li v-for="b in page.items" :key="b.id" class="card row">
        <div>
          <div class="mono id">{{ b.id }}</div>
          <div class="muted small">Time: {{ b.time ?? b.startTime ?? '—' }}</div>
          <div class="status">{{ b.status ?? '—' }}</div>
        </div>
        <RouterLink class="link" :to="{ name: 'customer.bookingDetail', params: { id: b.id } }">
          Details
        </RouterLink>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.page__header h1 {
  margin: 0 0 6px;
  font-size: 22px;
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  gap: 12px;
  margin-top: 14px;
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
}
.field {
  display: grid;
  gap: 6px;
}
.label {
  font-size: 13px;
  opacity: 0.85;
}
.input {
  min-width: 160px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: #ffffff;
  color: #111827;
}
.btn {
  padding: 10px 16px;
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.1);
  color: inherit;
  cursor: pointer;
  height: 42px;
}
.btn:disabled {
  opacity: 0.5;
}
.muted {
  opacity: 0.8;
}
.small {
  font-size: 12px;
  margin-top: 4px;
}
.card {
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
}
.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.list {
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}
.id {
  font-weight: 600;
}
.status {
  margin-top: 6px;
  font-size: 13px;
  font-weight: 600;
}
.mono {
  font-family: ui-monospace, monospace;
  font-size: 12px;
}
.link {
  color: inherit;
  font-weight: 600;
  text-decoration: underline;
  text-underline-offset: 3px;
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
.empty {
  margin-top: 16px;
  padding: 18px;
  border: 1px dashed rgba(255, 255, 255, 0.16);
  border-radius: 14px;
}
.empty__title {
  font-weight: 700;
  margin-bottom: 6px;
}
</style>
