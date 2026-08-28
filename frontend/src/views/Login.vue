<template>
  <div class="login-container">
    <section class="login-panel">
      <div class="login-story">
        <div class="brand-row">
          <div class="brand-mark login-mark">F</div>
          <div>
            <div class="brand-title dark">FIVA</div>
            <div class="brand-subtitle dark">企业财税系统</div>
          </div>
        </div>
        <h1>凭证、账簿与税务，集中管理。</h1>
        <p>面向财务团队的企业工作台，覆盖发票、凭证、账簿、税务测算与报表。</p>
        <div class="metric-strip">
          <div><strong>凭证</strong><span>录入与审核</span></div>
          <div><strong>税务</strong><span>测算与筹划</span></div>
          <div><strong>报表</strong><span>账簿与披露</span></div>
        </div>
      </div>

      <div class="login-card">
        <div class="login-header">
          <h2>登录工作台</h2>
          <p>使用已开通的账号继续处理财税业务</p>
        </div>
        <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleLogin">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名" size="large" prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码" size="large" prefix-icon="Lock" show-password />
          </el-form-item>
          <el-form-item prop="captchaAnswer">
            <div class="captcha-row">
              <el-input v-model="form.captchaAnswer" placeholder="请输入计算结果" size="large" prefix-icon="Key" @keyup.enter="handleLogin" />
              <button type="button" class="captcha-box" @click="refreshCaptcha" title="点击刷新验证码">
                {{ captchaQuestion || '加载中...' }}
              </button>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleLogin" :loading="loading" size="large" class="login-button">登录</el-button>
          </el-form-item>
        </el-form>
        <div class="login-footer">
          <span>还没有账号？</span>
          <router-link to="/register" class="link">立即注册</router-link>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { login, getCaptcha } from '../api/auth'
const formRef = ref(null)
const loading = ref(false)
const captchaQuestion = ref('')
const captchaId = ref('')
const form = ref({ username: '', password: '', captchaAnswer: '', captchaId: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaAnswer: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function refreshCaptcha() {
  try {
    const { data } = await getCaptcha()
    captchaId.value = data.data.captchaId
    captchaQuestion.value = data.data.question
    form.value.captchaId = data.data.captchaId
  } catch (e) {
    captchaQuestion.value = '点击重试'
  }
}

onMounted(() => { refreshCaptcha() })

async function handleLogin() {
  try { await formRef.value.validate() } catch { return }
  loading.value = true
  try {
    const { data } = await login({
      username: form.value.username,
      password: form.value.password,
      captchaId: captchaId.value,
      captchaAnswer: form.value.captchaAnswer
    })
    const loginData = data.data
    localStorage.setItem('token', loginData.token)
    localStorage.setItem('user', JSON.stringify({
      username: loginData.username,
      realName: loginData.realName,
      role: loginData.role,
      department: loginData.department
    }))
    ElMessage.success('登录成功')
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
.login-container {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px;
  background: var(--fiva-bg);
}
.login-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) 420px;
  width: min(980px, 100%);
  min-height: 560px;
  overflow: hidden;
  border: 1px solid var(--fiva-line);
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 24px 70px rgba(21, 32, 51, .12);
}
.login-story {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 40px;
  color: #fff;
  background: linear-gradient(180deg, #121a2a 0%, #172033 48%, #102c2d 100%);
}
.brand-row { display: flex; align-items: center; gap: 12px; }
.login-mark { width: 36px; height: 36px; display: grid; place-items: center; border-radius: 8px; color: #fff; font-weight: 700; background: linear-gradient(135deg, #1a6b68, #209477); box-shadow: 0 10px 22px rgba(26,107,104,.28); }
.dark.brand-title { color: #fff; font-size: 18px; font-weight: 650; letter-spacing: .04em; }
.dark.brand-subtitle { color: rgba(226, 232, 240, .58); font-size: 13px; }
.login-story h1 {
  max-width: 480px;
  margin-top: 48px;
  font-size: 32px;
  line-height: 1.28;
  font-weight: 600;
}
.login-story p {
  max-width: 460px;
  margin-top: 14px;
  color: rgba(226, 232, 240, .68);
  font-size: 14px;
  line-height: 1.8;
}
.metric-strip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: auto;
}
.metric-strip div {
  padding: 14px;
  border: 1px solid rgba(255,255,255,.10);
  border-radius: 8px;
  background: rgba(255,255,255,.06);
}
.metric-strip strong { display: block; font-size: 16px; font-weight: 600; }
.metric-strip span { display: block; margin-top: 4px; color: rgba(226, 232, 240, .58); font-size: 12px; }
.login-card { display: flex; flex-direction: column; justify-content: center; padding: 40px 36px; }
.login-header { margin-bottom: 24px; }
.login-header h2 { margin: 0 0 8px; color: var(--fiva-text); font-size: 24px; font-weight: 600; }
.login-header p { color: var(--fiva-muted); font-size: 14px; }
.captcha-row { display: grid; grid-template-columns: 1fr 116px; gap: 10px; width: 100%; }
.captcha-box {
  height: 40px;
  border: 1px solid var(--fiva-line);
  border-radius: 8px;
  color: var(--fiva-primary-strong);
  font-size: 15px;
  font-weight: 700;
  background: var(--fiva-primary-soft);
  cursor: pointer;
}
.captcha-box:hover { background: #dceceb; }
.login-button { width: 100%; height: 42px; }
.login-footer { text-align: center; margin-top: 14px; color: var(--fiva-muted); font-size: 14px; }
.login-footer .link { color: var(--fiva-primary); text-decoration: none; margin-left: 4px; font-weight: 600; }
.login-footer .link:hover { text-decoration: underline; }
@media (max-width: 900px) {
  .login-panel { grid-template-columns: 1fr; }
  .login-story { display: none; }
}
@media (max-width: 520px) {
  .login-container { padding: 14px; }
  .login-card { padding: 28px 20px; }
}
</style>
