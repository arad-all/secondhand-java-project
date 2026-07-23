# Second-Hand Marketplace

A client-server desktop application for buying and selling second-hand items, built with Java 21, Spring Boot, JavaFX, and SQLite.

## Contributors

| Name | Email | Primary Role |
|------|-------|-------------|
| Arad Allahdini | arad.a1385@gmail.com | Backend Development |
| Amirmohammad Sherafat | amirmohammad.sherafat.86@gmail.com | Frontend Development |

### Arad Allahdini — Backend

Responsible for the entire backend architecture. Set up the project skeleton, JPA model layer, and repository layer for all entities. Implemented the security infrastructure including JWT authentication, password hashing, and role-based access control. Built the service and controller layers for all major features: advertisements (create, edit, search, approve, reject, image management), authentication, favorites, ratings, chat/messaging, and user management. Designed a custom exception hierarchy for consistent error responses and wrote unit tests for the core services (89 tests total). Also handled search/filter logic with hierarchical category support and various security fixes such as blocking access for compromised accounts.

### Amirmohammad Sherafat — Frontend

Responsible for the entire JavaFX frontend and client-server integration. Built the `ApiClient` and `SessionManager` to handle all HTTP communication with the backend. Implemented all 13 FXML view layouts and their corresponding controllers covering login, registration, advertisement browsing, creation/editing, favorites, purchase history, seller profiles, admin panel, and chat/messaging. Designed and applied a global CSS stylesheet with a cohesive visual system. In the early and late phases of the project, also contributed select backend components including initial controller stubs, the seller profile endpoint, and admin-note mapping in the advertisement response.

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 21 |
| Build Tool | Maven |
| Backend Framework | Spring Boot 3.3.4 |
| Security | Spring Security + JWT (jjwt 0.13.0) |
| ORM | Spring Data JPA + Hibernate 6.5 |
| Database | SQLite 3.46 (via hibernate-community-dialects) |
| Frontend | JavaFX 21 (FXML + CSS) |
| Testing | JUnit 5 + Mockito |
| Utilities | Lombok |

## Features

### Authentication & User Management
- User registration with unique username, email, and phone number constraints
- JWT-based authentication (24-hour token expiry)
- Password hashing via BCrypt
- Role-based access control (USER, ADMIN)
- Account blocking/unblocking by administrators

### Advertisement Management
- Create, edit, and delete advertisements (owner or admin)
- Advertisement subtypes: Vehicle, Electronics, Real Estate (with type-specific fields)
- Image upload (up to 8 images per ad, 5 MB each, 25 MB per request)
- Image gallery with display ordering
- Mark as sold (records the buyer)
- Status lifecycle: PENDING_REVIEW → ACTIVE → SOLD | REJECTED | DELETED
- Admin moderation: approve, reject (with reason note), or delete any ad
- Soft deletion (status change, not physical removal)

### Search & Browse
- Paginated browsing of active advertisements
- Multi-filter search: keyword (title/description), category, city, price range
- Hierarchical category filtering (parent category includes all subcategories)
- Sorting by creation date (newest first)

### Favorites
- Save/unsave advertisements to personal favorites list
- View all saved favorites

### Seller Ratings & Profiles
- Rate a seller after purchasing their item (1–5 stars, one rating per buyer per ad)
- View a seller's average rating, total count, and individual ratings
- Public seller profile lookup by username

### Messaging (Chat)
- Initiate a conversation with a seller about an advertisement
- Full message thread between buyer and seller
- Conversation inbox with last message preview
- Mark messages as read

### Purchase History
- View all advertisements purchased by the authenticated user

### Admin Panel
- View and moderate pending advertisements (approve/reject with reason)
- View all registered users
- Block/unblock user accounts
- Create categories and cities (hierarchical category tree)
- Filter users by account status

## Project Structure

