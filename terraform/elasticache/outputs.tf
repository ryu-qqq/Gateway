# ========================================
# ElastiCache Outputs
# ========================================

output "redis_endpoint" {
  description = "Redis primary endpoint"
  value       = module.redis.primary_endpoint_address
}

output "redis_port" {
  description = "Redis port"
  value       = module.redis.port
}

output "redis_security_group_id" {
  description = "Redis security group ID"
  value       = aws_security_group.redis.id
}

output "cluster_id" {
  description = "ElastiCache cluster ID"
  value       = module.redis.cluster_id
}
