# MobileHub

MobileHub is a smartphone storefront served by Spring Boot. The HTML, CSS, JavaScript, and homepage video are packaged as static web resources. Spring Boot exposes the product catalog, account authentication, product administration, inventory, shopping-cart, and order APIs. MySQL is the configured application database.

Checkout creates a database-backed demo order and updates inventory. It does not process payments, shipping, or fulfillment.

## Project objectives

- Browse smartphones and their product information from the existing catalog API.
- Search and filter catalog listings in the browser.
- Register and sign in using a stateless, cookie-based JWT flow.
- Keep guest carts in browser storage and signed-in carts in MySQL.
- Manage catalog items and inventory through allowlisted administrator access.
- Place demo orders, reserve stock transactionally, and review order history.
- Serve the existing storefront and API from one Spring Boot application.

## Features

- Homepage, smartphone catalog, product details, cart, about, contact, login, registration, and privacy pages.
- Product catalog with brand, model, price, specifications, images, and related display information.
- Search, brand, price, RAM, storage, and camera filters and sorting in the catalog.
- Database-backed customer accounts and signed-in cart items.
- Guest cart saved in browser `localStorage`.
- JWT authentication using an HttpOnly cookie and CSRF protection for state-changing requests.
- Admin-allowlisted product create, update, delete, and search.
- Per-product inventory; missing inventory rows are initialized to zero without changing existing stock.
- Transactional order placement, stock decrement, cart clearing, and account-scoped order history.
- Responsive HTML/CSS storefront and a homepage background video.
- Demo checkout without payment processing or shipping fulfillment.

## Technology stack

| Area | Technology |
|---|---|
| Frontend | HTML5, CSS3, browser JavaScript |
| Backend | Java 17, Spring Boot 3.5.7, Spring MVC |
| Persistence | Spring Data JPA, Hibernate |
| Application database | MySQL Connector/J, MySQL |
| Authentication | Spring Security, JWT (JJWT), BCrypt, cookie CSRF tokens |
| Build and tests | Maven, JUnit, Spring Boot Test, MockMvc |
| Test-only database | H2, activated by the `test` profile |

## Existing project layout

The project layout and locations are intentionally unchanged:

```text
Ecommerce/
├── .vscode/                 VS Code workspace settings
├── backups/                 Existing backup archive(s)
├── pages/                   Existing HTML pages and nested files
├── scripts/                 Browser JavaScript
├── src/
│   ├── main/java/           Spring Boot application, APIs, entities, and services
│   ├── main/resources/      Application configuration and static entry resource
│   └── test/                Automated tests and test-profile configuration
├── styles/                  Storefront stylesheets
├── target/                  Maven-generated build and test output
├── bg-video.mp4             Homepage video asset
├── pom.xml                  Maven project and resource configuration
├── .gitignore               Local/generated-file ignore rules
└── README.md                Project documentation
```

### Frontend

The current HTML pages are in `pages/`. They load the shared storefront stylesheet and the relevant browser scripts from `styles/` and `scripts/`. The Maven resource configuration packages the existing `pages/*.html`, `scripts/*.js`, `styles/*.css`, and `bg-video.mp4` paths under Spring Boot's static resource root. No frontend or backend files need to be moved to run the application.

- `scripts/storefront.js` retrieves products from `GET /api/products` and renders catalog, homepage, and detail content.
- `scripts/cart.js` handles the guest cart and the signed-in cart API.
- `scripts/auth.js` wires the login, registration, and signed-in navigation flows.
- `scripts/products.js` is an empty compatibility placeholder; it does not contain a second product dataset.
- `styles/storefront.css` is linked by the current pages.

Open the application through Spring Boot (for example, `http://localhost:8080/pages/Home-Page.html`). Opening an HTML file directly from disk does not provide the same-origin API endpoints.

### Backend

The Java sources are under `src/main/java/com/mobilehub/`:

- `catalog/`: products, inventory, public catalog, and admin product management.
- `account/`: account entity/repository and authentication API.
- `cart/`: cart-item entity, repository, service, and authenticated/guest-cart merge APIs.
- `orders/`: order and item entities, repositories, transactional checkout, and order history API.
- `config/`: Spring Security configuration, JWT service, and JWT request filter.
- `MobileHubApplication.java`: Spring Boot application entry point.

