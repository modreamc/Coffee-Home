<template>
  <div id="forum-app">
    <!-- 环境光层：纯装饰，不参与交互 -->
    <div class="ambient" aria-hidden="true">
      <div class="ambient-wall"></div>
      <div class="ambient-sunbeam"></div>
      <div class="ambient-lamp"></div>
      <div class="ambient-dust">
        <span v-for="n in 14" :key="`d${n}`" />
      </div>
      <div class="ambient-rain">
        <span v-for="n in 18" :key="`r${n}`" />
      </div>
      <div class="ambient-mist"></div>
      <div class="ambient-vignette"></div>
      <div class="ambient-grain"></div>
    </div>

    <div class="app-wrap">
      <Navbar />
      <router-view v-slot="{ Component }">
        <transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>
  </div>
</template>

<script setup>
import Navbar from './components/Navbar.vue'
import { onMounted } from 'vue'
import { useUserStore } from './stores/user'
const userStore = useUserStore()
onMounted(() => { userStore.fetchUserInfo() })
</script>

<style>
/* ── 页面切换：淡入 + 轻推 + 微模糊 ── */
.page-fade-enter-active { transition: opacity .38s var(--ease-soft), transform .38s var(--ease-soft), filter .38s ease; }
.page-fade-leave-active { transition: opacity .22s ease, transform .22s ease, filter .22s ease; }
.page-fade-enter-from { opacity: 0; transform: translateY(14px) scale(.995); filter: blur(3px); }
.page-fade-leave-to   { opacity: 0; transform: translateY(-8px); filter: blur(2px); }

/* ── 环境层 ── */
.ambient {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  overflow: hidden;
}

/* 墙面：极淡的斜纹 + 顶部暖光，制造"室内"感 */
.ambient-wall {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(120% 80% at 50% -18%, rgba(255, 226, 182, 0.30), transparent 62%),
    repeating-linear-gradient(115deg, rgba(122, 78, 51, 0.022) 0 2px, transparent 2px 9px),
    linear-gradient(180deg, var(--bg-body), var(--bg-deep));
}
[data-theme="dark"] .ambient-wall {
  background:
    radial-gradient(120% 80% at 50% -18%, rgba(242, 192, 119, 0.10), transparent 62%),
    repeating-linear-gradient(115deg, rgba(242, 192, 119, 0.018) 0 2px, transparent 2px 9px),
    linear-gradient(180deg, var(--bg-body), var(--bg-deep));
}

/* 白天：从左上窗子斜射进来的一束阳光 */
.ambient-sunbeam {
  position: absolute;
  top: -28vh;
  left: -14vw;
  width: 58vw;
  height: 150vh;
  opacity: var(--sun-opacity);
  background: linear-gradient(
    104deg,
    transparent 0%,
    rgba(255, 226, 170, 0.20) 26%,
    rgba(255, 210, 150, 0.10) 44%,
    transparent 66%
  );
  transform: rotate(6deg);
  filter: blur(12px);
  animation: beamBreathe 11s ease-in-out infinite;
  transition: opacity .6s ease;
}
@keyframes beamBreathe {
  0%, 100% { opacity: calc(var(--sun-opacity) * 0.75); transform: rotate(6deg) translateX(0); }
  50%      { opacity: var(--sun-opacity); transform: rotate(6deg) translateX(18px); }
}
/* 白天阳光里的树影光斑 */
.ambient-sunbeam::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 46px 26px at 28% 34%, rgba(96, 116, 74, 0.10), transparent 70%),
    radial-gradient(ellipse 30px 18px at 44% 52%, rgba(96, 116, 74, 0.08), transparent 70%),
    radial-gradient(ellipse 52px 28px at 66% 26%, rgba(96, 116, 74, 0.09), transparent 72%),
    radial-gradient(ellipse 26px 16px at 78% 60%, rgba(96, 116, 74, 0.07), transparent 72%);
  filter: blur(6px);
  opacity: calc(var(--sun-opacity) * 0.95);
  animation: leafDapple 15s ease-in-out infinite;
}
@keyframes leafDapple {
  0%, 100% { transform: translate(0, 0); }
  50%      { transform: translate(7px, 5px); }
}

