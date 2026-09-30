# Employee Management Application

Employee Management Application is a technical-test full-stack system for managing application users and employee records. It provides JWT authentication with HttpOnly cookies, backend-enforced role and permission authorization, user management, employee management, employee photo and CV storage in MinIO, password reset by email, and employment contract PDF generation.

Permissions are enforced by the Spring Boot backend. Frontend visibility rules are used for user experience only and are not the security boundary.

## Technology Stack

### Frontend

- Vue.js
- TypeScript
- Pinia
- Vue Router
- Vite

### Backend

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- JWT
- Apache PDFBox
- Apache POI

### Infrastructure

- PostgreSQL
- MinIO
- Docker

## Main Features

### Authentication

- Login using username or email.
- JWT-protected backend routes using HttpOnly cookies.
- The backend sets an `access_token` cookie and a `refresh_token` cookie.
- The frontend does not store JWTs in `localStorage`.
- Frontend API calls include cookies with `credentials: include`.
- Access tokens live for 15 minutes.
- Sessions have an absolute 7-day maximum lifetime.
- The refresh endpoint rotates cookies without extending the absolute session lifetime.
- Logout clears both authentication cookies.
- Stateless backend security.
- Frontend logout clears the local cached user session.

### Inactivity Handling

- After 3 minutes of inactivity, the frontend displays a session warning modal.
- The warning modal includes a 30-second countdown.
- `Stay connected` refreshes the current session if it is still valid.
- If the user does not respond before the countdown ends, the frontend logs out automatically.

### Password Reset

- Users can request a reset link from the forgot-password page.
- Reset links are sent by email through Gmail SMTP.
- Password reset tokens expire after 30 minutes.
- Reset tokens are one-time use.
- After a successful password reset, the user must log in again with the new password.

### Users

- Two initial Super Administrator accounts are created on backend startup when missing.
- Create, read, update, and delete users.
- Search and pagination.
- Activate and deactivate users.
- Grant and revoke `CREATE`, `READ`, `UPDATE`, and `DELETE` permissions for normal users.
- Permission management is restricted to Super Administrators.

### Employees

- Create, read, update, and delete employees.
- Search and pagination.
- Photo upload, view, replace, and delete.
- CV PDF upload, view, download, replace, and delete.
- Employment contract PDF generation.
- Import employees from Excel `.xlsx` files.
- Export all employees to Excel `.xlsx`.
- Export all employees to PDF `.pdf`.
- Download an Excel import template.

Employee import and export applies only to employees, not application users. Excel and PDF export include employee business data only: First Name, Last Name, Email, Phone, Job Title, Department, Hire Date, and Salary.

The Employees page shows one visible `Export` button. Clicking it opens a dropdown with `Export as Excel` and `Export as PDF`. The toolbar also includes `Import Excel` and `Download Template`.

### Files

- Employee photos and CVs are stored in MinIO.
- PostgreSQL stores object names/references only, not image or PDF bytes.
- When an employee is deleted, the associated photo and CV objects are also removed from MinIO.
- Photo and CV files are not imported or exported through employee Excel or PDF reports.
- Employee Excel/PDF exports do not expose MinIO object names or internal file-storage fields.

## Prerequisites

Install these tools locally:

- Java 21
- Node.js and npm
- PostgreSQL
- Docker Desktop

The frontend package declares support for Node.js `^22.18.0 || >=24.12.0`.

Default local ports:

| Service | Port |
| --- | --- |
| Frontend | `5173` |
| Backend | `8080` |
| PostgreSQL | `5432` |
| MinIO API | `9000` |
| MinIO Console | `9001` |

## PostgreSQL Setup

Create the application role and database in PostgreSQL:

```sql
CREATE USER employee_app WITH PASSWORD 'YOUR_DB_PASSWORD';
CREATE DATABASE employee_management OWNER employee_app;
```

The backend uses this JDBC URL:

```text
jdbc:postgresql://localhost:5432/employee_management
```

Set the `DB_PASSWORD` environment variable to the password used for the `employee_app` PostgreSQL role.

## MinIO Setup

The project includes a root MinIO Dockerfile:

```text
Dockerfile.minio
```

Build the local MinIO image from the project root:

```powershell
docker build -f Dockerfile.minio -t employee-minio .
```

Run MinIO with placeholder credentials:

