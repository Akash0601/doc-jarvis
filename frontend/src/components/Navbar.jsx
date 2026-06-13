import { useState, useRef, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

function Navbar({ onUpload, uploading }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [dropdownOpen, setDropdownOpen] = useState(false)
  const dropdownRef = useRef(null)

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const getInitial = () => {
    if (user?.name) return user.name.charAt(0).toUpperCase()
    if (user?.email) return user.email.charAt(0).toUpperCase()
    return 'A'
  }

  return (
    <nav className="bg-white border-b border-gray-200 px-6 py-3">
      <div className="flex items-center justify-between">

        {/* Logo */}
        <Link to="/dashboard" className="text-xl font-bold text-[#1D9E75]">
          Doc Jarvis
        </Link>

        {/* Right side */}
        <div className="flex items-center gap-3">

          {/* Upload button */}
          {onUpload && (
            <label className={`cursor-pointer bg-[#1D9E75] hover:bg-[#178a63] text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors ${uploading ? 'opacity-60 cursor-not-allowed' : ''}`}>
              {uploading ? (
                <span className="flex items-center gap-2">
                  <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24" fill="none">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"/>
                  </svg>
                  Uploading...
                </span>
              ) : '+ Upload Document'}
              <input
                type="file"
                accept=".pdf,.docx,.txt"
                onChange={onUpload}
                disabled={uploading}
                className="hidden"
              />
            </label>
          )}

          {/* Avatar dropdown */}
          <div className="relative" ref={dropdownRef}>
            <button
              onClick={() => setDropdownOpen(!dropdownOpen)}
              className="w-9 h-9 rounded-full bg-[#1D9E75] text-white font-semibold text-sm flex items-center justify-center cursor-pointer hover:bg-[#178a63] transition-colors"
            >
              {getInitial()}
            </button>

            {/* Dropdown */}
            {dropdownOpen && (
              <div className="absolute right-0 mt-2 w-56 bg-white rounded-xl shadow-lg border border-gray-100 z-50 overflow-hidden">

                {/* Profile info */}
                <div className="px-4 py-4 border-b border-gray-100">
                  {user?.name && (
                    <p className="text-sm font-semibold text-gray-900 mb-0.5">{user.name}</p>
                  )}
                  <p className="text-xs text-gray-500 truncate">{user?.email}</p>
                </div>

                {/* Logout */}
                <div className="px-2 py-2">
                  <button
                    onClick={handleLogout}
                    className="w-full text-left px-3 py-2 text-sm text-red-500 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                  >
                    Log out
                  </button>
                </div>

              </div>
            )}
          </div>

        </div>
      </div>
    </nav>
  )
}

export default Navbar