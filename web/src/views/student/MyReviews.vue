<template>
  <div class="page">
    <div class="card">
      <div class="toolbar">
        <span class="card-title">我的评价</span>
        <div class="spacer"></div>
        <span class="muted">共 {{ total }} 条评价</span>
      </div>

      <el-table :data="list" v-loading="loading" stripe empty-text="还没有评价过任何活动">
        <el-table-column label="活动名称" min-width="240">
          <template slot-scope="scope">
            <span class="act-link" @click="goActivity(scope.row)">{{ scope.row.activityTitle }}</span>
          </template>
        </el-table-column>

        <el-table-column label="评分" width="160">
          <template slot-scope="scope">
            <el-rate :value="Number(scope.row.score) || 0" disabled show-score text-color="#e6a23c" score-template="{value} 星"></el-rate>
          </template>
        </el-table-column>

        <el-table-column label="评价内容" min-width="280">
          <template slot-scope="scope">
            <span class="review-text">{{ scope.row.content || '未填写评价内容' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="评价时间" width="160">
          <template slot-scope="scope">{{ fmtDateTime(scope.row.createTime) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="90" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="mini" class="danger-text" @click="handleDelete(scope.row)">删除</el-button>
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
import { myReviews, deleteReview } from '@/api/review'
import { fmtDateTime } from '@/utils/format'

export default {
  name: 'MyReviews',
  data () {
    return {
      list: [],
      total: 0,
      loading: false,
      page: { pageNum: 1, pageSize: 10 }
    }
  },
  created () {
    this.loadList()
  },
  methods: {
    fmtDateTime,
    async loadList () {
      this.loading = true
      try {
        const data = await myReviews({
          pageNum: this.page.pageNum,
          pageSize: this.page.pageSize
        })
        this.list = (data && data.records) || []
        this.total = (data && data.total) || 0
      } catch (e) {
        this.list = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    goActivity (row) {
      if (row.activityId) this.$router.push(`/activity/${row.activityId}`)
    },
    async handleDelete (row) {
      try {
        await this.$confirm('确定删除这条评价吗？删除后不可恢复。', '提示', { type: 'warning' })
      } catch (e) {
        return
      }
      try {
        await deleteReview(row.id)
        this.$message.success('评价已删除')
        if (this.list.length === 1 && this.page.pageNum > 1) {
          this.page.pageNum -= 1
        }
        await this.loadList()
      } catch (e) {
        // 错误提示已由 request 拦截器统一处理
      }
    }
  }
}
</script>

<style scoped>
.card-title {
  font-size: 16px;
  font-weight: 600;
  position: relative;
  padding-left: 10px;
}
.card-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 3px;
  width: 3px;
  height: 15px;
  border-radius: 2px;
  background: var(--primary);
}
.act-link { color: var(--primary); cursor: pointer; }
.act-link:hover { text-decoration: underline; }
.review-text {
  display: block;
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.danger-text { color: #f56c6c; }
</style>
