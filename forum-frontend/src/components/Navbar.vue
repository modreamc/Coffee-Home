<template>
<nav class="navbar" :class="{scrolled}">
<div class="navbar-inner">
  <router-link to="/" class="logo" aria-label="咖啡港湾首页">
    <span class="logo-mark" aria-hidden="true">
      <span class="steam"><i/><i/><i/></span>
      <span class="cup">
        <span class="cup-body"/>
        <span class="cup-handle"/>
        <span class="cup-saucer"/>
      </span>
    </span>
    <span class="logo-text">
      <span class="logo-main">咖啡港湾</span>
      <span class="logo-sub">COFFEE HARBOR</span>
    </span>
  </router-link>

  <div class="nav-right">
    <div class="nav-links">
      <router-link to="/" class="nav-link">首页</router-link>
      <router-link to="/game" class="nav-link">🎮 单机游戏</router-link>
      <router-link to="/anime" class="nav-link">📺 动漫</router-link>
      <router-link to="/topics" class="nav-link"># 话题</router-link>
    </div>

    <div class="nav-search">
      <span class="search-ic" aria-hidden="true">⌕</span>
      <input v-model="keyword" class="search-input" placeholder="搜索帖子 / 话题..." @keyup.enter="doSearch"/>
      <button class="search-btn" @click="doSearch" aria-label="搜索">→</button>
    </div>

    <button
      class="theme-switch"
      :class="{night: themeStore.theme==='dark'}"
      @click="themeStore.toggle()"
      :title="themeStore.theme==='dark'?'切换到午后':'切换到雨夜'"
      :aria-label="themeStore.theme==='dark'?'切换到白天模式':'切换到夜晚模式'"
    >
      <span class="ts-knob" aria-hidden="true">
        <span class="ts-face sun">☀</span>
        <span class="ts-face moon">☾</span>
      </span>
    </button>

    <template v-if="userStore.isLoggedIn">
      <button class="signin-btn" :class="{done:signedToday}" @click="doSignin" :disabled="signedToday" :title="signedToday?'今日已签到':'每日签到 +10 积分'">{{signedToday?'✓ 已签到':'☕ 签到'}}</button>
      <el-popover placement="bottom-end" :width="330" trigger="click" @show="loadNotifications">
        <template #reference>
          <button class="bell-btn" title="通知" aria-label="通知">
            <span>🔔</span>
            <span v-if="unreadCount>0" class="bell-dot">{{unreadCount>99?'99+':unreadCount}}</span>
          </button>
        </template>
        <div class="notif-panel">
          <div class="notif-head"><b>🔔 通知</b><span class="notif-read" @click="markRead">全部已读</span></div>
          <div v-if="notifications.length===0" class="notif-empty">还没有新消息，先来杯咖啡吧</div>
          <div v-for="n in notifications" :key="n.id" class="notif-item" :class="{unread:!n.isRead}" @click="openNotif(n)">
            <span class="notif-icon">{{n.type==='COMMENT'?'💬':'👍'}}</span>
            <span class="notif-text">{{n.content}}</span>
            <span class="notif-time">{{formatNotifTime(n.createdAt)}}</span>
          </div>
        </div>
      </el-popover>
      <router-link to="/create" class="warm-btn create-btn" title="发布帖子">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M12 5v14M5 12h14"/></svg>
        <span class="create-txt">发帖</span>
      </router-link>
      <el-dropdown trigger="click" @command="handleCommand">
        <span class="user-dropdown">
          <span class="avatar-ring"><el-avatar :size="28" :src="userStore.avatar">{{userStore.nickname?.charAt(0)||userStore.username.charAt(0)}}</el-avatar></span>
          <span class="user-name">{{userStore.nickname||userStore.username}}</span>
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 9l6 6 6-6"/></svg>
        </span>
        <template #dropdown>
          <el-dropdown-menu class="warm-dropdown">
            <el-dropdown-item command="profile"><span class="dd-icon">☕</span> 我的主页</el-dropdown-item>
            <el-dropdown-item command="collects"><span class="dd-icon">★</span> 我的收藏</el-dropdown-item>
            <el-dropdown-item v-if="userStore.role==='ADMIN'" command="admin"><span class="dd-icon">⚙</span> 管理后台</el-dropdown-item>
            <el-dropdown-item divided command="logout"><span class="dd-icon">⊘</span> 退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </template>
    <template v-else>
      <router-link to="/login" class="warm-btn outline login-btn">登录</router-link>
      <router-link to="/register" class="warm-btn">注册</router-link>
    </template>
  </div>
