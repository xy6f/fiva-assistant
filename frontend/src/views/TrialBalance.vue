<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2 style="margin:0">科目余额表</h2>
      <el-date-picker v-model="period" type="month" value-format="YYYY-MM" placeholder="选择月份" @change="loadTrialBalance" style="width:160px" />
    </div>

    <el-card v-loading="loading">
      <el-table :data="data.items || []" border size="small">
        <el-table-column prop="accountCode" label="科目编码" width="100" />
        <el-table-column prop="accountName" label="科目名称" width="140" />
        <el-table-column label="期初借方" width="110" align="right">
          <template #default="{row}">
            <span v-if="row.openingDebit > 0">¥{{ fmt(row.openingDebit) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="期初贷方" width="110" align="right">
          <template #default="{row}">
            <span v-if="row.openingCredit > 0">¥{{ fmt(row.openingCredit) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="本期借方" width="110" align="right">
          <template #default="{row}">
            <span style="color:#1a6b68">¥{{ fmt(row.periodDebit) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="本期贷方" width="110" align="right">
          <template #default="{row}">
            <span style="color:#e6a23c">¥{{ fmt(row.periodCredit) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="期末借方" width="110" align="right">
          <template #default="{row}">
            <span v-if="row.closingDebit > 0" style="font-weight:bold">¥{{ fmt(row.closingDebit) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="期末贷方" width="110" align="right">
          <template #default="{row}">
            <span v-if="row.closingCredit > 0" style="font-weight:bold">¥{{ fmt(row.closingCredit) }}</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 合计行 -->
      <div v-if="data.totals" style="margin-top:12px;padding:12px;background:#f5f7fa;border-radius:4px">
        <el-row :gutter="20">
          <el-col :span="4" style="font-weight:bold;text-align:center">合计</el-col>
          <el-col :span="5" style="text-align:right">¥{{ fmt(data.totals.openingDebit) }}</el-col>
          <el-col :span="5" style="text-align:right">¥{{ fmt(data.totals.openingCredit) }}</el-col>
          <el-col :span="5" style="text-align:right;color:#1a6b68;font-weight:bold">¥{{ fmt(data.totals.periodDebit) }}</el-col>
          <el-col :span="5" style="text-align:right;color:#e6a23c;font-weight:bold">¥{{ fmt(data.totals.periodCredit) }}</el-col>
        </el-row>
        <el-row :gutter="20" style="margin-top:8px">
          <el-col :span="14"></el-col>
          <el-col :span="5" style="text-align:right;font-weight:bold">¥{{ fmt(data.totals.closingDebit) }}</el-col>
          <el-col :span="5" style="text-align:right;font-weight:bold">¥{{ fmt(data.totals.closingCredit) }}</el-col>
        </el-row>
      </div>

      <el-empty v-if="!loading && (!data.items || data.items.length === 0)" description="暂无数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getTrialBalance } from '../api/ledger'
import { ElMessage } from 'element-plus'

const period = ref('')
const loading = ref(false)
const data = ref({})

function fmt(v) {
  if (!v) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function loadTrialBalance() {
  loading.value = true
  try {
    const { data: res } = await getTrialBalance({ period: period.value })
    data.value = res.data || {}
  } catch (e) {
    ElMessage.error('加载科目余额表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => loadTrialBalance())
</script>
