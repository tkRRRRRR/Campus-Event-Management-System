<template>
  <div class="register-page">
    <div class="panel">
      <h2>学生注册</h2>
      <p class="muted">注册后默认为「学生」角色，可浏览活动、报名、签到与评价。</p>
      <el-form ref="form" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" placeholder="6~20 位，登录时使用" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model.trim="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="学号" prop="studentNo">
          <el-input v-model.trim="form.studentNo" placeholder="学号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model.trim="form.password" show-password placeholder="6~20 位" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input v-model.trim="form.confirm" show-password placeholder="再次输入密码" />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="onSubmit">注册</el-button>
        <div class="tips">
          <span>已有账号？</span>
          <el-link type="primary" @click="$router.push('/login')">返回登录</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script>
import { register } from '@/api/auth'

export default {
  name: 'Register',
  data () {
    const checkConfirm = (rule, value, callback) => {
      if (value !== this.form.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      loading: false,
      form: { username: '', realName: '', studentNo: '', password: '', confirm: '' },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 6, max: 20, message: '用户名长度为 6~20 位', trigger: 'blur' }
        ],
        realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度为 6~20 位', trigger: 'blur' }
        ],
        confirm: [
          { required: true, message: '请再次输入密码', trigger: 'blur' },
          { validator: checkConfirm, trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    onSubmit () {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        this.loading = true
        try {
          await register({
            username: this.form.username,
            realName: this.form.realName,
            studentNo: this.form.studentNo,
            password: this.form.password
          })
          this.$message.success('注册成功，请登录')
          this.$router.replace('/login')
        } catch (e) {
          // 已统一提示
        } finally {
          this.loading = false
        }
      })
    }
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  background: linear-gradient(135deg, #1f5a4f 0%, #2f7d6b 45%, #64b6a3 100%);
}
.panel {
  width: 420px;
  background: #fff;
  border-radius: 10px;
  padding: 30px 32px 24px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
}
.panel h2 { margin: 0 0 6px; font-size: 20px; }
.panel .muted { margin: 0 0 18px; }
.submit { width: 100%; }
.tips { margin-top: 14px; text-align: center; font-size: 13px; color: #909399; }
</style>
