# ========================================
# ECS Cluster for Gateway - STAGE
# ========================================
# Dedicated ECS Cluster (not shared)
# Naming: gateway-cluster-stage
# ========================================

# ========================================
# Common Tags (for governance)
# ========================================
locals {
  common_tags = {
    environment  = var.environment
    service_name = "${var.project_name}-ecs-cluster"
    team         = "platform-team"
    owner        = "platform@ryuqqq.com"
    cost_center  = "engineering"
    project      = var.project_name
    data_class   = "internal"
  }
}

# ========================================
# ECS Cluster
# ========================================
resource "aws_ecs_cluster" "main" {
  name = "${var.project_name}-cluster-${var.environment}"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = {
    Name        = "${var.project_name}-cluster-${var.environment}"
    Environment = var.environment
    Service     = local.common_tags.service_name
    Owner       = local.common_tags.owner
    CostCenter  = local.common_tags.cost_center
    DataClass   = local.common_tags.data_class
    Lifecycle   = "staging"
    ManagedBy   = "terraform"
    Project     = var.project_name
  }
}

# ========================================
# ECS Cluster Capacity Providers
# ========================================
resource "aws_ecs_cluster_capacity_providers" "main" {
  cluster_name = aws_ecs_cluster.main.name

  capacity_providers = ["FARGATE", "FARGATE_SPOT"]

  # Stage: FARGATE_SPOT 70% (비용 절감)
  default_capacity_provider_strategy {
    base              = 0
    weight            = 30
    capacity_provider = "FARGATE"
  }

  default_capacity_provider_strategy {
    base              = 1
    weight            = 70
    capacity_provider = "FARGATE_SPOT"
  }
}

# ========================================
# SSM Parameters for Cross-Stack Reference
# ========================================
resource "aws_ssm_parameter" "cluster_arn" {
  name        = "/${var.project_name}/${var.environment}/ecs/cluster-arn"
  description = "Gateway ECS cluster ARN (${var.environment})"
  type        = "String"
  value       = aws_ecs_cluster.main.arn

  tags = {
    Name        = "${var.project_name}-ecs-cluster-arn-${var.environment}"
    Environment = var.environment
  }
}

resource "aws_ssm_parameter" "cluster_name" {
  name        = "/${var.project_name}/${var.environment}/ecs/cluster-name"
  description = "Gateway ECS cluster name (${var.environment})"
  type        = "String"
  value       = aws_ecs_cluster.main.name

  tags = {
    Name        = "${var.project_name}-ecs-cluster-name-${var.environment}"
    Environment = var.environment
  }
}

# ========================================
# Outputs
# ========================================
output "cluster_id" {
  description = "ECS Cluster ID"
  value       = aws_ecs_cluster.main.id
}

output "cluster_arn" {
  description = "ECS Cluster ARN"
  value       = aws_ecs_cluster.main.arn
}

output "cluster_name" {
  description = "ECS Cluster Name"
  value       = aws_ecs_cluster.main.name
}

# Atlantis trigger: Initial stage deployment
