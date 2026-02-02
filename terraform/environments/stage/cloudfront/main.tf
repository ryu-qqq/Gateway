# ========================================
# CloudFront Distribution for API Gateway Routing - STAGE
# ========================================
# Path-based routing:
#   /api/v1/* → Gateway ALB (Stage) → Legacy API
#   /*        → Frontend ALB → Next.js
# ========================================

# ========================================
# AWS Managed Cache Policies (Data Sources)
# ========================================
data "aws_cloudfront_cache_policy" "caching_disabled" {
  name = "Managed-CachingDisabled"
}

data "aws_cloudfront_cache_policy" "caching_optimized" {
  name = "Managed-CachingOptimized"
}

data "aws_cloudfront_origin_request_policy" "all_viewer" {
  name = "Managed-AllViewer"
}

# ========================================
# Public Static File Paths (for DRY cache behaviors)
# ========================================
locals {
  public_static_paths = [
    "/favicon.ico",
    "/robots.txt",
    "/sitemap*.xml"
  ]
}

# ========================================
# Cache Policies
# ========================================

# API Cache Policy - No caching, forward all headers
resource "aws_cloudfront_cache_policy" "api_no_cache" {
  name        = "${var.project_name}-api-no-cache-${var.environment}"
  comment     = "No caching for API requests - forward all to origin (${var.environment})"
  min_ttl     = 0
  default_ttl = 0
  max_ttl     = 0

  parameters_in_cache_key_and_forwarded_to_origin {
    cookies_config {
      cookie_behavior = "none"
    }
    headers_config {
      header_behavior = "none"
    }
    query_strings_config {
      query_string_behavior = "none"
    }
  }
}

# Public Static Files Cache Policy - Override Origin headers
resource "aws_cloudfront_cache_policy" "public_static" {
  name        = "${var.project_name}-public-static-${var.environment}"
  comment     = "Cache policy for public static files (${var.environment})"
  min_ttl     = 3600
  default_ttl = 86400
  max_ttl     = 604800

  parameters_in_cache_key_and_forwarded_to_origin {
    cookies_config {
      cookie_behavior = "none"
    }
    headers_config {
      header_behavior = "none"
    }
    query_strings_config {
      query_string_behavior = "none"
    }
    enable_accept_encoding_brotli = true
    enable_accept_encoding_gzip   = true
  }
}

# Next.js Image Optimization Cache Policy
resource "aws_cloudfront_cache_policy" "nextjs_image" {
  name        = "${var.project_name}-nextjs-image-${var.environment}"
  comment     = "Cache policy for Next.js Image Optimization (${var.environment})"
  min_ttl     = 0
  default_ttl = 86400
  max_ttl     = 31536000

  parameters_in_cache_key_and_forwarded_to_origin {
    cookies_config {
      cookie_behavior = "none"
    }
    headers_config {
      header_behavior = "whitelist"
      headers {
        items = ["Accept"]
      }
    }
    query_strings_config {
      query_string_behavior = "all"
    }
    enable_accept_encoding_brotli = true
    enable_accept_encoding_gzip   = true
  }
}

# ========================================
# Origin Request Policies
# ========================================

# API Origin Request Policy - Forward all necessary headers
resource "aws_cloudfront_origin_request_policy" "api_all_viewer" {
  name    = "${var.project_name}-api-all-viewer-${var.environment}"
  comment = "Forward all viewer headers + X-Forwarded-For for API requests (${var.environment})"

  cookies_config {
    cookie_behavior = "all"
  }
  headers_config {
    header_behavior = "allViewerAndWhitelistCloudFront"
    headers {
      items = [
        "CloudFront-Forwarded-Proto",
        "CloudFront-Is-Mobile-Viewer",
        "CloudFront-Viewer-Address"
      ]
    }
  }
  query_strings_config {
    query_string_behavior = "all"
  }
}

# ========================================
# Response Headers Policy (CORS for Public API)
# ========================================
resource "aws_cloudfront_response_headers_policy" "api_cors" {
  name    = "${var.project_name}-api-cors-${var.environment}"
  comment = "CORS headers for Public API responses (${var.environment})"

  cors_config {
    access_control_allow_credentials = true

    access_control_allow_headers {
      items = ["Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"]
    }

    access_control_allow_methods {
      items = ["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"]
    }

    access_control_allow_origins {
      items = ["https://set-of.com", "https://stage.set-of.com", "https://www.set-of.com"]
    }

    access_control_expose_headers {
      items = ["X-New-Access-Token", "X-Trace-Id"]
    }

    access_control_max_age_sec = 86400

    origin_override = false
  }
}

