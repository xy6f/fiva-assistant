<template>
  <div>
    <h2 style="margin-bottom:20px">查看凭证</h2>

    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span style="font-weight:bold;font-size:16px">凭证列表</span>
          <div>
            <el-select v-model="filterStatus" placeholder="按状态筛选" clearable @change="loadVouchers" style="width:130px;margin-right:10px">
              <el-option label="全部" value="" />
              <el-option label="草稿" value="DRAFT" />
              <el-option label="已审核" value="REVIEWED" />
              <el-option label="已过账" value="POSTED" />
            </el-select>
            <el-select v-model="filterType" placeholder="按类型筛选" clearable @change="loadVouchers" style="width:130px;margin-right:10px">
              <el-option label="全部" value="" />
              <el-option label="收入" value="INCOME" />
              <el-option label="支出" value="EXPENSE" />
            </el-select>
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
              start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD"
              @change="loadVouchers" style="width:240px;margin-right:10px" />
            <el-button @click="handleExport">导出Excel</el-button>
          </div>
        </div>
      </template>
      <el-table :data="vouchers" stripe v-loading="loading" @row-click="showDetail">
        <el-table-column prop="voucherNo" label="凭证编号" width="160" />
        <el-table-column prop="voucherDate" label="凭证日期" width="120" />
        <el-table-column prop="type" label="类型" width="80">
          <template #default="{row}">
            <el-tag :type="row.type === 'INCOME' ? 'success' : 'danger'" size="small">
              {{ row.type === 'INCOME' ? '收入' : '支出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{row}">¥{{ row.amount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="counterparty" label="往来方" min-width="120" show-overflow-tooltip />
        <el-table-column prop="description" label="摘要" min-width="120" show-overflow-tooltip />
        <el-table-column label="分录数" width="80" align="center">
          <template #default="{row}">{{ row.entries?.length || 0 }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{row}">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{row}">
            <template v-if="row.status === 'DRAFT'">
              <el-button size="small" type="warning" @click.stop="handleSubmit(row.id)">提交审核</el-button>
              <el-button size="small" @click.stop="openEditDialog(row)">编辑</el-button>
              <el-button size="small" type="danger" @click.stop="handleDelete(row.id)">删除</el-button>
            </template>
            <template v-else-if="row.status === 'REVIEWED'">
              <el-button size="small" type="success" @click.stop="handleApprove(row.id)">通过</el-button>
              <el-button size="small" type="warning" @click.stop="handleReject(row.id)">驳回</el-button>
            </template>
            <template v-else>
              <span style="color:#909399;font-size:12px">已过账</span>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:14px;display:flex;justify-content:flex-end">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadVouchers"
          @size-change="handleSizeChange" />
      </div>
    </el-card>

    <!-- 凭证详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="凭证详情" width="800px">
      <div v-if="currentVoucher">
        <el-descriptions :column="3" border style="margin-bottom:16px">
          <el-descriptions-item label="凭证编号">{{ currentVoucher.voucherNo }}</el-descriptions-item>
          <el-descriptions-item label="凭证日期">{{ currentVoucher.voucherDate }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(currentVoucher.status)" size="small">{{ statusText(currentVoucher.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="往来方">{{ currentVoucher.counterparty || '-' }}</el-descriptions-item>
          <el-descriptions-item label="金额">¥{{ currentVoucher.amount?.toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ currentVoucher.description || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div style="font-weight:bold;margin-bottom:8px">会计分录</div>
        <el-table :data="currentVoucher.entries || []" border size="small">
          <el-table-column type="index" label="#" width="50" />
          <el-table-column label="摘要" prop="summary" />
          <el-table-column label="科目">
            <template #default="{row}">
              {{ row.account ? `${row.account.code} ${row.account.name}` : '-' }}
            </template>
          </el-table-column>
          <el-table-column label="借方金额" width="120" align="right">
            <template #default="{row}">
              <span v-if="row.debitAmount > 0" style="color:#1a6b68">¥{{ row.debitAmount?.toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="贷方金额" width="120" align="right">
            <template #default="{row}">
              <span v-if="row.creditAmount > 0" style="color:#e6a23c">¥{{ row.creditAmount?.toFixed(2) }}</span>
            </template>
          </el-table-column>
        </el-table>
        <div style="margin-top:8px;text-align:right;font-weight:bold">
          借方合计: ¥{{ entryTotal(currentVoucher.entries, 'debit') }} | 贷方合计: ¥{{ entryTotal(currentVoucher.entries, 'credit') }}
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑凭证对话框 -->
    <el-dialog v-model="editDialogVisible" title="编辑凭证" width="800px" destroy-on-close>
      <el-form :model="editForm" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="凭证日期" required>
              <el-date-picker v-model="editForm.voucherDate" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="类型">
              <el-select v-model="editForm.type" style="width:100%">
                <el-option label="收入" value="INCOME" />
                <el-option label="支出" value="EXPENSE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="往来方">
              <el-input v-model="editForm.counterparty" placeholder="对方单位" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div style="font-weight:bold;margin:8px 0">会计分录</div>
      <el-table :data="editEntries" border size="small">
        <el-table-column label="摘要" min-width="120">
          <template #default="{row}">
            <el-input v-model="row.summary" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="科目" width="220">
          <template #default="{row}">
            <el-select v-model="row.accountId" filterable size="small" style="width:100%">
              <el-option v-for="acc in allAccounts" :key="acc.id" :label="`${acc.code} ${acc.name}`" :value="acc.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="借方" width="120">
          <template #default="{row}">
            <el-input-number v-model="row.debitAmount" :precision="2" :min="0" size="small" style="width:100%" />
          </template>
        </el-table-column>
        <el-table-column label="贷方" width="120">
          <template #default="{row}">
            <el-input-number v-model="row.creditAmount" :precision="2" :min="0" size="small" style="width:100%" />
          </template>
        </el-table-column>
        <el-table-column label="" width="60">
          <template #default="{$index}">
            <el-button size="small" type="danger" link @click="editEntries.splice($index, 1)" :disabled="editEntries.length <= 2">删</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-button size="small" plain style="margin-top:8px" @click="editEntries.push({summary:'',accountId:null,debitAmount:0,creditAmount:0})">+ 添加分录</el-button>

      <el-form label-width="100px" style="margin-top:12px">
        <el-form-item label="备注">
          <el-input v-model="editForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getVoucherPage, updateVoucher, deleteVoucher, exportVoucherExcel, submitForReview, approveVoucher, rejectVoucher, getVoucher } from '../api/voucher'
import { getAccountList } from '../api/account'
import { ElMessage, ElMessageBox } from 'element-plus'

const vouchers = ref([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const loading = ref(false)
const filterType = ref('')
const filterStatus = ref('')
const dateRange = ref(null)
const allAccounts = ref([])

// 详情
const detailDialogVisible = ref(false)
const currentVoucher = ref(null)

// 编辑
const editDialogVisible = ref(false)
const editId = ref(null)
const editForm = ref({})
const editEntries = ref([])

const statusText = (s) => ({ DRAFT: '草稿', REVIEWED: '已审核', POSTED: '已过账' }[s] || s)
const statusTagType = (s) => ({ DRAFT: 'info', REVIEWED: 'warning', POSTED: 'success' }[s] || 'info')

function entryTotal(entries, type) {
  if (!entries) return '0.00'
  const sum = entries.reduce((acc, e) => acc + (e[type + 'Amount'] || 0), 0)
  return sum.toFixed(2)
}

async function loadVouchers() {
  loading.value = true
  try {
    const params = { page: page.value, pageSize: pageSize.value }
    if (filterType.value) params.type = filterType.value
    if (filterStatus.value) params.status = filterStatus.value
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const { data } = await getVoucherPage(params)
    vouchers.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  page.value = 1
  loadVouchers()
}

async function showDetail(row) {
  try {
    const { data } = await getVoucher(row.id)
    currentVoucher.value = data
    detailDialogVisible.value = true
  } catch (e) {
    ElMessage.error('加载详情失败')
  }
}

function openEditDialog(row) {
  editId.value = row.id
  editForm.value = { ...row }
  editEntries.value = (row.entries || []).map(e => ({ ...e }))
  if (editEntries.value.length < 2) {
    editEntries.value.push({ summary: '', accountId: null, debitAmount: 0, creditAmount: 0 })
  }
  editDialogVisible.value = true
}

async function submitEdit() {
  if (!editForm.value.voucherDate) {
    ElMessage.warning('请填写凭证日期')
    return
  }
  const validEntries = editEntries.value.filter(e => e.accountId && (e.debitAmount > 0 || e.creditAmount > 0))
  if (validEntries.length < 2) {
    ElMessage.warning('至少需要两条有效分录')
    return
  }
  try {
    const payload = {
      voucher: editForm.value,
      entries: validEntries
    }
    const { data } = await updateVoucher(editId.value, payload)
    if (data.success) {
      ElMessage.success('更新成功')
      editDialogVisible.value = false
      loadVouchers()
    }
  } catch (e) {
    ElMessage.error('操作失败: ' + (e.response?.data?.error || e.message))
  }
}

async function handleSubmit(id) {
  try {
    await ElMessageBox.confirm('确定提交审核？提交后不可编辑', '提示', { type: 'warning' })
    await submitForReview(id)
    ElMessage.success('已提交审核')
    loadVouchers()
  } catch (e) { /* cancelled or error */ }
}

async function handleApprove(id) {
  try {
    await ElMessageBox.confirm('确定审核通过？通过后将不可修改', '提示', { type: 'warning' })
    await approveVoucher(id)
    ElMessage.success('审核通过，已过账')
    loadVouchers()
  } catch (e) { /* cancelled or error */ }
}

async function handleReject(id) {
  try {
    await ElMessageBox.confirm('确定驳回？凭证将退回为草稿状态', '提示', { type: 'warning' })
    await rejectVoucher(id)
    ElMessage.success('已驳回')
    loadVouchers()
  } catch (e) { /* cancelled or error */ }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该凭证？', '提示', { type: 'warning' })
    await deleteVoucher(id)
    ElMessage.success('删除成功')
    loadVouchers()
  } catch (e) { /* cancelled */ }
}

function handleExport() {
  const params = {}
  if (filterType.value) params.type = filterType.value
  if (dateRange.value && dateRange.value.length === 2) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  }
  exportVoucherExcel(params).then(({ data }) => {
    const blob = new Blob([data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `凭证列表_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  }).catch(() => { ElMessage.error('导出失败') })
}

async function loadAccounts() {
  try {
    const { data } = await getAccountList()
    allAccounts.value = data
  } catch (e) { /* ignore */ }
}

onMounted(() => {
  loadAccounts()
  loadVouchers()
})
</script>
