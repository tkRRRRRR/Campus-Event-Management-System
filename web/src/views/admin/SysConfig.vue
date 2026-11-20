<template>
  <div class="page">
    <h2 class="page-title">系统参数</h2>

    <div class="card">
      <el-alert
        class="config-tip"
        type="warning"
        :closable="false"
        show-icon
        title="违规与签到的四项参数（violation_limit、ban_days、checkin_code_minutes、activity_default_capacity）以及质量分三项权重（quality_weight_checkin、quality_weight_score、quality_weight_complete）必须为正整数；权重之和不必等于 100，计算时按比例归一" />

      <div class="toolbar">
        <span class="muted">参数保存后立即生效，共 {{ list.length }} 项</span>
        <div class="spacer"></div>
        <el-button icon="el-icon-refresh" @click="loadList">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe empty-text="暂无系统参数">
        <el-table-column prop="configKey" label="参数键" width="230" show-overflow-tooltip />
        <el-table-column label="参数值" width="240">
          <template slot-scope="scope">
            <el-input
              v-model.trim="scope.row.editValue"
              size="small"
              :placeholder="scope.row.configValue"
              @keyup.enter.native="handleSave(scope.row)" />
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="260">
          <template slot-scope="scope">{{ scope.row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center">
          <template slot-scope="scope">
            <el-button
              type="primary"
              size="mini"
              plain
              :loading="savingId === scope.row.id"
              @click="handleSave(scope.row)">保存</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script>
import { listConfigs, updateConfig } from '@/api/config'

// 后端 SysConfigServiceImpl 中必须为正整数的参数键
const INT_KEYS = [
  'violation_limit', 'ban_days', 'checkin_code_minutes', 'activity_default_capacity',
  'quality_weight_checkin', 'quality_weight_score', 'quality_weight_complete'
]

export default {
  name: 'SysConfig',
  data () {
    return {
      loading: false,
      list: [],
      savingId: null
    }
  },
  created () {
    this.loadList()
  },
  methods: {
    async loadList () {
      this.loading = true
      try {
        const data = (await listConfigs()) || []
        this.list = data.map(item => Object.assign({}, item, { editValue: item.configValue }))
      } catch (e) {
        this.list = []
      } finally {
        this.loading = false
      }
    },
    async handleSave (row) {
      const value = String(row.editValue == null ? '' : row.editValue).trim()
      if (!value) {
        this.$message.warning('参数值不能为空')
        return
      }
      if (INT_KEYS.indexOf(row.configKey) >= 0 && !/^[1-9]\d*$/.test(value)) {
        this.$message.warning('该参数必须为正整数')
        return
      }
      this.savingId = row.id
      try {
        await updateConfig(row.id, value)
        this.$message.success('参数已保存')
        await this.loadList()
      } catch (e) {
        // 错误提示由 request 拦截器统一处理
      } finally {
        this.savingId = null
      }
    }
  }
}
</script>

<style scoped>
.config-tip {
  margin-bottom: 14px;
}
</style>
