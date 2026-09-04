# 💼 SmartInvoice

> A full-stack SaaS platform for creating professional invoices, tracking payments, automating recurring billing, and managing business finances.

## 🚀 Features

- 🔐 Secure Authentication & Authorization (JWT)
- 👥 Client Management
- 📄 Professional Invoice Generation
- 💳 Payment Tracking
- 🔁 Recurring Invoices
- 📧 Automated Payment Reminders
- 📊 Business Analytics Dashboard
- 📑 PDF Invoice Export
- 📖 REST API Documentation
- 🐳 Docker Support

## 🛠️ Tech Stack

### Backend
- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- JWT
- Maven

### Frontend
- React
- Tailwind CSS

### Database
- MariaDB / MySQL

## 📁 Project Structure

```text
SmartInvoice/
├── backend/
├── frontend/
└── docs/
```

## 📌 Current Status

- ✅ User registration (`POST /api/v1/auth/register`)
- ✅ Login with JWT (`POST /api/v1/auth/login`)
- ✅ Client management (CRUD, `/api/v1/clients`)
- ✅ Invoice creation with auto-calculated totals (CRUD, `/api/v1/invoices`)
- ✅ PDF export
- ✅ Payment tracking
- ✅ Recurring invoices & reminders (`/api/v1/recurring-invoices`)
- ⬜ Analytics dashboard
- ⬜ Frontend (not started)
