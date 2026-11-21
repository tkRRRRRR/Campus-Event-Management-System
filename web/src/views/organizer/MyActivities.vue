<template>
  <div class="page">
    <div class="page-title">我的活动</div>

    <div class="card">
      <div class="toolbar">
        <el-radio-group v-model="query.status" size="small" @change="handleFilter">
          <el-radio-button :label="''">全部</el-radio-button>
          <el-radio-button :label="0">草稿</el-radio-button>
          <el-radio-button :label="1">待审核</el-radio-button>
          <el-radio-button :label="2">报名中</el-radio-button>
          <el-radio-button :label="3">已驳回</el-radio-button>
          <el-radio-button :label="4">已取消</el-radio-button>
          <el-radio-button :label="5">已结束</el-radio-button>
        </el-radio-group>

        <div class="spacer"></div>

        <el-input
          v-model="query.keyword"
          class="search-input"
          size="small"
          clearable
          placeholder="搜索活动标题"
          prefix-icon="el-icon-search"
          @keyup.enter.native="handleFilter"
          @clear="handleFilter"
        />
        <el-button size="small" @click="handleFilter">搜索</el-button>
        <el-button size="small" type="primary" icon="el-icon-plus" @click="goCreate">发布活动</el-button>
      </div>

      <el-table class="only-desktop" v-loading="loading" :data="list" size="small" stripe empty-text="暂无活动">
        <el-table-column label="封面" width="88" align="center">
          <template slot-scope="{ row }">
            <div class="thumb">
              <img v-if="row.coverUrl" :src="resolveUrl(row.coverUrl)" :alt="row.title" />
              <span v-else>{{ (row.categoryName || '活动').slice(0, 2) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="title" label="标题" min-width="170" show-overflow-tooltip />

        <el-table-column label="分类" width="100">
          <template slot-scope="{ row }">
            <span>{{ row.categoryName || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="开始时间" width="150">
          <template slot-scope="{ row }">{{ fmtDateTime(row.startTime) }}</template>
        </el-table-column>

        <el-table-column label="报名" width="100">
          <template slot-scope="{ row }">
            <span class="join-count">{{ row.signedCount || 0 }} / {{ row.capacity || 0 }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="96">
          <template slot-scope="{ row }">
            <status-tag :status="row.status" />
          </template>
        </el-table-column>

        <el-table-column label="驳回理由" width="170">
          <template slot-scope="{ row }">
            <el-tooltip
              v-if="row.status === 3"
              :content="row.auditRemark || '暂无驳回理由'"
              placement="top"
            >
              <span class="ellipsis reason">{{ row.auditRemark || '暂无驳回理由' }}</span>
            </el-tooltip>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="236" fixed="right">
          <template slot-scope="{ row }">
            <el-button v-if="canEdit(row)" type="text" size="mini" @click="goEdit(row)">编辑</el-button>
            <el-button type="text" size="mini" @click="goRegistrations(row)">报名名单</el-button>
            <el-button type="text" size="mini" @click="goCheckin(row)">签到管理</el-button>
            <el-button v-if="canCancel(row)" type="text" size="mini" class="text-danger" @click="handleCancel(row)">取消活动</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="only-mobile m-list" v-loading="loading">
        <div class="m-card" v-for="row in list" :key="row.id">
          <div class="m-card-head">
            <div class="m-card-title">{{ row.title }}</div>
            <status-tag :status="row.status" />
          </div>
          <div class="m-card-meta">
            <div><i class="el-icon-collection-tag"></i>{{ row.categoryName || '未分类' }} · 报名 {{ row.signedCount || 0 }}/{{ row.capacity || 0 }}</div>
            <div><i class="el-icon-time"></i>{{ fmtDateTime(row.startTime) }}</div>
            <div v-if="row.status === 3" class="m-reason">
              <i class="el-icon-warning-outline"></i>驳回理由：{{ row.auditRemark || '暂无' }}
            </div>
          </div>
          <div class="m-card-actions">
            <el-button v-if="canEdit(row)" size="mini" @click="goEdit(row)">编辑</el-button>
            <el-button size="mini" @click="goRegistrations(row)">报名名单</el-button>
            <el-button size="mini" @click="goCheckin(row)">签到管理</el-button>
            <el-button v-if="canCancel(row)" size="mini" type="danger" plain @click="handleCancel(row)">取消活动</el-button>
          </div>
        </div>
        <div class="empty-box" v-if="!loading && !list.length">暂无活动</div>
      </div>

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
import { myActivities, getActivity, cancelActivity } from '@/api/activity'
import { resolveUrl } from '@/utils/request'
import { fmtDateTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

/** 兼容性更好的时间解析：'yyyy-MM-dd HH:mm:ss' 在部分浏览器里直接 new Date 会失败 */
function timestamp (value) {
  if (!value) return 0
  return new Date(String(value).replace(/-/g, '/')).getTime()
}

export default {
  name: 'MyActivities',
  components: { StatusTag },
  data () {
    return {
      loading: false,
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
  created () {
    this.load()
  },
  methods: {
    resolveUrl,
    fmtDateTime,
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

        const data = await myActivities(params)
        this.list = (data && data.records) || []
        this.total = (data && data.total) || 0
        this.enrichRejected()
      } catch (e) {
        this.list = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    /**
     * 列表接口返回的 ActivityListVO 里没有 auditRemark 字段，
     * 这里对已驳回的活动逐条取详情补上驳回理由，失败不影响列表展示。
     */
    async enrichRejected () {
      const rows = this.list.filter(row => row.status === 3 && !row.auditRemark)
      if (!rows.length) return
      await Promise.all(rows.map(async row => {
        try {
          const detail = await getActivity(row.id)
          const remark = (detail && detail.auditRemark) || ''
          this.$set(row, 'auditRemark', remark)
        } catch (e) {
          /* 单条补充失败时保持"暂无驳回理由" */
        }
      }))
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
    canEdit (row) {
      return row.status === 0 || row.status === 1 || row.status === 3
    },
    canCancel (row) {
      return row.status === 2 && timestamp(row.startTime) > Date.now()
    },
    goCreate () {
      this.$router.push('/organizer/activity/edit')
    },
    goEdit (row) {
      this.$router.push('/organizer/activity/edit/' + row.id)
    },
    goRegistrations (row) {
      this.$router.push('/organizer/registrations/' + row.id)
    },
    goCheckin (row) {
      this.$router.push('/organizer/checkin/' + row.id)
    },
    async handleCancel (row) {
      try {
        await this.$confirm(
          '确定取消活动「' + row.title + '」吗？取消后活动将不再对学生展示，并会通知已报名的学生。',
          '取消活动',
          { type: 'warning', confirmButtonText: '确定取消', cancelButtonText: '再想想' }
        )
      } catch (e) {
        return
      }
      try {
        await cancelActivity(row.id)
        this.$message.success('活动已取消')
        this.load()
      } catch (e) {
        /* 错误提示已由 request 拦截器统一处理 */
      }
    }
  }
}
</script>

<style scoped>
.search-input {
  width: 200px;
}
.thumb {
  width: 64px;
  height: 40px;
  border-radius: 4px;
  overflow: hidden;
  background: #eef1f2;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-sub);
  font-size: 12px;
}
.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.join-count {
  color: var(--primary);
}
.reason {
  display: inline-block;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
  color: #606266;
  cursor: pointer;
}
.text-danger {
  color: #f56c6c;
}
.m-reason { color: #e6a23c; }
</style>
