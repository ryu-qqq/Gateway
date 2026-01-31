# ========================================
# CloudFront Module Outputs - STAGE
# ========================================

# ========================================
# Staging CloudFront Distribution
# ========================================
output "stage_distribution_id" {
  description = "CloudFront distribution ID for staging (stage.set-of.com)"
  value       = aws_cloudfront_distribution.stage.id
}

output "stage_distribution_arn" {
  description = "CloudFront distribution ARN for staging"
  value       = aws_cloudfront_distribution.stage.arn
}

output "stage_distribution_domain_name" {
  description = "CloudFront domain name for staging"
  value       = aws_cloudfront_distribution.stage.domain_name
}

output "stage_distribution_hosted_zone_id" {
  description = "CloudFront hosted zone ID for staging (for Route53 alias)"
  value       = aws_cloudfront_distribution.stage.hosted_zone_id
}

# ========================================
# Policy IDs (for reference)
# ========================================
output "api_cache_policy_id" {
  description = "Cache policy ID for API requests (no caching)"
  value       = aws_cloudfront_cache_policy.api_no_cache.id
}

output "api_origin_request_policy_id" {
  description = "Origin request policy ID for API requests"
  value       = aws_cloudfront_origin_request_policy.api_all_viewer.id
}

output "api_cors_policy_id" {
  description = "Response headers policy ID for CORS"
  value       = aws_cloudfront_response_headers_policy.api_cors.id
}

# ========================================
# Route53 Record Info
# ========================================
output "route53_record" {
  description = "Route53 record created for CloudFront"
  value       = aws_route53_record.stage.fqdn
}

# ========================================
# Routing Summary
# ========================================
output "routing_summary" {
  description = "Summary of CloudFront path-based routing configuration"
  value = {
    staging = {
      domains          = ["stage.set-of.com"]
      api_path         = "/api/v1/*"
      api_origin       = data.aws_lb.gateway.dns_name
      frontend_origin  = data.aws_lb.frontend_stage.dns_name
    }
  }
}
