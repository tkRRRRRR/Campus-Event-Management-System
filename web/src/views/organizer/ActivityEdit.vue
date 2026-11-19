<template>
  <div class="page">
    <div class="page-title">{{ isEdit ? '编辑活动' : '发布活动' }}</div>

    <el-alert
      class="tip"
      type="info"
      :closable="false"
      show-icon
      title="提交后需管理员审核，审核通过才会在学生端展示"
    />

    <el-alert
      v-if="readonly"
      class="tip"
      type="warning"
      :closable="false"
      show-icon
      title="当前状态不可编辑"
      :description="readonlyText"
    />

    <el-alert
      v-if="isEdit && !readonly && detail.status === 3"
      class="tip"
      type="error"
      :closable="false"
      show-icon
      title="活动审核未通过"
      :description="'驳回理由：' + (detail.auditRemark || '管理员未填写理由') + '。修改后可重新提交审核。'"
    />

    <div class="card" v-loading="loadingDetail">
      <el-form
        ref="form"
        :model="form"
        :rules="rules"
        :disabled="readonly"
        label-width="110px"
        size="small"
      >
        <el-form-item label="活动标题" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="请输入活动标题" />
        </el-form-item>

        <el-form-item label="活动分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择活动分类" style="width: 100%">
            <el-option
              v-for="item in categories"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="活动地点" prop="location">
          <el-input v-model="form.location" maxlength="100" placeholder="请输入活动地点" />
        </el-form-item>

        <el-form-item label="人数上限" prop="capacity">
          <el-input-number
            v-model="form.capacity"
            :min="1"
            :max="1000"
            :step="10"
            controls-position="right"
            style="width: 180px"
          />
          <span class="muted hint">1 ~ 1000 人，且不能小于当前已报名人数</span>
        </el-form-item>

        <el-form-item label="报名审核" prop="needAudit">
          <el-switch
            v-model="form.needAudit"
            :active-value="1"
            :inactive-value="0"
            active-text="需要审核"
            inactive-text="无需审核"
          />
        </el-form-item>

        <el-form-item label="活动开始" prop="startTime">
          <el-date-picker
            v-model="form.startTime"
            type="datetime"
            placeholder="选择开始时间"
            value-format="yyyy-MM-dd HH:mm:ss"
            format="yyyy-MM-dd HH:mm"
            style="width: 240px"
          />
        </el-form-item>

        <el-form-item label="活动结束" prop="endTime">
          <el-date-picker
            v-model="form.endTime"
            type="datetime"
            placeholder="选择结束时间"
            value-format="yyyy-MM-dd HH:mm:ss"
            format="yyyy-MM-dd HH:mm"
            style="width: 240px"
          />
        </el-form-item>

        <el-form-item label="报名截止" prop="signupDeadline">
          <el-date-picker
            v-model="form.signupDeadline"
            type="datetime"
            placeholder="选择报名截止时间"
            value-format="yyyy-MM-dd HH:mm:ss"
            format="yyyy-MM-dd HH:mm"
            style="width: 240px"
          />
        </el-form-item>

        <el-form-item label="活动简介" prop="summary">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="3"
            maxlength="100"
            show-word-limit
            placeholder="用一句话介绍活动亮点，最多 100 字"
          />
        </el-form-item>

        <el-form-item label="活动详情" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="8"
            maxlength="2000"
            show-word-limit
            placeholder="活动流程、注意事项、参与要求等，最多 2000 字"
          />
        </el-form-item>
      </el-form>

      <div class="form-footer">
        <el-button size="small" @click="goBack">返回列表</el-button>
        <div class="spacer"></div>
        <template v-if="!readonly">
          <el-button size="small" :loading="saving" @click="handleSave(false)">保存草稿</el-button>
          <el-button size="small" type="primary" :loading="saving" @click="handleSave(true)">提交审核</el-button>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import { getActivity, createActivity, updateActivity } from '@/api/activity'
import { listCategories } from '@/api/category'
import { activityStatusText } from '@/utils/format'

/** 兼容性更好的时间解析：'yyyy-MM-dd HH:mm:ss' 在部分浏览器里直接 new Date 会失败 */
function timestamp (value) {
  if (!value) return 0
  return new Date(String(value).replace(/-/g, '/')).getTime()
}

const EDITABLE_STATUS = [0, 1, 3]

