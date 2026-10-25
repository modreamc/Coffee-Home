<template>
<div class="home-wrap">
<!-- ═══ 港湾小屋 hero ═══ -->
<section class="hero-section">
  <div class="hero-glow" aria-hidden="true"/>
  <div class="hero-inner">
    <div class="hero-copy">
      <div class="hero-badge"><span class="badge-dot"/>⚓ COFFEE HARBOR · 咖啡港湾</div>
      <h1 class="hero-title">每一盏灯下，<br/>都有同好</h1>
      <p class="hero-sub">游戏与动漫爱好者的深夜港湾 —— 冲杯咖啡，上岸聊聊你的热爱。</p>
      <div class="hero-actions">
        <router-link v-if="!userStore.isLoggedIn" to="/register" class="warm-btn">登船加入</router-link>
        <router-link v-else to="/create" class="warm-btn">✍ 发布帖子</router-link>
        <router-link to="/game" class="warm-btn outline">🎮 单机游戏</router-link>
        <router-link to="/anime" class="warm-btn outline">📺 动漫</router-link>
      </div>
      <div class="hero-meta">
        <span class="meta-item"><b>{{ total }}</b> 篇帖子</span>
        <span class="meta-sep"/>
        <span class="meta-item">☕ 今天也要好好喝咖啡</span>
      </div>
    </div>

    <!-- 纯 CSS 小屋：窗、灯、桌、咖啡、黑胶、绿植 -->
    <div class="hero-room" aria-hidden="true">
      <span class="room-wall"/>
      <span class="string-lights"><i v-for="n in 7" :key="n"/></span>
      <div class="room-window">
        <span class="sky"/>
        <span class="sun"/>
        <span class="moon"/>
        <span class="cloud c1"/>
        <span class="cloud c2"/>
        <span class="rain-lines"><i v-for="n in 9" :key="n"/></span>
        <span class="tree-shadow"/>
        <span class="glass-fog"/>
        <span class="glass-streaks"><i v-for="n in 6" :key="n"/></span>
        <span class="glass-droplets"><i v-for="n in 14" :key="n"/></span>
        <span class="window-bar h"/>
        <span class="window-bar v"/>
      </div>
      <span class="room-curtain left"/>
      <span class="room-curtain right"/>
      <div class="room-lamp">
        <span class="cord"/>
        <span class="shade"/>
        <span class="bulb"/>
        <span class="halo"/>
      </div>
      <div class="room-desk">
        <span class="desk-top"/>
        <div class="mug">
          <span class="mug-steam"><i/><i/><i/></span>
          <span class="mug-body"/>
          <span class="mug-handle"/>
        </div>
        <div class="vinyl">
          <span class="disc"/>
          <span class="label"/>
          <span class="tonearm"/>
        </div>
        <div class="plant">
          <span class="leaf l1"/><span class="leaf l2"/><span class="leaf l3"/>
          <span class="pot"/>
        </div>
      </div>
      <span class="room-rug"/>
      <span class="room-note">lo-fi beats · coffee · 同好</span>
    </div>
  </div>
</section>

<div class="page-container">
<div class="home-layout">
  <!-- 主内容流 -->
  <div class="home-main">
    <div class="stream-head">
      <div class="stream-title"><span class="stream-dot"/>港湾动态<span class="stream-sub">共 {{ total }} 篇 · 慢慢翻</span></div>
      <div class="harbor-tabs">
        <button class="harbor-tab" :class="{active:activeTab==='latest'}" @click="switchTab('latest')">✨ 最新</button>
        <button class="harbor-tab" :class="{active:activeTab==='hot'}" @click="switchTab('hot')">🔥 热榜</button>
      </div>
    </div>

    <div class="post-list">
      <div v-for="(post,i) in posts" :key="post.id" class="fade-up" :class="{'feature-slot': activeTab==='latest' && i===0}" :style="{transitionDelay:`${(i%6)*0.06}s`}"><PostCard :post="post" :rank="activeTab==='hot'?i+1:0" :featured="activeTab==='latest' && i===0"/></div>
    </div>

    <div v-if="posts.length===0&&!loading" class="empty-state">
      <svg width="52" height="52" viewBox="0 0 24 24" fill="none" stroke="var(--text-muted)" stroke-width="1.3"><path d="M17 8h1a4 4 0 1 1 0 8h-1"/><path d="M3 8h14v9a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4Z"/><path d="M6 2v2M10 2v2M14 2v2"/></svg>
      <p style="margin-top:14px">港口还空着，来煮第一杯咖啡吧</p>
    </div>

    <div v-if="total>size" class="pagination-wrap"><el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="prev,pager,next" @current-change="loadPosts" background/></div>
  </div>

  <!-- 右侧边栏 -->
  <aside class="home-side">
    <!-- 黑胶机：氛围担当 -->
    <div class="side-card now-playing">
      <div class="side-title"><span class="side-bar"/>正在播放</div>
      <div class="np-row">
        <div class="np-disc"><span class="np-hole"/></div>
        <div class="np-meta">
          <b>Midnight Coffee</b>
          <em>Lo-Fi Harbor · 03:24</em>
        </div>
      </div>
      <div class="np-bars" aria-hidden="true"><i v-for="n in 14" :key="n"/></div>
    </div>

    <div class="side-card">
      <div class="side-title"><span class="side-bar"/>板块入口</div>
      <router-link to="/game" class="board-entry"><span class="board-icon">🎮</span><span><b>单机游戏</b><em>攻略 · 测评 · 安利</em></span><i>→</i></router-link>
      <router-link to="/anime" class="board-entry"><span class="board-icon">📺</span><span><b>动漫</b><em>番剧 · 漫评 · 二创</em></span><i>→</i></router-link>
      <router-link to="/create" class="board-entry primary"><span class="board-icon">✍</span><span><b>发布帖子</b><em>把你的热爱写下来</em></span><i>→</i></router-link>
    </div>

    <div class="side-card" v-if="hotPosts.length">
      <div class="side-title"><span class="side-bar"/>🔥 本周热榜</div>
      <div v-for="(p,idx) in hotPosts" :key="p.id" class="hot-item" @click="$router.push(`/post/${p.id}`)">
        <span class="hot-rank" :class="{top:idx<3}">{{idx+1}}</span>
        <span class="hot-title">{{p.title}}</span>
        <span class="hot-heat">{{p.likeCount*3+p.collectCount*2+p.commentCount*4}}</span>
      </div>
    </div>

    <div class="side-card" v-if="sideTags.length">
      <div class="side-title"><span class="side-bar"/>热门话题</div>
      <div class="side-tags">
        <span v-for="t in sideTags" :key="t.name" class="side-tag" @click="goTopic(t.name)">#{{t.name}} <em>{{t.postCount}}</em></span>
      </div>
    </div>

    <div class="side-card">
      <div class="side-title"><span class="side-bar"/>港湾公告</div>
      <template v-if="notices.length">
        <div v-for="n in notices" :key="n.id" class="notice-item"><span class="notice-dot"/><div><b>{{n.title}}</b><p>{{n.content}}</p></div></div>
      </template>
      <template v-else>
      <div class="notice-item"><span class="notice-dot"/>欢迎来到咖啡港湾，愿你在这里找到同好</div>
      <div class="notice-item"><span class="notice-dot"/>发布攻略 / 求助 / 安利帖，点亮更多灯塔</div>
      </template>
    </div>
  </aside>
