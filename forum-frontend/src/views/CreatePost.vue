<template>
<div class="page-container create-page">
<div class="create-card">
<h2 class="create-title">{{isEdit?'✎ 编辑帖子':'✦ 发布新帖'}}</h2>
<div v-if="!isEdit" class="template-pick">
<span class="tpl-label">选择发帖模板：</span>
<button v-for="t in templates" :key="t.type" type="button" class="tpl-btn" :class="{active:templateType===t.type}" @click="pickTemplate(t)">
<span class="tpl-icon">{{t.icon}}</span><span class="tpl-name">{{t.name}}</span><span class="tpl-desc">{{t.desc}}</span>
</button>
</div>
<el-form ref="formRef" :model="form" :rules="rules" label-position="top">
<el-form-item label="标题" prop="title"><el-input v-model="form.title" placeholder="给帖子起个标题..." size="large" maxlength="200" show-word-limit/></el-form-item>
<el-form-item label="板块" prop="category"><el-radio-group v-model="form.category"><el-radio-button value="GAME">🎮 单机游戏</el-radio-button><el-radio-button value="ANIME">📺 动漫</el-radio-button></el-radio-group></el-form-item>
<el-form-item label="标签（最多 3 个，回车可创建新标签）"><el-select v-model="form.tags" multiple filterable allow-create default-first-option :reserve-keyword="false" placeholder="选择已有话题或输入新话题" style="width:100%"><el-option v-for="t in tagOptions" :key="t" :label="t" :value="t"/></el-select></el-form-item>
<el-form-item label="视频链接（选填，最多 3 个，支持 B站 / YouTube / 腾讯视频 / 优酷）">
<div class="video-add">
<el-input v-model="videoInput" placeholder="粘贴视频链接，如 https://www.bilibili.com/video/BV..." @keyup.enter="addVideo"/>
<button type="button" class="video-add-btn" :disabled="videos.length>=3" @click="addVideo">添加</button>
</div>
<div class="video-list" v-if="videos.length">
<div v-for="(v,i) in videos" :key="v.url" class="video-item">
<div class="video-thumb" :class="{empty:!v.cover}">
<img v-if="v.cover" :src="v.cover" alt=""/>
<span v-else class="video-thumb-ic">▶</span>
</div>
<div class="video-info">
<b>{{ v.title || (v.loading ? '正在抓取封面…' : providerName(v.url) + ' 视频') }}</b>
<span class="video-meta">{{ providerName(v.url) }}<template v-if="v.owner"> · {{ v.owner }}</template><template v-if="v.durationSeconds"> · {{ fmtDuration(v.durationSeconds) }}</template></span>
</div>
<button type="button" class="video-chip-del" @click="videos.splice(i,1)" title="移除">✕</button>
</div>
</div>
<p class="video-tip">粘贴 B站链接会自动抓取标题和封面并保存到站内；YouTube / 腾讯 / 优酷 视网络情况抓取；抖音没有公开的通用播放接口，会显示成跳转卡片。</p>
</el-form-item>
<el-form-item label="正文" prop="content">
<div class="editor-actions">
<button type="button" class="tb-btn" @click="insertTag('b')" title="加粗"><b>B</b></button>
<button type="button" class="tb-btn" @click="insertTag('i')" title="斜体"><i>I</i></button>
<button type="button" class="tb-btn" @click="insertTag('u')" title="下划线"><u>U</u></button>
<button type="button" class="tb-btn" @click="insertLink" title="插入链接">🔗</button>
<button type="button" class="tb-btn" @click="uploadImage" title="上传图片">🖼</button>
<input ref="fileInput" type="file" accept="image/*" style="display:none" @change="handleFileSelect"/>
<span class="upload-hint" v-if="uploading">上传中… <button class="cu-btn" @click="cancelUpload">取消</button></span>
</div>
<div class="img-manage" v-if="imagesList.length">
<div v-for="(img,i) in imagesList" :key="img" class="img-thumb">
<img :src="img" @error="removeImage(i)"/>
<button type="button" class="img-remove" @click="removeImage(i)" title="移除这张图片">✕</button>
</div>
</div>
<el-input v-model="form.content" type="textarea" :rows="14" :placeholder="placeholder" resize="vertical"/>
</el-form-item>
<el-form-item><button type="button" class="neon-btn" :disabled="submitting||uploading" @click="submitPost" style="padding:10px 36px">{{submitting?'发布中…':(isEdit?'保存修改':'发布帖子')}}</button><button type="button" class="neon-btn outline" @click="router.back()" style="margin-left:10px">取消</button></el-form-item>
</el-form>
</div>
</div>
</template>
<script setup>
import{ref,reactive,computed,onMounted}from'vue'
import{useRoute,useRouter}from'vue-router'
import{postApi,imageApi,tagApi,videoApi}from'../api/post'
import{ElMessage}from'element-plus'
const route=useRoute();const router=useRouter();const formRef=ref(null);const fileInput=ref(null);const submitting=ref(false);const uploading=ref(false);let abortRef=null
const isEdit=computed(()=>!!route.params.id)
const tagOptions=ref([])
const templates=[
{type:'guide',icon:'🎮',name:'攻略',desc:'分享通关技巧与心得',suggestTags:['攻略'],placeholder:'分享你的游戏攻略 / 通关技巧 / 隐藏要素...'},
{type:'help',icon:'🔍',name:'求助',desc:'遇到难题来求援',suggestTags:['求助'],placeholder:'卡关了？把问题描述清楚，大家来帮你...'},
{type:'anli',icon:'📢',name:'安利',desc:'向同好安利作品',suggestTags:['安利'],placeholder:'想安利什么作品？写出它的闪光点...'},
{type:'chat',icon:'💬',name:'闲聊',desc:'轻松水一帖',suggestTags:[],placeholder:'随便聊聊游戏与动漫的日常...'},
]
const templateType=ref('guide')
const placeholder=ref(templates[0].placeholder)
function pickTemplate(t){templateType.value=t.type;placeholder.value=t.placeholder;if(t.suggestTags.length&&!form.tags.includes(t.suggestTags[0])){form.tags=[...new Set([...form.tags,...t.suggestTags])]}}
const form=reactive({title:'',category:'GAME',content:'',tags:[]})
const rules={title:[{required:true,message:'请输入标题',trigger:'blur'}],category:[{required:true,message:'请选择板块',trigger:'change'}],content:[{required:true,message:'请输入内容',trigger:'blur'}]}
const imagesList=ref([])
const videoInput=ref('')
const videos=ref([])
const VIDEO_HOSTS=['bilibili.com','b23.tv','youtube.com','youtu.be','v.qq.com','youku.com','douyin.com','ixigua.com']
function providerName(url){
  try{
    const h=new URL(url).hostname.replace(/^www\./i,'').toLowerCase()
    if(h.endsWith('bilibili.com')||h==='b23.tv')return 'B站'
    if(h.endsWith('youtube.com')||h==='youtu.be')return 'YouTube'
    if(h.endsWith('v.qq.com'))return '腾讯视频'
    if(h.endsWith('youku.com'))return '优酷'
    if(h.endsWith('douyin.com'))return '抖音'
    if(h.endsWith('ixigua.com'))return '西瓜'
    return '视频'
  }catch{return '视频'}
}
function addVideo(){
  let v=(videoInput.value||'').trim()
  if(!v)return
  if(videos.value.length>=3){ElMessage.warning('最多添加 3 个视频');return}
  if(!/^https?:\/\//i.test(v))v='https://'+v
  let host=''
  try{host=new URL(v).hostname.replace(/^www\./i,'').toLowerCase()}catch{ElMessage.error('链接格式不正确');return}
  if(!VIDEO_HOSTS.some(h=>host===h||host.endsWith('.'+h))){
    ElMessage.error('目前只支持 B站 / YouTube / 腾讯视频 / 优酷 / 抖音 / 西瓜视频')
    return
  }
  if(videos.value.some(x=>x.url===v)){ElMessage.info('这个链接已经添加过了');return}
  const item={url:v,title:'',cover:'',owner:'',durationSeconds:0,loading:true}
  videos.value.push(item)
  videoInput.value=''
  fetchVideoMeta(item)
}
async function fetchVideoMeta(item){
  try{
    const m=await videoApi.meta(item.url,{skipAuthRedirect:true})
    if(m){
      item.title=m.title||''
      item.cover=m.cover||''
      item.owner=m.owner||''
      item.durationSeconds=m.durationSeconds||0
    }
  }catch{
    ElMessage.warning('没能抓到视频信息，仍然可以添加')
  }finally{
    item.loading=false
  }
}
function fmtDuration(seconds){
  const m=Math.floor((seconds||0)/60)
  const s=Math.floor((seconds||0)%60)
  return `${m}:${String(s).padStart(2,'0')}`
}
function hydrateVideos(list){
  videos.value=(list||[]).map(v=>{
    if(typeof v==='string')return{url:v,title:'',cover:'',owner:'',durationSeconds:0,loading:false}
    return{url:v.url,title:v.title||'',cover:v.cover||'',owner:v.owner||'',durationSeconds:v.durationSeconds||0,loading:false}
  })
  videos.value.filter(v=>v.url&&!v.title&&!v.cover).forEach(v=>{v.loading=true;fetchVideoMeta(v)})
}
onMounted(async()=>{
tagApi.list().then(list=>{tagOptions.value=(list||[]).map(t=>t.name)}).catch(()=>{})
if(isEdit.value){
  try{
    const res=await postApi.detail(route.params.id)
    form.title=res.title;form.category=res.category;form.content=res.content
    try{form.tags=JSON.parse(res.tags||'[]')}catch{form.tags=[]}
    try{imagesList.value=JSON.parse(res.images||'[]')}catch{imagesList.value=[]}
    try{hydrateVideos(JSON.parse(res.videos||'[]'))}catch{videos.value=[]}  }catch{
    ElMessage.error('帖子加载失败')
    router.push('/')
  }
}
})
function insertTag(t){form.content=(form.content||'')+`<${t}></${t}>`}
function insertLink(){form.content=(form.content||'')+'<a href="https://">链接文本</a>'}
function uploadImage(){fileInput.value?.click()}
async function handleFileSelect(e){
const file=e.target.files?.[0]
if(!file)return
abortRef=new AbortController()
uploading.value=true
try{
const res=await imageApi.upload(file,abortRef.signal)
const url=res.url
// 只进图集，不再同时往正文塞 <img>，否则详情页会重复展示同一张图
imagesList.value.push(url)
ElMessage.success('图片已上传')
}catch(err){
if(err?.name==='CanceledError'||err?.code==='ERR_CANCELED')return
ElMessage.error('图片上传失败')
}finally{
uploading.value=false;abortRef=null;e.target.value=''
}
}
function cancelUpload(){if(abortRef){abortRef.abort();abortRef=null}uploading.value=false}
function removeImage(i){imagesList.value.splice(i,1)}
async function submitPost(){
const valid=await formRef.value.validate().catch(()=>false)
if(!valid||uploading.value)return
submitting.value=true
try{
const data={title:form.title,category:form.category,content:form.content,tags:form.tags||[],images:imagesList.value.length>0?JSON.stringify(imagesList.value):null,videos:videos.value.map(v=>v.url)}
if(isEdit.value){await postApi.update(route.params.id,data);ElMessage.success('修改成功');router.push(`/post/${route.params.id}`)}
else{const res=await postApi.create(data);ElMessage.success('发布成功');router.push(`/post/${res.id}`)}
}catch(e){/* 错误提示由 axios 拦截器统一处理 */}finally{submitting.value=false}
}
</script>
<style scoped>
.create-page{max-width:800px;margin:0 auto}
.create-card{background:var(--bg-card);backdrop-filter:blur(6px);-webkit-backdrop-filter:blur(6px);border-radius:var(--radius);padding:32px;border:1px solid var(--border-accent)}
.create-title{font-size:22px;font-weight:700;margin-bottom:16px}
.template-pick{display:flex;align-items:center;gap:8px;flex-wrap:wrap;margin-bottom:20px;padding:14px;border-radius:12px;background:var(--bg-surface);border:1px solid var(--border-light)}
.tpl-label{font-size:13px;font-weight:700;color:var(--text-secondary);margin-right:4px}
.tpl-btn{display:inline-flex;align-items:center;gap:6px;padding:7px 14px;border-radius:12px;border:1px solid var(--border-light);background:transparent;cursor:pointer;transition:all var(--transition)}
.tpl-btn:hover{border-color:var(--accent-warm);transform:translateY(-1px)}
.tpl-btn.active{border-color:var(--accent-warm);background:rgba(193,127,58,.1);box-shadow:var(--glow-warm)}
.tpl-icon{font-size:16px}
.tpl-name{font-size:13px;font-weight:700;color:var(--text-primary)}
.tpl-desc{font-size:11px;color:var(--text-muted)}
.editor-actions{display:flex;gap:4px;margin-bottom:8px;align-items:center;flex-wrap:wrap}
.tb-btn{display:inline-flex;align-items:center;justify-content:center;width:34px;height:34px;border-radius:8px;font-size:14px;border:1px solid var(--border-accent);cursor:pointer;background:var(--bg-surface);color:var(--text-secondary);transition:all var(--transition)}
.tb-btn:hover{border-color:var(--accent-warm);color:var(--accent-warm);background:rgba(193,127,58,.1)}
.upload-hint{font-size:12px;color:var(--accent-warm);margin-left:8px;display:inline-flex;align-items:center;gap:6px}
.cu-btn{padding:2px 10px;border-radius:10px;font-size:11px;font-weight:600;border:1px solid rgba(192,128,128,.35);cursor:pointer;background:transparent;color:var(--accent-rose);transition:all var(--transition)}
.cu-btn:hover{background:rgba(192,128,128,.12);box-shadow:var(--glow-rose)}
.img-manage{display:flex;flex-wrap:wrap;gap:10px;margin-bottom:12px}
.img-thumb{position:relative;width:96px;height:72px;border-radius:8px;overflow:hidden;border:1px solid var(--border-light);background:var(--bg-surface)}
.img-thumb img{width:100%;height:100%;object-fit:cover;display:block}
.img-remove{position:absolute;top:4px;right:4px;width:20px;height:20px;border-radius:50%;border:none;background:rgba(0,0,0,.55);color:#fff;font-size:11px;cursor:pointer;display:flex;align-items:center;justify-content:center;transition:all var(--transition)}
.img-remove:hover{background:var(--accent-rose)}

/* ═══ Lo-Fi 写作台 ═══ */
.create-page{max-width:860px}
.create-card{
  position:relative;
  padding:34px 32px 28px;
  background:
    radial-gradient(120% 90% at 0% 0%, rgba(255,255,255,.5), transparent 58%),
    var(--paper);
  border:1px solid var(--border-light);
  border-radius:var(--radius);
  box-shadow:var(--shadow-md), inset 0 1px 0 rgba(255,255,255,.45);
}
[data-theme="dark"] .create-card{background:radial-gradient(120% 90% at 0% 0%, rgba(255,205,140,.05), transparent 58%),var(--paper)}
.create-card::before{
  content:'';position:absolute;top:-7px;left:50%;transform:translateX(-50%) rotate(-1deg);
  width:96px;height:16px;border-radius:3px;
  background:linear-gradient(180deg,rgba(226,196,152,.85),rgba(206,172,128,.65));
  box-shadow:0 2px 8px rgba(122,78,51,.16);
}
[data-theme="dark"] .create-card::before{background:linear-gradient(180deg,rgba(242,192,119,.26),rgba(242,192,119,.12))}
.create-title{
  font-size:24px;font-weight:900;margin-bottom:18px;color:var(--text-primary);
  display:flex;align-items:center;gap:10px;
}
.create-title::after{
  content:'';flex:1;height:1px;
  background:linear-gradient(90deg,var(--border-accent),transparent);
}
.template-pick{
  gap:9px;padding:14px;border-radius:14px;
  background:var(--bg-surface);
  border:1px dashed var(--border-accent);
}
.tpl-btn{
  border-radius:999px;padding:7px 14px;background:var(--paper);
  border:1px solid var(--border-light);
}
.tpl-btn:hover{border-color:var(--accent-warm);transform:translateY(-2px);box-shadow:var(--shadow-sm)}
.tpl-btn.active{
  border-color:transparent;
  background:linear-gradient(135deg,rgba(217,142,74,.22),rgba(199,154,107,.16));
  box-shadow:0 0 0 1px rgba(217,142,74,.4) inset, var(--glow-warm);
}
.tpl-icon{font-size:15px}
.tpl-name{color:var(--text-primary)}
.tpl-desc{color:var(--text-muted)}
.editor-actions{
  gap:6px;margin-bottom:10px;
  padding:8px;border-radius:12px;
  background:var(--bg-surface);border:1px solid var(--border-light);
  width:fit-content;
}
.tb-btn{
  width:32px;height:32px;border-radius:9px;
  background:var(--paper);border-color:var(--border-light);
}
.tb-btn:hover{background:rgba(217,142,74,.14);border-color:rgba(217,142,74,.35);transform:translateY(-1px)}
.img-manage{gap:14px;margin-bottom:16px}
.img-thumb{
  width:104px;height:92px;padding:6px 6px 16px;overflow:visible;
  background:#FFFCF6;border:1px solid rgba(122,78,51,.14);
  border-radius:4px;box-shadow:var(--shadow-sm);
  transition:transform .45s var(--ease-soft),box-shadow .45s var(--ease-soft);
}
[data-theme="dark"] .img-thumb{background:#2B242E;border-color:rgba(232,190,140,.18)}
.img-thumb:nth-child(odd){transform:rotate(-1.2deg)}
.img-thumb:nth-child(even){transform:rotate(1.2deg)}
.img-thumb:hover{transform:rotate(0) translateY(-3px);box-shadow:var(--shadow-md)}
.img-thumb img{height:70px;border-radius:2px}
.img-remove{top:2px;right:2px;width:18px;height:18px;background:rgba(60,36,20,.55)}
.create-card :deep(.el-textarea__inner){
  background:
    repeating-linear-gradient(180deg, transparent 0 27px, rgba(122,78,51,.07) 27px 28px),
    var(--paper) !important;
  line-height:28px !important;
  padding:10px 14px !important;
  font-size:14.5px;
  border-radius:10px !important;
}
[data-theme="dark"] .create-card :deep(.el-textarea__inner){
  background:
    repeating-linear-gradient(180deg, transparent 0 27px, rgba(232,190,140,.06) 27px 28px),
    var(--paper) !important;
}
.create-card :deep(.el-input__wrapper){background:var(--paper)!important}

/* 视频链接 */
.video-add{display:flex;gap:8px;width:100%}
.video-add-btn{
  flex-shrink:0;padding:0 18px;border-radius:10px;cursor:pointer;
  border:1px solid var(--border-accent);background:var(--paper);
  color:var(--accent-primary);font-weight:700;font-size:13px;
  transition:all var(--transition);
}
.video-add-btn:hover:not(:disabled){border-color:var(--accent-warm);color:var(--accent-warm);box-shadow:var(--glow-warm)}
.video-add-btn:disabled{opacity:.5;cursor:not-allowed}
.video-list{display:flex;flex-direction:column;gap:8px;width:100%;margin-top:10px}
.video-item{
  display:flex;align-items:center;gap:12px;
  padding:8px;border-radius:12px;
  background:var(--bg-surface);border:1px solid var(--border-light);
  transition:border-color var(--transition), box-shadow var(--transition);
}
.video-item:hover{border-color:var(--border-accent);box-shadow:var(--shadow-sm)}
.video-thumb{
  flex-shrink:0;width:96px;height:54px;border-radius:8px;overflow:hidden;
  background:#221a20;display:flex;align-items:center;justify-content:center;
}
.video-thumb.empty{background:linear-gradient(135deg,#2B2129,#161115)}
.video-thumb img{width:100%;height:100%;object-fit:cover}
.video-thumb-ic{color:rgba(255,245,235,.75);font-size:14px}
.video-info{display:flex;flex-direction:column;gap:3px;min-width:0;flex:1}
.video-info b{
  font-size:12.8px;color:var(--text-primary);font-weight:700;
  overflow:hidden;text-overflow:ellipsis;white-space:nowrap;
}
.video-meta{font-size:11px;color:var(--text-muted);overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.video-chip-del{
  flex-shrink:0;border:none;background:none;cursor:pointer;
  color:var(--accent-rose);font-size:12px;padding:4px 8px;border-radius:8px;
}
.video-chip-del:hover{background:rgba(192,128,124,.14)}
.video-tip{font-size:11.5px;line-height:1.6;color:var(--text-muted);margin-top:8px}
</style>
