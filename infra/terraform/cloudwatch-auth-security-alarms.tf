resource "aws_cloudwatch_metric_alarm" "auth_failed_login_spike" {
  alarm_name          = "khaleo-auth-failed-login-spike"
  alarm_description   = "Spike in failed login outcomes indicating brute-force risk"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  threshold           = 10
  metric_name         = "AuthFailedLogin"
  namespace           = "KhaLeo/Backend"
  period              = 60
  statistic           = "Sum"
  treat_missing_data  = "notBreaching"

  dimensions = {
    Service = "flashcard-backend"
  }

  tags = var.common_tags
}

resource "aws_cloudwatch_metric_alarm" "auth_lockout_triggered" {
  alarm_name          = "khaleo-auth-lockout-triggered"
  alarm_description   = "Account lockout events detected"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  threshold           = 1
  metric_name         = "AuthAccountLocked"
  namespace           = "KhaLeo/Backend"
  period              = 300
  statistic           = "Sum"
  treat_missing_data  = "notBreaching"

  dimensions = {
    Service = "flashcard-backend"
  }

  tags = var.common_tags
}

resource "aws_cloudwatch_metric_alarm" "auth_refresh_rejection_high" {
  alarm_name          = "khaleo-auth-refresh-rejection-high"
  alarm_description   = "High rate of refresh-token rejection events"
  comparison_operator = "GreaterThanOrEqualToThreshold"
  evaluation_periods  = 1
  threshold           = 5
  metric_name         = "AuthRefreshRejected"
  namespace           = "KhaLeo/Backend"
  period              = 300
  statistic           = "Sum"
  treat_missing_data  = "notBreaching"

  dimensions = {
    Service = "flashcard-backend"
  }

  tags = var.common_tags
}