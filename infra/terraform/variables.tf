variable "aws_region" {
  description = "AWS region for resources"
  type        = string
  default     = "ap-southeast-1"
}

variable "github_owner" {
  description = "GitHub org or user that owns the repository"
  type        = string
}

variable "github_repository" {
  description = "Repository name (owner/NAME is handled by provider.owner + this)"
  type        = string
}

variable "github_environment_name" {
  description = "GitHub Environment name used by deploy workflows"
  type        = string
  default     = "production"
}

variable "artifact_bucket" {
  description = "S3 bucket name for backend artifacts"
  type        = string
}

variable "frontend_bucket" {
  description = "S3 bucket name for frontend static site"
  type        = string
}

variable "cloudfront_distribution_id" {
  description = "CloudFront distribution ID used by frontend deploy invalidation"
  type        = string
  default     = "TO_BE_SET"
}

variable "deploy_target_tag_key" {
  description = "EC2 tag key used to find deployment targets"
  type        = string
  default     = "Role"
}

variable "deploy_target_tag_value" {
  description = "EC2 tag value used to find deployment targets"
  type        = string
  default     = "khaleo-backend"
}

variable "deploy_service_name" {
  description = "Systemd service name on EC2 instances"
  type        = string
  default     = "flashcard-backend"
}

variable "backend_base_url" {
  description = "Public backend base URL exposed to frontend clients"
  type        = string
  default     = "https://api.khaleoshop.click"
}

variable "app_env" {
  description = "Application environment label consumed by workflows"
  type        = string
  default     = "production"
}

variable "java_version" {
  description = "Java version used by CI and deploy workflows"
  type        = string
  default     = "17"
}

variable "node_version" {
  description = "Node.js version used by CI and deploy workflows"
  type        = string
  default     = "20"
}

variable "db_secret_name" {
  description = "Secrets Manager secret name for database runtime credentials"
  type        = string
  default     = "khaleo/prod/db-credentials"
}

variable "jwt_secret_name" {
  description = "Secrets Manager secret name for JWT signing secret"
  type        = string
  default     = "khaleo/prod/jwt-secret"
}

variable "ses_secret_name" {
  description = "Secrets Manager secret name for SES runtime credentials"
  type        = string
  default     = "khaleo/prod/ses-credentials"
}

variable "runtime_env_path" {
  description = "Path to the generated runtime properties file consumed by Spring Boot"
  type        = string
  default     = "/opt/khaleo/flashcard-backend/runtime-secrets.env"
}

variable "placeholder_secret_value" {
  description = "Placeholder value written to GitHub secrets; replace with real values after apply"
  type        = string
  default     = "TO_BE_SET"
}

variable "github_manual_environment_secrets" {
  description = "Additional GitHub environment secret names to create as placeholders"
  type        = list(string)
  default = [
    "SPRING_DATASOURCE_URL",
    "SPRING_DATASOURCE_USERNAME",
    "SPRING_DATASOURCE_PASSWORD",
    "JWT_SECRET",
    "SES_ACCESS_KEY_ID",
    "SES_SECRET_ACCESS_KEY"
  ]
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

variable "aurora_database_name" {
  description = "Default database name created in Aurora MySQL cluster"
  type        = string
  default     = "khaleo_flashcard"
}

variable "aurora_master_username" {
  description = "Aurora MySQL master username"
  type        = string
  default     = "khaleo_admin"
}

variable "aurora_engine_version" {
  description = "Aurora MySQL engine version"
  type        = string
  default     = "8.0.mysql_aurora.3.08.0"
}

variable "aurora_instance_class" {
  description = "Aurora instance class for writer/readers"
  type        = string
  default     = "db.t4g.medium"
}

variable "aurora_instance_count" {
  description = "Number of Aurora instances to create in the cluster"
  type        = number
  default     = 1
}

variable "aurora_backup_retention_period" {
  description = "Aurora automated backup retention in days"
  type        = number
  default     = 7
}

variable "aurora_preferred_backup_window" {
  description = "Preferred backup window for Aurora cluster"
  type        = string
  default     = "17:00-18:00"
}

variable "aurora_preferred_maintenance_window" {
  description = "Preferred maintenance window for Aurora cluster"
  type        = string
  default     = "sun:18:00-sun:19:00"
}

variable "aurora_deletion_protection" {
  description = "Enable deletion protection for Aurora cluster"
  type        = bool
  default     = false
}

variable "aurora_skip_final_snapshot" {
  description = "Skip final snapshot when destroying Aurora cluster"
  type        = bool
  default     = true
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

variable "backend_http_5xx_alarm_threshold" {
  description = "Threshold for backend HTTP 5xx alarms over 5 minutes"
  type        = number
  default     = 5
}

variable "deploy_artifact_bucket" {
  description = "S3 bucket storing immutable backend deployment artifacts"
  type        = string
  default     = "kha-leo-build-artifacts"
}

variable "root_domain_name" {
  description = "Primary frontend domain"
  type        = string
  default     = "khaleoshop.click"
}

variable "api_subdomain" {
  description = "API subdomain prefix"
  type        = string
  default     = "api"
}

variable "route53_zone_id" {
  description = "Route53 hosted zone ID for khaleoshop.click"
  type        = string
  default     = ""
}

variable "availability_zones" {
  description = "Availability zones for multi-AZ deployment"
  type        = list(string)
  default     = ["ap-southeast-1a", "ap-southeast-1b", "ap-southeast-1c"]
}

variable "vpc_cidr_block" {
  description = "CIDR block for the VPC"
  type        = string
  default     = "10.42.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public ALB/NAT subnets"
  type        = list(string)
  default     = ["10.42.0.0/24", "10.42.1.0/24", "10.42.2.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private backend subnets"
  type        = list(string)
  default     = ["10.42.10.0/24", "10.42.11.0/24", "10.42.12.0/24"]
}

variable "backend_instance_type" {
  description = "EC2 instance type for backend ASG"
  type        = string
  default     = "t3.micro"
}

variable "backend_ami_id" {
  description = "AMI ID for backend launch template"
  type        = string
  default     = "ami-0c02fb55956c7d316"
}

variable "backend_desired_capacity" {
  description = "Desired instance count in backend ASG"
  type        = number
  default     = 1
}

variable "backend_min_size" {
  description = "Minimum backend ASG size"
  type        = number
  default     = 1
}

variable "backend_max_size" {
  description = "Maximum backend ASG size"
  type        = number
  default     = 4
}

variable "backend_health_check_path" {
  description = "ALB target group health check path"
  type        = string
  default     = "/actuator/health"
}

variable "alb_certificate_arn" {
  description = "ACM certificate ARN for backend HTTPS listener"
  type        = string
  default     = ""
}

variable "cloudfront_acm_certificate_arn" {
  description = "ACM certificate ARN in us-east-1 for CloudFront custom domain"
  type        = string
  default     = ""
}

variable "waf_rate_limit" {
  description = "Rate limit for WAF rate-based rule"
  type        = number
  default     = 2000
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
