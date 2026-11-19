<template>
  <div class="page">
    <div class="page-title">报名名单</div>

    <div class="card head-card">
      <div class="head">
        <div class="head-main">
          <h3 class="act-title">{{ activity.title || '加载中…' }}</h3>
          <div class="muted act-meta">
            <span><i class="el-icon-time"></i> {{ fmtDateTime(activity.startTime) }}</span>
            <span><i class="el-icon-location-outline"></i> {{ activity.location || '地点待定' }}</span>
            <span><i class="el-icon-user"></i> 名额 {{ activity.signedCount || 0 }} / {{ activity.capacity || 0 }}</span>
            <status-tag v-if="activity.status !== undefined" :status="activity.status" />
          </div>
        </div>
        <el-button size="small" icon="el-icon-back" @click="goBack">返回</el-button>
      </div>
    </div>

    <el-row :gutter="14" class="stat-row">
      <el-col v-for="item in statCards" :key="item.key" :span="6">
        <div class="card stat-card" :class="'stat-' + item.key">
          <div class="num">{{ item.value }}</div>
          <div class="label">{{ item.label }}</div>
          <div class="hint" v-if="item.hint">{{ item.hint }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="card">
      <div class="toolbar">
        <el-select v-model="query.status" size="small" style="width: 140px" @change="handleFilter">
          <el-option label="全部状态" value="" />
          <el-option label="待审核" :value="5" />
          <el-option label="已报名" :value="1" />
          <el-option label="已签到" :value="2" />
          <el-option label="爽约" :value="4" />
          <el-option label="已取消" :value="3" />
        </el-select>

        <el-input
          v-model="query.keyword"
          class="search-input"
          size="small"
          clearable
          placeholder="搜索姓名 / 学号"
          prefix-icon="el-icon-search"
          @keyup.enter.native="handleFilter"
          @clear="handleFilter"
        />
        <el-button size="small" @click="handleFilter">搜索</el-button>
        <div class="spacer"></div>
        <el-button size="small" icon="el-icon-refresh" @click="refreshAll">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="list" size="small" stripe empty-text="暂无报名记录">
        <el-table-column label="姓名" width="100">
          <template slot-scope="{ row }">{{ row.realName || '-' }}</template>
        </el-table-column>
        <el-table-column label="学号" width="130">
          <template slot-scope="{ row }">{{ row.studentNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
        <el-table-column label="手机号" width="130">
          <template slot-scope="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template slot-scope="{ row }">
            <status-tag :status="row.status" kind="registration" />
          </template>
        </el-table-column>
        <el-table-column label="报名时间" width="150">
          <template slot-scope="{ row }">{{ fmtDateTime(row.signupTime) }}</template>
        </el-table-column>
        <el-table-column label="签到时间" width="150">
          <template slot-scope="{ row }">{{ fmtDateTime(row.checkinTime) }}</template>
        </el-table-column>
        <el-table-column label="签到方式" width="96">
          <template slot-scope="{ row }">{{ checkinTypeText(row.checkinType) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template slot-scope="{ row }">
            <template v-if="row.status === 5">
              <el-button type="text" size="mini" @click="handleAudit(row, true)">通过</el-button>
              <el-button type="text" size="mini" class="text-danger" @click="handleAudit(row, false)">驳回</el-button>
            </template>
            <el-button
              v-else-if="row.status === 1"
              type="text"
              size="mini"
              @click="handleManualCheckin(row)"
            >补签</el-button>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { getActivity } from '@/api/activity'
import { memberPage, auditRegistration } from '@/api/registration'
import { checkinStat, manualCheckin } from '@/api/checkin'
import { fmtDateTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

export default {
  name: 'RegistrationList',
  components: { StatusTag },
  data () {
    return {
      loading: false,
      activity: {},
      stat: {},
      list: [],
      total: 0,
      query: {
        pageNum: 1,
        pageSize: 10,
        status: '',
        keyword: ''
      }
    }
  },
  computed: {
    activityId () {
      return Number(this.$route.params.id)
    },
    statCards () {
      const stat = this.stat || {}
      return [
        { key: 'signed', label: '报名总数', value: stat.signedCount || 0, hint: '已占名额（含已签到与爽约）' },
        { key: 'checked', label: '已签到', value: stat.checkedCount || 0 },
        { key: 'absent', label: '爽约', value: stat.absentCount || 0 },
        { key: 'canceled', label: '取消报名', value: (this.activity && this.activity.cancelCount) || 0 }
      ]
    }
  },
  created () {
    this.loadActivity()
    this.loadStat()
    this.load()
  },
  methods: {
    fmtDateTime,
    checkinTypeText (type) {
      if (type === 1) return '签到码'
      if (type === 2) return '补签'
      return '-'
    },
    async loadActivity () {
      try {
        this.activity = (await getActivity(this.activityId)) || {}
      } catch (e) {
        /* 错误提示已由 request 拦截器统一处理 */
      }
    },
    async loadStat () {
      try {
        this.stat = (await checkinStat(this.activityId)) || {}
      } catch (e) {
        this.stat = {}
      }
    },
    async load () {
      this.loading = true
      try {
        const params = {
          pageNum: this.query.pageNum,
          pageSize: this.query.pageSize
        }
        if (this.query.status !== '' && this.query.status !== null && this.query.status !== undefined) {
          params.status = this.query.status
        }
        const keyword = (this.query.keyword || '').trim()
        if (keyword) params.keyword = keyword

        const data = await memberPage(this.activityId, params)
        this.list = (data && data.records) || []
        this.total = (data && data.total) || 0
      } catch (e) {
        this.list = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    refreshAll () {
      this.loadStat()
      this.load()
    },
    handleFilter () {
      this.query.pageNum = 1
      this.load()
    },
    handlePageChange (page) {
      this.query.pageNum = page
      this.load()
    },
    handleSizeChange (size) {
      this.query.pageSize = size
      this.query.pageNum = 1
      this.load()
    },
    async handleAudit (row, pass) {
      if (pass) {
        try {
          await this.$confirm(
            '确定通过「' + (row.realName || row.username) + '」的报名申请吗？',
            '通过报名',
            { type: 'info', confirmButtonText: '通过', cancelButtonText: '取消' }
          )
        } catch (e) {
          return
        }
        await this.doAudit(row, { pass: true }, '已通过报名')
        return
      }

      let reason = ''
      try {
        const result = await this.$prompt(
          '填写驳回理由（可留空），学生可在消息中心查看审核结果。',
          '驳回报名',
          {
            inputPlaceholder: '例如：名额已满 / 信息不完整',
            confirmButtonText: '确定驳回',
            cancelButtonText: '取消',
            inputType: 'textarea'
          }
        )
        reason = (result && result.value) || ''
      } catch (e) {
        return
      }
      await this.doAudit(row, { pass: false, reason: reason }, '已驳回报名')
    },
    async doAudit (row, body, successText) {
      try {
        await auditRegistration(row.id, body)
        this.$message.success(successText)
        this.load()
        this.loadStat()
      } catch (e) {
        /* 错误提示已由 request 拦截器统一处理 */
      }
    },
    async handleManualCheckin (row) {
      try {
        await this.$confirm(
          '确定为「' + (row.realName || row.username) + '」补签吗？补签后该学生状态将变为已签到。',
          '补签确认',
          { type: 'warning', confirmButtonText: '补签', cancelButtonText: '取消' }
        )
      } catch (e) {
        return
      }
      try {
        await manualCheckin(row.id)
        this.$message.success('补签成功')
        this.load()
        this.loadStat()
      } catch (e) {
        /* 错误提示已由 request 拦截器统一处理 */
      }
    },
    goBack () {
      this.$router.push('/organizer/activities')
    }
  }
}
</script>

<style scoped>
.head-card {
  margin-bottom: 14px;
}
.head {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}
.head-main {
  flex: 1;
  min-width: 0;
}
.act-title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
}
.act-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
}
.stat-row {
  margin-bottom: 14px;
}
.stat-card {
  padding: 14px 16px;
}
.stat-card .num {
  font-size: 24px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--primary);
}
.stat-card .label {
  margin-top: 2px;
  font-size: 13px;
  color: var(--text-sub);
}
.stat-card .hint {
  margin-top: 2px;
  font-size: 12px;
  color: #c0c4cc;
}
.stat-absent .num {
  color: #f56c6c;
}
.stat-canceled .num {
  color: #909399;
}
.search-input {
  width: 200px;
}
.text-danger {
  color: #f56c6c;
}
</style>