</div>
</div>
</div>
</template>
<script setup>
import{ref,onMounted,nextTick,onUnmounted}from'vue'
import{useRouter}from'vue-router'
import{postApi,tagApi,announcementApi}from'../api/post'
import{useUserStore}from'../stores/user'
import PostCard from'../components/PostCard.vue'
const router=useRouter()
const userStore=useUserStore();const posts=ref([]);const hotPosts=ref([]);const sideTags=ref([]);const notices=ref([])
const activeTab=ref('latest');const page=ref(1);const size=ref(12);const total=ref(0);const loading=ref(false)
let observer=null
async function loadPosts(){
loading.value=true
try{const res=await postApi.list({sort:activeTab.value==='hot'?'hot':undefined,page:page.value,size:size.value});posts.value=res.list||[];total.value=res.total||0;nextTick(()=>observePosts())}
catch(e){posts.value=[];total.value=0}
finally{loading.value=false}
}
async function loadHotSide(){
try{const res=await postApi.list({sort:'hot',page:1,size:5});hotPosts.value=res.list||[]}catch{}
try{sideTags.value=(await tagApi.list())||[]}catch{}
try{notices.value=(await announcementApi.list())||[]}catch{}
}
function goTopic(name){router.push({path:'/topics',query:{tag:name}})}
function switchTab(tab){if(activeTab.value===tab)return;activeTab.value=tab;page.value=1;loadPosts()}
function observePosts(){
if(observer)observer.disconnect()
observer=new IntersectionObserver(entries=>{entries.forEach(e=>{if(e.isIntersecting)e.target.classList.add('visible')})},{threshold:.1})
document.querySelectorAll('.fade-up').forEach(el=>observer.observe(el))
}
onMounted(async()=>{await loadPosts();loadHotSide();nextTick(()=>observePosts())})
onUnmounted(()=>{if(observer)observer.disconnect()})
</script>
<style scoped>
.home-wrap{position:relative;z-index:1}

