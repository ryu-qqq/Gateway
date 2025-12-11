# ============================================================================
# Shared Infrastructure Reference: ACM Certificate
# ============================================================================
# References centrally managed ACM certificate from Infrastructure repository
# Used for ALB HTTPS termination
# ============================================================================

# ACM Certificate ARN
data "aws_ssm_parameter" "acm_certificate_arn" {
  name = "/shared/network/certificate-arn"
}

# ============================================================================
# Local Variables
# ============================================================================

locals {
  certificate_arn = data.aws_ssm_parameter.acm_certificate_arn.value
}
