<!--
  @component HeroStatsCard
  @description 顶部统计卡片组件 - 展示关键指标和智能洞察
  @props stats - 统计数据
  @props snapshot - 快照数据
  @props insights - 洞察建议
  @props selectionTag - 筛选标签文本
-->
<template>
  <section class="hero-card">
    <div class="hero-copy">
      <div class="hero-badge">云惠农作业智能分析系统</div>
      <h1 class="hero-title">数据可视化洞察中心</h1>
      <p class="hero-desc">
        从时间趋势、结构构成与区域分布三个维度，为业务团队提供对农情数据的动态感知能力，助力制定科学的种植与调度策略。
      </p>
      <div class="hero-stats">
        <div v-for="item in stats" :key="item.label" class="hero-stat">
          <div class="hero-stat-label">{{ item.label }}</div>
          <div class="hero-stat-value">{{ item.value }}</div>
          <div class="hero-stat-sub">{{ item.sub }}</div>
        </div>
      </div>
    </div>
    <div class="hero-side">
      <div class="snapshot-card">
        <div class="snapshot-title">当前筛选概览</div>
        <div class="snapshot-chip">{{ selectionTag }}</div>
        <ul class="snapshot-list">
          <li>
            <span class="snapshot-label">记录总量</span>
            <span class="snapshot-value">{{ snapshot.total }}</span>
          </li>
          <li>
            <span class="snapshot-label">覆盖作物</span>
            <span class="snapshot-value">{{ snapshot.crops }}</span>
          </li>
          <li>
            <span class="snapshot-label">时间范围</span>
            <span class="snapshot-value">{{ snapshot.range }}</span>
          </li>
        </ul>
      </div>
      <div class="insight-card">
        <div class="insight-card-title">智能洞察建议</div>
        <ul class="insight-list">
          <li v-for="tip in insights" :key="tip">{{ tip }}</li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script setup>
defineProps({
  stats: { type: Array, default: () => [] },
  snapshot: {
    type: Object,
    default: () => ({ total: 0, crops: 0, range: '-' })
  },
  insights: { type: Array, default: () => [] },
  selectionTag: { type: String, default: '全部数据' }
})
</script>

<style scoped>
.hero-card {
  display: grid;
  grid-template-columns: 1fr 400px;
  gap: 32px;
  padding: 32px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: #fff;
  margin-bottom: 24px;
}

.hero-badge {
  display: inline-block;
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 16px;
}

.hero-title {
  font-size: 32px;
  font-weight: 700;
  margin: 0 0 12px 0;
}

.hero-desc {
  font-size: 15px;
  line-height: 1.8;
  opacity: 0.95;
  margin-bottom: 24px;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 20px;
}

.hero-stat {
  padding: 16px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  backdrop-filter: blur(10px);
}

.hero-stat-label {
  font-size: 13px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.hero-stat-value {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 4px;
}

.hero-stat-sub {
  font-size: 12px;
  opacity: 0.8;
}

.hero-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.snapshot-card,
.insight-card {
  padding: 20px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 10px;
  color: #303133;
}

.snapshot-title,
.insight-card-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #606266;
}

.snapshot-chip {
  display: inline-block;
  padding: 6px 12px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 4px;
  font-size: 13px;
  margin-bottom: 12px;
}

.snapshot-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.snapshot-list li {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.snapshot-list li:last-child {
  border-bottom: none;
}

.snapshot-label {
  font-size: 13px;
  color: #909399;
}

.snapshot-value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.insight-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.insight-list li {
  font-size: 13px;
  line-height: 1.8;
  color: #606266;
  padding-left: 16px;
  position: relative;
  margin-bottom: 8px;
}

.insight-list li::before {
  content: '💡';
  position: absolute;
  left: 0;
}

@media (max-width: 1200px) {
  .hero-card {
    grid-template-columns: 1fr;
  }
  
  .hero-side {
    flex-direction: row;
  }
}

@media (max-width: 768px) {
  .hero-side {
    flex-direction: column;
  }
}
</style>
