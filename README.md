# ArcanaERP Platform

Java 21 + Spring Boot + Spring Modulith rewrite of Arcana ERP.

## Tech Stack

- Java 21
- Spring Boot 3.4.2
- Spring Modulith 1.4.2
- Spring Data JPA + Hibernate
- H2 (dev/test)

## How To Run

Prerequisite: JDK 21 installed and available on `PATH`.

Build:

```bash
./gradlew build
```

Run tests:

```bash
./gradlew test
```

Start the app:

```bash
./gradlew bootRun
```

Default app URL:

- `http://localhost:8080`

## H2 Console (Local Dev)

- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:arcanaerp;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- Username: `sa`
- Password: *(blank)*

## Current HTTP Endpoints

Identity:

- `POST /api/identity/users`
- `GET /api/identity/users?page=&size=`

Products:

- `POST /api/products`
- `GET /api/products?page=&size=&active=`
- `PATCH /api/products/{sku}/active`
- `GET /api/products/{sku}/activation-history?page=&size=&tenantCode=&changedBy=&currentActive=&changedAtFrom=&changedAtTo=`

Orders:

- `POST /api/orders`
- `GET /api/orders?page=&size=`
- `PATCH /api/orders/{orderNumber}/status`

Inventory:

- `GET /api/inventory/{sku}`
- `POST /api/inventory/{sku}/adjustments`

Actuator:

- `GET /actuator/health`
- `GET /actuator/info`

Actuator exposure is intentionally limited to `health` and `info`.
