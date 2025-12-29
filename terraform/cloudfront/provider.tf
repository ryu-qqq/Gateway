# ========================================
# Terraform Provider Configuration
# ========================================
# CloudFront for Host-Based API Routing
# - set-of.com, stage.set-of.com → /api/v1/* → Gateway ALB
# - admin.set-of.com → /api/v1/* → Gateway ALB (Legacy Admin)
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
    key            = "gateway/cloudfront/terraform.tfstate"
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
  default     = "prod"
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

# ACM Certificate (ap-northeast-2 - for ALB reference)
data "aws_ssm_parameter" "certificate_arn" {
  name = "/shared/network/certificate-arn"
}

# ========================================
# CloudFront requires ACM certificate in us-east-1
# ========================================
# NOTE: CloudFront용 인증서가 us-east-1에 있어야 합니다.
# 인증서는 set-of.com + *.set-of.com 둘 다 커버해야 합니다.
data "aws_acm_certificate" "cloudfront_cert" {
  provider    = aws.us_east_1
  domain      = "set-of.com"
  statuses    = ["ISSUED"]
  most_recent = true
}

# ========================================
# ALB References
# ========================================

# Gateway ALB (API routing target)
data "aws_lb" "gateway" {
  name = "gateway-alb-prod"
}

# Frontend ALB - set-of.com (Next.js production)
data "aws_lb" "frontend_prod" {
  name = "turbo-setof-web-lb"
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

  # Domain configurations
  domains = {
    prod = {
      domain_name   = "set-of.com"
      aliases       = ["set-of.com", "www.set-of.com"]
      frontend_alb  = data.aws_lb.frontend_prod
    }
    stage = {
      domain_name   = "stage.set-of.com"
      aliases       = ["stage.set-of.com"]
      frontend_alb  = data.aws_lb.frontend_stage
    }
  }
}
