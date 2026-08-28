<template>
  <div class="register-container">
    <div class="register-card">
      <div class="register-brand">
        <div class="brand-mark">F</div>
        <div>
          <div class="brand-name">FIVA</div>
          <div class="brand-sub">企业财税系统</div>
        </div>
      </div>
      <div class="register-header">
        <h2>注册账号</h2>
        <p>一个账号对应一家独立核算主体</p>
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
            注册
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
import { ElMessage } from 'element-plus'
import { register, getCaptcha } from '../api/auth'

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
  padding: 32px;
  background: var(--fiva-bg);
}
.register-card {
  width: 420px;
  padding: 36px 32px;
  background: #fff;
  border: 1px solid var(--fiva-line);
  border-radius: 12px;
  box-shadow: 0 24px 70px rgba(21, 32, 51, .12);
}
.register-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 24px;
}
.brand-mark {
  display: grid;
  width: 32px;
  height: 32px;
  place-items: center;
  border-radius: 8px;
  color: #fff;
  font-weight: 700;
  background: linear-gradient(135deg, #1a6b68, #209477);
  box-shadow: 0 10px 22px rgba(26,107,104,.28);
}
.brand-name { color: var(--fiva-text); font-size: 16px; font-weight: 650; letter-spacing: .04em; }
.brand-sub { color: var(--fiva-muted); font-size: 12px; }
.register-header {
  margin-bottom: 24px;
}
.register-header h2 {
  color: var(--fiva-text);
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
}
.register-header p {
  color: var(--fiva-muted);
  margin: 0;
  font-size: 14px;
}
.register-footer {
  text-align: center;
  margin-top: 16px;
  color: var(--fiva-muted);
  font-size: 14px;
}
.register-footer .link {
  color: var(--fiva-primary);
  text-decoration: none;
  margin-left: 4px;
  font-weight: 600;
}
.register-footer .link:hover {
  text-decoration: underline;
}
.captcha-box {
  min-width: 120px;
  height: 40px;
  line-height: 40px;
  text-align: center;
  background: var(--fiva-primary-soft);
  border-radius: 8px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 700;
  color: var(--fiva-primary-strong);
  user-select: none;
  border: 1px solid var(--fiva-line);
}
.captcha-box:hover {
  background: #dceceb;
}
@media (max-width: 520px) {
  .register-container { padding: 14px; }
  .register-card { width: 100%; padding: 28px 20px; }
}
</style>
