<template>
  <div class="page" v-loading="loading">
    <!-- 上半部分：详情信息 + 报名操作 -->
    <el-row :gutter="16" class="top-row">
      <el-col :span="17">
        <div class="card info-card">
          <div class="detail-cover">
            <img v-if="detail.coverUrl" :src="resolveUrl(detail.coverUrl)" class="cover" :alt="detail.title" />
            <div v-else class="cover-fallback">{{ detail.categoryName || '活动' }}</div>
          </div>

          <div class="detail-head">
            <h2 class="detail-title">{{ detail.title || '活动详情' }}</h2>
            <status-tag :status="detail.status" />
          </div>

          <div class="info-grid">
            <div class="info-item">
              <label>活动分类</label>
              <span>{{ detail.categoryName || '未分类' }}</span>
            </div>
            <div class="info-item">
              <label>活动时间</label>
              <span>{{ fmtDateTime(detail.startTime) }} ~ {{ fmtDateTime(detail.endTime) }}</span>
            </div>
            <div class="info-item">
              <label>报名截止</label>
              <span>{{ fmtDateTime(detail.signupDeadline) }}</span>
            </div>
            <div class="info-item">
              <label>活动地点</label>
              <span>{{ detail.location || '待定' }}</span>
            </div>
            <div class="info-item">
              <label>组织者</label>
              <span>{{ detail.organizerName || '-' }}</span>
            </div>
            <div class="info-item">
              <label>质量分</label>
              <span class="quality-score">{{ qualityText }}</span>
            </div>
            <div class="info-item rating-item">
              <label>活动评分</label>
              <span class="rating-line">
                <el-rate :value="avgScore" disabled allow-half />
                <span class="muted">{{ avgScoreText }} 分 · {{ detail.reviewCount || 0 }} 条评价</span>
              </span>
            </div>
          </div>

          <div class="progress-line">
            <span class="progress-label">报名进度</span>
            <el-progress class="progress-bar" :percentage="progressPercent" :show-text="false" :stroke-width="10" />
            <span class="progress-text">{{ signedCount }}/{{ capacity }} 人</span>
          </div>
        </div>
      </el-col>

      <el-col :span="7">
        <div class="card action-card">
          <div class="action-title">报名操作</div>

          <div class="action-state">
            <span class="muted">我的报名状态</span>
            <status-tag v-if="detail.regStatus" :status="detail.regStatus" kind="registration" />
            <el-tag v-else size="mini" type="info" effect="plain">未报名</el-tag>
          </div>

          <div class="action-rows">
            <div class="action-row">
              <span>剩余名额</span>
              <b>{{ remaining }} 个</b>
            </div>
            <div class="action-row">
              <span>报名截止</span>
              <b>{{ fmtDateTime(detail.signupDeadline) }}</b>
            </div>
            <div class="action-row" v-if="!expired && detail.signupDeadline">
              <span>截止倒计时</span>
              <b class="primary-text">{{ remainText(detail.signupDeadline) }}</b>
            </div>
          </div>

          <div class="tips">
            <div class="tip danger" v-if="expired">报名已截止</div>
            <div class="tip warning" v-if="detail.needAudit">该活动报名需组织者审核</div>
            <div class="tip warning" v-if="detail.regStatus === 5">报名审核中，请耐心等待</div>
            <div class="tip success" v-if="detail.regStatus === 2">您已签到成功</div>
            <div class="tip danger" v-if="detail.regStatus === 4">您已爽约该活动，请注意违约次数</div>
            <div class="tip" v-if="detail.regStatus === 3">您已取消报名</div>
            <div class="tip" v-if="!detail.regStatus && detail.status === 5">活动已结束</div>
          </div>

          <div class="action-btns">
            <el-button
              v-if="canRegister"
              type="primary"
              class="block-btn"
              :loading="registering"
              @click="handleRegister"
            >立即报名</el-button>
            <el-button
              v-if="detail.regStatus === 1"
              type="danger"
              plain
              class="block-btn"
              :loading="canceling"
              @click="handleCancel"
            >取消报名</el-button>
            <el-button
              v-if="canCheckin"
              type="primary"
              plain
              class="block-btn"
              @click="openCheckin"
            >输入签到码</el-button>
            <el-button
              v-if="!canRegister && !detail.regStatus"
              type="info"
              class="block-btn"
              disabled
            >{{ unavailableText }}</el-button>
          </div>

          <div class="manage-links" v-if="isManager">
            <el-divider></el-divider>
            <div class="muted manage-title">组织者操作</div>
            <div class="manage-btns">
              <el-link type="primary" :underline="false" @click="goPage('/organizer/registrations/' + detail.id)">报名名单</el-link>
              <el-link type="primary" :underline="false" @click="goPage('/organizer/checkin/' + detail.id)">签到管理</el-link>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 下半部分：详情 / 评价 -->
    <div class="card tabs-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="活动详情" name="detail">
          <div class="detail-content" v-if="detail.content">{{ detail.content }}</div>
          <div class="empty-box" v-else>暂无活动详情</div>
        </el-tab-pane>

        <el-tab-pane label="活动评价" name="review">
          <div class="review-summary">
            <div class="summary-score">
              <div class="big-score">{{ avgScoreText }}</div>
              <el-rate :value="avgScore" disabled allow-half />
              <div class="muted">{{ reviewCount }} 条评价</div>
            </div>
            <div class="summary-dist">
              <div class="dist-row" v-for="row in distribution" :key="row.star">
                <span class="dist-label">{{ row.star }} 星</span>
                <el-progress class="dist-bar" :percentage="row.percent" :show-text="false" :stroke-width="8" />
                <span class="dist-count">{{ row.count }}</span>
              </div>
            </div>
          </div>

          <div class="review-form" v-if="canReview">
            <div class="form-title">发表评价</div>
            <div class="form-row">
              <span class="form-label">评分</span>
              <el-rate
                v-model="reviewForm.score"
                show-text
                :texts="['很差', '较差', '一般', '满意', '非常满意']"
              />
              <span class="muted" v-if="!reviewForm.score">请选择 1~5 星</span>
            </div>
            <el-input
              v-model="reviewForm.content"
              type="textarea"
              :rows="3"
              maxlength="200"
              show-word-limit
              placeholder="分享你的活动体验..."
            />
            <div class="form-foot">
              <el-switch v-model="reviewForm.anonymous" active-text="匿名评价"></el-switch>
              <el-button type="primary" :loading="reviewSubmitting" @click="submitReviewForm">提交评价</el-button>
            </div>
          </div>
          <div class="my-review" v-else-if="myReview">
            <span class="muted">我的评价：</span>
            <el-rate :value="Number(myReview.score) || 0" disabled />
            <span class="my-review-content">{{ myReview.content || '未填写评价内容' }}</span>
          </div>

          <div class="review-list">
            <div class="review-item" v-for="r in reviews" :key="r.id">
              <div class="review-head">
                <el-avatar :size="30" class="review-avatar">{{ avatarText(r.userName) }}</el-avatar>
                <span class="review-name">{{ r.userName || '匿名同学' }}</span>
                <el-rate :value="Number(r.score) || 0" disabled class="review-rate" />
                <span class="review-time">{{ fmtDateTime(r.createTime) }}</span>
              </div>
              <div class="review-content">{{ r.content || '该同学没有填写评价内容' }}</div>
            </div>
            <div class="empty-box" v-if="!reviews.length">暂无评价，快来抢沙发吧</div>
            <div class="pager" v-if="reviewTotal > reviewPage.pageSize">
              <el-pagination
                small
                background
                layout="prev, pager, next"
                :total="reviewTotal"
                :page-size="reviewPage.pageSize"
                :current-page.sync="reviewPage.pageNum"
                @current-change="loadReviews"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 签到码弹窗 -->
    <el-dialog title="输入签到码" :visible.sync="checkinVisible" width="380px" @closed="checkinCode = ''">
      <div class="checkin-body">
        <p class="muted">请输入组织者提供的 6 位签到码</p>
        <el-input
          v-model="checkinCode"
          size="large"
          maxlength="6"
          placeholder="6 位数字签到码"
          @input="sanitizeCode"
          @keyup.enter.native="submitCheckin"
        ></el-input>
      </div>
      <span slot="footer">
        <el-button @click="checkinVisible = false">取消</el-button>
        <el-button type="primary" :loading="checkinLoading" @click="submitCheckin">确认签到</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import dayjs from 'dayjs'
