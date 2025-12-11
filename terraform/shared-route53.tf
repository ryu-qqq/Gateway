# ============================================================================
# Shared Infrastructure Reference: Route53
# ============================================================================
# References centrally managed Route53 hosted zone from Infrastructure repository
# Used for DNS record creation
# ============================================================================

# Route53 Hosted Zone
data "aws_ssm_parameter" "hosted_zone_id" {
  name = "/shared/network/route53-zone-id"
}

# ============================================================================
# Local Variables
# ============================================================================

locals {
  hosted_zone_id = data.aws_ssm_parameter.hosted_zone_id.value
  domain_name    = "set-of.com"
}
