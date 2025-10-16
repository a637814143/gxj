<template>
  <div class="page">
    <section class="overview-banner">
      <div class="banner-content">
        <h1 class="banner-title">农业生产智能概览</h1>
        <p class="banner-desc">
          通过数据驱动的方式实时掌握重点区域、作物和模型任务的运行情况，助力科学决策。
        </p>
        <div class="banner-meta">
          <div class="meta-item" v-for="item in bannerHighlights" :key="item.label">
            <div class="meta-label">{{ item.label }}</div>
            <div class="meta-value">{{ item.value }}</div>
          </div>
        </div>
      </div>
      <div class="banner-illustration">
        <div class="dot dot-1" />
        <div class="dot dot-2" />
        <div class="dot dot-3" />
      </div>
    </section>

    <section class="stat-grid">
      <el-card v-for="card in cards" :key="card.title" class="metric-card" shadow="hover">
        <div class="metric-header">
          <span class="metric-title">{{ card.title }}</span>
          <span class="metric-tag">{{ card.tag }}</span>
        </div>
        <div class="metric-value">{{ card.value }}</div>
        <div class="metric-desc">{{ card.description }}</div>
      </el-card>
    </section>

    <el-card class="chart-card" shadow="never">
      <template #header>
        <div class="chart-header">
          <div>
            <div class="chart-title">近期产量预测趋势</div>
            <div class="chart-subtitle">结合历史表现与模型预测，洞察未来走势</div>
          </div>
          <el-button type="primary" plain round size="small">导出分析</el-button>
        </div>
      </template>
      <div class="placeholder">图表占位，后续接入可视化组件</div>
    </el-card>
  </div>
</template>

<script setup>
const cards = [
  { title: '覆盖区域', value: '32', tag: '运行正常', description: '已接入的重点农业产区数量' },
  { title: '监测作物', value: '18', tag: '持续更新', description: '追踪预测的主要农作物种类' },
  { title: '数据文件', value: '256', tag: '数据资产', description: '已上传的历史数据文件数量' },
  { title: '模型任务', value: '12', tag: '近期任务', description: '近30天内执行的预测任务数' }
]

const bannerHighlights = [
  { label: '今日新增数据', value: '18,420 条' },
  { label: '模型更新', value: '4 次' },
  { label: '运行异常', value: '0 起' }
]
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.overview-banner {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 24px;
  padding: 28px;
  border-radius: 20px;
  background: linear-gradient(135deg, #1f6ff7 0%, #5ba7ff 55%, #9fcaff 100%);
  color: #f5f7ff;
  position: relative;
  overflow: hidden;
  box-shadow: 0 20px 45px rgba(31, 111, 247, 0.25);
}

.banner-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  z-index: 1;
}

.banner-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 0.6px;
}

.banner-desc {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: rgba(245, 247, 255, 0.85);
}

.banner-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.meta-item {
  padding: 12px 18px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(4px);
  min-width: 148px;
}

.meta-label {
  font-size: 12px;
  color: rgba(245, 247, 255, 0.8);
  margin-bottom: 6px;
}

.meta-value {
  font-size: 18px;
  font-weight: 600;
}

.banner-illustration {
  position: relative;
  min-height: 160px;
}

.dot {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.28);
  box-shadow: 0 10px 30px rgba(10, 55, 132, 0.35);
}

.dot-1 {
  width: 140px;
  height: 140px;
  top: 10px;
  right: 18%;
}

.dot-2 {
  width: 90px;
  height: 90px;
  bottom: 18px;
  right: 6%;
}

.dot-3 {
  width: 65px;
  height: 65px;
  top: 38px;
  right: -12px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 18px;
}

.metric-card {
  border-radius: 16px;
  border: none;
  background: linear-gradient(180deg, #ffffff 0%, #f7faff 100%);
  box-shadow: 0 12px 30px rgba(15, 61, 124, 0.08);
  padding: 20px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.metric-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 18px 42px rgba(15, 61, 124, 0.12);
}

.metric-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.metric-title {
  font-size: 15px;
  color: #1f2d3d;
  font-weight: 600;
}

.metric-tag {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(64, 158, 255, 0.16);
  color: #0b66ff;
  font-weight: 500;
}

.metric-value {
  font-size: 32px;
  font-weight: 700;
  color: #0b2a6f;
  margin-bottom: 8px;
}

.metric-desc {
  font-size: 13px;
  color: #6b7a90;
}

.chart-card {
  min-height: 320px;
  border-radius: 18px;
  border: none;
  background: #ffffff;
  box-shadow: 0 16px 36px rgba(15, 61, 124, 0.08);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2d3d;
}

.chart-subtitle {
  font-size: 13px;
  color: #8a97a9;
  margin-top: 4px;
}

.placeholder {
  height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #aeb6c3;
  border: 1px dashed #c8d6f2;
  border-radius: 12px;
  background: linear-gradient(180deg, rgba(244, 249, 255, 0.65), rgba(244, 249, 255, 0.25));
}

@media (max-width: 768px) {
  .overview-banner {
    padding: 22px;
  }

  .metric-value {
    font-size: 28px;
  }

  .chart-card {
    border-radius: 16px;
  }
}
</style>
