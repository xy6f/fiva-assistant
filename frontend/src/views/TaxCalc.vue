<template>
  <div>
    <h2 style="margin-bottom:20px">税务测算</h2>

    <el-row :gutter="20">
      <!-- Input Form -->
      <el-col :span="10">
        <el-card>
          <template #header>企业基本信息</template>
          <el-form :model="form" label-width="100px">
            <el-form-item label="年营业收入">
              <el-input-number v-model="form.annualRevenue" :min="0" :step="10000" :precision="2" style="width:100%" />
            </el-form-item>
            <el-form-item label="年营业成本">
              <el-input-number v-model="form.annualCost" :min="0" :step="10000" :precision="2" style="width:100%" />
            </el-form-item>
            <el-form-item label="年期间费用">
              <el-input-number v-model="form.annualExpenses" :min="0" :step="1000" :precision="2" style="width:100%" />
            </el-form-item>
            <el-form-item label="员工人数">
              <el-input-number v-model="form.employeeCount" :min="0" style="width:100%" />
            </el-form-item>
            <el-form-item label="纳税人类型">
              <el-radio-group v-model="form.smallScale">
                <el-radio :value="false">一般纳税人</el-radio>
                <el-radio :value="true">小规模纳税人</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleCalc" :loading="calculating" style="width:100%">
                开始测算
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- Result -->
      <el-col :span="14">
        <el-card v-if="result" style="margin-bottom:20px">
          <template #header>测算结果</template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="适用增值税率">{{ (result.vatRate * 100).toFixed(0) }}%</el-descriptions-item>
            <el-descriptions-item label="应纳增值税">{{ formatMoney(result.vatPayable) }}</el-descriptions-item>
            <el-descriptions-item label="城建税">{{ formatMoney(result.urbanMaintenanceTax) }}</el-descriptions-item>
            <el-descriptions-item label="教育费附加">{{ formatMoney(result.educationSurcharge) }}</el-descriptions-item>
            <el-descriptions-item label="地方教育附加">{{ formatMoney(result.localEducationSurcharge) }}</el-descriptions-item>
            <el-descriptions-item label="应纳税所得额">{{ formatMoney(result.taxableIncome) }}</el-descriptions-item>
            <el-descriptions-item label="所得税税率">{{ (result.incomeTaxRate * 100).toFixed(0) }}%</el-descriptions-item>
            <el-descriptions-item label="应纳企业所得税">{{ formatMoney(result.incomeTaxPayable) }}</el-descriptions-item>
            <el-descriptions-item label="优惠减免金额">
              <span style="color:#67c23a">-{{ formatMoney(result.incomeTaxDiscount) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="税费合计">
              <span style="color:#f56c6c;font-weight:bold">{{ formatMoney(result.totalTaxBurden) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="综合税负率">{{ result.taxBurdenRate }}%</el-descriptions-item>
            <el-descriptions-item label="净利润">
              <span :style="{ color: result.netProfit >= 0 ? '#67c23a' : '#f56c6c', fontWeight: 'bold' }">
                {{ formatMoney(result.netProfit) }}
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="净利润率">{{ result.profitMargin }}%</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- AI Suggestion -->
        <el-card v-if="suggestion || suggestLoading">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>税务筹划建议</span>
              <div>
                <el-button size="small" @click="getSuggestion" :loading="suggestLoading">刷新建议</el-button>
                <el-button size="small" type="warning" @click="goAnalysis" :loading="suggestLoading" style="margin-left:8px">深度分析</el-button>
              </div>
            </div>
          </template>
          <div v-if="suggestionError" style="color:#f56c6c;line-height:1.8">{{ suggestion }}</div>
          <div v-else style="line-height:2;color:#333" v-html="renderedSuggestion"></div>
          <span v-if="suggestLoading" style="display:inline-block;width:2px;height:1em;background:#67c23a;animation:blink 1s infinite;margin-left:2px;vertical-align:text-bottom"></span>
        </el-card>

        <el-card v-if="!result && !suggestLoading">
          <el-empty description="请输入企业信息后点击「开始测算」" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}
</style>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { calculateTax, streamTaxSuggestion } from '../api/tax'
import { renderMarkdown } from '../utils/markdown'

const router = useRouter()

const form = ref({
  annualRevenue: 1000000,
  annualCost: 600000,
  annualExpenses: 200000,
  employeeCount: 10,
  smallScale: false
})

const result = ref(null)
const suggestion = ref('')
const calculating = ref(false)
const suggestLoading = ref(false)
const suggestionError = ref(false)

const renderedSuggestion = computed(() => renderMarkdown(suggestion.value))

function formatMoney(val) {
  if (!val) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function handleCalc() {
  calculating.value = true
  try {
    const { data } = await calculateTax(form.value)
    result.value = data.data
    ElMessage.success('测算完成')
    getSuggestion()
  } catch (e) {
    ElMessage.error('测算失败')
  } finally {
    calculating.value = false
  }
}

function getSuggestion() {
  if (!result.value) return
  suggestLoading.value = true
  suggestion.value = ''

  streamTaxSuggestion(result.value, {
    onToken(token) {
      suggestion.value += token
    },
    onDone() {
      suggestLoading.value = false
    },
    onError(err) {
      if (!suggestion.value) {
        suggestion.value = 'AI建议获取失败，请重试'
        suggestionError.value = true
        ElMessage.error('AI建议获取失败')
      }
      suggestLoading.value = false
    }
  })
}

function goAnalysis() {
  if (!result.value) return
  // 保存测算结果到 sessionStorage，供分析页面使用
  sessionStorage.setItem('tax_analysis_data', JSON.stringify(result.value))
  router.push('/tax/analysis')
}
</script>
