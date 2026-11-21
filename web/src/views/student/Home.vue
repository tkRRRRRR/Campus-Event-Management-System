<template>
  <div class="page">
    <!-- 公告轮播 -->
    <div class="banner" v-if="announcements.length">
      <el-carousel height="132px" :interval="5000" indicator-position="none" arrow="hover">
        <el-carousel-item v-for="a in announcements" :key="a.id">
          <div class="banner-item" @click="openAnnouncement(a)">
            <div class="banner-tag">公告</div>
            <h3>{{ a.title }}</h3>
            <p>{{ a.content }}</p>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <!-- 热门活动 -->
    <div class="section" v-loading="loadingHot">
      <div class="section-head">
        <h3>热门活动</h3>
        <el-link type="primary" :underline="false" @click="$router.push('/activities')">查看全部</el-link>
      </div>
      <div class="hot-grid">
        <div class="activity-card" v-for="item in hotList" :key="item.id" @click="openActivity(item.id)">
            <div class="cover-box">
              <img v-if="item.coverUrl" :src="resolveUrl(item.coverUrl)" class="cover" :alt="item.title" />
              <div v-else class="cover-fallback">{{ item.categoryName || '活动' }}</div>
              <el-tag class="hot-tag" size="mini" type="danger" effect="dark">热</el-tag>
            </div>
            <div class="card-body">
              <h4>{{ item.title }}</h4>
              <div class="meta"><i class="el-icon-time"></i> {{ fmtDateTime(item.startTime) }}</div>
              <div class="meta"><i class="el-icon-location-outline"></i> {{ item.location || '待定' }}</div>
              <div class="foot">
                <span class="join">{{ item.signedCount }}/{{ item.capacity }} 人</span>
                <status-tag :status="item.status" />
              </div>
            </div>
        </div>
      </div>
      <div class="empty-box" v-if="!loadingHot && !hotList.length">暂无活动</div>
    </div>

    <!-- 最新活动 -->
    <div class="section" v-loading="loadingLatest">
      <div class="section-head">
        <h3>最新发布</h3>
        <el-link type="primary" :underline="false" @click="$router.push('/activities')">查看全部</el-link>
      </div>
      <div class="latest-list">
        <div class="latest-item" v-for="item in latestList" :key="item.id" @click="openActivity(item.id)">
          <div class="latest-cover">
            <img v-if="item.coverUrl" :src="resolveUrl(item.coverUrl)" class="cover" :alt="item.title" />
            <div v-else class="cover-fallback">{{ item.categoryName || '活动' }}</div>
          </div>
          <div class="latest-body">
            <div class="latest-title">
              {{ item.title }}
              <status-tag :status="item.status" />
            </div>
            <div class="meta">
              <span><i class="el-icon-time"></i> {{ fmtDateTime(item.startTime) }}</span>
              <span><i class="el-icon-location-outline"></i> {{ item.location || '待定' }}</span>
              <span><i class="el-icon-user"></i> {{ item.signedCount }}/{{ item.capacity }}</span>
            </div>
            <p class="summary">{{ item.summary || '暂无活动简介' }}</p>
          </div>
        </div>
        <div class="empty-box" v-if="!loadingLatest && !latestList.length">暂无活动</div>
      </div>
    </div>

    <el-dialog :title="current.title" :visible.sync="dialogVisible" width="520px">
      <div class="announce-content">{{ current.content }}</div>
      <span slot="footer">
        <el-button type="primary" @click="dialogVisible = false">我知道了</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { pageActivities } from '@/api/activity'
import { publishedAnnouncements } from '@/api/announcement'
import { resolveUrl } from '@/utils/request'
import { fmtDateTime } from '@/utils/format'
import StatusTag from '@/components/StatusTag.vue'

export default {
  name: 'Home',
  components: { StatusTag },
  data () {
    return {
      announcements: [],
      hotList: [],
      latestList: [],
      loadingHot: false,
      loadingLatest: false,
      dialogVisible: false,
      current: {}
    }
  },
  created () {
    this.loadAnnouncements()
    this.loadActivities()
  },
  methods: {
    resolveUrl,
    fmtDateTime,
    async loadAnnouncements () {
      try {
        this.announcements = (await publishedAnnouncements()) || []
      } catch (e) {
        this.announcements = []
      }
    },
    async loadActivities () {
      this.loadingHot = true
      this.loadingLatest = true
      try {
        const hot = await pageActivities({ pageNum: 1, pageSize: 3, sort: 'hot' })
        this.hotList = hot.records || []
      } catch (e) {
        this.hotList = []
      } finally {
        this.loadingHot = false
      }
      try {
        const latest = await pageActivities({ pageNum: 1, pageSize: 6, sort: 'latest' })
        this.latestList = latest.records || []
      } catch (e) {
        this.latestList = []
      } finally {
        this.loadingLatest = false
      }
    },
    openActivity (id) {
      this.$router.push(`/activity/${id}`)
    },
    openAnnouncement (a) {
      this.current = a
      this.dialogVisible = true
    }
  }
}
</script>

