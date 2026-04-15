<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api/client'
import { showAlertModal } from '@/ui/alertModal'

const router = useRouter()

const slots = ref([])
const loading = ref(false)
const error = ref('')
const success = ref('')
const deletingId = ref('')

const formattedSlots = computed(() => {
  return slots.value.map(slot => ({
    ...slot,
    formattedDate: formatDate(slot.date),
    formattedDuration: formatDuration(slot.duration),
    formattedAmount: formatCurrency(slot.amount, slot.currency),
    formattedType: formatType(slot.type)
  }))
})

function formatDate(dateStr) {
  if (!dateStr) return '--'
  try {
    return new Date(dateStr).toLocaleDateString('zh-CN')
  } catch {
    return dateStr
  }
}

function formatDuration(minutes) {
  const mins = Number(minutes)
  return mins > 0 ? `${mins} min` : '--'
}

function formatCurrency(amount, currency = 'CNY') {
  const num = Number(amount)
  if (!Number.isFinite(num)) return '--'
  return `${num.toFixed(2)} ${currency}`
}

function formatType(type) {
  return String(type || '--')
}

function sortSlots(rows) {
  return [...rows].sort((a, b) =>
    (a.date + a.start).localeCompare(b.date + b.start)
  )
}

async function loadSlots() {
  loading.value = true
  try {
    const res = await api.specialistListSlots()
    slots.value = sortSlots(Array.isArray(res) ? res : [])
  } catch (e) {
    error.value = e?.message || 'Failed to load'
    showAlertModal({ type: 'error', message: error.value })
  } finally {
    loading.value = false
  }
}

async function handleDelete(id) {
  if (!confirm('Delete this slot?')) return
  deletingId.value = id
  try {
    await api.specialistDeleteSlot(id)
    await loadSlots()
    success.value = `Slot ${id} deleted`
    showAlertModal({ type: 'success', message: success.value })
  } catch (e) {
    error.value = e?.message || 'Delete failed'
    showAlertModal({ type: 'error', message: error.value })
  } finally {
    deletingId.value = ''
  }
}

function handleEdit(id) {
  router.push({ name: 'specialist.slotEdit', params: { id } })
}

function goToCreate() {
  router.push({ name: 'specialist.slotCreate' })
}

onMounted(loadSlots)
</script>

<template>
  <section class="page">
    <header class="page__header">
      <div>
        <h1>Slot Management</h1>
        <p class="subtitle">Manage your consultation slots</p>
      </div>

      <!-- ✅ btn-neutral（不会再被覆盖） -->
      <button class="btn-neutral" @click="goToCreate">
        Create Slot
      </button>
    </header>

    <section class="calc-card">
      <div v-if="error" class="banner banner--error">{{ error }}</div>
      <div v-if="success" class="banner banner--success">{{ success }}</div>

      <!-- ✅ 第一份代码风格 -->
      <div class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>Schedule</th>
              <th>Price</th>
              <th>Session</th>
              <th>Detail</th>
              <th>Availability</th>
              <th class="th-actions">Actions</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="slot in formattedSlots" :key="slot.id">
              
              <td>
                {{ slot.formattedDate }} {{ slot.start }}-{{ slot.end }}
              </td>

              <td>{{ slot.formattedAmount }}</td>

              <td>
                {{ slot.formattedDuration }} · {{ slot.formattedType }}
              </td>

              <td class="cell--detail" :title="slot.detail">
                {{ slot.detail || '--' }}
              </td>

              <td>
                <span
                  class="status-pill"
                  :class="{ 'status-pill--off': !slot.available }"
                >
                  {{ slot.available ? 'Available' : 'Unavailable' }}
                </span>
              </td>

              <td>
                <div class="row-actions">
                  <button class="action-btn" @click="handleEdit(slot.id)">
                    Edit
                  </button>

                  <button
                    class="action-btn action-btn--danger"
                    @click="handleDelete(slot.id)"
                    :disabled="deletingId === slot.id"
                  >
                    {{ deletingId === slot.id ? 'Deleting...' : 'Delete' }}
                  </button>
                </div>
              </td>

            </tr>
          </tbody>
        </table>

        <div v-if="!loading && !formattedSlots.length" class="state state--empty">
          No slots found.
        </div>

        <div v-if="loading" class="state">
          Loading slots...
        </div>
      </div>
    </section>
  </section>
</template>

<style scoped>
.page__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.subtitle {
  color: #4b5563;
}

.calc-card {
  background: #fff;
  border: 1px solid rgba(17,24,39,0.1);
  padding: 16px;
}

/* ✅ 你的按钮风格（最终版） */
.btn-neutral {
  height: 44px;
  padding: 0 20px;
  border: 1px solid #d8d1cb;
  background: #ffffff;
  color: #374151;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.btn-neutral:hover {
  background: #f8f5f2;
}

/* ✅ 表格风格（第一份代码） */
.table-wrap {
  overflow-x: auto;
  border: 1px solid #eceff3;
}

.table {
  width: 100%;
  border-collapse: collapse;
  min-width: 900px;
}

.table th,
.table td {
  padding: 12px 14px;
  border-bottom: 1px solid #eceff3;
  text-align: center;
}

.table th {
  background: #fafafa;
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
}

.cell--detail {
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ✅ 状态 */
.status-pill {
  padding: 4px 10px;
  border: 1px solid rgba(34,197,94,0.3);
  background: rgba(34,197,94,0.08);
  color: #166534;
  font-size: 12px;
  font-weight: 700;
}

.status-pill--off {
  border-color: rgba(248,113,113,0.3);
  background: rgba(248,113,113,0.12);
  color: #991b1b;
}

/* ✅ 操作按钮 */
.row-actions {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.action-btn {
  height: 34px;
  padding: 0 12px;
  border: 1px solid #202124;
  background: #fff;
  cursor: pointer;
  font-weight: 700;
}

.action-btn--danger {
  border-color: #a94442;
  color: #a94442;
}

/* 状态 */
.state {
  text-align: center;
  padding: 16px;
  color: #6b7280;
}

.state--empty {
  border-style: dashed;
}
</style>