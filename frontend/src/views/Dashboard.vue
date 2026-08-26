<template>
  <div class="dashboard-page">
    <section class="dashboard-hero">
      <div>
        <span class="hero-kicker">经营驾驶舱</span>
        <h2>本期经营概览</h2>
        <p>核心财务指标、发票结构和利润趋势集中呈现，优先暴露值得关注的经营信号。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" @click="$emit('open-ai', aiPrompt)"><el-icon><ChatDotRound /></el-icon>AI 解读本月数据</el-button>
        <el-button plain @click="renderCharts"><el-icon><RefreshRight /></el-icon>刷新</el-button>
      </div>
    </section>

    <section class="overview-grid">
      <article class="profit-panel">
        <span>年度净利润</span>
        <strong :class="summary.netProfit >= 0 ? 'good' : 'bad'">¥{{ formatMoney(summary.netProfit) }}</strong>
        <div class="profit-subline">
          <small>本月利润 ¥{{ formatMoney(summary.monthlyProfit) }}</small>
          <small>利润率 {{ profitMargin }}%</small>
        </div>
      </article>
      <article class="metric-card">
        <span>本月收入</span>
        <strong>¥{{ formatMoney(summary.monthlyIncome) }}</strong>
        <small>支出 ¥{{ formatMoney(summary.monthlyExpense) }}</small>
      </article>
      <article class="metric-card">
        <span>本月开票额</span>
        <strong>¥{{ formatMoney(summary.monthlyAmount) }}</strong>
        <small>{{ summary.monthlyInvoiceCount || 0 }} 张发票</small>
      </article>
      <article class="metric-card">
        <span>税额占比</span>
        <strong>{{ taxRatio }}%</strong>
        <small>税额 ¥{{ formatMoney(summary.monthlyTaxAmount) }}</small>
      </article>
    </section>

    <section class="invoice-strip">
      <div v-for="item in invoiceCards" :key="item.label" class="invoice-chip">
        <span>{{ item.label }}</span>
        <strong :style="{ color: item.color }">{{ item.value }}</strong>
      </div>
    </section>

    <section class="analysis-grid">
      <el-card shadow="never" class="chart-card trend-card">
        <template #header>
          <div class="card-title"><span>收支利润走势</span><small>含指标均衡度</small></div>
        </template>
        <div class="trend-combo">
          <div ref="trendChartRef" class="chart-box trend"></div>
          <div class="radar-side">
            <div class="side-title">指标均衡度</div>
            <div ref="radarChartRef" class="chart-box compact"></div>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="chart-card structure-card">
        <template #header>
          <div class="card-title"><span>发票结构</span><small>分类占比</small></div>
        </template>
        <div ref="pieChartRef" class="chart-box"></div>
      </el-card>

      <el-card shadow="never" class="chart-card rank-card">
        <template #header>
          <div class="card-title"><span>分类排行</span><small>TOP 5</small></div>
        </template>
        <div class="rank-list">
          <div v-for="item in categoryRank" :key="item.category" class="rank-row">
            <div><strong>{{ item.category || '未分类' }}</strong><span>{{ item.count }} 张</span></div>
            <el-progress :percentage="item.percent" :show-text="false" />
          </div>
        </div>
      </el-card>
    </section>

    <section class="brief-grid">
      <article class="brief-card">
        <span>经营效率</span>
        <strong>{{ profitMargin }}%</strong>
        <p>本月利润率，适合和历史月份对比观察经营质量。</p>
      </article>
      <article class="brief-card">
        <span>税负观察</span>
        <strong>{{ taxRatio }}%</strong>
        <p>本月税额占开票额比例，异常波动可进入税务测算复核。</p>
      </article>
      <article class="brief-card">
        <span>AI 情报</span>
        <strong>Ready</strong>
        <p>右上角 AI 可直接读取当前仪表盘指标，生成经营摘要和风险提示。</p>
      </article>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDashboardSummary } from '../api/dashboard'

defineEmits(['open-ai'])

const summary = ref({})
const trendChartRef = ref(null)
const pieChartRef = ref(null)
const radarChartRef = ref(null)
const invoiceCards = ref([])
let trendChart
let pieChart
let radarChart

const profitMargin = computed(() => {
  const income = Number(summary.value.monthlyIncome || 0)
  if (!income) return '0.0'
  return ((Number(summary.value.monthlyProfit || 0) / income) * 100).toFixed(1)
})
const taxRatio = computed(() => {
  const amount = Number(summary.value.monthlyAmount || 0)
  if (!amount) return '0.0'
  return ((Number(summary.value.monthlyTaxAmount || 0) / amount) * 100).toFixed(1)
})
const categoryRank = computed(() => {
  const list = [...(summary.value.categoryStats || [])].sort((a, b) => Number(b.count || 0) - Number(a.count || 0))
  const max = Math.max(...list.map(i => Number(i.count || 0)), 1)
  return list.slice(0, 5).map(i => ({ ...i, percent: Math.round((Number(i.count || 0) / max) * 100) }))
})
const aiPrompt = computed(() => `请基于当前仪表盘数据做财税经营分析：本月收入${formatMoney(summary.value.monthlyIncome)}，本月支出${formatMoney(summary.value.monthlyExpense)}，本月利润${formatMoney(summary.value.monthlyProfit)}，本月税额${formatMoney(summary.value.monthlyTaxAmount)}，年度净利润${formatMoney(summary.value.netProfit)}。`)

