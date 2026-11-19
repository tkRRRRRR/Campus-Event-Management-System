<template>
  <div class="page">
    <div class="card">
      <el-tabs v-model="activeTab" @tab-click="handleTabChange">
        <el-tab-pane v-for="t in tabs" :key="t.name" :label="t.label" :name="t.name"></el-tab-pane>
      </el-tabs>

      <div class="toolbar search-bar">
        <el-input
          v-model="keyword"
          size="small"
          class="kw-input"
          placeholder="搜索活动名称"
          clearable
          @keyup.enter.native="handleSearch"
          @clear="handleSearch"
        >
          <el-button slot="append" icon="el-icon-search" @click="handleSearch"></el-button>
        </el-input>
        <el-button size="small" @click="handleSearch">搜索</el-button>
        <div class="spacer"></div>
        <span class="muted">共 {{ total }} 条报名记录</span>
      </div>

      <el-table :data="list" v-loading="loading" stripe empty-text="暂无报名记录">
        <el-table-column label="活动" min-width="240">
          <template slot-scope="scope">
            <div class="act-cell">
              <div class="act-cover">
                <img v-if="scope.row.coverUrl" :src="resolveUrl(scope.row.coverUrl)" class="cover" :alt="scope.row.activityTitle" />
                <div v-else class="cover-fallback">活动</div>
              </div>
              <div class="act-info">
                <div class="act-title" @click="goDetail(scope.row)">{{ scope.row.activityTitle }}</div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="地点" min-width="130">
          <template slot-scope="scope">{{ scope.row.location || '待定' }}</template>
        </el-table-column>

        <el-table-column label="活动时间" width="190">
          <template slot-scope="scope">
            <div class="time-cell">{{ fmtDateTime(scope.row.startTime) }}</div>
            <div class="time-cell muted">至 {{ fmtDateTime(scope.row.endTime) }}</div>
          </template>
        </el-table-column>

        <el-table-column label="报名时间" width="150">
          <template slot-scope="scope">{{ fmtDateTime(scope.row.signupTime) }}</template>
        </el-table-column>

        <el-table-column label="签到时间" width="150">
          <template slot-scope="scope">{{ fmtDateTime(scope.row.checkinTime) }}</template>
        </el-table-column>

        <el-table-column label="报名状态" width="100">
          <template slot-scope="scope">
            <status-tag :status="scope.row.status" kind="registration" />
          </template>
        </el-table-column>

        <el-table-column label="活动状态" width="100">
          <template slot-scope="scope">
            <status-tag :status="scope.row.activityStatus" />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="130" fixed="right">
          <template slot-scope="scope">
            <el-button
              v-if="canCancel(scope.row)"
              type="text"
              size="mini"
              @click="handleCancel(scope.row)"
            >取消报名</el-button>
            <el-button
              v-if="showReview(scope.row)"
              type="text"
              size="mini"
              @click="goDetail(scope.row)"
            >去评价</el-button>
            <el-button
              v-if="showCheckin(scope.row)"
              type="text"
              size="mini"
              @click="goDetail(scope.row)"
            >去签到</el-button>
            <span
              v-if="!canCancel(scope.row) && !showReview(scope.row) && !showCheckin(scope.row)"
              class="muted"
            >—</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager" v-if="total > 0">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="page.pageSize"
          :current-page.sync="page.pageNum"
          @current-change="loadList"
        />
      </div>
    </div>
  </div>
</template>

<script>
import dayjs from 'dayjs'
import { myRegistrations, cancelRegistration } from '@/api/registration'
import { resolveUrl } from '@/utils/request'
import { fmtDateTime, signupExpired } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

export default {
  name: 'MyRegistrations',
  components: { StatusTag },
  data () {
    return {
      tabs: [
        { name: 'all', label: '全部' },
        { name: '5', label: '待审核' },
        { name: '1', label: '已报名' },
        { name: '2', label: '已签到' },
        { name: 'ended', label: '已结束' },
        { name: '3', label: '已取消' },
        { name: '4', label: '爽约' }
      ],
      activeTab: 'all',
      keyword: '',
      page: { pageNum: 1, pageSize: 10 },
      list: [],
      total: 0,
      loading: false
    }
  },
  created () {
    this.loadList()
  },
  methods: {
    resolveUrl,
    fmtDateTime,
    statusParam () {
      if (this.activeTab === 'all' || this.activeTab === 'ended') return null
      return Number(this.activeTab)
    },
    async loadList () {
      this.loading = true
      const kw = (this.keyword || '').trim()
      try {
        const params = {
          pageNum: this.page.pageNum,
          pageSize: this.page.pageSize
        }
        if (this.activeTab === 'ended') {
          // 「已结束」按活动状态筛选，后端支持 activityStatus 参数
          params.activityStatus = 5
        } else {
          const status = this.statusParam()
          if (status !== null) params.status = status
        }
        if (kw) params.keyword = kw
        const data = await myRegistrations(params)
        this.list = (data && data.records) || []
        this.total = (data && data.total) || 0
      } catch (e) {
        this.list = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    isStarted (row) {
      if (!row.startTime) return false
      return !dayjs().isBefore(dayjs(row.startTime))
    },
    isOngoing (row) {
      if (!row.startTime) return false
      const now = dayjs()
      const start = dayjs(row.startTime)
      if (now.isBefore(start)) return false
      return row.endTime ? now.isBefore(dayjs(row.endTime)) : true
    },
    /** 签到窗口：活动开始前 30 分钟 ~ 结束时间（与后端 early-minutes 一致） */
    isCheckinOpen (row) {
      if (!row.startTime) return false
      const now = dayjs()
      if (now.isBefore(dayjs(row.startTime).subtract(30, 'minute'))) return false
      return row.endTime ? now.isBefore(dayjs(row.endTime)) : true
    },
    canCancel (row) {
      return (row.status === 1 || row.status === 5) &&
        !this.isStarted(row) &&
        !signupExpired(row.signupDeadline)
    },
    showReview (row) {
      return row.status === 2 && row.activityStatus === 5 && !row.reviewed
    },
    showCheckin (row) {
      return row.status === 1 && this.isCheckinOpen(row)
    },
    handleTabChange () {
      this.page.pageNum = 1
      this.loadList()
    },
    handleSearch () {
      this.page.pageNum = 1
      this.loadList()
    },
    goDetail (row) {
      this.$router.push(`/activity/${row.activityId}`)
    },
    async handleCancel (row) {
      try {
        await this.$confirm(`确定取消报名「${row.activityTitle}」吗？`, '提示', { type: 'warning' })
      } catch (e) {
        return
      }
      try {
        await cancelRegistration(row.activityId)
        this.$message.success('已取消报名')
        await this.loadList()
      } catch (e) {
        // 错误提示已由 request 拦截器统一处理
      }
    }
  }
}
</script>

<style scoped>
.search-bar { margin-top: 2px; }
.kw-input { width: 260px; }
.act-cell { display: flex; align-items: center; gap: 10px; }
.act-cover {
  width: 58px;
  height: 42px;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
  background: #eef1f2;
}
.act-cover .cover-fallback { font-size: 12px; letter-spacing: 0; }
.act-info { min-width: 0; }
.act-title {
  font-size: 13px;
  color: var(--primary);
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 230px;
}
.act-title:hover { text-decoration: underline; }
.act-sub {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 230px;
}
.time-cell { font-size: 12px; line-height: 18px; }
</style>
