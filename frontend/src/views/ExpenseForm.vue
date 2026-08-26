<template>
  <el-dialog :title="isEdit ? '编辑报销单' : '新建报销单'" v-model="visible" width="550px" @close="resetForm">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
      <el-form-item label="报销标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入报销标题" />
      </el-form-item>
      <el-form-item label="报销人" prop="applicant">
        <el-input v-model="form.applicant" placeholder="请输入报销人姓名" />
      </el-form-item>
      <el-form-item label="费用类别" prop="category">
        <el-select v-model="form.category" placeholder="请选择类别" style="width:100%">
          <el-option label="差旅费" value="差旅费" />
          <el-option label="办公费" value="办公费" />
          <el-option label="招待费" value="招待费" />
          <el-option label="交通费" value="交通费" />
          <el-option label="通讯费" value="通讯费" />
          <el-option label="培训费" value="培训费" />
          <el-option label="其他" value="其他" />
        </el-select>
      </el-form-item>
      <el-form-item label="报销金额" prop="amount">
        <el-input-number v-model="form.amount" :min="0" :precision="2" :step="100" style="width:100%" />
      </el-form-item>
      <el-form-item label="费用日期" prop="expenseDate">
        <el-date-picker v-model="form.expenseDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width:100%" />
      </el-form-item>
      <el-form-item label="备注说明">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入备注说明" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="submitting">提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createExpense, updateExpense } from '../api/expense'

const props = defineProps({
  modelValue: Boolean,
  editData: Object
})
const emit = defineEmits(['update:modelValue', 'saved'])

const visible = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const defaultForm = {
  title: '',
  applicant: '',
  category: '差旅费',
  amount: 0,
  expenseDate: '',
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

watch(visible, (val) => {
  emit('update:modelValue', val)
})

const rules = {
  title: [{ required: true, message: '请输入报销标题', trigger: 'blur' }],
  applicant: [{ required: true, message: '请输入报销人', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入报销金额', trigger: 'blur' }],
  expenseDate: [{ required: true, message: '请选择费用日期', trigger: 'change' }]
}

function resetForm() {
  formRef.value?.resetFields()
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch { return }

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateExpense(form.value.id, form.value)
      ElMessage.success('修改成功')
    } else {
      await createExpense(form.value)
      ElMessage.success('创建成功')
    }
    visible.value = false
    emit('saved')
  } catch (e) {
    ElMessage.error(isEdit.value ? '修改失败' : '创建失败')
  } finally {
    submitting.value = false
  }
}
</script>
