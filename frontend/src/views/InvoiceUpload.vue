<template>
  <div>
    <h2 style="margin-bottom:20px">发票上传与识别</h2>
    <el-card>
      <el-upload
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        :limit="1"
        accept="image/*"
        :file-list="fileList"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将发票图片拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 JPG/PNG 格式，大小不超过 20MB</div>
        </template>
      </el-upload>
      <div style="margin-top:20px;text-align:center">
        <el-button type="primary" @click="startOcr" :loading="loading" :disabled="!currentFile">
          开始识别
        </el-button>
      </div>
    </el-card>

    <!-- OCR Result -->
    <el-card v-if="result" style="margin-top:20px">
      <template #header>
        <span>识别结果</span>
        <el-tag type="success" style="margin-left:10px">{{ result.invoiceNo || '待确认' }}</el-tag>
      </template>
      <el-form :model="result" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="发票号码"><el-input v-model="result.invoiceNo" /></el-form-item>
            <el-form-item label="开票日期"><el-input v-model="result.invoiceDate" placeholder="yyyy-MM-dd" /></el-form-item>
            <el-form-item label="销售方"><el-input v-model="result.sellerName" /></el-form-item>
            <el-form-item label="购买方"><el-input v-model="result.buyerName" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="金额(不含税)"><el-input v-model="result.amount" /></el-form-item>
            <el-form-item label="税额"><el-input v-model="result.taxAmount" /></el-form-item>
            <el-form-item label="价税合计"><el-input v-model="result.totalAmount" /></el-form-item>
            <el-form-item label="发票类别"><el-input v-model="result.category" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <el-collapse style="margin-top:10px">
        <el-collapse-item title="识别原文" name="1">
          <pre style="white-space:pre-wrap;max-height:200px;overflow:auto">{{ result.rawText }}</pre>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { uploadInvoice } from '../api/invoice'
import { ElMessage } from 'element-plus'

const fileList = ref([])
const currentFile = ref(null)
const loading = ref(false)
const result = ref(null)

function handleFileChange(file) {
  currentFile.value = file.raw
  fileList.value = [file]
}

async function startOcr() {
  if (!currentFile.value) return
  loading.value = true
  try {
    const { data } = await uploadInvoice(currentFile.value)
    if (data.success) {
      result.value = data.data
      ElMessage.success('识别完成！')
    }
  } catch(e) {
    ElMessage.error('识别失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>
