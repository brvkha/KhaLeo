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
    name            = var.study_activity_log_user_index_name
    hash_key        = "userId"
    range_key       = "timestamp"
    projection_type = "ALL"
  }

  tags = var.common_tags
}
