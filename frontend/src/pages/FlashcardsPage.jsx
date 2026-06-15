/* eslint-disable react-hooks/exhaustive-deps */
/* eslint-disable react-hooks/set-state-in-effect */
import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { getDocuments } from '../services/documentService'
import { generateFlashcards } from '../services/chatService';

function FlashcardsPage() {
  const { documentId } = useParams()
  const navigate = useNavigate()
  const [flashcards, setFlashcards] = useState([])
  const [flipped, setFlipped] = useState({})
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [documentName, setDocumentName] = useState('')
  const [generated, setGenerated] = useState(false)

  const loadDocumentName = async () => {
    try {
      const response = await getDocuments()
      const doc = response.data.find(d => d.id === parseInt(documentId))
      if (doc) setDocumentName(doc.fileName)
    } catch {
      // silently fail
    }
  }

  useEffect(() => {
    loadDocumentName()
  }, [documentId])

  const loadFlashcards = async () => {
    setLoading(true)
    setError('')
    setFlashcards([])
    setFlipped({})
    try {
      const cards = await generateFlashcards(parseInt(documentId))
      setFlashcards(cards)
      setGenerated(true)
    } catch {
      setError('Failed to generate flashcards. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const toggleFlip = (index) => {
    setFlipped(prev => ({ ...prev, [index]: !prev[index] }))
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      {/* Title bar */}
      <div className="bg-white border-b border-gray-200 px-6 py-3">
        <div className="max-w-4xl mx-auto flex items-center gap-3">
          <button
            onClick={() => navigate('/dashboard')}
            className="text-gray-400 hover:text-gray-600 transition-colors text-sm cursor-pointer"
          >
            ← Back
          </button>
          <span className="text-gray-300">|</span>
          <span className="text-sm font-medium text-gray-700 truncate">
            Flashcards · {documentName || 'Loading...'}
          </span>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-6 py-8">

        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Flashcards</h1>
            <p className="text-gray-500 text-sm mt-1">
              Click a card to reveal the answer
            </p>
          </div>
          {(generated || error) && (
            <button
              onClick={loadFlashcards}
              disabled={loading}
              className="text-sm text-[#1D9E75] border border-[#1D9E75] px-4 py-2 rounded-lg hover:bg-[#1D9E75] hover:text-white transition-colors disabled:opacity-50 cursor-pointer"
            >
              {loading ? 'Generating...' : '↺ Regenerate'}
            </button>
          )}
        </div>

        {error && (
            <div className="bg-red-50 border border-red-200 text-red-600 text-sm rounded-lg px-4 py-3 mb-6">
                {error} <span className="text-red-400">(Click Regenerate to try again)</span>
            </div>
        )}

        {/* Initial state - not yet generated */}
        {!generated && !loading && (
          <div className="text-center py-20">
            <div className="text-5xl mb-4">🃏</div>
            <h3 className="text-lg font-medium text-gray-700 mb-2">
              Generate flashcards from this document
            </h3>
            <p className="text-gray-400 text-sm mb-6">
              AI will create 5 question & answer cards based on the document content
            </p>
            <button
              onClick={loadFlashcards}
              className="bg-[#1D9E75] hover:bg-[#178a63] text-white px-6 py-2.5 rounded-lg text-sm font-medium transition-colors cursor-pointer"
            >
              Generate Flashcards
            </button>
          </div>
        )}

        {/* Loading state */}
        {loading && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {[1, 2, 3, 4, 5].map(i => (
              <div key={i} className="bg-white rounded-xl border border-gray-200 p-6 h-40 animate-pulse">
                <div className="h-4 bg-gray-200 rounded w-3/4 mb-3"></div>
                <div className="h-3 bg-gray-200 rounded w-1/2"></div>
              </div>
            ))}
          </div>
        )}

        {/* Flashcards grid */}
        {!loading && flashcards.length > 0 && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {flashcards.map((card, index) => (
              <div
                key={index}
                onClick={() => toggleFlip(index)}
                className="cursor-pointer bg-white rounded-xl border border-gray-200 p-6 min-h-40 hover:shadow-md transition-all duration-200 flex flex-col justify-between"
              >
                <div>
                  <span className="text-xs font-medium text-[#1D9E75] uppercase tracking-wide">
                    {flipped[index] ? 'Answer' : 'Question'}
                  </span>
                  <p className="text-gray-800 text-sm mt-2 leading-relaxed">
                    {flipped[index] ? card.answer : card.question}
                  </p>
                </div>
                <p className="text-xs text-gray-400 mt-4">
                  {flipped[index] ? 'Click to see question' : 'Click to reveal answer'}
                </p>
              </div>
            ))}
          </div>
        )}

      </div>
    </div>
  )
}

export default FlashcardsPage