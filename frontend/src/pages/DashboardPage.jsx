/* eslint-disable react-hooks/exhaustive-deps */
/* eslint-disable react-hooks/set-state-in-effect */
import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import Navbar from '../components/Navbar'
import Toast from '../components/Toast'
import useToast from '../hooks/useToast'
import { getDocuments, uploadDocument, deleteDocument } from '../services/documentService'

function DashboardPage() {
  const [documents, setDocuments] = useState([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [deletingId, setDeletingId] = useState(null)
  const navigate = useNavigate()
  const { toast, showToast, hideToast } = useToast()
  const [searchQuery, setSearchQuery] = useState('')

  const filteredDocuments = documents.filter(doc =>
    doc.fileName.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const fetchDocuments = useCallback(async () => {
    try {
      const response = await getDocuments()
      setDocuments(response.data)
    } catch {
      showToast('Failed to load documents', 'error')
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

    const allowedTypes = [
      'application/pdf',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'text/plain'
    ]
    if (!allowedTypes.includes(file.type)) {
      showToast('Only PDF, DOCX, and TXT files are supported', 'error')
      return
    }

    // ── Duplicate detection ──────────────────────────────
    const duplicate = documents.find(
      doc => doc.fileName.toLowerCase() === file.name.toLowerCase()
    )
    if (duplicate) {
      showToast(
        'This document already exists! You can find it using the search bar.',
        'error'
      )
      e.target.value = ''
      return
    }

    setUploading(true)
    try {
      await uploadDocument(file)
      await fetchDocuments()
      showToast('Document uploaded successfully!', 'success')
    } catch (err) {
      showToast(
        err.response?.data?.message || 
        err.response?.data?.error || 
        'Upload failed. Please try again.', 
        'error'
      )
    } finally {
      setUploading(false)
      e.target.value = ''
    }
  }

  const handleDelete = async (documentId) => {
    if (!window.confirm('Are you sure you want to delete this document?')) return
    setDeletingId(documentId)
    try {
      await deleteDocument(documentId)
      await fetchDocuments()
      showToast('Document deleted successfully', 'success')
    } catch {
      showToast('Failed to delete document. Please try again.', 'error')
    } finally {
      setDeletingId(null)
    }
  }

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric', month: 'short', day: 'numeric'
    })
  }

  const formatSize = (bytes) => {
    if (!bytes) return 'Unknown size'
    if (bytes < 1024) return bytes + ' B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar onUpload={handleUpload} uploading={uploading} />

      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-6 sm:py-8">

        {/* Header */}
        <div className="mb-8">
          <h1 className="text-xl sm:text-2xl font-bold text-gray-900">My Documents</h1>
          <p className="text-gray-500 text-sm mt-1">
            Upload documents and chat with them using AI
          </p>
        </div>

        {/* Search bar */}
        {documents.length > 0 && (
          <div className="relative mb-6">
            <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm">🔍</span>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search your documents here..."
              className="w-full border border-gray-200 rounded-xl pl-9 pr-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-[#1D9E75] focus:border-transparent"
            />
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 text-lg"
              >
                ×
              </button>
            )}
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
        {!loading && filteredDocuments.length === 0 && !searchQuery && (
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

        {/* No search results */}
        {!loading && searchQuery && filteredDocuments.length === 0 && (
          <div className="text-center py-20">
            <div className="text-5xl mb-4">🔍</div>
            <h3 className="text-lg font-medium text-gray-700 mb-2">
              No documents found
            </h3>
            <p className="text-gray-400 text-sm">
              No documents match "{searchQuery}"
            </p>
          </div>
        )}

        {/* Document cards grid */}
        {!loading && documents.length > 0 && (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {filteredDocuments.map((doc) => (
              <div
                key={doc.id}
                className="bg-white rounded-xl border border-gray-200 p-5 hover:shadow-md transition-shadow"
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
                <div className="flex gap-2 mb-2">
                  <button
                    onClick={() => navigate(`/chat/${doc.id}`)}
                    className="flex-1 bg-[#1D9E75] hover:bg-[#178a63] text-white text-sm font-medium py-2 rounded-lg transition-colors cursor-pointer"
                  >
                    Chat
                  </button>
                  <button
                    onClick={() => navigate(`/flashcards/${doc.id}`)}
                    className="flex-1 border border-[#1D9E75] text-[#1D9E75] hover:bg-[#1D9E75] hover:text-white text-sm font-medium py-2 rounded-lg transition-colors cursor-pointer"
                  >
                    Flashcards
                  </button>
                </div>
                <button
                  onClick={() => handleDelete(doc.id)}
                  disabled={deletingId === doc.id}
                  className="w-full border border-red-200 text-red-400 hover:bg-red-50 text-sm font-medium py-2 rounded-lg transition-colors disabled:opacity-50 cursor-pointer"
                >
                  {deletingId === doc.id ? 'Deleting...' : 'Delete'}
                </button>
              </div>
            ))}
          </div>
        )}

      </div>

      {/* Toast notification */}
      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={hideToast}
        />
      )}
    </div>
  )
}

export default DashboardPage