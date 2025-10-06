# ECS Task 실행을 위한 IAM 역할

# ECS Task 실행 역할
resource "aws_iam_role" "ecs_task_execution_role" {
  name = "techbloghub-ecs-task-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Name = "TechBlogHub ECS Task Execution Role"
  }
}

# ECS Task 실행 역할에 정책 연결
resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_policy" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# CloudWatch 로그 그룹
resource "aws_cloudwatch_log_group" "backend" {
  name              = "/ecs/techbloghub/backend"
  retention_in_days = 7

  tags = {
    Name = "TechBlogHub Backend Logs"
  }
}

# Output
output "ecs_task_execution_role_arn" {
  description = "ARN of the ECS task execution role"
  value       = aws_iam_role.ecs_task_execution_role.arn
}