# ========================================
# Response Headers Policy (CORS for Admin API)
# ========================================
resource "aws_cloudfront_response_headers_policy" "admin_api_cors" {
  name    = "${var.project_name}-admin-api-cors-${var.environment}"
  comment = "CORS headers for Admin API responses (${var.environment})"

  cors_config {
    access_control_allow_credentials = true

    access_control_allow_headers {
      items = ["Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"]
    }

    access_control_allow_methods {
      items = ["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"]
    }

    access_control_allow_origins {
      items = ["https://stage-admin.set-of.com", "https://admin.set-of.com"]
    }

    access_control_expose_headers {
      items = ["X-New-Access-Token", "X-Trace-Id"]
    }

    access_control_max_age_sec = 86400

    origin_override = false
  }
}

# ========================================
# CloudFront Distribution - Staging (stage.set-of.com)
# ========================================
resource "aws_cloudfront_distribution" "stage" {
  enabled             = true
  is_ipv6_enabled     = true
  comment             = "stage.set-of.com - API Gateway routing (Stage)"
  default_root_object = ""
  price_class         = "PriceClass_200"
  aliases             = ["stage.set-of.com"]

  # ========================================
  # Origin 1: Frontend ALB (default)
  # ========================================
  origin {
    domain_name = data.aws_lb.frontend_stage.dns_name
    origin_id   = "frontend-alb"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  # ========================================
  # Origin 2: Gateway ALB (Stage - for /api/v1/*)
  # ========================================
  origin {
    domain_name = data.aws_lb.gateway.dns_name
    origin_id   = "gateway-alb"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }

    custom_header {
      name  = "X-Forwarded-Host"
      value = "stage.set-of.com"
    }
  }

  # ========================================
  # Default Cache Behavior → Frontend (HTML pages - no cache)
  # ========================================
  default_cache_behavior {
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "frontend-alb"

    cache_policy_id          = data.aws_cloudfront_cache_policy.caching_disabled.id
    origin_request_policy_id = data.aws_cloudfront_origin_request_policy.all_viewer.id

    viewer_protocol_policy = "redirect-to-https"
    compress               = true
  }

  # ========================================
  # Ordered Cache Behavior: /_next/static/* → Frontend (long-term cache)
  # ========================================
  ordered_cache_behavior {
    path_pattern     = "/_next/static/*"
    allowed_methods  = ["GET", "HEAD", "OPTIONS"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "frontend-alb"

    cache_policy_id          = data.aws_cloudfront_cache_policy.caching_optimized.id
    origin_request_policy_id = data.aws_cloudfront_origin_request_policy.all_viewer.id

    viewer_protocol_policy = "redirect-to-https"
    compress               = true
  }

  # ========================================
  # Ordered Cache Behavior: /_next/image/* → Frontend (with query string caching)
  # ========================================
  ordered_cache_behavior {
    path_pattern     = "/_next/image*"
    allowed_methods  = ["GET", "HEAD", "OPTIONS"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "frontend-alb"

    cache_policy_id          = aws_cloudfront_cache_policy.nextjs_image.id
    origin_request_policy_id = data.aws_cloudfront_origin_request_policy.all_viewer.id

    viewer_protocol_policy = "redirect-to-https"
    compress               = true
  }

  # ========================================
  # Ordered Cache Behavior: Public Static Files → Frontend (force cache)
  # ========================================
  dynamic "ordered_cache_behavior" {
    for_each = toset(local.public_static_paths)

    content {
      path_pattern     = ordered_cache_behavior.value
      allowed_methods  = ["GET", "HEAD"]
      cached_methods   = ["GET", "HEAD"]
      target_origin_id = "frontend-alb"

      cache_policy_id          = aws_cloudfront_cache_policy.public_static.id
      origin_request_policy_id = data.aws_cloudfront_origin_request_policy.all_viewer.id

      viewer_protocol_policy = "redirect-to-https"
      compress               = true
    }
  }

  # ========================================
  # Ordered Cache Behavior: /api/v1/* → Gateway (Stage)
  # ========================================
  ordered_cache_behavior {
    path_pattern     = "/api/v1/*"
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "gateway-alb"

    cache_policy_id            = aws_cloudfront_cache_policy.api_no_cache.id
    origin_request_policy_id   = aws_cloudfront_origin_request_policy.api_all_viewer.id
    response_headers_policy_id = aws_cloudfront_response_headers_policy.api_cors.id

    viewer_protocol_policy = "https-only"
    compress               = false
  }

  # ========================================
  # SSL Certificate
  # ========================================
  viewer_certificate {
    acm_certificate_arn      = data.aws_acm_certificate.cloudfront_cert.arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }

  # ========================================
  # Restrictions (No geo restrictions)
  # ========================================
  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-cloudfront-${var.environment}"
  })
}

# ========================================
# Route53 Records - Point to CloudFront
# ========================================

# stage.set-of.com → CloudFront
import {
  to = aws_route53_record.stage
  id = "Z104656329CL6XBYE8OIJ_stage.set-of.com_A"
}

