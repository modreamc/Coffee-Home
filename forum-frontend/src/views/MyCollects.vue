<template>
  <div class="page-container">
    <h2 class="page-title"><svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="var(--accent-gold)" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg> 我的收藏</h2>
    <div class="post-list"><div v-for="(post,i) in posts" :key="post.id" class="fade-up" :style="{transitionDelay:`${i*0.08}s`}"><PostCard :post="post" /></div></div>
    <div v-if="posts.length===0" class="empty-state"><svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="1.5"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg><p style="margin-top:12px">还没有收藏帖子</p></div>
    <div v-if="total>size" class="pagination-wrap"><el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev,pager,next" @current-change="loadCollects" background /></div>
  </div>
</template>
<script setup>
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import { collectApi } from '../api/post'
import PostCard from '../components/PostCard.vue'
const posts = ref([]); const page = ref(1); const size = ref(20); const total = ref(0)
let observer = null
async function loadCollects(){
  try{
    const res = await collectApi.list({page:page.value,size:size.value})
    posts.value = res.list||[]; total.value = res.total||0
    nextTick(()=>{
      if(observer)observer.disconnect()
      observer = new IntersectionObserver(entries=>{entries.forEach(e=>{if(e.isIntersecting)e.target.classList.add('visible')})},{threshold:.1})
      document.querySelectorAll('.fade-up').forEach(el=>observer.observe(el))
    })
  }catch(e){posts.value=[];total.value=0}
}
onMounted(loadCollects)
onUnmounted(()=>{if(observer)observer.disconnect()})
</script>
<style scoped>
.page-title{font-size:24px;font-weight:800;margin-bottom:22px;display:flex;align-items:center;gap:10px}
.post-list{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:20px}
@media(max-width:1200px){.post-list{grid-template-columns:repeat(2,minmax(0,1fr))}}
@media(max-width:768px){.post-list{grid-template-columns:1fr}}
</style>
