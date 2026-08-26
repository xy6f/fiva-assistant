<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2 style="margin:0">总账</h2>
      <el-date-picker v-model="period" type="month" value-format="YYYY-MM" placeholder="选择月份" @change="loadLedger" style="width:160px" />
    </div>

    <el-card v-loading="loading">
      <el-table :data="ledger.items || []" border size="small" show-summary :summary-method="getSummary">
        <el-table-column prop="accountCode" label="科目编码" width="100" />
        <el-table-column prop="accountName" label="科目名称" width="160" />
        <el-table-column prop="balanceDirection" label="方向" width="80" align="center">
          <template #default="{row}">{{ row.balanceDirection === 'DEBIT' ? '借' : '贷' }}</template>
        </el-table-column>
        <el-table-column label="期初余额" width="130" align="right">
          <template #default="{row}">¥{{ fmt(row.openingBalance) }}</template>
        </el-table-column>
        <el-table-column label="本期借方" width="130" align="right">
          <template #default="{row}">
            <span style="color:#409eff">¥{{ fmt(row.periodDebit) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="本期贷方" width="130" align="right">
          <template #default="{row}">
            <span style="color:#e6a23c">¥{{ fmt(row.periodCredit) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="期末余额" width="130" align="right">
          <template #default="{row}">
            <span :style="{fontWeight:'bold', color: row.closingBalance >= 0 ? '#303133' : '#f56c6c'}">¥{{ fmt(row.closingBalance) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && (!ledger.items || ledger.items.length === 0)" description="暂无数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getGeneralLedger } from '../api/ledger'
import { ElMessage } from 'element-plus'

const period = ref('')
const loading = ref(false)
const ledger = ref({})

function fmt(v) {
  if (!v) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function getSummary({ columns, data }) {
  const sums = []
  columns.forEach((col, index) => {
    if (index === 0) { sums[index] = '合计'; return }
    if (index === 1 || index === 2) { sums[index] = ''; return }
    const values = data.map(item => {
      const key = index === 3 ? 'openingBalance' : index === 4 ? 'periodDebit' : index === 5 ? 'periodCredit' : 'closingBalance'
      return Number(item[key]) || 0
    })
    sums[index] = '¥' + fmt(values.reduce((a, b) => a + b, 0))
  })
  return sums
}

async function loadLedger() {
  loading.value = true
  try {
    const { data } = await getGeneralLedger({ period: period.value })
    ledger.value = data.data || {}
  } catch (e) {
    ElMessage.error('加载总账失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => loadLedger())
</script>
