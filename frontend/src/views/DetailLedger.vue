<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2 style="margin:0">明细账</h2>
      <div>
        <el-select v-model="selectedAccount" filterable placeholder="选择科目" @change="loadLedger" style="width:240px;margin-right:10px">
          <el-option v-for="acc in accounts" :key="acc.id" :label="`${acc.code} ${acc.name}`" :value="acc.id" />
        </el-select>
        <el-date-picker v-model="period" type="month" value-format="YYYY-MM" placeholder="选择月份" @change="loadLedger" style="width:160px;margin-right:10px" />
        <el-button type="success" @click="handleExport">导出Excel</el-button>
      </div>
    </div>

    <el-card v-loading="loading">
      <div v-if="ledger.account" style="margin-bottom:16px">
        <el-descriptions :column="4" border>
          <el-descriptions-item label="科目">{{ ledger.account.code }} {{ ledger.account.name }}</el-descriptions-item>
          <el-descriptions-item label="期初余额">¥{{ fmt(ledger.openingBalance) }}</el-descriptions-item>
          <el-descriptions-item label="本期借方">¥{{ fmt(ledger.periodDebit) }}</el-descriptions-item>
          <el-descriptions-item label="本期贷方">¥{{ fmt(ledger.periodCredit) }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <el-table :data="ledger.items || []" border size="small">
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="voucherNo" label="凭证号" width="160" />
        <el-table-column prop="summary" label="摘要" min-width="160" />
        <el-table-column label="借方" width="120" align="right">
          <template #default="{row}">
            <span v-if="row.debitAmount > 0" style="color:#1a6b68">¥{{ fmt(row.debitAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="贷方" width="120" align="right">
          <template #default="{row}">
            <span v-if="row.creditAmount > 0" style="color:#e6a23c">¥{{ fmt(row.creditAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="余额" width="120" align="right">
          <template #default="{row}">
            <span :style="{color: row.balance >= 0 ? '#303133' : '#f56c6c'}">¥{{ fmt(row.balance) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="方向" width="80" align="center">
          <template #default="{row}">
            {{ row.direction === 'DEBIT' ? '借' : row.direction === 'CREDIT' ? '贷' : '平' }}
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && (!ledger.items || ledger.items.length === 0)" description="暂无数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDetailLedger, exportDetailLedgerExcel } from '../api/ledger'
import { getAccountList } from '../api/account'
import { downloadBlob } from '../utils/download'
import { ElMessage } from 'element-plus'

const accounts = ref([])
const selectedAccount = ref(null)
const period = ref('')
const loading = ref(false)
const ledger = ref({})

function fmt(v) {
  if (!v) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function loadAccounts() {
  try {
    const { data } = await getAccountList()
    accounts.value = data
    if (data.length > 0) selectedAccount.value = data[0].id
  } catch (e) { /* ignore */ }
}

async function loadLedger() {
  if (!selectedAccount.value) return
  loading.value = true
  try {
    const { data } = await getDetailLedger({ accountId: selectedAccount.value, period: period.value })
    ledger.value = data.data || {}
  } catch (e) {
    ElMessage.error('加载明细账失败')
  } finally {
    loading.value = false
  }
}

async function handleExport() {
  if (!selectedAccount.value) {
    ElMessage.warning('请先选择科目')
    return
  }
  try {
    const acc = accounts.value.find(a => a.id === selectedAccount.value)
    const { data: blob } = await exportDetailLedgerExcel({ accountId: selectedAccount.value, period: period.value })
    const name = `明细账_${acc ? acc.code + acc.name : ''}_${period.value || '全部'}.xlsx`
    downloadBlob(blob, name)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

onMounted(async () => {
  await loadAccounts()
  loadLedger()
})
</script>
