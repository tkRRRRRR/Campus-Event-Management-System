<template>
  <div class="page">
    <!-- 筛选栏 -->
    <div class="card filter-card">
      <div class="toolbar">
        <el-select v-model="type" size="small" class="type-select" @change="handleSearch">
          <el-option label="全部类型" value="" />
          <el-option v-for="opt in typeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>

        <el-radio-group v-model="unreadFilter" size="small" @change="handleSearch">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="unread">未读</el-radio-button>
        </el-radio-group>

        <div class="spacer"></div>
        <el-button type="primary" plain size="small" icon="el-icon-check" @click="handleReadAll">全部标记已读</el-button>
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="card notice-card" v-loading="loading">
      <div
        class="notice-item"
        v-for="n in list"
        :key="n.id"
        :class="{ unread: n.isRead === 0 }"
        @click="openNotice(n)"
      >
        <span class="dot" :class="{ show: n.isRead === 0 }"></span>
        <div class="notice-main">
          <div class="notice-title">
            <span class="title-text">{{ n.title }}</span>
            <el-tag size="mini" :type="typeTag(n.noticeType)" effect="plain">{{ typeText(n.noticeType) }}</el-tag>
          </div>
          <div class="notice-foot">
            <span class="notice-time"><i class="el-icon-time"></i> {{ fmtDateTime(n.createTime) }}</span>
            <span class="unread-label" v-if="n.isRead === 0">未读</span>
          </div>
        </div>
        <div class="notice-actions" @click.stop>
          <el-button type="text" size="mini" class="danger-text" @click="handleDelete(n)">删除</el-button>
        </div>
      </div>

      <div class="empty-box" v-if="!loading && !list.length">暂无消息</div>

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

    <!-- 消息内容 -->
    <el-dialog :title="current.title" :visible.sync="dialogVisible" width="560px">
      <div class="notice-meta">
        <el-tag size="mini" :type="typeTag(current.noticeType)" effect="plain">{{ typeText(current.noticeType) }}</el-tag>
        <span class="muted">{{ fmtDateTime(current.createTime) }}</span>
      </div>
      <div class="notice-content">{{ current.content }}</div>
      <span slot="footer">
        <el-button type="primary" @click="dialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { myNotices, unreadCount, readNotice, readAllNotices, deleteNotice } from '@/api/notice'
import { fmtDateTime, NOTICE_TYPE_TEXT } from '@/utils/format'

export default {
  name: 'Notices',
  data () {
    return {
      type: '',
      unreadFilter: 'all',
      page: { pageNum: 1, pageSize: 10 },
      list: [],
      total: 0,
      loading: false,
      dialogVisible: false,
      current: {}
    }
  },
  computed: {
    typeOptions () {
      return Object.keys(NOTICE_TYPE_TEXT).map(key => ({
        value: key,
        label: NOTICE_TYPE_TEXT[key]
      }))
    }
  },
  created () {
    this.loadList()
    this.refreshUnread()
  },
  methods: {
    fmtDateTime,
    typeText (type) {
      return NOTICE_TYPE_TEXT[type] || '通知'
    },
    typeTag (type) {
      if (type === 'AUDIT') return 'warning'
      if (type === 'ACTIVITY') return 'success'
      return 'info'
    },
    async loadList () {
      this.loading = true
      try {
        const params = {
          pageNum: this.page.pageNum,
          pageSize: this.page.pageSize
        }
        if (this.type) params.type = this.type
        if (this.unreadFilter === 'unread') params.isRead = 0
        const data = await myNotices(params)
        this.list = (data && data.records) || []
        this.total = (data && data.total) || 0
      } catch (e) {
        this.list = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    async refreshUnread () {
      try {
        const data = await unreadCount()
        this.$store.commit('SET_UNREAD', (data && data.count) || 0)
      } catch (e) {
        // 未读数刷新失败不影响页面展示
      }
    },
    handleSearch () {
      this.page.pageNum = 1
      this.loadList()
    },
    async openNotice (n) {
      this.current = n
      this.dialogVisible = true
      if (n.isRead === 0) {
        try {
          await readNotice(n.id)
          await this.loadList()
          await this.refreshUnread()
        } catch (e) {
          // 错误提示已由 request 拦截器统一处理
        }
      }
    },
    async handleReadAll () {
      if (!this.$store.getters.unreadCount) {
        this.$message.info('当前没有未读消息')
        return
      }
      try {
        await readAllNotices()
        this.$message.success('已全部标记为已读')
        await this.loadList()
        await this.refreshUnread()
      } catch (e) {
        // 错误提示已由 request 拦截器统一处理
      }
    },
    async handleDelete (n) {
      try {
        await this.$confirm('确定删除这条消息吗？', '提示', { type: 'warning' })
      } catch (e) {
        return
      }
      try {
        await deleteNotice(n.id)
        this.$message.success('消息已删除')
        if (this.list.length === 1 && this.page.pageNum > 1) {
          this.page.pageNum -= 1
        }
        await this.loadList()
        await this.refreshUnread()
      } catch (e) {
        // 错误提示已由 request 拦截器统一处理
      }
    }
  }
}
</script>

<style scoped>
.filter-card { margin-bottom: 14px; }
.type-select { width: 140px; }
.notice-card { padding: 6px 18px; }
.notice-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 15px 4px;
  border-bottom: 1px solid #f0f2f5;
  cursor: pointer;
  transition: background 0.15s;
}
.notice-item:last-of-type { border-bottom: none; }
.notice-item:hover { background: #fafbfc; }
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 7px;
  flex-shrink: 0;
  background: transparent;
}
.dot.show { background: var(--primary); }
.notice-main { flex: 1; min-width: 0; }
.notice-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.title-text {
  font-size: 14px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.notice-item.unread .title-text { color: #303133; font-weight: 700; }
.notice-foot {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #c0c4cc;
}
.unread-label { color: var(--primary); }
.notice-actions { flex-shrink: 0; padding-top: 1px; }
.danger-text { color: #f56c6c; }
.notice-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #ebeef5;
  margin-bottom: 12px;
}
.notice-content {
  font-size: 14px;
  line-height: 1.9;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 340px;
  overflow-y: auto;
}
</style>
