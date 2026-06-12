import api from './api'

export const signup = (name, email, password) => {
  return api.post('/auth/signup', { name, email, password })
}

export const login = (email, password) => {
  return api.post('/auth/login', { email, password })
}