/* 夜晚：右上角一盏常亮的落地灯 */
.ambient-lamp {
  position: absolute;
  top: -18vh;
  right: -6vw;
  width: 54vw;
  height: 62vh;
  background: radial-gradient(
    closest-side,
    var(--lamp-color),
    rgba(242, 192, 119, 0.08) 52%,
    transparent 74%
  );
  filter: blur(8px);
  opacity: var(--lamp-opacity);
  animation: lampFlicker 7.5s ease-in-out infinite;
  transition: opacity .6s ease;
}

/* 浮尘：白天是阳光里的尘土，夜晚变成暖色光点 */
.ambient-dust span {
  position: absolute;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--dust-color);
  filter: blur(0.6px);
  opacity: 0;
  animation: dustFloat linear infinite;
}
.ambient-dust span:nth-child(1) { left: 8%;  top: 78%; animation-duration: 17s; animation-delay: 0s; }
.ambient-dust span:nth-child(2) { left: 16%; top: 92%; animation-duration: 22s; animation-delay: 2.5s; width: 3px; height: 3px; }
.ambient-dust span:nth-child(3) { left: 24%; top: 84%; animation-duration: 19s; animation-delay: 5s; }
.ambient-dust span:nth-child(4) { left: 33%; top: 96%; animation-duration: 25s; animation-delay: 1.2s; width: 5px; height: 5px; }
.ambient-dust span:nth-child(5) { left: 41%; top: 80%; animation-duration: 21s; animation-delay: 7s; }
.ambient-dust span:nth-child(6) { left: 52%; top: 94%; animation-duration: 18s; animation-delay: 3.4s; width: 3px; height: 3px; }
.ambient-dust span:nth-child(7) { left: 61%; top: 86%; animation-duration: 24s; animation-delay: 6.2s; }
.ambient-dust span:nth-child(8) { left: 69%; top: 90%; animation-duration: 20s; animation-delay: 0.8s; width: 5px; height: 5px; }
.ambient-dust span:nth-child(9) { left: 76%; top: 82%; animation-duration: 23s; animation-delay: 4.6s; }
.ambient-dust span:nth-child(10){ left: 84%; top: 95%; animation-duration: 19s; animation-delay: 8s; width: 3px; height: 3px; }
.ambient-dust span:nth-child(11){ left: 91%; top: 88%; animation-duration: 26s; animation-delay: 2s; }
.ambient-dust span:nth-child(12){ left: 96%; top: 80%; animation-duration: 20s; animation-delay: 9s; width: 4px; height: 4px; }
.ambient-dust span:nth-child(13){ left: 12%; top: 88%; animation-duration: 28s; animation-delay: 11s; }
.ambient-dust span:nth-child(14){ left: 47%; top: 90%; animation-duration: 22s; animation-delay: 13s; width: 3px; height: 3px; }

