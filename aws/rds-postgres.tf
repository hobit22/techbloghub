# AWS RDS PostgreSQL 인스턴스 생성
# terraform init && terraform plan && terraform apply 로 실행

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# AWS Provider 설정
provider "aws" {
  region = var.aws_region
}

# Variables
variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"  # Seoul region
}

variable "db_name" {
  description = "Database name"
  type        = string
  default     = "techbloghub"
}

variable "db_username" {
  description = "Database username - Set via terraform.tfvars or TF_VAR_db_username environment variable"
  type        = string
  sensitive   = true
  # No default value for security - must be provided via terraform.tfvars
}

variable "db_password" {
  description = "Database password - Set via terraform.tfvars or TF_VAR_db_password environment variable"
  type        = string
  sensitive   = true
  # No default value for security - must be provided via terraform.tfvars
}

variable "openai_api_key" {
  description = "OpenAI API key for LLM tagging functionality - Set via terraform.tfvars or TF_VAR_openai_api_key environment variable"
  type        = string
  sensitive   = true
  # No default value for security - must be provided via terraform.tfvars
}

# VPC 생성 또는 기존 VPC 사용
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# RDS 서브넷 그룹
resource "aws_db_subnet_group" "techbloghub" {
  name       = "techbloghub-db-subnet-group"
  subnet_ids = data.aws_subnets.default.ids

  tags = {
    Name = "TechBlogHub DB subnet group"
  }
}

# RDS 보안 그룹
resource "aws_security_group" "rds" {
  name_prefix = "techbloghub-rds-"
  vpc_id      = data.aws_vpc.default.id

  # ingress rule은 별도의 aws_security_group_rule로 정의됨 (순환 참조 방지)

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "TechBlogHub RDS Security Group"
  }
}

# RDS PostgreSQL 인스턴스
resource "aws_db_instance" "techbloghub" {
  identifier = "techbloghub-postgres"

  # Engine settings
  engine         = "postgres"
  engine_version = "15"
  instance_class = "db.t3.micro"  # 프리티어 적격

  # Database settings
  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  # Storage settings
  allocated_storage     = 20
  max_allocated_storage = 100
  storage_type          = "gp2"
  storage_encrypted     = true

  # Network settings
  db_subnet_group_name   = aws_db_subnet_group.techbloghub.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false

  # Maintenance settings
  backup_retention_period = 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"

  # Deletion protection
  deletion_protection = false  # 개발환경용, 운영환경에서는 true 권장
  skip_final_snapshot = true   # 개발환경용, 운영환경에서는 false 권장

  tags = {
    Name = "TechBlogHub PostgreSQL"
  }
}

# Outputs
output "rds_endpoint" {
  description = "RDS instance endpoint"
  value       = aws_db_instance.techbloghub.endpoint
}

output "rds_port" {
  description = "RDS instance port"
  value       = aws_db_instance.techbloghub.port
}

output "database_url" {
  description = "Database connection URL"
  value       = "jdbc:postgresql://${aws_db_instance.techbloghub.endpoint}:${aws_db_instance.techbloghub.port}/${var.db_name}"
  sensitive   = true
}