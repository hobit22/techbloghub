# 전체 Outputs

# Load Balancer & Domain
output "application_url" {
  description = "Application URL (HTTPS)"
  value       = "https://teckbloghub.kr"
}

output "backend_api_url" {
  description = "Backend API URL (HTTPS)"
  value       = "https://api.teckbloghub.kr"
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

# ECS
output "ecs_cluster_arn" {
  description = "ECS Cluster ARN"
  value       = aws_ecs_cluster.techbloghub.arn
}

output "backend_service_name" {
  description = "Backend ECS service name"
  value       = aws_ecs_service.backend.name
}

# Deployment Information
output "deployment_info" {
  description = "Deployment summary"
  value = {
    frontend_url       = "https://teckbloghub.kr (Vercel)"
    backend_api_url    = "https://api.teckbloghub.kr"
    backend_health     = "https://api.teckbloghub.kr/actuator/health"
    swagger_ui         = "https://api.teckbloghub.kr/swagger-ui.html"
    ecs_cluster        = aws_ecs_cluster.techbloghub.name
    backend_service    = aws_ecs_service.backend.name
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
    step3 = "완료 후 https://teckbloghub.kr 접속 가능"
  }
}