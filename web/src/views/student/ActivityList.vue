<template>
  <div class="page">
    <!-- 筛选栏 -->
    <div class="card filter-card">
      <div class="toolbar">
        <el-select v-model="query.categoryId" size="small" class="category-select" placeholder="全部分类" @change="handleSearch">
          <el-option label="全部分类" value="" />
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>

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

        <el-radio-group v-model="query.sort" size="small" @change="handleSearch">
          <el-radio-button label="latest">最新发布</el-radio-button>
          <el-radio-button label="hot">最热门</el-radio-button>
        </el-radio-group>

        <el-button size="small" icon="el-icon-refresh" @click="handleReset">重置</el-button>

        <div class="spacer"></div>
        <span class="muted">共 {{ total }} 个活动</span>
      </div>
    </div>

    <!-- 活动网格 -->
    <div class="card list-card" v-loading="loading">
      <el-row :gutter="16">
        <el-col :span="8" v-for="item in list" :key="item.id" class="grid-col">
          <div class="activity-card" @click="openDetail(item.id)">
            <div class="cover-box">
              <img v-if="item.coverUrl" :src="resolveUrl(item.coverUrl)" class="cover" :alt="item.title" />
              <div v-else class="cover-fallback">{{ item.categoryName || '活动' }}</div>
            </div>
            <div class="card-body">
              <h4 :title="item.title">{{ item.title }}</h4>
              <div class="meta"><i class="el-icon-time"></i> {{ fmtDateTime(item.startTime) }}</div>
              <div class="meta"><i class="el-icon-location-outline"></i> {{ item.location || '待定' }}</div>
              <div class="foot">
                <span class="join">已报名 {{ item.signedCount || 0 }}/{{ item.capacity || 0 }} 人 · 剩 {{ remainingOf(item) }}</span>
                <status-tag :status="item.status" />
              </div>
            </div>
          </div>
        </el-col>
      </el-row>

      <div class="empty-box" v-if="!loading && !list.length">暂无符合条件的活动</div>

      <div class="pager" v-if="total > 0">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :page-size="query.pageSize"
          :current-page.sync="query.pageNum"
          @current-change="loadList"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { pageActivities } from '@/api/activity'
import { listCategories } from '@/api/category'
import { resolveUrl } from '@/utils/request'
import { fmtDateTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

export default {
  name: 'ActivityList',
  components: { StatusTag },
  data () {
    return {
      categories: [],
      keyword: '',
      query: {
        categoryId: '',
        sort: 'latest',
        pageNum: 1,
        pageSize: 9
      },
      list: [],
      total: 0,
      loading: false
    }
  },
  created () {
    this.loadCategories()
    this.loadList()
  },
  methods: {
    resolveUrl,
    fmtDateTime,
    remainingOf (item) {
      if (item.remaining !== null && item.remaining !== undefined) return item.remaining
      return Math.max(0, Number(item.capacity || 0) - Number(item.signedCount || 0))
    },
    buildParams () {
      const params = {
        pageNum: this.query.pageNum,
        pageSize: this.query.pageSize,
        sort: this.query.sort
      }
      if (this.query.categoryId !== '' && this.query.categoryId !== null) {
        params.categoryId = this.query.categoryId
      }
      const kw = (this.keyword || '').trim()
      if (kw) params.keyword = kw
      return params
    },
    async loadCategories () {
      try {
        this.categories = (await listCategories()) || []
      } catch (e) {
        this.categories = []
      }
    },
    async loadList () {
      this.loading = true
      try {
        const data = await pageActivities(this.buildParams())
        this.list = (data && data.records) || []
        this.total = (data && data.total) || 0
      } catch (e) {
        this.list = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    handleSearch () {
      this.query.pageNum = 1
      this.loadList()
    },
    handleReset () {
      this.keyword = ''
      this.query.categoryId = ''
      this.query.sort = 'latest'
      this.query.pageNum = 1
      this.loadList()
    },
    openDetail (id) {
      this.$router.push(`/activity/${id}`)
    }
  }
}
</script>

<style scoped>
.filter-card { margin-bottom: 14px; }
.category-select { width: 150px; }
.kw-input { width: 260px; }
.list-card { min-height: 320px; }
.grid-col { margin-bottom: 16px; }
.activity-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}
.activity-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}
.cover-box { position: relative; height: 150px; background: #eef1f2; overflow: hidden; }
.card-body { padding: 12px 14px 14px; }
.card-body h4 {
  margin: 0 0 8px;
  font-size: 14px;
  height: 40px;
  line-height: 20px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.meta { font-size: 12px; color: #909399; margin-bottom: 4px; }
.foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  min-height: 22px;
}
.join { font-size: 12px; color: var(--primary); }
</style>
