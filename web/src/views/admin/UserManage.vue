<template>
  <div class="page">
    <h2 class="page-title">用户管理</h2>

    <div class="card">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="姓名 / 学号 / 手机号"
          clearable
          style="width: 230px"
          @keyup.enter.native="handleSearch" />
        <el-select v-model="query.role" style="width: 140px" placeholder="全部角色">
          <el-option label="全部角色" value="" />
          <el-option label="学生" value="STUDENT" />
          <el-option label="组织者" value="ORGANIZER" />
          <el-option label="管理员" value="ADMIN" />
        </el-select>
        <el-select v-model="query.status" style="width: 140px" placeholder="全部状态">
          <el-option label="全部状态" value="" />
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
        <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        <div class="spacer"></div>
        <span class="muted">共 {{ total }} 个用户</span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe empty-text="暂无用户" class="only-desktop">
        <el-table-column label="头像" width="70" align="center">
          <template slot-scope="scope">
            <el-avatar :size="34" :src="resolveUrl(scope.row.avatar)">
              <i class="el-icon-user"></i>
            </el-avatar>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" width="110" show-overflow-tooltip />
        <el-table-column label="姓名" width="90">
          <template slot-scope="scope">{{ scope.row.realName || '-' }}</template>
        </el-table-column>
        <el-table-column label="学号" width="110">
          <template slot-scope="scope">{{ scope.row.studentNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="手机号" width="125">
          <template slot-scope="scope">{{ scope.row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="95" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" :type="roleTagType(scope.row.role)" effect="light">{{ roleText(scope.row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="95" align="center">
          <template slot-scope="scope">
            <el-switch
              v-model="scope.row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="isSelf(scope.row) || statusSavingId === scope.row.id"
              @change="handleStatusChange(scope.row)" />
          </template>
        </el-table-column>
        <el-table-column label="违约次数" width="90" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.violationCount > 0" size="mini" type="danger" effect="plain">{{ scope.row.violationCount }} 次</el-tag>
            <span v-else class="muted">0</span>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="150">
          <template slot-scope="scope">{{ fmtDateTime(scope.row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="175" fixed="right" align="center">
          <template slot-scope="scope">
            <el-dropdown
              v-if="!isSelf(scope.row)"
              trigger="click"
              @command="handleRoleCommand(scope.row, $event)">
              <el-button type="text" size="mini">改角色<i class="el-icon-arrow-down el-icon--right"></i></el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="STUDENT" :disabled="scope.row.role === 'STUDENT'">设为学生</el-dropdown-item>
                <el-dropdown-item command="ORGANIZER" :disabled="scope.row.role === 'ORGANIZER'">设为组织者</el-dropdown-item>
                <el-dropdown-item command="ADMIN" :disabled="scope.row.role === 'ADMIN'">设为管理员</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
            <span v-else class="muted">本人</span>
            <el-button type="text" size="mini" class="reset-text" @click="handleResetPassword(scope.row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="only-mobile m-list">
        <div class="m-card" v-for="row in list" :key="row.id">
          <div class="m-card-head">
            <div class="m-card-title">
              {{ row.realName || row.username }}
              <el-tag size="mini" :type="roleTagType(row.role)" effect="light">{{ roleText(row.role) }}</el-tag>
            </div>
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="isSelf(row) || statusSavingId === row.id"
              @change="handleStatusChange(row)" />
          </div>
          <div class="m-card-meta">学号 {{ row.studentNo || '-' }} · {{ row.username }}</div>
          <div class="m-card-meta">手机 {{ row.phone || '-' }}</div>
          <div class="m-card-meta">
            注册 {{ fmtDateTime(row.createTime) }}
            <span v-if="row.violationCount > 0" class="warn-text">· 违约 {{ row.violationCount }} 次</span>
          </div>
          <div class="m-card-actions">
            <el-dropdown trigger="click" @command="handleRoleCommand(row, $event)">
              <el-button size="mini">改角色<i class="el-icon-arrow-down el-icon--right"></i></el-button>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="STUDENT" :disabled="row.role === 'STUDENT'">学生</el-dropdown-item>
                <el-dropdown-item command="ORGANIZER" :disabled="row.role === 'ORGANIZER'">组织者</el-dropdown-item>
                <el-dropdown-item command="ADMIN" :disabled="row.role === 'ADMIN'">管理员</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
            <el-button size="mini" type="warning" plain @click="handleResetPassword(row)">重置密码</el-button>
          </div>
        </div>
        <div class="empty-box" v-if="!loading && !list.length">暂无用户</div>
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
  </div>
</template>

<script>
import { pageUsers, updateUserStatus, updateUserRole, resetUserPassword } from '@/api/user'
import { resolveUrl } from '@/utils/request'
import { fmtDateTime, ROLE_TEXT } from '@/utils/format'

export default {
  name: 'UserManage',
  data () {
    return {
      loading: false,
      list: [],
      total: 0,
      statusSavingId: null,
      query: {
        pageNum: 1,
        pageSize: 10,
        keyword: '',
        role: '',
        status: ''
      }
    }
  },
  created () {
    this.loadList()
  },
  methods: {
    resolveUrl,
    fmtDateTime,
    roleText (role) {
      return ROLE_TEXT[role] || role || '-'
    },
    roleTagType (role) {
      if (role === 'ADMIN') return 'danger'
      if (role === 'ORGANIZER') return 'warning'
      return ''
    },
    isSelf (row) {
      return Number(row.id) === Number(this.$store.getters.userId)
    },
    buildParams () {
      const params = {
        pageNum: this.query.pageNum,
        pageSize: this.query.pageSize
      }
      const keyword = (this.query.keyword || '').trim()
      if (keyword) params.keyword = keyword
      if (this.query.role) params.role = this.query.role
      if (this.query.status === 0 || this.query.status === 1) params.status = this.query.status
      return params
    },
    async loadList () {
      this.loading = true
      try {
        const data = await pageUsers(this.buildParams())
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
      this.query.keyword = ''
      this.query.role = ''
      this.query.status = ''
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
    async handleStatusChange (row) {
      const next = row.status
      this.statusSavingId = row.id
      try {
        await updateUserStatus(row.id, next)
        this.$message.success(next === 1 ? '账号已启用' : '账号已禁用')
      } catch (e) {
        // 后端不允许禁用自己或最后一个管理员，失败时回滚开关
        row.status = next === 1 ? 0 : 1
      } finally {
        this.statusSavingId = null
      }
    },
    async handleRoleCommand (row, role) {
      if (!role || role === row.role) return
      const name = row.realName || row.username
      try {
        await this.$confirm('确认将「' + name + '」的角色改为' + this.roleText(role) + '吗？', '修改角色', {
          type: 'warning',
          confirmButtonText: '确认修改',
          cancelButtonText: '取消'
        })
      } catch (e) {
        return
      }
      try {
        await updateUserRole(row.id, role)
        this.$message.success('角色已修改')
        this.loadList()
      } catch (e) {
        // 错误提示由 request 拦截器统一处理
      }
    },
    async handleResetPassword (row) {
      const name = row.realName || row.username
      try {
        await this.$confirm('确认重置「' + name + '」的登录密码吗？重置后新密码为 123456。', '重置密码', {
          type: 'warning',
          confirmButtonText: '确认重置',
          cancelButtonText: '取消'
        })
      } catch (e) {
        return
      }
      try {
        await resetUserPassword(row.id)
        this.$message.success('密码已重置为 123456')
      } catch (e) {
        // 错误提示由 request 拦截器统一处理
      }
    }
  }
}
</script>

<style scoped>
.reset-text {
  color: #e6a23c;
}
.warn-text { color: #f56c6c; }
</style>
