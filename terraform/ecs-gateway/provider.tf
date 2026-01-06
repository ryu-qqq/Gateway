# ========================================
# Terraform Provider Configuration
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
    key            = "gateway/ecs-gateway/terraform.tfstate"
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
  default     = "prod"
}

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "gateway_cpu" {
  description = "CPU units for gateway task"
  type        = number
  default     = 512
}

variable "gateway_memory" {
  description = "Memory for gateway task"
  type        = number
  default     = 1024
}

variable "gateway_desired_count" {
  description = "Desired count for gateway service"
  type        = number
  default     = 2
}

variable "image_tag" {
  description = "Docker image tag to deploy. Auto-set by GitHub Actions build-and-deploy.yml. Format: gateway-{build-number}-{git-sha}"
  type        = string
  default     = "gateway-1-initial" # Fallback only - GitHub Actions will override this

  validation {
    condition     = can(regex("^gateway-[0-9]+-[a-z0-9]+$", var.image_tag))
    error_message = "Image tag must follow format: gateway-{build-number}-{git-sha} (e.g., gateway-1-abc1234)"
  }
}

variable "auth_hub_url" {
  description = "AuthHub service URL for authentication"
  type        = string
  default     = "https://auth.set-of.com"
}

# NOTE: sentry_dsn is managed externally via SSM Parameter (manually created)
# Terraform references it via data source in main.tf

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
# Redis Configuration
# ========================================
data "aws_ssm_parameter" "redis_endpoint" {
  name = "/${var.project_name}/elasticache/redis-endpoint"
}

data "aws_ssm_parameter" "redis_port" {
  name = "/${var.project_name}/elasticache/redis-port"
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
  fqdn            = "api.set-of.com"

  # Redis Configuration
  redis_host = data.aws_ssm_parameter.redis_endpoint.value
  redis_port = tonumber(data.aws_ssm_parameter.redis_port.value)

  # AMP Configuration
  amp_workspace_arn    = data.aws_ssm_parameter.amp_workspace_arn.value
  amp_remote_write_url = data.aws_ssm_parameter.amp_remote_write_url.value
}