```
secondhand-java-project/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── backend/
│   │   │   │   ├── SecondHandApplication.java          # Spring Boot entry point
│   │   │   │   ├── controller/                         # REST API endpoints
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   ├── AdvertisementController.java
│   │   │   │   │   ├── AdminController.java
│   │   │   │   │   ├── ChatController.java
│   │   │   │   │   ├── FavoriteController.java
│   │   │   │   │   ├── RatingController.java
│   │   │   │   │   ├── UserController.java
│   │   │   │   │   ├── CategoryController.java
│   │   │   │   │   ├── CityController.java
│   │   │   │   │   └── dto/                            # Request/Response DTOs
│   │   │   │   ├── service/                            # Business logic
│   │   │   │   │   ├── AuthService.java
│   │   │   │   │   ├── AdvertisementService.java
│   │   │   │   │   ├── ChatService.java
│   │   │   │   │   ├── FavoriteService.java
│   │   │   │   │   ├── RatingService.java
│   │   │   │   │   ├── UserService.java
│   │   │   │   │   ├── CategoryService.java
│   │   │   │   │   ├── CityService.java
│   │   │   │   │   └── FileStorageService.java
│   │   │   │   ├── repository/                         # Spring Data JPA repositories
│   │   │   │   ├── model/
│   │   │   │   │   ├── entity/                         # JPA entities
│   │   │   │   │   └── enums/                          # Role, AccountStatus, AdvertisementStatus
│   │   │   │   ├── mapper/                             # Entity-to-DTO mappers
│   │   │   │   ├── exception/                          # Custom exception hierarchy
│   │   │   │   └── security/                           # JWT filter, SecurityConfig, JwtService
│   │   │   └── frontend/
│   │   │       ├── Main.java                           # JavaFX entry point
│   │   │       ├── controller/                         # FXML controllers
│   │   │       └── service/
│   │   │           ├── ApiClient.java                  # HTTP client for backend
│   │   │           └── SessionManager.java             # Client-side auth state
│   │   └── resources/
│   │       ├── application.properties                  # Backend configuration
│   │       └── view/                                   # FXML layouts + CSS
│   │           ├── app.css
│   │           ├── login.fxml
│   │           ├── register.fxml
│   │           ├── advertisement-list.fxml
│   │           ├── advertisement-details.fxml
│   │           ├── create-advertisement.fxml
│   │           ├── edit-advertisement.fxml
│   │           ├── my-advertisements.fxml
│   │           ├── favorites.fxml
│   │           ├── purchase-history.fxml
│   │           ├── seller-profile.fxml
│   │           ├── admin-panel.fxml
│   │           ├── chat-list.fxml
│   │           └── chat-detail.fxml
│   └── test/
│       └── java/
│           └── backend/
│               ├── mapper/AdvertisementMapperTest.java
│               └── service/
│                   ├── AdvertisementServiceTest.java
│                   ├── RatingServiceTest.java
│                   ├── FileStorageServiceTest.java
│                   └── ChatServiceTest.java
```

## Prerequisites

- Java 21 (JDK)
- Maven 3.8+
- No external database server required (SQLite is embedded)

## Building & Running

### Start the Backend Server

```bash
mvn spring-boot:run
```

The REST API starts on `http://localhost:8080`. The SQLite database file (`secondhand.db`) is created automatically in the working directory on first run.

### Start the JavaFX Frontend

In a separate terminal:

```bash
mvn javafx:run
```

### Run Tests

```bash
mvn test
```

This runs all 89 unit tests across the mapper and service test suites.

## Configuration

All backend configuration is in `src/main/resources/application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | Backend API port |
| `spring.datasource.url` | `jdbc:sqlite:secondhand.db` | SQLite database path |
| `spring.servlet.multipart.max-file-size` | `5MB` | Max per-image upload size |
| `spring.servlet.multipart.max-request-size` | `25MB` | Max total upload size per request |
| `app.upload.dir` | `uploads` | Directory for uploaded images |
| `jwt.secret` | (dev placeholder) | JWT signing key (≥32 chars for HS256) |
| `jwt.expiration-ms` | `86400000` | Token lifetime (24 hours) |

## API Endpoints

### Public (no authentication required)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login, returns JWT |
| GET | `/api/advertisements` | List all active advertisements (paginated) |
| GET | `/api/advertisements/search` | Search with keyword/category/city/price filters |
| GET | `/api/advertisements/{id}` | View advertisement details |
| GET | `/api/advertisements/{id}/images/{filename}` | Serve advertisement image |
| GET | `/api/categories` | List top-level categories |
| GET | `/api/categories/{id}` | Get category by ID |
| GET | `/api/categories/{id}/children` | List subcategories |
| GET | `/api/cities` | List all cities |
| GET | `/api/cities/{id}` | Get city by ID |
| GET | `/api/users/by-username/{username}` | View seller profile |
| GET | `/api/users/{id}/ratings` | View seller ratings and average |

### Authenticated (requires `Authorization: Bearer <token>`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/advertisements/my` | List own advertisements |
| GET | `/api/advertisements/purchased` | List purchased advertisements |
| POST | `/api/advertisements` | Create new advertisement |
| PATCH | `/api/advertisements/{id}` | Edit advertisement |
| PATCH | `/api/advertisements/{id}/sold` | Mark as sold |
| DELETE | `/api/advertisements/{id}` | Delete advertisement |
| POST | `/api/advertisements/{id}/images` | Upload images (multipart) |
| DELETE | `/api/advertisements/{id}/images/{filename}` | Remove image |
| POST | `/api/advertisements/{id}/messages` | Message seller about ad |
| POST | `/api/advertisements/{id}/ratings` | Rate seller after purchase |
| GET | `/api/favorites` | List own favorites |
| POST | `/api/favorites/{id}` | Add to favorites |
| DELETE | `/api/favorites/{id}` | Remove from favorites |
| GET | `/api/conversations` | List own conversations |
| GET | `/api/conversations/{id}` | View conversation messages |
| POST | `/api/conversations/{id}/messages` | Reply in conversation |

