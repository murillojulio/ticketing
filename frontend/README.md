# Ticketing Admin (Frontend)

Panel admin (SPA) para el backend **Reactive Ticketing Platform**.

## Requisitos

- Node.js instalado (recomendado: **Node 20+**).
- Backend corriendo en `http://localhost:8080`.

## Cómo correr en local

Desde `C:\dev\udemy\backend\ticketing\frontend`:

```bash
npm install
npm run dev
```

Luego abre `http://localhost:5173`.

## Docker (frontend + backend)

Desde la raíz del repo:

```bash
docker compose up --build
```

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`

## Conexión al backend (sin CORS en dev)

El frontend usa un proxy de Vite para reenviar llamadas a `/api/*` hacia el backend.

- Por defecto el proxy apunta a `http://localhost:8080`.
- Si necesitas cambiarlo, define:

```bash
VITE_API_PROXY_TARGET=http://localhost:8080
```

## Login

Usa `POST /api/auth/login` y guarda el JWT en `localStorage`.

## Rutas

- `/login`
- `/events`
- `/orders`

