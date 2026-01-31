# ========================================
# ECR Outputs
# ========================================

output "gateway_repository_url" {
  description = "ECR repository URL for gateway"
  value       = module.ecr_gateway.repository_url
}

output "gateway_repository_arn" {
  description = "ECR repository ARN for gateway"
  value       = module.ecr_gateway.repository_arn
}
