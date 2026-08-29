<template>
  <div>
    <h2 style="margin-bottom:20px">新增凭证</h2>

    <el-card>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="手动录入" name="manual" />
        <el-tab-pane label="上传发票识别" name="upload" />
      </el-tabs>

      <!-- Tab1: 手动录入（复式记账） -->
      <div v-if="activeTab === 'manual'" style="margin-top:16px">
        <!-- 表头信息 -->
        <el-form :model="form" label-width="100px">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="凭证日期" required>
                <el-date-picker v-model="form.voucherDate" value-format="YYYY-MM-DD" style="width:100%" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="类型">
                <el-select v-model="form.type" style="width:100%">
                  <el-option label="收入" value="INCOME" />
                  <el-option label="支出" value="EXPENSE" />
                  <el-option label="转账" value="TRANSFER" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="往来方">
                <el-input v-model="form.counterparty" placeholder="对方单位" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <!-- 分录表格 -->
        <div style="margin:16px 0 8px;font-weight:bold;font-size:15px">会计分录</div>
        <el-table :data="entries" border style="width:100%">
          <el-table-column label="摘要" min-width="160">
            <template #default="{row}">
              <el-input v-model="row.summary" placeholder="摘要" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="会计科目" width="240">
            <template #default="{row}">
              <el-select v-model="row.accountId" filterable placeholder="选择科目" size="small" style="width:100%">
                <el-option-group v-for="group in accountGroups" :key="group.label" :label="group.label">
                  <el-option v-for="acc in group.accounts" :key="acc.id" :label="`${acc.code} ${acc.name}`" :value="acc.id" />
                </el-option-group>
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="借方金额" width="140">
            <template #default="{row}">
              <el-input-number v-model="row.debitAmount" :precision="2" :min="0" size="small" style="width:100%"
                @change="calcBalance" />
            </template>
          </el-table-column>
          <el-table-column label="贷方金额" width="140">
            <template #default="{row}">
              <el-input-number v-model="row.creditAmount" :precision="2" :min="0" size="small" style="width:100%"
                @change="calcBalance" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template #default="{$index}">
              <el-button size="small" type="danger" link @click="removeEntry($index)" :disabled="entries.length <= 2">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div style="margin-top:12px;display:flex;justify-content:space-between;align-items:center">
          <el-button type="primary" plain size="small" @click="addEntry">+ 添加分录</el-button>
          <div style="font-size:14px">
            <span>借方合计: <b style="color:#1a6b68">{{ formatMoney(totalDebit) }}</b></span>
            <span style="margin-left:20px">贷方合计: <b style="color:#e6a23c">{{ formatMoney(totalCredit) }}</b></span>
            <el-tag :type="totalDebit === totalCredit && totalDebit > 0 ? 'success' : 'danger'" style="margin-left:20px">
              {{ totalDebit === totalCredit && totalDebit > 0 ? '借贷平衡' : '借贷不平衡' }}
            </el-tag>
          </div>
        </div>

        <!-- 摘要 -->
        <el-form label-width="100px" style="margin-top:16px">
          <el-form-item label="备注">
            <el-input v-model="form.description" type="textarea" :rows="2" placeholder="备注说明" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="submitForm" :disabled="totalDebit !== totalCredit || totalDebit <= 0">保存凭证</el-button>
            <el-button @click="resetForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- Tab2: 上传发票识别 -->
      <div v-if="activeTab === 'upload'" style="margin-top:16px">
        <el-upload drag :auto-upload="false" :on-change="handleInvoiceFileChange"
          :limit="1" accept="image/*" :file-list="invoiceFileList">
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">将发票图片拖到此处，或<em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">支持 JPG/PNG 格式，上传后将自动OCR识别</div>
          </template>
        </el-upload>

        <div v-if="ocrLoading" style="text-align:center;margin-top:20px">
          <el-icon class="is-loading" :size="24"><Loading /></el-icon>
          <span style="margin-left:8px">正在识别...</span>
        </div>

        <div v-if="ocrResult" style="margin-top:20px">
          <el-descriptions title="识别结果" :column="2" border>
            <el-descriptions-item label="发票号码">{{ ocrResult.invoiceNo }}</el-descriptions-item>
            <el-descriptions-item label="开票日期">{{ ocrResult.invoiceDate }}</el-descriptions-item>
            <el-descriptions-item label="销售方">{{ ocrResult.sellerName }}</el-descriptions-item>
            <el-descriptions-item label="购买方">{{ ocrResult.buyerName }}</el-descriptions-item>
            <el-descriptions-item label="金额(不含税)">{{ ocrResult.amount }}</el-descriptions-item>
            <el-descriptions-item label="税额">{{ ocrResult.taxAmount }}</el-descriptions-item>
            <el-descriptions-item label="价税合计">{{ ocrResult.totalAmount }}</el-descriptions-item>
            <el-descriptions-item label="类别">{{ ocrResult.category }}</el-descriptions-item>
          </el-descriptions>

          <div style="margin-top:16px;text-align:center">
            <el-button type="primary" @click="generateVoucherFromOcr">确认生成凭证</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { UploadFilled, Loading } from '@element-plus/icons-vue'