import { getActivity } from '@/api/activity'
import { registerActivity, cancelRegistration } from '@/api/registration'
import { checkinByCode } from '@/api/checkin'
import { reviewPage, reviewStat, submitReview } from '@/api/review'
import { resolveUrl } from '@/utils/request'
import { fmtDateTime, remainText, signupExpired } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

export default {
  name: 'ActivityDetail',
  components: { StatusTag },
  data () {
    return {
      loading: false,
      detail: {},
      registering: false,
      canceling: false,
      activeTab: 'detail',
      reviewStat: {},
      reviews: [],
      reviewTotal: 0,
      reviewPage: { pageNum: 1, pageSize: 5 },
      reviewForm: { score: 0, content: '', anonymous: false },
      reviewSubmitting: false,
      checkinVisible: false,
      checkinCode: '',
      checkinLoading: false
    }
  },
  computed: {
    activityId () {
      return this.$route.params.id
    },
    capacity () {
      return Number(this.detail.capacity || 0)
    },
    signedCount () {
      if (this.detail.signedCount !== null && this.detail.signedCount !== undefined) {
        return Number(this.detail.signedCount)
      }
      return Number(this.detail.joinedCount || 0)
    },
    remaining () {
      if (this.detail.remaining !== null && this.detail.remaining !== undefined) {
        return Number(this.detail.remaining)
      }
      return Math.max(0, this.capacity - this.signedCount)
    },
    progressPercent () {
      if (!this.capacity) return 0
      return Math.min(100, Math.round((this.signedCount / this.capacity) * 100))
    },
    started () {
      if (!this.detail.startTime) return false
      return !dayjs().isBefore(dayjs(this.detail.startTime))
    },
    expired () {
      return signupExpired(this.detail.signupDeadline)
    },
    canRegister () {
      return !this.detail.regStatus && this.detail.status === 2 && this.remaining > 0 && !this.expired
    },
    canCheckin () {
      return this.started && this.detail.regStatus === 1 && this.detail.status !== 5
    },
    unavailableText () {
      if (this.detail.status === 5) return '活动已结束'
      if (this.detail.status === 2 && this.remaining <= 0) return '名额已满'
      if (this.detail.status === 2 && this.expired) return '报名已截止'
      return '暂不可报名'
    },
    avgScore () {
      const value = this.reviewStat.avgScore !== null && this.reviewStat.avgScore !== undefined
        ? this.reviewStat.avgScore
        : this.detail.avgScore
      return Number(value || 0)
    },
    avgScoreText () {
      return this.avgScore > 0 ? this.avgScore.toFixed(1) : '暂无评分'
    },
    reviewCount () {
      if (this.reviewStat.total !== null && this.reviewStat.total !== undefined) {
        return Number(this.reviewStat.total)
      }
      return Number(this.detail.reviewCount || 0)
    },
    distribution () {
      const total = Number(this.reviewStat.total || 0)
      return [5, 4, 3, 2, 1].map(star => {
        const count = Number(this.reviewStat['score' + star] || 0)
        return {
          star,
          count,
          percent: total ? Math.round((count / total) * 100) : 0
        }
      })
    },
    myReview () {
      return this.reviewStat.myReview || null
    },
    canReview () {
      return this.detail.regStatus === 2 && this.detail.status === 5 && !this.myReview
    },
    isManager () {
      return this.$store.getters.isOrganizer
    },
    qualityText () {
      const value = this.detail.qualityScore
      return value !== null && value !== undefined ? value + ' 分' : '-'
    }
  },
  created () {
    this.init()
  },
  watch: {
    '$route.params.id' () {
      this.init()
    }
  },
  methods: {
    resolveUrl,
    fmtDateTime,
    remainText,
    init () {
      this.activeTab = 'detail'
      this.loadDetail()
      this.loadStat()
      this.loadReviews()
    },
    async loadDetail (silent) {
      if (!silent) this.loading = true
      try {
        this.detail = (await getActivity(this.activityId)) || {}
      } catch (e) {
        this.detail = {}
      } finally {
        if (!silent) this.loading = false
      }
    },
    async loadStat () {
      try {
        this.reviewStat = (await reviewStat(this.activityId)) || {}
      } catch (e) {
        this.reviewStat = {}
      }
    },
    async loadReviews () {
      try {
        const data = await reviewPage(this.activityId, {
          pageNum: this.reviewPage.pageNum,
          pageSize: this.reviewPage.pageSize
        })
        this.reviews = (data && data.records) || []
        this.reviewTotal = (data && data.total) || 0
      } catch (e) {
        this.reviews = []
        this.reviewTotal = 0
      }
    },
    avatarText (name) {
      return (name || '匿').charAt(0)
    },
    async handleRegister () {
      if (this.registering) return
      this.registering = true
      try {
        await registerActivity(this.activityId)
        this.$message.success('报名成功')
        await this.loadDetail(true)
      } catch (e) {
        // 错误提示已由 request 拦截器统一处理
      } finally {
        this.registering = false
      }
    },
    async handleCancel () {
      try {
        await this.$confirm('确定取消报名吗？取消后如需参加需重新报名。', '提示', { type: 'warning' })
      } catch (e) {
        return
      }
      this.canceling = true
      try {
        await cancelRegistration(this.activityId)
        this.$message.success('已取消报名')
        await this.loadDetail(true)
      } catch (e) {
        // 错误提示已由 request 拦截器统一处理
      } finally {
        this.canceling = false
      }
    },
    openCheckin () {
      this.checkinCode = ''
      this.checkinVisible = true
    },
    sanitizeCode () {
      this.checkinCode = String(this.checkinCode || '').replace(/\D/g, '').slice(0, 6)
    },
    async submitCheckin () {
      this.sanitizeCode()
      if (!/^\d{6}$/.test(this.checkinCode)) {
        this.$message.warning('请输入 6 位数字签到码')
        return
      }
      this.checkinLoading = true
      try {
        await checkinByCode(this.activityId, this.checkinCode)
        this.checkinVisible = false
        this.$message.success('签到成功')
        await this.loadDetail(true)
      } catch (e) {
        // 错误提示已由 request 拦截器统一处理
      } finally {
        this.checkinLoading = false
      }
    },
    async submitReviewForm () {
      if (!this.reviewForm.score || this.reviewForm.score < 1) {
        this.$message.warning('请先选择 1~5 星评分')
        return
      }
      this.reviewSubmitting = true
      try {
        await submitReview(this.activityId, {
          score: this.reviewForm.score,
          content: (this.reviewForm.content || '').trim(),
          anonymous: this.reviewForm.anonymous ? 1 : 0
        })
        this.$message.success('评价提交成功')
        this.reviewForm = { score: 0, content: '', anonymous: false }
        this.reviewPage.pageNum = 1
        await this.loadStat()
        await this.loadReviews()
        await this.loadDetail(true)
      } catch (e) {
        // 错误提示已由 request 拦截器统一处理
      } finally {
        this.reviewSubmitting = false
      }
    },
    goPage (path) {
      this.$router.push(path)
    }
  }
}
</script>

