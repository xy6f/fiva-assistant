<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2 style="margin:0">会计科目</h2>
      <div>
        <el-select v-model="filterType" placeholder="按类型筛选" clearable @change="loadAccounts" style="width:140px;margin-right:10px">
          <el-option label="全部" value="" />
          <el-option label="资产" value="ASSET" />
          <el-option label="负债" value="LIABILITY" />
          <el-option label="权益" value="EQUITY" />
          <el-option label="收入" value="REVENUE" />
          <el-option label="费用" value="EXPENSE" />
        </el-select>
        <el-button type="primary" @click="openCreateDialog">新增科目</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="accounts" stripe v-loading="loading">
        <el-table-column prop="code" label="科目编码" width="120" />
        <el-table-column prop="name" label="科目名称" width="160" />
        <el-table-column prop="type" label="科目类型" width="100">
          <template #default="{row}">
            <el-tag :type="typeTagMap[row.type] || 'info'" size="small">{{ typeTextMap[row.type] || row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="balanceDirection" label="余额方向" width="100">
          <template #default="{row}">
            {{ row.balanceDirection === 'DEBIT' ? '借方' : '贷方' }}
          </template>
        </el-table-column>
        <el-table-column prop="enabled" label="状态" width="80" align="center">
          <template #default="{row}">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{row}">
            <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑科目' : '新增科目'" width="500px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="科目编码" required>
          <el-input v-model="form.code" placeholder="如：1001" />
        </el-form-item>
        <el-form-item label="科目名称" required>
          <el-input v-model="form.name" placeholder="如：库存现金" />
        </el-form-item>
        <el-form-item label="科目类型" required>
          <el-select v-model="form.type" style="width:100%">
            <el-option label="资产" value="ASSET" />
            <el-option label="负债" value="LIABILITY" />
            <el-option label="权益" value="EQUITY" />
            <el-option label="收入" value="REVENUE" />
            <el-option label="费用" value="EXPENSE" />
          </el-select>
        </el-form-item>
        <el-form-item label="余额方向">
          <el-select v-model="form.balanceDirection" style="width:100%">
            <el-option label="借方" value="DEBIT" />
            <el-option label="贷方" value="CREDIT" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAccountList, createAccount, updateAccount, deleteAccount } from '../api/account'
import { ElMessage, ElMessageBox } from 'element-plus'

const accounts = ref([])
const loading = ref(false)
const filterType = ref('')

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const form = ref(getEmptyForm())

const typeTextMap = { ASSET: '资产', LIABILITY: '负债', EQUITY: '权益', REVENUE: '收入', EXPENSE: '费用' }
const typeTagMap = { ASSET: '', LIABILITY: 'warning', EQUITY: 'success', REVENUE: 'success', EXPENSE: 'danger' }

function getEmptyForm() {
  return { code: '', name: '', type: 'ASSET', balanceDirection: 'DEBIT', enabled: true }
}

async function loadAccounts() {
  loading.value = true
  try {
    const params = {}
    if (filterType.value) params.type = filterType.value
    const { data } = await getAccountList(params)
    accounts.value = data
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  isEdit.value = false
  editId.value = null
  form.value = getEmptyForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  form.value = { ...row }
  dialogVisible.value = true
}

async function submitForm() {
  if (!form.value.code || !form.value.name || !form.value.type) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    if (isEdit.value) {
      const { data } = await updateAccount(editId.value, form.value)
      if (data.success) {
        ElMessage.success('更新成功')
        dialogVisible.value = false
        loadAccounts()
      }
    } else {
      const { data } = await createAccount(form.value)
      if (data.success) {
        ElMessage.success('创建成功')
        dialogVisible.value = false
        loadAccounts()
      }
    }
  } catch (e) {
    ElMessage.error('操作失败: ' + (e.response?.data?.error || e.message))
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该科目？', '提示', { type: 'warning' })
    await deleteAccount(id)
    ElMessage.success('删除成功')
    loadAccounts()
  } catch (e) { /* cancelled */ }
}

onMounted(() => loadAccounts())
</script>
