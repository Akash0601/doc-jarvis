# Doc Jarvis 🤖📄

> An AI-powered document intelligence platform — upload any document and chat with it using RAG (Retrieval-Augmented Generation).

**🌐 Live Demo: [doc-jarvis.vercel.app](https://doc-jarvis.vercel.app)**

![Doc Jarvis](https://img.shields.io/badge/Status-Live-brightgreen)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)
![React](https://img.shields.io/badge/React-18-blue)
![LangChain4j](https://img.shields.io/badge/LangChain4j-1.0.0--beta2-purple)

---

## ✨ Features

- 📤 **Document Upload** — Upload PDF, DOCX, or TXT files
- 🤖 **AI Chat** — Ask questions about your document, get grounded answers with citations
- 🃏 **Flashcards** — Auto-generate 5 Q&A flashcards from any document
- 🔍 **Semantic Search** — Vector-based similarity search using Qdrant
- 🔐 **JWT Authentication** — Secure signup/login with BCrypt password hashing
- 📱 **Mobile Responsive** — Works on all screen sizes

---

## 🏗️ Architecture

```
    React Frontend (Vercel)
                ↓ HTTPS
    Spring Boot Backend (Railway)
                ↓
┌───────────────────────────────────┐
│  Document Upload & Text Extraction│
│  (PDFBox + Apache POI + Tika)     │
└───────────────┬───────────────────┘
                ↓
┌───────────────────────────────────┐
│  Embedding Pipeline               │
│  AllMiniLmL6V2 → 384-dim vectors  │
│  Stored in Qdrant Cloud           │
└───────────────┬───────────────────┘
                ↓
┌───────────────────────────────────┐
│  RAG Pipeline                     │
│  Query → Embed → Search Qdrant    │
│  → Context → GPT-4o-mini → Answer │
└───────────────────────────────────┘
                ↓
PostgreSQL (Railway)
Chat history + Document metadata
```

---

## 🛠️ Tech Stack

| Layer           | Technology                             |
|-----------------|----------------------------------------|
| Frontend        | React 18, Vite, Tailwind CSS           |
| Backend         | Java 21, Spring Boot 3.5               |
| AI/LLM          | OpenAI GPT-4o-mini, LangChain4j        |
| Embeddings      | AllMiniLmL6V2 (384-dim, local ONNX)    |
| Vector DB       | Qdrant Cloud                           |
| Database        | PostgreSQL 17                          |
| Auth            | JWT (JJWT), BCrypt                     |
| Text Extraction | Apache PDFBox, Apache POI, Apache Tika |
| Deployment      | Railway (backend), Vercel (frontend)   |

---

## 🚀 Running Locally

### Prerequisites
- Java 21
- Maven
- Node.js 18+
- Docker (for Qdrant)
- PostgreSQL 17

### Backend Setup

```bash
# Clone the repo
git clone https://github.com/Akash0601/doc-jarvis.git
cd doc-jarvis

# Start Qdrant via Docker
docker run -p 6333:6333 -p 6334:6334 qdrant/qdrant:v1.13.6

# Set environment variables
set DB_PASSWORD=yourpassword
set SPRING_PROFILES_ACTIVE=local

# Run the backend
mvnw spring-boot:run
```

### Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

Frontend runs at `http://localhost:5173`
Backend runs at `http://localhost:8080`

---

## 🔑 Environment Variables

### Backend (Railway)
| Variable                     | Description               |
|------------------------------|---------------------------|
| `SPRING_PROFILES_ACTIVE`     | Set to `prod`             |
| `SPRING_DATASOURCE_URL`      | PostgreSQL connection URL |
| `SPRING_DATASOURCE_USERNAME` | DB username               |
| `SPRING_DATASOURCE_PASSWORD` | DB password               |
| `OPENAI_API_KEY`             | OpenAI API key            |
| `QDRANT_URL`                 | Qdrant Cloud host         |
| `QDRANT_API_KEY`             | Qdrant Cloud API key      |
| `JWT_SECRET`                 | JWT signing secret        |

### Frontend (Vercel)
| Variable       | Description                  |
|----------------|------------------------------|
| `VITE_API_URL` | Railway backend URL + `/api` |

---

## 📡 API Endpoints

### Auth (Public)
```
POST /api/auth/signup
POST /api/auth/login
```

### Documents (JWT Protected)
```
POST   /api/documents/upload
GET    /api/documents
DELETE /api/documents/{id}
POST   /api/documents/{id}/reembed
```

### Chat (JWT Protected)
```
POST /api/chat/ask
GET  /api/chat/history/{documentId}
```

### Flashcards (JWT Protected)
```
POST /api/flashcards/generate
```

---

## 👨‍💻 Author

**Akash Shaw**
- GitHub: [@Akash0601](https://github.com/Akash0601)
- Live Demo: [doc-jarvis.vercel.app](https://doc-jarvis.vercel.app)

---

## 📄 License

MIT License