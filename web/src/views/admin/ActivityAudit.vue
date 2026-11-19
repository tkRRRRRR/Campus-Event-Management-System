<template>
  <div class="page">
    <h2 class="page-title">活动审核</h2>

    <div class="card">
      <el-alert
        class="audit-tip"
        type="info"
        :closable="false"
        show-icon
        :title="'共 ' + total + ' 个活动待审核'"
        description="审核通过后活动进入报名流程；驳回时请填写具体理由，便于组织者修改后重新提交。" />

      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        empty-text="暂无待审核活动">
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column label="分类" width="110">
          <template slot-scope="scope">{{ scope.row.categoryName || '未分类' }}</template>
        </el-table-column>
        <el-table-column label="组织者" width="110">
          <template slot-scope="scope">{{ scope.row.organizerName || '-' }}</template>
        </el-table-column>
        <el-table-column label="开始时间" width="150">
          <template slot-scope="scope">{{ fmtDateTime(scope.row.startTime) }}</template>
        </el-table-column>
        <el-table-column label="名额" width="120" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.capacity }}</span>
            <span class="muted"> / 已报 {{ scope.row.signedCount || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="150">
          <template slot-scope="scope">{{ fmtDateTime(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="openDetail(scope.row)">查看详情</el-button>
            <el-button type="text" size="mini" class="pass-text" @click="confirmPass(scope.row)">通过</el-button>
            <el-button type="text" size="mini" class="danger-text" @click="openReject(scope.row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager" v-if="total > 0">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          :page-sizes="[10, 20, 50]"
          :page-size="query.pageSize"
          :current-page="query.pageNum"
          @size-change="handleSizeChange"
          @current-change="handlePageChange" />
      </div>
    </div>

    <!-- 活动详情 -->
    <el-dialog title="活动详情" :visible.sync="detailVisible" width="720px">
      <div v-loading="detailLoading" class="activity-detail">
        <img v-if="detail.coverUrl" class="detail-cover" :src="resolveUrl(detail.coverUrl)" :alt="detail.title" />
        <div v-else class="detail-cover-fallback">{{ detail.categoryName || '活动' }}</div>

        <h3 class="detail-title">{{ detail.title }}</h3>
        <div class="detail-tags">
          <el-tag v-if="detail.categoryName" size="mini" effect="plain">{{ detail.categoryName }}</el-tag>
          <status-tag :status="detail.status" />
          <el-tag v-if="detail.needAudit === 1" size="mini" type="warning" effect="plain">报名需审核</el-tag>
          <el-tag v-else size="mini" type="info" effect="plain">报名无需审核</el-tag>
        </div>

        <el-descriptions :column="2" size="small" border class="detail-desc">
          <el-descriptions-item label="组织者">{{ detail.organizerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地点">{{ detail.location || '待定' }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ fmtDateTime(detail.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ fmtDateTime(detail.endTime) }}</el-descriptions-item>
          <el-descriptions-item label="报名截止">{{ fmtDateTime(detail.signupDeadline) }}</el-descriptions-item>
          <el-descriptions-item label="名额">{{ detail.capacity || 0 }} 人（已报名 {{ detail.signedCount || 0 }} 人）</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ fmtDateTime(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="审核意见">{{ detail.auditRemark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-label">活动简介</div>
        <p class="detail-text">{{ detail.summary || '暂无简介' }}</p>
        <div class="detail-label">活动内容</div>
        <div class="detail-text detail-content">{{ detail.content || '暂无内容' }}</div>
      </div>
      <span slot="footer">
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="danger" plain :disabled="detail.status !== 1" @click="openReject(detail)">驳回</el-button>
        <el-button type="primary" :disabled="detail.status !== 1" @click="confirmPass(detail)">通过</el-button>
      </span>
    </el-dialog>

    <!-- 驳回理由 -->
    <el-dialog title="驳回活动" :visible.sync="rejectVisible" width="480px">
      <el-form ref="rejectForm" :model="rejectForm" :rules="rejectRules" label-width="80px">
        <el-form-item label="活动">
          <span class="reject-title">{{ rejectRow && rejectRow.title }}</span>
        </el-form-item>
        <el-form-item label="驳回理由" prop="reason">
          <el-input
            v-model.trim="rejectForm.reason"
            type="textarea"
            :rows="4"
            maxlength="100"
            show-word-limit
            placeholder="请填写驳回理由，组织者可据此修改后重新提交" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejectSubmitting" @click="submitReject">确认驳回</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { auditPage, auditActivity, getActivity } from '@/api/activity'
import { resolveUrl } from '@/utils/request'
import { fmtDateTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

export default {
  name: 'ActivityAudit',
  components: { StatusTag },
  data () {
    return {
      loading: false,
      list: [],
      total: 0,
      query: {
        pageNum: 1,
        pageSize: 10
      },
      detailVisible: false,
      detailLoading: false,
      detail: {},
      rejectVisible: false,
      rejectSubmitting: false,
      rejectRow: null,
      rejectForm: {
        reason: ''
      },
      rejectRules: {
        reason: [
          { required: true, message: '请填写驳回理由', trigger: 'blur' },
          { max: 100, message: '驳回理由不能超过 100 字', trigger: 'blur' }
        ]
      }
    }
  },
  created () {
    this.loadList()
  },
  methods: {
    resolveUrl,
    fmtDateTime,
    async loadList () {
      this.loading = true
      try {
        const data = await auditPage({
          pageNum: this.query.pageNum,
          pageSize: this.query.pageSize
        })
        this.list = (data && data.records) || []
        this.total = Number((data && data.total) || 0)
        // 当前页已无数据时回退一页
        if (!this.list.length && this.total > 0 && this.query.pageNum > 1) {
          this.query.pageNum -= 1
          await this.loadList()
          return
        }
      } catch (e) {
        this.list = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    handleSizeChange (size) {
      this.query.pageSize = size
      this.query.pageNum = 1
      this.loadList()
    },
    handlePageChange (page) {
      if (page === this.query.pageNum) return
      this.query.pageNum = page
      this.loadList()
    },
    async openDetail (row) {
      this.detail = { title: row.title }
      this.detailVisible = true
      this.detailLoading = true
      try {
        this.detail = (await getActivity(row.id)) || {}
      } catch (e) {
        this.detailVisible = false
      } finally {
        this.detailLoading = false
      }
    },
    async confirmPass (row) {
      if (!row || !row.id) return
      try {
        await this.$confirm('确认通过活动「' + row.title + '」的审核吗？通过后活动将进入报名流程。', '审核确认', {
          type: 'warning',
          confirmButtonText: '确认通过',
          cancelButtonText: '取消'
        })
      } catch (e) {
        return
      }
      try {
        await auditActivity(row.id, { pass: true })
        this.$message.success('活动已通过审核')
        this.detailVisible = false
        this.loadList()
      } catch (e) {
        // 错误提示由 request 拦截器统一处理
      }
    },
    openReject (row) {
      if (!row || !row.id) return
      this.rejectRow = row
      this.rejectForm.reason = ''
      this.rejectVisible = true
      this.$nextTick(() => {
        if (this.$refs.rejectForm) this.$refs.rejectForm.clearValidate()
      })
    },
    submitReject () {
      if (!this.rejectRow) return
      this.$refs.rejectForm.validate(async valid => {
        if (!valid) return
        this.rejectSubmitting = true
        try {
          // 接口文档（doc/02-接口文档.md）字段为 reason，
          // 后端 ActivityAuditDTO 实际接收 remark；两个字段都传以兼容文档与实现，
          // 多余字段会被 Jackson 忽略（后端未开启 FAIL_ON_UNKNOWN_PROPERTIES）
          await auditActivity(this.rejectRow.id, {
            pass: false,
            reason: this.rejectForm.reason,
            remark: this.rejectForm.reason
          })
          this.$message.success('活动已驳回')
          this.rejectVisible = false
          this.detailVisible = false
          this.loadList()
        } catch (e) {
          // 错误提示由 request 拦截器统一处理
        } finally {
          this.rejectSubmitting = false
        }
      })
    }
  }
}
</script>

<style scoped>
.audit-tip {
  margin-bottom: 14px;
}
.pass-text {
  color: #67c23a;
}
.danger-text {
  color: #f56c6c;
}
.activity-detail {
  min-height: 120px;
}
.detail-cover {
  width: 100%;
  max-height: 220px;
  object-fit: cover;
  border-radius: 6px;
  display: block;
}
.detail-cover-fallback {
  width: 100%;
  height: 120px;
  border-radius: 6px;
  background: #f0f4f3;
  color: #9aa7a3;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
}
.detail-title {
  margin: 14px 0 8px;
  font-size: 18px;
  font-weight: 600;
}
.detail-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}
.detail-desc {
  margin-bottom: 14px;
}
.detail-label {
  font-size: 14px;
  font-weight: 600;
  margin: 12px 0 6px;
}
.detail-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-word;
}
.detail-content {
  max-height: 200px;
  overflow-y: auto;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 6px;
}
.reject-title {
  color: #303133;
}
</style>
