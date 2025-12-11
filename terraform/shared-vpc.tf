# ============================================================================
# Shared Infrastructure Reference: VPC
# ============================================================================
# References centrally managed VPC from Infrastructure repository
# Read-only access via SSM Parameter Store
# ============================================================================

# VPC ID
data "aws_ssm_parameter" "vpc_id" {
  name = "/shared/network/vpc-id"
}

# Subnet IDs
data "aws_ssm_parameter" "public_subnet_ids" {
  name = "/shared/network/public-subnets"
}

data "aws_ssm_parameter" "private_subnet_ids" {
  name = "/shared/network/private-subnets"
}

# ============================================================================
# Local Variables
# ============================================================================

locals {
  vpc_id          = data.aws_ssm_parameter.vpc_id.value
  public_subnets  = split(",", data.aws_ssm_parameter.public_subnet_ids.value)
  private_subnets = split(",", data.aws_ssm_parameter.private_subnet_ids.value)
}
