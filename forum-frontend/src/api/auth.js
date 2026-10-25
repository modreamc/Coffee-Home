import request from './request'
export const authApi = {
  login: (data) => request.post('/auth/login', data).then(r => r.data),
  register: (data) => request.post('/auth/register', data).then(r => r.data),
  me: () => request.get('/auth/me').then(r => r.data),
}
