# TechBlogHub Makefile

.PHONY: help dev dev-down build up down logs clean test

help: ## Show this help message
	@echo 'Usage: make [target]'
	@echo ''
	@echo 'Targets:'
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  %-15s %s\n", $$1, $$2}' $(MAKEFILE_LIST)

dev: ## Start development environment
	@echo "Starting development environment..."
	cd docker && docker-compose -f docker-compose.dev.yml up -d
	@echo "Development services started:"
	@echo "  - PostgreSQL: localhost:5432"

dev-down: ## Stop development environment
	@echo "Stopping development environment..."
	cd docker && docker-compose -f docker-compose.dev.yml down

build: ## Build all services
	@echo "Building backend..."
	cd backend && ./gradlew build -x test
	@echo "Building frontend..."
	cd frontend && npm install && npm run build
	@echo "Building docker images..."
	docker-compose build

up: ## Start all services
	@echo "Starting all services..."
	docker-compose up -d

down: ## Stop all services
	@echo "Stopping all services..."
	docker-compose down

logs: ## Show logs
	docker-compose logs -f

clean: ## Clean up docker resources
	@echo "Cleaning up docker resources..."
	docker-compose down -v
	docker system prune -f

test: ## Run tests
	@echo "Running backend tests..."
	cd backend && ./gradlew test
	@echo "Running frontend tests..."
	cd frontend && npm test

backend-run: ## Run backend locally
	@echo "Starting backend locally..."
	cd backend && ./gradlew bootRun

frontend-run: ## Run frontend locally
	@echo "Starting frontend locally..."
	cd frontend && npm run dev

setup: ## Initial setup
	@echo "Setting up development environment..."
	make dev
	@echo "Waiting for services to start..."
	sleep 30
	@echo "Setup complete!"
	@echo ""
	@echo "You can now:"
	@echo "  1. Run backend: make backend-run"
	@echo "  2. Run frontend: make frontend-run"
	@echo "  3. Visit: http://localhost:3000"