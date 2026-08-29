<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2 style="margin:0">固定资产管理</h2>
      <div>
        <el-button v-if="canWrite()" type="warning" @click="showDepreciateDialog">计提折旧</el-button>
        <el-button v-if="canWrite()" type="primary" @click="showCreateDialog">新增资产</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="assets" stripe v-loading="loading">
        <el-table-column prop="assetCode" label="资产编码" width="120" />
        <el-table-column prop="assetName" label="资产名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="category" label="类别" width="100" />
        <el-table-column label="原值" width="120" align="right">
          <template #default="{row}">¥{{ formatMoney(row.originalValue) }}</template>
        </el-table-column>
        <el-table-column label="累计折旧" width="120" align="right">
          <template #default="{row}">¥{{ formatMoney(row.accumulatedDepreciation) }}</template>
        </el-table-column>
        <el-table-column label="净值" width="120" align="right">
          <template #default="{row}">
            <span style="color:#1a6b68;font-weight:bold">¥{{ formatMoney(getNetValue(row)) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="月折旧额" width="110" align="right">
          <template #default="{row}">¥{{ formatMoney(row.monthlyDepreciation) }}</template>
        </el-table-column>
        <el-table-column label="折旧方法" width="100">
          <template #default="{row}">{{ getMethodText(row.depreciationMethod) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{row}">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{row}">
            <el-button v-if="canWrite()" size="small" @click="showEditDialog(row)">编辑</el-button>
            <el-button v-if="canWrite()" size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑资产' : '新增资产'" width="600px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="资产编码">
          <el-input v-model="form.assetCode" placeholder="如 FA-001" />
        </el-form-item>
        <el-form-item label="资产名称" required>
          <el-input v-model="form.assetName" placeholder="如 办公电脑" />
        </el-form-item>
        <el-form-item label="类别">
          <el-input v-model="form.category" placeholder="如 电子设备" />
        </el-form-item>
        <el-form-item label="购入日期">
          <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="原值(元)" required>
          <el-input-number v-model="form.originalValue" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="残值(元)">
          <el-input-number v-model="form.salvageValue" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="折旧方法">
          <el-select v-model="form.depreciationMethod" style="width:100%">
            <el-option label="直线法" value="STRAIGHT_LINE" />
            <el-option label="双倍余额递减法" value="DOUBLE_DECLINING" />
            <el-option label="年数总和法" value="SUM_OF_YEARS" />
          </el-select>
        </el-form-item>
        <el-form-item label="使用年限(月)" required>
          <el-input-number v-model="form.usefulLifeMonths" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="使用中" value="IN_USE" />
            <el-option label="已处置" value="DISPOSED" />
            <el-option label="已报废" value="SCRAPPED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :disabled="!form.assetName || !form.originalValue">保存</el-button>
      </template>
    </el-dialog>

    <!-- 计提折旧对话框 -->
    <el-dialog v-model="depreciateDialogVisible" title="计提折旧" width="400px">
      <el-form label-width="100px">
        <el-form-item label="折旧期间">
          <el-date-picker v-model="depreciatePeriod" type="month" value-format="YYYY-MM" placeholder="选择期间" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="depreciateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleDepreciate" :loading="depreciating" :disabled="!depreciatePeriod">确认计提</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAssets, createAsset, updateAsset, deleteAsset, depreciate } from '../api/fixedAsset'
import { canWrite } from '../utils/auth'

const assets = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const depreciateDialogVisible = ref(false)
const depreciatePeriod = ref('')
const depreciating = ref(false)

const defaultForm = () => ({
  assetCode: '', assetName: '', category: '', purchaseDate: '',
  originalValue: 0, salvageValue: 0, depreciationMethod: 'STRAIGHT_LINE',
  usefulLifeMonths: 60, status: 'IN_USE'
})
const form = ref(defaultForm())

function formatMoney(v) {
  return Number(v || 0).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

function getNetValue(row) {
  return (row.originalValue || 0) - (row.accumulatedDepreciation || 0)
}

function getMethodText(m) {
  return { STRAIGHT_LINE: '直线法', DOUBLE_DECLINING: '双倍余额递减', SUM_OF_YEARS: '年数总和' }[m] || m
}

function getStatusText(s) {
  return { IN_USE: '使用中', DISPOSED: '已处置', SCRAPPED: '已报废' }[s] || s
}

function getStatusType(s) {
  return { IN_USE: 'success', DISPOSED: 'info', SCRAPPED: 'danger' }[s] || ''
}

async function loadData() {
  loading.value = true
  try {
    const { data } = await getAssets()
    assets.value = data.data || []
  } finally {
    loading.value = false
  }
}

function showCreateDialog() {
  isEdit.value = false
  editId.value = null
  form.value = defaultForm()
  dialogVisible.value = true
}

function showEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  form.value = { ...row }
  dialogVisible.value = true
}

async function handleSave() {
  try {
    if (isEdit.value) {
      await updateAsset(editId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await createAsset(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.error || e.message))
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该资产？', '提示', { type: 'warning' })
    await deleteAsset(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) { /* cancelled */ }
}

function showDepreciateDialog() {
  depreciatePeriod.value = ''
  depreciateDialogVisible.value = true
}

async function handleDepreciate() {
  depreciating.value = true
  try {
    const { data } = await depreciate(depreciatePeriod.value)
    ElMessage.success('折旧计提成功')
    depreciateDialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error('计提失败: ' + (e.response?.data?.error || e.message))
  } finally {
    depreciating.value = false
  }
}

onMounted(() => loadData())
</script>