function formatMoney(val) {
  if (!val) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function updateCards() {
  const s = summary.value
  invoiceCards.value = [
    { label: '本月发票', value: s.monthlyInvoiceCount || 0, color: '#1e7d7a' },
    { label: '本月开票', value: formatMoney(s.monthlyAmount), color: '#2b7fff' },
    { label: '本月税额', value: formatMoney(s.monthlyTaxAmount), color: '#c98716' },
    { label: '累计发票', value: s.totalInvoiceCount || 0, color: '#53627a' },
    { label: '累计开票', value: formatMoney(s.totalAmount), color: '#53627a' },
    { label: '累计税额', value: formatMoney(s.totalTaxAmount), color: '#53627a' }
  ]
}
function renderTrendChart() {
  if (!trendChartRef.value) return
  trendChart?.dispose()
  trendChart = echarts.init(trendChartRef.value)
  const trends = summary.value.monthlyTrends || []
  trendChart.setOption({
    color: ['#20b486', '#d94f45', '#2b7fff'],
    tooltip: { trigger: 'axis', borderWidth: 0, backgroundColor: 'rgba(23,32,51,.92)', textStyle: { color: '#fff' } },
    legend: { data: ['收入', '支出', '利润'], top: 0, right: 0, icon: 'roundRect' },
    grid: { left: '2%', right: '2%', bottom: '5%', top: '16%', containLabel: true },
    xAxis: { type: 'category', data: trends.map(t => t.month), axisLine: { lineStyle: { color: '#d8e0ea' } }, axisTick: { show: false } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#edf2f7' } } },
    series: [
      { name: '收入', type: 'bar', stack: 'cash', barWidth: 14, data: trends.map(t => t.income), itemStyle: { borderRadius: [5, 5, 0, 0] } },
      { name: '支出', type: 'bar', stack: 'cash', barWidth: 14, data: trends.map(t => -Math.abs(Number(t.expense || 0))), itemStyle: { borderRadius: [0, 0, 5, 5] } },
      { name: '利润', type: 'line', smooth: true, symbolSize: 7, data: trends.map(t => t.profit), lineStyle: { width: 3 }, areaStyle: { opacity: .08 } }
    ]
  })
}
function renderPieChart() {
  if (!pieChartRef.value) return
  pieChart?.dispose()
  pieChart = echarts.init(pieChartRef.value)
  const data = summary.value.categoryStats || []
  pieChart.setOption({
    color: ['#1e7d7a', '#2b7fff', '#20b486', '#c98716', '#d94f45', '#53627a'],
    tooltip: { trigger: 'item', borderWidth: 0, backgroundColor: 'rgba(23,32,51,.92)', textStyle: { color: '#fff' } },
    series: [{ type: 'pie', radius: ['52%', '76%'], center: ['50%', '50%'], data: data.map(d => ({ name: d.category || '未分类', value: d.count })), label: { show: false }, itemStyle: { borderColor: '#fff', borderWidth: 3 } }]
  })
}
function renderRadarChart() {
  if (!radarChartRef.value) return
  radarChart?.dispose()
  radarChart = echarts.init(radarChartRef.value)
  const s = summary.value
  radarChart.setOption({
    color: ['#1e7d7a'],
    radar: { radius: '62%', splitLine: { lineStyle: { color: '#edf2f7' } }, indicator: [
      { name: '收入', max: Math.max(Number(s.monthlyIncome || 0), 1) * 1.2 },
      { name: '利润', max: Math.max(Math.abs(Number(s.monthlyProfit || 0)), 1) * 1.8 },
      { name: '开票', max: Math.max(Number(s.monthlyAmount || 0), 1) * 1.2 },
      { name: '税额', max: Math.max(Number(s.monthlyTaxAmount || 0), 1) * 1.5 },
      { name: '发票', max: Math.max(Number(s.monthlyInvoiceCount || 0), 1) * 1.5 }
    ]},
    series: [{ type: 'radar', areaStyle: { opacity: .16 }, data: [{ value: [s.monthlyIncome || 0, Math.abs(s.monthlyProfit || 0), s.monthlyAmount || 0, s.monthlyTaxAmount || 0, s.monthlyInvoiceCount || 0] }] }]
  })
}
function renderCharts() {
  nextTick(() => { renderTrendChart(); renderPieChart(); renderRadarChart() })
}

onMounted(async () => {
  try {
    const { data } = await getDashboardSummary()
    summary.value = data
    updateCards()
    renderCharts()
    window.addEventListener('resize', () => { trendChart?.resize(); pieChart?.resize(); radarChart?.resize() })
  } catch(e) {}
})
</script>

<style scoped>
.dashboard-page { display: flex; flex-direction: column; gap: 16px; }
.dashboard-hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; padding: 24px; border: 1px solid rgba(38,54,78,.08); border-radius: 8px; color: #fff; background: linear-gradient(135deg, #172033 0%, #17333a 52%, #1e7d7a 100%); box-shadow: 0 18px 45px rgba(17,32,50,.12); }
.hero-kicker { color: rgba(232,241,255,.70); font-size: 12px; font-weight: 780; letter-spacing: .12em; text-transform: uppercase; }
.dashboard-hero h2 { margin: 8px 0; color: #fff !important; font-size: 30px !important; }
.dashboard-hero p { max-width: 720px; color: rgba(232,241,255,.74); line-height: 1.8; }
.hero-actions { display: flex; flex-shrink: 0; gap: 10px; }
.overview-grid { display: grid; grid-template-columns: 1.45fr repeat(3, 1fr); gap: 14px; }
.profit-panel, .metric-card { min-height: 128px; padding: 18px; border: 1px solid rgba(38,54,78,.08); border-radius: 8px; background: rgba(255,255,255,.94); box-shadow: 0 14px 35px rgba(17,32,50,.06); transition: transform .22s ease, box-shadow .22s ease, border-color .22s ease; }
.profit-panel:hover, .metric-card:hover, .invoice-chip:hover, .brief-card:hover, .chart-card:hover { transform: translateY(-3px) scale(1.012); border-color: rgba(30,125,122,.20); box-shadow: 0 20px 48px rgba(17,32,50,.12) !important; }
.profit-panel { background: linear-gradient(145deg, #ffffff, #eef8f6); }
.profit-panel span, .metric-card span, .invoice-chip span { display: block; color: #718096; font-size: 13px; font-weight: 680; }
.profit-panel strong, .metric-card strong { display: block; margin-top: 12px; color: #172033; font-size: 28px; line-height: 1.15; }
.profit-subline { display: flex; gap: 16px; margin-top: 12px; color: #718096; }
.metric-card small { display: block; margin-top: 12px; color: #718096; }
.good { color: #20b486 !important; }
.bad { color: #d94f45 !important; }
.invoice-strip { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 10px; padding: 10px; border: 1px solid rgba(38,54,78,.08); border-radius: 8px; background: rgba(255,255,255,.56); }
.invoice-chip { padding: 12px 14px; border-radius: 7px; background: rgba(255,255,255,.86); transition: transform .22s ease, box-shadow .22s ease; }
.invoice-chip strong { display: block; margin-top: 8px; font-size: 19px; word-break: break-word; }
.analysis-grid { display: grid; grid-template-columns: minmax(0, 1.35fr) minmax(280px, .75fr) minmax(280px, .75fr); gap: 16px; align-items: stretch; }
.trend-card { grid-column: span 1; }
.card-title { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; }
.card-title span { font-size: 15px; font-weight: 760; }
.card-title small { color: #718096; font-size: 12px; font-weight: 600; }
.chart-card { transition: transform .22s ease, box-shadow .22s ease, border-color .22s ease; }
.chart-box { width: 100%; height: 248px; transition: transform .22s ease; }
.chart-card:hover .chart-box { transform: scale(1.015); }
.chart-box.trend { height: 330px; }
.trend-combo { display: grid; grid-template-columns: minmax(0, 1fr) 240px; gap: 14px; align-items: stretch; }
.radar-side { padding-left: 14px; border-left: 1px solid rgba(38,54,78,.08); }
.side-title { color: #53627a; font-size: 13px; font-weight: 760; margin-bottom: 6px; }
.chart-box.compact { height: 288px; }
.rank-list { display: grid; gap: 15px; min-height: 220px; align-content: start; }
.rank-row > div { display: flex; justify-content: space-between; margin-bottom: 8px; color: #26364f; }
.rank-row strong { font-size: 14px; }
.rank-row span { color: #718096; font-size: 12px; }
@media (max-width: 1260px) { .dashboard-hero { align-items: stretch; flex-direction: column; } .overview-grid, .analysis-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .trend-card { grid-column: 1 / -1; } .invoice-strip { grid-template-columns: repeat(3, minmax(0, 1fr)); } .trend-combo { grid-template-columns: 1fr; } .radar-side { padding-left: 0; border-left: 0; border-top: 1px solid rgba(38,54,78,.08); padding-top: 12px; } }
@media (max-width: 640px) { .overview-grid, .analysis-grid, .invoice-strip, .brief-grid { grid-template-columns: 1fr; } .hero-actions { flex-direction: column; } .chart-box.trend { height: 300px; } }
.brief-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.brief-card { padding: 18px; border: 1px solid rgba(38,54,78,.08); border-radius: 8px; background: rgba(255,255,255,.86); box-shadow: 0 12px 30px rgba(17,32,50,.05); transition: transform .22s ease, box-shadow .22s ease, border-color .22s ease; }
.brief-card span { color: #718096; font-size: 13px; font-weight: 720; }
.brief-card strong { display: block; margin-top: 10px; color: #172033; font-size: 24px; }
.brief-card p { margin-top: 10px; color: #718096; line-height: 1.7; font-size: 13px; }
</style>
