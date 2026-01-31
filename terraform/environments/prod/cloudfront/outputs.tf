# ========================================
# CloudFront Module Outputs - PROD
# ========================================

# ========================================
# Production CloudFront Distribution
# ========================================
output "prod_distribution_id" {
  description = "CloudFront distribution ID for production (set-of.com)"
  value       = aws_cloudfront_distribution.prod.id
}

output "prod_distribution_arn" {
  description = "CloudFront distribution ARN for production"
  value       = aws_cloudfront_distribution.prod.arn
}

output "prod_distribution_domain_name" {
  description = "CloudFront domain name for production"
  value       = aws_cloudfront_distribution.prod.domain_name
}

output "prod_distribution_hosted_zone_id" {
  description = "CloudFront hosted zone ID for production (for Route53 alias)"
  value       = aws_cloudfront_distribution.prod.hosted_zone_id
}

# ========================================
# Admin CloudFront Distribution
# ========================================
output "admin_distribution_id" {
  description = "CloudFront distribution ID for admin (admin.set-of.com)"
  value       = aws_cloudfront_distribution.admin.id
}

output "admin_distribution_arn" {
  description = "CloudFront distribution ARN for admin"
  value       = aws_cloudfront_distribution.admin.arn
}

output "admin_distribution_domain_name" {
  description = "CloudFront domain name for admin"
  value       = aws_cloudfront_distribution.admin.domain_name
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
output "route53_records" {
  description = "Route53 records created for CloudFront"
  value = {
    prod_apex = aws_route53_record.prod_apex.fqdn
    prod_www  = aws_route53_record.prod_www.fqdn
    admin     = aws_route53_record.admin.fqdn
  }
}

# ========================================
# Routing Summary
# ========================================
output "routing_summary" {
  description = "Summary of CloudFront path-based routing configuration"
  value = {
    production = {
      domains          = ["set-of.com", "www.set-of.com"]
      api_path         = "/api/v1/*"
      api_origin       = data.aws_lb.gateway.dns_name
      frontend_origin  = data.aws_lb.frontend_prod.dns_name
    }
    admin = {
      domains    = ["admin.set-of.com"]
      api_origin = data.aws_lb.gateway.dns_name
    }
  }
}
