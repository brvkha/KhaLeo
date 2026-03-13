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

# Placeholder resources for foundational persistence infra.
# Concrete Aurora and DynamoDB resources will be expanded in later tasks.
resource "aws_dynamodb_table" "study_activity_log" {
  name         = var.study_activity_log_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "logId"
  range_key    = "timestamp"

  attribute {
    name = "logId"
    type = "S"
  }

  attribute {
    name = "timestamp"
    type = "S"
  }

  attribute {
    name = "userId"
    type = "S"
  }

  global_secondary_index {
    name            = "userId-timestamp-index"
    hash_key        = "userId"
    range_key       = "timestamp"
    projection_type = "ALL"
  }

  tags = var.common_tags
}