</div>
</nav>
</template>
<script setup>
import{ref,onMounted,onUnmounted,watch}from'vue'
import{useRouter}from'vue-router'
import{useUserStore}from'../stores/user'
import{useThemeStore}from'../stores/theme'
import{signinApi,notificationApi}from'../api/post'
import{ElMessage}from'element-plus'
const router=useRouter();const userStore=useUserStore();const themeStore=useThemeStore();const scrolled=ref(false);const keyword=ref('')
const signedToday=ref(false);const notifications=ref([]);const unreadCount=ref(0)
let notifTimer=null
function startPolling(){if(notifTimer)return;notifTimer=setInterval(()=>{if(userStore.isLoggedIn)loadNotifications()},60000)}
function stopPolling(){if(notifTimer){clearInterval(notifTimer);notifTimer=null}}
function onScroll(){scrolled.value=window.scrollY>20}
function doSearch(){const k=keyword.value.trim();if(k){router.push({path:'/search',query:{keyword:k}})}}
onMounted(()=>{window.addEventListener('scroll',onScroll,{passive:true});if(userStore.isLoggedIn){loadSignStatus();loadNotifications();startPolling()}})
onUnmounted(()=>{window.removeEventListener('scroll',onScroll);stopPolling()})
watch(()=>userStore.isLoggedIn,(v)=>{if(v){loadSignStatus();loadNotifications();startPolling()}else{stopPolling();notifications.value=[];unreadCount.value=0;signedToday.value=false}})
async function loadSignStatus(){try{const r=await signinApi.today();signedToday.value=r.signed}catch{}}
async function doSignin(){if(signedToday.value)return;try{const r=await signinApi.do();signedToday.value=true;ElMessage.success(r.already?'今天已经签过到啦':'签到成功，+'+r.reward+' 积分');userStore.fetchUserInfo()}catch{}}
async function loadNotifications(){try{const r=await notificationApi.list();notifications.value=r.list||[];unreadCount.value=r.unreadCount||0}catch{}}
async function markRead(){try{await notificationApi.readAll();unreadCount.value=0;notifications.value.forEach(n=>{n.isRead=true})}catch{}}
function openNotif(n){router.push(n.relatedId?`/post/${n.relatedId}`:'/')}
function formatNotifTime(t){if(!t)return'';const d=new Date(t);const now=new Date();const diff=now-d;if(diff<3600000)return Math.max(1,Math.floor(diff/60000))+'分钟前';if(diff<86400000)return Math.floor(diff/3600000)+'小时前';return `${d.getMonth()+1}-${d.getDate()}`}
function handleCommand(cmd){if(cmd==='logout'){userStore.logout();router.push('/')}else if(cmd==='admin'){router.push('/admin')}else if(cmd==='collects'){router.push('/collects')}else if(cmd==='profile'){router.push(`/user/${userStore.userInfo?.id}`)}}
</script>
<style scoped>
/* ── 木质吧台 + 磨砂玻璃 ── */
.navbar{
  position:sticky;top:0;z-index:100;
  background:linear-gradient(180deg, rgba(252,247,240,.86), rgba(247,239,228,.72));
  backdrop-filter:blur(16px) saturate(1.15);
  -webkit-backdrop-filter:blur(16px) saturate(1.15);
  border-bottom:1px solid var(--border-light);
  transition:background-color .5s ease, box-shadow var(--transition), border-color .5s ease;
}
[data-theme="dark"] .navbar{
  background:linear-gradient(180deg, rgba(28,22,30,.88), rgba(20,16,22,.74));
}
.navbar::after{
  content:'';position:absolute;left:0;right:0;bottom:-1px;height:2px;
  background:linear-gradient(90deg,transparent,rgba(199,154,107,.55),rgba(217,142,74,.35),rgba(199,154,107,.55),transparent);
  opacity:.5;pointer-events:none;
}
.navbar.scrolled{box-shadow:0 14px 34px -26px rgba(90,60,40,.7)}
.navbar-inner{max-width:1280px;margin:0 auto;padding:0 24px;height:66px;display:flex;align-items:center;justify-content:space-between;gap:12px}

