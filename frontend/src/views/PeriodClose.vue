<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2 style="margin:0">期末结转</h2>
      <el-date-picker v-model="period" type="month" value-format="YYYY-MM" placeholder="选择月份" @change="loadStatus" style="width:160px" />
    </div>

    <el-card v-loading="loading">
      <!-- 结转状态 -->
      <div v-if="status.closed" style="text-align:center;padding:40px">
        <el-icon :size="60" color="#67c23a"><CircleCheck /></el-icon>
        <div style="font-size:18px;margin-top:16px;color:#303133;font-weight:bold">该期间已完成结转</div>
        <div style="color:#909399;margin-top:8px">结转凭证数: {{ status.closeVoucherCount }}</div>
      </div>

      <div v-else>
        <!-- 收入科目 -->
        <div style="margin-bottom:20px">
          <h3 style="margin-bottom:12px;color:#1a6b68">收入科目（将转入未分配利润贷方）</h3>
          <el-table :data="status.revenueItems || []" border size="small">
            <el-table-column prop="code" label="编码" width="100" />
            <el-table-column prop="name" label="科目名称" width="200" />
            <el-table-column label="贷方发生额" width="150" align="right">
              <template #default="{row}">¥{{ fmt(row.creditAmount) }}</template>
            </el-table-column>
          </el-table>
          <div style="text-align:right;font-weight:bold;margin-top:8px;color:#1a6b68">
            收入合计: ¥{{ fmt(status.totalRevenue) }}
          </div>
        </div>

        <!-- 费用科目 -->
        <div style="margin-bottom:20px">
          <h3 style="margin-bottom:12px;color:#e6a23c">费用科目（将转入未分配利润借方）</h3>
          <el-table :data="status.expenseItems || []" border size="small">
            <el-table-column prop="code" label="编码" width="100" />
            <el-table-column prop="name" label="科目名称" width="200" />
            <el-table-column label="借方发生额" width="150" align="right">
              <template #default="{row}">¥{{ fmt(row.debitAmount) }}</template>
            </el-table-column>
          </el-table>
          <div style="text-align:right;font-weight:bold;margin-top:8px;color:#e6a23c">
            费用合计: ¥{{ fmt(status.totalExpense) }}
          </div>
        </div>

        <!-- 净利润 -->
        <div style="text-align:center;padding:20px;background:#f5f7fa;border-radius:8px;margin-bottom:20px">
          <div style="color:#909399;font-size:14px">本期净利润</div>
          <div :style="{ fontSize: '28px', fontWeight: 'bold', color: status.netProfit >= 0 ? '#67c23a' : '#f56c6c', marginTop: '8px' }">
            ¥{{ fmt(status.netProfit) }}
          </div>
        </div>

        <!-- 结转按钮 -->
        <div style="text-align:center">
          <el-button type="primary" size="large" @click="handleClose"
            :disabled="(!status.revenueItems || status.revenueItems.length === 0) && (!status.expenseItems || status.expenseItems.length === 0)"
            :loading="closing">
            一键结转损益
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'
import api from '../api/index'

const period = ref('')
const loading = ref(false)
const closing = ref(false)
const status = ref({})

function fmt(v) {
  if (!v && v !== 0) return '0.00'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function loadStatus() {
  loading.value = true
  try {
    const { data } = await api.get('/period-close/status', { params: { period: period.value } })
    status.value = data.data || {}
  } catch (e) {
    ElMessage.error('加载结转状态失败')
  } finally {
    loading.value = false
  }
}

async function handleClose() {
  try {
    await ElMessageBox.confirm(
      `确定执行期末结转？结转后将自动生成结转凭证（已过账状态），收入 ¥${fmt(status.value.totalRevenue)} 和费用 ¥${fmt(status.value.totalExpense)} 将转入未分配利润。`,
      '期末结转确认',
      { type: 'warning', confirmButtonText: '确认结转', cancelButtonText: '取消' }
    )
    closing.value = true
    const { data } = await api.post('/period-close/close', null, { params: { period: period.value } })
    if (data.success) {
      const result = data.data
      ElMessage.success(`结转成功！凭证号: ${result.voucherNo}，净利润: ¥${fmt(result.netProfit)}`)
      loadStatus()
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('结转失败: ' + (e.response?.data?.error || e.message))
    }
  } finally {
    closing.value = false
  }
}

onMounted(() => loadStatus())
</script>
