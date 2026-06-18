# My Bookshelf

Web application for managing a personal book collection and sharing books between registered users. Owners can add books to their shelf, lend them to other users with a return deadline, and track transfers. Receivers can return borrowed books; administrators can view all registered users.

**Repository:** [https://github.com/DanailMagriotov/MyBookshelf.git](https://github.com/DanailMagriotov/MyBookshelf.git)

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 17 |
| Framework | Spring Boot 4.0.6 |
| Build tool | Maven |
| Database | MySQL |
| Data access | Spring Data JPA / Hibernate |
| Security | Spring Security (form login, BCrypt password hashing, session-based authentication) |
| Validation | Jakarta Bean Validation |
| Frontend | Spring MVC + Thymeleaf, CSS, JavaScript |
| Dev tools | Spring Boot DevTools, Lombok |

---

## Supported Features

### Authentication & access control
- User registration with server-side validation
- Session-based login (user data stored in HTTP session after successful authentication)
- Role-based access: `USER` and `ADMIN`
- Guests can access the landing, login, and register pages only
- Authenticated users can access all other application endpoints
- Admin-only access to the users management screen

### Book management
- Paginated bookshelf view (6 books per page)
- Add new books (title, author, description, category, price)
- Delete owned books (only when not in an active transfer)
- Counter showing how many books are visible on the user's shelf

### Book transfers
- Send a book to another user by username with a return deadline
- Edit return deadline for books currently lent out (new date cannot be earlier than the current deadline)
- Return a borrowed book (transfer is removed; ownership is restored to the sender)
- Validation for self-transfer, invalid recipient, and unavailable books

### User profile
- View and update profile (first name, last name, email, city, optional password change)
- Password change requires confirmation and must differ from the current password
- Delete account (disabled for `ADMIN` accounts)

### Administration
- Admin users list with pagination (username, name, email, role, city, owned book count)

### UI / UX
- Responsive layouts with themed background images per screen
- Flash messages for successful actions
- Inline field validation errors (server-side and client-side where applicable)
- Confirmation dialog before account deletion

---

## Functionalities

The following domain functionalities are triggered from the frontend, invoke backend `POST`/`DELETE` endpoints, and show a visible result to the user.

| Functionality | Entity | Operation | Endpoint |
|---------------|--------|-----------|----------|
| Add book | `Book` | Create | `POST /add-book` |
| Delete book | `Book` | Delete | `POST /my-bookshelf/delete/{bookId}` |
| Send book | `BookTransfer` | Create | `POST /send-book` |
| Return book | `BookTransfer` | Delete | `POST /my-bookshelf/return/{bookId}` |
| Update return deadline | `BookTransfer` | Update | `POST /my-bookshelf/edit/{bookId}` |

Additional user-related flows (registration, login, profile update) are supported but operate on the `User` entity.

### Domain model

- **User** — account with username, hashed password, email, role, region, and optional name
- **Book** — title, author, description, category, price, owner
- **BookTransfer** — sender, receiver, book, created/updated timestamps, return deadline

All entities use UUID primary keys. Relationships include `User` ↔ `Book` (owner) and `BookTransfer` links between users and books.

---

## Integrations

| System | Purpose |
|--------|---------|
| **MySQL** | Primary relational database for users, books, and transfers. Schema is managed via Hibernate `ddl-auto=update`. |
| **Spring Security** | Handles form-based authentication and protects routes; integrates with custom session storage (`UserSession` in HTTP session). |

The application is a **single standalone Spring Boot service**. It does not integrate with external REST APIs, message brokers, or additional microservices.

---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.9+
- MySQL 8+

### Configuration

Set database credentials in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/my_bookshelf_app?createDatabaseIfNotExist=true
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
```

### Run

```bash
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080) in a browser.

The first registered user automatically receives the `ADMIN` role; subsequent registrations receive the `USER` role.

---

## Web Pages

| Path | Description | Access |
|------|-------------|--------|
| `/` | Landing page | Guest |
| `/login` | Sign in | Guest |
| `/register` | Create account | Guest |
| `/home` | Main navigation hub | Authenticated |
| `/my-bookshelf` | Book list with actions | Authenticated |
| `/add-book` | Add a new book | Authenticated |
| `/send-book` | Lend a book to another user | Authenticated |
| `/my-bookshelf/edit/{bookId}` | Edit return deadline | Authenticated (owner) |
| `/my-profile` | Profile settings | Authenticated |
| `/users` | All users (admin table) | Admin |

---

## To be implemented

- Messaging system between users
