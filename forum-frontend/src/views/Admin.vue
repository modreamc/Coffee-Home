<template>
  <div class="page-container">
    <h2 class="admin-title"><span class="title-glow">◆</span> 管理后台</h2>
    <el-tabs v-model="activeTab" class="warm-tabs">
      <el-tab-pane label="数据概览" name="stats">
        <div class="stats-grid" v-if="stats">
          <div class="stat-card"><div class="stat-num">{{stats.userCount}}</div><div class="stat-label">用户总数</div></div>
          <div class="stat-card"><div class="stat-num">{{stats.postCount}}</div><div class="stat-label">帖子总数</div></div>
          <div class="stat-card"><div class="stat-num">{{stats.gameCount}}</div><div class="stat-label">单机游戏帖</div></div>
          <div class="stat-card"><div class="stat-num">{{stats.animeCount}}</div><div class="stat-label">动漫帖</div></div>
        </div>
        <div class="stats-tools">
          <button class="op-btn" @click="rebuildCounters">🧮 重算点赞/收藏/评论与话题计数</button>
          <span class="stats-hint">用于修复并发或手工改库导致的数据漂移</span>
        </div>
      </el-tab-pane>
      <el-tab-pane label="用户管理" name="users">
        <el-table :data="users" stripe style="width:100%" class="warm-table">
          <el-table-column prop="id" label="ID" width="60"/>
          <el-table-column prop="username" label="用户名"/>
          <el-table-column prop="nickname" label="昵称"/>
          <el-table-column prop="email" label="邮箱"/>
          <el-table-column prop="role" label="角色" width="80"><template #default="{row}"><span :class="['role-tag',row.role.toLowerCase()]">{{row.role}}</span></template></el-table-column>
          <el-table-column prop="createdAt" label="注册时间" width="160"><template #default="{row}">{{formatTime(row.createdAt)}}</template></el-table-column>
          <el-table-column label="操作" width="100"><template #default="{row}"><button v-if="row.role!=='ADMIN'" class="del-btn" @click="deleteUser(row.id)">删除</button></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="帖子管理" name="posts">
        <el-table :data="posts" stripe style="width:100%" class="warm-table">
          <el-table-column prop="id" label="ID" width="60"/>
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip/>
          <el-table-column prop="authorName" label="作者" width="120"/>
          <el-table-column label="板块" width="90"><template #default="{row}"><span :class="['category-tag',row.category==='GAME'?'game':'anime']">{{row.category==='GAME'?'单机游戏':'动漫'}}</span></template></el-table-column>
          <el-table-column label="状态" width="120"><template #default="{row}"><span v-if="row.isTop" class="status-chip top">置顶</span><span v-if="row.isEssence" class="status-chip essence">精华</span></template></el-table-column>
          <el-table-column prop="likeCount" label="点赞" width="60"/>
          <el-table-column prop="commentCount" label="评论" width="60"/>
          <el-table-column prop="createdAt" label="发布时间" width="150"><template #default="{row}">{{formatTime(row.createdAt)}}</template></el-table-column>
          <el-table-column label="操作" width="230"><template #default="{row}">
            <button class="op-btn" :class="{on:row.isTop}" @click="toggleTop(row)">{{row.isTop?'取消置顶':'📌置顶'}}</button>
            <button class="op-btn" :class="{on:row.isEssence}" @click="toggleEssence(row)">{{row.isEssence?'取消精华':'✨加精'}}</button>
            <button class="del-btn" @click="deletePost(row.id)">删除</button>
          </template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="标签管理" name="tags">
        <el-table :data="tags" stripe style="width:100%" class="warm-table">
          <el-table-column prop="id" label="ID" width="60"/>
          <el-table-column prop="name" label="标签名"><template #default="{row}"><span class="tag-name">#{{row.name}}</span></template></el-table-column>
          <el-table-column prop="postCount" label="帖子数" width="100"/>
          <el-table-column prop="createdAt" label="创建时间" width="160"><template #default="{row}">{{formatTime(row.createdAt)}}</template></el-table-column>
          <el-table-column label="操作" width="100"><template #default="{row}"><button class="del-btn" @click="deleteTag(row.id)">删除</button></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="公告管理" name="anns">
        <div class="ann-form">
          <el-input v-model="annTitle" placeholder="公告标题" style="width:260px"/>
          <el-input v-model="annContent" placeholder="公告内容" style="width:380px"/>
          <button class="op-btn on" @click="createAnn">📢 发布公告</button>
        </div>
        <el-table :data="anns" stripe style="width:100%;margin-top:14px" class="warm-table">
          <el-table-column prop="id" label="ID" width="60"/>
          <el-table-column prop="title" label="标题" width="180"/>
          <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip/>
          <el-table-column prop="createdAt" label="发布时间" width="160"><template #default="{row}">{{formatTime(row.createdAt)}}</template></el-table-column>
          <el-table-column label="操作" width="100"><template #default="{row}"><button class="del-btn" @click="deleteAnn(row.id)">删除</button></template></el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>
