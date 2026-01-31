# ========================================
# Terraform Provider Configuration - STAGE
# ========================================
# CloudFront for Host-Based API Routing
# - stage.set-of.com → /api/v1/* → Gateway ALB (Stage)
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
    key            = "gateway/stage/cloudfront/terraform.tfstate"
    region         = "ap-northeast-2"
    dynamodb_table = "prod-connectly-tf-lock"
    encrypt        = true
    kms_key_id     = "arn:aws:kms:ap-northeast-2:646886795421:key/086b1677-614f-46ba-863e-23c215fb5010"
  }
}

# CloudFront requires us-east-1 for ACM certificates
provider "aws" {
  alias  = "us_east_1"
  region = "us-east-1"
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
# Variables
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

# ========================================
# Common Tags
# ========================================
locals {
  common_tags = {
    environment  = var.environment
    service_name = var.project_name
    team         = "platform-team"
    owner        = "platform@ryuqqq.com"
    cost_center  = "engineering"
    project      = var.project_name
  }
}

# ========================================
# Shared Resource References (SSM)
# ========================================

# Route53 Hosted Zone
data "aws_ssm_parameter" "route53_zone_id" {
  name = "/shared/network/route53-zone-id"
}

# ========================================
# CloudFront requires ACM certificate in us-east-1
# ========================================
data "aws_acm_certificate" "cloudfront_cert" {
  provider    = aws.us_east_1
  domain      = "set-of.com"
  statuses    = ["ISSUED"]
  most_recent = true
}

# ========================================
# ALB References
# ========================================

# Gateway ALB (Stage - API routing target)
data "aws_lb" "gateway" {
  name = "gateway-alb-stage"
}

# Frontend ALB - stage.set-of.com (staging)
data "aws_lb" "frontend_stage" {
  name = "turbo-setof-web-stage"
}

# ========================================
# Locals
# ========================================
locals {
  route53_zone_id = data.aws_ssm_parameter.route53_zone_id.value
}
