# ========================================
# Terraform Provider Configuration - STAGE
# ========================================

terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket         = "prod-connectly"
    key            = "gateway/stage/ecs-gateway/terraform.tfstate"
    region         = "ap-northeast-2"
    dynamodb_table = "prod-connectly-tf-lock"
    encrypt        = true
    kms_key_id     = "arn:aws:kms:ap-northeast-2:646886795421:key/086b1677-614f-46ba-863e-23c215fb5010"
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = var.project_name
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

# ========================================
# Common Variables
# ========================================
variable "project_name" {
  description = "Project name"
  type        = string
  default     = "gateway"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "stage"
}

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "gateway_cpu" {
  description = "CPU units for gateway task"
  type        = number
  default     = 256  # Stage: 더 낮은 스펙
}

variable "gateway_memory" {
  description = "Memory for gateway task"
  type        = number
  default     = 512  # Stage: 더 낮은 스펙
}

variable "gateway_desired_count" {
  description = "Desired count for gateway service"
  type        = number
  default     = 1  # Stage: 1개만
}

variable "image_tag" {
  description = "Docker image tag to deploy. Format: gateway-{build-number}-{git-sha}"
  type        = string
  default     = "gateway-1-initial"

  validation {
    condition     = can(regex("^gateway-[0-9]+-[a-z0-9]+$", var.image_tag))
    error_message = "Image tag must follow format: gateway-{build-number}-{git-sha} (e.g., gateway-1-abc1234)"
  }
}

variable "auth_hub_url" {
  description = "AuthHub service URL for authentication (Stage)"
  type        = string
  default     = "https://stage-auth.set-of.com"  # Stage AuthHub
}

# ========================================
# Shared Resource References (SSM)
# ========================================
data "aws_ssm_parameter" "vpc_id" {
  name = "/shared/network/vpc-id"
}

data "aws_ssm_parameter" "private_subnets" {
  name = "/shared/network/private-subnets"
}

data "aws_ssm_parameter" "public_subnets" {
  name = "/shared/network/public-subnets"
}

data "aws_ssm_parameter" "certificate_arn" {
  name = "/shared/network/certificate-arn"
}

data "aws_ssm_parameter" "route53_zone_id" {
  name = "/shared/network/route53-zone-id"
}

# ========================================
# Monitoring Configuration (AMP)
# ========================================
data "aws_ssm_parameter" "amp_workspace_arn" {
  name = "/shared/monitoring/amp-workspace-arn"
}

data "aws_ssm_parameter" "amp_remote_write_url" {
  name = "/shared/monitoring/amp-remote-write-url"
}

# ========================================
# Redis Configuration (공유 Redis - Stage)
# ========================================
data "aws_ssm_parameter" "redis_endpoint" {
  name = "/shared/stage/elasticache/redis-endpoint"
}

data "aws_ssm_parameter" "redis_port" {
  name = "/shared/stage/elasticache/redis-port"
}

# ========================================
# Locals
# ========================================
locals {
  vpc_id          = data.aws_ssm_parameter.vpc_id.value
  private_subnets = split(",", data.aws_ssm_parameter.private_subnets.value)
  public_subnets  = split(",", data.aws_ssm_parameter.public_subnets.value)
  certificate_arn = data.aws_ssm_parameter.certificate_arn.value
  route53_zone_id = data.aws_ssm_parameter.route53_zone_id.value
  fqdn            = "stage-api.set-of.com"  # Stage 전용 API 도메인

  # Redis Configuration (공유 Redis)
  redis_host = data.aws_ssm_parameter.redis_endpoint.value
  redis_port = tonumber(data.aws_ssm_parameter.redis_port.value)

  # AMP Configuration
  amp_workspace_arn    = data.aws_ssm_parameter.amp_workspace_arn.value
  amp_remote_write_url = data.aws_ssm_parameter.amp_remote_write_url.value
}
