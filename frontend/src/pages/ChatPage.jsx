/* eslint-disable react-hooks/exhaustive-deps */
/* eslint-disable react-hooks/set-state-in-effect */
import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { askQuestion, getChatHistory } from '../services/chatService'
import { getDocuments } from '../services/documentService'

function ChatPage() {
  const { documentId } = useParams()
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const [messages, setMessages] = useState([])
  const [question, setQuestion] = useState('')
  const [loading, setLoading] = useState(false)
  const [documents, setDocuments] = useState([])
  const [documentName, setDocumentName] = useState('')
  const [chatPreviews, setChatPreviews] = useState({})
  const [sidebarOpen, setSidebarOpen] = useState(window.innerWidth >= 768)
  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth < 768) setSidebarOpen(false)
    }
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])
  const messagesEndRef = useRef(null)

  const loadDocuments = async () => {
    try {
      const response = await getDocuments()
      setDocuments(response.data)
      const doc = response.data.find(d => d.id === parseInt(documentId))
      if (doc) setDocumentName(doc.fileName)

      // Load first message preview for each document
      const previews = {}
      for (const doc of response.data) {
        try {
          const historyRes = await getChatHistory(doc.id)
          if (historyRes.data.length > 0) {
            const realMessages = historyRes.data.filter(msg =>
              !msg.question.includes('Generate exactly 5 flashcards') &&
              !msg.question.includes('JSON array')
            )
            if (realMessages.length > 0) {
              const firstQ = realMessages[0].question
              previews[doc.id] = firstQ.length > 40
                ? firstQ.substring(0, 40) + '...'
                : firstQ
            }
          }
        } catch {
          // no history for this doc
        }
      }
      setChatPreviews(previews)
    } catch {
      // silently fail
    }
  }

  const loadChatHistory = async () => {
    try {
      const response = await getChatHistory(documentId)
      
      // Filter out flashcard generation prompts
      const filtered = response.data.filter(msg => 
        !msg.question.includes('Generate exactly 5 flashcards') &&
        !msg.question.includes('JSON array')
      )
      
      const history = filtered.flatMap(msg => [
        { role: 'user', content: msg.question },
        {
          role: 'assistant',
          content: msg.answer,
          sourceDocument: msg.sourceDocument,
          pageNumber: msg.pageNumber
        }
      ])
      setMessages(history)
    } catch {
      // no history yet
    }
  }

  useEffect(() => {
    loadDocuments()
    loadChatHistory()
  }, [documentId])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const handleSend = async () => {
    if (!question.trim() || loading) return

    const userMessage = { role: 'user', content: question }
    setMessages(prev => [...prev, userMessage])

    // Update preview with first question
    if (!chatPreviews[documentId]) {
      setChatPreviews(prev => ({
        ...prev,
        [documentId]: question.length > 40
          ? question.substring(0, 40) + '...'
          : question
      }))
    }

    setQuestion('')
    setLoading(true)

    try {
      const response = await askQuestion(parseInt(documentId), question)
      const data = response.data
      const assistantMessage = {
        role: 'assistant',
        content: data.answer,
        sourceDocument: data.sourceDocument,
        pageNumber: data.pageNumber
      }
      setMessages(prev => [...prev, assistantMessage])
    } catch {
      setMessages(prev => [...prev, {
        role: 'assistant',
        content: 'Sorry, something went wrong. Please try again.',
      }])
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const formatFileName = (name) => {
    return name.length > 22 ? name.substring(0, 22) + '...' : name
  }

  const getInitial = () => {
    if (user?.name) return user.name.charAt(0).toUpperCase()
    if (user?.email) return user.email.charAt(0).toUpperCase()
    return 'A'
  }

  return (
    <div className="h-screen flex overflow-hidden bg-gray-50">

      {/* LEFT SIDEBAR */}
      <div className={`${sidebarOpen ? 'w-64' : 'w-0'} transition-all duration-300 bg-gray-900 flex flex-col overflow-hidden flex-shrink-0`}>

        {/* Sidebar header */}
        <div className="px-4 py-4 border-b border-gray-700">
          <div className="flex items-center justify-between">
            <span className="text-white font-bold text-lg">Doc Jarvis</span>
            <button
              onClick={() => navigate('/dashboard')}
              className="text-gray-400 hover:text-white text-xs transition-colors cursor-pointer"
            >
              Dashboard
            </button>
          </div>
        </div>

        {/* Document list */}
        <div className="flex-1 overflow-y-auto py-3">
          <p className="text-gray-500 text-xs uppercase tracking-wider px-4 mb-2">
            Your Documents
          </p>
          {documents.map((doc) => (
            <button
              key={doc.id}
              onClick={() => navigate(`/chat/${doc.id}`)}
              className={`w-full text-left px-4 py-3 hover:bg-gray-800 transition-colors cursor-pointer ${
                doc.id === parseInt(documentId) ? 'bg-gray-800 border-l-2 border-[#1D9E75]' : ''
              }`}
            >
              <div className="flex items-start gap-2">
                <span className="text-base mt-0.5">📕</span>
                <div className="flex-1 min-w-0">
                  <p className="text-gray-200 text-xs font-medium truncate">
                    {formatFileName(doc.fileName)}
                  </p>
                  {chatPreviews[doc.id] ? (
                    <p className="text-gray-500 text-xs mt-0.5 truncate">
                      "{chatPreviews[doc.id]}"
                    </p>
                  ) : (
                    <p className="text-gray-600 text-xs mt-0.5 italic">
                      No conversations yet
                    </p>
                  )}
                </div>
              </div>
            </button>
          ))}
        </div>

        {/* Sidebar footer - user info */}
        <div className="border-t border-gray-700 px-4 py-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2 min-w-0">
              <div className="w-7 h-7 rounded-full bg-[#1D9E75] text-white text-xs font-bold flex items-center justify-center flex-shrink-0">
                {getInitial()}
              </div>
              <div className="min-w-0">
                {user?.name && (
                  <p className="text-gray-200 text-xs font-medium truncate">{user.name}</p>
                )}
                <p className="text-gray-500 text-xs truncate">{user?.email}</p>
              </div>
            </div>
            <button
              onClick={handleLogout}
              className="text-gray-500 hover:text-red-400 text-xs transition-colors cursor-pointer ml-2 flex-shrink-0"
            >
              Logout
            </button>
          </div>
        </div>
      </div>

      {/* MAIN CHAT AREA */}
      <div className="flex-1 flex flex-col overflow-hidden">

        {/* Top bar */}
        <div className="bg-white border-b border-gray-200 px-4 py-3 flex items-center gap-3 flex-shrink-0">
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            className="text-gray-400 hover:text-gray-600 transition-colors cursor-pointer text-lg"
            title="Toggle sidebar"
          >
            ☰
          </button>
          <span className="text-sm font-medium text-gray-700 truncate">
            📄 {documentName || 'Loading...'}
          </span>
        </div>

        {/* Messages area */}
        <div className="flex-1 overflow-y-auto px-4 py-6">
          <div className="max-w-3xl mx-auto space-y-4">

            {/* Empty state */}
            {messages.length === 0 && !loading && (
              <div className="text-center py-20">
                <div className="text-5xl mb-4">🤖</div>
                <h3 className="text-lg font-medium text-gray-700 mb-2">
                  Ask anything about this document
                </h3>
                <p className="text-gray-400 text-sm">
                  I'll answer based only on the document content
                </p>
              </div>
            )}

            {/* Message bubbles */}
            {messages.map((msg, index) => (
              <div
                key={index}
                className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}
              >
                <div className={`max-w-[75%] flex flex-col gap-1 ${msg.role === 'user' ? 'items-end' : 'items-start'}`}>
                  <div className={`rounded-2xl px-4 py-3 text-sm leading-relaxed ${
                    msg.role === 'user'
                      ? 'bg-[#1D9E75] text-white rounded-br-sm'
                      : 'bg-white border border-gray-200 text-gray-800 rounded-bl-sm'
                  }`}>
                    {msg.content}
                  </div>

                  {/* Citation badge */}
                  {msg.role === 'assistant' && msg.sourceDocument && (
                    <span className="text-xs text-gray-400 bg-gray-100 px-2 py-1 rounded-full">
                      📎 {msg.sourceDocument}
                      {msg.pageNumber && ` · Page ${msg.pageNumber}`}
                    </span>
                  )}
                </div>
              </div>
            ))}

            {/* Loading indicator */}
            {loading && (
              <div className="flex justify-start">
                <div className="bg-white border border-gray-200 rounded-2xl rounded-bl-sm px-4 py-3">
                  <div className="flex gap-1 items-center">
                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></div>
                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }}></div>
                    <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }}></div>
                  </div>
                </div>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>
        </div>

        {/* Input area */}
        <div className="bg-white border-t border-gray-200 px-4 py-4 flex-shrink-0">
          <div className="max-w-3xl mx-auto flex gap-3">
            <textarea
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask a question about this document..."
              rows={1}
              className="flex-1 border border-gray-200 rounded-xl px-4 py-2.5 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-[#1D9E75] focus:border-transparent"
            />
            <button
              onClick={handleSend}
              disabled={loading || !question.trim()}
              className="bg-[#1D9E75] hover:bg-[#178a63] text-white px-5 py-2.5 rounded-xl text-sm font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Send
            </button>
          </div>
          <p className="text-center text-xs text-gray-400 mt-2">
            Press Enter to send · Shift+Enter for new line
          </p>
        </div>
      </div>
    </div>
  )
}

export default ChatPage