/* ═══ Hero ═══ */
.hero-section{
  position:relative;overflow:hidden;
  padding:64px 20px 78px;
  border-bottom:1px solid var(--border-light);
  background:
    radial-gradient(120% 90% at 12% 0%, rgba(255,224,178,.42), transparent 58%),
    radial-gradient(90% 80% at 92% 12%, rgba(199,154,107,.28), transparent 62%),
    linear-gradient(180deg, #F8EEDF 0%, #F3E4D0 58%, var(--bg-body) 100%);
}
[data-theme="dark"] .hero-section{
  background:
    radial-gradient(110% 90% at 12% 0%, rgba(242,192,119,.10), transparent 58%),
    radial-gradient(90% 80% at 92% 12%, rgba(90,120,140,.22), transparent 62%),
    linear-gradient(180deg, #1B161F 0%, #191419 58%, var(--bg-body) 100%);
}
/* 墙面细纹 + 踢脚线 */
.hero-section::before{
  content:'';position:absolute;inset:0;pointer-events:none;
  background:repeating-linear-gradient(90deg, rgba(122,78,51,.028) 0 1px, transparent 1px 7px);
  mask-image:linear-gradient(180deg,#000 0%,#000 62%,transparent 92%);
  -webkit-mask-image:linear-gradient(180deg,#000 0%,#000 62%,transparent 92%);
}
.hero-section::after{
  content:'';position:absolute;left:0;right:0;bottom:0;height:26px;
  background:linear-gradient(180deg, rgba(122,78,51,.10), transparent);
}
[data-theme="dark"] .hero-section::after{background:linear-gradient(180deg, rgba(0,0,0,.28), transparent)}
.hero-glow{
  position:absolute;top:-12%;left:-6%;width:52%;height:120%;pointer-events:none;
  background:radial-gradient(closest-side, rgba(255,214,150,.42), transparent 72%);
  filter:blur(18px);opacity:var(--sun-opacity);transition:opacity .6s ease;
}
[data-theme="dark"] .hero-glow{
  opacity:1;
  background:radial-gradient(closest-side, rgba(242,192,119,.16), transparent 72%);
}

.hero-inner{
  position:relative;z-index:2;max-width:1280px;margin:0 auto;
  display:grid;grid-template-columns:1.02fr .98fr;gap:40px;align-items:center;
}

/* 文案 */
.hero-copy{max-width:560px}
.hero-badge{
  display:inline-flex;align-items:center;gap:8px;
  padding:6px 16px;border-radius:999px;font-size:11.5px;font-weight:800;letter-spacing:.14em;
  background:rgba(255,251,244,.66);border:1px solid rgba(122,78,51,.22);
  color:var(--accent-primary);backdrop-filter:blur(6px);
  box-shadow:0 6px 18px -14px rgba(122,78,51,.8);
  margin-bottom:20px;
}
[data-theme="dark"] .hero-badge{background:rgba(38,31,40,.6);border-color:rgba(232,190,140,.26);color:var(--accent-warm)}
.badge-dot{
  width:7px;height:7px;border-radius:50%;background:var(--accent-warm);
  box-shadow:0 0 10px rgba(217,142,74,.8);animation:dotPulse 2.6s ease-out infinite;
}
.hero-title{
  font-family:'Georgia','Songti SC','Noto Serif SC',serif;
  font-size:clamp(34px,4.6vw,56px);
  font-weight:900;line-height:1.18;letter-spacing:-.01em;
  color:var(--text-primary);
  text-shadow:0 2px 0 rgba(255,255,255,.35);
  margin-bottom:16px;
}
[data-theme="dark"] .hero-title{
  color:#FBF1E2;
  text-shadow:0 0 40px rgba(242,192,119,.22);
}
.hero-title::after{
  content:'';display:block;width:96px;height:5px;border-radius:99px;margin-top:18px;
  background:linear-gradient(90deg,var(--accent-warm),var(--accent-secondary),transparent);
  box-shadow:0 0 16px rgba(217,142,74,.4);
}
.hero-sub{
  font-size:15.5px;line-height:1.85;color:var(--text-secondary);margin-bottom:28px;
}
.hero-actions{display:flex;gap:12px;flex-wrap:wrap;margin-bottom:26px}
.hero-meta{display:flex;align-items:center;gap:12px;font-size:12.5px;color:var(--text-muted)}
.hero-meta b{color:var(--accent-primary);font-size:15px;font-weight:800}
.meta-sep{width:4px;height:4px;border-radius:50%;background:var(--accent-secondary);opacity:.7}

/* ═══ CSS 小屋 ═══ */
.hero-room{
  position:relative;height:392px;width:100%;max-width:470px;margin-left:auto;
  border-radius:var(--radius-lg);
  background:
    radial-gradient(90% 70% at 50% 0%, rgba(255,236,206,.6), transparent 70%),
    linear-gradient(180deg, rgba(255,250,242,.5), rgba(233,213,190,.35));
  border:1px solid rgba(122,78,51,.16);
  box-shadow:0 30px 60px -34px rgba(90,60,40,.55), inset 0 1px 0 rgba(255,255,255,.6);
  overflow:hidden;
  animation:roomIn .9s var(--ease-soft) both;
}
[data-theme="dark"] .hero-room{
  background:
    radial-gradient(90% 70% at 50% 0%, rgba(242,192,119,.10), transparent 70%),
    linear-gradient(180deg, rgba(50,40,52,.6), rgba(26,21,28,.7));
  border-color:rgba(232,190,140,.16);
  box-shadow:0 34px 70px -36px rgba(0,0,0,.9), inset 0 1px 0 rgba(255,255,255,.05);
}
@keyframes roomIn{from{opacity:0;transform:translateY(18px) scale(.97)}to{opacity:1;transform:none}}
.room-wall{
  position:absolute;inset:0;
  background:
    radial-gradient(90% 60% at 50% 0%, rgba(255,255,255,.45), transparent 72%),
    repeating-linear-gradient(90deg, rgba(122,78,51,.035) 0 1px, transparent 1px 8px);
}
[data-theme="dark"] .room-wall{
  background:
    radial-gradient(90% 60% at 50% 0%, rgba(242,192,119,.07), transparent 72%),
    repeating-linear-gradient(90deg, rgba(232,190,140,.02) 0 1px, transparent 1px 8px);
}

/* 串灯 */
.string-lights{position:absolute;left:0;right:0;top:4px;height:46px;pointer-events:none}
.string-lights::before{
  content:'';position:absolute;left:4%;right:4%;top:0;height:28px;
  border-bottom:1.5px solid rgba(122,78,51,.34);
  border-radius:0 0 50% 50%/0 0 100% 100%;
}
[data-theme="dark"] .string-lights::before{border-color:rgba(232,190,140,.26)}
.string-lights i{
  position:absolute;width:7px;height:7px;border-radius:50%;
  background:radial-gradient(circle,#FFF9E7,#FFC46B);
  box-shadow:0 0 12px 4px rgba(255,200,120,.55);
  animation:twinkle 3.2s ease-in-out infinite;
}
[data-theme="dark"] .string-lights i{box-shadow:0 0 12px 4px rgba(242,192,119,.6)}
.string-lights i:nth-child(1){left:8%;top:20px}
.string-lights i:nth-child(2){left:22%;top:27px;animation-delay:-.5s}
.string-lights i:nth-child(3){left:36%;top:31px;animation-delay:-1s}
.string-lights i:nth-child(4){left:50%;top:32px;animation-delay:-1.5s}
.string-lights i:nth-child(5){left:64%;top:31px;animation-delay:-2s}
.string-lights i:nth-child(6){left:78%;top:27px;animation-delay:-2.5s}
.string-lights i:nth-child(7){left:92%;top:20px;animation-delay:-3s}
@keyframes twinkle{0%,100%{opacity:.55;transform:scale(.9)}50%{opacity:1;transform:scale(1.15)}}

/* 窗 */
.room-window{
  position:absolute;left:6%;top:8%;width:62%;height:52%;
  border-radius:12px;overflow:hidden;
  background:linear-gradient(180deg,var(--sky-top),var(--sky-bottom));
  box-shadow:inset 0 0 0 9px var(--wood), inset 0 0 0 11px rgba(255,255,255,.16), 0 22px 40px -24px rgba(0,0,0,.6);
  transition:background .6s ease;
}
.sky{
  position:absolute;inset:0;
  background:linear-gradient(180deg,var(--sky-top),var(--sky-bottom));
}
.sun{
  position:absolute;right:16%;top:12%;width:46px;height:46px;border-radius:50%;
  background:radial-gradient(circle at 38% 34%,#FFFDF0,#FFC46B);
  box-shadow:0 0 54px 24px rgba(255,196,110,.45);
  opacity:var(--sun-opacity);transition:opacity .6s ease;
  animation:breathe 7s ease-in-out infinite;
}
.moon{
  position:absolute;right:22%;top:15%;width:34px;height:34px;border-radius:50%;
  background:radial-gradient(circle at 36% 32%,#FFF9E8,#E8D9B0);
  box-shadow:0 0 30px 10px rgba(240,220,170,.35);
  opacity:var(--moon-opacity);transition:opacity .6s ease;
}
.moon::after{
  content:'';position:absolute;left:10px;top:-3px;width:28px;height:28px;border-radius:50%;
  background:var(--sky-bottom);opacity:.92;
}
.cloud{
  position:absolute;height:16px;border-radius:99px;
  background:
    radial-gradient(ellipse 62% 120% at 26% 62%, rgba(255,255,255,.78), transparent 72%),
    radial-gradient(ellipse 48% 112% at 55% 44%, rgba(255,255,255,.66), transparent 72%),
    radial-gradient(ellipse 36% 96% at 82% 64%, rgba(255,255,255,.52), transparent 72%);
  filter:blur(1.4px);opacity:.9;
}
.cloud.c1{width:74px;left:12%;top:22%;animation:cloudDrift 26s linear infinite}
.cloud.c2{width:50px;left:48%;top:44%;opacity:.6;animation:cloudDrift 34s linear infinite reverse}
[data-theme="dark"] .cloud{
  background:
    radial-gradient(ellipse 62% 120% at 26% 62%, rgba(170,200,215,.34), transparent 72%),
    radial-gradient(ellipse 48% 112% at 55% 44%, rgba(170,200,215,.28), transparent 72%),
    radial-gradient(ellipse 36% 96% at 82% 64%, rgba(170,200,215,.22), transparent 72%);
  opacity:.7;
}
@keyframes cloudDrift{0%{transform:translateX(-8px)}50%{transform:translateX(14px)}100%{transform:translateX(-8px)}}
.rain-lines{position:absolute;inset:0;opacity:var(--rain-opacity);transition:opacity .6s ease}
.rain-lines i{
  position:absolute;top:-20%;width:1px;height:34px;border-radius:2px;
  background:linear-gradient(180deg,transparent,rgba(200,228,240,.7),transparent);
  animation:rainInWindow 1.3s linear infinite;
}
.rain-lines i:nth-child(1){left:12%;animation-delay:-.2s}
.rain-lines i:nth-child(2){left:24%;animation-delay:-.7s;height:26px}
.rain-lines i:nth-child(3){left:36%;animation-delay:-.4s}
.rain-lines i:nth-child(4){left:47%;animation-delay:-1s;height:30px}
.rain-lines i:nth-child(5){left:58%;animation-delay:-.1s}
.rain-lines i:nth-child(6){left:69%;animation-delay:-.8s;height:24px}
.rain-lines i:nth-child(7){left:79%;animation-delay:-.5s}
.rain-lines i:nth-child(8){left:88%;animation-delay:-1.1s;height:28px}
.rain-lines i:nth-child(9){left:94%;animation-delay:-.3s}
@keyframes rainInWindow{from{transform:translateY(-20px)}to{transform:translateY(190px)}}

/* 窗外树影（白天投在玻璃上，夜里隐去） */
.tree-shadow{
  position:absolute;inset:-12% -8%;z-index:2;pointer-events:none;
  background:
    radial-gradient(ellipse 34px 20px at 24% 28%, rgba(86,104,66,.30), transparent 68%),
    radial-gradient(ellipse 26px 16px at 40% 44%, rgba(86,104,66,.24), transparent 68%),
    radial-gradient(ellipse 44px 24px at 74% 22%, rgba(86,104,66,.26), transparent 70%),
    radial-gradient(ellipse 30px 18px at 86% 56%, rgba(86,104,66,.22), transparent 70%),
    radial-gradient(ellipse 22px 14px at 56% 68%, rgba(86,104,66,.18), transparent 70%);
  filter:blur(2.5px);
  opacity:calc(var(--sun-opacity) * .9);
  transform-origin:top center;
  animation:leafShadow 16s ease-in-out infinite;
  mix-blend-mode:multiply;
}
[data-theme="dark"] .tree-shadow{mix-blend-mode:normal}
@keyframes leafShadow{
  0%,100%{transform:rotate(-.8deg) translateX(-2px);opacity:calc(var(--sun-opacity) * .75)}
  50%    {transform:rotate(.9deg) translateX(3px);opacity:calc(var(--sun-opacity) * .95)}
}

/* 玻璃上的雾气与雨痕 */
.glass-fog{
  position:absolute;left:0;right:0;bottom:0;height:48%;z-index:3;pointer-events:none;
  background:linear-gradient(180deg, transparent, rgba(208,232,238,.18) 58%, rgba(208,232,238,.34));
  filter:blur(1.2px);
  opacity:var(--rain-opacity);
  transition:opacity .6s ease;
}
.glass-streaks{position:absolute;inset:0;z-index:4;pointer-events:none;opacity:var(--rain-opacity);transition:opacity .6s ease}
.glass-streaks i{
  position:absolute;top:-8%;width:1.5px;height:26px;border-radius:99px;
  background:linear-gradient(180deg, transparent, rgba(228,246,250,.65), transparent);
  animation:streakSlide 7s linear infinite;
  opacity:0;
}
.glass-streaks i:nth-child(1){left:18%;animation-delay:-.5s;animation-duration:6.4s}
.glass-streaks i:nth-child(2){left:33%;animation-delay:-3.2s;animation-duration:8.2s;height:20px}
.glass-streaks i:nth-child(3){left:47%;animation-delay:-1.6s;animation-duration:7.1s}
.glass-streaks i:nth-child(4){left:62%;animation-delay:-4.8s;animation-duration:9s;height:32px}
.glass-streaks i:nth-child(5){left:76%;animation-delay:-2.4s;animation-duration:6.8s}
.glass-streaks i:nth-child(6){left:89%;animation-delay:-5.6s;animation-duration:8.6s;height:22px}
@keyframes streakSlide{
  0%  {transform:translateY(-20px);opacity:0}
  15% {opacity:.7}
  85% {opacity:.35}
  100%{transform:translateY(150px);opacity:0}
}
.glass-droplets{position:absolute;inset:0;z-index:4;pointer-events:none;opacity:var(--rain-opacity);transition:opacity .6s ease}
.glass-droplets i{
  position:absolute;width:3px;height:4px;border-radius:50%;
  background:radial-gradient(circle at 34% 30%, rgba(255,255,255,.95), rgba(198,230,240,.45));
  box-shadow:0 1px 2px rgba(10,25,35,.25);
  animation:dropletDrift 9s ease-in-out infinite;
}
.glass-droplets i:nth-child(1){left:8%;top:22%;animation-delay:-1s}
.glass-droplets i:nth-child(2){left:14%;top:58%;animation-delay:-3.4s;width:4px;height:5px}
.glass-droplets i:nth-child(3){left:22%;top:36%;animation-delay:-5.2s}
.glass-droplets i:nth-child(4){left:29%;top:72%;animation-delay:-2.1s}
.glass-droplets i:nth-child(5){left:37%;top:18%;animation-delay:-6.8s;width:2px;height:3px}
.glass-droplets i:nth-child(6){left:44%;top:52%;animation-delay:-4.3s}
.glass-droplets i:nth-child(7){left:52%;top:30%;animation-delay:-7.5s}
.glass-droplets i:nth-child(8){left:59%;top:66%;animation-delay:-0.6s;width:4px;height:5px}
.glass-droplets i:nth-child(9){left:66%;top:40%;animation-delay:-3.9s}
.glass-droplets i:nth-child(10){left:73%;top:20%;animation-delay:-6.1s}
.glass-droplets i:nth-child(11){left:80%;top:55%;animation-delay:-2.8s;width:2px;height:3px}
.glass-droplets i:nth-child(12){left:86%;top:34%;animation-delay:-5.7s}
.glass-droplets i:nth-child(13){left:92%;top:68%;animation-delay:-1.9s}
.glass-droplets i:nth-child(14){left:70%;top:78%;animation-delay:-7.9s;width:4px;height:5px}
@keyframes dropletDrift{
  0%,100%{transform:translateY(0) scale(1);opacity:.75}
  50%    {transform:translateY(7px) scale(1.08);opacity:.95}
}
.window-bar{
  position:absolute;background:var(--wood);opacity:.9;box-shadow:0 1px 0 rgba(255,255,255,.14) inset;
}
.window-bar.h{left:0;right:0;top:50%;height:7px;transform:translateY(-50%)}
.window-bar.v{top:0;bottom:0;left:50%;width:7px;transform:translateX(-50%)}

/* 窗帘 */
.room-curtain{
  position:absolute;top:5%;width:10%;height:54%;border-radius:8px;
  background:linear-gradient(90deg, rgba(199,154,107,.5), rgba(160,116,78,.36));
  box-shadow:inset -3px 0 10px rgba(90,60,40,.14);
  transform-origin:top center;
  animation:curtainSway 9s ease-in-out infinite;
  opacity:.72;
}
.room-curtain.left{left:2%;border-radius:10px 4px 14px 6px}
.room-curtain.right{left:61%;width:9%;animation-delay:-3s;border-radius:4px 10px 6px 14px}
[data-theme="dark"] .room-curtain{background:linear-gradient(90deg, rgba(90,68,58,.7), rgba(60,45,40,.55));opacity:.7}
@keyframes curtainSway{0%,100%{transform:rotate(-.8deg) scaleY(1)}50%{transform:rotate(.8deg) scaleY(1.012)}}

/* 吊灯 */
.room-lamp{position:absolute;right:9%;top:0;width:96px;height:190px}
.room-lamp .cord{position:absolute;left:50%;top:0;width:2px;height:64px;background:linear-gradient(180deg,rgba(90,60,40,.35),var(--wood));transform:translateX(-50%)}
.room-lamp .shade{
  position:absolute;left:50%;top:62px;width:64px;height:34px;transform:translateX(-50%);
  background:linear-gradient(180deg,#C98A4B,#8A5A38);
  border-radius:32px 32px 6px 6px;
  box-shadow:inset 0 -6px 12px rgba(0,0,0,.25), 0 6px 16px -8px rgba(0,0,0,.6);
}
[data-theme="dark"] .room-lamp .shade{background:linear-gradient(180deg,#B98B5E,#5A4030)}
.room-lamp .bulb{
  position:absolute;left:50%;top:94px;width:12px;height:12px;border-radius:50%;
  transform:translateX(-50%);
  background:radial-gradient(circle,#FFF6DC,#FFD08A);
  box-shadow:0 0 18px 6px rgba(255,205,130,.55);
  animation:lampFlicker 6s ease-in-out infinite;
}
.room-lamp .halo{
  position:absolute;left:50%;top:52px;width:210px;height:180px;transform:translateX(-50%);
  background:radial-gradient(closest-side, rgba(255,205,130,.42), transparent 74%);
  filter:blur(6px);opacity:var(--lamp-opacity);
  animation:lampFlicker 6s ease-in-out infinite;
}

/* 桌面 */
.room-desk{position:absolute;left:0;right:0;bottom:13%;height:74px}
.room-desk .desk-top{
  position:absolute;left:0;right:0;top:0;height:16px;border-radius:4px;
  background:linear-gradient(180deg,var(--wood),var(--wood-dark));
  box-shadow:0 -2px 0 rgba(255,255,255,.14) inset, 0 18px 30px -22px rgba(0,0,0,.8);
}
.room-desk::after{
  content:'';position:absolute;left:8%;right:8%;top:16px;height:58px;
  background:linear-gradient(180deg, rgba(90,60,40,.28), transparent);
  filter:blur(6px);
}

/* 咖啡杯 + 热气 */
.mug{position:absolute;left:16%;bottom:64px;width:40px;height:34px}
.mug-body{
  position:absolute;left:0;bottom:0;width:28px;height:22px;border-radius:3px 3px 10px 10px;
  background:linear-gradient(160deg,#FFFDF8,#EADBC8);
  box-shadow:inset 0 -4px 0 rgba(0,0,0,.06), 0 6px 12px -8px rgba(90,60,40,.9);
}
.mug-body::after{
  content:'';position:absolute;left:3px;right:3px;top:3px;height:5px;border-radius:99px;
  background:linear-gradient(180deg,#6B4A32,#8A5A3B);
}
.mug-handle{
  position:absolute;left:26px;bottom:7px;width:11px;height:12px;border-radius:50%;
  border:2.5px solid #EADBC8;border-left-color:transparent;transform:rotate(-14deg);
}
.mug-steam{position:absolute;left:4px;bottom:24px;width:20px;height:20px;display:flex;justify-content:space-between}
.mug-steam i{
  width:4px;height:4px;border-radius:50%;background:rgba(255,250,240,.85);
  filter:blur(.4px);opacity:0;animation:steamRise 3.4s ease-in-out infinite;
}
.mug-steam i:nth-child(2){animation-delay:1.1s}
.mug-steam i:nth-child(3){animation-delay:2.2s}

/* 黑胶唱片机 */
.vinyl{position:absolute;right:20%;bottom:70px;width:62px;height:56px}
.vinyl .disc{
  position:absolute;left:0;bottom:0;width:52px;height:52px;border-radius:50%;
  background:
    repeating-radial-gradient(circle at 50% 50%, rgba(255,255,255,.055) 0 1px, transparent 1px 3px),
    radial-gradient(circle at 38% 32%, #4A4038, #1C1815 70%);
  box-shadow:0 8px 18px -10px rgba(0,0,0,.9), inset 0 0 0 1px rgba(255,255,255,.06);
  animation:vinylSpin 3.6s linear infinite;
}
.vinyl .label{
  position:absolute;left:17px;bottom:17px;width:18px;height:18px;border-radius:50%;
  background:linear-gradient(135deg,var(--accent-warm),var(--accent-rose));
  box-shadow:0 0 10px rgba(217,142,74,.5);
}
.vinyl .label::after{
  content:'';position:absolute;left:50%;top:50%;width:4px;height:4px;border-radius:50%;
  background:#241E27;transform:translate(-50%,-50%);
}
.vinyl .tonearm{
  position:absolute;right:0;top:0;width:2px;height:40px;border-radius:2px;
  background:linear-gradient(180deg,#D9C7B0,#8A5A3B);
  transform:rotate(24deg);transform-origin:top center;
  box-shadow:0 0 6px rgba(0,0,0,.4);
}

/* 绿植 */
.plant{position:absolute;right:6%;bottom:62px;width:44px;height:58px}
.plant .pot{
  position:absolute;left:10px;bottom:0;width:24px;height:20px;border-radius:3px 3px 8px 8px;
  background:linear-gradient(160deg,#C0807C,#96605C);
}
.plant .leaf{
  position:absolute;left:50%;bottom:18px;width:16px;height:26px;border-radius:50% 50% 50% 0;
  background:linear-gradient(160deg,#8FA07C,#5E7150);transform-origin:bottom center;
  animation:sway 5.5s ease-in-out infinite;
}
.plant .leaf.l1{transform:translateX(-50%) rotate(-22deg)}
.plant .leaf.l2{transform:translateX(-50%) rotate(4deg);animation-delay:-1.4s;background:linear-gradient(160deg,#9FB08B,#6C7F5C)}
.plant .leaf.l3{transform:translateX(-50%) rotate(28deg);animation-delay:-2.6s;height:22px}
[data-theme="dark"] .plant .leaf{filter:brightness(.72) saturate(.9)}

/* 地毯 */
.room-rug{
  position:absolute;left:14%;right:12%;bottom:0;height:34px;border-radius:50%;
  background:
    repeating-radial-gradient(ellipse at 50% 50%, rgba(192,128,124,.28) 0 6px, rgba(199,154,107,.20) 6px 12px);
  filter:blur(.6px);opacity:.7;
}

/* 小屋签名 */
.room-note{
  position:absolute;left:16px;top:14px;
  font-family:'Georgia','Songti SC',serif;font-size:10px;letter-spacing:.18em;
  color:var(--text-muted);
  padding:4px 10px;border-radius:999px;
  background:rgba(255,251,244,.42);
  border:1px solid rgba(122,78,51,.12);
  backdrop-filter:blur(4px);
}
[data-theme="dark"] .room-note{
  background:rgba(30,24,33,.45);border-color:rgba(232,190,140,.16);color:rgba(244,234,221,.7);
}

/* ═══ 内容区 ═══ */
.home-layout{display:grid;grid-template-columns:minmax(0,1fr) 300px;gap:26px;align-items:start}
.home-side{position:sticky;top:84px}

.stream-head{display:flex;justify-content:space-between;align-items:center;margin-bottom:20px;gap:12px;flex-wrap:wrap}
.stream-title{
  display:flex;align-items:center;gap:10px;
  font-family:'Georgia','Songti SC','Noto Serif SC',serif;
  font-size:20px;font-weight:800;color:var(--text-primary);
}
.stream-sub{
  font-family:'Nunito','PingFang SC','Microsoft YaHei',sans-serif;
  font-size:11.5px;font-weight:600;color:var(--text-muted);
  padding-left:10px;margin-left:2px;border-left:1px solid var(--border-light);
  letter-spacing:.03em;
}
.stream-dot{
  width:9px;height:9px;border-radius:50%;
  background:linear-gradient(135deg,var(--accent-warm),var(--accent-rose));
  box-shadow:0 0 0 4px rgba(217,142,74,.14), 0 0 14px rgba(217,142,74,.55);
}
.post-list{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:20px}

/* 正在播放 */
.now-playing{padding-bottom:16px}
.np-row{display:flex;align-items:center;gap:14px;margin-bottom:12px}
.np-disc{
  position:relative;width:54px;height:54px;border-radius:50%;flex-shrink:0;
  background:
    repeating-radial-gradient(circle at 50% 50%, rgba(255,255,255,.06) 0 1px, transparent 1px 3px),
    radial-gradient(circle at 36% 30%, #4A4038, #1C1815 72%);
  box-shadow:0 8px 18px -10px rgba(0,0,0,.8), inset 0 0 0 1px rgba(255,255,255,.06);
  animation:vinylSpin 4.2s linear infinite;
}
.np-disc::before{
  content:'';position:absolute;left:19px;top:19px;width:16px;height:16px;border-radius:50%;
  background:linear-gradient(135deg,var(--accent-warm),var(--accent-rose));
}
.np-hole{
  position:absolute;left:50%;top:50%;width:5px;height:5px;border-radius:50%;
  background:var(--bg-card);transform:translate(-50%,-50%);
}
.np-meta{min-width:0}
.np-meta b{display:block;font-size:14px;color:var(--text-primary);margin-bottom:3px}
.np-meta em{font-style:normal;font-size:11.5px;color:var(--text-muted);letter-spacing:.04em}
.np-bars{display:flex;align-items:flex-end;gap:3px;height:22px;padding-left:2px}
.np-bars i{
  display:block;width:4px;border-radius:2px;height:30%;
  background:linear-gradient(180deg,var(--accent-warm),var(--accent-secondary));
  animation:eq 1.15s ease-in-out infinite alternate;
}
.np-bars i:nth-child(1){animation-delay:-.1s}
.np-bars i:nth-child(2){animation-delay:-.5s;height:60%}
.np-bars i:nth-child(3){animation-delay:-.9s;height:40%}
.np-bars i:nth-child(4){animation-delay:-.3s;height:75%}
.np-bars i:nth-child(5){animation-delay:-.7s;height:50%}
.np-bars i:nth-child(6){animation-delay:-1.1s;height:85%}
.np-bars i:nth-child(7){animation-delay:-.2s;height:35%}
.np-bars i:nth-child(8){animation-delay:-.6s;height:65%}
.np-bars i:nth-child(9){animation-delay:-1s;height:45%}
.np-bars i:nth-child(10){animation-delay:-.4s;height:80%}
.np-bars i:nth-child(11){animation-delay:-.8s;height:55%}
.np-bars i:nth-child(12){animation-delay:-1.2s;height:70%}
.np-bars i:nth-child(13){animation-delay:-.15s;height:40%}
.np-bars i:nth-child(14){animation-delay:-.55s;height:60%}
@keyframes eq{from{height:18%}to{height:92%}}

/* 板块入口 */
.board-entry{
  display:flex;align-items:center;gap:12px;padding:11px 12px;border-radius:12px;
  text-decoration:none;transition:all var(--transition);margin-bottom:8px;
  border:1px solid transparent;
}
.board-entry:hover{background:rgba(199,154,107,.12);border-color:var(--border-light);transform:translateX(3px)}
.board-entry.primary{background:linear-gradient(135deg,rgba(217,142,74,.14),rgba(199,154,107,.08));border-color:var(--border-accent)}
.board-icon{
  font-size:19px;width:34px;height:34px;border-radius:10px;display:flex;align-items:center;justify-content:center;
  background:var(--bg-surface);box-shadow:inset 0 1px 0 rgba(255,255,255,.5);
}
.board-entry b{display:block;font-size:14px;color:var(--text-primary)}
.board-entry em{display:block;font-size:11.5px;color:var(--text-muted);font-style:normal;margin-top:2px}
.board-entry i{font-style:normal;margin-left:auto;color:var(--text-muted);transition:all var(--transition)}
.board-entry:hover i{color:var(--accent-warm);transform:translateX(4px)}

/* 热榜 */
.hot-item{display:flex;align-items:center;gap:10px;padding:8px 4px;border-radius:10px;cursor:pointer;transition:all var(--transition)}
.hot-item:hover{background:rgba(199,154,107,.12);transform:translateX(2px)}
.hot-rank{
  flex-shrink:0;width:22px;height:22px;border-radius:7px;display:flex;align-items:center;justify-content:center;
  font-size:11.5px;font-weight:800;background:var(--bg-surface);color:var(--text-muted);
}
.hot-rank.top{
  background:linear-gradient(135deg,#E8652D,#F0A33C);color:#fff;
  box-shadow:0 0 14px rgba(232,101,45,.4);
}
.hot-title{flex:1;font-size:13px;color:var(--text-primary);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.hot-heat{font-size:11px;color:var(--accent-warm);font-weight:800}

/* 话题 chips */
.side-tags{display:flex;flex-wrap:wrap;gap:7px}
.side-tag{
  display:inline-flex;align-items:center;gap:4px;font-size:12px;font-weight:700;
  color:var(--accent-warm);background:rgba(217,142,74,.10);
  border:1px solid rgba(217,142,74,.24);padding:4px 11px;border-radius:999px;
  cursor:pointer;transition:all var(--transition);
}
.side-tag:hover{background:rgba(217,142,74,.20);transform:translateY(-2px);box-shadow:var(--glow-warm)}
.side-tag em{font-style:normal;font-size:10px;opacity:.7}

/* 公告 */
.notice-item{display:flex;align-items:flex-start;gap:9px;font-size:12.5px;color:var(--text-secondary);line-height:1.7;padding:7px 2px}
.notice-item b{font-weight:800;color:var(--text-primary);font-size:13px}
.notice-item p{margin-top:3px;color:var(--text-muted)}
.notice-dot{
  flex-shrink:0;width:6px;height:6px;border-radius:50%;margin-top:7px;
  background:var(--accent-warm);box-shadow:0 0 8px rgba(217,142,74,.7);
}

/* 响应式 */
@media(max-width:1200px){
  .post-list{grid-template-columns:repeat(2,minmax(0,1fr))}
}
@media(max-width:980px){
  .hero-inner{grid-template-columns:1fr;gap:34px}
  .hero-copy{max-width:none}
  .hero-room{margin:0 auto;max-width:440px;height:340px}
  .home-layout{grid-template-columns:minmax(0,1fr)}
  .home-side{position:static;display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px;align-items:start}
  .home-side .side-card{margin-bottom:0}
}
@media(max-width:768px){
  .hero-section{padding:44px 16px 56px}
  .post-list{grid-template-columns:1fr}
  .home-side{grid-template-columns:1fr}
  .hero-room{height:300px}
  .room-window{height:48%}
}
@media(max-width:480px){
  .hero-actions .warm-btn{flex:1}
  .hero-room{height:264px}
  .room-lamp{right:2%;transform:scale(.86);transform-origin:top right}
}
</style>
