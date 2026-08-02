# My Bookshelf

Web application for managing a personal book collection, sharing books between registered users, and exchanging messages. Owners can add and edit books on their shelf, lend them with a return deadline, and track transfers. Administrators can manage registered users and roles.

**Repository:** [https://github.com/DanailMagriotov/MyBookshelf.git](https://github.com/DanailMagriotov/MyBookshelf.git)

---

## Architecture

The project consists of two Spring Boot applications:

| Application | Port | Database | Responsibility |
|-------------|------|----------|----------------|
| **my_bookshelf** (main app) | 8080 | `my_bookshelf_app` | Web UI, users, books, transfers, security |
| **message-service** | 8081 | `my_bookshelf_messages` | REST API for user messages |

The main app communicates with the message microservice through a **Feign client** (`MessageServiceClient`). Each service has its own MySQL database. Schema is managed with Hibernate `ddl-auto=update` (no Flyway/Liquibase migration scripts).

---

## Tech Stack

- **Language:** Java 17
- **Framework:** Spring Boot 4.0.6
- **Build tool:** Maven
- **Database:** MySQL 8+
- **Data access:** Spring Data JPA / Hibernate
- **Security:** Spring Security (custom login, bcrypt, HTTP session with `UserSession`)
- **Inter-service communication:** Spring Cloud OpenFeign
- **Validation:** Jakarta Bean Validation (DTOs, entities, service logic)
- **Caching & scheduling:** Spring Cache, `@Scheduled` jobs
- **Frontend:** Spring MVC + Thymeleaf, CSS, JavaScript
- **Testing:** JUnit 5, Mockito, MockMvc, H2 (test profile), JaCoCo (70% line coverage gate)
- **Dev tools:** Spring Boot DevTools, Lombok

---

## Supported Features

### Authentication & access control

- User registration and login with server-side validation
- Session-based authentication (`UserSession` stored in HTTP session)
- Roles: **USER**, **ADMIN**, **MASTER_ADMIN**
- The **first registered user** receives **MASTER_ADMIN**; all later registrations receive **USER**
- Guests: landing, login, register only
- Authenticated users: bookshelf, transfers, profile, messaging
- Admin screens (`/users`): visible to **ADMIN** and **MASTER_ADMIN**

### Book management

- Paginated bookshelf (4 books per page)
- Add owned books with title, author, description, category, and price
- **Edit book** metadata when the book is at home (`my book`) and has no active transfer
- Delete owned books (delete blocked while a transfer is active)
- Visible book counter on home and bookshelf

### Book transfers

- Send a book by recipient username with return deadline
- **Edit return deadline** for lent books (not earlier than current deadline)
- Return borrowed books
- Validation: self-transfer, unknown recipient, unavailable book
- **Scheduled overdue reminders** (system messages to sender and receiver)

### Messaging

- Send messages to other users by username
- Inbox (6 messages per page), sent list (8 per page), unread counter
- Opening an inbox message marks it as read; soft delete (hidden from inbox/sent; permanently removed when both sides delete)
- **System messages** from a dedicated system account (overdue return reminders, role change notifications)
- Graceful degradation when message-service is unavailable

### User profile

- View and update profile (name, email, city, optional password change)
- Delete account (disabled for **MASTER_ADMIN** with tooltip)

### Administration (`/users`)

- Paginated user list (system account is **hidden** from the list)
- **Delete** — only **USER** accounts
- **Make admin** — promote **USER** → **ADMIN** (any admin)
- **Make user** — demote **ADMIN** → **USER** (**MASTER_ADMIN** only)
- **MASTER_ADMIN** and system account cannot be deleted or have roles changed
- Confirmation dialogs for delete and role changes
- Inbox notification sent on successful role change

### Cross-cutting

- Validation on all layers: DTOs (`@Valid`), entities (Bean Validation + `EntityValidator` before persist), and service business rules
- Custom error page (`error.html`) and `@ControllerAdvice` exception handling
- Structured logging in service layer (books, transfers, users, messages)
- In-memory caching for bookshelf counts, sendable books, unread message counts

---

## Domain model

### Main application (`my_bookshelf_app`)

- **User** — username, password (bcrypt), email, role, region, optional name
- **Book** — title, author, description, category, price, owner
- **BookTransfer** — sender, receiver, book, timestamps, return deadline, overdue reminder flag

### Message service (`my_bookshelf_messages`)

- **Message** — senderId, receiverId, subject (`about`), content, sent/read timestamps, soft-delete flags

All entities use UUID primary keys.

---

## Integrations

- **MySQL (main)** — users, books, transfers
- **MySQL (messages)** — message storage for the microservice
- **message-service REST API** — consumed by the main app via Feign (`http://localhost:8081`)
- **Spring Security** — route protection and login flow

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.9+
- MySQL 8+

### Configuration

Set database credentials as environment variables (used by both applications):

```bash
# Windows (PowerShell)
$env:MY_USERNAME="your_mysql_user"
$env:MY_PASSWORD="your_mysql_password"

# Linux / macOS
export MY_USERNAME=your_mysql_user
export MY_PASSWORD=your_mysql_password
```

Default JDBC URLs (can be changed in `application.properties`):

- Main app: `jdbc:mysql://localhost:3306/my_bookshelf_app?createDatabaseIfNotExist=true`
- Message service: `jdbc:mysql://localhost:3306/my_bookshelf_messages?createDatabaseIfNotExist=true`

Main app Feign target (default): `message-service.url=http://localhost:8081`

Scheduling can be disabled in tests via `app.scheduling.enabled=false` (test profile).

### Run

Start **message-service first**, then the main application:

```bash
# Terminal 1 — message microservice (port 8081)
cd message-service
mvn spring-boot:run
```

```bash
# Terminal 2 — main application (port 8080)
cd ..
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080) in a browser.

Register the first account to obtain **MASTER_ADMIN**. Start message-service before using messaging features.

### Test

From the repository root (runs main app tests):

```bash
mvn verify
```

From `message-service/` (runs microservice tests):

```bash
mvn verify
```

JaCoCo reports: `target/site/jacoco/index.html` (each module). Minimum **70% line coverage** is enforced on core packages during `verify`.

---

## Web Pages

| Path | Description | Access |
|------|-------------|--------|
| `/` | Landing page | Guest |
| `/login`, `/register` | Authentication | Guest |
| `/home` | Navigation hub | Authenticated |
| `/my-bookshelf` | Book list and actions (edit, delete, return, edit deadline) | Authenticated |
| `/add-book` | Add a book | Authenticated |
| `/edit-book/{bookId}` | Edit book metadata | Authenticated (owner, no active transfer) |
| `/send-book` | Lend a book | Authenticated |
| `/my-bookshelf/edit/{bookId}` | Edit return deadline | Authenticated (owner, active transfer) |
| `/my-profile` | Profile settings | Authenticated |
| `/messages` | Messages hub | Authenticated |
| `/messages/new` | Compose message | Authenticated |
| `/messages/inbox` | Inbox | Authenticated |
| `/messages/inbox/{id}` | View inbox message | Authenticated |
| `/messages/sent` | Sent messages | Authenticated |
| `/users` | User administration | Admin |
