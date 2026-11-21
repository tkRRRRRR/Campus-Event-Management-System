<template>
  <div class="page">
    <h2 class="page-title">公告管理</h2>

    <div class="card">
      <div class="toolbar">
        <el-select v-model="query.status" style="width: 130px" placeholder="全部状态">
          <el-option label="全部状态" value="" />
          <el-option label="已发布" :value="1" />
          <el-option label="已下架" :value="0" />
        </el-select>
        <el-input
          v-model="query.keyword"
          placeholder="搜索公告标题"
          clearable
          style="width: 230px"
          @keyup.enter.native="handleSearch" />
        <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
        <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        <div class="spacer"></div>
        <el-button type="primary" icon="el-icon-plus" @click="openCreate">新增公告</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe empty-text="暂无公告" class="only-desktop">
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="内容摘要" min-width="280">
          <template slot-scope="scope">
            <el-tooltip placement="top" :open-delay="300" :content="scope.row.content || ''">
              <span class="summary-text">{{ scope.row.content || '-' }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="150" align="center">
          <template slot-scope="scope">
            <el-switch
              v-model="scope.row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="statusSavingId === scope.row.id"
              @change="handleStatusChange(scope.row)" />
            <span class="switch-text">{{ scope.row.status === 1 ? '已发布' : '已下架' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="150">
          <template slot-scope="scope">{{ fmtDateTime(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="openEdit(scope.row)">编辑</el-button>
            <el-button type="text" size="mini" class="danger-text" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="only-mobile m-list">
        <div class="m-card" v-for="row in list" :key="row.id">
          <div class="m-card-head">
            <div class="m-card-title">{{ row.title }}</div>
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="statusSavingId === row.id"
              @change="handleStatusChange(row)" />
          </div>
          <div class="m-card-meta">发布 {{ fmtDateTime(row.createTime) }} · {{ row.status === 1 ? '已发布' : '已下架' }}</div>
          <div class="m-card-text">{{ row.content }}</div>
          <div class="m-card-actions">
            <el-button size="mini" @click="openEdit(row)">编辑</el-button>
            <el-button size="mini" type="danger" plain @click="handleDelete(row)">删除</el-button>
          </div>
        </div>
        <div class="empty-box" v-if="!loading && !list.length">暂无公告</div>
      </div>


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

    <el-dialog
      :title="form.id ? '编辑公告' : '新增公告'"
      :visible.sync="dialogVisible"
      width="640px"
      @closed="resetForm">
      <el-form ref="form" :model="form" :rules="rules" label-width="70px">
        <el-form-item label="标题" prop="title">
          <el-input v-model.trim="form.title" maxlength="50" show-word-limit placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            maxlength="2000"
            show-word-limit
            placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">发布</el-radio>
            <el-radio :label="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { announcementPage, createAnnouncement, updateAnnouncement, changeAnnouncementStatus, deleteAnnouncement } from '@/api/announcement'
import { fmtDateTime } from '@/utils/format'

export default {
  name: 'AnnouncementManage',
  data () {
    return {
      loading: false,
      list: [],
      total: 0,
      statusSavingId: null,
      dialogVisible: false,
      submitting: false,
      query: {
        pageNum: 1,
        pageSize: 10,
        status: '',
        keyword: ''
      },
      form: {
        id: null,
        title: '',
        content: '',
        status: 1
      },
      rules: {
        title: [
          { required: true, message: '请输入公告标题', trigger: 'blur' },
          { max: 50, message: '标题不能超过 50 个字符', trigger: 'blur' }
        ],
        content: [
          { required: true, message: '请输入公告内容', trigger: 'blur' },
          { max: 2000, message: '内容不能超过 2000 个字符', trigger: 'blur' }
        ]
      }
    }
  },
  created () {
    this.loadList()
  },
  methods: {
    fmtDateTime,
    buildParams () {
      const params = {
        pageNum: this.query.pageNum,
        pageSize: this.query.pageSize
      }
      const keyword = (this.query.keyword || '').trim()
      if (keyword) params.keyword = keyword
      if (this.query.status === 0 || this.query.status === 1) params.status = this.query.status
      return params
    },
    async loadList () {
      this.loading = true
      try {
        const data = await announcementPage(this.buildParams())
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
    handleSearch () {
      this.query.pageNum = 1
      this.loadList()
    },
    handleReset () {
      this.query.pageNum = 1
      this.query.status = ''
      this.query.keyword = ''
      this.loadList()
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
    openCreate () {
      this.resetForm()
      this.dialogVisible = true
    },
    openEdit (row) {
      this.form = {
        id: row.id,
        title: row.title || '',
        content: row.content || '',
        status: row.status == null ? 1 : row.status
      }
      this.dialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.form) this.$refs.form.clearValidate()
      })
    },
    resetForm () {
      this.form = {
        id: null,
        title: '',
        content: '',
        status: 1
      }
      this.$nextTick(() => {
        if (this.$refs.form) this.$refs.form.clearValidate()
      })
    },
    submitForm () {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        this.submitting = true
        const payload = {
          title: this.form.title,
          content: this.form.content,
          status: this.form.status
        }
        try {
          if (this.form.id) {
            await updateAnnouncement(this.form.id, payload)
            this.$message.success('公告已更新')
          } else {
            await createAnnouncement(payload)
            this.$message.success('公告已新增')
          }
          this.dialogVisible = false
          this.loadList()
        } catch (e) {
          // 错误提示由 request 拦截器统一处理
        } finally {
          this.submitting = false
        }
      })
    },
    async handleStatusChange (row) {
      const next = row.status
      this.statusSavingId = row.id
      try {
        await changeAnnouncementStatus(row.id, next)
        this.$message.success(next === 1 ? '公告已发布' : '公告已下架')
        // 若当前筛选了状态，刷新后该行应按筛选条件移除
        this.loadList()
      } catch (e) {
        row.status = next === 1 ? 0 : 1
      } finally {
        this.statusSavingId = null
      }
    },
    async handleDelete (row) {
      try {
        await this.$confirm('确认删除公告「' + row.title + '」吗？删除后不可恢复。', '删除公告', {
          type: 'warning',
          confirmButtonText: '确认删除',
          cancelButtonText: '取消'
        })
      } catch (e) {
        return
      }
      try {
        await deleteAnnouncement(row.id)
        this.$message.success('公告已删除')
        this.loadList()
      } catch (e) {
        // 错误提示由 request 拦截器统一处理
      }
    }
  }
}
</script>

<style scoped>
.summary-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
  color: #606266;
}
.switch-text {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}
.danger-text {
  color: #f56c6c;
}
.m-card-text { font-size: 12px; color: #909399; line-height: 1.5; margin: 4px 0 2px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
</style>
