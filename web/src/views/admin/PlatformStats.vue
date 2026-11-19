<template>
  <div class="page">
    <h2 class="page-title">平台统计</h2>

    <!-- 核心指标 -->
    <el-row :gutter="16" class="stat-row" v-loading="overviewLoading">
      <el-col v-for="card in overviewCards" :key="card.label" :xs="12" :sm="8" :md="4">
        <div class="card stat-card">
          <div class="stat-label">{{ card.label }}</div>
          <div class="stat-value" :style="{ color: card.color }">
            {{ card.value }}<span class="stat-unit">{{ card.unit }}</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 状态分布 -->
    <el-row :gutter="16" class="stat-row" v-loading="overviewLoading">
      <el-col :md="12" :sm="24">
        <div class="card">
          <div class="card-head">
            <h3>活动状态分布</h3>
            <span class="muted">共 {{ num(overview.activityCount) }} 个活动</span>
          </div>
          <div class="dist-list">
            <div class="dist-item" v-for="item in activityStatusDist" :key="item.label">
              <span class="dist-dot" :style="{ backgroundColor: item.color }"></span>
              <span class="dist-label">{{ item.label }}</span>
              <span class="dist-value">{{ item.value }}</span>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :md="12" :sm="24">
        <div class="card">
          <div class="card-head">
            <h3>报名状态分布</h3>
            <span class="muted">共 {{ num(overview.registrationCount) }} 条报名</span>
          </div>
          <div class="dist-list">
            <div class="dist-item" v-for="item in registrationStatusDist" :key="item.label">
              <span class="dist-dot" :style="{ backgroundColor: item.color }"></span>
              <span class="dist-label">{{ item.label }}</span>
              <span class="dist-value">{{ item.value }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :md="14" :sm="24">
        <div class="card">
          <div class="card-head">
            <h3>报名 / 签到趋势</h3>
            <el-radio-group v-model="trendDays" size="mini" @change="loadTrend">
              <el-radio-button :label="7">近 7 天</el-radio-button>
              <el-radio-button :label="14">近 14 天</el-radio-button>
              <el-radio-button :label="30">近 30 天</el-radio-button>
            </el-radio-group>
          </div>
          <div class="chart-wrap" v-loading="trendLoading">
            <div ref="trendChart" class="chart"></div>
            <div class="chart-empty" v-if="!trendData.length">暂无趋势数据</div>
          </div>
        </div>
      </el-col>
      <el-col :md="10" :sm="24">
        <div class="card">
          <div class="card-head">
            <h3>分类分布</h3>
          </div>
          <div class="chart-wrap" v-loading="categoryLoading">
            <div ref="categoryChart" class="chart"></div>
            <div class="chart-empty" v-if="!categoryData.length">暂无分类数据</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 报名排行 -->
    <div class="card">
      <div class="card-head">
        <h3>报名排行 TOP5</h3>
        <span class="muted">按实际报名人数排序</span>
      </div>
      <el-table v-loading="topLoading" :data="topList" border stripe empty-text="暂无报名数据">
        <el-table-column label="排名" width="80" align="center">
          <template slot-scope="scope">
            <span class="rank" :class="'rank-' + (scope.$index + 1)">{{ scope.$index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="活动标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="分类" width="120">
          <template slot-scope="scope">{{ scope.row.categoryName || '未分类' }}</template>
        </el-table-column>
        <el-table-column label="名额" width="90" align="center">
          <template slot-scope="scope">{{ scope.row.capacity }}</template>
        </el-table-column>
        <el-table-column label="报名人数" width="100" align="center">
          <template slot-scope="scope">{{ scope.row.joinedCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="签到人数" width="100" align="center">
          <template slot-scope="scope">{{ scope.row.signedCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="scope">
            <status-tag :status="scope.row.status" />
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { platformOverview, categoryStat, trendStat, topActivities } from '@/api/stat'
import StatusTag from '@/components/StatusTag.vue'

export default {
  name: 'PlatformStats',
  components: { StatusTag },
  data () {
    return {
      overview: {},
      overviewLoading: false,
      trendDays: 7,
      trendData: [],
      trendLoading: false,
      categoryData: [],
      categoryLoading: false,
      topList: [],
      topLoading: false
    }
  },
  computed: {
    overviewCards () {
      const o = this.overview || {}
      return [
        { label: '用户总数', value: this.num(o.userCount), unit: '人', color: '#2f7d6b' },
        { label: '活动总数', value: this.num(o.activityCount), unit: '个', color: '#409eff' },
        { label: '报名总数', value: this.num(o.registrationCount), unit: '人次', color: '#e6a23c' },
        { label: '签到率', value: this.dec(o.checkinRate), unit: '%', color: '#67c23a' },
        { label: '评价数', value: this.num(o.reviewCount), unit: '条', color: '#909399' },
        { label: '平均分', value: this.dec(o.avgScore), unit: '分', color: '#f56c6c' }
      ]
    },
    activityStatusDist () {
      const o = this.overview || {}
      return [
        { label: '草稿', value: this.num(o.draftCount), color: '#909399' },
        { label: '待审核', value: this.num(o.pendingCount), color: '#e6a23c' },
        { label: '报名中', value: this.num(o.publishedCount), color: '#67c23a' },
        { label: '已结束', value: this.num(o.finishedCount), color: '#409eff' },
        { label: '已取消', value: this.num(o.canceledCount), color: '#c0c4cc' }
      ]
    },
    registrationStatusDist () {
      const o = this.overview || {}
      return [
        { label: '已报名', value: this.num(o.signedUpCount), color: '#409eff' },
        { label: '已签到', value: this.num(o.checkedCount), color: '#67c23a' },
        { label: '爽约', value: this.num(o.absentCount), color: '#f56c6c' },
        { label: '已取消', value: this.num(o.cancelRegCount), color: '#909399' }
      ]
    }
  },
  mounted () {
    this.trendChart = echarts.init(this.$refs.trendChart)
    this.categoryChart = echarts.init(this.$refs.categoryChart)
    window.addEventListener('resize', this.handleResize)
    this.loadOverview()
    this.loadTrend()
    this.loadCategory()
    this.loadTop()
  },
  beforeDestroy () {
    window.removeEventListener('resize', this.handleResize)
    if (this.trendChart) {
      this.trendChart.dispose()
      this.trendChart = null
    }
    if (this.categoryChart) {
      this.categoryChart.dispose()
      this.categoryChart = null
    }
  },
  methods: {
    num (value) {
      if (value === null || value === undefined || value === '') return 0
      const n = Number(value)
      return isNaN(n) ? 0 : n
    },
    dec (value) {
      if (value === null || value === undefined || value === '') return '0.0'
      const n = Number(value)
      return isNaN(n) ? '0.0' : n.toFixed(1)
    },
    async loadOverview () {
      this.overviewLoading = true
      try {
        this.overview = (await platformOverview()) || {}
      } catch (e) {
        this.overview = {}
      } finally {
        this.overviewLoading = false
      }
    },
    async loadTrend () {
      this.trendLoading = true
      try {
        this.trendData = (await trendStat(this.trendDays)) || []
        this.renderTrend()
      } catch (e) {
        this.trendData = []
        this.renderTrend()
      } finally {
        this.trendLoading = false
      }
    },
    async loadCategory () {
      this.categoryLoading = true
      try {
        const data = (await categoryStat()) || []
        this.categoryData = data
          .filter(item => this.num(item.activityCount) > 0)
          .map(item => ({ name: item.name || '未分类', value: this.num(item.activityCount) }))
        this.renderCategory()
      } catch (e) {
        this.categoryData = []
        this.renderCategory()
      } finally {
        this.categoryLoading = false
      }
    },
    async loadTop () {
      this.topLoading = true
      try {
        this.topList = (await topActivities(5)) || []
      } catch (e) {
        this.topList = []
      } finally {
        this.topLoading = false
      }
    },
    renderTrend () {
      if (!this.trendChart) return
      if (!this.trendData.length) {
        this.trendChart.clear()
        return
      }
      const dates = this.trendData.map(item => item.date || '')
      const registrations = this.trendData.map(item => this.num(item.registrationCount))
      const checkins = this.trendData.map(item => this.num(item.checkinCount))
      this.trendChart.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['报名数', '签到数'], top: 0, itemWidth: 14, itemHeight: 8 },
        grid: { left: 44, right: 20, top: 40, bottom: 30 },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: dates,
          axisLine: { lineStyle: { color: '#dcdfe6' } },
          axisLabel: { color: '#909399' }
        },
        yAxis: {
          type: 'value',
          minInterval: 1,
          axisLine: { show: false },
          axisTick: { show: false },
          splitLine: { lineStyle: { color: '#f0f2f5' } },
          axisLabel: { color: '#909399' }
        },
        series: [
          {
            name: '报名数',
            type: 'line',
            smooth: true,
            symbolSize: 6,
            data: registrations,
            itemStyle: { color: '#2f7d6b' },
            areaStyle: { color: 'rgba(47, 125, 107, 0.12)' }
          },
          {
            name: '签到数',
            type: 'line',
            smooth: true,
            symbolSize: 6,
            data: checkins,
            itemStyle: { color: '#e6a23c' },
            areaStyle: { color: 'rgba(230, 162, 60, 0.10)' }
          }
        ]
      }, true)
    },
    renderCategory () {
      if (!this.categoryChart) return
      if (!this.categoryData.length) {
        this.categoryChart.clear()
        return
      }
      this.categoryChart.setOption({
        tooltip: { trigger: 'item', formatter: '{b}：{c} 个活动（{d}%）' },
        legend: {
          type: 'scroll',
          bottom: 0,
          itemWidth: 10,
          itemHeight: 10,
          textStyle: { fontSize: 12 }
        },
        series: [
          {
            name: '分类分布',
            type: 'pie',
            radius: ['42%', '68%'],
            center: ['50%', '44%'],
            avoidLabelOverlap: true,
            itemStyle: { borderColor: '#fff', borderWidth: 2 },
            label: { formatter: '{b}\n{c}' },
            data: this.categoryData
          }
        ]
      }, true)
    },
    handleResize () {
      if (this.trendChart) this.trendChart.resize()
      if (this.categoryChart) this.categoryChart.resize()
    }
  }
}
</script>

<style scoped>
.stat-row {
  margin-bottom: 16px;
}
.stat-card {
  text-align: center;
  padding: 16px 12px;
  height: 100%;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 26px;
  font-weight: 600;
  line-height: 1.2;
}
.stat-unit {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
  margin-left: 4px;
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.card-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.dist-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 4px 0;
}
.dist-item {
  display: flex;
  align-items: center;
  font-size: 13px;
}
.dist-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
  flex-shrink: 0;
}
.dist-label {
  color: #606266;
}
.dist-value {
  margin-left: auto;
  font-weight: 600;
  color: #303133;
}
.chart-wrap {
  position: relative;
}
.chart {
  width: 100%;
  height: 300px;
}
.chart-empty {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  text-align: center;
  font-size: 13px;
  color: #909399;
  pointer-events: none;
}
.rank {
  display: inline-block;
  width: 22px;
  height: 22px;
  line-height: 22px;
  border-radius: 50%;
  background: #f0f2f5;
  color: #909399;
  font-size: 12px;
  font-weight: 600;
}
.rank-1 {
  background: #fef0f0;
  color: #f56c6c;
}
.rank-2 {
  background: #fdf6ec;
  color: #e6a23c;
}
.rank-3 {
  background: #ecf5ff;
  color: #409eff;
}
</style>
