<template>
<article class="post-card" :class="{featured}" @click="goDetail">
  <span class="card-tape" aria-hidden="true"/>
  <span v-if="rank" class="rank-badge" :class="{top:rank<=3}">{{rank}}</span>
  <span v-if="featured" class="feature-tag">✦ 编辑推荐</span>

  <div class="card-header">
    <span :class="['category-tag',post.category==='GAME'?'game':'anime']">{{post.category==='GAME'?'🎮 单机游戏':'📺 动漫'}}</span>
    <span class="card-time">{{formatTime(post.createdAt)}}</span>
  </div>

  <h3 class="card-title">{{post.title}}</h3>

  <div class="card-tags" v-if="tagList.length"><span v-for="t in tagList" :key="t" class="card-tag">#{{t}}</span></div>

  <div class="card-content" v-if="post.content"><p class="card-excerpt">{{truncate(post.content, featured ? 160 : 86)}}</p></div>

  <div class="card-gallery" v-if="post.images&&imagesList.length>0&&imgOk">
    <div class="gallery-item" :style="{backgroundImage:`url(${imagesList[0]})`}">
      <span v-if="hasVideo" class="video-badge">▶ 视频</span>
      <div class="gallery-overlay"><svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg></div>
    </div>
  </div>

  <div class="card-foot">
    <div class="card-author">
      <span class="author-avatar">{{(post.authorName||'匿')[0]}}</span>
      <span class="author-name">{{post.authorName||'匿名'}}</span>
    </div>
    <div class="card-stats">
      <span class="stat-item" title="点赞"><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-3.3H14zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"/></svg><span class="stat-num">{{post.likeCount||0}}</span></span>
      <span class="stat-item" title="收藏"><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg><span class="stat-num">{{post.collectCount||0}}</span></span>
      <span class="stat-item" title="评论"><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg><span class="stat-num">{{post.commentCount||0}}</span></span>
    </div>
  </div>
</article>
</template>
<script setup>
import{computed,ref,onMounted}from'vue'
import{useRouter}from'vue-router'
const props=defineProps({
  post:Object,
  rank:{type:Number,default:0},
  featured:{type:Boolean,default:false}
})
const router=useRouter()
const imagesList=computed(()=>{try{return JSON.parse(props.post.images||'[]')}catch{return[]}})
const tagList=computed(()=>{try{return JSON.parse(props.post.tags||'[]')}catch{return[]}})
const hasVideo=computed(()=>{try{return JSON.parse(props.post.videos||'[]').length>0}catch{return false}})
const imgOk=ref(true)
onMounted(()=>{if(imagesList.value.length){const im=new Image();im.onerror=()=>{imgOk.value=false};im.src=imagesList.value[0]}})
function goDetail(){router.push(`/post/${props.post.id}`)}
function formatTime(t){if(!t)return'';const d=new Date(t);return`${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`}
function truncate(s,l){if(!s)return'';const p=s.replace(/<[^>]+>/g,'').replace(/\s+/g,' ').trim();return p.length>l?p.substring(0,l)+'…':p}
</script>
<style scoped>
.post-card{
  position:relative;
  display:flex;flex-direction:column;
  padding:20px 18px 16px;
  cursor:pointer;overflow:hidden;
  background:
    radial-gradient(120% 90% at 0% 0%, rgba(255,255,255,.5), transparent 58%),
    var(--paper);
  border:1px solid var(--border-light);
  border-radius:var(--radius);
  box-shadow:var(--shadow-sm), inset 0 1px 0 rgba(255,255,255,.4);
  transition:transform .45s var(--ease-soft), box-shadow .45s var(--ease-soft), border-color .45s ease;
}
[data-theme="dark"] .post-card{
  background:
    radial-gradient(120% 90% at 0% 0%, rgba(255,205,140,.06), transparent 58%),
    var(--paper);
  box-shadow:var(--shadow-sm), inset 0 1px 0 rgba(255,255,255,.04);
}
.post-card::before{
  content:'';position:absolute;left:0;top:18px;bottom:18px;width:3px;border-radius:0 3px 3px 0;
  background:linear-gradient(180deg,var(--accent-warm),var(--accent-secondary));
  transform:scaleY(0);transform-origin:top;transition:transform .45s var(--ease-soft);
  opacity:.9;
}
.post-card:hover{
  transform:translateY(-5px) rotate(-.3deg);
  border-color:var(--border-accent);
  box-shadow:var(--shadow-hover);
}
.post-card:hover::before{transform:scaleY(1)}
.post-card:active{transform:translateY(-2px) rotate(0) scale(.995)}

