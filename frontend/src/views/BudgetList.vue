<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2 style="margin:0">预算管理</h2>
      <el-button type="primary" @click="openCreate">新建预算</el-button>
    </div>

    <!-- 筛选 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6">
        <el-input-number v-model="filterYear" :min="2020" :max="2030" placeholder="年度" @change="loadList" style="width:100%" />
      </el-col>
      <el-col :span="6">
        <el-select v-model="filterMonth" placeholder="月份" clearable @change="loadList" style="width:100%">
          <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m" />
        </el-select>
      </el-col>
      <el-col :span="12">
        <div style="display:flex;gap:12px;justify-content:flex-end">
          <el-tag type="success" size="large">预算总额: ¥{{ formatMoney(totalBudget) }}</el-tag>
          <el-tag type="warning" size="large">已使用: ¥{{ formatMoney(totalUsed) }}</el-tag>
          <el-tag type="danger" size="large">超预算: {{ overBudgetCount }}</el-tag>
        </div>
      </el-col>
    </el-row>

    <!-- 预算列表 -->
    <el-card>
      <el-table :data="list" stripe style="width:100%" v-loading="loading">
        <el-table-column prop="department" label="部门" width="120" />
        <el-table-column label="期间" width="100">
          <template #default="{ row }">{{ row.year }}-{{ String(row.month).padStart(2, '0') }}</template>
        </el-table-column>
        <el-table-column prop="category" label="类别" width="90" />
        <el-table-column label="预算金额" width="120">
          <template #default="{ row }">
            <span style="font-weight:bold">¥{{ formatMoney(row.budgetAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="已使用" width="120">
          <template #default="{ row }">
            <span :style="{ color: row.overBudget ? '#f56c6c' : '#303133' }">
              ¥{{ formatMoney(row.usedAmount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="执行进度" min-width="200">
          <template #default="{ row }">
            <el-progress 
              :percentage="Math.min(row.usagePercent, 100)" 
              :status="row.overBudget ? 'exception' : row.alertTriggered ? 'warning' : 'success'"
              :stroke-width="18"
              :text-inside="true"
            />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.overBudget" type="danger" size="small">超支</el-tag>
            <el-tag v-else-if="row.alertTriggered" type="warning" size="small">预警</el-tag>
            <el-tag v-else type="success" size="small">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建/编辑弹窗 -->
    <BudgetForm v-model="formVisible" :editData="editData" @saved="loadList" />

    <!-- 更新已用金额弹窗 -->
    <el-dialog title="更新已使用金额" v-model="updateUsedVisible" width="400px">
      <el-form label-width="100px">
        <el-form-item label="预算金额">
          <span>¥{{ formatMoney(currentBudget?.budgetAmount) }}</span>
        </el-form-item>
        <el-form-item label="已使用金额">
          <el-input-number v-model="newUsedAmount" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="updateUsedVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUpdateUsed" :loading="updating">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBudgetList, deleteBudget, updateBudgetUsed } from '../api/budget'
import BudgetForm from './BudgetForm.vue'

const list = ref([])
const loading = ref(false)
const now = new Date()
const filterYear = ref(now.getFullYear())
const filterMonth = ref(null)

const formVisible = ref(false)
const editData = ref(null)

const updateUsedVisible = ref(false)
const updating = ref(false)
const currentBudget = ref(null)
const newUsedAmount = ref(0)

function formatMoney(val) {
  if (!val) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const totalBudget = computed(() => list.value.reduce((sum, b) => sum + (b.budgetAmount || 0), 0))
const totalUsed = computed(() => list.value.reduce((sum, b) => sum + (b.usedAmount || 0), 0))
const overBudgetCount = computed(() => list.value.filter(b => b.overBudget).length)

async function loadList() {
  loading.value = true
  try {
    const { data } = await getBudgetList(filterYear.value, filterMonth.value)
    list.value = data.data.map(b => ({
      ...b,
      usagePercent: b.budgetAmount > 0 ? Number((b.usedAmount / b.budgetAmount * 100).toFixed(1)) : 0,
      overBudget: b.usedAmount > b.budgetAmount,
      alertTriggered: b.budgetAmount > 0 && (b.usedAmount / b.budgetAmount * 100) >= b.alertThreshold
    }))
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

function openCreate() {
  editData.value = null
  formVisible.value = true
}

function openEdit(row) {
  editData.value = { ...row }
  formVisible.value = true
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.department} ${row.year}-${row.month}」的预算？`, '提示', { type: 'warning' })
    await deleteBudget(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch {}
}

function openUpdateUsed(row) {
  currentBudget.value = row
  newUsedAmount.value = row.usedAmount || 0
  updateUsedVisible.value = true
}

async function submitUpdateUsed() {
  updating.value = true
  try {
    await updateBudgetUsed(currentBudget.value.id, newUsedAmount.value)
    ElMessage.success('更新成功')
    updateUsedVisible.value = false
    loadList()
  } catch { ElMessage.error('更新失败') }
  finally { updating.value = false }
}

onMounted(() => loadList())
</script>
