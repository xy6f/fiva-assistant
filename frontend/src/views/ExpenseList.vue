<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2 style="margin:0">费用报销管理</h2>
      <div>
        <el-button type="success" @click="handleExport">导出Excel</el-button>
        <el-button type="primary" @click="openCreate">新建报销单</el-button>
      </div>
    </div>

    <!-- 搜索和统计 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="8">
        <el-input v-model="keyword" placeholder="搜索报销人/标题" clearable @clear="loadList" @keyup.enter="loadList">
          <template #append>
            <el-button @click="loadList"><el-icon><Search /></el-icon></el-button>
          </template>
        </el-input>
      </el-col>
      <el-col :span="16">
        <div style="display:flex;gap:12px;justify-content:flex-end">
          <el-tag type="warning" size="large">待审批: {{ stats.pendingCount || 0 }}</el-tag>
          <el-tag type="success" size="large">已通过: {{ stats.approvedCount || 0 }}</el-tag>
          <el-tag type="danger" size="large">已驳回: {{ stats.rejectedCount || 0 }}</el-tag>
        </div>
      </el-col>
    </el-row>

    <!-- 报销列表 -->
    <el-card>
      <el-table :data="list" stripe style="width:100%" v-loading="loading">
        <el-table-column prop="title" label="报销标题" min-width="150" />
        <el-table-column prop="applicant" label="报销人" width="100" />
        <el-table-column prop="category" label="类别" width="90" />
        <el-table-column label="金额" width="120">
          <template #default="{ row }">
            <span style="font-weight:bold;color:#303133">¥{{ formatMoney(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="expenseDate" label="费用日期" width="110" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="handleApprove(row, true)">通过</el-button>
            <el-button v-if="row.status === 'PENDING'" size="small" type="danger" @click="handleApprove(row, false)">驳回</el-button>
            <el-button v-if="row.status === 'PENDING'" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'PENDING'" size="small" type="danger" plain @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建/编辑弹窗 -->
    <ExpenseForm v-model="formVisible" :editData="editData" @saved="loadList" />

    <!-- 审批弹窗 -->
    <el-dialog title="审批操作" v-model="approveVisible" width="400px">
      <el-form label-width="80px">
        <el-form-item label="审批人">
          <el-input v-model="approveForm.approver" placeholder="请输入审批人" />
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="approveForm.opinion" type="textarea" :rows="2" placeholder="请输入审批意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button :type="approveForm.approved ? 'success' : 'danger'" @click="submitApprove" :loading="approving">
          {{ approveForm.approved ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getExpenseList, approveExpense, deleteExpense, getExpenseStatistics, exportExpenseExcel } from '../api/expense'
import ExpenseForm from './ExpenseForm.vue'

const list = ref([])
const stats = ref({})
const loading = ref(false)
const keyword = ref('')

const formVisible = ref(false)
const editData = ref(null)

const approveVisible = ref(false)
const approving = ref(false)
const approveForm = ref({ id: null, approved: true, approver: '管理员', opinion: '' })

function formatMoney(val) {
  if (!val) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function statusType(s) {
  return s === 'APPROVED' ? 'success' : s === 'REJECTED' ? 'danger' : 'warning'
}
function statusText(s) {
  return s === 'APPROVED' ? '已通过' : s === 'REJECTED' ? '已驳回' : '待审批'
}

async function loadList() {
  loading.value = true
  try {
    const { data } = await getExpenseList(keyword.value)
    list.value = data.data
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

async function loadStats() {
  try {
    const { data } = await getExpenseStatistics()
    stats.value = data.data
  } catch {}
}

function openCreate() {
  editData.value = null
  formVisible.value = true
}

function openEdit(row) {
  editData.value = { ...row }
  formVisible.value = true
}

function handleApprove(row, approved) {
  approveForm.value = { id: row.id, approved, approver: '管理员', opinion: '' }
  approveVisible.value = true
}

async function submitApprove() {
  approving.value = true
  try {
    await approveExpense(approveForm.value.id, approveForm.value)
    ElMessage.success(approveForm.value.approved ? '已通过' : '已驳回')
    approveVisible.value = false
    loadList()
    loadStats()
  } catch (e) {
    ElMessage.error('审批失败')
  } finally { approving.value = false }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.title}」？`, '提示', { type: 'warning' })
    await deleteExpense(row.id)
    ElMessage.success('删除成功')
    loadList()
    loadStats()
  } catch {}
}

onMounted(() => {
  loadList()
  loadStats()
})

function handleExport() {
  exportExpenseExcel(keyword.value).then(({ data }) => {
    const blob = new Blob([data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `费用报销_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('Excel 导出成功')
  }).catch(() => { ElMessage.error('Excel 导出失败') })
}
</script>
