# ========================================
# CloudFront Distribution for API Gateway Routing
# ========================================
# Path-based routing:
#   /api/v1/* → Gateway ALB → Legacy API
#   /*        → Frontend ALB → Next.js
# ========================================

# ========================================
# AWS Managed Cache Policies (Data Sources)
# ========================================
# Using data sources instead of hardcoded IDs for better maintainability
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
# NOTE: favicon.ico, robots.txt, sitemap.xml 등은 Origin이 max-age=0을 보내도
# CloudFront에서 강제로 캐시하여 Origin 요청 감소
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
# NOTE: X-Forwarded-For는 CloudFront가 자동 추가하지만, 명시적 whitelist 필요
# - allViewerAndWhitelistCloudFront: 뷰어 헤더 + whitelist CloudFront 헤더 전달
# - X-Forwarded-For: Rate Limiting을 위한 클라이언트 IP 추출에 필수
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
        "CloudFront-Viewer-Address" # 클라이언트 IP:Port 포함 (X-Forwarded-For 대체)
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
      items = ["https://set-of.com", "https://stage.set-of.com", "https://www.set-of.com"]
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
  aliases             = ["set-of.com", "www.set-of.com"]

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
  # NOTE: HTTP 사용 - CloudFront ↔ ALB 내부 통신이므로 안전
  # ALB 인증서가 *.set-of.com이라 ALB DNS로 HTTPS 연결 시 도메인 불일치 발생
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
  # NOTE: HTML 페이지는 캐시하면 안됨 - 배포 후 즉시 반영 필요
  # 정적 자산(/_next/static/*)은 파일명에 해시가 포함되어 있어 별도 캐시 정책 적용
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
  # NOTE: 파일명에 해시 포함 (예: main-app-a27d5f1dbddc8d0f.js)
  # 새 빌드 시 파일명 변경되므로 장기 캐시해도 안전
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
  # NOTE: Origin이 max-age=0을 보내도 min_ttl=3600으로 강제 캐시
  # 파일 변경 시 CloudFront Invalidation 필요
  # Using dynamic block to reduce code duplication
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

  # ========================================
  # Logging (Optional - enable if needed)
  # ========================================
  # logging_config {
  #   include_cookies = false
  #   bucket          = "your-logs-bucket.s3.amazonaws.com"
  #   prefix          = "cloudfront/prod/"
  # }

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-cloudfront-prod"
  })
}

# ========================================
# CloudFront Distribution - Staging (stage.set-of.com)
# ========================================
resource "aws_cloudfront_distribution" "stage" {
  enabled             = true
  is_ipv6_enabled     = true
  comment             = "stage.set-of.com - API Gateway routing"
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
  # Origin 2: Gateway ALB (for /api/v1/*)
  # ========================================
  # NOTE: HTTP 사용 - CloudFront ↔ ALB 내부 통신이므로 안전
  # ALB 인증서가 *.set-of.com이라 ALB DNS로 HTTPS 연결 시 도메인 불일치 발생
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
  # NOTE: HTML 페이지는 캐시하면 안됨 - 배포 후 즉시 반영 필요
  # 정적 자산(/_next/static/*)은 파일명에 해시가 포함되어 있어 별도 캐시 정책 적용
  default_cache_behavior {
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "frontend-alb"

    # CachingDisabled - HTML 페이지 캐시 안함
    cache_policy_id          = data.aws_cloudfront_cache_policy.caching_disabled.id
    origin_request_policy_id = data.aws_cloudfront_origin_request_policy.all_viewer.id

    viewer_protocol_policy = "redirect-to-https"
    compress               = true
  }

  # ========================================
  # Ordered Cache Behavior: /_next/static/* → Frontend (long-term cache)
  # ========================================
  # NOTE: 파일명에 해시 포함 (예: main-app-a27d5f1dbddc8d0f.js)
  # 새 빌드 시 파일명 변경되므로 장기 캐시해도 안전
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
  # NOTE: Origin이 max-age=0을 보내도 min_ttl=3600으로 강제 캐시
  # 파일 변경 시 CloudFront Invalidation 필요
  # DRY: Dynamic block으로 중복 제거 (favicon.ico, robots.txt, sitemap*.xml)
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

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  tags = merge(local.common_tags, {
    Name = "${var.project_name}-cloudfront-stage"
  })
}

# ========================================
# Route53 Records - Point to CloudFront
# ========================================

# set-of.com → CloudFront
import {
  to = aws_route53_record.prod_apex
  id = "Z104656329CL6XBYE8OIJ_set-of.com_A"
}

resource "aws_route53_record" "prod_apex" {
  zone_id = local.route53_zone_id
  name    = "set-of.com"
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.prod.domain_name
    zone_id                = aws_cloudfront_distribution.prod.hosted_zone_id
    evaluate_target_health = false
  }
}

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

# stage.set-of.com → CloudFront
# Import existing record: terraform import aws_route53_record.stage Z104656329CL6XBYE8OIJ_stage.set-of.com_A
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
# CloudFront Distribution - Admin (admin.set-of.com)
# ========================================
# TODO: 배포 전 체크리스트 (stage.set-of.com 배포 시 발생했던 문제 방지)
# ──────────────────────────────────────────────────────────────────────
# 1. [  ] Gateway ALB Security Group (sg-0086211d4194a6d58) 확인
#         → Port 80 인바운드가 0.0.0.0/0 허용되어 있는지 확인
# 2. [  ] Route53에 기존 admin.set-of.com 레코드 존재 여부 확인
#         → 있으면 import 블록 추가 필요
# 3. [  ] terraform plan 실행 후 변경사항 리뷰
# 4. [  ] terraform apply 실행
# 5. [  ] 배포 후 테스트: curl -I https://admin.set-of.com/api/v1/...
# ──────────────────────────────────────────────────────────────────────
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
  # NOTE: HTTP 사용 - CloudFront ↔ ALB 내부 통신
  # ALB 인증서가 *.set-of.com이라 ALB DNS로 HTTPS 연결 시 도메인 불일치 발생
  origin {
    domain_name = data.aws_lb.gateway.dns_name
    origin_id   = "gateway-alb"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "http-only"  # HTTPS가 아닌 HTTP 사용 (stage와 동일)
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

# TODO: 배포 전 Route53에 기존 레코드가 있는지 확인하고, 있으면 아래 import 블록 추가
# import {
#   to = aws_route53_record.admin
#   id = "Z104656329CL6XBYE8OIJ_admin.set-of.com_A"
# }

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
