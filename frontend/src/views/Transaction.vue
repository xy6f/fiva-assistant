<template>
  <div>
    <h2 style="margin-bottom:20px">收支记账</h2>

    <!-- Summary Cards -->
    <el-row :gutter="20" style="margin-bottom:20px">
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>本月收入</template>
          <div class="card-value income">{{ formatMoney(summary.monthlyIncome) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>本月支出</template>
          <div class="card-value expense">{{ formatMoney(summary.monthlyExpense) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>本月利润</template>
          <div class="card-value" :class="summary.monthlyProfit >= 0 ? 'income' : 'expense'">
            {{ formatMoney(summary.monthlyProfit) }}
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <template #header>年度净利润</template>
          <div class="card-value" :class="summary.netProfit >= 0 ? 'income' : 'expense'">
            {{ formatMoney(summary.netProfit) }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Add Form -->
    <el-card style="margin-bottom:20px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>{{ editingId ? '编辑记录' : '新增记录' }}</span>
          <el-button v-if="editingId" size="small" @click="resetForm">取消编辑</el-button>
        </div>
      </template>
      <el-form :model="form" label-width="80px" :inline="true">
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio-button value="INCOME">收入</el-radio-button>
            <el-radio-button value="EXPENSE">支出</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="form.amount" :min="0" :precision="2" :step="100" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="选择分类">
            <el-option-group v-if="form.type === 'INCOME'" label="收入分类">
              <el-option label="销售收入" value="销售收入" />
              <el-option label="服务收入" value="服务收入" />
              <el-option label="投资收益" value="投资收益" />
              <el-option label="其他收入" value="其他收入" />
            </el-option-group>
            <el-option-group v-else label="支出分类">
              <el-option label="工资薪金" value="工资薪金" />
              <el-option label="办公费用" value="办公费用" />
              <el-option label="房租物业" value="房租物业" />
              <el-option label="水电费" value="水电费" />
              <el-option label="原材料" value="原材料" />
              <el-option label="差旅费" value="差旅费" />
              <el-option label="招待费" value="招待费" />
              <el-option label="税费" value="税费" />
              <el-option label="其他支出" value="其他支出" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="form.transactionDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="往来方">
          <el-input v-model="form.counterparty" placeholder="对方名称" style="width:160px" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.description" placeholder="备注说明" style="width:200px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">
            {{ editingId ? '更新' : '保存' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Transaction List -->
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>收支记录</span>
          <div style="display:flex;gap:12px;align-items:center">
            <el-button type="success" size="small" @click="handleExport">导出Excel</el-button>
            <el-radio-group v-model="filterType" size="small" @change="loadList">
              <el-radio-button value="">全部</el-radio-button>
              <el-radio-button value="INCOME">收入</el-radio-button>
              <el-radio-button value="EXPENSE">支出</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>
      <el-table :data="list" style="width:100%" empty-text="暂无记录">
        <el-table-column prop="transactionDate" label="日期" width="120" />
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.type === 'INCOME' ? 'success' : 'danger'" size="small">
              {{ row.type === 'INCOME' ? '收入' : '支出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column prop="amount" label="金额" width="130">
          <template #default="{ row }">
            <span :style="{ color: row.type === 'INCOME' ? '#67c23a' : '#f56c6c', fontWeight: 'bold' }">
              {{ row.type === 'INCOME' ? '+' : '-' }}{{ formatMoney(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="counterparty" label="往来方" width="150" />
        <el-table-column prop="description" label="备注" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="editRow(row)">编辑</el-button>
            <el-popconfirm title="确认删除?" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTransactionList, getTransactionSummary, createTransaction, updateTransaction, deleteTransaction, exportTransactionExcel } from '../api/transaction'

const summary = ref({ monthlyIncome: 0, monthlyExpense: 0, monthlyProfit: 0, netProfit: 0 })
const list = ref([])
const filterType = ref('')
const editingId = ref(null)
const saving = ref(false)

const form = ref({
  type: 'EXPENSE',
  amount: 0,
  category: '',
  transactionDate: new Date().toISOString().split('T')[0],
  counterparty: '',
  description: ''
})

function formatMoney(val) {
  if (!val) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function loadSummary() {
  try {
    const { data } = await getTransactionSummary()
    summary.value = data
  } catch (e) {}
}

async function loadList() {
  try {
    const params = filterType.value ? { type: filterType.value } : {}
    const { data } = await getTransactionList(params)
    list.value = data
  } catch (e) {}
}

function editRow(row) {
  editingId.value = row.id
  form.value = { ...row }
}

function resetForm() {
  editingId.value = null
  form.value = {
    type: 'EXPENSE',
    amount: 0,
    category: '',
    transactionDate: new Date().toISOString().split('T')[0],
    counterparty: '',
    description: ''
  }
}

async function handleSave() {
  if (!form.value.amount || form.value.amount <= 0) {
    ElMessage.warning('请输入有效金额')
    return
  }
  if (!form.value.category) {
    ElMessage.warning('请选择分类')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateTransaction(editingId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await createTransaction(form.value)
      ElMessage.success('保存成功')
    }
    resetForm()
    loadList()
    loadSummary()
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    saving.value = false
  }
}

async function handleDelete(id) {
  try {
    await deleteTransaction(id)
    ElMessage.success('删除成功')
    loadList()
    loadSummary()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadSummary()
  loadList()
})

function handleExport() {
  const params = filterType.value ? { type: filterType.value } : {}
  exportTransactionExcel(params).then(({ data }) => {
    const blob = new Blob([data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `收支记录_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('Excel 导出成功')
  }).catch(() => { ElMessage.error('Excel 导出失败') })
}
</script>

<style scoped>
.card-value { font-size: 28px; font-weight: bold; text-align: center; padding: 8px 0; }
.income { color: #67c23a; }
.expense { color: #f56c6c; }
</style>
