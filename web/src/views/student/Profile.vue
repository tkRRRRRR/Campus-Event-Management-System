<template>
  <div class="page">
    <el-row :gutter="16">
      <!-- 头像卡片 -->
      <el-col :span="7">
        <div class="card avatar-card">
          <div class="avatar-wrap">
            <el-avatar v-if="avatarUrl" :size="96" :src="avatarUrl" class="avatar" />
            <div v-else class="avatar-empty">
              <i class="el-icon-user-solid"></i>
            </div>
          </div>
          <div class="avatar-name">{{ user.realName || user.username || '未设置' }}</div>
          <div class="avatar-role">{{ roleText }}</div>
          <el-upload
            class="avatar-upload"
            action="#"
            accept="image/*"
            :show-file-list="false"
            :before-upload="beforeAvatarUpload"
            :http-request="handleAvatarUpload"
          >
            <el-button type="primary" plain size="small" icon="el-icon-picture-outline" :loading="avatarLoading">更换头像</el-button>
          </el-upload>
          <div class="avatar-tip">支持 jpg / png 等图片，大小不超过 2MB</div>
        </div>
      </el-col>

      <!-- 基本资料 + 修改密码 -->
      <el-col :span="10">
        <div class="card">
          <div class="card-title">基本资料</div>
          <el-form ref="profileForm" :model="profileForm" :rules="profileRules" label-width="76px" size="small">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="profileForm.realName" maxlength="20" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profileForm.phone" maxlength="11" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" maxlength="50" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="profileLoading" @click="saveProfile">保存资料</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="card password-card">
          <div class="card-title">修改密码</div>
          <el-form ref="passwordForm" :model="passwordForm" :rules="passwordRules" label-width="76px" size="small">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="6-20 位新密码" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="passwordLoading" @click="savePassword">确认修改</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>

      <!-- 账号信息 -->
      <el-col :span="7">
        <div class="card">
          <div class="card-title">账号信息</div>
          <div class="info-row">
            <span class="info-label">用户名</span>
            <span class="info-value">{{ user.username || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">学号</span>
            <span class="info-value">{{ user.studentNo || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">角色</span>
            <span class="info-value">{{ roleText }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">违约次数</span>
            <span class="info-value" :class="{ 'danger-text': violationCount > 0 }">{{ violationCount }} 次</span>
          </div>
          <div class="info-row">
            <span class="info-label">封禁到期</span>
            <span class="info-value" :class="{ 'danger-text': !!user.banUntil }">{{ banText }}</span>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { uploadAvatar, updateProfile, changePassword } from '@/api/user'
import { resolveUrl } from '@/utils/request'
import { fmtDateTime, ROLE_TEXT } from '@/utils/format'

export default {
  name: 'Profile',
  data () {
    return {
      avatarLoading: false,
      profileLoading: false,
      passwordLoading: false,
      profileForm: {
        realName: '',
        phone: '',
        email: ''
      },
      profileRules: {
        realName: [
          { required: true, message: '请输入真实姓名', trigger: 'blur' },
          { min: 2, max: 20, message: '姓名长度为 2-20 个字符', trigger: 'blur' }
        ],
        phone: [
          { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
        ],
        email: [
          { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
        ]
      },
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      passwordRules: {
        oldPassword: [
          { required: true, message: '请输入原密码', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入新密码', trigger: 'blur' },
          {
            validator: (rule, value, callback) => {
              if (value !== this.passwordForm.newPassword) {
                callback(new Error('两次输入的密码不一致'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }
        ]
      }
    }
  },
  computed: {
    ...mapGetters(['user']),
    avatarUrl () {
      const avatar = this.user && this.user.avatar
      return avatar ? resolveUrl(avatar) : ''
    },
    roleText () {
      const role = this.user && this.user.role
      return ROLE_TEXT[role] || role || '-'
    },
    violationCount () {
      return Number(this.user && this.user.violationCount) || 0
    },
    banText () {
      const banUntil = this.user && this.user.banUntil
      return banUntil ? fmtDateTime(banUntil) : '正常'
    }
  },
  watch: {
    user: {
      immediate: true,
      handler () {
        this.fillProfileForm()
      }
    }
  },
  methods: {
    fillProfileForm () {
      const user = this.user || {}
      this.profileForm = {
        realName: user.realName || '',
        phone: user.phone || '',
        email: user.email || ''
      }
    },
    beforeAvatarUpload (file) {
      const isImage = /^image\/(jpeg|jpg|png|gif|bmp|webp)$/.test(file.type)
      if (!isImage) {
        this.$message.error('只能上传 jpg / png / gif / bmp / webp 格式的图片')
        return false
      }
      if (file.size / 1024 / 1024 > 2) {
        this.$message.error('图片大小不能超过 2MB')
        return false
      }
      return true
    },
    async handleAvatarUpload (option) {
      this.avatarLoading = true
      try {
        await uploadAvatar(option.file)
        this.$message.success('头像已更新')
        try {
          await this.$store.dispatch('loadUser')
        } catch (e) {
          // 用户信息刷新失败不影响已上传的头像
        }
      } catch (e) {
        // 错误提示已由 request 拦截器统一处理
      } finally {
        this.avatarLoading = false
      }
    },
    saveProfile () {
      this.$refs.profileForm.validate(async valid => {
        if (!valid) return
        this.profileLoading = true
        try {
          await updateProfile({
            realName: this.profileForm.realName,
            phone: this.profileForm.phone,
            email: this.profileForm.email
          })
          this.$message.success('资料已保存')
          try {
            await this.$store.dispatch('loadUser')
          } catch (e) {
            // 忽略用户信息刷新失败
          }
        } catch (e) {
          // 错误提示已由 request 拦截器统一处理
        } finally {
          this.profileLoading = false
        }
      })
    },
    savePassword () {
      this.$refs.passwordForm.validate(async valid => {
        if (!valid) return
        this.passwordLoading = true
        try {
          await changePassword({
            oldPassword: this.passwordForm.oldPassword,
            newPassword: this.passwordForm.newPassword
          })
          this.$message.success('密码已修改，请重新登录')
          try {
            await this.$store.dispatch('logout')
          } catch (e) {
            // 退出登录失败也继续跳转
          }
          this.$router.replace('/login')
        } catch (e) {
          // 错误提示已由 request 拦截器统一处理
        } finally {
          this.passwordLoading = false
        }
      })
    }
  }
}
</script>

<style scoped>
.avatar-card {
  text-align: center;
  padding-top: 26px;
}
.avatar-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 14px;
}
.avatar {
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.08);
}
.avatar-empty {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: #f0f2f5;
  color: #c0c4cc;
  font-size: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
}
.avatar-role {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-sub);
}
.avatar-upload {
  margin-top: 18px;
}
.avatar-tip {
  margin-top: 12px;
  font-size: 12px;
  color: #c0c4cc;
  line-height: 1.6;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  position: relative;
  padding-left: 10px;
  margin-bottom: 18px;
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
.password-card {
  margin-top: 16px;
}
.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 13px 2px;
  border-bottom: 1px dashed #f0f2f5;
  font-size: 14px;
}
.info-row:last-child {
  border-bottom: none;
}
.info-label {
  color: var(--text-sub);
}
.info-value {
  color: var(--text);
  word-break: break-all;
  text-align: right;
}
.danger-text {
  color: #f56c6c;
}
</style>
