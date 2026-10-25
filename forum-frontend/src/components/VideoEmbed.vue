<template>
  <div class="video-card">
    <div class="vc-head">
      <span class="vc-provider">{{ info.label }}</span>
      <span class="vc-hint">{{ meta.title || info.hint }}</span>
      <a :href="info.href" target="_blank" rel="noopener noreferrer" class="vc-open">在新窗口打开 ↗</a>
    </div>

    <div v-if="info.embed" class="vc-frame">
      <iframe
        v-if="active"
        :src="info.embed"
        :title="meta.title || info.label + ' 视频'"
        loading="lazy"
        frameborder="0"
        scrolling="no"
        allow="autoplay; fullscreen; picture-in-picture; encrypted-media"
        allowfullscreen
        referrerpolicy="no-referrer-when-downgrade"
      />
      <button v-else type="button" class="vc-poster" :class="{ 'has-cover': !!meta.cover }" @click="active = true">
        <img v-if="meta.cover" :src="meta.cover" class="vc-poster-img" alt=""/>
        <span class="vc-play-lg" aria-hidden="true">▶</span>
        <span v-if="!meta.cover" class="vc-poster-label">点击播放 · {{ info.label }}</span>
        <span v-if="metaLine" class="vc-poster-meta">{{ metaLine }}</span>
      </button>
    </div>

    <a v-else :href="info.href" target="_blank" rel="noopener noreferrer" class="vc-fallback">
      <span class="vc-play" aria-hidden="true">▶</span>
      <span class="vc-fb-text">
        <b>{{ meta.title || info.label + ' 视频' }}</b>
        <em>{{ info.hint || '该平台不支持站内播放，点击前往观看' }}</em>
      </span>
    </a>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { videoApi } from '../api/post'

const props = defineProps({
  /** { url, title, cover, provider, videoId, durationSeconds, owner } 或纯字符串 */
  video: { type: [Object, String], default: null },
  url: { type: String, default: '' },
})

const active = ref(false)
const meta = reactive({
  url: '',
  title: '',
  cover: '',
  owner: '',
  durationSeconds: 0,
})

function seed() {
  const source = props.video
  if (typeof source === 'string') {
    meta.url = source
  } else if (source && typeof source === 'object') {
    meta.url = source.url || ''
    meta.title = source.title || ''
    meta.cover = source.cover || ''
    meta.owner = source.owner || ''
    meta.durationSeconds = source.durationSeconds || 0
  }
  if (!meta.url) meta.url = props.url
}
seed()

const info = computed(() => parse(meta.url))

const metaLine = computed(() => {
  const parts = []
  if (meta.durationSeconds) parts.push(formatDuration(meta.durationSeconds))
  if (meta.owner) parts.push('UP 主：' + meta.owner)
  return parts.join(' · ')
})

onMounted(async () => {
  // 老数据只有链接、没有封面时，补抓一次（后端有缓存）
  if (!meta.url || (meta.title && meta.cover)) return
  try {
    const data = await videoApi.meta(meta.url, { skipAuthRedirect: true })
    if (data) {
      meta.title = meta.title || data.title || ''
      meta.cover = meta.cover || data.cover || ''
      meta.owner = meta.owner || data.owner || ''
      meta.durationSeconds = meta.durationSeconds || data.durationSeconds || 0
    }
  } catch {
    /* 抓不到就退回默认封面 */
  }
})

function formatDuration(seconds) {
  const m = Math.floor(seconds / 60)
  const s = Math.floor(seconds % 60)
  return `${m}:${String(s).padStart(2, '0')}`
}

function parse(raw) {
  let u
  try {
    u = new URL(raw)
  } catch {
    return { label: '视频', href: raw, embed: null, hint: '链接格式不正确' }
  }
  const host = u.hostname.replace(/^www\./i, '').toLowerCase()

  if (host.endsWith('bilibili.com')) {
    const bv = u.pathname.match(/\/video\/(BV[0-9A-Za-z]{10})/)
    if (bv) {
      return {
        label: '哔哩哔哩',
        embed: `https://player.bilibili.com/player.html?isOutside=true&bvid=${bv[1]}&page=1&high_quality=1&danmaku=0&autoplay=0`,
        href: raw,
        hint: 'B 站播放器 · 可站内播放',
      }
    }
    const av = u.pathname.match(/\/video\/av(\d+)/i)
    if (av) {
      return {
        label: '哔哩哔哩',
        embed: `https://player.bilibili.com/player.html?isOutside=true&aid=${av[1]}&page=1&high_quality=1&danmaku=0&autoplay=0`,
        href: raw,
        hint: 'B 站播放器 · 可站内播放',
      }
    }
    return { label: '哔哩哔哩', embed: null, href: raw, hint: '该链接无法直接嵌入，点击前往观看' }
  }
  if (host === 'b23.tv') {
    return { label: '哔哩哔哩', embed: null, href: raw, hint: 'B 站短链，点击前往观看' }
  }

  if (host.endsWith('youtube.com') || host === 'youtu.be') {
    let id = ''
    if (host === 'youtu.be') id = u.pathname.slice(1).split('/')[0]
    else if (u.searchParams.get('v')) id = u.searchParams.get('v')
    else {
      const m = u.pathname.match(/\/(?:embed|shorts)\/([\w-]{11})/)
      if (m) id = m[1]
    }
    if (id) {
      return { label: 'YouTube', embed: `https://www.youtube.com/embed/${id}?rel=0`, href: raw, hint: 'YouTube 播放器' }
    }
  }

  if (host.endsWith('v.qq.com')) {
    const m = u.pathname.match(/\/x\/page\/(\w+)\.html/)
    if (m) return { label: '腾讯视频', embed: `https://v.qq.com/txp/iframe/player.html?vid=${m[1]}`, href: raw, hint: '腾讯视频播放器' }
  }

  if (host.endsWith('youku.com')) {
    const m = u.pathname.match(/\/v_show\/id_([\w=]+)\.html/)
    if (m) return { label: '优酷', embed: `https://player.youku.com/embed/${m[1]}`, href: raw, hint: '优酷播放器' }
  }

  if (host.endsWith('douyin.com')) {
    return { label: '抖音', embed: null, href: raw, hint: '抖音未开放通用站内播放，点击前往观看' }
  }
  if (host.endsWith('ixigua.com')) {
    return { label: '西瓜视频', embed: null, href: raw, hint: '点击前往观看' }
  }
  return { label: '视频', embed: null, href: raw, hint: '点击前往观看' }
}
</script>

