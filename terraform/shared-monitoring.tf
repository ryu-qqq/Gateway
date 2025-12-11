# ============================================================================
# Shared Infrastructure Reference: Monitoring (AMP)
# ============================================================================
# References centrally managed Amazon Managed Prometheus
# Used for metrics collection via ADOT
# ============================================================================

# AMP Workspace
data "aws_ssm_parameter" "amp_workspace_arn" {
  name = "/shared/monitoring/amp-workspace-arn"
}

data "aws_ssm_parameter" "amp_remote_write_url" {
  name = "/shared/monitoring/amp-remote-write-url"
}

# ============================================================================
# Local Variables
# ============================================================================

locals {
  amp_workspace_arn    = data.aws_ssm_parameter.amp_workspace_arn.value
  amp_remote_write_url = data.aws_ssm_parameter.amp_remote_write_url.value
}
