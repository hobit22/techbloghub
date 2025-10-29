# ECS Task Definitions

# Backend Task Definition
resource "aws_ecs_task_definition" "backend" {
  family                   = "techbloghub-backend"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn

  container_definitions = jsonencode([
    {
      name  = "backend"
      image = "${aws_ecr_repository.backend.repository_url}:latest"
      
      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        }
      ]

      environment = [
        {
          name  = "SPRING_PROFILES_ACTIVE"
          value = "prod"
        },
        {
          name  = "DATABASE_URL"
          value = var.database_url
        },
        {
          name  = "DATABASE_USERNAME"
          value = var.database_username
        },
        {
          name  = "DATABASE_PASSWORD"
          value = var.database_password
        },
        {
          name  = "OPENAI_API_KEY"
          value = var.openai_api_key
        },
        {
          name = "APP_ADMIN_USERNAME"
          value = var.app_admin_username
        },
        {
          name = "APP_ADMIN_PASSWORD"
          value = var.app_admin_password
        },
        {
          name  = "RSS_PROXY_URL"
          value = var.rss_proxy_url
        }, 
        {
          name = "DISCORD_WEBHOOK_URL"
          value = var.discord_webhook_url
        },
        {
          name  = "DISCORD_WEBHOOK_ENABLED"
          value = "true"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group         = aws_cloudwatch_log_group.backend.name
          awslogs-region        = var.aws_region
          awslogs-stream-prefix = "ecs"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
        interval    = 30
        timeout     = 10
        retries     = 3
        startPeriod = 120
      }

      essential = true
    }
  ])

  tags = {
    Name = "TechBlogHub Backend Task"
  }
}