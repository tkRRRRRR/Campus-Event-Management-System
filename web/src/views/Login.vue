<template>
  <div class="login-page">
    <div class="brand">
      <h1>校园活动管理系统</h1>
      <p>活动发布 · 在线报名 · 扫码签到 · 参与评价</p>
      <ul>
        <li>名额并发安全，不会超卖</li>
        <li>签到码限时有效，爽约自动记录</li>
        <li>活动质量分，一看便知口碑</li>
      </ul>
    </div>

    <div class="panel">
      <h2>账号登录</h2>
      <el-form ref="form" :model="form" :rules="rules" @submit.native.prevent="onSubmit">
        <el-form-item prop="username">
          <el-input v-model.trim="form.username" prefix-icon="el-icon-user" placeholder="学号 / 工号 / 用户名" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model.trim="form.password" prefix-icon="el-icon-lock" show-password placeholder="密码" />
        </el-form-item>
        <el-button type="primary" class="submit" :loading="loading" @click="onSubmit">登 录</el-button>
        <div class="tips">
          <span>还没有账号？</span>
          <el-link type="primary" @click="$router.push('/register')">立即注册</el-link>
        </div>
        <div class="demo">
          <div>演示账号（密码均为 123456）</div>
          <div>管理员 admin ｜ 组织者 2022010102 ｜ 学生 2023000201</div>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Login',
  data () {
    return {
      loading: false,
      form: { username: '', password: '' },
      rules: {
        username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      }
    }
  },
  methods: {
    onSubmit () {
      this.$refs.form.validate(async valid => {
        if (!valid) return
        this.loading = true
        try {
          await this.$store.dispatch('login', this.form)
          this.$message.success('登录成功')
          const redirect = this.$route.query.redirect || '/home'
          this.$router.replace(redirect)
        } catch (e) {
          // 错误提示已由拦截器统一处理
        } finally {
          this.loading = false
        }
      })
    }
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f5a4f 0%, #2f7d6b 45%, #64b6a3 100%);
}
.brand {
  color: #fff;
  margin-right: 80px;
  max-width: 420px;
}
.brand h1 { font-size: 34px; margin: 0 0 12px; letter-spacing: 2px; }
.brand p { font-size: 15px; opacity: 0.9; margin: 0 0 24px; }
.brand ul { list-style: none; padding: 0; margin: 0; }
.brand li {
  padding: 6px 0 6px 22px;
  position: relative;
  opacity: 0.92;
}
.brand li::before {
  content: "";
  position: absolute;
  left: 0;
  top: 14px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.75);
}
.panel {
  width: 360px;
  background: #fff;
  border-radius: 10px;
  padding: 34px 32px 26px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
}
.panel h2 { margin: 0 0 22px; font-size: 20px; text-align: center; }
.submit { width: 100%; margin-top: 4px; }
.tips { margin-top: 14px; text-align: center; font-size: 13px; color: #909399; }
.demo {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px dashed #ebeef5;
  font-size: 12px;
  color: #a8abb2;
  line-height: 1.7;
}
@media (max-width: 768px) {
  .login-page {
    flex-direction: column;
    justify-content: flex-start;
    padding: 32px 16px 28px;
    gap: 22px;
    min-height: 100vh;
    box-sizing: border-box;
  }
  .brand { max-width: 100%; text-align: center; margin: 0 auto; }
  .brand h1 { font-size: 24px; line-height: 1.35; margin: 0 0 8px; }
  .brand p { font-size: 13px; margin: 0; }
  .brand ul { display: none; }
  .panel {
    width: 100%;
    max-width: 100%;
    padding: 22px 18px 18px;
    border-radius: 14px;
  }
  .panel h2 { font-size: 18px; margin-bottom: 16px; }
  .demo { font-size: 12px; line-height: 1.7; }
  .tips { font-size: 12px; }
}
</style>
