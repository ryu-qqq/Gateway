# ========================================
# CloudFront Distribution for API Gateway Routing - PROD
# ========================================
# Path-based routing:
#   /api/v1/* → Gateway ALB → Legacy API
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
  comment     = "No caching for API requests - forward all to origin"
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
  comment     = "Cache policy for public static files - override Origin no-cache headers"
  min_ttl     = 3600      # 최소 1시간 (Origin이 no-cache 보내도 무시)
  default_ttl = 86400     # 기본 1일
  max_ttl     = 604800    # 최대 1주

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

# Next.js Image Optimization Cache Policy - Include query strings in cache key
resource "aws_cloudfront_cache_policy" "nextjs_image" {
  name        = "${var.project_name}-nextjs-image-${var.environment}"
  comment     = "Cache policy for Next.js Image Optimization - includes query strings"
  min_ttl     = 0
  default_ttl = 86400    # 1 day
  max_ttl     = 31536000 # 1 year

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
      query_string_behavior = "all"  # Include all query strings (url, w, q)
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
  comment = "Forward all viewer headers + X-Forwarded-For for API requests"

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
# Response Headers Policy (CORS for API)
# ========================================
resource "aws_cloudfront_response_headers_policy" "api_cors" {
  name    = "${var.project_name}-api-cors-${var.environment}"
  comment = "CORS headers for API responses"

  cors_config {
    access_control_allow_credentials = true

    access_control_allow_headers {
      items = ["Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin"]
    }

    access_control_allow_methods {
      items = ["GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"]
    }

    access_control_allow_origins {
      items = ["https://set-of.com", "https://stage.set-of.com", "https://www.set-of.com", "https://admin.set-of.com", "https://oms.set-of.com"]
    }

    access_control_max_age_sec = 86400

    origin_override = false
  }
}

# ========================================
# CloudFront Distribution - Production (set-of.com)
# ========================================
resource "aws_cloudfront_distribution" "prod" {
  enabled             = true
  is_ipv6_enabled     = true
  comment             = "set-of.com - API Gateway routing"
  default_root_object = ""
  price_class         = "PriceClass_200" # Asia, Europe, North America
  aliases             = ["www.set-of.com"]

  # ========================================
  # Origin 1: Frontend ALB (default)
  # ========================================
  origin {
    domain_name = data.aws_lb.frontend_prod.dns_name
    origin_id   = "frontend-alb"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }
  }

  # ========================================
  # Origin 2: Gateway ALB (for /api/v1/*)
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
      value = "set-of.com"
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
  # Ordered Cache Behavior: /api/v1/* → Gateway
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
    Name = "${var.project_name}-cloudfront-prod"
  })
}

# ========================================
# CloudFront Distribution - Admin (admin.set-of.com)
# ========================================
import {
  to = aws_cloudfront_distribution.admin
  id = "E1XBS551INTJTQ"
}

resource "aws_cloudfront_distribution" "admin" {
  enabled             = true
  is_ipv6_enabled     = true
  comment             = "admin.set-of.com - Admin API Gateway routing"
  default_root_object = ""
  price_class         = "PriceClass_200"
  aliases             = ["admin.set-of.com"]

  # ========================================
  # Origin: Gateway ALB (모든 요청 → Gateway → Legacy Admin API)
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
      value = "admin.set-of.com"
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

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-cloudfront-admin"
  })
}

# ========================================
# Route53 Records - Point to CloudFront
# ========================================
# NOTE: set-of.com Route53 record is managed in infrastructure repository
# This Terraform only manages www.set-of.com

# www.set-of.com → CloudFront
import {
  to = aws_route53_record.prod_www
  id = "Z104656329CL6XBYE8OIJ_www.set-of.com_A"
}

resource "aws_route53_record" "prod_www" {
  zone_id = local.route53_zone_id
  name    = "www.set-of.com"
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.prod.domain_name
    zone_id                = aws_cloudfront_distribution.prod.hosted_zone_id
    evaluate_target_health = false
  }
}

# admin.set-of.com → CloudFront
import {
  to = aws_route53_record.admin
  id = "Z104656329CL6XBYE8OIJ_admin.set-of.com_A"
}

resource "aws_route53_record" "admin" {
  zone_id = local.route53_zone_id
  name    = "admin.set-of.com"
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.admin.domain_name
    zone_id                = aws_cloudfront_distribution.admin.hosted_zone_id
    evaluate_target_health = false
  }
}