.card-tape{
  position:absolute;top:-6px;left:50%;width:64px;height:15px;transform:translateX(-50%) rotate(-1.5deg);
  background:linear-gradient(180deg, rgba(226,196,152,.85), rgba(206,172,128,.66));
  border-radius:3px;box-shadow:0 2px 8px rgba(122,78,51,.16);opacity:.85;
}
[data-theme="dark"] .card-tape{background:linear-gradient(180deg, rgba(242,192,119,.28), rgba(242,192,119,.14))}

.rank-badge{
  position:absolute;top:12px;right:12px;z-index:2;
  min-width:25px;height:25px;padding:0 7px;border-radius:9px;
  display:flex;align-items:center;justify-content:center;
  font-size:12.5px;font-weight:900;color:var(--text-muted);
  background:var(--bg-surface);border:1px solid var(--border-light);
}
.rank-badge.top{
  background:linear-gradient(135deg,#E8652D,#F0A33C);color:#fff;border:none;
  box-shadow:0 0 16px rgba(232,101,45,.45);
}
.feature-tag{
  position:absolute;top:12px;right:12px;z-index:2;
  padding:3px 10px;border-radius:999px;
  font-size:10.5px;font-weight:800;letter-spacing:.04em;
  color:var(--accent-warm);
  background:rgba(217,142,74,.14);
  border:1px solid rgba(217,142,74,.32);
  box-shadow:0 0 14px rgba(217,142,74,.18);
}

.card-header{display:flex;align-items:center;justify-content:space-between;gap:8px;margin-bottom:10px}
.card-time{font-size:11px;color:var(--text-muted);letter-spacing:.02em;white-space:nowrap}

.card-title{
  font-family:'Georgia','Songti SC','Noto Serif SC',serif;
  font-size:16.5px;font-weight:800;line-height:1.45;margin-bottom:8px;
  color:var(--text-primary);
  display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;
  transition:color var(--transition);
}
.post-card:hover .card-title{color:var(--accent-primary)}

.card-tags{display:flex;flex-wrap:wrap;gap:5px;margin-bottom:8px;max-height:22px;overflow:hidden}
.card-tag{
  font-size:10.5px;font-weight:700;color:var(--accent-warm);
  background:rgba(217,142,74,.10);border:1px solid rgba(217,142,74,.22);
  padding:1px 8px;border-radius:999px;white-space:nowrap;
}

.card-content{margin-bottom:12px}
.card-excerpt{
  font-size:12.8px;color:var(--text-secondary);line-height:1.7;
  display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;
}

.card-gallery{margin-top:auto;margin-bottom:14px}
.gallery-item{
  height:142px;border-radius:var(--radius-sm);overflow:hidden;
  background-size:cover;background-position:center;
  border:1px solid var(--border-light);
  box-shadow:inset 0 0 0 1px rgba(255,255,255,.14);
  transition:transform .8s var(--ease-soft);
  position:relative;
}
.post-card:hover .gallery-item{transform:scale(1.04)}
.video-badge{
  position:absolute;left:10px;bottom:10px;z-index:2;
  display:inline-flex;align-items:center;gap:3px;
  padding:2px 9px;border-radius:999px;
  font-size:10.5px;font-weight:800;color:#fff;
  background:rgba(20,16,18,.62);
  border:1px solid rgba(255,255,255,.22);
  backdrop-filter:blur(6px);
  box-shadow:0 2px 10px rgba(0,0,0,.35);
}
.post-card.featured .video-badge{bottom:16px;left:16px;font-size:11.5px}
.gallery-overlay{
  position:absolute;inset:0;display:flex;align-items:center;justify-content:center;
  background:linear-gradient(180deg, transparent, rgba(40,26,18,.45));
  color:#fff;opacity:0;transition:opacity var(--transition);
}
.gallery-item:hover .gallery-overlay{opacity:1}

.card-foot{
  display:flex;align-items:center;justify-content:space-between;gap:8px;
  margin-top:auto;padding-top:12px;
  border-top:1px dashed var(--border-light);
}
.card-author{display:flex;align-items:center;gap:6px;min-width:0}
.author-avatar{
  width:24px;height:24px;border-radius:50%;flex-shrink:0;
  background:linear-gradient(135deg,var(--accent-secondary),var(--accent-primary));
  display:flex;align-items:center;justify-content:center;
  font-size:11px;font-weight:800;color:var(--text-inverse);
  box-shadow:0 2px 8px -4px rgba(122,78,51,.9);
}
.author-name{font-size:12px;color:var(--text-secondary);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}

.card-stats{display:flex;gap:9px;flex-shrink:0}
.stat-item{display:flex;align-items:center;gap:3px;font-size:11.5px;color:var(--text-muted);transition:color var(--transition)}
.stat-item:hover{color:var(--accent-warm)}
.stat-num{font-weight:700}
.stat-item svg{width:13px;height:13px}

/* ══ 推荐位：横跨两列，做成左图右文的横向卡片 ══ */
.post-card.featured{
  display:grid;
  grid-template-columns:0.92fr 1.08fr;
  grid-template-areas:
    "media header"
    "media title"
    "media tags"
    "media excerpt"
    "media foot";
  column-gap:26px;
  row-gap:6px;
  align-content:stretch;
  padding:24px 24px 18px;
  background:
    radial-gradient(90% 120% at 0% 0%, rgba(255,236,205,.55), transparent 62%),
    radial-gradient(80% 120% at 100% 0%, rgba(217,142,74,.10), transparent 60%),
    var(--paper);
}
[data-theme="dark"] .post-card.featured{
  background:
    radial-gradient(90% 120% at 0% 0%, rgba(242,192,119,.10), transparent 62%),
    radial-gradient(80% 120% at 100% 0%, rgba(217,142,74,.08), transparent 60%),
    var(--paper);
}
.post-card.featured::before{top:24px;bottom:24px}
.post-card.featured .card-header{grid-area:header;padding-right:92px;margin-bottom:2px}
.post-card.featured .card-time{display:none}
.post-card.featured .card-title{grid-area:title;font-size:22px;line-height:1.4;margin-bottom:4px;-webkit-line-clamp:3}
.post-card.featured .card-tags{grid-area:tags;max-height:none;margin-bottom:2px}
.post-card.featured .card-content{grid-area:excerpt;margin-bottom:6px}
.post-card.featured .card-excerpt{font-size:13px;-webkit-line-clamp:3}
.post-card.featured .card-gallery{
  grid-area:media;margin:0;height:100%;min-height:236px;
}
.post-card.featured .gallery-item{height:100%;min-height:236px}
.post-card.featured .card-foot{grid-area:foot;padding-top:12px}
.post-card.featured .feature-tag{top:22px;right:22px}

@media (max-width:1100px){
  .post-card.featured{grid-template-columns:0.85fr 1.15fr;column-gap:20px}
  .post-card.featured .card-title{font-size:20px}
  .post-card.featured .card-gallery,
  .post-card.featured .gallery-item{min-height:210px}
}
@media (max-width:860px){
  .post-card.featured{
    grid-template-columns:1fr;
    grid-template-areas:
      "header"
      "title"
      "tags"
      "excerpt"
      "media"
      "foot";
    padding:20px 18px 16px;
  }
  .post-card.featured .card-header{padding-right:88px}
  .post-card.featured .card-title{font-size:19px}
  .post-card.featured .card-gallery,
  .post-card.featured .gallery-item{min-height:170px;height:170px}
}
@media (max-width:680px){
  .post-card.featured .card-title{font-size:18px}
  .post-card.featured .feature-tag{top:14px;right:14px}
}
</style>
