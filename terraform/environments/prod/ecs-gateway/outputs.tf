# ========================================
# ECS gateway Outputs - PROD
# ========================================

output "alb_dns_name" {
  description = "ALB DNS name"
  value       = aws_lb.gateway.dns_name
}

output "alb_arn" {
  description = "ALB ARN"
  value       = aws_lb.gateway.arn
}

output "service_name" {
  description = "ECS service name"
  value       = module.ecs_service.service_name
}

output "service_arn" {
  description = "ECS service ARN"
  value       = module.ecs_service.service_id
}

output "task_definition_arn" {
  description = "Task definition ARN"
  value       = module.ecs_service.task_definition_arn
}

output "fqdn" {
  description = "Fully qualified domain name"
  value       = local.fqdn
}

output "url" {
  description = "Application URL"
  value       = "https://${local.fqdn}"
}

output "log_group_name" {
  description = "CloudWatch log group name"
  value       = module.gateway_logs.log_group_name
}

output "kms_key_arn" {
  description = "KMS key ARN for logs encryption"
  value       = aws_kms_key.logs.arn
}