<script setup>
import {ref,onMounted} from 'vue'
import {adminApi} from '../api/post'
import {ElMessage,ElMessageBox} from 'element-plus'
const activeTab=ref('stats');const stats=ref(null);const users=ref([]);const posts=ref([]);const tags=ref([]);const anns=ref([]);const annTitle=ref('');const annContent=ref('')
async function loadStats(){stats.value=await adminApi.stats()}
async function loadUsers(){const r=await adminApi.users();users.value=r}
async function loadPosts(){const r=await adminApi.posts();posts.value=r}
async function loadTags(){const r=await adminApi.tags();tags.value=r}
async function loadAnns(){const r=await adminApi.announcements();anns.value=r}
async function deleteUser(id){try{await ElMessageBox.confirm('确定删除此用户？');await adminApi.deleteUser(id);ElMessage.success('已删除');loadUsers()}catch{}}
async function deletePost(id){try{await ElMessageBox.confirm('确定删除此帖子？');await adminApi.deletePost(id);ElMessage.success('已删除');loadPosts()}catch{}}
async function deleteTag(id){try{await ElMessageBox.confirm('确定删除此标签？');await adminApi.deleteTag(id);ElMessage.success('已删除');loadTags()}catch{}}
async function createAnn(){if(!annTitle.value.trim()){ElMessage.warning('请输入公告标题');return}await adminApi.createAnnouncement({title:annTitle.value,content:annContent.value});ElMessage.success('公告已发布');annTitle.value='';annContent.value='';loadAnns()}
async function deleteAnn(id){try{await ElMessageBox.confirm('确定删除此公告？');await adminApi.deleteAnnouncement(id);ElMessage.success('已删除');loadAnns()}catch{}}
async function toggleTop(row){await adminApi.setTop(row.id,!row.isTop);ElMessage.success('操作成功');loadPosts()}
async function toggleEssence(row){await adminApi.setEssence(row.id,!row.isEssence);ElMessage.success('操作成功');loadPosts()}
async function rebuildCounters(){
  try{
    const res=await adminApi.rebuildCounters()
    ElMessage.success(`已重算：帖子 ${res.postsScanned} 条 / 标签 ${res.tagsScanned} 个`)
    loadStats()
  }catch{}
}
function formatTime(t){if(!t)return'';const d=new Date(t);return`${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`}
onMounted(()=>{loadStats();loadUsers();loadPosts();loadTags();loadAnns()})
</script>
<style scoped>
.admin-title{font-size:22px;font-weight:700;margin-bottom:20px;display:flex;align-items:center;gap:8px}
.title-glow{color:var(--accent-warm);text-shadow:0 0 15px rgba(193,127,58,.35)}
.stats-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(180px,1fr));gap:16px}
.stats-tools{display:flex;align-items:center;gap:12px;flex-wrap:wrap;margin-top:16px}
.stats-hint{font-size:12px;color:var(--text-muted)}
.stat-card{background:var(--bg-card);backdrop-filter:blur(6px);-webkit-backdrop-filter:blur(6px);border-radius:var(--radius);padding:24px;text-align:center;border:1px solid var(--border-accent);transition:all var(--transition)}
.stat-card:hover{border-color:var(--accent-warm);box-shadow:var(--glow-warm);transform:translateY(-2px)}
.stat-num{font-size:32px;font-weight:800;color:var(--accent-primary)}
.stat-label{font-size:14px;color:var(--text-muted);margin-top:4px}
.role-tag{display:inline-block;padding:2px 10px;border-radius:12px;font-size:12px;font-weight:600}
.role-tag.admin{background:rgba(192,128,128,.15);color:var(--accent-rose);border:1px solid rgba(192,128,128,.35)}
.role-tag.user{background:rgba(111,78,55,.1);color:var(--accent-primary);border:1px solid var(--border-accent)}
.del-btn{padding:4px 10px;border-radius:6px;font-size:12px;font-weight:600;border:1px solid rgba(192,128,128,.35);cursor:pointer;background:transparent;color:var(--accent-rose);transition:all var(--transition)}
.del-btn:hover{background:rgba(192,128,128,.12);box-shadow:var(--glow-rose)}
.op-btn{padding:4px 10px;border-radius:6px;font-size:12px;font-weight:600;border:1px solid var(--border-light);cursor:pointer;background:transparent;color:var(--text-secondary);transition:all var(--transition);margin-right:4px}
.op-btn:hover{border-color:var(--accent-warm);color:var(--accent-warm)}
.op-btn.on{border-color:var(--accent-warm);color:var(--accent-warm);background:rgba(193,127,58,.1)}
.status-chip{display:inline-block;padding:1px 8px;border-radius:10px;font-size:11px;font-weight:700;margin-right:4px}
.status-chip.top{background:rgba(232,101,45,.15);color:#E8652D;border:1px solid rgba(232,101,45,.35)}
.status-chip.essence{background:rgba(184,134,11,.12);color:#B8860B;border:1px solid rgba(184,134,11,.35)}
.tag-name{font-weight:700;color:var(--accent-warm)}
.ann-form{display:flex;gap:10px;flex-wrap:wrap;align-items:center;margin-bottom:6px}
:deep(.warm-tabs .el-tabs__item){color:var(--text-secondary)!important}
:deep(.warm-tabs .el-tabs__item.is-active){color:var(--accent-primary)!important}
:deep(.warm-tabs .el-tabs__active-bar){background:var(--accent-primary)!important}
:deep(.warm-table){background:transparent!important}
:deep(.warm-table th){background:var(--bg-surface)!important;color:var(--text-secondary)!important;border-bottom-color:var(--border-accent)!important}
:deep(.warm-table td){background:transparent!important;color:var(--text-primary)!important;border-bottom-color:rgba(193,127,58,.1)!important}
:deep(.warm-table .el-table__row:hover td){background:rgba(193,127,58,.05)!important}

/* ═══ Lo-Fi 后台皮肤 ═══ */
.admin-title{
  font-size:24px;font-weight:900;letter-spacing:.01em;
  color:var(--text-primary);
}
.admin-title .title-glow{
  color:var(--accent-warm);
  text-shadow:0 0 18px rgba(217,142,74,.45);
  animation:floaty 5s ease-in-out infinite;
}
.stats-grid{gap:18px}
.stat-card{
  position:relative;
  padding:26px 22px 22px;
  border-radius:var(--radius);
  background:
    radial-gradient(120% 90% at 0% 0%, rgba(255,255,255,.5), transparent 58%),
    var(--paper);
  border:1px solid var(--border-light);
  box-shadow:var(--shadow-sm), inset 0 1px 0 rgba(255,255,255,.45);
  overflow:hidden;
  transition:transform var(--transition), box-shadow var(--transition), border-color var(--transition);
}
[data-theme="dark"] .stat-card{
  background:radial-gradient(120% 90% at 0% 0%, rgba(255,205,140,.05), transparent 58%),var(--paper);
}
.stat-card::after{
  content:'';position:absolute;left:0;right:0;top:0;height:3px;
  background:linear-gradient(90deg,var(--accent-warm),var(--accent-secondary),transparent);
  opacity:.55;
}
.stat-card:hover{transform:translateY(-4px);box-shadow:var(--shadow-md);border-color:var(--border-accent)}
.stat-num{
  font-family:'Georgia','Songti SC','Noto Serif SC',serif;
  font-size:38px;font-weight:900;line-height:1;
  color:var(--accent-primary);
  background:linear-gradient(135deg,var(--accent-primary),var(--accent-warm));
  -webkit-background-clip:text;background-clip:text;
  -webkit-text-fill-color:transparent;
  margin-bottom:8px;
}
.stat-label{font-size:12.5px;letter-spacing:.06em;color:var(--text-muted)}
.stats-tools{
  padding:10px 14px;border-radius:12px;
  background:var(--bg-surface);border:1px dashed var(--border-accent);
}
.stats-tools .op-btn{padding:7px 14px;border-radius:999px;font-weight:700}
.stats-hint{opacity:.85}
:deep(.warm-tabs .el-tabs__header){margin-bottom:18px}
:deep(.warm-tabs .el-tabs__item){font-weight:700}
</style>
