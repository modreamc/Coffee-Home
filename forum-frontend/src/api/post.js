import request from './request'
export const postApi = {
  list: (params) => request.get('/posts', { params }).then(r => r.data),
  detail: (id) => request.get(`/posts/${id}`).then(r => r.data),
  create: (data) => request.post('/posts', data).then(r => r.data),
  update: (id, data) => request.put(`/posts/${id}`, data).then(r => r.data),
  delete: (id) => request.delete(`/posts/${id}`).then(r => r.data),
  // 搜索已改为分页：返回 { list, total, page, size }
  search: (keyword, params) => request.get('/posts/search', { params: { keyword, ...params } }).then(r => r.data),
  userPosts: (userId, params) => request.get(`/posts/user/${userId}`, { params }).then(r => r.data),
}
export const commentApi = {
  list: (postId, params) => request.get(`/comments/${postId}`, { params }).then(r => r.data),
  create: (postId, data) => request.post(`/comments/${postId}`, data).then(r => r.data),
  delete: (commentId) => request.delete(`/comments/${commentId}`).then(r => r.data),
}
export const likeApi = {
  toggle: (postId) => request.post(`/likes/${postId}`).then(r => r.data),
  status: (postId) => request.get(`/likes/${postId}/status`).then(r => r.data),
}
export const collectApi = {
  toggle: (postId) => request.post(`/collects/${postId}`).then(r => r.data),
  status: (postId) => request.get(`/collects/${postId}/status`).then(r => r.data),
  list: (params) => request.get('/collects', { params }).then(r => r.data),
}
export const imageApi = {
  upload: (file, signal) => {
    const form = new FormData()
    form.append('file', file)
    return request.post('/images/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      signal,
    }).then(r => r.data)
  },
}
export const adminApi = {
  stats: () => request.get('/admin/stats').then(r => r.data),
  users: () => request.get('/admin/users').then(r => r.data),
  posts: () => request.get('/admin/posts').then(r => r.data),
  deleteUser: (id) => request.delete(`/admin/users/${id}`).then(r => r.data),
  deletePost: (id) => request.delete(`/admin/posts/${id}`).then(r => r.data),
  setTop: (id, top) => request.put(`/admin/posts/${id}/top`, { top }).then(r => r.data),
  setEssence: (id, essence) => request.put(`/admin/posts/${id}/essence`, { essence }).then(r => r.data),
  tags: () => request.get('/admin/tags').then(r => r.data),
  deleteTag: (id) => request.delete(`/admin/tags/${id}`).then(r => r.data),
  announcements: () => request.get('/admin/announcements').then(r => r.data),
  createAnnouncement: (data) => request.post('/admin/announcements', data).then(r => r.data),
  deleteAnnouncement: (id) => request.delete(`/admin/announcements/${id}`).then(r => r.data),
  // 计数对账：按真实记录重算点赞/收藏/评论/话题计数
  rebuildCounters: () => request.post('/admin/stats/rebuild').then(r => r.data),
}
export const tagApi = {
  list: () => request.get('/tags').then(r => r.data),
}
/** 视频元信息：B站会抓标题/封面/时长/UP 主并下载封面到本地 */
export const videoApi = {
  meta: (url, opts = {}) => request.get('/videos/meta', { params: { url }, ...opts }).then(r => r.data),
}
export const userApi = {
  info: (id) => request.get(`/users/${id}`).then(r => r.data),
}
export const signinApi = {
  do: () => request.post('/signin').then(r => r.data),
  today: () => request.get('/signin/today').then(r => r.data),
}
export const notificationApi = {
  list: () => request.get('/notifications').then(r => r.data),
  readAll: () => request.post('/notifications/read').then(r => r.data),
}
export const announcementApi = {
  list: () => request.get('/announcements').then(r => r.data),
}
