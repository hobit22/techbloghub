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

# Load Balancer & Domain
output "application_url" {
  description = "Application URL (HTTPS)"
  value       = "https://techbloghub.kr"
}

output "backend_api_url" {
  description = "Backend API URL (HTTPS)"
  value       = "https://api.techbloghub.kr"
}

output "load_balancer_dns_name" {
  description = "ALB DNS name (for debugging)"
  value       = aws_lb.techbloghub.dns_name
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
    application_url    = "https://techbloghub.kr"
    backend_api_url    = "https://api.techbloghub.kr"
    backend_health     = "https://techbloghub.kr/actuator/health"
    swagger_ui         = "https://api.techbloghub.kr/swagger-ui.html"
    ecs_cluster        = aws_ecs_cluster.techbloghub.name
    backend_service    = aws_ecs_service.backend.name
    frontend_service   = aws_ecs_service.frontend.name
    ssl_certificate    = "Active"
    nameservers_info   = "가비아에서 네임서버를 변경하세요"
  }
}

# Domain & SSL Information
output "domain_setup_instructions" {
  description = "도메인 설정 안내"
  value = {
    step1 = "가비아 도메인 관리에서 네임서버를 다음 값으로 변경하세요:"
    nameservers = aws_route53_zone.techbloghub.name_servers
    step2 = "네임서버 변경 후 24-48시간 내에 SSL 인증서가 자동 발급됩니다"
    step3 = "완료 후 https://techbloghub.kr 접속 가능"
  }
}