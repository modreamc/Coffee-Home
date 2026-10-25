<template>
<div class="page-container post-detail">
  <div v-if="loadError" class="empty-state" style="padding:60px 20px">
    <p style="font-size:16px;font-weight:700">帖子不存在或已被删除</p>
    <p style="margin-top:8px;font-size:13px;color:var(--text-muted)">{{ loadError }}</p>
    <button class="neon-btn" style="margin-top:18px" @click="router.push('/')">返回首页</button>
  </div>
  <template v-else-if="post">
  <div class="detail-header glass-card">
    <span :class="['category-tag',post.category==='GAME'?'game':'anime']">{{ post.category==='GAME'?'🎮 单机游戏':'📺 动漫' }}</span>
    <span v-if="post.isEssence" class="essence-badge" style="margin-left:8px">✨ 精华</span>
    <span v-if="post.isTop" class="hot-badge" style="margin-left:8px">📌 置顶</span>
    <h1 class="detail-title">{{ post.title }}</h1>
    <div class="detail-meta">
      <span class="meta-author"><span class="meta-avatar">{{(post.authorName||'匿')[0]}}</span>{{post.authorName}}</span>
      <span class="meta-divider">|</span><span>{{formatTime(post.createdAt)}}</span>
      <span class="meta-divider">|</span><span><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-3.3H14zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"/></svg>{{post.likeCount}}</span>
      <span><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>{{post.collectCount}}</span>
      <span><svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>{{post.commentCount}}</span>
    </div>
    <div class="detail-tags" v-if="tagList.length"><span v-for="t in tagList" :key="t" class="detail-tag" @click="goTopic(t)">#{{t}}</span></div>
  </div>
  <div class="detail-images" v-if="post.images&&imagesList.length>0">
    <div v-for="(img,i) in imagesList" :key="i" v-show="!imgFailed[i]" class="detail-img-wrap" @click="previewImage(img)">
      <img :src="img" class="detail-img" @error="imgFailed[i]=true"/>
      <div class="detail-img-overlay"><svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg></div></div></div>
  <div class="detail-videos" v-if="videoList.length">
    <VideoEmbed v-for="(v,i) in videoList" :key="`${v.url}-${i}`" :video="v"/>
  </div>
  <div class="detail-content content-rich glass-card" v-html="safeContent"/>
  <div class="detail-actions" v-if="userStore.isLoggedIn">
    <button :class="['action-btn',{active:isLiked}]" @click="toggleLike"><svg width="18" height="18" viewBox="0 0 24 24" :fill="isLiked?'currentColor':'none'" stroke="currentColor" stroke-width="2"><path d="M14 9V5a3 3 0 0 0-3-3l-4 9v11h11.28a2 2 0 0 0 2-1.7l1.38-9a2 2 0 0 0-2-3.3H14zM7 22H4a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2h3"/></svg>{{isLiked?'已点赞':'点赞'}} {{post.likeCount}}</button>
    <button :class="['action-btn','star-btn',{active:isCollected}]" @click="toggleCollect"><svg width="18" height="18" viewBox="0 0 24 24" :fill="isCollected?'currentColor':'none'" stroke="currentColor" stroke-width="2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>{{isCollected?'已收藏':'收藏'}} {{post.collectCount}}</button>
    <button v-if="isOwner||userStore.role==='ADMIN'" class="action-btn danger" @click="handleDelete"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>删除</button>
    <button v-if="isOwner" class="action-btn" @click="handleEdit"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>编辑</button>
  </div>
  <div class="comments-section glass-card">
    <h3 class="comments-title"><svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg> 评论 ({{post.commentCount}})</h3>
    <div class="comment-input" v-if="userStore.isLoggedIn">
      <el-input v-model="commentText" type="textarea" :rows="3" :placeholder="replyTarget?`回复 @${replyTarget.userName}：`:'写下你的评论...'"/>
      <div class="comment-input-actions">
        <button v-if="replyTarget" class="cancel-reply" @click="cancelReply">✕ 取消回复 @{{replyTarget.userName}}</button>
        <button class="neon-btn" @click="submitComment" :disabled="commentLoading" style="margin-top:10px">{{replyTarget?'发送回复':'发表评论'}}</button>
      </div></div>
    <div v-else class="login-hint"><router-link to="/login">登录</router-link> 后即可参与评论</div>
    <div class="comment-list">
      <div v-for="c in topComments" :key="c.id" class="comment-item">
        <div class="comment-header">
          <span class="comment-avatar">{{c.userName?.charAt(0)||'?'}}</span>
          <span class="comment-user">{{c.userName}}</span>
          <span class="comment-time">{{formatTime(c.createdAt)}}</span>
          <button v-if="userStore.role==='ADMIN'||c.userId===parseInt(userStore.userInfo?.id)" class="comment-del" @click="deleteComment(c.id)">删除</button></div>
        <div class="comment-content">{{c.content}}</div>
        <button v-if="userStore.isLoggedIn" class="reply-btn" @click="replyTo(c)">↩ 回复</button>
        <div class="reply-list" v-if="threadReplies(c.id).length">
          <div v-for="r in threadReplies(c.id)" :key="r.id" class="reply-item">
            <span class="reply-user">{{r.userName}}</span>
            <span v-if="parentOf(r)&&parentOf(r).id!==c.id" class="reply-at">回复 @{{parentOf(r).userName}}：</span>
            <span class="reply-text">{{r.content}}</span>
            <span class="reply-time">{{formatTime(r.createdAt)}}</span>
            <button v-if="userStore.role==='ADMIN'||r.userId===parseInt(userStore.userInfo?.id)" class="comment-del" @click="deleteComment(r.id)">删除</button>
          </div>
        </div>
      </div></div>
    <div v-if="comments.length===0" class="empty-state" style="padding:20px"><p>暂无评论</p></div>
    <div v-if="commentTotal>commentSize" class="pagination-wrap"><el-pagination v-model:current-page="commentPage" :page-size="commentSize" :total="commentTotal" layout="prev,pager,next" @current-change="loadComments" background small/></div>
  </div>
  </template>
