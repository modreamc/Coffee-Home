<template>
<div class="page-container search-page">
  <div class="search-head">
    <h2 class="search-title">🔍 搜索"{{keyword}}"</h2>
    <p class="search-sub">共找到 {{total}} 条结果</p>
  </div>
  <div class="post-list">
    <div v-for="(post,i) in posts" :key="post.id" class="fade-up" :style="{transitionDelay:`${(i%6)*0.08}s`}"><PostCard :post="post"/></div>
  </div>
  <div v-if="!loading&&posts.length===0" class="empty-state">
    <p>没有找到与"{{keyword}}"相关的内容</p>
    <p style="font-size:13px;margin-top:8px">换个关键词试试，或去 <router-link to="/topics">话题广场</router-link> 逛逛</p>
  </div>
  <div v-if="total>size" class="pagination-wrap">
    <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev,pager,next" @current-change="doSearch" background />
  </div>
</div>
</template>
<script setup>
import{ref,onMounted,nextTick,watch,onUnmounted}from'vue'
import{useRoute}from'vue-router'
import{postApi}from'../api/post'
import PostCard from'../components/PostCard.vue'
const route=useRoute()
const keyword=ref('');const posts=ref([]);const total=ref(0);const loading=ref(false)
const page=ref(1);const size=ref(20)
let observer=null
async function doSearch(){
  keyword.value=route.query.keyword||''
  if(!keyword.value){posts.value=[];total.value=0;return}
  loading.value=true
  try{
    // 后端已改为分页接口，返回 { list, total }
    const res=await postApi.search(keyword.value,{page:page.value,size:size.value})
    posts.value=res.list||[];total.value=res.total||0
    nextTick(()=>observePosts())
  }catch(e){
    posts.value=[];total.value=0
  }finally{
    loading.value=false
  }
}
function observePosts(){
  if(observer)observer.disconnect()
  observer=new IntersectionObserver(entries=>{entries.forEach(e=>{if(e.isIntersecting)e.target.classList.add('visible')})},{threshold:.1})
  document.querySelectorAll('.fade-up').forEach(el=>observer.observe(el))
}
watch(()=>route.query.keyword,()=>{page.value=1;doSearch()})
onMounted(()=>{doSearch();nextTick(()=>observePosts())})
onUnmounted(()=>{if(observer)observer.disconnect()})
</script>
<style scoped>
.search-page{max-width:100%}
.search-head{margin-bottom:24px;display:flex;flex-direction:column;gap:6px}
.search-title{font-size:26px;font-weight:900;color:var(--text-primary)}
.search-sub{font-size:13px;color:var(--text-muted)}
.post-list{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:20px}
@media(max-width:1200px){.post-list{grid-template-columns:repeat(2,minmax(0,1fr))}}
@media(max-width:768px){.post-list{grid-template-columns:1fr}}
</style>
