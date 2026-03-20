resource "aws_cloudwatch_metric_alarm" "persistence_error_rate_high" {
  alarm_name          = "khaleo-persistence-error-rate-high"
  alarm_description   = "High persistence error rate in backend logs"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  threshold           = 5
  metric_name         = "PersistenceWriteErrors"
  namespace           = "KhaLeo/Backend"
  period              = 60
  statistic           = "Sum"
  treat_missing_data  = "notBreaching"

  dimensions = {
    Service = "flashcard-backend"
  }

  tags = var.common_tags
}

resource "aws_cloudwatch_metric_alarm" "async_retry_or_dead_letter_high" {
  alarm_name          = "khaleo-async-retry-dead-letter-high"
  alarm_description   = "High retry or dead-letter volume for async activity logs"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  threshold           = 3
  metric_name         = "ActivityLogRetryOrDeadLetter"
  namespace           = "KhaLeo/Backend"
  period              = 60
  statistic           = "Sum"
  treat_missing_data  = "notBreaching"

  dimensions = {
    Service = "flashcard-backend"
  }

  tags = var.common_tags
}

resource "aws_cloudwatch_metric_alarm" "deck_media_operation_failure_high" {
  alarm_name          = "khaleo-deck-media-operation-failure-high"
  alarm_description   = "Spike in deck/card/media operation failures"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  threshold           = 3
  metric_name         = "DeckMediaOperationFailure"
  namespace           = "KhaLeo/Backend"
  period              = 60
  statistic           = "Sum"
  treat_missing_data  = "notBreaching"

  dimensions = {
    Service = "flashcard-backend"
  }

  tags = var.common_tags
}

resource "aws_cloudwatch_metric_alarm" "media_cleanup_failure_high" {
  alarm_name          = "khaleo-media-cleanup-failure-high"
  alarm_description   = "Media cleanup failures detected"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  threshold           = 1
  metric_name         = "MediaCleanupFailure"
  namespace           = "KhaLeo/Backend"
  period              = 300
  statistic           = "Sum"
  treat_missing_data  = "notBreaching"

  dimensions = {
    Service = "flashcard-backend"
  }

  tags = var.common_tags
}

resource "aws_cloudwatch_metric_alarm" "deployment_command_failure_high" {
  alarm_name          = "khaleo-deployment-command-failure-high"
  alarm_description   = "Deployment target command failures detected"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  threshold           = var.deployment_command_failure_alarm_threshold
  metric_name         = "DeploymentCommandFailure"
  namespace           = "KhaLeo/Backend"
  period              = 300
  statistic           = "Sum"
  treat_missing_data  = "notBreaching"

  dimensions = {
    Service = "flashcard-backend"
  }

  tags = var.common_tags
}

resource "aws_cloudwatch_metric_alarm" "public_discovery_error_rate_high" {
  alarm_name          = "khaleo-public-discovery-error-rate-high"
  alarm_description   = "High error rate in public discovery path"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  threshold           = 3
  metric_name         = "PublicDiscoveryErrors"
  namespace           = "KhaLeo/Backend"
  period              = 60
  statistic           = "Sum"
  treat_missing_data  = "notBreaching"

  dimensions = {
    Service = "flashcard-backend"
  }

  tags = var.common_tags
}

resource "aws_cloudwatch_metric_alarm" "public_import_error_rate_high" {
  alarm_name          = "khaleo-public-import-error-rate-high"
  alarm_description   = "High error rate in public import path"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  threshold           = 3
  metric_name         = "PublicImportErrors"
  namespace           = "KhaLeo/Backend"
  period              = 60
  statistic           = "Sum"
  treat_missing_data  = "notBreaching"

  dimensions = {
    Service = "flashcard-backend"
  }

  tags = var.common_tags
}

resource "aws_cloudwatch_metric_alarm" "reimport_merge_error_rate_high" {
  alarm_name          = "khaleo-reimport-merge-error-rate-high"
  alarm_description   = "High error rate in re-import merge path"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  threshold           = 3
  metric_name         = "ReimportMergeErrors"
  namespace           = "KhaLeo/Backend"
  period              = 60
  statistic           = "Sum"
  treat_missing_data  = "notBreaching"

  dimensions = {
    Service = "flashcard-backend"
  }

  tags = var.common_tags
}

resource "aws_cloudwatch_metric_alarm" "study_rating_error_rate_high" {
  alarm_name          = "khaleo-study-rating-error-rate-high"
  alarm_description   = "High error rate in study session rating path"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  threshold           = 5
  metric_name         = "StudyRatingErrors"
  namespace           = "KhaLeo/Backend"
  period              = 60
  statistic           = "Sum"
  treat_missing_data  = "notBreaching"

  dimensions = {
    Service = "flashcard-backend"
  }

  tags = var.common_tags
}
