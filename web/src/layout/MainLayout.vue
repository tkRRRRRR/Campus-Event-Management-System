<template>
  <el-container class="layout">
    <el-aside width="212px" class="aside">
      <div class="logo">
        <i class="el-icon-s-promotion"></i>
        <span>校园活动管理</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="menu"
        background-color="#1f5a4f"
        text-color="#cfe3dd"
        active-text-color="#ffffff"
        router
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <i :class="item.icon"></i>
          <span slot="title">{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header height="56px" class="header">
        <div class="crumb">{{ currentTitle }}</div>
        <div class="spacer"></div>
        <el-tooltip content="消息中心" placement="bottom">
          <div class="icon-btn" @click="$router.push('/notices')">
            <el-badge :value="unread" :hidden="unread === 0" :max="99">
              <i class="el-icon-bell"></i>
            </el-badge>
          </div>
        </el-tooltip>
        <el-dropdown trigger="click" @command="onCommand">
          <span class="user">
            <el-avatar :size="28" :src="avatar"><i class="el-icon-user"></i></el-avatar>
            <span class="name">{{ user.realName || user.username }}</span>
            <el-tag size="mini" type="primary" effect="plain">{{ roleText }}</el-tag>
            <i class="el-icon-arrow-down"></i>
          </span>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="profile">个人中心</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </el-header>

      <el-main class="main">
        <transition name="fade" mode="out-in">
          <router-view />
        </transition>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import { mapGetters } from 'vuex'
import { unreadCount } from '@/api/notice'
import { resolveUrl } from '@/utils/request'
import { ROLE_TEXT } from '@/utils/format'

export default {
  name: 'MainLayout',
  data () {
    return { unread: 0, timer: null }
  },
  computed: {
    ...mapGetters(['user', 'role']),
    roleText () {
      return ROLE_TEXT[this.role] || this.role
    },
    avatar () {
      return resolveUrl(this.user && this.user.avatar)
    },
    activeMenu () {
      const path = this.$route.path
      if (path.indexOf('/activity/') === 0) return '/activities'
      if (path.indexOf('/organizer/activity/edit') === 0) return '/organizer/activities'
      if (/^\/organizer\/(registrations|checkin)\//.test(path)) return '/organizer/activities'
      return path
    },
    currentTitle () {
      return (this.$route.meta && this.$route.meta.title) || ''
    },
    menus () {
      const student = [
        { path: '/home', title: '首页', icon: 'el-icon-s-home' },
        { path: '/activities', title: '活动中心', icon: 'el-icon-s-flag' },
        { path: '/my-registrations', title: '我的报名', icon: 'el-icon-tickets' },
        { path: '/my-reviews', title: '我的评价', icon: 'el-icon-star-off' },
        { path: '/notices', title: '消息中心', icon: 'el-icon-bell' },
        { path: '/profile', title: '个人中心', icon: 'el-icon-setting' }
      ]
      const organizer = [
        { path: '/organizer/activities', title: '我的活动', icon: 'el-icon-s-order' },
        { path: '/organizer/activity/edit', title: '发布活动', icon: 'el-icon-circle-plus-outline' },
        { path: '/organizer/stats', title: '活动数据', icon: 'el-icon-data-line' }
      ]
      const admin = [
        { path: '/admin/audit', title: '活动审核', icon: 'el-icon-s-check' },
        { path: '/admin/users', title: '用户管理', icon: 'el-icon-user-solid' },
        { path: '/admin/categories', title: '分类管理', icon: 'el-icon-collection-tag' },
        { path: '/admin/announcements', title: '公告管理', icon: 'el-icon-chat-line-square' },
        { path: '/admin/stats', title: '平台统计', icon: 'el-icon-pie-chart' },
        { path: '/admin/configs', title: '系统参数', icon: 'el-icon-s-tools' }
      ]
      let list = student.slice()
      if (this.role === 'ORGANIZER') list = list.concat(organizer)
      // 管理员有独立的后台菜单；组织者端接口按"只能操作自己的活动"做了归属校验，
      // 管理员进入会出现 403，因此这里不再把组织者菜单拼给管理员。
      if (this.role === 'ADMIN') list = list.concat(admin)
      return list
    }
  },
  mounted () {
    this.refreshUnread()
    this.timer = setInterval(this.refreshUnread, 60000)
  },
  beforeDestroy () {
    if (this.timer) clearInterval(this.timer)
  },
  methods: {
    async refreshUnread () {
      try {
        const data = await unreadCount()
        this.unread = data.count || 0
      } catch (e) {
        this.unread = 0
      }
    },
    onCommand (cmd) {
      if (cmd === 'profile') {
        this.$router.push('/profile')
      } else if (cmd === 'logout') {
        this.$store.dispatch('logout')
        this.$router.replace('/login')
        this.$message.success('已退出登录')
      }
    }
  }
}
</script>

<style scoped>
.layout { height: 100%; }
.aside {
  background: #1f5a4f;
  display: flex;
  flex-direction: column;
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 18px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  background: rgba(0, 0, 0, 0.12);
}
.logo i { font-size: 20px; }
.menu { border-right: none; flex: 1; }
.menu >>> .el-menu-item { height: 46px; line-height: 46px; }
.header {
  background: #fff;
  display: flex;
  align-items: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  padding: 0 20px;
}
.crumb { font-size: 15px; font-weight: 600; }
.spacer { flex: 1; }
.icon-btn {
  cursor: pointer;
  padding: 0 14px;
  font-size: 18px;
  color: #606266;
}
.user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 13px;
}
.user .name { max-width: 110px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.main { background: var(--bg); padding: 0; overflow-y: auto; }
.fade-enter-active, .fade-leave-active { transition: opacity 0.15s; }
.fade-enter, .fade-leave-to { opacity: 0; }
</style>
