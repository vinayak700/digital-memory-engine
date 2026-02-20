# Production Readiness Implementation Plan

This plan outlines the steps to remove WebSockets, clean up redundant code, and prepare the `digital-memory` system for production deployment on Fly.io.

## Proposed Changes

### [Backend] digital-memory-engine

#### [DELETE] WebSocket Configuration
- Remove any `WebSocketConfig.java` or similar classes.
- Remove messaging/stomp controllers if they exist.

#### [MODIFY] [application.properties](file:///Users/vinayakg08/Documents/digital-memory-engine/src/main/resources/application.properties)
- Remove WebSocket-related logging (`logging.level.org.springframework.web.socket`, `logging.level.org.springframework.messaging`).
- Externalize all secrets to environment variables.
- Set production-level logging and performance settings.

#### [ORCHESTRATION] [NEW] Dockerfile
- Create a multi-stage Dockerfile for the Spring Boot application.

#### [ORCHESTRATION] [NEW] fly.toml
- Configure Fly.io deployment settings.

### [Frontend] digital-memory-ui

#### [DELETE] WebSocket Hooks and Components
- Delete `src/hooks/useRealTime.ts`.
- Remove `sockjs-client` and `@stomp/stompjs` from `package.json`.

#### [MODIFY] [App.tsx] or Main Components
- Remove any real-time listeners or providers.

## Secrets Management
I will provide a `.env.production` template containing:
- `GEMINI_API_KEY`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
- `KAFKA_USER`, `KAFKA_PASSWORD`
- `SEMANTIC_CACHE_SIMILARITY_THRESHOLD`

## Verification Plan

### Automated Tests
- Run `./mvnw clean package` to- [x] Creating Dockerfile
- [x] Configuring Render Deployment & Keep-Alive
- [x] Final verification of Render setup
- [x] Documentation of Render deployment steps
rategy

### [Backend] Render (Web Service)
- **CORS Configuration**: Ensure `SecurityConfig.java` allows the Render frontend URL.
- **Environment**: Use Render's secret management for all DB/API keys.
- **Keep-Alive**: Implement a `/api/v1/health` or use Spring Actuator.

### [Frontend] Render (Static Site) or Vercel
- **API URL**: Set `VITE_API_BASE_URL` to the Render backend URL.
- **Keep-Alive Scheduler**: Add a `useKeepAlive` hook in the React frontend to ping the backend every 5 minutes (as a fallback).

## Updated Proposed Changes

### [Backend] [NEW] [render.yaml](file:///Users/vinayakg08/Documents/digital-memory-engine/render.yaml)
- Define a blueprint for the Web Service and PostgreSQL (if managed).

### [Frontend] [NEW] [src/hooks/useKeepAlive.ts](file:///Users/vinayakg08/Documents/digital-memory-ui/src/hooks/useKeepAlive.ts)
- Implement an effect that pings the health endpoint.

## Verification Plan
- Build and run production versions locally if possible, or verify configuration values.
