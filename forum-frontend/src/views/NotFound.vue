<template>
  <div class="page-container notfound-page">
    <div class="nf-code">404</div>
    <h2 class="nf-title">这片海域没有灯塔</h2>
    <p class="nf-desc">你要找的页面不存在，或者已经被潮水带走了。</p>
    <div class="nf-actions">
      <router-link to="/" class="warm-btn">⚓ 回港湾首页</router-link>
      <router-link to="/topics" class="warm-btn outline"># 逛逛话题</router-link>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.notfound-page {
  min-height: 60vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}
.nf-code {
  font-size: 96px;
  font-weight: 900;
  line-height: 1;
  color: var(--accent-warm);
  text-shadow: 0 0 32px rgba(193, 127, 58, 0.35);
  letter-spacing: 4px;
}
.nf-title { font-size: 22px; font-weight: 800; margin: 18px 0 8px; }
.nf-desc { font-size: 14px; color: var(--text-secondary); margin-bottom: 24px; }
.nf-actions { display: flex; gap: 12px; flex-wrap: wrap; justify-content: center; }
</style>
