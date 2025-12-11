# ========================================
# ElastiCache (Redis) for Gateway
# ========================================
# Redis cluster using Infrastructure module
# Naming: gateway-redis-prod
# Purpose: Session Store + Rate Limiting + Cache
# ========================================

# ========================================
# Common Tags (for governance)
# ========================================
locals {
  common_tags = {
    environment  = var.environment
    service_name = "${var.project_name}-redis"
    team         = "platform-team"
    owner        = "platform@ryuqqq.com"
    cost_center  = "engineering"
    project      = var.project_name
    data_class   = "internal"
  }
}

# ========================================
# Security Group for Redis
# ========================================
resource "aws_security_group" "redis" {
  name        = "${var.project_name}-redis-sg-${var.environment}"
  description = "Security group for ElastiCache Redis"
  vpc_id      = local.vpc_id

  ingress {
    description = "Redis from ECS tasks"
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/8"]  # VPC CIDR
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "${var.project_name}-redis-sg-${var.environment}"
    Environment = var.environment
    Service     = "${var.project_name}-redis"
    Owner       = local.common_tags.owner
    CostCenter  = local.common_tags.cost_center
    DataClass   = local.common_tags.data_class
    Lifecycle   = "production"
    ManagedBy   = "terraform"
    Project     = var.project_name
  }
}

# ========================================
# ElastiCache using Infrastructure Module
# ========================================
module "redis" {
  source = "git::https://github.com/ryu-qqq/Infrastructure.git//terraform/modules/elasticache?ref=main"

  cluster_id     = "${var.project_name}-redis-${var.environment}"
  engine         = "redis"
  engine_version = "7.0"
  node_type      = var.redis_node_type
  num_cache_nodes = 1

  # Explicit port to avoid module validation bug with null
  port = 6379

  # Network Configuration
  subnet_ids         = local.private_subnets
  security_group_ids = [aws_security_group.redis.id]

  # Parameter Group (Redisson doesn't need keyspace events)
  parameter_group_family = "redis7"
  parameters = [
    {
      name  = "maxmemory-policy"
      value = "allkeys-lru"
    }
  ]

  # Maintenance and Backup
  snapshot_retention_limit = 1
  snapshot_window          = "05:00-09:00"
  maintenance_window       = "mon:09:00-mon:10:00"

  # CloudWatch Alarms
  enable_cloudwatch_alarms = true
  alarm_cpu_threshold      = 80
  alarm_memory_threshold   = 85
  alarm_connection_threshold = 1000

  # Required Tags (governance compliance)
  environment  = local.common_tags.environment
  service_name = local.common_tags.service_name
  team         = local.common_tags.team
  owner        = local.common_tags.owner
  cost_center  = local.common_tags.cost_center
  project      = local.common_tags.project
  data_class   = local.common_tags.data_class
}

# ========================================
# Variables
# ========================================
variable "redis_node_type" {
  description = "ElastiCache node type"
  type        = string
  default     = "cache.t3.micro"
}

# ========================================
# SSM Parameters for Cross-Stack Reference
# ========================================
resource "aws_ssm_parameter" "redis_endpoint" {
  name        = "/${var.project_name}/elasticache/redis-endpoint"
  description = "Gateway Redis endpoint"
  type        = "String"
  value       = module.redis.endpoint_address

  tags = {
    Name        = "${var.project_name}-redis-endpoint"
    Environment = var.environment
  }
}

resource "aws_ssm_parameter" "redis_port" {
  name        = "/${var.project_name}/elasticache/redis-port"
  description = "Gateway Redis port"
  type        = "String"
  value       = tostring(module.redis.port)

  tags = {
    Name        = "${var.project_name}-redis-port"
    Environment = var.environment
  }
}
