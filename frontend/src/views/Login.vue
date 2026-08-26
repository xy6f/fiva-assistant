<template>
  <div class="login-container">
    <section class="login-panel">
      <div class="login-story">
        <div class="brand-row">
          <div class="brand-mark login-mark">F</div>
          <div>
            <div class="brand-title dark">FIVA Assistant</div>
            <div class="brand-subtitle dark">财税智能经营中枢</div>
          </div>
        </div>
        <h1>让凭证、税务与经营数据在一个工作台里流动。</h1>
        <p>面向财务团队的 AI 辅助系统，覆盖发票、凭证、账簿、税务测算与报表分析。</p>
        <div class="metric-strip">
          <div><strong>AI</strong><span>财税问答</span></div>
          <div><strong>16+</strong><span>业务模块</span></div>
          <div><strong>实时</strong><span>经营洞察</span></div>
        </div>
      </div>

      <div class="login-card">
        <div class="login-header">
          <span class="login-kicker">Welcome back</span>
          <h2>登录工作台</h2>
          <p>使用你的账号继续处理财税业务</p>
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, getCaptcha } from '../api/auth'

const router = useRouter()
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
  background: linear-gradient(135deg, #eef2f7 0%, #f8fafc 52%, #e8f4f3 100%);
}
.login-panel {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) 430px;
  width: min(1080px, 100%);
  min-height: 640px;
  overflow: hidden;
  border: 1px solid rgba(38, 54, 78, .10);
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 30px 80px rgba(17, 32, 50, .14);
}
.login-story {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 44px;
  color: #fff;
  background: linear-gradient(145deg, #121a2a 0%, #17333a 58%, #1e7d7a 100%);
}
.login-story::after {
  content: "";
  position: absolute;
  right: 38px;
  bottom: 38px;
  width: 260px;
  height: 260px;
  border: 1px solid rgba(255,255,255,.15);
  border-radius: 50%;
}
.brand-row { display: flex; align-items: center; gap: 12px; position: relative; z-index: 1; }
.login-mark { background: linear-gradient(135deg, #20b486, #2b7fff); }
.dark.brand-title { color: #fff; }
.dark.brand-subtitle { color: rgba(232, 241, 255, .66); }
.login-story h1 {
  position: relative;
  z-index: 1;
  max-width: 560px;
  margin-top: 70px;
  font-size: 42px;
  line-height: 1.18;
  font-weight: 820;
  letter-spacing: 0;
}
.login-story p {
  position: relative;
  z-index: 1;
  max-width: 520px;
  margin-top: 18px;
  color: rgba(232, 241, 255, .72);
  font-size: 15px;
  line-height: 1.9;
}
.metric-strip {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-top: auto;
}
.metric-strip div {
  padding: 16px;
  border: 1px solid rgba(255,255,255,.12);
  border-radius: 8px;
  background: rgba(255,255,255,.08);
}
.metric-strip strong { display: block; font-size: 22px; }
.metric-strip span { display: block; margin-top: 4px; color: rgba(232, 241, 255, .62); font-size: 12px; }
.login-card { display: flex; flex-direction: column; justify-content: center; padding: 44px 42px; }
.login-header { margin-bottom: 28px; }
.login-kicker { color: #1e7d7a; font-size: 12px; font-weight: 780; text-transform: uppercase; letter-spacing: .12em; }
.login-header h2 { margin: 8px 0 8px; color: #172033; font-size: 30px; font-weight: 820; }
.login-header p { color: #718096; font-size: 14px; }
.captcha-row { display: grid; grid-template-columns: 1fr 116px; gap: 10px; width: 100%; }
.captcha-box {
  height: 40px;
  border: 1px solid rgba(30, 125, 122, .24);
  border-radius: 6px;
  color: #14615f;
  font-size: 15px;
  font-weight: 800;
  background: #e8f4f3;
  cursor: pointer;
}
.captcha-box:hover { background: #d9eeec; }
.login-button { width: 100%; height: 42px; }
.login-footer { text-align: center; margin-top: 14px; color: #718096; font-size: 14px; }
.login-footer .link { color: #1e7d7a; text-decoration: none; margin-left: 4px; font-weight: 700; }
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