/* ── Logo：手冲咖啡杯 + 热气 ── */
.logo{display:flex;align-items:center;gap:11px;text-decoration:none;flex-shrink:0}
.logo-mark{position:relative;width:34px;height:34px;display:flex;align-items:flex-end;justify-content:center}
.cup{position:relative;width:26px;height:20px;display:block}
.cup-body{
  position:absolute;left:0;bottom:5px;width:19px;height:14px;border-radius:3px 3px 9px 9px;
  background:linear-gradient(160deg,var(--accent-secondary),var(--accent-primary));
  box-shadow:inset 0 -3px 0 rgba(0,0,0,.12), 0 4px 10px -6px rgba(122,78,51,.8);
}
.cup-body::after{
  content:'';position:absolute;left:2px;right:2px;top:2px;height:3px;border-radius:2px;
  background:rgba(255,245,230,.55);
}
.cup-handle{
  position:absolute;right:-1px;bottom:9px;width:9px;height:10px;border-radius:50%;
  border:2.5px solid var(--accent-primary);border-left-color:transparent;transform:rotate(-18deg);
}
.cup-saucer{
  position:absolute;left:-2px;bottom:0;width:26px;height:4px;border-radius:999px;
  background:linear-gradient(90deg,var(--accent-secondary),var(--accent-primary));opacity:.85;
}
.steam{position:absolute;top:-2px;left:3px;width:18px;height:16px;display:flex;justify-content:space-between;pointer-events:none}
.steam i{
  display:block;width:4px;height:4px;border-radius:50%;
  background:var(--text-muted);opacity:0;
  animation:navSteam 3.2s ease-in-out infinite;
}
.steam i:nth-child(2){animation-delay:.9s}
.steam i:nth-child(3){animation-delay:1.8s}
@keyframes navSteam{
  0%{opacity:0;transform:translateY(6px) scale(.6)}
  30%{opacity:.7}
  100%{opacity:0;transform:translateY(-12px) scale(1.4)}
}
.logo-text{display:flex;flex-direction:column;line-height:1.05}
.logo-main{font-family:'Georgia','Songti SC','Noto Serif SC',serif;font-size:20px;font-weight:900;color:var(--accent-primary);letter-spacing:.04em}
.logo-sub{font-size:9px;font-weight:800;letter-spacing:.24em;color:var(--text-muted);margin-top:3px}
.logo:hover .cup-body{background:linear-gradient(160deg,var(--accent-warm),var(--accent-secondary))}
.logo:hover .cup{transform:translateY(-1px)}
.cup{transition:transform var(--transition)}

/* ── 右侧区 ── */
.nav-right{display:flex;align-items:center;gap:10px;min-width:0}
.nav-links{display:flex;gap:2px}
.nav-link{
  position:relative;padding:8px 13px;border-radius:12px;font-size:13.5px;font-weight:600;
  color:var(--text-secondary);text-decoration:none;white-space:nowrap;
  transition:color var(--transition),background-color var(--transition);
}
.nav-link::after{
  content:'';position:absolute;left:13px;right:13px;bottom:5px;height:2px;border-radius:2px;
  background:linear-gradient(90deg,var(--accent-warm),var(--accent-secondary));
  transform:scaleX(0);transform-origin:left;transition:transform var(--transition);
}
.nav-link:hover{color:var(--accent-primary);background:rgba(199,154,107,.10)}
.nav-link:hover::after{transform:scaleX(1)}
.nav-link.router-link-active{color:var(--accent-primary);font-weight:800}
.nav-link.router-link-active::after{transform:scaleX(1)}

