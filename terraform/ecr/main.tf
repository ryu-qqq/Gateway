# ========================================
# ECR Repository for Gateway
# ========================================
# Container registry using Infrastructure module
# - gateway: API Gateway server
# ========================================

# ========================================
# Common Tags (for governance)
# ========================================
locals {
  common_tags = {
    environment  = var.environment
    service_name = "${var.project_name}-ecr"
    team         = "platform-team"
    owner        = "platform@ryuqqq.com"
    cost_center  = "engineering"
    project      = var.project_name
    data_class   = "internal"
  }
}

# ========================================
# ECR Repository: gateway
# ========================================
module "ecr_gateway" {
  source = "git::https://github.com/ryu-qqq/Infrastructure.git//terraform/modules/ecr?ref=main"

  name                 = "${var.project_name}-${var.environment}"
  image_tag_mutability = "IMMUTABLE"
  scan_on_push         = true
  encryption_type      = "AES256"  # 기존 ECR과 동일하게 AES256 사용

  # Lifecycle Policy
  enable_lifecycle_policy    = true
  max_image_count            = 30
  lifecycle_tag_prefixes     = ["v", "prod", "latest"]
  untagged_image_expiry_days = 7

  # SSM Parameter for cross-stack reference
  create_ssm_parameter = true

  # Required Tags (governance compliance)
  environment  = local.common_tags.environment
  service_name = "${var.project_name}"
  team         = local.common_tags.team
  owner        = local.common_tags.owner
  cost_center  = local.common_tags.cost_center
  project      = local.common_tags.project
  data_class   = local.common_tags.data_class
}