import { createVoucher } from '../api/voucher'
import { uploadInvoice } from '../api/invoice'
import { getAccountList } from '../api/account'
import { ElMessage } from 'element-plus'

const route = useRoute()
const activeTab = ref('manual')
const form = ref(getEmptyForm())
const entries = ref([newEntry(), newEntry()])
const accounts = ref([])
const totalDebit = ref(0)
const totalCredit = ref(0)

// 上传发票识别
const invoiceFileList = ref([])
const invoiceFile = ref(null)
const ocrLoading = ref(false)
const ocrResult = ref(null)

// 科目分组
const accountGroups = computed(() => {
  const groups = {}
  const typeNames = { ASSET: '资产', LIABILITY: '负债', EQUITY: '权益', REVENUE: '收入', EXPENSE: '费用' }
  for (const acc of accounts.value) {
    const label = typeNames[acc.type] || acc.type
    if (!groups[label]) groups[label] = []
    groups[label].push(acc)
  }
  return Object.entries(groups).map(([label, accs]) => ({ label, accounts: accs }))
})

function getEmptyForm() {
  return {
    type: 'EXPENSE',
    amount: null,
    category: '',
    voucherDate: new Date().toISOString().slice(0, 10),
    counterparty: '',
    description: '',
    status: 'DRAFT',
    hasInvoice: false,
    invoiceImageUrl: ''
  }
}

function newEntry() {
  return { summary: '', accountId: null, debitAmount: 0, creditAmount: 0 }
}

function addEntry() {
  entries.value.push(newEntry())
}

function removeEntry(index) {
  if (entries.value.length > 2) {
    entries.value.splice(index, 1)
  }
}

function calcBalance() {
  totalDebit.value = entries.value.reduce((sum, e) => sum + (e.debitAmount || 0), 0)
  totalCredit.value = entries.value.reduce((sum, e) => sum + (e.creditAmount || 0), 0)
}

