<template>
  <el-dialog :title="isEdit ? '编辑预算' : '新建预算'" v-model="visible" width="550px" @close="resetForm">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
      <el-form-item label="部门" prop="department">
        <el-input v-model="form.department" placeholder="请输入部门名称" />
      </el-form-item>
      <el-form-item label="年度" prop="year">
        <el-input-number v-model="form.year" :min="2020" :max="2030" style="width:100%" />
      </el-form-item>
      <el-form-item label="月份" prop="month">
        <el-select v-model="form.month" placeholder="请选择月份" style="width:100%">
          <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m" />
        </el-select>
      </el-form-item>
      <el-form-item label="预算类别">
        <el-select v-model="form.category" placeholder="请选择类别" clearable style="width:100%">
          <el-option label="办公费" value="办公费" />
          <el-option label="差旅费" value="差旅费" />
          <el-option label="招待费" value="招待费" />
          <el-option label="人工费" value="人工费" />
          <el-option label="营销费" value="营销费" />
          <el-option label="其他" value="其他" />
        </el-select>
      </el-form-item>
      <el-form-item label="预算金额" prop="budgetAmount">
        <el-input-number v-model="form.budgetAmount" :min="0" :precision="2" :step="1000" style="width:100%" />
      </el-form-item>
      <el-form-item label="预警阈值">
        <el-input-number v-model="form.alertThreshold" :min="50" :max="100" style="width:100%" />
        <div style="color:#909399;font-size:12px;margin-top:4px">当使用率达到该百分比时触发预警</div>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.description" type="textarea" :rows="2" placeholder="备注说明" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createBudget, updateBudget } from '../api/budget'

const props = defineProps({
  modelValue: Boolean,
  editData: Object
})
const emit = defineEmits(['update:modelValue', 'saved'])

const visible = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const now = new Date()
const defaultForm = {
  department: '',
  year: now.getFullYear(),
  month: now.getMonth() + 1,
  category: '',
  budgetAmount: 0,
  alertThreshold: 80,
  description: ''
}
const form = ref({ ...defaultForm })
const isEdit = ref(false)

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val && props.editData) {
    isEdit.value = true
    form.value = { ...props.editData }
  } else {
    isEdit.value = false
    form.value = { ...defaultForm }
  }
})

watch(visible, (val) => emit('update:modelValue', val))

const rules = {
  department: [{ required: true, message: '请输入部门名称', trigger: 'blur' }],
  year: [{ required: true, message: '请选择年度', trigger: 'change' }],
  month: [{ required: true, message: '请选择月份', trigger: 'change' }],
  budgetAmount: [{ required: true, message: '请输入预算金额', trigger: 'blur' }]
}

function resetForm() {
  formRef.value?.resetFields()
}

async function handleSubmit() {
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateBudget(form.value.id, form.value)
      ElMessage.success('修改成功')
    } else {
      await createBudget(form.value)
      ElMessage.success('创建成功')
    }
    visible.value = false
    emit('saved')
  } catch {
    ElMessage.error(isEdit.value ? '修改失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}
</script>