### Admin Only (requires `ADMIN` role)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/advertisements/pending` | List ads awaiting review |
| PATCH | `/api/admin/advertisements/{id}/approve` | Approve advertisement |
| PATCH | `/api/admin/advertisements/{id}/reject` | Reject with reason |
| GET | `/api/admin/users` | List all users |
| PATCH | `/api/admin/users/{id}/block` | Block user account |
| PATCH | `/api/admin/users/{id}/unblock` | Unblock user account |
| POST | `/api/categories` | Create category (via `@PreAuthorize`, outside `/api/admin/` prefix) |
| POST | `/api/cities` | Create city (via `@PreAuthorize`, outside `/api/admin/` prefix) |

## Architecture

The application follows a layered architecture:

```
┌─────────────────────────────────────────────┐
│           JavaFX Frontend (FXML)            │
│   ApiClient → HTTP → Backend REST API       │
└─────────────────────────────────────────────┘
                     │
┌─────────────────────────────────────────────┐
│           Spring Boot REST Layer            │
│   Controllers → Service → Repository → JPA  │
│   ┌─────────────┐  ┌──────────────────────┐ │
│   │ JWT Filter   │  │ GlobalExceptionHandler│ │
│   └─────────────┘  └──────────────────────┘ │
└─────────────────────────────────────────────┘
                     │
┌─────────────────────────────────────────────┐
│           SQLite (embedded database)         │
└─────────────────────────────────────────────┘
```

**Design decisions:**

- Controllers contain no business logic — they only translate HTTP requests into service calls and resolve the caller's identity from the JWT principal.
- Entity-to-DTO conversion is centralized in dedicated `*Mapper` utility classes.
- Custom exception hierarchy (`ApiException` subclasses) provides consistent `{message, status}` error responses via `GlobalExceptionHandler`.
- Blocked users are immediately denied API access (their tokens are rejected at the JWT filter level, and their ads are filtered from public views).
- Advertisements from blocked users are hidden from public listings and search results.
- Path traversal protection in `FileStorageService` prevents directory escape attacks on image filenames.

## Database Setup

The project uses SQLite as an embedded database — no external database server is required.

### Automatic Creation

The SQLite database file (`secondhand.db`) is created automatically in the project root when the backend starts for the first time. Hibernate's `ddl-auto=update` strategy creates all tables from the JPA entity definitions. No manual database setup is needed.

### Sample Data

To populate the database with sample data (users, categories, cities, advertisements), run the provided SQL script:

```bash
sqlite3 secondhand.db < docs/sample-data.sql
```

The script is located at `docs/sample-data.sql` and contains INSERT statements for test data including:
- Sample users (admin and regular accounts)
- Hierarchical categories (Electronics, Vehicles, Real Estate, etc.)
- Sample cities
- Test advertisements in various statuses

### Test Accounts

| Role | Username | Password |
|------|----------|----------|
| Admin | `admin` | `admin_pass` |
| User | `arad` | `1234` |
| User | `amirmohammad` | `1234` |

### Database Schema

Key entities:

- **User** — username, email, phone, hashed password, role (USER/ADMIN), account status (ACTIVE/BLOCKED)
- **Advertisement** — title, description, price, status, category (hierarchical), city, owner, buyer, subtype fields (vehicle/electronics/real estate)
- **AdvertisementImage** — filename, display order, linked to advertisement
- **Category** — name, parent (self-referential for hierarchy)
- **City** — name, province
- **Conversation** — buyer, seller, linked to advertisement
- **ChatMessage** — sender, content, sent timestamp, read status
- **Favorite** — user + advertisement pair (unique constraint)
- **Rating** — buyer, seller, advertisement, score (1–5), timestamp (unique constraint on buyer+advertisement)
