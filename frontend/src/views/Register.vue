<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-header">
        <h2>注册新账号</h2>
        <p>一账号对应一个独立公司</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleRegister">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名（3-20个字符）" size="large" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（至少6个字符）" size="large"
                    prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" size="large"
                    prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名（选填）" size="large" prefix-icon="Postcard" />
        </el-form-item>
        <el-form-item prop="companyName">
          <el-input v-model="form.companyName" placeholder="公司名称" size="large" prefix-icon="OfficeBuilding" />
        </el-form-item>
        <el-form-item prop="captchaAnswer">
          <div style="display:flex;gap:10px;width:100%">
            <el-input v-model="form.captchaAnswer" placeholder="请输入计算结果" size="large"
                      prefix-icon="Key" style="flex:1" />
            <div class="captcha-box" @click="refreshCaptcha" title="点击刷新验证码">
              {{ captchaQuestion || '加载中...' }}
            </div>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" :loading="loading" size="large" style="width:100%">
            注 册
          </el-button>
        </el-form-item>
      </el-form>
      <div class="register-footer">
        <span>已有账号？</span>
        <router-link to="/login" class="link">返回登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register, getCaptcha } from '../api/auth'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const captchaQuestion = ref('')
const captchaId = ref('')
const form = ref({
  username: '', password: '', confirmPassword: '',
  realName: '', companyName: '', captchaAnswer: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== form.value.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度需为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ],
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  captchaAnswer: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function refreshCaptcha() {
  try {
    const { data } = await getCaptcha()
    captchaId.value = data.data.captchaId
    captchaQuestion.value = data.data.question
  } catch (e) {
    captchaQuestion.value = '获取失败，点击重试'
  }
}

onMounted(() => {
  refreshCaptcha()
})

async function handleRegister() {
  try { await formRef.value.validate() } catch { return }

  loading.value = true
  try {
    const { data } = await register({
      username: form.value.username,
      password: form.value.password,
      realName: form.value.realName,
      companyName: form.value.companyName,
      captchaId: captchaId.value,
      captchaAnswer: form.value.captchaAnswer
    })
    const loginData = data.data

    localStorage.setItem('token', loginData.token)
    localStorage.setItem('user', JSON.stringify({
      username: loginData.username,
      realName: loginData.realName,
      role: loginData.role
    }))

    ElMessage.success('注册成功，已自动登录')
    window.location.href = '/dashboard'
  } catch (e) {
    refreshCaptcha()
    form.value.captchaAnswer = ''
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.register-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
}
.register-header {
  text-align: center;
  margin-bottom: 30px;
}
.register-header h2 {
  color: #303133;
  margin: 0 0 8px;
  font-size: 28px;
}
.register-header p {
  color: #909399;
  margin: 0;
}
.register-footer {
  text-align: center;
  margin-top: 16px;
  color: #909399;
  font-size: 14px;
}
.register-footer .link {
  color: #409eff;
  text-decoration: none;
  margin-left: 4px;
}
.register-footer .link:hover {
  text-decoration: underline;
}
.captcha-box {
  min-width: 120px;
  height: 40px;
  line-height: 40px;
  text-align: center;
  background: #f0f2f5;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  font-weight: bold;
  color: #409eff;
  user-select: none;
  border: 1px solid #dcdfe6;
}
.captcha-box:hover {
  background: #e6f0ff;
}
</style>