<style scoped>
.top-row { margin-bottom: 16px; }
.info-card { padding: 18px; }
.detail-cover {
  height: 260px;
  border-radius: 6px;
  overflow: hidden;
  background: #eef1f2;
  margin-bottom: 16px;
}
.detail-head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 14px;
}
.detail-title {
  margin: 0;
  font-size: 20px;
  line-height: 28px;
  flex: 1;
}
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px 24px;
  margin-bottom: 16px;
}
.info-item { display: flex; font-size: 13px; line-height: 22px; }
.info-item label {
  width: 68px;
  flex-shrink: 0;
  color: #909399;
}
.info-item span { color: #303133; word-break: break-all; }
.rating-item { grid-column: 1 / 3; }
.rating-line { display: flex; align-items: center; gap: 8px; }
.rating-line >>> .el-rate { line-height: 1; }
.quality-score { color: #e6a23c; font-weight: 600; }
.progress-line {
  display: flex;
  align-items: center;
  gap: 12px;
  border-top: 1px dashed #ebeef5;
  padding-top: 14px;
}
.progress-label { font-size: 13px; color: #909399; flex-shrink: 0; }
.progress-bar { flex: 1; }
.progress-text { font-size: 13px; color: var(--primary); flex-shrink: 0; }

.action-card { position: sticky; top: 16px; }
.action-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 14px;
  padding-left: 10px;
  position: relative;
}
.action-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 3px;
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: var(--primary);
}
.action-state {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 12px;
}
.action-rows { margin-bottom: 12px; }
.action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  margin-bottom: 8px;
}
.action-row span { color: #909399; }
.primary-text { color: var(--primary); }
.tips .tip {
  font-size: 12px;
  line-height: 20px;
  padding: 2px 8px;
  border-radius: 4px;
  margin-bottom: 6px;
  background: #f4f6f8;
  color: #606266;
}
.tips .tip.warning { background: #fdf6ec; color: #e6a23c; }
.tips .tip.danger { background: #fef0f0; color: #f56c6c; }
.tips .tip.success { background: #f0f9eb; color: #67c23a; }
.action-btns { margin-top: 14px; }
.block-btn { width: 100%; margin-left: 0; margin-bottom: 10px; }
.manage-title { margin-bottom: 8px; }
.manage-btns { display: flex; gap: 18px; }

.tabs-card { padding: 8px 18px 18px; }
.detail-content {
  padding: 4px 2px;
  font-size: 14px;
  line-height: 1.9;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-word;
}

.review-summary {
  display: flex;
  gap: 40px;
  padding: 8px 0 18px;
  border-bottom: 1px dashed #ebeef5;
  margin-bottom: 16px;
}
.summary-score {
  width: 180px;
  text-align: center;
  flex-shrink: 0;
}
.big-score {
  font-size: 38px;
  line-height: 1.1;
  font-weight: 600;
  color: #e6a23c;
  margin-bottom: 4px;
}
.summary-score .el-rate { display: inline-block; }
.summary-dist { flex: 1; padding-top: 4px; }
.dist-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.dist-label { width: 40px; font-size: 12px; color: #909399; }
.dist-bar { flex: 1; }
.dist-count { width: 30px; text-align: right; font-size: 12px; color: #909399; }

.review-form {
  background: #f7faf9;
  border: 1px solid #e8f1ee;
  border-radius: 6px;
  padding: 14px 16px;
  margin-bottom: 18px;
}
.form-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 10px;
}
.form-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.form-label { font-size: 13px; color: #606266; }
.form-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}
.my-review {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f4f6f8;
  border-radius: 6px;
  padding: 10px 14px;
  margin-bottom: 18px;
  font-size: 13px;
  color: #606266;
}
.my-review .el-rate { display: inline-block; }
.my-review-content { flex: 1; word-break: break-word; }

.review-item {
  padding: 14px 2px;
  border-bottom: 1px solid #f0f2f5;
}
.review-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.review-avatar { background: var(--primary); color: #fff; font-size: 13px; }
.review-name { font-size: 13px; font-weight: 600; }
.review-rate { line-height: 1; }
.review-time { margin-left: auto; font-size: 12px; color: #c0c4cc; }
.review-content {
  font-size: 13px;
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-word;
}
.checkin-body { padding: 4px 0 8px; }
.checkin-body .el-input { margin-top: 10px; }
</style>