<style scoped>
.banner { border-radius: 8px; overflow: hidden; margin-bottom: 18px; box-shadow: var(--card-shadow); }
.banner-item {
  height: 132px;
  padding: 22px 26px;
  background: linear-gradient(120deg, #23695b, #3f9c86);
  color: #fff;
  cursor: pointer;
}
.banner-tag {
  display: inline-block;
  font-size: 12px;
  padding: 1px 8px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.22);
  margin-bottom: 8px;
}
.banner-item h3 { margin: 0 0 8px; font-size: 20px; }
.banner-item p {
  margin: 0;
  opacity: 0.9;
  font-size: 13px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.section { margin-bottom: 22px; }
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.section-head h3 { margin: 0; font-size: 16px; position: relative; padding-left: 10px; }
.section-head h3::before {
  content: "";
  position: absolute;
  left: 0;
  top: 4px;
  width: 3px;
  height: 14px;
  border-radius: 2px;
  background: var(--primary);
}
.activity-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: var(--card-shadow);
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}
.activity-card:hover { transform: translateY(-3px); box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1); }
.cover-box { position: relative; height: 130px; background: #eef1f2; }
.hot-tag { position: absolute; right: 8px; top: 8px; }
.card-body { padding: 12px 14px 14px; }
.card-body h4 {
  margin: 0 0 8px;
  font-size: 14px;
  height: 40px;
  line-height: 20px;
  overflow: hidden;
}
.meta { font-size: 12px; color: #909399; margin-bottom: 4px; }
.foot { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; }
.join { font-size: 12px; color: var(--primary); }
.latest-list { display: flex; flex-direction: column; gap: 12px; }
.latest-item {
  display: flex;
  gap: 14px;
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  box-shadow: var(--card-shadow);
  cursor: pointer;
}
.latest-item:hover { box-shadow: 0 6px 18px rgba(0, 0, 0, 0.09); }
.latest-cover { width: 132px; height: 88px; border-radius: 6px; overflow: hidden; flex-shrink: 0; background: #eef1f2; }
.latest-body { flex: 1; min-width: 0; }
.latest-title { font-size: 15px; font-weight: 600; margin-bottom: 8px; display: flex; align-items: center; gap: 8px; }
.latest-title .el-tag { font-weight: 400; }
.latest-body .meta { display: flex; gap: 16px; }
.summary {
  margin: 8px 0 0;
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hot-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.hot-grid .card-body h4 { height: auto; min-height: 40px; }
@media (max-width: 768px) {
  .banner-item { padding: 14px 16px; }
  .banner-item h3 { font-size: 16px; line-height: 1.45; margin: 4px 0 6px; }
  .banner-item p { -webkit-line-clamp: 2; font-size: 12px; line-height: 1.6; }
  .hot-grid {
    display: flex;
    gap: 12px;
    overflow-x: auto;
    scroll-snap-type: x mandatory;
    -webkit-overflow-scrolling: touch;
    padding: 2px 2px 8px;
  }
  .hot-grid::-webkit-scrollbar { display: none; }
  .hot-grid .activity-card {
    min-width: 76%;
    flex: 0 0 auto;
    scroll-snap-align: start;
  }
  .cover-box { height: 118px; }
  .latest-item { padding: 10px; gap: 10px; align-items: flex-start; }
  .latest-cover { width: 96px; height: 72px; }
  .latest-title { font-size: 14px; align-items: flex-start; flex-wrap: wrap; margin-bottom: 6px; }
  .latest-body .meta { flex-wrap: wrap; gap: 4px 10px; }
  .summary { margin-top: 6px; font-size: 12px; white-space: normal; -webkit-line-clamp: 2; display: -webkit-box; -webkit-box-orient: vertical; }
  .section { margin-bottom: 18px; }
}
.announce-content { line-height: 1.8; white-space: pre-wrap; color: #606266; }
</style>