Spring Boot serves the static frontend and JSON endpoints from the same origin. `ProductCatalogService` contains 29 starter catalog definitions and inserts a definition only when its product ID is not already present. It does not update an existing product with the bundled starter values. The configured Hibernate setting is `spring.jpa.hibernate.ddl-auto=update`; use reviewed schema migrations for a production deployment instead of relying on automatic schema updates.

## MySQL database and entity relationships

The configured database name is `mobilehub`. The application configuration uses the MySQL JDBC driver and defaults the local JDBC URL to `jdbc:mysql://localhost:3306/mobilehub` (with connection options). Credentials are supplied through environment variables; no database password is documented here.

The following describes the current JPA entity mappings in source code. It is not a live `SHOW CREATE TABLE` dump: the running MySQL schema was not queried as part of this documentation update. Hibernate's MySQL dialect determines concrete SQL column types, and naming conventions map Java camel-case properties to underscore-separated column names.

### `products`

- **Primary key:** `id` (`String` product ID).
- Product properties: `brand`, `model`, `price`, `original_price`, `ram`, `storage`, `display`, `processor`, `camera`, `battery`, `rating`, `launch_year`, `image`, and `fallback_image`.
- Product data is consumed through the public product API.
- The response DTO also includes `images`, `variants`, `colors`, `description`, and `reviews`. The service constructs these response values; they are not all persisted product columns. In particular, the service builds a second display variant using its current pricing rule.

### `accounts`

- **Primary key:** `id` (UUID stored as a string and generated by the application).
- Account properties: `first_name`, `last_name`, `email`, `phone`, `password_hash`, `date_of_birth`, `state`, and `address`.
- `email` is required and unique. Passwords are stored as BCrypt hashes, not plaintext.
- Public registration creates customer accounts; it does not accept a role.

### `cart_items`

- **Primary key:** `id` (database-generated `Long`).
- Cart properties: `account_id`, `product_id`, `variant_ram`, `variant_storage`, `color`, `unit_price`, and `quantity`.
- `account_id` is a non-null foreign key to `accounts.id`.
- `product_id` is a non-null foreign key to `products.id`.
- Each cart row belongs to one account and references one product. An account or product can be referenced by multiple cart rows.

```text
accounts (1) ──── (many) cart_items (many) ──── (1) products
products (1) ──── (1) product_inventory
accounts (1) ──── (many) orders (1) ──── (many) order_items
products (1) ──── (many) order_items (optional historical reference)
```

### `product_inventory`

- **Primary key and foreign key:** `product_id` references `products.id`.
- `stock` is a nonnegative integer. The catalog startup process inserts rows only when missing, with stock set to zero; it does not reset existing inventory.

### `orders`

- **Primary key:** database-generated `id`.
- **Foreign key:** non-null `account_id` references `accounts.id`.
- Stores placement time, status, subtotal, delivery, and total.

### `order_items`

- **Primary key:** database-generated `id`.
- **Foreign keys:** non-null `order_id` references `orders.id`; nullable `product_id` references `products.id`.
- Stores a product-name snapshot, variant, color, unit price, and quantity. Deleting a product detaches the optional product reference while retaining the historical snapshot.

There is no SQL setup script that drops or recreates application tables. Hibernate currently updates the schema on application startup. The database creation command below only creates the database if it does not exist; keep a backup and use reviewed migrations for production schema changes.

## Prerequisites

- Windows with PowerShell.
- Java 17 or newer.
- Maven 3.9 or newer.
- MySQL 8 or compatible, running and reachable by the application.
- A MySQL user with permission to connect to `mobilehub` and the schema permissions needed by the configured Hibernate `update` behavior.

Check installed tools in PowerShell:

```powershell
java -version
mvn -version
Get-Service -Name 'MySQL*'
```

## MySQL setup

If the database has not already been created, create it once in MySQL Workbench or a MySQL client:

