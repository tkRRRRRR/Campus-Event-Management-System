<template>
  <div class="page">
    <div class="page-title">我的活动数据</div>

    <el-row :gutter="14" class="ov-row">
      <el-col v-for="item in overviewCards" :key="item.key" :span="4">
        <div class="card ov-card" :class="'ov-' + item.key">
          <div class="num">{{ item.value }}</div>
          <div class="label">{{ item.label }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="14" class="chart-row">
      <el-col :span="15">
        <div class="card">
          <div class="card-title">
            <span>报名 / 签到趋势</span>
            <el-radio-group v-model="days" size="mini" @change="loadTrend">
              <el-radio-button :label="7">7 天</el-radio-button>
              <el-radio-button :label="14">14 天</el-radio-button>
              <el-radio-button :label="30">30 天</el-radio-button>
            </el-radio-group>
          </div>
          <div class="chart-box" v-loading="trendLoading">
            <div ref="trendChartEl" class="chart"></div>
            <div v-if="!trendLoading && !trendHasData" class="empty-chart">暂无数据</div>
          </div>
        </div>
      </el-col>

      <el-col :span="9">
        <div class="card">
          <div class="card-title">
            <span>活动分类分布</span>
            <span class="muted">按报名人数</span>
          </div>
          <div class="chart-box" v-loading="categoryLoading">
            <div ref="categoryChartEl" class="chart"></div>
            <div v-if="!categoryLoading && !categoryHasData" class="empty-chart">暂无数据</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="card">
      <div class="card-title"><span>报名排行 TOP 10</span></div>
      <el-table v-loading="topLoading" :data="topList" size="small" stripe empty-text="暂无数据">
        <el-table-column label="排名" width="80" align="center">
          <template slot-scope="{ $index }">
            <span class="rank" :class="'rank-' + ($index + 1)">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="活动标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="分类" width="130">
          <template slot-scope="{ row }">{{ row.categoryName || '-' }}</template>
        </el-table-column>
        <el-table-column label="名额" width="90">
          <template slot-scope="{ row }">{{ row.capacity || 0 }}</template>
        </el-table-column>
        <el-table-column label="报名数" width="100">
          <template slot-scope="{ row }">
            <span class="join">{{ row.joinedCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template slot-scope="{ row }">
            <status-tag :status="row.status" />
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { organizerOverview, trendStat, categoryStat, topActivities } from '@/api/stat'
import StatusTag from '@/components/StatusTag.vue'

export default {
  name: 'ActivityStats',
  components: { StatusTag },
  data () {
    return {
      overview: {},
      days: 7,
      trendHasData: false,
      trendLoading: false,
      trendChart: null,
      categoryHasData: false,
      categoryLoading: false,
      categoryChart: null,
      topList: [],
      topLoading: false
    }
  },
  computed: {
    overviewCards () {
      const data = this.overview || {}
      const rate = Number(data.checkinRate) || 0
      const score = Number(data.avgScore) || 0
      return [
        { key: 'activity', label: '活动数', value: data.activityCount || 0 },
        { key: 'registration', label: '报名总数', value: data.registrationCount || 0 },
        { key: 'checked', label: '签到数', value: data.checkedCount || 0 },
        { key: 'rate', label: '签到率', value: rate.toFixed(1) + '%' },
        { key: 'review', label: '评价数', value: data.reviewCount || 0 },
        { key: 'score', label: '平均分', value: score.toFixed(1) }
      ]
    }
  },
  mounted () {
    window.addEventListener('resize', this.handleResize)
    // 图表容器始终在 DOM 中，mounted 阶段即可初始化实例，数据到达后 setOption
    this.trendChart = echarts.init(this.$refs.trendChartEl)
    this.categoryChart = echarts.init(this.$refs.categoryChartEl)
    this.loadOverview()
    this.loadTrend()
    this.loadCategory()
    this.loadTop()
  },
  beforeDestroy () {
    window.removeEventListener('resize', this.handleResize)
    this.disposeChart('trendChart')
    this.disposeChart('categoryChart')
  },
  methods: {
    async loadOverview () {
      try {
        this.overview = (await organizerOverview()) || {}
      } catch (e) {
        this.overview = {}
      }
    },
    async loadTrend () {
      this.trendLoading = true
      try {
        const rows = (await trendStat(this.days)) || []
        this.trendHasData = rows.some(row => (row.registrationCount || 0) > 0 || (row.checkinCount || 0) > 0)
        if (this.trendHasData) {
          this.renderTrend(rows)
        } else if (this.trendChart) {
          this.trendChart.clear()
        }
      } catch (e) {
        this.trendHasData = false
        if (this.trendChart) this.trendChart.clear()
      } finally {
        this.trendLoading = false
      }
    },
    renderTrend (rows) {
      if (!this.trendChart) return
      this.trendChart.setOption({
        color: ['#2f7d6b', '#e6a23c'],
        tooltip: { trigger: 'axis' },
        legend: { data: ['报名数', '签到数'], right: 10, top: 0 },
        grid: { left: 44, right: 20, top: 40, bottom: 32 },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: rows.map(row => row.date)
        },
        yAxis: { type: 'value', minInterval: 1 },
        series: [
          {
            name: '报名数',
            type: 'line',
            smooth: true,
            showSymbol: true,
            symbolSize: 6,
            data: rows.map(row => row.registrationCount || 0),
            areaStyle: { opacity: 0.12 }
          },
          {
            name: '签到数',
            type: 'line',
            smooth: true,
            showSymbol: true,
            symbolSize: 6,
            data: rows.map(row => row.checkinCount || 0),
            areaStyle: { opacity: 0.12 }
          }
        ]
      }, true)
      this.trendChart.resize()
    },
    async loadCategory () {
      this.categoryLoading = true
      try {
        const rows = (await categoryStat()) || []
        const pieRows = rows.filter(row => (row.registrationCount || 0) > 0)
        this.categoryHasData = pieRows.length > 0
        if (this.categoryHasData) {
          this.renderCategory(pieRows)
        } else if (this.categoryChart) {
          this.categoryChart.clear()
        }
      } catch (e) {
        this.categoryHasData = false
        if (this.categoryChart) this.categoryChart.clear()
      } finally {
        this.categoryLoading = false
      }
    },
    renderCategory (rows) {
      if (!this.categoryChart) return
      this.categoryChart.setOption({
        color: ['#2f7d6b', '#45a08b', '#e6a23c', '#f56c6c', '#409eff', '#67c23a', '#909399', '#9c6ade'],
        tooltip: { trigger: 'item', formatter: '{b}：{c} 人（{d}%）' },
        legend: {
          type: 'scroll',
          orient: 'vertical',
          right: 4,
          top: 'middle',
          itemWidth: 10,
          itemHeight: 10
        },
        series: [
          {
            name: '报名人数',
            type: 'pie',
            radius: ['42%', '68%'],
            center: ['38%', '50%'],
            avoidLabelOverlap: true,
            itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
            label: { show: false },
            emphasis: { label: { show: true, fontSize: 13, fontWeight: 'bold' } },
            data: rows.map(row => ({ name: row.name, value: row.registrationCount || 0 }))
          }
        ]
      }, true)
      this.categoryChart.resize()
    },
    async loadTop () {
      this.topLoading = true
      try {
        this.topList = (await topActivities(10)) || []
      } catch (e) {
        this.topList = []
      } finally {
        this.topLoading = false
      }
    },
    disposeChart (key) {
      const chart = this[key]
      if (chart) {
        chart.dispose()
        this[key] = null
      }
    },
    handleResize () {
      if (this.trendChart) this.trendChart.resize()
      if (this.categoryChart) this.categoryChart.resize()
    }
  }
}
</script>

<style scoped>
.ov-row {
  margin-bottom: 14px;
}
.ov-card {
  padding: 14px 16px;
}
.ov-card .num {
  font-size: 26px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--primary);
}
.ov-card .label {
  margin-top: 2px;
  font-size: 13px;
  color: var(--text-sub);
}
.ov-rate .num {
  color: #67c23a;
}
.ov-score .num {
  color: #e6a23c;
}
.chart-row {
  margin-bottom: 14px;
}
.card-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 14px;
}
.card-title .muted {
  font-weight: 400;
  font-size: 12px;
}
.chart-box {
  position: relative;
}
.chart {
  width: 100%;
  height: 300px;
}
.empty-chart {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  color: var(--text-sub);
  font-size: 13px;
}
.rank {
  display: inline-block;
  width: 22px;
  height: 22px;
  line-height: 22px;
  border-radius: 50%;
  background: #f0f2f3;
  color: #909399;
  font-size: 12px;
  font-weight: 600;
}
.rank-1 {
  background: #fdf0d5;
  color: #e6a23c;
}
.rank-2 {
  background: #eef1f2;
  color: #606266;
}
.rank-3 {
  background: #fbe9e7;
  color: #d2691e;
}
.join {
  color: var(--primary);
  font-weight: 600;
}
</style>