```powershell
docker run --name employee-minio `
  -p 9000:9000 `
  -p 9001:9001 `
  -v employee-minio-data:/data `
  -e MINIO_ROOT_USER=YOUR_MINIO_USER `
  -e MINIO_ROOT_PASSWORD=YOUR_MINIO_PASSWORD `
  employee-minio server /data --console-address ":9001"
```

Open the MinIO Console:

```text
http://localhost:9001
```

Create a private bucket named:

```text
employee-files
```

Use the same MinIO credentials for the backend `MINIO_ACCESS_KEY` and `MINIO_SECRET_KEY` environment variables.

## Backend Environment Variables

Required environment variables:

- `DB_PASSWORD`
- `APP_JWT_SECRET`
- `MINIO_ACCESS_KEY`
- `MINIO_SECRET_KEY`
- `APP_ADMIN1_PASSWORD`
- `APP_ADMIN2_PASSWORD`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`

Optional environment variables with local defaults:

- `MINIO_ENDPOINT`, default: `http://localhost:9000`
- `MINIO_BUCKET`, default: `employee-files`
- `APP_FRONTEND_URL`, default: `http://localhost:5173`

Example PowerShell configuration:

```powershell
$env:DB_PASSWORD="YOUR_DB_PASSWORD"
$env:APP_JWT_SECRET="USE_A_LONG_RANDOM_SECRET_VALUE"
$env:MINIO_ACCESS_KEY="YOUR_MINIO_USER"
$env:MINIO_SECRET_KEY="YOUR_MINIO_PASSWORD"
$env:APP_ADMIN1_PASSWORD="YOUR_SUPER_ADMIN_1_PASSWORD"
$env:APP_ADMIN2_PASSWORD="YOUR_SUPER_ADMIN_2_PASSWORD"
$env:MAIL_USERNAME="YOUR_GMAIL_ADDRESS"
$env:MAIL_PASSWORD="YOUR_GMAIL_APP_PASSWORD"

# Optional because application.properties provides these defaults:
$env:MINIO_ENDPOINT="http://localhost:9000"
$env:MINIO_BUCKET="employee-files"
$env:APP_FRONTEND_URL="http://localhost:5173"
```

Do not commit real passwords, MinIO credentials, Gmail App Passwords, or JWT secrets.

## Run the Backend

From the backend directory:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

The backend starts at:

```text
http://localhost:8080
```

Troubleshooting: if Maven Wrapper has a `user.home` resolution issue in a specific Windows environment, verify the Java/Maven wrapper configuration or set `user.home` correctly for that shell.

## Run the Frontend

From the frontend directory:

```powershell
cd frontend
npm install
npm run dev
```

The frontend starts at:

```text
http://localhost:5173
```

For a production build verification:

```powershell
npm run build
```

## Initial Super Administrators

On backend startup, the application ensures two Super Administrator accounts exist:

| Name | Username | Email | Password Source |
| --- | --- | --- | --- |
| Super Administrator 1 | `superadmin1` | `superadmin1@employee.local` | `APP_ADMIN1_PASSWORD` |
| Super Administrator 2 | `superadmin2` | `superadmin2@employee.local` | `APP_ADMIN2_PASSWORD` |

Both accounts use `SUPER_ADMIN` user type and receive all permissions: `CREATE`, `READ`, `UPDATE`, and `DELETE`.

Passwords are read from environment variables and stored as BCrypt hashes. Plain-text passwords are not stored by the application.

## Application Usage

1. Start PostgreSQL.
2. Start MinIO and ensure the `employee-files` bucket exists.
3. Set the required backend environment variables.
4. Start the Spring Boot backend.
5. Start the Vue frontend.
6. Log in with one of the initialized Super Administrator accounts.
7. Manage users and permissions.
8. Manage employees, photos, CVs, and generated contracts.

## Security Notes

- Passwords are hashed with BCrypt.
- API authentication uses JWTs stored in HttpOnly cookies.
- Backend sessions are stateless.
- Role and permission checks are enforced on backend endpoints with Spring Security method-level authorization.
- Secrets are supplied through environment variables.
- Employee file uploads are validated by type and size.
- Unexpected technical server exceptions are converted to safe API error responses instead of exposing stack traces or internal details to users.

## File Validation

### Employee Photos

Allowed photo types:

- JPEG: `image/jpeg`
- PNG: `image/png`
- WebP: `image/webp`

Maximum photo size: `5 MB`.

### Employee CVs

Allowed CV type:

- PDF: `application/pdf`

Maximum CV size: `10 MB`.

The backend also checks the PDF signature for uploaded CV files.

