<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2 style="margin:0">工资管理</h2>
      <div style="display:flex;gap:10px;align-items:center">
        <el-date-picker v-model="currentMonth" type="month" value-format="YYYY-MM" placeholder="选择月份" style="width:160px" @change="loadData" />
        <el-button @click="addRow">+ 添加员工</el-button>
        <el-button type="success" @click="handleBatchSave" :disabled="dirtyRows.length === 0">保存</el-button>
        <el-button type="primary" @click="handleGenerateVoucher">生成计提凭证</el-button>
        <el-button type="warning" @click="handleGeneratePayment">生成分录凭证</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="records" stripe v-loading="loading" border show-summary :summary-method="getSummary">
        <el-table-column prop="employeeNo" label="工号" width="100">
          <template #default="{row}">
            <el-input v-model="row.employeeNo" size="small" placeholder="工号" @input="markDirty(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="employeeName" label="姓名" width="100">
          <template #default="{row}">
            <el-input v-model="row.employeeName" size="small" placeholder="姓名" @input="markDirty(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="department" label="部门" width="100">
          <template #default="{row}">
            <el-input v-model="row.department" size="small" placeholder="部门" @input="markDirty(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="position" label="职位" width="100">
          <template #default="{row}">
            <el-input v-model="row.position" size="small" placeholder="职位" @input="markDirty(row)" />
          </template>
        </el-table-column>
        <el-table-column label="基本工资" width="110">
          <template #default="{row}">
            <el-input-number v-model="row.baseSalary" size="small" :min="0" :precision="2" controls-position="right" style="width:100%" @change="markDirty(row)" />
          </template>
        </el-table-column>
        <el-table-column label="奖金" width="100">
          <template #default="{row}">
            <el-input-number v-model="row.bonus" size="small" :min="0" :precision="2" controls-position="right" style="width:100%" @change="markDirty(row)" />
          </template>
        </el-table-column>
        <el-table-column label="津贴" width="100">
          <template #default="{row}">
            <el-input-number v-model="row.allowance" size="small" :min="0" :precision="2" controls-position="right" style="width:100%" @change="markDirty(row)" />
          </template>
        </el-table-column>
        <el-table-column label="加班费" width="100">
          <template #default="{row}">
            <el-input-number v-model="row.overtime" size="small" :min="0" :precision="2" controls-position="right" style="width:100%" @change="markDirty(row)" />
          </template>
        </el-table-column>
        <el-table-column label="社保" width="100">
          <template #default="{row}">
            <el-input-number v-model="row.socialInsurance" size="small" :min="0" :precision="2" controls-position="right" style="width:100%" @change="markDirty(row)" />
          </template>
        </el-table-column>
        <el-table-column label="公积金" width="100">
          <template #default="{row}">
            <el-input-number v-model="row.housingFund" size="small" :min="0" :precision="2" controls-position="right" style="width:100%" @change="markDirty(row)" />
          </template>
        </el-table-column>
        <el-table-column label="个税" width="100">
          <template #default="{row}">
            <el-input-number v-model="row.tax" size="small" :min="0" :precision="2" controls-position="right" style="width:100%" @change="markDirty(row)" />
          </template>
        </el-table-column>
        <el-table-column label="应发" width="100" align="right">
          <template #default="{row}">
            <span style="font-weight:bold">{{ calcGross(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="实发" width="100" align="right">
          <template #default="{row}">
            <span style="font-weight:bold;color:#409eff">{{ calcNet(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="70" align="center">
          <template #default="{row}">
            <el-tag :type="row.status === 'POSTED' ? 'success' : 'info'" size="small">
              {{ row.status === 'POSTED' ? '已过' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" fixed="right">
          <template #default="{row}">
            <el-button size="small" type="danger" link @click="handleDelete(row)" v-if="row.status !== 'POSTED'">删</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSalaryByMonth, batchSave, generateVoucher, generatePayment, deleteRecord } from '../api/salary'

const records = ref([])
const loading = ref(false)
const currentMonth = ref(new Date().toISOString().slice(0, 7))
const dirtyRows = ref([])

function markDirty(row) {
  if (!dirtyRows.value.includes(row)) {
    dirtyRows.value.push(row)
  }
}

function calcGross(row) {
  return ((Number(row.baseSalary) || 0) + (Number(row.bonus) || 0) + (Number(row.allowance) || 0) + (Number(row.overtime) || 0)).toFixed(2)
}

function calcNet(row) {
  const gross = Number(calcGross(row))
  return (gross - (Number(row.socialInsurance) || 0) - (Number(row.housingFund) || 0) - (Number(row.tax) || 0) - (Number(row.otherDeduction) || 0)).toFixed(2)
}

function getSummary({ columns, data }) {
  const sums = []
  columns.forEach((col, index) => {
    if (index === 0) { sums[index] = '合计'; return }
    if (index === 1) { sums[index] = `${data.length}人`; return }
    const numFields = ['baseSalary', 'bonus', 'allowance', 'overtime', 'socialInsurance', 'housingFund', 'tax']
    const fieldMap = { 4: 'baseSalary', 5: 'bonus', 6: 'allowance', 7: 'overtime', 8: 'socialInsurance', 9: 'housingFund', 10: 'tax' }
    if (fieldMap[index]) {
      const sum = data.reduce((s, r) => s + (Number(r[fieldMap[index]]) || 0), 0)
      sums[index] = sum.toFixed(2)
    } else if (index === 11) {
      sums[index] = data.reduce((s, r) => s + Number(calcGross(r)), 0).toFixed(2)
    } else if (index === 12) {
      sums[index] = data.reduce((s, r) => s + Number(calcNet(r)), 0).toFixed(2)
    } else {
      sums[index] = ''
    }
  })
  return sums
}

async function loadData() {
  if (!currentMonth.value) return
  loading.value = true
  try {
    const { data } = await getSalaryByMonth(currentMonth.value)
    records.value = data.data || []
    dirtyRows.value = []
  } finally {
    loading.value = false
  }
}

function addRow() {
  records.value.push({
    employeeNo: '', employeeName: '', department: '', position: '',
    baseSalary: 0, bonus: 0, allowance: 0, overtime: 0,
    socialInsurance: 0, housingFund: 0, tax: 0, otherDeduction: 0,
    status: 'DRAFT'
  })
  markDirty(records.value[records.value.length - 1])
}

async function handleBatchSave() {
  const payload = dirtyRows.value.map(r => ({
    employeeNo: r.employeeNo || '',
    employeeName: r.employeeName || '',
    department: r.department || '',
    position: r.position || '',
    baseSalary: r.baseSalary || 0,
    bonus: r.bonus || 0,
    allowance: r.allowance || 0,
    overtime: r.overtime || 0,
    socialInsurance: r.socialInsurance || 0,
    housingFund: r.housingFund || 0,
    tax: r.tax || 0,
    otherDeduction: r.otherDeduction || 0,
    month: currentMonth.value
  }))
  try {
    const { data } = await batchSave(payload)
    ElMessage.success(`保存成功，共${data.count}条`)
    dirtyRows.value = []
    loadData()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.error || e.message))
  }
}

async function handleGenerateVoucher() {
  try {
    await ElMessageBox.confirm(`确定生成${currentMonth.value}月计提凭证？`, '提示')
    const { data } = await generateVoucher(currentMonth.value)
    ElMessage.success('计提凭证生成成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('生成失败: ' + (e.response?.data?.error || e.message))
  }
}

async function handleGeneratePayment() {
  try {
    await ElMessageBox.confirm(`确定生成${currentMonth.value}月发放凭证？`, '提示')
    const { data } = await generatePayment(currentMonth.value)
    ElMessage.success('发放凭证生成成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('生成失败: ' + (e.response?.data?.error || e.message))
  }
}

async function handleDelete(row) {
  if (row.id) {
    try {
      await ElMessageBox.confirm('确定删除该记录？', '提示', { type: 'warning' })
      await deleteRecord(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch (e) { /* cancelled */ }
  } else {
    records.value = records.value.filter(r => r !== row)
  }
}

onMounted(() => loadData())
</script>
