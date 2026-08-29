<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2 style="margin:0">客户/供应商管理</h2>
      <el-button type="primary" @click="openForm" v-if="canWrite()">新增联系人</el-button>
    </div>

    <!-- 筛选 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6">
        <el-select v-model="filterType" placeholder="类型筛选" @change="loadList" style="width:100%">
          <el-option label="全部" value="ALL" />
          <el-option label="客户" value="CUSTOMER" />
          <el-option label="供应商" value="SUPPLIER" />
        </el-select>
      </el-col>
      <el-col :span="8">
        <el-input v-model="keyword" placeholder="搜索名称/联系人" clearable @clear="loadList" @keyup.enter="loadList">
          <template #append>
            <el-button @click="loadList"><el-icon><Search /></el-icon></el-button>
          </template>
        </el-input>
      </el-col>
    </el-row>

    <!-- 列表 -->
    <el-card>
      <el-table :data="list" stripe style="width:100%" v-loading="loading">
        <el-table-column prop="name" label="名称" min-width="150" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.type === 'CUSTOMER' ? 'primary' : 'warning'" size="small">
              {{ row.type === 'CUSTOMER' ? '客户' : '供应商' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="email" label="邮箱" width="160" />
        <el-table-column prop="address" label="地址" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openForm(row)" v-if="canWrite()">编辑</el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(row)" v-if="canWrite()">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="formData.id ? '编辑联系人' : '新增联系人'" v-model="dialogVisible" width="600px">
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="公司/个人名称" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="formData.type">
            <el-radio value="CUSTOMER">客户</el-radio>
            <el-radio value="SUPPLIER">供应商</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="formData.contactPerson" placeholder="联系人姓名" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="formData.phone" placeholder="联系电话" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="formData.email" placeholder="邮箱地址" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="formData.address" placeholder="详细地址" />
        </el-form-item>
        <el-form-item label="税号">
          <el-input v-model="formData.taxNo" placeholder="纳税人识别号" />
        </el-form-item>
        <el-form-item label="开户行">
          <el-input v-model="formData.bankName" placeholder="开户银行" />
        </el-form-item>
        <el-form-item label="银行账号">
          <el-input v-model="formData.bankAccount" placeholder="银行账号" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getContactList, createContact, updateContact, deleteContact } from '../api/contact'
import { canWrite } from '../utils/auth'

const list = ref([])
const loading = ref(false)
const filterType = ref('ALL')
const keyword = ref('')

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const defaultForm = {
  name: '', type: 'CUSTOMER', contactPerson: '', phone: '', email: '',
  address: '', taxNo: '', bankName: '', bankAccount: '', remark: ''
}
const formData = ref({ ...defaultForm })

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

async function loadList() {
  loading.value = true
  try {
    const { data } = await getContactList(filterType.value, keyword.value)
    list.value = data.data
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

function openForm(row) {
  if (row) {
    formData.value = { ...row }
  } else {
    formData.value = { ...defaultForm }
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  try { await formRef.value.validate() } catch { return }
  submitting.value = true
  try {
    if (formData.value.id) {
      await updateContact(formData.value.id, formData.value)
      ElMessage.success('修改成功')
    } else {
      await createContact(formData.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadList()
  } catch { ElMessage.error('操作失败') }
  finally { submitting.value = false }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.name}」？`, '提示', { type: 'warning' })
    await deleteContact(row.id)
    ElMessage.success('删除成功')
    loadList()
  } catch {}
}

onMounted(() => loadList())
</script>