```sql
CREATE DATABASE IF NOT EXISTS mobilehub
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

This command does not drop, recreate, or modify existing database records. Use the existing database and account if they are already configured.

## Environment configuration

The application reads these environment variables:

| Variable | Required | Meaning |
|---|---:|---|
| `DB_URL` | Optional | MySQL JDBC URL. Defaults to the local `mobilehub` database URL in `application.properties`. |
| `DB_USERNAME` | Optional | MySQL username. The current local default is `root`. |
| `DB_PASSWORD` | Yes for a password-protected MySQL account | MySQL password. There is no password value in this README. |
| `JWT_SECRET` | Yes | Base64-encoded HMAC key; decoded key must be at least 32 bytes. Keep it private and stable while tokens are in use. |
| `JWT_EXPIRATION_SECONDS` | No | JWT lifetime in seconds; default is `3600`. |
| `JWT_COOKIE_SECURE` | No | Set `true` when serving over HTTPS; the configured default is `false` for local HTTP. |
| `ADMIN_EMAILS` | No | Comma-separated email allowlist for catalog administration. Defaults to no administrators. |
| `PORT` | No | Application port; default is `8080`. |

Set configuration for the current PowerShell process. The password prompt avoids placing the password directly in the command text; the application still receives it as an environment variable:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/mobilehub?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME = "root"

$securePassword = Read-Host "Enter your MySQL password" -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
}

$bytes = New-Object byte[] 32
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $rng.GetBytes($bytes)
    $env:JWT_SECRET = [Convert]::ToBase64String($bytes)
} finally {
    $rng.Dispose()
}
```

The generated JWT key is suitable for a new local development environment. For an existing environment, use its existing `JWT_SECRET` so previously issued tokens continue to verify until expiry. Do not commit credentials, local `.env` files, JWT keys, or passwords.

## Run the application (Windows PowerShell)

From the project root:

```powershell
mvn spring-boot:run
```

Open `http://localhost:8080/pages/Home-Page.html`. The root URL is also mapped to the packaged static welcome resource. Stop the foreground process with `Ctrl+C`.

The application connects to MySQL using `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. The test profile is not needed to run the application and must not be used for live MySQL verification.
For a shopper, “Exit” means closing the browser tab; to stop the local server, focus its PowerShell window and press `Ctrl+C`. The application does not expose a public shutdown endpoint.

## Maven commands

Run these commands from the directory containing `pom.xml`:

```powershell
# Run all automated tests
mvn clean test

# Build the executable Spring Boot JAR
mvn clean package

