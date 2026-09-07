# SmartInvoice Frontend

React + Tailwind CSS client for the SmartInvoice API. Ledger-styled UI for managing clients,
invoices, payments, recurring billing schedules, and the analytics dashboard.

## Stack

- React 19 + React Router 7
- Tailwind CSS v4 (via `@tailwindcss/vite`)
- Axios (JWT auth interceptor, auto-logout on 401)
- Vite

## Setup

```bash
cd frontend
npm install
npm run dev
```

The dev server runs on `http://localhost:5173` and proxies any `/api/*` request to
`http://localhost:8081` (see `vite.config.js`), so the backend must be running first. If your
backend runs on a different host/port, update the `server.proxy` target in `vite.config.js`.

## Build

```bash
npm run build
```

Outputs a production bundle to `dist/`.

## Structure

```text
src/
├── api/          One module per backend resource + shared axios client
├── components/   Shared UI primitives (Button, Modal, StatusBadge, StatCard, ...)
├── context/      AuthContext (session state, backed by localStorage)
├── layouts/      AppLayout (sidebar shell) and AuthLayout (login/register)
├── lib/          Formatting helpers (money, dates)
└── pages/        One file per route
```

## Pages

- `/login`, `/register` — auth
- `/` — analytics dashboard
- `/clients` — client list, create/edit
- `/invoices`, `/invoices/new`, `/invoices/:id` — invoice list, creation, and detail
  (items, status, PDF download, payments)
- `/recurring` — recurring billing schedules (create, pause/resume/cancel, generate now)
