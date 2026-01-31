# ========================================
# ElastiCache Outputs
# ========================================

output "redis_endpoint" {
  description = "Redis cluster endpoint"
  value       = module.redis.endpoint_address
}

output "redis_port" {
  description = "Redis port"
  value       = module.redis.port
}

output "redis_security_group_id" {
  description = "Redis security group ID"
  value       = aws_security_group.redis.id
}
