<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2 style="margin:0">银行流水导入</h2>
    </div>

    <!-- 上传区域 -->
    <el-card style="margin-bottom:20px">
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls,.csv"
        :on-change="handleFileChange"
        :on-exceed="() => ElMessage.warning('只能上传一个文件')"
      >
        <el-icon style="font-size:40px;color:#909399"><UploadFilled /></el-icon>
        <div>将Excel/CSV文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div style="color:#909399;font-size:12px">支持 .xlsx / .xls / .csv 格式</div>
        </template>
      </el-upload>
      <el-button type="primary" @click="handleImport" :loading="importing" style="margin-top:10px" :disabled="!selectedFile">
        导入流水
      </el-button>
    </el-card>

    <!-- 筛选和操作 -->
    <el-card>
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:15px">
        <div style="display:flex;gap:10px;align-items:center">
          <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width:150px" @change="loadData">
            <el-option label="全部" value="" />
            <el-option label="未匹配" value="UNMATCHED" />
            <el-option label="已匹配" value="MATCHED" />
            <el-option label="已忽略" value="IGNORED" />
          </el-select>
          <el-button type="primary" @click="handleBatchMatch" :disabled="selectedRows.length === 0">
            批量匹配 ({{ selectedRows.length }})
          </el-button>
        </div>
        <el-button @click="loadData">刷新</el-button>
      </div>

      <el-table :data="transactions" stripe v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="45" :selectable="(row) => row.status === 'UNMATCHED'" />
        <el-table-column prop="transactionDate" label="交易日期" width="120" />
        <el-table-column prop="counterparty" label="对方" min-width="150" show-overflow-tooltip />
        <el-table-column prop="summary" label="摘要" min-width="150" show-overflow-tooltip />
        <el-table-column label="收入" width="120" align="right">
          <template #default="{row}">
            <span v-if="row.debitAmount > 0" style="color:#67c23a">¥{{ formatMoney(row.debitAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="支出" width="120" align="right">
          <template #default="{row}">
            <span v-if="row.creditAmount > 0" style="color:#f56c6c">¥{{ formatMoney(row.creditAmount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="余额" width="120" align="right">
          <template #default="{row}">
            <span v-if="row.balance != null">¥{{ formatMoney(row.balance) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{row}">
            <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{row}">
            <template v-if="row.status === 'UNMATCHED'">
              <el-button size="small" type="primary" @click="handleMatch(row.id)">匹配</el-button>
              <el-button size="small" @click="handleIgnore(row.id)">忽略</el-button>
            </template>
            <el-button size="small" type="danger" @click="handleDelete(row.id)" v-if="row.status !== 'MATCHED'">删除</el-button>
            <el-tag v-if="row.status === 'MATCHED'" size="small" type="info">凭证#{{ row.voucherId }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { importFile, getTransactions, matchTransaction, batchMatch, ignoreTransaction, deleteTransaction } from '../api/bankTransaction'

const transactions = ref([])
const loading = ref(false)
const importing = ref(false)
const statusFilter = ref('')
const selectedFile = ref(null)
const selectedRows = ref([])
const uploadRef = ref(null)

function formatMoney(v) {
  return Number(v).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

function getStatusText(s) {
  return { UNMATCHED: '未匹配', MATCHED: '已匹配', IGNORED: '已忽略' }[s] || s
}

function getStatusType(s) {
  return { UNMATCHED: 'warning', MATCHED: 'success', IGNORED: 'info' }[s] || ''
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
}

function handleFileChange(file) {
  selectedFile.value = file.raw
}

async function handleImport() {
  if (!selectedFile.value) return
  importing.value = true
  try {
    const { data } = await importFile(selectedFile.value)
    ElMessage.success(`导入成功，共${data.count}条流水`)
    selectedFile.value = null
    if (uploadRef.value) uploadRef.value.clearFiles()
    loadData()
  } catch (e) {
    ElMessage.error('导入失败: ' + (e.response?.data?.error || e.message))
  } finally {
    importing.value = false
  }
}

async function loadData() {
  loading.value = true
  try {
    const { data } = await getTransactions(statusFilter.value || undefined)
    transactions.value = data.data || []
  } finally {
    loading.value = false
  }
}

async function handleMatch(id) {
  try {
    const { data } = await matchTransaction(id)
    ElMessage.success('匹配成功，已生成凭证')
    loadData()
  } catch (e) {
    ElMessage.error('匹配失败: ' + (e.response?.data?.error || e.message))
  }
}

async function handleBatchMatch() {
  const ids = selectedRows.value.map(r => r.id)
  try {
    const { data } = await batchMatch(ids)
    ElMessage.success(`批量匹配完成，成功生成${data.count}张凭证`)
    loadData()
  } catch (e) {
    ElMessage.error('批量匹配失败: ' + (e.response?.data?.error || e.message))
  }
}

async function handleIgnore(id) {
  try {
    await ignoreTransaction(id)
    ElMessage.success('已忽略')
    loadData()
  } catch (e) {
    ElMessage.error(e.response?.data?.error || e.message)
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该流水？', '提示', { type: 'warning' })
    await deleteTransaction(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) { /* cancelled */ }
}

onMounted(() => loadData())
</script>
