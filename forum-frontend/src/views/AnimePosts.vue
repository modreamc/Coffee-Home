<template>
  <div class="page-container">
    <div class="section-header fade-up">
      <h2 class="section-title"><span class="section-line" />📺 动漫<span class="section-line" /></h2>
      <p class="section-sub">共 {{ total }} 篇 · 番剧 / 漫评 / 二创，慢慢翻</p>
    </div>
    <div class="post-list">
      <div v-for="(post,i) in posts" :key="post.id" class="fade-up" :class="{'feature-slot': i===0}" :style="{transitionDelay:`${(i%6)*0.08}s`}"><PostCard :post="post" :featured="i===0" /></div>
    </div>
    <div v-if="posts.length===0&&!loading" class="empty-state">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="1.5"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
      <p style="margin-top:12px;color:var(--text-muted)">暂无动漫帖子</p>
    </div>
    <div v-if="total>size" class="pagination-wrap"><el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev,pager,next" @current-change="loadPosts" background /></div>
  </div>
</template>
<script setup>
import {ref,onMounted,nextTick,onUnmounted} from 'vue'
import {postApi} from '../api/post'
import PostCard from '../components/PostCard.vue'
const posts=ref([]);const page=ref(1);const size=ref(20);const total=ref(0);const loading=ref(false)
let observer=null
async function loadPosts(){
  loading.value=true
  try{const res=await postApi.list({category:'ANIME',page:page.value,size:size.value});posts.value=res.list||[];total.value=res.total||0;nextTick(()=>observePosts())}
  catch(e){posts.value=[];total.value=0}
  finally{loading.value=false}
}
function observePosts(){
  if(observer)observer.disconnect()
  observer=new IntersectionObserver(entries=>{entries.forEach(e=>{if(e.isIntersecting)e.target.classList.add('visible')})},{threshold:.1})
  document.querySelectorAll('.fade-up').forEach(el=>observer.observe(el))
}
onMounted(()=>{loadPosts();nextTick(()=>observePosts())})
onUnmounted(()=>{if(observer)observer.disconnect()})
</script>
<style scoped>
.section-header{display:flex;flex-direction:column;align-items:center;justify-content:center;margin-bottom:26px;padding-top:14px;gap:10px}
.section-sub{font-size:12.5px;color:var(--text-muted);letter-spacing:.04em}
.section-title{display:flex;align-items:center;gap:16px;font-size:24px;font-weight:700}
.section-line{display:block;width:40px;height:2px;background:linear-gradient(90deg,transparent,var(--accent-secondary))}
.section-line:last-child{background:linear-gradient(90deg,var(--accent-secondary),transparent)}
.post-list{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:20px}
@media(max-width:1200px){.post-list{grid-template-columns:repeat(2,minmax(0,1fr))}}
@media(max-width:768px){.post-list{grid-template-columns:1fr}}

/* ═══ 板块招牌 ═══ */
.section-header{padding-top:14px;margin-bottom:26px}
.section-title{
  position:relative;
  font-family:'Georgia','Songti SC','Noto Serif SC',serif;
  font-size:clamp(22px,2.6vw,28px);font-weight:900;
  padding:12px 30px;gap:16px;
  background:
    radial-gradient(120% 90% at 0% 0%, rgba(255,255,255,.5), transparent 60%),
    var(--paper);
  border:1px solid var(--border-light);
  border-radius:14px;
  box-shadow:var(--shadow-sm), inset 0 1px 0 rgba(255,255,255,.4);
}
[data-theme="dark"] .section-title{background:radial-gradient(120% 90% at 0% 0%, rgba(255,205,140,.05), transparent 60%),var(--paper)}
.section-title::before{
  content:'';position:absolute;top:-6px;left:50%;transform:translateX(-50%) rotate(-1.4deg);
  width:64px;height:13px;border-radius:3px;
  background:linear-gradient(180deg,rgba(226,196,152,.85),rgba(206,172,128,.62));
  box-shadow:0 2px 6px rgba(122,78,51,.16);
}
[data-theme="dark"] .section-title::before{background:linear-gradient(180deg,rgba(242,192,119,.26),rgba(242,192,119,.12))}
.section-line{width:34px;height:2px}
</style>
