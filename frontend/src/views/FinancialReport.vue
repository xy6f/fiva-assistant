<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2 style="margin:0">财务报表</h2>
      <el-date-picker v-model="period" type="month" value-format="YYYY-MM" placeholder="选择月份" @change="loadCurrent" style="width:160px" />
    </div>

    <el-card>
      <el-tabs v-model="activeTab" @tab-change="loadCurrent">
        <el-tab-pane label="资产负债表" name="balance" />
        <el-tab-pane label="利润表" name="income" />
        <el-tab-pane label="现金流量表" name="cashflow" />
      </el-tabs>

      <div v-loading="loading">
        <!-- 资产负债表 -->
        <div v-if="activeTab === 'balance'">
          <el-alert v-if="balanceData.balanceCheck && !balanceData.balanceCheck.passed"
            type="error" :closable="false" show-icon style="margin-bottom:12px"
            :title="`勾稽关系校验失败：资产 ≠ 负债+权益，差额 ¥${fmt(balanceData.balanceCheck.diff)}，请检查凭证借贷平衡`" />
          <el-alert v-else-if="balanceData.balanceCheck && balanceData.balanceCheck.passed && (balanceData.assets||[]).length"
            type="success" :closable="false" show-icon style="margin-bottom:12px"
            title="勾稽关系校验通过：资产 = 负债 + 所有者权益" />
          <el-row :gutter="20">
            <el-col :span="12">
              <h3 style="margin-bottom:12px;color:#409eff">资产</h3>
              <el-table :data="balanceData.assets || []" border size="small">
                <el-table-column prop="code" label="编码" width="80" />
                <el-table-column prop="name" label="项目" />
                <el-table-column label="金额" width="130" align="right">
                  <template #default="{row}">¥{{ fmt(row.amount) }}</template>
                </el-table-column>
              </el-table>
              <div style="text-align:right;font-weight:bold;margin-top:8px;padding:8px;background:#ecf5ff;border-radius:4px">
                资产合计: ¥{{ fmt(balanceData.totalAssets) }}
              </div>
            </el-col>
            <el-col :span="12">
              <h3 style="margin-bottom:12px;color:#e6a23c">负债</h3>
              <el-table :data="balanceData.liabilities || []" border size="small">
                <el-table-column prop="code" label="编码" width="80" />
                <el-table-column prop="name" label="项目" />
                <el-table-column label="金额" width="130" align="right">
                  <template #default="{row}">¥{{ fmt(row.amount) }}</template>
                </el-table-column>
              </el-table>
              <div style="text-align:right;font-weight:bold;margin-top:8px;padding:8px;background:#fdf6ec;border-radius:4px">
                负债合计: ¥{{ fmt(balanceData.totalLiabilities) }}
              </div>

              <h3 style="margin:16px 0 12px;color:#67c23a">所有者权益</h3>
              <el-table :data="balanceData.equities || []" border size="small">
                <el-table-column prop="code" label="编码" width="80" />
                <el-table-column prop="name" label="项目" />
                <el-table-column label="金额" width="130" align="right">
                  <template #default="{row}">¥{{ fmt(row.amount) }}</template>
                </el-table-column>
              </el-table>
              <div style="text-align:right;font-weight:bold;margin-top:8px;padding:8px;background:#f0f9eb;border-radius:4px">
                权益合计: ¥{{ fmt(balanceData.totalEquity) }}
              </div>
              <div style="text-align:right;font-weight:bold;margin-top:8px;padding:8px;background:#f5f7fa;border-radius:4px">
                负债+权益合计: ¥{{ fmt(balanceData.totalLiabilitiesAndEquity) }}
              </div>
            </el-col>
          </el-row>
          <el-empty v-if="!balanceData.assets || balanceData.assets.length === 0" description="暂无数据" />
        </div>

        <!-- 利润表 -->
        <div v-if="activeTab === 'income'">
          <el-table :data="buildIncomeRows()" border size="small" :show-summary="true" :summary-method="incomeSummary">
            <el-table-column prop="code" label="编码" width="100" />
            <el-table-column prop="name" label="项目" width="200" />
            <el-table-column label="金额" width="150" align="right">
              <template #default="{row}">
                <span :style="{color: row.amount < 0 ? '#f56c6c' : '#303133', fontWeight: row.bold ? 'bold' : 'normal'}">
                  ¥{{ fmt(row.amount) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!incomeData.revenues || (incomeData.revenues.length === 0 && incomeData.expenses.length === 0)" description="暂无数据" />
        </div>

        <!-- 现金流量表 -->
        <div v-if="activeTab === 'cashflow'">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="一、经营活动产生的现金流量">
              <div style="margin-top:4px">
                <div>现金流入: ¥{{ fmt(cashflowData.operating?.inflow) }}</div>
                <div>现金流出: ¥{{ fmt(cashflowData.operating?.outflow) }}</div>
                <div style="font-weight:bold;color:#409eff">经营活动净现金流: ¥{{ fmt(cashflowData.operating?.netCashFlow) }}</div>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="二、投资活动产生的现金流量">
              <div style="margin-top:4px">
                <div>现金流入: ¥{{ fmt(cashflowData.investing?.inflow) }}</div>
                <div>现金流出: ¥{{ fmt(cashflowData.investing?.outflow) }}</div>
                <div style="font-weight:bold;color:#e6a23c">投资活动净现金流: ¥{{ fmt(cashflowData.investing?.netCashFlow) }}</div>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="三、筹资活动产生的现金流量">
              <div style="margin-top:4px">
                <div>现金流入: ¥{{ fmt(cashflowData.financing?.inflow) }}</div>
                <div>现金流出: ¥{{ fmt(cashflowData.financing?.outflow) }}</div>
                <div style="font-weight:bold;color:#67c23a">筹资活动净现金流: ¥{{ fmt(cashflowData.financing?.netCashFlow) }}</div>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="四、现金及现金等价物净增加额">
              <span style="font-size:18px;font-weight:bold" :style="{color: cashflowData.totalNetCashFlow >= 0 ? '#409eff' : '#f56c6c'}">
                ¥{{ fmt(cashflowData.totalNetCashFlow) }}
              </span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getBalanceSheet, getIncomeStatement, getCashFlowStatement } from '../api/financialStatement'
import { ElMessage } from 'element-plus'

const activeTab = ref('balance')
const period = ref('')
const loading = ref(false)

const balanceData = ref({})
const incomeData = ref({})
const cashflowData = ref({})

function fmt(v) {
  if (!v && v !== 0) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function loadCurrent() {
  loading.value = true
  try {
    if (activeTab.value === 'balance') {
      const { data } = await getBalanceSheet({ period: period.value })
      balanceData.value = data.data || {}
    } else if (activeTab.value === 'income') {
      const { data } = await getIncomeStatement({ period: period.value })
      incomeData.value = data.data || {}
    } else {
      const { data } = await getCashFlowStatement({ period: period.value })
      cashflowData.value = data.data || {}
    }
  } catch (e) {
    ElMessage.error('加载报表失败')
  } finally {
    loading.value = false
  }
}

function buildIncomeRows() {
  const rows = []
  for (const r of (incomeData.value.revenues || [])) {
    rows.push({ code: r.code, name: r.name, amount: r.amount, bold: false })
  }
  rows.push({ code: '', name: '营业收入合计', amount: incomeData.value.totalRevenue || 0, bold: true })
  for (const e of (incomeData.value.expenses || [])) {
    rows.push({ code: e.code, name: e.name, amount: -(e.amount || 0), bold: false })
  }
  rows.push({ code: '', name: '营业费用合计', amount: -(incomeData.value.totalExpenses || 0), bold: true })
  rows.push({ code: '', name: '净利润', amount: incomeData.value.netProfit || 0, bold: true })
  return rows
}

function incomeSummary({ columns, data }) {
  const sums = []
  columns.forEach((col, index) => {
    if (index === 0) { sums[index] = ''; return }
    if (index === 1) { sums[index] = ''; return }
    // 不显示合计行，因为已经手动添加了
    sums[index] = ''
  })
  return sums
}

onMounted(() => loadCurrent())
</script>
