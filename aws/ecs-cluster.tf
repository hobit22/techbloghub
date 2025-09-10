# ECS 클러스터 및 네트워킹 설정

# ECS 클러스터
resource "aws_ecs_cluster" "techbloghub" {
  name = "techbloghub-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = {
    Name = "TechBlogHub ECS Cluster"
  }
}

# Application Load Balancer용 보안 그룹
resource "aws_security_group" "alb" {
  name_prefix = "techbloghub-alb-"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "TechBlogHub ALB Security Group"
  }
}

# ECS Tasks용 보안 그룹
resource "aws_security_group" "ecs_tasks" {
  name_prefix = "techbloghub-ecs-tasks-"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    from_port       = 3000
    to_port         = 3000
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  # HTTP/HTTPS 및 DNS 아웃바운드 (RDS는 별도 rule로)

  egress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # DNS 해석을 위한 아웃바운드 (RDS 엔드포인트 도메인 해석용)
  egress {
    from_port   = 53
    to_port     = 53
    protocol    = "udp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 53
    to_port     = 53
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "TechBlogHub ECS Tasks Security Group"
  }
}

# ECS Tasks에서 RDS로 접근 허용 (아웃바운드)
resource "aws_security_group_rule" "ecs_to_rds" {
  type                     = "egress"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.rds.id
  security_group_id        = aws_security_group.ecs_tasks.id
}

# RDS에서 ECS Tasks 접근 허용 (별도 rule로 순환 참조 방지)  
resource "aws_security_group_rule" "rds_from_ecs" {
  type                     = "ingress"
  from_port                = 5432
  to_port                  = 5432
  protocol                 = "tcp"
  source_security_group_id = aws_security_group.ecs_tasks.id
  security_group_id        = aws_security_group.rds.id
}

# Application Load Balancer
resource "aws_lb" "techbloghub" {
  name               = "techbloghub-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = data.aws_subnets.default.ids

  enable_deletion_protection = false

  tags = {
    Name = "TechBlogHub ALB"
  }
}

# Frontend Target Group (Port 3000)
resource "aws_lb_target_group" "frontend" {
  name        = "techbloghub-frontend-tg"
  port        = 3000
  protocol    = "HTTP"
  vpc_id      = data.aws_vpc.default.id
  target_type = "ip"

  health_check {
    enabled             = true
    healthy_threshold   = 2
    interval            = 30
    matcher             = "200"
    path                = "/health"
    port                = "traffic-port"
    protocol            = "HTTP"
    timeout             = 5
    unhealthy_threshold = 2
  }

  tags = {
    Name = "TechBlogHub Frontend Target Group"
  }
}

# Backend Target Group (Port 8080)
resource "aws_lb_target_group" "backend" {
  name        = "techbloghub-backend-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = data.aws_vpc.default.id
  target_type = "ip"

  health_check {
    enabled             = true
    healthy_threshold   = 2
    interval            = 30
    matcher             = "200"
    path                = "/actuator/health"
    port                = "traffic-port"
    protocol            = "HTTP"
    timeout             = 10
    unhealthy_threshold = 3
  }

  tags = {
    Name = "TechBlogHub Backend Target Group"
  }
}

# ALB Listener (HTTP) - HTTP to HTTPS 리디렉션
resource "aws_lb_listener" "http_redirect" {
  load_balancer_arn = aws_lb.techbloghub.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

# ALB Listener (HTTPS) - 메인 리스너
resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.techbloghub.arn
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS-1-2-2017-01"
  certificate_arn   = aws_acm_certificate_validation.techbloghub.certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.frontend.arn
  }
}

# Backend API용 Listener Rule (HTTPS)
resource "aws_lb_listener_rule" "backend_api" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 100

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }

  condition {
    path_pattern {
      values = ["/api/*", "/actuator/*", "/swagger-ui/*", "/v3/api-docs/*"]
    }
  }
}

# 도메인별 라우팅 (api.techbloghub.kr -> Backend)
resource "aws_lb_listener_rule" "api_subdomain" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 50

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }

  condition {
    host_header {
      values = ["api.techbloghub.kr"]
    }
  }
}

# Outputs
output "load_balancer_dns" {
  description = "DNS name of the load balancer"
  value       = aws_lb.techbloghub.dns_name
}

output "ecs_cluster_name" {
  description = "Name of the ECS cluster"
  value       = aws_ecs_cluster.techbloghub.name
}