# Run the application
mvn spring-boot:run
```

The automated integration test explicitly activates the `test` profile and uses an in-memory H2 database with `create-drop`. A passing test run verifies the test configuration; it does not prove that the local MySQL service is reachable or that the live `mobilehub` schema contains a particular number of records.

## API reference

All endpoints are served from the same origin. Product endpoints are public. Cart endpoints and `GET /api/auth/me` require a valid `ACCESS_TOKEN` cookie. State-changing requests require CSRF protection.

### Products

| Method and path | Access | Behavior |
|---|---|---|
| `GET /api/products?q={term}` | Public | Returns the product catalog; optional `q` searches product ID, brand, and model. |
| `GET /api/products/{id}` | Public | Returns one product; returns `404` when the product ID does not exist. |

The product response includes `id`, `brand`, `model`, `price`, `originalPrice`, `stock`, `ram`, `storage`, `display`, `processor`, `camera`, `battery`, `rating`, `year`, `image`, `fallbackImage`, `images`, `variants`, `colors`, `description`, and `reviews`. Some of these fields (such as stock, variant choices, colors, description, and review examples) are assembled by the catalog service and are not columns in `products`.

### Authentication and CSRF

| Method and path | Access | Behavior |
|---|---|---|
| `GET /api/auth/csrf` | Public | Initializes/returns the CSRF token used by browser write requests. |
| `POST /api/auth/register` | Public, CSRF required | Validates registration data and creates a customer account. |
| `POST /api/auth/login` | Public, CSRF required | Authenticates credentials and sets the HttpOnly `ACCESS_TOKEN` cookie. |
| `GET /api/auth/me` | Authenticated | Returns the signed-in account's `id`, `firstName`, `lastName`, `email`, and allowlist-derived `admin` flag. |
| `POST /api/auth/logout` | Public, CSRF required | Clears the JWT cookie. |

Registration JSON fields: `firstName`, `lastName`, `email`, `phone`, `password`, `dateOfBirth` (`YYYY-MM-DD`), `state`, and `address`. The first six fields are required by the API validation rules; state and address may be omitted. Registration returns `201` on success and `409` if the email already exists.

Login JSON fields: `email` and `password`. Successful login returns the user summary and sends the JWT in an HttpOnly cookie. The browser does not read or store the JWT in JavaScript.

The frontend first requests `/api/auth/csrf`. Spring Security issues the `XSRF-TOKEN` cookie; browser write requests send its decoded value in the `X-XSRF-TOKEN` header. The JWT cookie is HttpOnly, SameSite=Lax, and stateless. No server-side login session is used. The default token lifetime is one hour. Clearing the cookie at logout does not revoke a token copied elsewhere; it remains valid until expiry.

### Shopping cart

| Method and path | Access | Behavior |
|---|---|---|
| `GET /api/cart` | Authenticated | Returns the signed-in account's items and cart summary. |
| `POST /api/cart/items` | Authenticated, CSRF required | Adds a product configuration. |
| `POST /api/cart/merge` | Authenticated, CSRF required | Merges validated guest-cart items into the signed-in cart. |
| `PUT /api/cart/items/{id}` | Authenticated, CSRF required | Sets quantity from 1 through 99. |
| `DELETE /api/cart/items/{id}` | Authenticated, CSRF required | Removes an item belonging to the signed-in account. |

Add-item JSON fields are `productId`, `variantRam`, `variantStorage`, and `color`. Merge JSON has an `items` array with the same product/configuration details and a `quantity`. The API validates products, configurations, stock, and quantities; price is determined by the server, not trusted from a browser-submitted price. Cart responses include `items`, `itemCount`, `subtotal`, `delivery`, `total`, and `deliveryNote`.

### Orders

| Method and path | Access | Behavior |
|---|---|---|
| `POST /api/orders` | Authenticated, CSRF required | Places an order from the signed-in user's cart. Returns `409` if stock is insufficient and `400` for an empty cart. |
| `GET /api/orders` | Authenticated | Returns only the signed-in account's order history and item snapshots. |

Checkout locks relevant inventory rows, revalidates aggregate quantities, writes the order and item snapshots, decreases stock, and clears the cart in one database transaction. Delivery is free for subtotals of ₹10,000 or more and ₹499 otherwise. This is a demo order flow; payment and fulfillment are not implemented.

### Product administration

| Method and path | Access | Behavior |
|---|---|---|
| `GET /api/admin/products?q={term}` | Admin | Lists products and stock; optional `q` searches ID, brand, and model. |
| `POST /api/admin/products` | Admin, CSRF required | Creates a product and its initial stock row. |
| `PUT /api/admin/products/{id}` | Admin, CSRF required | Updates product details and inventory. The body ID must match the path. |
| `DELETE /api/admin/products/{id}` | Admin, CSRF required | Deletes a product only when it is not present in a cart; order snapshots are retained. |

An admin is an existing account whose email matches `ADMIN_EMAILS` (case-insensitive). Set this environment variable before launching, for example `$env:ADMIN_EMAILS = "owner@example.com"`. Public registration does not grant administrator access.

## Login, registration, and cart workflows

1. Registration sends validated customer fields to `POST /api/auth/register` after obtaining the CSRF token.
2. The browser redirects to the login page after successful registration.
3. Login sends credentials to `POST /api/auth/login`; Spring Security verifies the BCrypt hash and issues the JWT cookie.
4. Subsequent requests authenticate from the cookie. The JWT filter loads the account from the database, and no server-side session is created.
5. The frontend uses `/api/auth/me` to detect the current account and show the sign-out action.
6. Signed-in cart operations persist in `cart_items`; guest cart operations remain in browser storage.
7. After a successful login, guest-cart contents are merged through `POST /api/cart/merge`; subsequent cart requests use MySQL.
8. Checkout creates a transactional order and updates stock. Signed-in users can review it on `Order-History.html`.
9. Allowlisted administrators can manage catalog and stock through `Admin-Products.html`.
10. Logout sends `POST /api/auth/logout` and clears the browser cookie.

## Catalog behavior

`GET /api/products` reads persisted `products` rows and returns their stock from `product_inventory` (zero if a row is absent). The browser uses that API response for homepage product sections, product cards, product details, and catalog filtering. The bundled 29-row starter catalog is inserted only for IDs that are not already present; it is not a frontend product list and does not overwrite existing matching product records. Newly initialized stock is zero; an allowlisted administrator can set inventory through the product management page.

The catalog service constructs some response-only demo information, including a second RAM/storage variant, color choices, a description, and sample reviews. Its current second-variant price rule adds ₹7,000 to the stored base price. The `originalPrice` property is populated by the entity constructor when a starter product is first inserted; existing database values remain in control of API responses. Confirm product details and pricing with the actual retailer before using this demo data commercially.

## Testing

Run:

```powershell
mvn clean test
```

The current tests cover the seeded catalog response and product count, static resource responses, registration, JWT login, stateless behavior, cart persistence, guest-cart merge, successful and insufficient-stock checkout behavior, order history, admin authorization, product CRUD, search, and validation using H2. `application-test.properties` sets `ddl-auto=create-drop`; it is test-only. These tests do not connect to MySQL.

To check the live database, start the normal application with the real `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` set. Confirm the application starts against MySQL, then query `SELECT COUNT(*) FROM products;` using an authorized MySQL client and request `GET http://localhost:8080/api/products`. Do not substitute H2 for this check, do not print credentials, and do not modify or reseed existing records just to perform verification.