/* 搜索：聚焦时展开 */
.nav-search{
  display:flex;align-items:center;gap:6px;
  background:var(--bg-surface);border:1px solid var(--border-light);border-radius:999px;
  padding:4px 5px 4px 12px;transition:all var(--transition);
}
.nav-search:focus-within{border-color:var(--accent-warm);box-shadow:0 0 0 4px rgba(217,142,74,.12)}
.search-ic{font-size:15px;color:var(--text-muted);line-height:1;transform:rotate(-45deg)}
.search-input{
  border:none;outline:none;background:transparent;width:112px;
  font-size:13px;color:var(--text-primary);transition:width var(--transition);
}
.nav-search:focus-within .search-input{width:158px}
.search-input::placeholder{color:var(--text-muted)}
.search-btn{
  border:none;cursor:pointer;width:26px;height:26px;border-radius:50%;
  background:var(--accent-primary);color:var(--text-inverse);
  display:flex;align-items:center;justify-content:center;font-size:13px;
  transition:all var(--transition);
}
.search-btn:hover{background:var(--accent-warm);transform:translateX(2px)}

/* ── 主题开关：白天/夜晚双态 ── */
.theme-switch{
  position:relative;width:56px;height:30px;border-radius:999px;cursor:pointer;
  border:1px solid var(--border-accent);padding:0;flex-shrink:0;
  background:linear-gradient(135deg,#F7D9AE,#F0B87E);
  box-shadow:inset 0 2px 6px rgba(122,78,51,.18);
  transition:background .5s ease,border-color .5s ease;
}
.theme-switch.night{
  background:linear-gradient(135deg,#26384A,#3A2E42);
  border-color:rgba(232,190,140,.28);
}
.ts-knob{
  position:absolute;top:3px;left:3px;width:22px;height:22px;border-radius:50%;
  background:radial-gradient(circle at 34% 30%,#FFFDF7,#F4E3CC);
  box-shadow:0 2px 6px rgba(90,60,40,.35);
  transition:transform .45s var(--ease-soft),background .45s ease;
  display:flex;align-items:center;justify-content:center;
}
.theme-switch.night .ts-knob{transform:translateX(26px);background:radial-gradient(circle at 34% 30%,#FFF6E4,#F2C077)}
.ts-face{
  position:absolute;inset:0;display:flex;align-items:center;justify-content:center;
  font-size:12px;line-height:1;transition:opacity .3s ease,transform .35s ease;
}
.ts-face.sun{opacity:1;transform:rotate(0);color:#B4702A}
.ts-face.moon{opacity:0;transform:rotate(-40deg);color:#6B4A2E}
.theme-switch.night .ts-face.sun{opacity:0;transform:rotate(60deg)}
.theme-switch.night .ts-face.moon{opacity:1;transform:rotate(0)}
.theme-switch:hover .ts-knob{box-shadow:0 2px 10px rgba(217,142,74,.6)}
.theme-switch:active .ts-knob{transform:scale(.92)}
.theme-switch.night:active .ts-knob{transform:translateX(26px) scale(.92)}

/* 签到 */
.signin-btn{
  display:inline-flex;align-items:center;gap:5px;padding:6px 13px;border-radius:999px;
  font-size:12px;font-weight:800;border:1px solid var(--accent-warm);
  background:linear-gradient(135deg,rgba(217,142,74,.16),rgba(199,154,107,.10));
  color:var(--accent-warm);cursor:pointer;white-space:nowrap;
  transition:all var(--transition);
}
.signin-btn:hover{background:linear-gradient(135deg,rgba(217,142,74,.28),rgba(199,154,107,.18));box-shadow:var(--glow-warm);transform:translateY(-1px)}
.signin-btn.done{border-color:var(--border-light);background:var(--bg-surface);color:var(--text-muted);cursor:default;box-shadow:none;transform:none}

/* 通知 */
.bell-btn{
  position:relative;display:inline-flex;align-items:center;justify-content:center;
  width:34px;height:34px;border-radius:50%;border:1px solid var(--border-light);
  background:var(--bg-surface);cursor:pointer;font-size:14px;transition:all var(--transition);
}
.bell-btn:hover{border-color:var(--accent-warm);box-shadow:var(--glow-warm);transform:rotate(-6deg)}
.bell-dot{
  position:absolute;top:-3px;right:-4px;min-width:16px;height:16px;padding:0 4px;border-radius:999px;
  background:linear-gradient(135deg,#E8652D,#F0A33C);color:#fff;font-size:10px;font-weight:800;
  display:flex;align-items:center;justify-content:center;animation:dotPulse 2.4s ease-out infinite;
}
.notif-panel{max-height:380px;overflow-y:auto;padding-right:2px}
.notif-head{display:flex;align-items:center;justify-content:space-between;padding:2px 2px 10px;border-bottom:1px dashed var(--border-light);margin-bottom:8px}
.notif-head b{font-size:14px;color:var(--text-primary)}
.notif-read{font-size:12px;color:var(--accent-warm);cursor:pointer}
.notif-read:hover{text-decoration:underline}
.notif-empty{text-align:center;padding:30px 12px;color:var(--text-muted);font-size:13px}
.notif-item{display:flex;align-items:flex-start;gap:8px;padding:10px 8px;border-radius:10px;cursor:pointer;transition:all var(--transition)}
.notif-item:hover{background:rgba(199,154,107,.12);transform:translateX(2px)}
.notif-item.unread{background:rgba(217,142,74,.08)}
.notif-item.unread .notif-text{font-weight:700}
.notif-icon{font-size:15px;flex-shrink:0;line-height:1.3}
.notif-text{flex:1;font-size:13px;color:var(--text-primary);line-height:1.55}
.notif-time{font-size:11px;color:var(--text-muted);flex-shrink:0;padding-top:2px}

/* 发帖按钮 */
.create-btn{padding:8px 16px;font-size:13px}
.create-txt{display:inline-block}

/* 用户下拉 */
.user-dropdown{display:flex;align-items:center;gap:7px;cursor:pointer;padding:4px 8px 4px 4px;border-radius:999px;transition:all var(--transition)}
.user-dropdown:hover{background:rgba(199,154,107,.12)}
.avatar-ring{display:flex;align-items:center;justify-content:center;width:32px;height:32px;border-radius:50%;background:linear-gradient(135deg,var(--accent-secondary),var(--accent-primary));padding:2px;box-shadow:0 2px 8px -4px rgba(122,78,51,.9)}
:deep(.avatar-ring .el-avatar){background:var(--bg-deep)!important;color:var(--accent-primary)!important;font-weight:700;border:none}
.user-name{font-size:13.5px;max-width:96px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;color:var(--text-primary);font-weight:600}
.dd-icon{color:var(--accent-warm);margin-right:5px}
:deep(.warm-dropdown){background:var(--bg-elevated)!important;border:1px solid var(--border-light)!important;border-radius:14px!important}
:deep(.warm-dropdown .el-dropdown-menu__item){color:var(--text-primary)!important;border-radius:9px;margin:2px 6px}
:deep(.warm-dropdown .el-dropdown-menu__item:hover){background:rgba(199,154,107,.16)!important;color:var(--accent-primary)!important}

.login-btn{padding:8px 18px;font-size:13px}

/* ── 响应式 ── */
@media(max-width:1080px){
  .nav-link{padding:8px 9px;font-size:13px}
  .search-input{width:86px}
  .nav-search:focus-within .search-input{width:120px}
}
@media(max-width:900px){
  .nav-links{display:none}
  .create-txt{display:none}
  .create-btn{padding:8px 11px}
}
@media(max-width:680px){
  .navbar-inner{padding:0 12px;height:60px;gap:8px}
  .logo-sub{display:none}
  .logo-main{font-size:17px}
  .nav-search{display:none}
  .user-name{display:none}
  .signin-btn{display:none}
}
</style>
