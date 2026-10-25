<template>
<div class="page-container topics-page">
  <div class="topics-head fade-up visible">
    <h2 class="topics-title"><span class="title-ic">#</span> 话题广场</h2>
    <p class="topics-sub">点亮你感兴趣的话题，看看大家的同好都在聊什么</p>
  </div>

  <div class="side-card topic-cloud fade-up visible">
    <div class="side-title"><span class="side-bar"/>热门话题</div>
    <div v-if="tags.length" class="cloud-wrap">
      <span v-for="t in tags" :key="t.name" class="topic-tag" :class="{active:activeTag===t.name}"
            :style="{fontSize:tagSize(t.postCount)}" @click="selectTag(t.name)">
        #{{t.name}} <em>{{t.postCount}}</em>
      </span>
    </div>
    <div v-else class="empty-state" style="padding:30px"><p>还没有话题，发布帖子时添加标签即可创建</p></div>
  </div>

  <div class="topic-posts" v-if="activeTag">
    <div class="topic-headline">
      <span class="hot-badge">话题</span>
      <h3># {{activeTag}}</h3>
      <span class="clear-btn" @click="activeTag=''">✕ 退出话题</span>
    </div>
    <div class="post-list">
      <div v-for="(post,i) in posts" :key="post.id" class="fade-up" :style="{transitionDelay:`${(i%6)*0.08}s`}"><PostCard :post="post"/></div>
    </div>
    <div v-if="posts.length===0&&!loading" class="empty-state"><p>这个话题下还没有帖子</p></div>
    <div v-if="total>size" class="pagination-wrap"><el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev,pager,next" @current-change="loadPosts" background/></div>
  </div>
  <div v-else class="side-card pick-hint">
    <p>👆 从上方选择一个话题，浏览其中的帖子</p>
  </div>
</div>
</template>
<script setup>
import{ref,onMounted,nextTick,onUnmounted}from'vue'
import{useRoute}from'vue-router'
import{postApi,tagApi}from'../api/post'
import PostCard from'../components/PostCard.vue'
const route=useRoute()
const tags=ref([]);const activeTag=ref('');const posts=ref([]);const page=ref(1);const size=ref(20);const total=ref(0);const loading=ref(false)
let observer=null
function tagSize(count){const c=count||0;return `${Math.min(12+c*1.6,22)}px`}
async function loadTags(){try{tags.value=await tagApi.list()||[]}catch{}}
async function loadPosts(){
  if(!activeTag.value)return
  loading.value=true
  try{const res=await postApi.list({tag:activeTag.value,page:page.value,size:size.value});posts.value=res.list||[];total.value=res.total||0;nextTick(()=>observePosts())}
  finally{loading.value=false}
}
function selectTag(name){activeTag.value=name;page.value=1;loadPosts()}
function observePosts(){
  if(observer)observer.disconnect()
  observer=new IntersectionObserver(entries=>{entries.forEach(e=>{if(e.isIntersecting)e.target.classList.add('visible')})},{threshold:.1})
  document.querySelectorAll('.fade-up').forEach(el=>observer.observe(el))
}
onMounted(()=>{loadTags().then(()=>{if(route.query.tag){activeTag.value=route.query.tag;loadPosts()}});nextTick(()=>observePosts())})
onUnmounted(()=>{if(observer)observer.disconnect()})
</script>
<style scoped>
.topics-page{max-width:100%}
.topics-head{margin-bottom:18px}
.topics-title{font-size:26px;font-weight:900;display:flex;align-items:center;gap:10px;color:var(--accent-primary)}
.title-ic{font-size:20px;color:var(--accent-warm)}
.topics-sub{font-size:14px;color:var(--text-secondary);margin-top:6px}
.topic-cloud{padding:20px}
.cloud-wrap{display:flex;flex-wrap:wrap;gap:10px;align-items:center}
.topic-tag{display:inline-flex;align-items:center;gap:5px;padding:5px 14px;border-radius:20px;background:var(--bg-surface);border:1px solid var(--border-light);color:var(--text-secondary);cursor:pointer;font-weight:600;transition:all var(--transition)}
.topic-tag:hover{border-color:var(--accent-warm);color:var(--accent-warm);transform:translateY(-2px)}
.topic-tag.active{background:linear-gradient(135deg,var(--accent-warm),var(--accent-secondary));color:#fff;border-color:transparent;box-shadow:var(--glow-warm)}
.topic-tag em{font-style:normal;font-size:11px;opacity:.75}
.topic-headline{display:flex;align-items:center;gap:10px;margin:20px 0 16px}
.topic-headline h3{font-size:20px;font-weight:800;color:var(--text-primary)}
.clear-btn{margin-left:auto;font-size:13px;color:var(--text-muted);cursor:pointer;padding:5px 12px;border-radius:16px;border:1px solid var(--border-light);transition:all var(--transition)}
.clear-btn:hover{color:var(--accent-rose);border-color:var(--accent-rose)}
.post-list{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:20px}
.pick-hint{text-align:center;padding:40px;color:var(--text-muted);font-size:14px}
@media(max-width:1200px){.post-list{grid-template-columns:repeat(2,minmax(0,1fr))}}
@media(max-width:768px){.post-list{grid-template-columns:1fr}}
</style>