</div>
</template>
<script setup>
import {ref,computed,onMounted,nextTick} from 'vue'
import {useRoute,useRouter} from 'vue-router'
import {postApi,commentApi,likeApi,collectApi} from '../api/post'
import {useUserStore} from '../stores/user'
import VideoEmbed from '../components/VideoEmbed.vue'
import {ElMessage,ElMessageBox} from 'element-plus'
const route=useRoute();const router=useRouter();const userStore=useUserStore()
const post=ref(null);const comments=ref([]);const commentText=ref('');const commentLoading=ref(false)
const commentPage=ref(1);const commentSize=ref(20);const commentTotal=ref(0);const isLiked=ref(false);const isCollected=ref(false);const imgFailed=ref({});const replyTarget=ref(null)
const loadError=ref('')
const videoList=computed(()=>{try{const arr=JSON.parse(post.value?.videos||'[]');return arr.map(v=>typeof v==='string'?{url:v}:v).filter(v=>v&&v.url)}catch{return[]}})
const topComments=computed(()=>comments.value.filter(c=>!c.parentId))
// 后端已做白名单过滤；老数据可能是历史脏 HTML，这里再兜一层（DOMParser 不执行脚本）
const safeContent=computed(()=>sanitizeHtml(post.value?.content))
function sanitizeHtml(html){
  if(!html)return''
  try{
    const doc=new DOMParser().parseFromString(html,'text/html')
    doc.querySelectorAll('script,style,iframe,object,embed,form,input,button,link,meta').forEach(n=>n.remove())
    doc.querySelectorAll('*').forEach(el=>{
      for(const attr of [...el.attributes]){
        const name=attr.name.toLowerCase()
        if(name.startsWith('on')||((name==='href'||name==='src')&&/^\s*(javascript|data):/i.test(attr.value))){
          el.removeAttribute(attr.name)
        }
      }
    })
    return doc.body.innerHTML
  }catch{return ''}
}
// 把某条主评论下的全部子孙回复摊平展示（后端会一次带回整棵子树）
function threadReplies(rootId){
  const result=[];const seen=new Set([rootId]);let frontier=[rootId]
  while(frontier.length){
    const next=[]
    for(const c of comments.value){
      if(frontier.includes(c.parentId)&&!seen.has(c.id)){seen.add(c.id);result.push(c);next.push(c.id)}
    }
    frontier=next
  }
  return result
}
function parentOf(r){return comments.value.find(c=>c.id===r.parentId)}
function replyTo(c){replyTarget.value=c;commentText.value=''}
function cancelReply(){replyTarget.value=null;commentText.value=''}
const imagesList=computed(()=>{try{return JSON.parse(post.value?.images||'[]')}catch{return[]}})
const tagList=computed(()=>{try{return JSON.parse(post.value?.tags||'[]')}catch{return[]}})
const isOwner=computed(()=>post.value&&userStore.userInfo&&post.value.userId===userStore.userInfo.id)
function goTopic(t){router.push({path:'/topics',query:{tag:t}})}
function formatTime(t){if(!t)return'';const d=new Date(t);return`${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`}
async function loadPost(){
  loadError.value=''
  try{
    const res=await postApi.detail(route.params.id)
    post.value=res
    if(userStore.isLoggedIn){
      likeApi.status(route.params.id).then(r=>{isLiked.value=r.liked}).catch(()=>{})
      collectApi.status(route.params.id).then(r=>{isCollected.value=r.collected}).catch(()=>{})
    }
    bindContentImgErrors()
  }catch(e){
    post.value=null
    loadError.value=e?.response?.data?.message||'加载失败，请稍后重试'
  }
}
async function refreshPost(){
  try{post.value=await postApi.detail(route.params.id)}catch{}
}
function bindContentImgErrors(){nextTick(()=>{document.querySelectorAll('.content-rich img').forEach(im=>{if(im.dataset.bound)return;im.dataset.bound='1';im.addEventListener('error',()=>{const box=document.createElement('div');box.className='img-missing';box.innerHTML='<span>🖼</span><p>图片已失效</p>';im.replaceWith(box)},{once:true})})})}
async function loadComments(){const res=await commentApi.list(route.params.id,{page:commentPage.value,size:commentSize.value});comments.value=res.list||[];commentTotal.value=res.total||0}
async function toggleLike(){const res=await likeApi.toggle(route.params.id);isLiked.value=res.liked;if(post.value)post.value.likeCount=res.likeCount??(post.value.likeCount+(res.liked?1:-1))}
async function toggleCollect(){const res=await collectApi.toggle(route.params.id);isCollected.value=res.collected;if(post.value)post.value.collectCount=res.collectCount??(post.value.collectCount+(res.collected?1:-1))}
async function submitComment(){
  if(!commentText.value.trim())return
  commentLoading.value=true
  try{
    await commentApi.create(route.params.id,{content:commentText.value,parentId:replyTarget.value?.id||null})
    ElMessage.success(replyTarget.value?'回复成功':'评论成功')
    commentText.value='';cancelReply()
    commentPage.value=1
    await loadComments()
    refreshPost()
  }finally{commentLoading.value=false}
}
async function deleteComment(id){
  try{
    await ElMessageBox.confirm('确定删除此评论？')
    await commentApi.delete(id)
    ElMessage.success('已删除')
    await loadComments()
    refreshPost()
  }catch{}
}
function handleEdit(){router.push(`/edit/${post.value.id}`)}
async function handleDelete(){try{await ElMessageBox.confirm('确定删除此帖子？');await postApi.delete(post.value.id);ElMessage.success('已删除');router.push('/')}catch{}}
function previewImage(url){window.open(url,'_blank')}
onMounted(()=>{loadPost();loadComments()})
</script>
<style scoped>
.post-detail{max-width:800px;margin:0 auto}
.detail-header{padding:24px;margin-bottom:16px}
.detail-title{font-size:24px;font-weight:800;margin:10px 0;line-height:1.4}
.detail-meta{display:flex;flex-wrap:wrap;gap:8px;align-items:center;font-size:13px;color:var(--text-secondary)}
.meta-author{display:flex;align-items:center;gap:6px}
.meta-avatar{width:22px;height:22px;border-radius:50%;background:linear-gradient(135deg,var(--accent-secondary),var(--accent-primary));display:flex;align-items:center;justify-content:center;font-size:11px;font-weight:700;color:#fff}
.meta-divider{color:var(--text-muted)}
.detail-meta svg{vertical-align:middle;margin-right:3px}
.detail-tags{display:flex;flex-wrap:wrap;gap:6px;margin-top:12px}
.detail-tag{font-size:12px;font-weight:600;color:var(--accent-warm);background:rgba(193,127,58,.08);border:1px solid rgba(193,127,58,.2);padding:2px 10px;border-radius:12px;cursor:pointer;transition:all var(--transition)}
.detail-tag:hover{background:rgba(193,127,58,.16);transform:translateY(-1px)}
.detail-images{display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:10px;margin-bottom:16px}
.detail-img-wrap{position:relative;border-radius:8px;overflow:hidden;cursor:pointer;border:1px solid var(--border-accent)}
.detail-img{width:100%;height:160px;object-fit:cover;display:block;transition:transform var(--transition)}
.detail-img-missing{height:160px;border-radius:8px;background:var(--bg-surface);border:1px dashed var(--border-accent);display:flex;flex-direction:column;align-items:center;justify-content:center;gap:4px;color:var(--text-muted);font-size:12px}
.detail-img-missing span{font-size:24px}
.detail-img-wrap:hover .detail-img{transform:scale(1.05)}
.detail-img-overlay{position:absolute;inset:0;background:rgba(0,0,0,.3);display:flex;align-items:center;justify-content:center;opacity:0;transition:opacity var(--transition);color:#fff}
.detail-img-wrap:hover .detail-img-overlay{opacity:1}
.detail-content{padding:24px;margin-bottom:16px}
.detail-actions{display:flex;gap:12px;flex-wrap:wrap;margin-bottom:16px}
.action-btn{display:inline-flex;align-items:center;gap:6px;padding:8px 18px;border-radius:20px;font-size:14px;font-weight:600;border:1px solid var(--border-accent);cursor:pointer;background:transparent;color:var(--text-secondary);transition:all var(--transition)}
.action-btn:hover{border-color:var(--accent-warm);color:var(--accent-warm);background:rgba(193,127,58,.08)}
.action-btn.active{border-color:var(--accent-warm);color:var(--accent-warm);background:rgba(193,127,58,.12);box-shadow:var(--glow-warm)}
.action-btn.star-btn.active{border-color:var(--accent-gold);color:var(--accent-gold);background:rgba(240,192,64,.1)}
.action-btn.danger{border-color:rgba(192,128,128,.35);color:var(--accent-rose)}
.action-btn.danger:hover{background:rgba(192,128,128,.12);box-shadow:var(--glow-rose)}
.comments-section{padding:24px}
.comments-title{display:flex;align-items:center;gap:8px;font-size:18px;font-weight:700;margin-bottom:16px}
.comment-input{margin-bottom:20px}
.comment-input-actions{display:flex;align-items:center;gap:10px}
.cancel-reply{margin-top:10px;font-size:12px;color:var(--accent-rose);cursor:pointer;border:1px solid rgba(192,128,128,.35);background:transparent;padding:5px 14px;border-radius:16px;transition:all var(--transition)}
.cancel-reply:hover{background:rgba(192,128,128,.1)}
.login-hint{text-align:center;padding:20px;color:var(--text-muted);font-size:14px}
.login-hint a{color:var(--accent-primary)}
.comment-list{display:flex;flex-direction:column;gap:10px}
.comment-item{background:var(--bg-surface);border-radius:8px;padding:14px;border:1px solid rgba(193,127,58,.1);transition:border-color var(--transition)}
.comment-item:hover{border-color:var(--border-accent)}
.comment-header{display:flex;align-items:center;gap:8px;margin-bottom:8px}
.comment-avatar{width:26px;height:26px;border-radius:50%;background:linear-gradient(135deg,var(--accent-secondary),var(--accent-primary));display:flex;align-items:center;justify-content:center;font-size:12px;font-weight:700;color:#fff}
.comment-user{font-weight:600;font-size:14px}
.comment-time{font-size:12px;color:var(--text-muted);margin-left:auto}
.comment-del{background:none;border:none;color:var(--accent-rose);cursor:pointer;font-size:12px;padding:2px 6px;border-radius:4px}
.comment-del:hover{background:rgba(192,128,128,.12)}
.comment-content{font-size:14px;line-height:1.6;padding-left:34px;color:var(--text-primary)}
.reply-btn{margin:8px 0 0 34px;font-size:12px;color:var(--text-muted);cursor:pointer;background:none;border:none;padding:2px 8px;border-radius:10px;transition:all var(--transition)}
.reply-btn:hover{color:var(--accent-warm);background:rgba(193,127,58,.08)}
.reply-list{margin:10px 0 0 34px;border-left:2px solid var(--border-accent);padding-left:12px;display:flex;flex-direction:column;gap:8px}
.reply-item{font-size:13px;line-height:1.6;color:var(--text-secondary)}
.reply-user{font-weight:700;color:var(--accent-primary);margin-right:4px}
.reply-at{color:var(--accent-warm);margin-right:4px}
.reply-text{color:var(--text-primary)}
.reply-time{font-size:11px;color:var(--text-muted);margin-left:8px}
.reply-item .comment-del{font-size:11px}
</style>

<style scoped>
/* ═══════════════════════════════════════════════════════════════
   Lo-Fi 阅读层（覆盖基础样式）
   纸张正文 + 便利贴评论 + 悬浮操作条
   ═══════════════════════════════════════════════════════════════ */
.post-detail{max-width:860px}

/* 标题卡 */
.detail-header{
  position:relative;
  padding:30px 30px 26px;
  margin-bottom:18px;
  background:
    radial-gradient(120% 90% at 0% 0%, rgba(255,255,255,.5), transparent 58%),
    var(--paper);
  border:1px solid var(--border-light);
  border-radius:var(--radius);
  box-shadow:var(--shadow-sm), inset 0 1px 0 rgba(255,255,255,.4);
}
.detail-header::before{
  content:'';
  position:absolute;top:-7px;left:50%;transform:translateX(-50%) rotate(-1deg);
  width:90px;height:16px;border-radius:3px;
  background:linear-gradient(180deg,rgba(226,196,152,.85),rgba(206,172,128,.65));
  box-shadow:0 2px 8px rgba(122,78,51,.16);
}
[data-theme="dark"] .detail-header::before{background:linear-gradient(180deg,rgba(242,192,119,.26),rgba(242,192,119,.12))}
.detail-title{
  font-family:'Georgia','Songti SC','Noto Serif SC',serif;
  font-size:clamp(24px,3vw,31px);
  font-weight:900;line-height:1.42;letter-spacing:-.005em;
  margin:14px 0 14px;color:var(--text-primary);
}
.detail-meta{gap:12px;font-size:12.5px;color:var(--text-muted)}
.meta-author{color:var(--text-secondary);font-weight:700}
.meta-avatar{
  width:24px;height:24px;font-size:11px;
  box-shadow:0 2px 8px -4px rgba(122,78,51,.9);
}
.meta-divider{opacity:.5}
.detail-meta svg{opacity:.75}
.detail-tags{margin-top:16px;gap:7px}
.detail-tag{
  font-size:11.5px;font-weight:700;padding:3px 11px;border-radius:999px;
  background:rgba(217,142,74,.10);border:1px solid rgba(217,142,74,.24);
  color:var(--accent-warm);transition:all var(--transition);
}
.detail-tag:hover{background:rgba(217,142,74,.2);transform:translateY(-2px);box-shadow:var(--glow-warm)}

/* 图集：宝丽来相框 */
.detail-images{gap:14px;margin-bottom:18px}
.detail-img-wrap{
  border-radius:10px;overflow:hidden;
  border:1px solid var(--border-light);
  box-shadow:var(--shadow-sm);
  transition:transform .5s var(--ease-soft), box-shadow .5s var(--ease-soft);
}
.detail-img-wrap:nth-child(odd){transform:rotate(-.5deg)}
.detail-img-wrap:nth-child(even){transform:rotate(.5deg)}
.detail-img-wrap:hover{transform:rotate(0) translateY(-4px);box-shadow:var(--shadow-md)}
.detail-img{height:170px}
.detail-img-wrap:hover .detail-img{transform:scale(1.06)}
/* 只有一张图时铺满整行，避免宽幅封面被裁成小方框 */
.detail-img-wrap:only-child{grid-column:1 / -1}
.detail-img-wrap:only-child .detail-img{height:auto;aspect-ratio:21/9;width:100%}
.detail-videos{margin-bottom:18px}
.detail-videos :deep(.video-card:last-child){margin-bottom:0}
.detail-img-overlay{background:linear-gradient(180deg,transparent,rgba(40,26,18,.5))}

/* 正文：横线纸 */
.detail-content{
  position:relative;
  padding:34px 36px 38px 64px;
  margin-bottom:18px;
  background:
    linear-gradient(90deg, transparent 0 46px, rgba(192,128,124,.20) 46px 47px, transparent 47px),
    repeating-linear-gradient(180deg, transparent 0 33px, rgba(122,78,51,.07) 33px 34px),
    radial-gradient(120% 90% at 0% 0%, rgba(255,255,255,.5), transparent 58%),
    var(--paper);
  border:1px solid var(--border-light);
  border-radius:var(--radius);
  box-shadow:var(--shadow-sm), inset 0 1px 0 rgba(255,255,255,.4);
}
[data-theme="dark"] .detail-content{
  background:
    linear-gradient(90deg, transparent 0 46px, rgba(217,154,148,.18) 46px 47px, transparent 47px),
    repeating-linear-gradient(180deg, transparent 0 33px, rgba(232,190,140,.055) 33px 34px),
    radial-gradient(120% 90% at 0% 0%, rgba(255,205,140,.05), transparent 58%),
    var(--paper);
}
/* 正文里失效的图片压成一个小 chip，不再占掉整屏 */
.detail-content :deep(.img-missing){
  display:inline-flex;flex-direction:row;align-items:center;gap:7px;
  padding:7px 12px;margin:8px 0;border-radius:999px;
  font-size:12px;background:var(--bg-surface);border-style:dashed;
}
.detail-content :deep(.img-missing span){font-size:14px;margin:0}
.detail-content :deep(.img-missing p){margin:0}
.detail-content :deep(p){ margin:12px 0; }
.detail-content :deep(img){ border-radius:10px; }

/* 操作条：悬浮胶囊 */
.detail-actions{
  gap:10px;margin-bottom:18px;
  padding:10px 12px;
  background:var(--bg-glass);
  backdrop-filter:blur(12px);
  -webkit-backdrop-filter:blur(12px);
  border:1px solid var(--border-light);
  border-radius:999px;
  box-shadow:var(--shadow-sm);
  width:fit-content;
}
.action-btn{
  border-radius:999px;border:1px solid transparent;
  background:transparent;color:var(--text-secondary);
  padding:9px 18px;font-weight:700;font-size:13.5px;
  transition:all var(--transition);
}
.action-btn:hover{
  background:rgba(217,142,74,.12);border-color:rgba(217,142,74,.3);
  color:var(--accent-warm);transform:translateY(-1px);
}
.action-btn.active{
  background:linear-gradient(135deg,rgba(217,142,74,.22),rgba(199,154,107,.14));
  border-color:rgba(217,142,74,.45);color:var(--accent-warm);
  box-shadow:var(--glow-warm);
}
.action-btn.star-btn.active{
  color:var(--accent-gold);border-color:rgba(217,174,106,.5);
  background:linear-gradient(135deg,rgba(217,174,106,.24),rgba(217,142,74,.12));
}
.action-btn.danger{color:var(--accent-rose)}
.action-btn.danger:hover{background:rgba(192,128,124,.14);border-color:rgba(192,128,124,.4)}

/* 评论区：便利贴 */
.comments-section{
  padding:28px;
  background:
    radial-gradient(120% 90% at 100% 0%, rgba(255,255,255,.35), transparent 58%),
    var(--bg-card);
  backdrop-filter:blur(10px);
  border:1px solid var(--border-light);
  border-radius:var(--radius);
  box-shadow:var(--shadow-sm);
}
.comments-title{
  font-family:'Georgia','Songti SC','Noto Serif SC',serif;
  font-size:19px;font-weight:900;margin-bottom:18px;
  padding-bottom:14px;border-bottom:1px dashed var(--border-light);
}
.comment-input{
  padding:14px;border-radius:var(--radius-sm);
  background:var(--bg-surface);
  border:1px solid var(--border-light);
  margin-bottom:22px;
}
.comment-input :deep(.el-textarea__inner){background:var(--paper)!important}
.comment-list{gap:14px}
.comment-item{
  position:relative;
  background:var(--paper);
  border:1px solid var(--border-light);
  border-radius:14px;
  padding:16px 18px 14px;
  box-shadow:var(--shadow-sm);
  transition:transform var(--transition),box-shadow var(--transition);
  animation:noteIn .5s var(--ease-soft) both;
}
.comment-list > .comment-item:nth-child(3n+1){transform:rotate(-.4deg)}
.comment-list > .comment-item:nth-child(3n+2){transform:rotate(.35deg)}
.comment-item:hover{transform:rotate(0) translateY(-2px);box-shadow:var(--shadow-md)}
@keyframes noteIn{from{opacity:0;transform:translateY(10px) rotate(-.6deg)}to{opacity:1}}
.comment-item::before{
  content:'';position:absolute;top:-5px;left:22px;width:34px;height:10px;border-radius:2px;
  background:linear-gradient(180deg,rgba(217,174,106,.55),rgba(217,142,74,.32));
  transform:rotate(-3deg);
}
.comment-avatar{
  width:28px;height:28px;border-radius:9px;font-size:12px;
  box-shadow:0 2px 8px -4px rgba(122,78,51,.8);
}
.comment-user{font-weight:800;font-size:13.5px}
.comment-time{font-size:11.5px;color:var(--text-muted)}
.comment-content{padding-left:38px;font-size:14px;line-height:1.8;color:var(--text-primary)}
.reply-btn{
  margin-left:38px;font-size:11.5px;padding:3px 10px;border-radius:999px;
  border:1px solid transparent;
}
.reply-btn:hover{background:rgba(217,142,74,.12);border-color:rgba(217,142,74,.25)}
.reply-list{
  margin-left:38px;padding-left:14px;
  border-left:2px dashed rgba(199,154,107,.45);
  gap:10px;
}
.reply-item{font-size:12.8px;line-height:1.7}
.reply-user{color:var(--accent-primary)}
.login-hint{
  padding:24px;border-radius:var(--radius-sm);
  background:var(--bg-surface);
  border:1px dashed var(--border-accent);
  color:var(--text-muted);
}
.cancel-reply{border-radius:999px}
.comment-del{border-radius:6px;transition:all var(--transition)}
.comment-del:hover{background:rgba(192,128,124,.14)}
</style>