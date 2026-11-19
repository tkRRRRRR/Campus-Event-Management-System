<template>
  <div class="page">
    <h2 class="page-title">分类管理</h2>

    <div class="card">
      <el-alert
        class="category-tip"
        type="info"
        :closable="false"
        show-icon
        title="分类用于活动筛选，删除前请确认没有活动在用"
        description="停用后该分类将从学生端的活动筛选和发布页面中移除，但已有的活动不受影响。" />

      <div class="toolbar">
        <el-button type="primary" icon="el-icon-plus" @click="openCreate">新增分类</el-button>
        <div class="spacer"></div>
        <span class="muted">共 {{ list.length }} 个分类</span>
      </div>

      <el-table v-loading="loading" :data="list" border stripe empty-text="暂无分类">
        <el-table-column prop="name" label="分类名" min-width="220" />
        <el-table-column label="排序值" width="120" align="center">
          <template slot-scope="scope">{{ scope.row.sort == null ? 0 : scope.row.sort }}</template>
        </el-table-column>
        <el-table-column label="状态" width="170" align="center">
          <template slot-scope="scope">
            <el-switch
              v-model="scope.row.status"
              :active-value="1"
              :inactive-value="0"
              :disabled="statusSavingId === scope.row.id"
              @change="handleStatusChange(scope.row)" />
            <span class="switch-text">{{ scope.row.status === 1 ? '启用' : '停用' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="openEdit(scope.row)">编辑</el-button>
            <el-button type="text" size="mini" class="danger-text" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      :title="form.id ? '编辑分类' : '新增分类'"
      :visible.sync="dialogVisible"
      width="460px"
      @closed="resetForm">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="分类名" prop="name">
          <el-input v-model.trim="form.name" maxlength="32" show-word-limit placeholder="如：文艺演出" />
        </el-form-item>
        <el-form-item label="排序值" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
          <span class="muted"> 数值越小越靠前</span>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
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
import { listAllCategories, createCategory, updateCategory, deleteCategory } from '@/api/category'

export default {
  name: 'CategoryManage',
  data () {
    return {
      loading: false,
      list: [],
      statusSavingId: null,
      dialogVisible: false,
      submitting: false,
      form: {
        id: null,
        name: '',
        sort: 0,
        status: 1
      },
      rules: {
        name: [
          { required: true, message: '请输入分类名', trigger: 'blur' },
          { max: 32, message: '分类名不能超过 32 个字符', trigger: 'blur' }
        ],
        sort: [{ required: true, message: '请输入排序值', trigger: 'blur' }]
      }
    }
  },
  created () {
    this.loadList()
  },
  methods: {
    async loadList () {
      this.loading = true
      try {
        this.list = (await listAllCategories()) || []
      } catch (e) {
        this.list = []
      } finally {
        this.loading = false
      }
    },
    openCreate () {
      this.resetForm()
      this.dialogVisible = true
    },
    openEdit (row) {
      this.form = {
        id: row.id,
        name: row.name || '',
        sort: row.sort == null ? 0 : row.sort,
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
        name: '',
        sort: 0,
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
          name: this.form.name,
          sort: Number(this.form.sort) || 0,
          status: this.form.status
        }
        try {
          if (this.form.id) {
            await updateCategory(this.form.id, payload)
            this.$message.success('分类已更新')
          } else {
            await createCategory(payload)
            this.$message.success('分类已新增')
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
        await updateCategory(row.id, { status: next })
        this.$message.success(next === 1 ? '分类已启用' : '分类已停用')
        this.loadList()
      } catch (e) {
        row.status = next === 1 ? 0 : 1
      } finally {
        this.statusSavingId = null
      }
    },
    async handleDelete (row) {
      try {
        await this.$confirm('确认删除分类「' + row.name + '」吗？删除后不可恢复。', '删除分类', {
          type: 'warning',
          confirmButtonText: '确认删除',
          cancelButtonText: '取消'
        })
      } catch (e) {
        return
      }
      try {
        await deleteCategory(row.id)
        this.$message.success('分类已删除')
        this.loadList()
      } catch (e) {
        // 分类下仍有活动时后端会报错，提示由拦截器统一处理
      }
    }
  }
}
</script>

<style scoped>
.category-tip {
  margin-bottom: 14px;
}
.switch-text {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}
.danger-text {
  color: #f56c6c;
}
</style>
