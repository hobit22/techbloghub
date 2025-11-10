# Terraform Configuration and Provider Setup
# Variables for TechBlogHub Infrastructure (Backend on ECS + Supabase Database)

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

# Database connection variables (Supabase)
variable "database_url" {
  description = "Database connection URL (JDBC format) - Supabase PostgreSQL"
  type        = string
  sensitive   = true
}

variable "database_username" {
  description = "Database username - Supabase"
  type        = string
  sensitive   = true
}

variable "database_password" {
  description = "Database password - Supabase"
  type        = string
  sensitive   = true
}

variable "openai_api_key" {
  description = "OpenAI API key for LLM tagging functionality - Set via terraform.tfvars or TF_VAR_openai_api_key environment variable"
  type        = string
  sensitive   = true
  # No default value for security - must be provided via terraform.tfvars
}

variable "app_admin_username" {
  description = "App Admin UserName"
  type        = string
  sensitive   = true
  # No default value for security - must be provided via terraform.tfvars
}

variable "app_admin_password" {
  description = "App Admin Password"
  type        = string
  sensitive   = true
  # No default value for security - must be provided via terraform.tfvars
}

variable "rss_proxy_url" {
  description = "RSS Proxy URL for bypassing IP blocking (CloudFlare Workers)"
  type        = string
  sensitive   = true
  # No default value for security - must be provided via terraform.tfvars
}

variable "discord_webhook_url" {
  description = "Discord Webhook Url"
  type        = string
  sensitive   = true
  # No default value for security - must be provided via terraform.tfvars
}

# VPC 및 서브넷 데이터 소스
data "aws_vpc" "default" {
  default = true
}

# 가용 영역 데이터 소스
data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }

  # 첫 2개의 가용 영역만 사용 (비용 최적화)
  filter {
    name   = "availability-zone"
    values = [
      data.aws_availability_zones.available.names[0],
      data.aws_availability_zones.available.names[1]
    ]
  }
}
