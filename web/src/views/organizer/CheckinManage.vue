<template>
  <div class="page">
    <div class="page-title">签到管理</div>

    <div class="card head-card">
      <div class="head">
        <div class="head-main">
          <h3 class="act-title">{{ activity.title || '加载中…' }}</h3>
          <div class="muted act-meta">
            <span><i class="el-icon-time"></i> {{ fmtDateTime(activity.startTime) }} ~ {{ fmtDateTime(activity.endTime) }}</span>
            <span><i class="el-icon-location-outline"></i> {{ activity.location || '地点待定' }}</span>
            <status-tag v-if="activity.status !== undefined" :status="activity.status" />
          </div>
        </div>
        <el-button size="small" icon="el-icon-back" @click="goBack">返回</el-button>
      </div>
    </div>

    <el-row :gutter="14" class="mid-row">
      <el-col :span="12">
        <div class="card code-card">
          <div class="card-title">
            <span>签到码</span>
            <span class="muted" v-if="activity.status !== undefined && activity.status !== 2">仅进行中的活动可生成签到码</span>
          </div>

          <template v-if="hasCode">
            <div class="code" :class="{ expired: codeExpired }">{{ codeData.code }}</div>
            <div class="countdown" v-if="!codeExpired">
              <i class="el-icon-time"></i> 剩余有效时间 <b>{{ countdownText }}</b>
            </div>
            <div class="countdown expired-text" v-else>签到码已失效，请重新获取</div>
            <div class="code-actions">
              <el-button size="small" icon="el-icon-document-copy" @click="copyCode">复制签到码</el-button>
              <el-button
                size="small"
                type="primary"
                icon="el-icon-refresh"
                :loading="generating"
                :disabled="activity.status !== undefined && activity.status !== 2"
                @click="handleGenerate"
              >重新获取</el-button>
            </div>
          </template>

          <template v-else>
            <div class="code-empty">
              <p class="muted">还没有有效签到码。生成后请展示给学生，学生输入签到码即可签到。</p>
              <el-button
                size="small"
                type="primary"
                icon="el-icon-refresh"
                :loading="generating"
                :disabled="activity.status !== undefined && activity.status !== 2"
                @click="handleGenerate"
              >生成签到码</el-button>
            </div>
          </template>

          <div class="muted tip">
            签到码在活动开始前 30 分钟至活动结束之间可生成，有效期最长 30 分钟，重复生成会复用未过期的码。
          </div>
        </div>
      </el-col>

      <el-col :span="12">
        <div class="card">
          <div class="card-title"><span>签到统计</span></div>
          <div class="stat-grid">
            <div class="stat-item">
              <div class="num">{{ stat.signedCount || 0 }}</div>
              <div class="label">应到</div>
            </div>
            <div class="stat-item">
              <div class="num checked">{{ stat.checkedCount || 0 }}</div>
              <div class="label">已签到</div>
            </div>
            <div class="stat-item">
              <div class="num waiting">{{ stat.waitingCount || 0 }}</div>
              <div class="label">未签到</div>
            </div>
            <div class="stat-item">
              <div class="num absent">{{ stat.absentCount || 0 }}</div>
              <div class="label">爽约</div>
            </div>
            <div class="stat-item">
              <div class="num">{{ checkinRateText }}%</div>
              <div class="label">签到率</div>
            </div>
          </div>
          <div class="muted tip">
            签到率 = 已签到 /（已签到 + 未签到 + 爽约），待审核与已取消的报名不计入分母。
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="card">
      <div class="card-title"><span>签到名单</span></div>

      <div class="toolbar">
        <el-select v-model="query.status" size="small" style="width: 150px" @change="handleFilter">
          <el-option label="全部状态" value="" />
          <el-option label="未签到（已报名）" :value="1" />
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

      <el-table v-loading="loading" :data="list" size="small" stripe empty-text="暂无签到记录">
        <el-table-column label="姓名" width="110">
          <template slot-scope="{ row }">{{ row.realName || '-' }}</template>
        </el-table-column>
        <el-table-column label="学号" width="140">
          <template slot-scope="{ row }">{{ row.studentNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template slot-scope="{ row }">
            <status-tag :status="row.status" kind="registration" />
          </template>
        </el-table-column>
        <el-table-column label="签到时间" width="160">
          <template slot-scope="{ row }">{{ fmtDateTime(row.checkinTime) }}</template>
        </el-table-column>
        <el-table-column label="签到方式" width="110">
          <template slot-scope="{ row }">{{ checkinTypeText(row.checkinType) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="100">
          <template slot-scope="{ row }">
            <el-button
              v-if="row.status === 1"
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

      <div class="finish-bar">
        <span class="muted">提前结束活动后，系统会把未签到的报名者标记为爽约，并累加违约次数。</span>
        <el-button
          type="danger"
          size="small"
          :loading="finishing"
          :disabled="activity.status !== 2"
          @click="handleFinish"
        >结束活动并判定爽约</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { getActivity } from '@/api/activity'
import {
  currentCode,
  generateCode,
  checkinStat,
  checkinList,
  manualCheckin,
  finishActivity
} from '@/api/checkin'
import { fmtDateTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

export default {
  name: 'CheckinManage',
  components: { StatusTag },
  data () {
    return {
      loading: false,
      generating: false,
      finishing: false,
      activity: {},
      stat: {},
      codeData: {},
      remainSeconds: 0,
      timer: null,
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
    hasCode () {
      return !!(this.codeData && this.codeData.code)
    },
    codeExpired () {
      return !(this.codeData && this.codeData.valid && this.remainSeconds > 0)
    },
    countdownText () {
      const total = Math.max(0, this.remainSeconds)
      const hours = Math.floor(total / 3600)
      const minutes = Math.floor((total % 3600) / 60)
      const seconds = total % 60
      const pad = n => (n < 10 ? '0' + n : '' + n)
      if (hours > 0) return hours + ':' + pad(minutes) + ':' + pad(seconds)
      return pad(minutes) + ':' + pad(seconds)
    },
    checkinRateText () {
      const rate = this.stat && this.stat.checkinRate
      if (rate === undefined || rate === null) return '0.00'
      const num = Number(rate)
      return isNaN(num) ? '0.00' : num.toFixed(2)
    }
  },
  created () {
    this.loadActivity()
    this.loadStat()
    this.loadList()
    this.loadCurrentCode()
  },
  beforeDestroy () {
    this.stopTimer()
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
    async loadCurrentCode () {
      try {
        const data = await currentCode(this.activityId)
        this.applyCode(data)
      } catch (e) {
        /* 查询失败时保持在"未生成"状态 */
      }
    },
    applyCode (data) {
      this.stopTimer()
      this.codeData = data || {}
      this.remainSeconds = (data && data.remainSeconds) || 0
      if (data && data.valid && this.remainSeconds > 0) {
        this.startTimer()
      }
    },
    startTimer () {
      this.stopTimer()
      this.timer = setInterval(() => {
        if (this.remainSeconds <= 1) {
          this.remainSeconds = 0
          this.codeData.valid = false
          this.stopTimer()
          this.$message.warning('签到码已失效，请重新获取')
        } else {
          this.remainSeconds -= 1
        }
      }, 1000)
    },
    stopTimer () {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    },
    async handleGenerate () {
      if (this.generating) return
      this.generating = true
      try {
        const data = await generateCode(this.activityId)
        this.applyCode(data)
        this.$message.success('签到码已生成')
      } catch (e) {
        /* 错误提示已由 request 拦截器统一处理 */
      } finally {
        this.generating = false
      }
    },
    async copyCode () {
      const code = (this.codeData && this.codeData.code) || ''
      if (!code) return
      try {
        if (navigator.clipboard && navigator.clipboard.writeText) {
          await navigator.clipboard.writeText(code)
        } else {
          const textarea = document.createElement('textarea')
          textarea.value = code
          textarea.style.position = 'fixed'
          textarea.style.opacity = '0'
          document.body.appendChild(textarea)
          textarea.select()
          document.execCommand('copy')
          document.body.removeChild(textarea)
        }
        this.$message.success('签到码已复制')
      } catch (e) {
        this.$message.warning('复制失败，请手动选择签到码复制')
      }
    },
    async loadList () {
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

        const data = await checkinList(this.activityId, params)
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
      this.loadActivity()
      this.loadStat()
      this.loadList()
    },
    handleFilter () {
      this.query.pageNum = 1
      this.loadList()
    },
    handlePageChange (page) {
      this.query.pageNum = page
      this.loadList()
    },
    handleSizeChange (size) {
      this.query.pageSize = size
      this.query.pageNum = 1
      this.loadList()
    },
    async handleManualCheckin (row) {
      try {
        await this.$confirm(
          '确定为「' + (row.realName || row.username || '该学生') + '」补签吗？补签后状态将变为已签到。',
          '补签确认',
          { type: 'warning', confirmButtonText: '补签', cancelButtonText: '取消' }
        )
      } catch (e) {
        return
      }
      try {
        await manualCheckin(row.id)
        this.$message.success('补签成功')
        this.loadStat()
        this.loadList()
      } catch (e) {
        /* 错误提示已由 request 拦截器统一处理 */
      }
    },
    async handleFinish () {
      try {
        await this.$confirm(
          '确定结束活动吗？未签到的报名者将被记为爽约，请确认签到工作已经完成。',
          '结束活动',
          { type: 'warning', confirmButtonText: '确定结束', cancelButtonText: '取消' }
        )
      } catch (e) {
        return
      }
      this.finishing = true
      try {
        const absent = await finishActivity(this.activityId)
        this.$message.success(
          absent > 0 ? `活动已结束，本次爽约 ${absent} 人，已累计其违约次数` : '活动已结束，所有报名者均已签到'
        )
        this.stopTimer()
        this.codeData = {}
        this.remainSeconds = 0
        await Promise.all([this.loadActivity(), this.loadStat(), this.loadList()])
      } catch (e) {
        /* 错误提示已由 request 拦截器统一处理 */
      } finally {
        this.finishing = false
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
.mid-row {
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
.code-card {
  min-height: 250px;
}
.code {
  font-family: Consolas, "Courier New", monospace;
  font-size: 46px;
  font-weight: 700;
  letter-spacing: 10px;
  line-height: 1.3;
  color: var(--primary);
  text-align: center;
  padding: 10px 0 4px;
}
.code.expired {
  color: #c0c4cc;
  text-decoration: line-through;
}
.countdown {
  text-align: center;
  font-size: 13px;
  color: #606266;
}
.countdown b {
  color: var(--primary);
  font-size: 15px;
}
.expired-text {
  color: #f56c6c;
}
.code-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 16px;
}
.code-empty {
  padding: 30px 0 10px;
  text-align: center;
}
.code-empty p {
  margin: 0 0 14px;
}
.tip {
  margin-top: 14px;
  font-size: 12px;
  line-height: 1.6;
}
.stat-grid {
  display: flex;
  flex-wrap: wrap;
}
.stat-item {
  width: 33.33%;
  padding: 10px 0;
  text-align: center;
}
.stat-item .num {
  font-size: 26px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--primary);
}
.stat-item .num.checked {
  color: #67c23a;
}
.stat-item .num.waiting {
  color: #e6a23c;
}
.stat-item .num.absent {
  color: #f56c6c;
}
.stat-item .label {
  margin-top: 2px;
  font-size: 13px;
  color: var(--text-sub);
}
.search-input {
  width: 180px;
}
.finish-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #f0f2f3;
}
</style>
