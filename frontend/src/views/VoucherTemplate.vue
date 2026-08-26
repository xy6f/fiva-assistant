<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2 style="margin:0">凭证模板</h2>
      <div>
        <el-button @click="handleInit" v-if="templates.length === 0">初始化默认模板</el-button>
        <el-button type="primary" @click="showCreateDialog">新增模板</el-button>
      </div>
    </div>

    <el-card>
      <el-table :data="templates" stripe v-loading="loading">
        <el-table-column prop="name" label="模板名称" min-width="150" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{row}">
            <el-tag :type="getTypeTag(row.type)" size="small">{{ getTypeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="分录数" width="80" align="center">
          <template #default="{row}">{{ getEntryCount(row.entriesJson) }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{row}">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{row}">
            <el-button size="small" type="primary" @click="handleApply(row)">使用</el-button>
            <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑模板' : '新增模板'" width="700px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="模板名称" required>
          <el-input v-model="form.name" placeholder="如：收款-银行收款" />
        </el-form-item>
        <el-form-item label="凭证类型" required>
          <el-select v-model="form.type" placeholder="选择类型">
            <el-option label="收入" value="INCOME" />
            <el-option label="支出" value="EXPENSE" />
            <el-option label="转账" value="TRANSFER" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="模板用途说明" />
        </el-form-item>
        <el-form-item label="分录" required>
          <div style="width:100%">
            <el-table :data="form.entries" border size="small" style="width:100%">
              <el-table-column label="摘要" width="150">
                <template #default="{row}">
                  <el-input v-model="row.summary" size="small" placeholder="摘要" />
                </template>
              </el-table-column>
              <el-table-column label="科目编码" width="120">
                <template #default="{row}">
                  <el-input v-model="row.accountCode" size="small" placeholder="如1002" />
                </template>
              </el-table-column>
              <el-table-column label="借方金额" width="120">
                <template #default="{row}">
                  <el-input-number v-model="row.debitAmount" size="small" :min="0" :precision="2" controls-position="right" style="width:100%" />
                </template>
              </el-table-column>
              <el-table-column label="贷方金额" width="120">
                <template #default="{row}">
                  <el-input-number v-model="row.creditAmount" size="small" :min="0" :precision="2" controls-position="right" style="width:100%" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="60">
                <template #default="scope">
                  <el-button size="small" type="danger" link @click="removeEntry(scope.$index)">删</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-button size="small" @click="addEntry" style="margin-top:8px">+ 添加分录</el-button>
            <div style="margin-top:8px;color:#909399;font-size:12px">
              借方合计: ¥{{ totalDebit }} | 贷方合计: ¥{{ totalCredit }}
              <span v-if="totalDebit !== totalCredit" style="color:#f56c6c"> (不平衡)</span>
              <span v-else style="color:#67c23a"> (平衡)</span>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :disabled="totalDebit !== totalCredit || !form.name">保存</el-button>
      </template>
    </el-dialog>

    <!-- 使用模板对话框 -->
    <el-dialog v-model="applyDialogVisible" title="使用模板" width="500px">
      <el-form :model="applyForm" label-width="100px">
        <el-form-item label="模板">
          <span>{{ currentTemplate?.name }}</span>
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="applyForm.amount" :min="0" :precision="2" style="width:200px" placeholder="留空使用模板金额" />
        </el-form-item>
        <el-form-item label="凭证日期">
          <el-date-picker v-model="applyForm.voucherDate" type="date" value-format="YYYY-MM-DD" style="width:200px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmApply">生成凭证</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTemplates, createTemplate, updateTemplate, deleteTemplate, applyTemplate, initTemplates } from '../api/voucherTemplate'

const router = useRouter()
const templates = ref([])
const loading = ref(false)

// 对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const form = ref({ name: '', type: 'INCOME', description: '', entries: [] })

// 使用模板对话框
const applyDialogVisible = ref(false)
const currentTemplate = ref(null)
const applyForm = ref({ amount: null, voucherDate: new Date().toISOString().slice(0, 10) })

// 计算借贷合计
const totalDebit = computed(() => {
  return form.value.entries.reduce((sum, e) => sum + (Number(e.debitAmount) || 0), 0)
})
const totalCredit = computed(() => {
  return form.value.entries.reduce((sum, e) => sum + (Number(e.creditAmount) || 0), 0)
})

function getTypeText(type) {
  return { INCOME: '收入', EXPENSE: '支出', TRANSFER: '转账' }[type] || type
}

function getTypeTag(type) {
  return { INCOME: 'success', EXPENSE: 'warning', TRANSFER: 'info' }[type] || ''
}

function getEntryCount(json) {
  if (!json) return 0
  try { return JSON.parse(json).length } catch { return 0 }
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleString('zh-CN')
}

// 加载列表
async function loadTemplates() {
  loading.value = true
  try {
    const { data } = await getTemplates()
    templates.value = data.data || []
  } finally {
    loading.value = false
  }
}

// 初始化默认模板
async function handleInit() {
  try {
    await initTemplates()
    ElMessage.success('初始化成功')
    loadTemplates()
  } catch (e) {
    ElMessage.error('初始化失败: ' + (e.response?.data?.error || e.message))
  }
}

// 新增
function showCreateDialog() {
  isEdit.value = false
  editId.value = null
  form.value = { name: '', type: 'INCOME', description: '', entries: [
    { summary: '', accountCode: '', debitAmount: 0, creditAmount: 0 },
    { summary: '', accountCode: '', debitAmount: 0, creditAmount: 0 }
  ]}
  dialogVisible.value = true
}

// 编辑
function showEditDialog(row) {
  isEdit.value = true
  editId.value = row.id
  let entries = []
  try { entries = JSON.parse(row.entriesJson || '[]') } catch {}
  form.value = {
    name: row.name,
    type: row.type,
    description: row.description,
    entries: entries.length > 0 ? entries : [{ summary: '', accountCode: '', debitAmount: 0, creditAmount: 0 }]
  }
  dialogVisible.value = true
}

function addEntry() {
  form.value.entries.push({ summary: '', accountCode: '', debitAmount: 0, creditAmount: 0 })
}

function removeEntry(index) {
  if (form.value.entries.length <= 2) {
    ElMessage.warning('至少需要两条分录')
    return
  }
  form.value.entries.splice(index, 1)
}

// 保存
async function handleSave() {
  if (totalDebit.value !== totalCredit.value) {
    ElMessage.warning('借贷不平衡，无法保存')
    return
  }
  const payload = {
    name: form.value.name,
    type: form.value.type,
    description: form.value.description,
    entriesJson: JSON.stringify(form.value.entries)
  }
  try {
    if (isEdit.value) {
      await updateTemplate(editId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createTemplate(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadTemplates()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.response?.data?.error || e.message))
  }
}

// 删除
async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该模板？', '提示', { type: 'warning' })
    await deleteTemplate(id)
    ElMessage.success('删除成功')
    loadTemplates()
  } catch (e) { /* cancelled */ }
}

// 使用模板
function handleApply(row) {
  currentTemplate.value = row
  applyForm.value = { amount: null, voucherDate: new Date().toISOString().slice(0, 10) }
  applyDialogVisible.value = true
}

async function confirmApply() {
  try {
    const params = {}
    if (applyForm.value.amount) params.amount = applyForm.value.amount
    if (applyForm.value.voucherDate) params.voucherDate = applyForm.value.voucherDate
    const { data } = await applyTemplate(currentTemplate.value.id, params)
    applyDialogVisible.value = false
    // 跳转到新增凭证页面，携带模板数据
    router.push({ path: '/voucher/add', query: { templateData: JSON.stringify(data.data) } })
  } catch (e) {
    ElMessage.error('应用模板失败: ' + (e.response?.data?.error || e.message))
  }
}

onMounted(() => loadTemplates())
</script>
