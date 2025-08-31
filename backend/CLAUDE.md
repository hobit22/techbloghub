# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Commands

### Building and Running
- `./gradlew build` - Build the project
- `./gradlew bootRun` - Run the application locally
- `./gradlew test` - Run tests with JUnit Platform
- `./gradlew clean` - Clean build artifacts

### Database and Testing
- Application runs with H2 in-memory database by default (local profile)
- H2 Console available at `/h2-console` (username: `sa`, password: `password`)
- Use `docker` profile for PostgreSQL in containerized environment
- Use `prod` profile for production PostgreSQL setup

### API Documentation
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API Docs: `http://localhost:8080/v3/api-docs`

## Architecture Overview

This is a Spring Boot application implementing **Hexagonal Architecture** (Clean Architecture) with the following structure:

### Core Layers
- **Domain Layer** (`domain/`): Pure business logic, independent of frameworks
  - `model/`: Domain entities (Post, Blog, Category, Tag)
  - `port/in/`: Input ports (Use Cases interfaces)
  - `port/out/`: Output ports (Repository interfaces)
  - `service/`: Business logic implementation

- **Application Layer** (`application/`): External interface adapters
  - `controller/`: REST controllers
  - `dto/`: Response DTOs for API serialization

- **Persistence Layer** (`persistance/`): Data access and infrastructure
  - `adapter/`: Repository implementations (adapters)
  - `entity/`: JPA entities with database mapping
  - `repository/`: JPA repositories with QueryDSL support
  - `infrastructure/`: External service implementations (RSS crawler)

### Key Technologies
- **Spring Boot 3.2.0** with Java 21
- **Spring Data JPA** with QueryDSL for complex queries
- **Spring Security** with basic auth (admin/admin123)
- **Spring WebFlux** for HTTP client operations
- **Rome Tools** for RSS feed parsing
- **Lombok** for boilerplate code reduction
- **Testcontainers** for integration testing

### Domain Models and Business Rules
- **Post**: Core entity with validation rules (`isValid()`, `isRecent()`, `hasTag()`, `hasCategory()`)
- **Blog**: RSS feed source configuration
- **Tag/Category**: Automatic extraction from post content
- Posts are automatically deduplicated by `originalUrl`

### RSS Crawling System
- Scheduled crawling configured via `crawler.schedule` properties
- Hourly and daily cron jobs for feed processing
- Automatic tag and category extraction from post content
- Located in `batch/CrawlerScheduler.java` and `persistance/infrastructure/`

### Important Patterns
- Use **Builder pattern** for domain model construction
- Domain models are immutable with final fields
- Repository ports return domain objects, not JPA entities
- Controllers map domain objects to DTOs using `ResponseDto.from()` methods
- Business logic validation happens in domain services, not controllers

### Configuration Profiles
- `local`: H2 database, debug logging, scheduler enabled
- `docker`: PostgreSQL with environment variables
- `prod`: Production PostgreSQL, minimal logging, validate DDL mode