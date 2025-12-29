# ========================================
# CloudFront Distribution for API Gateway Routing
# ========================================
# Path-based routing:
#   /api/v1/* → Gateway ALB → Legacy API
#   /*        → Frontend ALB → Next.js
# ========================================

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

# ========================================
# Origin Request Policies
# ========================================

# API Origin Request Policy - Forward all necessary headers
resource "aws_cloudfront_origin_request_policy" "api_all_viewer" {
  name    = "${var.project_name}-api-all-viewer-${var.environment}"
  comment = "Forward all viewer headers for API requests"

  cookies_config {
    cookie_behavior = "all"
  }
  headers_config {
    header_behavior = "allViewerAndWhitelistCloudFront"
    headers {
      items = ["CloudFront-Forwarded-Proto", "CloudFront-Is-Mobile-Viewer"]
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
  origin {
    domain_name = data.aws_lb.gateway.dns_name
    origin_id   = "gateway-alb"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }

    custom_header {
      name  = "X-Forwarded-Host"
      value = "set-of.com"
    }
  }

  # ========================================
  # Default Cache Behavior → Frontend
  # ========================================
  default_cache_behavior {
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "frontend-alb"

    # Use managed cache policy for frontend
    cache_policy_id          = "658327ea-f89d-4fab-a63d-7e88639e58f6" # CachingOptimized
    origin_request_policy_id = "216adef6-5c7f-47e4-b989-5492eafa07d3" # AllViewer

    viewer_protocol_policy = "redirect-to-https"
    compress               = true
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
  origin {
    domain_name = data.aws_lb.gateway.dns_name
    origin_id   = "gateway-alb"

    custom_origin_config {
      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]
    }

    custom_header {
      name  = "X-Forwarded-Host"
      value = "stage.set-of.com"
    }
  }

  # ========================================
  # Default Cache Behavior → Frontend
  # ========================================
  default_cache_behavior {
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "frontend-alb"

    cache_policy_id          = "658327ea-f89d-4fab-a63d-7e88639e58f6" # CachingOptimized
    origin_request_policy_id = "216adef6-5c7f-47e4-b989-5492eafa07d3" # AllViewer

    viewer_protocol_policy = "redirect-to-https"
    compress               = true
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
# Future: admin.set-of.com (uncomment when ready)
# ========================================
# resource "aws_cloudfront_distribution" "admin" {
#   enabled             = true
#   is_ipv6_enabled     = true
#   comment             = "admin.set-of.com - Admin API Gateway routing"
#   default_root_object = ""
#   price_class         = "PriceClass_200"
#   aliases             = ["admin.set-of.com"]
#
#   origin {
#     domain_name = data.aws_lb.gateway.dns_name
#     origin_id   = "gateway-alb"
#
#     custom_origin_config {
#       http_port              = 80
#       https_port             = 443
#       origin_protocol_policy = "https-only"
#       origin_ssl_protocols   = ["TLSv1.2"]
#     }
#
#     custom_header {
#       name  = "X-Forwarded-Host"
#       value = "admin.set-of.com"
#     }
#   }
#
#   default_cache_behavior {
#     allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
#     cached_methods   = ["GET", "HEAD"]
#     target_origin_id = "gateway-alb"
#
#     cache_policy_id            = aws_cloudfront_cache_policy.api_no_cache.id
#     origin_request_policy_id   = aws_cloudfront_origin_request_policy.api_all_viewer.id
#     response_headers_policy_id = aws_cloudfront_response_headers_policy.api_cors.id
#
#     viewer_protocol_policy = "https-only"
#     compress               = false
#   }
#
#   viewer_certificate {
#     acm_certificate_arn      = data.aws_acm_certificate.cloudfront_cert.arn
#     ssl_support_method       = "sni-only"
#     minimum_protocol_version = "TLSv1.2_2021"
#   }
#
#   restrictions {
#     geo_restriction {
#       restriction_type = "none"
#     }
#   }
#
#   tags = merge(local.common_tags, {
#     Name = "${var.project_name}-cloudfront-admin"
#   })
# }
#
# resource "aws_route53_record" "admin" {
#   zone_id = local.route53_zone_id
#   name    = "admin.set-of.com"
#   type    = "A"
#
#   alias {
#     name                   = aws_cloudfront_distribution.admin.domain_name
#     zone_id                = aws_cloudfront_distribution.admin.hosted_zone_id
#     evaluate_target_health = false
#   }
# }