export default {
  name: 'ActivityEdit',
  data () {
    const validateEndTime = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请选择活动结束时间'))
        return
      }
      if (this.form.startTime && timestamp(value) <= timestamp(this.form.startTime)) {
        callback(new Error('结束时间必须晚于开始时间'))
        return
      }
      callback()
    }
    const validateSignupDeadline = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请选择报名截止时间'))
        return
      }
      if (this.form.startTime && timestamp(value) >= timestamp(this.form.startTime)) {
        callback(new Error('报名截止时间必须早于活动开始时间'))
        return
      }
      callback()
    }
    const validateStartTime = (rule, value, callback) => {
      if (!value) {
        callback(new Error('请选择活动开始时间'))
        return
      }
      if (timestamp(value) < Date.now()) {
        callback(new Error('活动开始时间不能早于当前时间'))
        return
      }
      callback()
    }
    return {
      loadingDetail: false,
      saving: false,
      categories: [],
      detail: {},
      form: this.createEmptyForm(),
      rules: {
        title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
        categoryId: [{ required: true, message: '请选择活动分类', trigger: 'change' }],
        location: [{ required: true, message: '请输入活动地点', trigger: 'blur' }],
        capacity: [{ required: true, type: 'number', min: 1, max: 1000, message: '人数上限为 1 ~ 1000', trigger: 'blur' }],
        startTime: [{ required: true, validator: validateStartTime, trigger: 'change' }],
        endTime: [{ required: true, validator: validateEndTime, trigger: 'change' }],
        signupDeadline: [{ required: true, validator: validateSignupDeadline, trigger: 'change' }]
      }
    }
  },
  computed: {
    activityId () {
      const id = this.$route.params.id
      return id ? Number(id) : null
    },
    isEdit () {
      return !!this.activityId
    },
    readonly () {
      if (!this.isEdit) return false
      if (this.detail.status === undefined || this.detail.status === null) return false
      return EDITABLE_STATUS.indexOf(this.detail.status) < 0
    },
    readonlyText () {
      const text = activityStatusText(this.detail.status)
      return '活动当前状态为「' + text + '」，仅草稿、待审核、已驳回的活动可以编辑。如需修改请先联系管理员。'
    }
  },
  created () {
    this.loadCategories()
    if (this.isEdit) this.loadDetail()
  },
  methods: {
    createEmptyForm () {
      return {
        title: '',
        categoryId: null,
        location: '',
        capacity: 50,
        needAudit: 0,
        startTime: '',
        endTime: '',
        signupDeadline: '',
        summary: '',
        content: ''
      }
    },
    async loadCategories () {
      try {
        this.categories = (await listCategories()) || []
      } catch (e) {
        this.categories = []
      }
    },
    async loadDetail () {
      this.loadingDetail = true
      try {
        const data = await getActivity(this.activityId)
        this.detail = data || {}
        this.fillForm(this.detail)
      } catch (e) {
        /* 错误提示已由 request 拦截器统一处理 */
      } finally {
        this.loadingDetail = false
      }
    },
    fillForm (data) {
      this.form = {
        title: data.title || '',
        categoryId: data.categoryId === undefined ? null : data.categoryId,
        location: data.location || '',
        capacity: data.capacity || 50,
        needAudit: data.needAudit === 1 ? 1 : 0,
        startTime: data.startTime || '',
        endTime: data.endTime || '',
        signupDeadline: data.signupDeadline || '',
        summary: data.summary || '',
        content: data.content || ''
      }
    },
    handleSave (submit) {
      if (this.saving) return
      this.$refs.form.validate(async valid => {
        if (!valid) return
        await this.doSave(submit)
      })
    },
    async doSave (submit) {
      const payload = {
        title: (this.form.title || '').trim(),
        categoryId: this.form.categoryId,
        location: (this.form.location || '').trim(),
        capacity: Number(this.form.capacity),
        needAudit: this.form.needAudit ? 1 : 0,
        startTime: this.form.startTime,
        endTime: this.form.endTime,
        signupDeadline: this.form.signupDeadline,
        summary: this.form.summary || '',
        content: this.form.content || '',
        // 后端 ActivitySaveDTO 用 draft 区分"存草稿"与"提交审核"
        draft: !submit
      }

      this.saving = true
      try {
        if (this.isEdit) {
          await updateActivity(this.activityId, payload)
          this.$message.success(submit ? '已重新提交审核' : '草稿已保存')
        } else {
          await createActivity(payload)
          this.$message.success(submit ? '已提交审核，请等待管理员审核' : '草稿已保存')
        }
        this.$router.push('/organizer/activities')
      } catch (e) {
        /* 错误提示已由 request 拦截器统一处理 */
      } finally {
        this.saving = false
      }
    },
    goBack () {
      this.$router.push('/organizer/activities')
    }
  }
}
</script>

<style scoped>
.tip {
  margin-bottom: 14px;
}
.hint {
  margin-left: 12px;
}
.form-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid #f0f2f3;
}
.form-footer .spacer {
  flex: 1;
}
</style>