## Troubleshooting

### Application cannot connect to MySQL

- Confirm the MySQL Windows service is running and listening on the host/port in `DB_URL`.
- Check the database name, username, password, and grants without printing the password.
- Confirm `DB_PASSWORD` is set in the same PowerShell process that runs Maven.
- Review the exception type/message in the application output; never put the password into logs or command history.

### Missing JWT secret or invalid key

- `JWT_SECRET` must be Base64 that decodes to at least 32 bytes.
- Keep an existing application's key stable; generating a new key invalidates existing cookies.
- Verify the variable is present without displaying its value.

### Port 8080 is already in use

Set another available port before starting the application, for example `$env:PORT = "8081"`, then browse to that port.

### `401 Unauthorized` for cart, orders, or `/api/auth/me`

- Sign in through the application and ensure the browser accepts the `ACCESS_TOKEN` cookie.
- Confirm the request is sent to the same host and port as the application.
- Cart, orders, and account endpoints require a valid, unexpired JWT.

### `403 Forbidden` for product administration

Confirm the signed-in account email is listed in `ADMIN_EMAILS` and restart the application after changing it. The allowlist is separate from public registration data.

### `403 Forbidden` on a write request

- Request `/api/auth/csrf` first.
- Send the current `XSRF-TOKEN` cookie value in the `X-XSRF-TOKEN` header for `POST`, `PUT`, and `DELETE`.

### Tests pass but MySQL is unavailable

The test profile uses H2 and does not verify MySQL. Check the normal application configuration and live MySQL connectivity separately; do not infer a live connection from `mvn clean test`.

## Potential cleanup opportunities (not performed)

- `styles/login.css` and `styles/home-stylings.css` appear unreferenced by the current HTML pages; verify no external/manual use before considering removal.
- `pages/oracleJdk-27/` is a nested directory outside the Maven resource includes. It may be unrelated or intentionally retained; inspect its purpose before any cleanup.
- `backups/thinkmart-before-style-sync-20261002-173818.zip` is a retained backup archive. Keep it unless its owner confirms it is no longer needed.
- `target/` contains generated Maven build/test output and can be recreated with Maven; it is now ignored by Git.
- No cleanup deletion, move, or rename was made.

## Future enhancements

Possible future work, not currently implemented: versioned database migrations, payment integration, shipping/fulfillment, transactional email/newsletter subscription, and production monitoring.

## Project information

- **Project:** MobileHub smartphone storefront
- **Repository/application name:** `com.mobilehub:mobilehub-storefront`
- **Author:** Not identified in the current project files; add the project owner's preferred name/contact here if desired.
- **License:** No root-project license file was identified during this documentation update. Review the licenses of bundled assets and nested files before distribution.