function formatMoney(val) {
  if (!val) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 提交复式记账凭证
async function submitForm() {
  if (!form.value.voucherDate) {
    ElMessage.warning('请填写凭证日期')
    return
  }
  // 过滤掉空分录
  const validEntries = entries.value.filter(e => e.accountId && (e.debitAmount > 0 || e.creditAmount > 0))
  if (validEntries.length < 2) {
    ElMessage.warning('至少需要两条有效分录')
    return
  }
  if (totalDebit.value !== totalCredit.value || totalDebit.value <= 0) {
    ElMessage.warning('借贷不平衡或金额为零')
    return
  }
  // 检查单行不能同时有借方和贷方
  for (let i = 0; i < validEntries.length; i++) {
    if (validEntries[i].debitAmount > 0 && validEntries[i].creditAmount > 0) {
      ElMessage.warning(`第${i + 1}行不能同时填写借方和贷方金额`)
      return
    }
  }

  try {
    const payload = {
      voucher: {
        ...form.value,
        amount: totalDebit.value
      },
      entries: validEntries
    }
    const { data } = await createVoucher(payload)
    if (data.success) {
      ElMessage.success('凭证创建成功')
      resetForm()
    }
  } catch (e) {
    ElMessage.error('创建失败: ' + (e.response?.data?.error || e.message))
  }
}

function resetForm() {
  form.value = getEmptyForm()
  entries.value = [newEntry(), newEntry()]
  totalDebit.value = 0
  totalCredit.value = 0
}

// 上传发票文件变更
async function handleInvoiceFileChange(file) {
  invoiceFile.value = file.raw
  invoiceFileList.value = [file]
  ocrLoading.value = true
  ocrResult.value = null
  try {
    const { data } = await uploadInvoice(invoiceFile.value)
    if (data.success) {
      ocrResult.value = data.data
      ElMessage.success('识别完成')
    }
  } catch (e) {
    ElMessage.error('识别失败，请重试')
  } finally {
    ocrLoading.value = false
  }
}

// 从OCR结果生成凭证
async function generateVoucherFromOcr() {
  if (!ocrResult.value) return
  try {
    const r = ocrResult.value
    const amount = parseFloat(r.totalAmount || r.amount) || 0
    // 查找默认科目
    const expenseAcc = accounts.value.find(a => a.code === '6602')
    const cashAcc = accounts.value.find(a => a.code === '1002')
    const payload = {
      voucher: {
        type: 'EXPENSE',
        amount: amount,
        category: r.category || '',
        voucherDate: r.invoiceDate || new Date().toISOString().slice(0, 10),
        counterparty: r.sellerName || '',
        description: '发票:' + (r.invoiceNo || ''),
        hasInvoice: true,
        invoiceImageUrl: r.imageUrl || '',
        status: 'DRAFT'
      },
      entries: [
        { summary: r.category || '费用', accountId: expenseAcc?.id, debitAmount: amount, creditAmount: 0 },
        { summary: r.category || '付款', accountId: cashAcc?.id, debitAmount: 0, creditAmount: amount }
      ]
    }
    const { data } = await createVoucher(payload)
    if (data.success) {
      ElMessage.success('凭证生成成功')
      invoiceFileList.value = []
      invoiceFile.value = null
      ocrResult.value = null
    }
  } catch (e) {
    ElMessage.error('生成失败: ' + (e.response?.data?.error || e.message))
  }
}

// 从模板数据填充
function loadFromTemplate(templateData) {
  if (!templateData) return
  try {
    const data = typeof templateData === 'string' ? JSON.parse(templateData) : templateData
    if (data.voucher) {
      form.value.type = data.voucher.type || 'EXPENSE'
      form.value.voucherDate = data.voucher.voucherDate || new Date().toISOString().slice(0, 10)
      form.value.description = data.voucher.description || ''
    }
    if (data.entries && Array.isArray(data.entries)) {
      entries.value = data.entries.map(e => ({
        summary: e.summary || '',
        accountId: e.accountId || null,
        debitAmount: Number(e.debitAmount) || 0,
        creditAmount: Number(e.creditAmount) || 0
      }))
      calcBalance()
    }
    if (data.templateName) {
      ElMessage.success(`已加载模板: ${data.templateName}`)
    }
  } catch (e) {
    console.error('加载模板数据失败:', e)
  }
}

// 加载科目列表
async function loadAccounts() {
  try {
    const { data } = await getAccountList()
    accounts.value = data
    // 加载完科目后检查是否有模板数据
    const templateData = route.query.templateData
    if (templateData) {
      loadFromTemplate(templateData)
    }
  } catch (e) {
    ElMessage.error('加载科目失败')
  }
}

onMounted(() => {
  loadAccounts()
})
</script>
