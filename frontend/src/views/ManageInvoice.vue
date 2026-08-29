<template>
  <div>
    <h2 style="margin-bottom:20px">发票管理</h2>

    <el-card>
      <template #header>
        <span style="font-weight:bold;font-size:16px">发票列表（OCR识别结果）</span>
      </template>
      <el-table :data="invoices" stripe v-loading="loading">
        <el-table-column prop="invoiceNo" label="发票号码" width="150" />
        <el-table-column prop="invoiceDate" label="开票日期" width="120" />
        <el-table-column prop="sellerName" label="销售方" min-width="140" show-overflow-tooltip />
        <el-table-column prop="buyerName" label="购买方" min-width="140" show-overflow-tooltip />
        <el-table-column prop="totalAmount" label="价税合计" width="120">
          <template #default="{row}">¥{{ row.totalAmount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="category" label="类别" width="140" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{row}">
            <el-tag :type="row.status === 'RECOGNIZED' ? 'success' : 'warning'" size="small">
              {{ row.status === 'RECOGNIZED' ? '已识别' : row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="查验" width="90" align="center">
          <template #default="{row}">
            <el-tag v-if="row.verified" type="success" size="small">已查验</el-tag>
            <el-tag v-else type="info" size="small">未查验</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{row}">
            <el-button size="small" type="warning" @click="handleVerify(row.id)" v-if="canWrite() && !row.verified">查验</el-button>
            <el-button size="small" @click="showVerifyResult(row)" v-if="row.verified">查验结果</el-button>
            <el-button size="small" @click="showInvoiceDetail(row)">详情</el-button>
            <el-button size="small" type="primary" @click="showInvoiceImg(row)">图片</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)" v-if="canWrite()">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 发票图片预览对话框 -->
    <el-dialog v-model="invoiceDialogVisible" title="发票预览" width="700px">
      <div style="text-align:center">
        <el-image :src="currentInvoiceUrl" fit="contain" style="max-width:100%;max-height:500px"
          :preview-src-list="[currentInvoiceUrl]" />
      </div>
      <template #footer>
        <el-button @click="invoiceDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="exportPdf">导出PDF</el-button>
      </template>
    </el-dialog>

    <!-- 发票详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="发票详情" width="600px">
      <el-descriptions v-if="currentInvoice" :column="2" border>
        <el-descriptions-item label="发票号码">{{ currentInvoice.invoiceNo }}</el-descriptions-item>
        <el-descriptions-item label="开票日期">{{ currentInvoice.invoiceDate }}</el-descriptions-item>
        <el-descriptions-item label="销售方">{{ currentInvoice.sellerName }}</el-descriptions-item>
        <el-descriptions-item label="购买方">{{ currentInvoice.buyerName }}</el-descriptions-item>
        <el-descriptions-item label="金额(不含税)">{{ currentInvoice.amount }}</el-descriptions-item>
        <el-descriptions-item label="税额">{{ currentInvoice.taxAmount }}</el-descriptions-item>
        <el-descriptions-item label="价税合计">{{ currentInvoice.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="类别">{{ currentInvoice.category }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 查验结果对话框 -->
    <el-dialog v-model="verifyDialogVisible" title="查验结果" width="600px">
      <div v-if="verifyResultInvoice">
        <el-descriptions :column="2" border style="margin-bottom:15px">
          <el-descriptions-item label="发票号码">{{ verifyResultInvoice.invoiceNo }}</el-descriptions-item>
          <el-descriptions-item label="查验时间">{{ verifyResultInvoice.verifiedAt }}</el-descriptions-item>
        </el-descriptions>
        <el-card>
          <pre style="white-space:pre-wrap;font-family:'Microsoft YaHei';line-height:1.8">{{ verifyResultInvoice.verifyMessage }}</pre>
        </el-card>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getInvoices, deleteInvoice, verifyInvoice } from '../api/invoice'
import { ElMessage, ElMessageBox } from 'element-plus'
import { canWrite } from '../utils/auth'

const invoices = ref([])
const loading = ref(false)

// 发票图片预览
const invoiceDialogVisible = ref(false)
const currentInvoiceUrl = ref('')

// 发票详情
const detailDialogVisible = ref(false)
const currentInvoice = ref(null)

// 查验结果
const verifyDialogVisible = ref(false)
const verifyResultInvoice = ref(null)

// 加载发票列表
async function loadInvoices() {
  loading.value = true
  try {
    const { data } = await getInvoices()
    invoices.value = data
  } finally {
    loading.value = false
  }
}

// 查看发票图片
function showInvoiceImg(row) {
  currentInvoiceUrl.value = row.imagePath
  invoiceDialogVisible.value = true
}

// 查看发票详情
function showInvoiceDetail(row) {
  currentInvoice.value = row
  detailDialogVisible.value = true
}

// 导出PDF
function exportPdf() {
  const url = currentInvoiceUrl.value
  if (!url) return
  const win = window.open('', '_blank')
  win.document.write(`
    <html><head><title>发票预览</title>
    <style>body{margin:0;display:flex;justify-content:center;align-items:center;min-height:100vh}
    img{max-width:100%;max-height:100vh}</style></head>
    <body><img src="${url}" onload="window.print()" /></body></html>
  `)
  win.document.close()
}

// 删除发票
async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该发票？', '提示', { type: 'warning' })
    await deleteInvoice(id)
    ElMessage.success('删除成功')
    loadInvoices()
  } catch (e) { /* cancelled */ }
}

// 查验发票
async function handleVerify(id) {
  try {
    ElMessage.info('正在查验中...')
    const { data } = await verifyInvoice(id)
    ElMessage.success('查验完成')
    loadInvoices()
  } catch (e) {
    ElMessage.error('查验失败: ' + (e.response?.data?.error || e.message))
  }
}

// 查看查验结果
function showVerifyResult(row) {
  verifyResultInvoice.value = row
  verifyDialogVisible.value = true
}

onMounted(() => {
  loadInvoices()
})
</script>
