/* eslint-disable react-hooks/exhaustive-deps */
/* eslint-disable react-hooks/set-state-in-effect */
import { useState, useEffect, useRef } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { askQuestion, getChatHistory } from '../services/chatService'
import { getDocuments } from '../services/documentService'

function ChatPage() {
  const { documentId } = useParams()
  const navigate = useNavigate()
  const [messages, setMessages] = useState([])
  const [question, setQuestion] = useState('')
  const [loading, setLoading] = useState(false)
  const [documentName, setDocumentName] = useState('')
  const messagesEndRef = useRef(null)

  const loadDocumentName = async () => {
    try {
      const response = await getDocuments()
      const doc = response.data.find(d => d.id === parseInt(documentId))
      if (doc) setDocumentName(doc.fileName)
    } catch {
      // silently fail
    }
  }

  const loadChatHistory = async () => {
    try {
      const response = await getChatHistory(documentId)
      const history = response.data.flatMap(msg => [
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
    loadDocumentName()
    loadChatHistory()
  }, [documentId])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const handleSend = async () => {
    if (!question.trim() || loading) return

    const userMessage = { role: 'user', content: question }
    setMessages(prev => [...prev, userMessage])
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

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <Navbar />

      {/* Document title bar */}
      <div className="bg-white border-b border-gray-200 px-6 py-3">
        <div className="max-w-4xl mx-auto flex items-center gap-3">
          <button
            onClick={() => navigate('/dashboard')}
            className="text-gray-400 hover:text-gray-600 transition-colors text-sm"
          >
            ← Back
          </button>
          <span className="text-gray-300">|</span>
          <span className="text-sm font-medium text-gray-700 truncate">
            {documentName || 'Loading...'}
          </span>
        </div>
      </div>

      {/* Messages area */}
      <div className="flex-1 overflow-y-auto px-4 py-6">
        <div className="max-w-4xl mx-auto space-y-4">

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
              <div className={`max-w-[75%] ${msg.role === 'user' ? 'items-end' : 'items-start'} flex flex-col gap-1`}>
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
      <div className="bg-white border-t border-gray-200 px-4 py-4">
        <div className="max-w-4xl mx-auto flex gap-3">
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
  )
}

export default ChatPage