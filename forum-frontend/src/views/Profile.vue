<template>
<div class="page-container profile-page">
  <div class="profile-card glass-card">
    <div class="profile-avatar">{{(user.nickname||user.username||'?').charAt(0)}}</div>
    <div class="profile-info">
      <h2 class="profile-name">{{user.nickname||user.username}}</h2>
      <p class="profile-meta">@{{user.username}}<span class="meta-dot">·</span>加入于 {{formatTime(user.createdAt)}}</p>
      <div class="profile-chips">
        <span class="profile-badge" v-if="user.role==='ADMIN'">⚓ 港湾管理员</span>
        <span class="level-chip">{{user.level||'港湾新人 🌱'}}</span>
        <span class="points-chip">⚡ {{user.points||0}} 积分</span>
      </div>
    </div>
  </div>

  <div class="harbor-tabs profile-tabs">
    <button class="harbor-tab" :class="{active:tab==='posts'}" @click="switchTab('posts')">📝 {{isSelf?'我的':'TA 的'}}帖子</button>
    <button v-if="isSelf" class="harbor-tab" :class="{active:tab==='collects'}" @click="switchTab('collects')">★ 我的收藏</button>
  </div>

  <div class="post-list">
    <div v-for="(post,i) in posts" :key="post.id" class="fade-up" :style="{transitionDelay:`${(i%6)*0.08}s`}"><PostCard :post="post"/></div>
  </div>
  <div v-if="posts.length===0&&!loading" class="empty-state"><p>{{tab==='posts'?'还没有发布过帖子':'还没有收藏'}}</p></div>
  <div v-if="total>size" class="pagination-wrap"><el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev,pager,next" @current-change="loadPosts" background/></div>
</div>
</template>
<script setup>
import{ref,computed,onMounted,nextTick,onUnmounted,watch}from'vue'
import{useRoute}from'vue-router'
import{postApi,collectApi,userApi}from'../api/post'
import{useUserStore}from'../stores/user'
import PostCard from'../components/PostCard.vue'
const route=useRoute();const userStore=useUserStore()
const user=ref({});const posts=ref([]);const tab=ref('posts');const page=ref(1);const size=ref(20);const total=ref(0);const loading=ref(false)
const isSelf=computed(()=>userStore.userInfo&&String(userStore.userInfo.id)===String(route.params.id))
let observer=null
function formatTime(t){if(!t)return'';const d=new Date(t);return`${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')}`}
async function loadUser(){try{user.value=await userApi.info(route.params.id)}catch{user.value={}}}
async function loadPosts(){
  loading.value=true
  try{
    const res=tab.value==='posts'
      ? await postApi.userPosts(route.params.id,{page:page.value,size:size.value})
      : await collectApi.list({page:page.value,size:size.value})
    posts.value=res.list||[];total.value=res.total||0;nextTick(()=>observePosts())
  }catch(e){posts.value=[];total.value=0}
  finally{loading.value=false}
}
function switchTab(t){if(tab.value===t)return;tab.value=t;page.value=1;loadPosts()}
function observePosts(){
  if(observer)observer.disconnect()
  observer=new IntersectionObserver(entries=>{entries.forEach(e=>{if(e.isIntersecting)e.target.classList.add('visible')})},{threshold:.1})
  document.querySelectorAll('.fade-up').forEach(el=>observer.observe(el))
}
watch(()=>route.params.id,()=>{page.value=1;loadUser();loadPosts()})
onMounted(()=>{loadUser();loadPosts();nextTick(()=>observePosts())})
onUnmounted(()=>{if(observer)observer.disconnect()})
</script>
<style scoped>
.profile-page{max-width:100%}
.profile-card{display:flex;align-items:center;gap:20px;padding:28px;margin-bottom:20px;background:linear-gradient(135deg,var(--bg-card),rgba(212,165,116,.08))}
.profile-avatar{width:72px;height:72px;border-radius:50%;background:linear-gradient(135deg,var(--accent-secondary),var(--accent-primary));display:flex;align-items:center;justify-content:center;font-size:30px;font-weight:900;color:#fff;box-shadow:var(--glow-warm)}
.profile-name{font-size:22px;font-weight:800;color:var(--text-primary)}
.profile-meta{font-size:13px;color:var(--text-muted);margin-top:6px}
.meta-dot{margin:0 6px}
.profile-badge{display:inline-block;margin-top:8px;padding:2px 10px;border-radius:10px;font-size:11px;font-weight:700;color:var(--accent-rose);background:rgba(192,128,128,.1);border:1px solid rgba(192,128,128,.3)}
.profile-chips{display:flex;gap:8px;margin-top:8px;flex-wrap:wrap}
.level-chip{display:inline-block;padding:2px 10px;border-radius:10px;font-size:11px;font-weight:700;color:var(--accent-warm);background:rgba(193,127,58,.12);border:1px solid rgba(193,127,58,.3)}
.points-chip{display:inline-block;padding:2px 10px;border-radius:10px;font-size:11px;font-weight:700;color:var(--accent-gold);background:rgba(212,165,116,.12);border:1px solid rgba(212,165,116,.3)}
.profile-tabs{margin-bottom:18px}
.post-list{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:20px}
@media(max-width:1200px){.post-list{grid-template-columns:repeat(2,minmax(0,1fr))}}
@media(max-width:768px){.post-list{grid-template-columns:1fr}}
</style>
