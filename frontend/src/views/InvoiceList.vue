<template>
  <div>
    <h2 style="margin-bottom:20px">发票管理列表</h2>
    <el-card>
      <el-table :data="invoices" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="invoiceNo" label="发票号码" width="150" />
        <el-table-column prop="invoiceDate" label="开票日期" width="120" />
        <el-table-column prop="sellerName" label="销售方" min-width="150" />
        <el-table-column prop="buyerName" label="购买方" min-width="150" />
        <el-table-column prop="totalAmount" label="价税合计" width="120">
          <template #default="{row}">{{ row.totalAmount }} 元</template>
        </el-table-column>
        <el-table-column prop="category" label="类别" width="150" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{row}">
            <el-tag :type="row.status==='RECOGNIZED'?'success':'warning'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{row}">
            <el-button size="small" @click="showDetail(row)">详情</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Detail Dialog -->
    <el-dialog v-model="dialogVisible" title="发票详情" width="600px">
      <el-descriptions v-if="current" :column="2" border>
        <el-descriptions-item label="发票号码">{{ current.invoiceNo }}</el-descriptions-item>
        <el-descriptions-item label="开票日期">{{ current.invoiceDate }}</el-descriptions-item>
        <el-descriptions-item label="销售方">{{ current.sellerName }}</el-descriptions-item>
        <el-descriptions-item label="购买方">{{ current.buyerName }}</el-descriptions-item>
        <el-descriptions-item label="金额(不含税)">{{ current.amount }}</el-descriptions-item>
        <el-descriptions-item label="税额">{{ current.taxAmount }}</el-descriptions-item>
        <el-descriptions-item label="价税合计">{{ current.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="类别">{{ current.category }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getInvoices, deleteInvoice } from '../api/invoice'
import { ElMessage, ElMessageBox } from 'element-plus'

const invoices = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const current = ref(null)

async function load() {
  loading.value = true
  try {
    const { data } = await getInvoices()
    invoices.value = data
  } finally {
    loading.value = false
  }
}

function showDetail(row) {
  current.value = row
  dialogVisible.value = true
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该发票？', '提示', { type: 'warning' })
    await deleteInvoice(id)
    ElMessage.success('删除成功')
    load()
  } catch(e) {}
}

onMounted(load)
</script>