/* 夜晚：窗外雨丝 */
.ambient-rain {
  position: absolute;
  inset: 0;
  opacity: var(--rain-opacity);
  transition: opacity .6s ease;
}
.ambient-rain span {
  position: absolute;
  top: -20vh;
  width: 1px;
  height: 74px;
  border-radius: 999px;
  background: linear-gradient(180deg, transparent, rgba(190, 222, 235, 0.55), transparent);
  animation: rainFall linear infinite;
}
.ambient-rain span:nth-child(odd)  { transform: rotate(11deg); }
.ambient-rain span:nth-child(3n)   { height: 96px; opacity: .8; }
.ambient-rain span:nth-child(4n+1) { height: 52px; opacity: .55; }
.ambient-rain span:nth-child(1)  { left: 4%;  animation-duration: 1.5s; animation-delay: -0.3s; }
.ambient-rain span:nth-child(2)  { left: 11%; animation-duration: 1.9s; animation-delay: -1.1s; }
.ambient-rain span:nth-child(3)  { left: 18%; animation-duration: 1.6s; animation-delay: -0.6s; }
.ambient-rain span:nth-child(4)  { left: 25%; animation-duration: 2.2s; animation-delay: -1.6s; }
.ambient-rain span:nth-child(5)  { left: 32%; animation-duration: 1.45s; animation-delay: -0.9s; }
.ambient-rain span:nth-child(6)  { left: 39%; animation-duration: 2.05s; animation-delay: -1.9s; }
.ambient-rain span:nth-child(7)  { left: 46%; animation-duration: 1.7s; animation-delay: -0.2s; }
.ambient-rain span:nth-child(8)  { left: 53%; animation-duration: 2.3s; animation-delay: -1.3s; }
.ambient-rain span:nth-child(9)  { left: 60%; animation-duration: 1.55s; animation-delay: -0.7s; }
.ambient-rain span:nth-child(10) { left: 67%; animation-duration: 2.1s; animation-delay: -1.8s; }
.ambient-rain span:nth-child(11) { left: 74%; animation-duration: 1.65s; animation-delay: -0.4s; }
.ambient-rain span:nth-child(12) { left: 81%; animation-duration: 2.25s; animation-delay: -1.5s; }
.ambient-rain span:nth-child(13) { left: 88%; animation-duration: 1.5s; animation-delay: -1s; }
.ambient-rain span:nth-child(14) { left: 94%; animation-duration: 1.95s; animation-delay: -0.5s; }
.ambient-rain span:nth-child(15) { left: 8%;  animation-duration: 2.15s; animation-delay: -2.1s; }
.ambient-rain span:nth-child(16) { left: 36%; animation-duration: 1.75s; animation-delay: -0.1s; }
.ambient-rain span:nth-child(17) { left: 64%; animation-duration: 2.0s;  animation-delay: -2.4s; }
.ambient-rain span:nth-child(18) { left: 90%; animation-duration: 1.6s;  animation-delay: -1.2s; }

/* 夜晚：贴地的雨雾 */
.ambient-mist {
  position: absolute;
  left: -10%;
  right: -10%;
  bottom: -6%;
  height: 36vh;
  pointer-events: none;
  background: linear-gradient(180deg, transparent, rgba(150, 180, 200, 0.10) 50%, rgba(150, 180, 200, 0.16));
  filter: blur(24px);
  opacity: calc(var(--rain-opacity) * 0.9);
  transition: opacity .6s ease;
  animation: mistDrift 20s ease-in-out infinite;
}
@keyframes mistDrift {
  0%, 100% { transform: translateX(-2%); }
  50%      { transform: translateX(2%); }
}

/* 暗角：把注意力收进页面中间 */
.ambient-vignette {
  position: absolute;
  inset: 0;
  background: radial-gradient(130% 100% at 50% 42%, transparent 55%, var(--vignette) 100%);
}

/* 颗粒噪点：Lo-Fi 的"磁带底噪"视觉版 */
.ambient-grain {
  position: absolute;
  inset: -20%;
  opacity: var(--grain-opacity);
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='140' height='140'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.85' numOctaves='3' stitchTiles='stitch'/%3E%3CfeColorMatrix type='saturate' values='0'/%3E%3C/filter%3E%3Crect width='140' height='140' filter='url(%23n)'/%3E%3C/svg%3E");
  animation: grainShift 1.1s steps(2) infinite;
  mix-blend-mode: multiply;
}
[data-theme="dark"] .ambient-grain { mix-blend-mode: screen; }
@keyframes grainShift {
  0%   { transform: translate3d(0, 0, 0); }
  50%  { transform: translate3d(-1.5%, 1%, 0); }
  100% { transform: translate3d(1%, -1.5%, 0); }
}

@media (max-width: 900px) {
  .ambient-sunbeam { width: 90vw; opacity: calc(var(--sun-opacity) * 0.6); }
  .ambient-lamp { width: 110vw; }
}
</style>
