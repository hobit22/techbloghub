# 전체 Outputs

# Database
output "database_endpoint" {
  description = "RDS PostgreSQL endpoint"
  value       = aws_db_instance.techbloghub.endpoint
}

output "database_connection_url" {
  description = "Complete database connection URL"
  value       = "jdbc:postgresql://${aws_db_instance.techbloghub.endpoint}:${aws_db_instance.techbloghub.port}/${var.db_name}"
  sensitive   = true
}

# Load Balancer
output "application_url" {
  description = "Application URL"
  value       = "http://${aws_lb.techbloghub.dns_name}"
}

output "backend_api_url" {
  description = "Backend API URL"
  value       = "http://${aws_lb.techbloghub.dns_name}/api"
}

# ECR Repositories
output "ecr_backend_repository" {
  description = "Backend ECR repository URL"
  value       = aws_ecr_repository.backend.repository_url
}

output "ecr_frontend_repository" {
  description = "Frontend ECR repository URL"
  value       = aws_ecr_repository.frontend.repository_url
}

# ECS
output "ecs_cluster_arn" {
  description = "ECS Cluster ARN"
  value       = aws_ecs_cluster.techbloghub.arn
}

output "backend_service_name" {
  description = "Backend ECS service name"
  value       = aws_ecs_service.backend.name
}

output "frontend_service_name" {
  description = "Frontend ECS service name"
  value       = aws_ecs_service.frontend.name
}

# Deployment Information
output "deployment_info" {
  description = "Deployment summary"
  value = {
    application_url    = "http://${aws_lb.techbloghub.dns_name}"
    backend_health     = "http://${aws_lb.techbloghub.dns_name}/actuator/health"
    swagger_ui         = "http://${aws_lb.techbloghub.dns_name}/swagger-ui.html"
    ecs_cluster        = aws_ecs_cluster.techbloghub.name
    backend_service    = aws_ecs_service.backend.name
    frontend_service   = aws_ecs_service.frontend.name
  }
}