## Employee Import and Export

Employee Excel import/export uses Apache POI. Employee PDF report export uses PDFBox. Existing employee contract PDF generation remains separate from the employee report export.

Excel import accepts `.xlsx` files using the template headers:

- First Name
- Last Name
- Email
- Phone
- Job Title
- Department
- Hire Date
- Salary

The import validates the complete file before inserting data. It is all-or-nothing: if any row contains an error, no employees are imported. Empty rows are ignored.

Import validation checks:

- required fields
- email format
- hire date format
- numeric salary values
- duplicate employee emails already existing in the database
- duplicate employee emails inside the same Excel file

When validation fails, the API returns row-numbered validation errors. After a successful import, the frontend refreshes the Employees list.

## Employment Contracts

Employee contracts are generated on demand from employee data as PDF files.

Contracts are not persisted in PostgreSQL or MinIO. Each generated PDF includes employee information, employment terms, and employer/employee signature areas.

## Project Structure

```text
.
|-- backend/
|   |-- .mvn/wrapper/
|   |-- src/main/java/com/employeemanagement/backend/
|   |   |-- config/
|   |   |-- controller/
|   |   |-- dto/
|   |   |-- entity/
|   |   |-- enums/
|   |   |-- exception/
|   |   |-- repository/
|   |   |-- security/
|   |   `-- service/
|   |-- src/main/resources/application.properties
|   |-- mvnw
|   |-- mvnw.cmd
|   `-- pom.xml
|-- frontend/
|   |-- src/
|   |   |-- layouts/
|   |   |-- router/
|   |   |-- services/
|   |   |-- stores/
|   |   |-- types/
|   |   `-- views/
|   |-- package.json
|   `-- vite.config.ts
|-- Dockerfile.minio
`-- README.md
```

## API Overview

This is a concise overview of the backend API groups.

### Authentication

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/auth/login` | Login with username or email and set HttpOnly authentication cookies. |
| `POST` | `/api/auth/refresh` | Refresh authentication cookies if the refresh token and absolute session are still valid. |
| `POST` | `/api/auth/logout` | Clear authentication cookies. |
| `POST` | `/api/auth/forgot-password` | Request a password reset email. |
| `POST` | `/api/auth/reset-password` | Reset a password using a valid one-time reset token. |

### Users

| Method | Endpoint | Description |
| --- | --- | --- |
| `GET` | `/api/users/me` | Get the authenticated user's profile. |
| `POST` | `/api/users` | Create a normal user. |
| `GET` | `/api/users` | List users with search and pagination. |
| `GET` | `/api/users/{id}` | Get one user. |
| `PUT` | `/api/users/{id}` | Update user profile fields and active status. |
| `PUT` | `/api/users/{id}/permissions` | Update a normal user's permissions. |
| `DELETE` | `/api/users/{id}` | Delete a normal user. |

### Employees

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/employees` | Create an employee. |
| `GET` | `/api/employees` | List employees with search and pagination. |
| `GET` | `/api/employees/export/excel` | Download all employees as an Excel file. Requires `READ` permission. |
| `GET` | `/api/employees/export/pdf` | Download all employees as a PDF report. Requires `READ` permission. |
| `GET` | `/api/employees/import-template` | Download the employee Excel import template. Requires `READ` permission. |
| `POST` | `/api/employees/import` | Import employees from a multipart/form-data Excel upload. Requires `CREATE` permission. |
| `GET` | `/api/employees/{id}` | Get one employee. |
| `PUT` | `/api/employees/{id}` | Update employee information. |
| `DELETE` | `/api/employees/{id}` | Delete an employee and clean up stored photo/CV objects. |
| `PUT` | `/api/employees/{id}/photo` | Upload or replace an employee photo. |
| `GET` | `/api/employees/{id}/photo` | View an employee photo. |
| `DELETE` | `/api/employees/{id}/photo` | Delete an employee photo. |
| `PUT` | `/api/employees/{id}/cv` | Upload or replace an employee CV PDF. |
| `GET` | `/api/employees/{id}/cv` | View an employee CV PDF inline. |
| `GET` | `/api/employees/{id}/cv/download` | Download an employee CV PDF. |
| `DELETE` | `/api/employees/{id}/cv` | Delete an employee CV. |
| `GET` | `/api/employees/{id}/contract` | Download a generated employee contract PDF. |

Backend permissions are enforced by Spring Security. Frontend button visibility is only a user-experience aid.
