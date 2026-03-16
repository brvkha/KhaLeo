terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

output "deploy_artifact_bucket" {
  description = "S3 bucket used for immutable backend artifacts"
  value       = var.deploy_artifact_bucket
}

output "deploy_target_tag" {
  description = "EC2 tag selector used by deployment workflow"
  value = {
    key   = var.deploy_target_tag_key
    value = var.deploy_target_tag_value
  }
}