<style scoped>
.video-card{
  position:relative;
  border:1px solid var(--border-light);
  border-radius:var(--radius);
  background:var(--paper);
  box-shadow:var(--shadow-sm);
  overflow:hidden;
  margin-bottom:18px;
}
.vc-head{
  display:flex;align-items:center;gap:10px;
  padding:10px 14px;
  border-bottom:1px dashed var(--border-light);
  background:linear-gradient(180deg, rgba(217,142,74,.06), transparent);
}
.vc-provider{
  flex-shrink:0;
  font-size:11px;font-weight:800;letter-spacing:.04em;
  color:var(--accent-warm);
  background:rgba(217,142,74,.12);
  border:1px solid rgba(217,142,74,.28);
  padding:2px 10px;border-radius:999px;
}
.vc-hint{
  font-size:11.5px;color:var(--text-muted);
  overflow:hidden;text-overflow:ellipsis;white-space:nowrap;min-width:0;
}
.vc-open{
  margin-left:auto;flex-shrink:0;
  font-size:11.5px;font-weight:700;color:var(--accent-primary);
  text-decoration:none;
}
.vc-open:hover{color:var(--accent-warm);text-decoration:underline}
.vc-frame{position:relative;width:100%;aspect-ratio:16/9;background:#0b0a0c}
.vc-frame iframe{position:absolute;inset:0;width:100%;height:100%;border:0}
.vc-poster{
  position:absolute;inset:0;width:100%;height:100%;
  border:0;cursor:pointer;padding:0;
  display:flex;flex-direction:column;align-items:center;justify-content:center;gap:8px;
  background:
    radial-gradient(60% 80% at 20% 18%, rgba(217,142,74,.26), transparent 62%),
    radial-gradient(52% 72% at 84% 82%, rgba(192,128,124,.18), transparent 62%),
    linear-gradient(135deg, #2B2129, #161115);
  color:#fff;overflow:hidden;
}
.vc-poster::before{
  content:'';position:absolute;inset:0;
  background:linear-gradient(180deg, rgba(16,12,16,.28), rgba(16,12,16,.66));
}
.vc-poster-img{
  position:absolute;inset:0;width:100%;height:100%;object-fit:cover;
  transform:scale(1.02);
  transition:transform .8s var(--ease-soft), filter .5s ease;
  filter:saturate(.95);
}
.vc-poster:hover .vc-poster-img{transform:scale(1.06)}
.vc-play-lg{
  position:relative;z-index:1;
  width:58px;height:58px;border-radius:50%;
  display:flex;align-items:center;justify-content:center;
  background:linear-gradient(135deg,var(--accent-warm),var(--accent-primary));
  color:#fff;font-size:18px;padding-left:4px;
  box-shadow:0 0 0 6px rgba(255,255,255,.14), var(--glow-warm);
  transition:transform var(--transition);
}
.vc-poster:hover .vc-play-lg{transform:scale(1.06)}
.vc-poster-label{
  position:relative;z-index:1;
  font-size:13px;font-weight:800;letter-spacing:.04em;
  text-shadow:0 2px 10px rgba(0,0,0,.7);
}
.vc-poster-meta{
  position:relative;z-index:1;
  font-size:11px;color:rgba(255,245,235,.78);
  text-shadow:0 1px 6px rgba(0,0,0,.7);
}
/* 有真实封面时，播放按钮居中、时长/UP 主挪到左下角，避免压住封面文字 */
.vc-poster.has-cover::after{
  content:'';position:absolute;left:0;right:0;bottom:0;height:38%;
  background:linear-gradient(0deg, rgba(10,8,10,.62), transparent);
}
.vc-poster.has-cover .vc-poster-meta{
  position:absolute;left:14px;bottom:12px;z-index:2;
}
.vc-poster.has-cover .vc-play-lg{width:52px;height:52px;font-size:16px}
.vc-fallback{
  display:flex;align-items:center;gap:14px;
  padding:22px 18px;
  text-decoration:none;
  background:
    radial-gradient(80% 120% at 0% 0%, rgba(217,142,74,.12), transparent 62%),
    var(--bg-surface);
  transition:background var(--transition);
}
.vc-fallback:hover{background:rgba(217,142,74,.14)}
.vc-play{
  flex-shrink:0;width:44px;height:44px;border-radius:50%;
  display:flex;align-items:center;justify-content:center;
  background:linear-gradient(135deg,var(--accent-warm),var(--accent-primary));
  color:#fff;font-size:14px;padding-left:3px;
  box-shadow:var(--glow-warm);
}
.vc-fb-text{display:flex;flex-direction:column;gap:3px;min-width:0}
.vc-fb-text b{font-size:13.5px;color:var(--text-primary)}
.vc-fb-text em{font-style:normal;font-size:12px;color:var(--text-muted)}
[data-theme="dark"] .vc-frame{background:#000}
</style>
