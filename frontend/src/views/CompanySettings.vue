<template>
  <div class="company-settings">
    <el-card>
      <template #header>
        <div style="display:flex;align-items:center;justify-content:space-between">
          <span style="font-size:18px;font-weight:bold">公司设置</span>
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
        </div>
      </template>
      <el-form :model="form" label-width="100px" style="max-width:600px">
        <el-form-item label="公司名称">
          <el-input v-model="form.name" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="税号">
          <el-input v-model="form.taxNo" placeholder="请输入纳税人识别号" />
        </el-form-item>
        <el-form-item label="法人代表">
          <el-input v-model="form.legalPerson" placeholder="请输入法人代表" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="公司地址">
          <el-input v-model="form.address" placeholder="请输入公司地址" />
        </el-form-item>
        <el-form-item label="开户银行">
          <el-input v-model="form.bankName" placeholder="请输入开户银行" />
        </el-form-item>
        <el-form-item label="银行账号">
          <el-input v-model="form.bankAccount" placeholder="请输入银行账号" />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCompany, updateCompany } from '../api/auth'

const saving = ref(false)
const form = ref({
  name: '', taxNo: '', legalPerson: '',
  phone: '', address: '', bankName: '', bankAccount: ''
})

async function loadCompany() {
  try {
    const { data } = await getCompany()
    const c = data.data || {}
    form.value = {
      name: c.name || '',
      taxNo: c.taxNo || '',
      legalPerson: c.legalPerson || '',
      phone: c.phone || '',
      address: c.address || '',
      bankName: c.bankName || '',
      bankAccount: c.bankAccount || ''
    }
  } catch (e) {
    // ignore
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateCompany(form.value)
    ElMessage.success('保存成功')
  } catch (e) {
    // error handled by interceptor
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadCompany()
})
</script>

<style scoped>
.company-settings {
  max-width: 800px;
}
</style>
