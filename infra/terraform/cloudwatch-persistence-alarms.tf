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
