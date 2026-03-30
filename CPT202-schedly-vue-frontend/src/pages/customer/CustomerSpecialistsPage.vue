<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { api } from '@/api/client'

const expertiseList = ref([])
const page = ref({ items: [], total: 0, page: 1, pageSize: 10 })
const expertiseId = ref('')
const keyword = ref('')
const loading = ref(false)
const error = ref('')

const filteredItems = computed(() => {
  const q = keyword.value.trim().toLowerCase()
  const items = page.value.items ?? []
  if (!q) return items
  return items.filter((s) => {
    const name = (s.name ?? '').toLowerCase()
    const ids = (s.expertiseIds ?? []).join(' ').toLowerCase()
    return name.includes(q) || ids.includes(q)
  })
})

async function loadExpertise() {
  try {
    expertiseList.value = await api.listExpertise()
  } catch {
    expertiseList.value = []
  }
}

async function loadSpecialists() {
  error.value = ''
  loading.value = true
  try {
    const params = {}
    if (expertiseId.value) params.expertiseId = expertiseId.value
    page.value = await api.listSpecialists(params)
  } catch (e) {
    error.value = e?.message || 'Failed to load'
    page.value = { items: [], total: 0, page: 1, pageSize: 10 }
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadExpertise()
  await loadSpecialists()
})

watch(expertiseId, () => loadSpecialists())
</script>

<template>
  <section class="page">
    <header class="page__header">
      <h1>Specialists</h1>
    </header>

    <div class="panel">
      <label class="field">
        <div class="label">Filter by Expertise</div>
        <select v-model="expertiseId" class="input">
          <option value="">All</option>
          <option v-for="e in expertiseList" :key="e.id" :value="e.id">
            {{ e.name }}
          </option>
        </select>
      </label>
      <label class="field">
        <div class="label">Keyword</div>
        <input v-model="keyword" class="input" placeholder="Name or expertise ID..." />
      </label>
      <button type="button" class="btn" :disabled="loading" @click="loadSpecialists">Refresh</button>
    </div>

    <div v-if="error" class="banner banner--error" role="alert">{{ error }}</div>

    <div v-if="loading && !(page.items || []).length" class="card muted">Loading…</div>

    <div v-else-if="!filteredItems.length && !error" class="empty">
      <div class="empty__title">No specialists found</div>
      <p class="muted">Try changing filters or wait for admin updates.</p>
    </div>

    <ul v-else class="list">
      <li v-for="s in filteredItems" :key="s.id" class="card card--row">
        <div>
          <div class="name">{{ s.name ?? '—' }}</div>
          <div class="muted small mono">ID: {{ s.id ?? '—' }}</div>
          <div v-if="s.price != null" class="muted small">Reference Price: {{ s.price }}</div>
        </div>
        <RouterLink class="link" :to="{ name: 'customer.specialistDetail', params: { id: s.id } }">
          View Details
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
.panel {
  margin-top: 14px;
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 12px;
  align-items: end;
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
  width: 100%;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.14);
  background: #ffffff;
  color: #111827;
  outline: none;
}
select.input {
  cursor: pointer;
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
  cursor: not-allowed;
}
.muted {
  opacity: 0.8;
}
.small {
  font-size: 12px;
  margin-top: 4px;
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
.list {
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 10px;
}
.card {
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
}
.card--row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.name {
  font-weight: 700;
}
.mono {
  font-family: ui-monospace, monospace;
}
.link {
  color: inherit;
  font-weight: 600;
  text-decoration: underline;
  text-underline-offset: 3px;
  white-space: nowrap;
}
@media (max-width: 720px) {
  .panel {
    grid-template-columns: 1fr;
  }
}
</style>
