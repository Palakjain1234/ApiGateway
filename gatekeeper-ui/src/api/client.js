import axios from 'axios'

const client = axios.create({ baseURL: '/api' })

// Attach JWT to every request automatically
client.interceptors.request.use(config => {
  const token = localStorage.getItem('gk_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Redirect to login on 401
client.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      localStorage.removeItem('gk_token')
      localStorage.removeItem('gk_user')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export default client
