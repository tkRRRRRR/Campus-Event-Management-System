<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面走丢了，或者你没有访问权限。</p>
    <el-button type="primary" @click="$router.replace('/home')">返回首页</el-button>
  </div>
</template>

<script>
export default { name: 'NotFound' }
</script>

<style scoped>
.not-found {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background: var(--bg);
}
.not-found h1 { font-size: 72px; margin: 0; color: var(--primary); }
.not-found p { color: #909399; margin: 0 0 8px; }
</style>
