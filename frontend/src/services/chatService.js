import api from './api'

export const askQuestion = (documentId, question) => {
  return api.post('/chat/ask', { documentId, question })
}

export const getChatHistory = (documentId) => {
  return api.get(`/chat/history/${documentId}`)
}