resource "aws_route53_record" "stage" {
  zone_id = local.route53_zone_id
  name    = "stage.set-of.com"
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.stage.domain_name
    zone_id                = aws_cloudfront_distribution.stage.hosted_zone_id
    evaluate_target_health = false
  }
}

# ========================================
# WAFv2 WebACL for Stage Admin CloudFront
# ========================================
# OWASP Core Rule Set 기반 보호 (SQLi, XSS, Bad Inputs 방어)
# CloudFront 전용 WebACL은 us-east-1 리전에 생성 필요
# ========================================
resource "aws_wafv2_web_acl" "admin_stage" {
  provider    = aws.us_east_1
  name        = "${var.project_name}-admin-waf-${var.environment}"
  description = "WAF WebACL for Stage Admin CloudFront"
  scope       = "CLOUDFRONT"

  default_action {
    allow {}
  }

  # AWS Managed Rules - Common Rule Set (SQLi, XSS 등 기본 방어)
  rule {
    name     = "AWSManagedRulesCommonRuleSet"
    priority = 1

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesCommonRuleSet"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "${var.project_name}-admin-common-rules"
      sampled_requests_enabled   = true
    }
  }

  # AWS Managed Rules - Known Bad Inputs (Log4j, 악성 페이로드 방어)
  rule {
    name     = "AWSManagedRulesKnownBadInputsRuleSet"
    priority = 2

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesKnownBadInputsRuleSet"
        vendor_name = "AWS"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "${var.project_name}-admin-bad-inputs"
      sampled_requests_enabled   = true
    }
  }

  # Rate Limiting (분당 2000 요청 초과 시 차단)
  rule {
    name     = "RateLimitRule"
    priority = 3

    action {
      block {}
    }

    statement {
      rate_based_statement {
        limit              = 2000
        aggregate_key_type = "IP"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "${var.project_name}-admin-rate-limit"
      sampled_requests_enabled   = true
    }
  }

  visibility_config {
    cloudwatch_metrics_enabled = true
    metric_name                = "${var.project_name}-admin-waf"
    sampled_requests_enabled   = true
  }

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-admin-waf-${var.environment}"
  })
}

# ========================================
# CloudFront Distribution - Stage Admin (stage-admin.set-of.com)
# ========================================
# Strangler Fig Pattern 테스트를 위한 Stage Admin CloudFront
# 모든 요청 → Gateway ALB → Legacy Admin 또는 New Service
# ========================================
resource "aws_cloudfront_distribution" "admin_stage" {
  # WAF WebACL 연결
  web_acl_id = aws_wafv2_web_acl.admin_stage.arn
  enabled             = true
  is_ipv6_enabled     = true
  comment             = "stage-admin.set-of.com - Admin API Gateway routing (Stage)"
  default_root_object = ""
  price_class         = "PriceClass_200"
  aliases             = ["stage-admin.set-of.com"]

  # ========================================
  # Origin: Gateway ALB (모든 요청 → Gateway → Legacy Admin 또는 New Service)
  # ========================================
  origin {
    domain_name = data.aws_lb.gateway.dns_name
    origin_id   = "gateway-alb"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "http-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }

    # Gateway가 호스트를 인식할 수 있도록 X-Forwarded-Host 헤더 추가
    custom_header {
      name  = "X-Forwarded-Host"
      value = "stage-admin.set-of.com"
    }
  }

  # ========================================
  # Default Cache Behavior → Gateway (API 전용, 캐싱 없음)
  # ========================================
  default_cache_behavior {
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "gateway-alb"

    cache_policy_id            = aws_cloudfront_cache_policy.api_no_cache.id
    origin_request_policy_id   = aws_cloudfront_origin_request_policy.api_all_viewer.id
    response_headers_policy_id = aws_cloudfront_response_headers_policy.admin_api_cors.id

    viewer_protocol_policy = "https-only"
    compress               = false
  }

  # ========================================
  # SSL Certificate
  # ========================================
  viewer_certificate {
    acm_certificate_arn      = data.aws_acm_certificate.cloudfront_cert.arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }

  # ========================================
  # Restrictions (No geo restrictions)
  # ========================================
  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-cloudfront-admin-${var.environment}"
  })
}

# ========================================
# Route53 Record - Stage Admin
# ========================================

# stage-admin.set-of.com → CloudFront
resource "aws_route53_record" "admin_stage" {
  zone_id = local.route53_zone_id
  name    = "stage-admin.set-of.com"
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.admin_stage.domain_name
    zone_id                = aws_cloudfront_distribution.admin_stage.hosted_zone_id
    evaluate_target_health = false
  }
}

# Atlantis trigger: Stage Admin CloudFront for Strangler Fig Pattern
