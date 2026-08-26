<template>
  <div>
    <div style="display:flex;align-items:center;margin-bottom:20px">
      <el-button @click="$router.back()" style="margin-right:12px">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h2 style="margin:0">AI 税务深度分析报告</h2>
    </div>

    <el-card v-if="!analysis && !analyzing" style="text-align:center;padding:60px 20px">
      <el-empty description="暂无分析数据，请先完成税务测算" />
      <el-button type="primary" @click="$router.push('/tax')">前往税务测算</el-button>
    </el-card>

    <el-card v-else>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>分析报告</span>
          <el-button size="small" @click="startAnalysis" :loading="analyzing">重新分析</el-button>
        </div>
      </template>

      <!-- 企业数据概览 -->
      <el-descriptions :column="3" border style="margin-bottom:20px" v-if="taxData">
        <el-descriptions-item label="年营业收入">{{ formatMoney(taxData.annualRevenue) }} 元</el-descriptions-item>
        <el-descriptions-item label="年营业成本">{{ formatMoney(taxData.annualCost) }} 元</el-descriptions-item>
        <el-descriptions-item label="纳税人类型">{{ taxData.smallScale ? '小规模' : '一般' }}</el-descriptions-item>
        <el-descriptions-item label="综合税负率">{{ taxData.taxBurdenRate }}%</el-descriptions-item>
        <el-descriptions-item label="净利润">{{ formatMoney(taxData.netProfit) }} 元</el-descriptions-item>
        <el-descriptions-item label="净利润率">{{ taxData.profitMargin }}%</el-descriptions-item>
      </el-descriptions>

      <!-- AI 分析内容 -->
      <div v-if="analysisError" style="color:#f56c6c;line-height:2;font-size:15px">{{ analysis }}</div>
      <div v-else-if="analysis" style="line-height:2;color:#333;font-size:15px">
        <div v-html="renderedAnalysis"></div>
      </div>

      <!-- 加载中 -->
      <div v-if="analyzing && !analysis" style="text-align:center;padding:40px">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <p style="margin-top:12px;color:#999">AI 正在生成深度分析报告，请稍候...</p>
      </div>

      <!-- 流式光标 -->
      <span v-if="analyzing && analysis" style="display:inline-block;width:2px;height:1em;background:#e6a23c;animation:blink 1s infinite;vertical-align:text-bottom"></span>
    </el-card>
  </div>
</template>

<style scoped>
@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}
</style>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { streamTaxAnalysis } from '../api/tax'

const taxData = ref(null)
const analysis = ref('')
const analyzing = ref(false)
const analysisError = ref(false)

function formatMoney(val) {
  if (!val) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 将 Markdown 风格的标题转为 HTML
const renderedAnalysis = computed(() => {
  if (!analysis.value) return ''
  return analysis.value
    .replace(/^## (.+)$/gm, '<h3 style="margin:20px 0 10px;color:#1f2d3d;border-bottom:2px solid #409eff;padding-bottom:6px">$1</h3>')
    .replace(/^### (.+)$/gm, '<h4 style="margin:16px 0 8px;color:#303133">$1</h4>')
    .replace(/^- (.+)$/gm, '<div style="padding-left:20px;margin:4px 0">• $1</div>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
})

function startAnalysis() {
  if (!taxData.value) {
    ElMessage.warning('请先完成税务测算')
    return
  }
  analyzing.value = true
  analysis.value = ''

  streamTaxAnalysis(taxData.value, {
    onToken(token) {
      analysis.value += token
    },
    onDone() {
      analyzing.value = false
    },
    onError(err) {
      if (!analysis.value) {
        analysis.value = '分析报告生成失败，请重试'
        analysisError.value = true
        ElMessage.error('分析报告生成失败')
      }
      analyzing.value = false
    }
  })
}

onMounted(() => {
  const data = sessionStorage.getItem('tax_analysis_data')
  if (data) {
    try {
      taxData.value = JSON.parse(data)
      startAnalysis()
    } catch (e) {
      ElMessage.error('数据解析失败')
    }
  }
})
</script>
