<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-art" aria-hidden="true">
        <span class="art-glow"/>
        <span class="art-window"><i class="art-sky"/><i class="art-sun"/><i class="art-moon"/></span>
        <div class="art-cup">
          <span class="steam"><i/><i/><i/></span>
          <span class="cup-body"/>
          <span class="cup-handle"/>
          <span class="saucer"/>
        </div>
        <span class="art-note">COFFEE HARBOR</span>
      </div>

      <div class="auth-body">
        <h2 class="auth-title">注册 咖啡港湾</h2>
        <p class="auth-desc">点亮你的第一盏灯，加入这片港湾</p>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleRegister">
          <el-form-item label="用户名" prop="username"><el-input v-model="form.username" placeholder="3-20 位字母 / 数字 / 中文" size="large" /></el-form-item>
          <el-form-item label="密码" prop="password"><el-input v-model="form.password" type="password" placeholder="至少 6 位密码" size="large" show-password /></el-form-item>
          <el-form-item label="昵称（选填）" prop="nickname"><el-input v-model="form.nickname" placeholder="给自己起个昵称" size="large" /></el-form-item>
          <el-form-item label="邮箱（选填）" prop="email"><el-input v-model="form.email" placeholder="example@email.com" size="large" /></el-form-item>
          <el-form-item>
            <button type="submit" class="warm-btn secondary auth-submit" :disabled="loading">{{ loading?'注册中…':'✦ 注册' }}</button>
          </el-form-item>
        </el-form>

        <div class="auth-footer">已有账号？<router-link to="/login">立即登录</router-link></div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null); const loading = ref(false)
const form = reactive({ username:'', password:'', nickname:'', email:'' })
const rules = { username:[{required:true,message:'请输入用户名',trigger:'blur'},{min:3,max:20,message:'3-20位',trigger:'blur'}], password:[{required:true,message:'请输入密码',trigger:'blur'},{min:6,message:'至少6位',trigger:'blur'}], email:[{type:'email',message:'邮箱格式不正确',trigger:'blur'}] }
async function handleRegister(){
  const valid = await formRef.value.validate().catch(()=>false)
  if(!valid)return; loading.value=true
  try{ await userStore.register(form); ElMessage.success('注册成功'); router.push('/') }
  catch(e){} finally{ loading.value=false }
}
</script>
<style scoped>
.auth-page{
  min-height:calc(100vh - 66px);
  display:flex;align-items:center;justify-content:center;
  padding:36px 16px 56px;position:relative;
}
.auth-card{
  width:100%;max-width:430px;overflow:hidden;
  background:var(--paper);
  border:1px solid var(--border-light);
  border-radius:var(--radius-lg);
  box-shadow:var(--shadow-md), inset 0 1px 0 rgba(255,255,255,.5);
  animation:cardIn .7s var(--ease-soft) both;
}
@keyframes cardIn{from{opacity:0;transform:translateY(18px) scale(.98)}to{opacity:1;transform:none}}

.auth-art{
  position:relative;height:148px;overflow:hidden;
  background:linear-gradient(180deg, var(--sky-top), var(--sky-bottom));
  border-bottom:1px solid var(--border-light);
}
.art-glow{
  position:absolute;inset:0;
  background:radial-gradient(90% 80% at 50% 120%, rgba(255,214,150,.5), transparent 70%);
  opacity:var(--lamp-opacity);transition:opacity .6s ease;
}
.art-window{
  position:absolute;left:-4%;top:-30%;width:108%;height:130%;
  background:
    linear-gradient(90deg, transparent 0 48%, rgba(122,78,51,.28) 48% 52%, transparent 52%),
    linear-gradient(180deg, transparent 0 44%, rgba(122,78,51,.28) 44% 48%, transparent 48%);
  border:10px solid rgba(122,78,51,.22);
  border-radius:8px;opacity:.55;
}
[data-theme="dark"] .art-window{border-color:rgba(232,190,140,.14)}
.art-sky{position:absolute;inset:0;background:linear-gradient(180deg, rgba(255,255,255,.16), transparent 60%)}
.art-sun{
  position:absolute;right:16%;top:16%;width:46px;height:46px;border-radius:50%;
  background:radial-gradient(circle at 38% 34%,#FFF9E6,#FFCA7D);
  box-shadow:0 0 40px 16px rgba(255,203,122,.4);
  opacity:var(--sun-opacity);transition:opacity .6s ease;
}
.art-moon{
  position:absolute;right:18%;top:14%;width:38px;height:38px;border-radius:50%;
  background:radial-gradient(circle at 36% 32%,#FFF9E8,#E8D9B0);
  box-shadow:0 0 26px 8px rgba(240,220,170,.35);
  opacity:var(--moon-opacity);transition:opacity .6s ease;
}
.art-moon::after{
  content:'';position:absolute;left:11px;top:-4px;width:32px;height:32px;border-radius:50%;
  background:var(--sky-bottom);opacity:.9;
}
.art-cup{position:absolute;left:50%;bottom:14px;width:64px;height:52px;transform:translateX(-50%)}
.cup-body{
  position:absolute;left:8px;bottom:8px;width:38px;height:30px;border-radius:4px 4px 12px 12px;
  background:linear-gradient(160deg,#FFFDF8,#EADBC8);
  box-shadow:inset 0 -5px 0 rgba(0,0,0,.05), 0 10px 22px -14px rgba(60,36,20,.9);
}
.cup-body::after{
  content:'';position:absolute;left:4px;right:4px;top:4px;height:6px;border-radius:99px;
  background:linear-gradient(180deg,#6B4A32,#8A5A3B);
}
.cup-handle{
  position:absolute;left:43px;bottom:15px;width:15px;height:15px;border-radius:50%;
  border:3px solid #EADBC8;border-left-color:transparent;transform:rotate(-14deg);
}
.saucer{
  position:absolute;left:2px;bottom:2px;width:50px;height:5px;border-radius:99px;
  background:linear-gradient(90deg,#EADBC8,#D3B99C);opacity:.9;
}
.steam{position:absolute;left:12px;bottom:38px;width:30px;height:26px;display:flex;justify-content:space-between}
.steam i{
  display:block;width:5px;height:5px;border-radius:50%;
  background:rgba(255,252,246,.9);filter:blur(.4px);opacity:0;
  animation:steamRise 3.6s ease-in-out infinite;
}
.steam i:nth-child(2){animation-delay:1.2s}
.steam i:nth-child(3){animation-delay:2.4s}
.art-note{
  position:absolute;left:18px;bottom:12px;
  font-size:9px;letter-spacing:.28em;font-weight:800;color:rgba(255,248,238,.7);
}
.auth-body{padding:26px 30px 28px}
.auth-title{
  font-family:'Georgia','Songti SC','Noto Serif SC',serif;
  font-size:22px;font-weight:900;text-align:center;color:var(--text-primary);margin-bottom:6px;
}
.auth-desc{text-align:center;font-size:13px;color:var(--text-secondary);margin-bottom:20px}
.auth-submit{width:100%;height:46px;font-size:15px;justify-content:center;border-radius:var(--radius-pill)}
.auth-footer{text-align:center;margin-top:16px;font-size:13.5px;color:var(--text-secondary)}
.auth-footer a{color:var(--accent-primary);font-weight:800}
.auth-footer a:hover{color:var(--accent-warm)}
:deep(.el-form-item){margin-bottom:15px}
:deep(.el-form-item__label){font-size:12.5px;letter-spacing:.04em}
@media(max-width:480px){
  .auth-body{padding:20px 20px 24px}
  .auth-art{height:120px}
}
</style>
