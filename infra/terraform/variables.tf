variable "aws_region" {
  description = "AWS region for Kha Leo infrastructure"
  type        = string
  default     = "ap-southeast-1"
}

variable "study_activity_log_table_name" {
  description = "DynamoDB table name for study activity logs"
  type        = string
  default     = "StudyActivityLog"
}

variable "study_activity_log_user_index_name" {
  description = "Global secondary index name for study activity lookups by user"
  type        = string
  default     = "userId-timestamp-index"
}

variable "study_activity_log_rate_alarm_threshold" {
  description = "Threshold for high study activity log failures per minute"
  type        = number
  default     = 5
}

variable "aurora_cluster_identifier" {
  description = "Aurora MySQL cluster identifier"
  type        = string
  default     = "khaleo-aurora-cluster"
}

variable "admin_authorization_denials_alarm_threshold" {
  description = "Threshold for admin authorization denial alarms over 5 minutes"
  type        = number
  default     = 5
}

variable "deployment_command_failure_alarm_threshold" {
  description = "Threshold for deployment command failures over 5 minutes"
  type        = number
  default     = 1
}

variable "deploy_artifact_bucket" {
  description = "S3 bucket storing immutable backend deployment artifacts"
  type        = string
  default     = "kha-leo-build-artifacts"
}

variable "deploy_target_tag_key" {
  description = "EC2 tag key used to target deployment instances via SSM"
  type        = string
  default     = "Role"
}

variable "deploy_target_tag_value" {
  description = "EC2 tag value used to target deployment instances via SSM"
  type        = string
  default     = "khaleo-backend"
}

variable "common_tags" {
  description = "Common tags applied to infrastructure resources"
  type        = map(string)
  default = {
    Project     = "KhaLeoFlashcard"
    ManagedBy   = "Terraform"
    Environment = "dev"
  }
}
