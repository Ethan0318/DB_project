# Collab Edit (Student Project)

A lightweight, modern collaborative document editor for a classroom demo. This repo contains a Spring Boot backend and a Vue 3 frontend, built to be easy to read, run, and present.

## Features (Core)

- [x] Email registration + login (JWT)
- [x] Password reset (demo code returned in API response)
- [x] Profile management (nickname, email, phone, bio, avatar upload)
- [x] Roles (ADMIN / EDITOR / VIEWER) and document ACL
- [x] Document CRUD, soft delete
- [x] Rich text editor (TipTap)
- [x] Auto-save + local draft restore
- [x] Tags + search (LIKE query, not fulltext)
- [x] Real-time editing (WebSocket broadcast)
- [x] Presence list + cursor display
- [x] Comments with replies and @ mentions
- [x] Notifications (real-time + history)
- [x] Document chat
- [x] Operation logs (login, doc create/delete, ACL grant, comment)

## Tech Stack

Backend
- Java 17 + Spring Boot 3
- Spring Security + JWT
- MyBatis-Plus
- WebSocket
- MySQL 8

Frontend
- Vue 3 + Vite + TypeScript
- Element Plus
- TipTap (ProseMirror)

## Project Structure

```
server/   # Spring Boot backend
web/      # Vue 3 frontend
```

## Database Setup (Remote MySQL)

Required connection template:

- host: 1.92.79.87
- port: 3306
- database: collab_edit
- username: YOUR_DB_USER
- password: YOUR_DB_PASSWORD
- serverTimezone: Asia/Shanghai

Steps:

1. Create the database and tables:

```sql
CREATE DATABASE IF NOT EXISTS collab_edit DEFAULT CHARACTER SET utf8mb4;
```

2. Run schema and optional seed data:

- `server/src/main/resources/schema.sql`
- `server/src/main/resources/seed.sql`

## Backend Setup

1. Copy config:

PowerShell:

```
Copy-Item server/src/main/resources/application.yml.example server/src/main/resources/application.yml
```

macOS/Linux:

```
cp server/src/main/resources/application.yml.example server/src/main/resources/application.yml
```

2. Edit `server/src/main/resources/application.yml` and fill:

- `spring.datasource.username`
- `spring.datasource.password`
- `app.jwt.secret` (must be at least 32 chars)

3. Start the backend:

```
cd server
mvn spring-boot:run
```

## Frontend Setup

```
cd web
npm install
npm run dev
```

Open: http://localhost:5173

## Demo Accounts

On first backend startup, default users are created automatically:

- admin@example.com / 123456 (ADMIN)
- editor@example.com / 123456 (EDITOR)
- viewer@example.com / 123456 (VIEWER)

You can also register new accounts from the UI.

## Demo Flow (Two Users)

1. Open two browsers or incognito windows.
2. Login as `admin@example.com` and `editor@example.com`.
3. Admin creates a document and shares it with the editor (permission: EDIT).
4. Both open the document:
   - See presence list and colored cursors.
   - Type in both windows and watch real-time updates.
5. Add a comment with @ mention. Check notifications in the other user.
6. Open chat drawer and send messages.
7. Refresh a page to see local draft recovery prompt.

## Collaboration Design (No CRDT)

To keep the project approachable, the real-time sync uses WebSocket broadcasting of the full HTML content. The server does not merge edits. This means:

- Conflicts are resolved by last write wins.
- Large documents are not optimal.
- This is enough for a classroom demo but not for production.

Presence and cursor updates are sent via WebSocket and rendered as lightweight decorations in the editor.

## Password Reset (Simplified)

The reset code is generated server-side and returned in the API response for demo use. In production, you should send the code via email/SMS and never return it directly.

## Common Issues

- CORS error: make sure `app.cors.allowed-origins` matches the frontend URL.
- 401/403: check JWT token and document ACL.
- WebSocket not connecting: verify `/ws?token=...` and server is running.
- MySQL connection failed: check remote DB credentials and `serverTimezone`.

## API Notes

- Auth endpoints: `/api/auth/*`
- Docs endpoints: `/api/docs/*`
- WebSocket: `ws://localhost:8080/ws?token=JWT`

## Next Steps (Optional)

- Switch to Yjs CRDT server for true concurrent editing
- Add full-text search via MySQL FULLTEXT
- Add role management UI
