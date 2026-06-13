/* eslint-disable react-hooks/exhaustive-deps */
/* eslint-disable react-hooks/set-state-in-effect */
import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import { getDocuments, uploadDocument, deleteDocument } from '../services/documentService'

function DashboardPage() {
  const [documents, setDocuments] = useState([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const fetchDocuments = useCallback(async () => {
    try {
      const response = await getDocuments()
      setDocuments(response.data)
    } catch {
      setError('Failed to load documents')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchDocuments()
  }, [fetchDocuments])

  const handleUpload = async (e) => {
    const file = e.target.files[0]
    if (!file) return

    const allowedTypes = ['application/pdf',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'text/plain'
    ]
    if (!allowedTypes.includes(file.type)) {
      setError('Only PDF, DOCX, and TXT files are supported')
      return
    }

    setUploading(true)
    setError('')
    try {
      await uploadDocument(file)
      await fetchDocuments()
    } catch (err) {
      setError(err.response?.data?.message || 'Upload failed. Please try again.')
    } finally {
      setUploading(false)
      e.target.value = ''
    }
  }

  const handleDelete = async (documentId) => {
    if (!window.confirm('Are you sure you want to delete this document?')) return
    try {
      await deleteDocument(documentId)
      await fetchDocuments()
    } catch {
      setError('Failed to delete document. Please try again.')
    }
  }

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric', month: 'long', day: 'numeric'
    })
  }

  const formatSize = (bytes) => {
    if (bytes < 1024) return bytes + ' B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar />

      <div className="max-w-6xl mx-auto px-6 py-8">

        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-2xl font-bold text-gray-900">My Documents</h1>
            <p className="text-gray-500 text-sm mt-1">
              Upload documents and chat with them using AI
            </p>
          </div>

          {/* Upload button */}
          <label className={`cursor-pointer bg-[#1D9E75] hover:bg-[#178a63] text-white text-sm font-medium px-4 py-2.5 rounded-lg transition-colors ${uploading ? 'opacity-60 cursor-not-allowed' : ''}`}>
            {uploading ? 'Uploading...' : '+ Upload Document'}
            <input
              type="file"
              accept=".pdf,.docx,.txt"
              onChange={handleUpload}
              disabled={uploading}
              className="hidden"
            />
          </label>
        </div>

        {/* Error message */}
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-600 text-sm rounded-lg px-4 py-3 mb-6">
            {error}
          </div>
        )}

        {/* Loading skeleton */}
        {loading && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="bg-white rounded-xl border border-gray-200 p-6 animate-pulse">
                <div className="h-4 bg-gray-200 rounded w-3/4 mb-3"></div>
                <div className="h-3 bg-gray-200 rounded w-1/2 mb-6"></div>
                <div className="h-8 bg-gray-200 rounded w-full"></div>
              </div>
            ))}
          </div>
        )}

        {/* Empty state */}
        {!loading && documents.length === 0 && (
          <div className="text-center py-20">
            <div className="text-5xl mb-4">📄</div>
            <h3 className="text-lg font-medium text-gray-700 mb-2">
              No documents yet
            </h3>
            <p className="text-gray-400 text-sm">
              Upload a PDF, DOCX, or TXT file to get started
            </p>
          </div>
        )}

        {/* Document cards grid */}
        {!loading && documents.length > 0 && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {documents.map((doc) => (
              <div
                key={doc.id}
                className="bg-white rounded-xl border border-gray-200 p-6 hover:shadow-md transition-shadow"
              >
                {/* File icon + name */}
                <div className="flex items-start gap-3 mb-4">
                  <span className="text-2xl">
                    {doc.fileType === 'application/pdf' ? '📕' : '📄'}
                  </span>
                  <div className="flex-1 min-w-0">
                    <h3 className="font-medium text-gray-900 text-sm truncate">
                      {doc.fileName}
                    </h3>
                    <p className="text-xs text-gray-400 mt-0.5">
                      {formatDate(doc.uploadedAt)} · {formatSize(doc.fileSize)}
                    </p>
                  </div>
                </div>

                {/* Action buttons */}
                <div className="flex gap-2 flex-wrap">
                  <button
                    onClick={() => navigate(`/chat/${doc.id}`)}
                    className="flex-1 bg-[#1D9E75] hover:bg-[#178a63] text-white text-sm font-medium py-2 rounded-lg transition-colors"
                  >
                    Chat with Doc
                  </button>
                  <button
                    onClick={() => navigate(`/flashcards/${doc.id}`)}
                    className="flex-1 border border-[#1D9E75] text-[#1D9E75] hover:bg-[#1D9E75] hover:text-white text-sm font-medium py-2 rounded-lg transition-colors"
                  >
                    Flashcards
                  </button>
                  <button
                    onClick={() => handleDelete(doc.id)}
                    className="w-full border border-red-200 text-red-400 hover:bg-red-50 text-sm font-medium py-2 rounded-lg transition-colors"
                  >
                    Delete
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}

      </div>
    </div>
  )
}

export default DashboardPage