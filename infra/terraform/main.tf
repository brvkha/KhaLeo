locals {
  repo            = var.github_repository
  repo_full_name  = "${var.github_owner}/${var.github_repository}"
  api_domain_name = "${var.api_subdomain}.${var.root_domain_name}"

  public_subnet_map = {
    for idx, az in var.availability_zones : az => var.public_subnet_cidrs[idx]
  }

  private_subnet_map = {
    for idx, az in var.availability_zones : az => var.private_subnet_cidrs[idx]
  }

  # Secrets required by current GitHub Actions deploy workflows.
  deployment_environment_secrets = {
    AWS_ROLE_TO_ASSUME         = aws_iam_role.github_actions_deploy.arn
    AWS_REGION                 = var.aws_region
    ARTIFACT_BUCKET            = var.artifact_bucket
    FRONTEND_S3_BUCKET         = var.frontend_bucket
    CLOUDFRONT_DISTRIBUTION_ID = aws_cloudfront_distribution.frontend.id
    DEPLOY_TARGET_TAG_KEY      = var.deploy_target_tag_key
    DEPLOY_TARGET_TAG_VALUE    = var.deploy_target_tag_value
    DEPLOY_SERVICE_NAME        = var.deploy_service_name
  }

  deployment_environment_variables = {
    BACKEND_BASE_URL = var.backend_base_url
    APP_ENV          = var.app_env
    JAVA_VERSION     = var.java_version
    NODE_VERSION     = var.node_version
    DB_SECRET_ID     = aws_secretsmanager_secret.db_credentials.name
    JWT_SECRET_ID    = aws_secretsmanager_secret.jwt_secret.name
    SES_SECRET_ID    = aws_secretsmanager_secret.ses_credentials.name
    RUNTIME_ENV_PATH = var.runtime_env_path
  }
}

data "aws_caller_identity" "current" {}

resource "aws_s3_bucket" "artifact_bucket" {
  bucket = var.artifact_bucket
}

resource "aws_s3_bucket_public_access_block" "artifact_block" {
  bucket = aws_s3_bucket.artifact_bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_versioning" "artifact_bucket_versioning" {
  bucket = aws_s3_bucket.artifact_bucket.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket" "frontend_bucket" {
  bucket = var.frontend_bucket
}

resource "aws_s3_bucket_public_access_block" "frontend_block" {
  bucket = aws_s3_bucket.frontend_bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_versioning" "frontend_bucket_versioning" {
  bucket = aws_s3_bucket.frontend_bucket.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr_block
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = merge(var.common_tags, {
    Name = "khaleo-vpc"
  })
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = merge(var.common_tags, {
    Name = "khaleo-igw"
  })
}

resource "aws_subnet" "public" {
  for_each = local.public_subnet_map

  vpc_id                  = aws_vpc.main.id
  cidr_block              = each.value
  availability_zone       = each.key
  map_public_ip_on_launch = true

  tags = merge(var.common_tags, {
    Name = "khaleo-public-${each.key}"
    Tier = "public"
  })
}

resource "aws_subnet" "private" {
  for_each = local.private_subnet_map

  vpc_id                  = aws_vpc.main.id
  cidr_block              = each.value
  availability_zone       = each.key
  map_public_ip_on_launch = false

  tags = merge(var.common_tags, {
    Name = "khaleo-private-${each.key}"
    Tier = "private"
  })
}

resource "aws_eip" "nat" {
  domain = "vpc"

  depends_on = [aws_internet_gateway.main]
}

resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = values(aws_subnet.public)[0].id

  tags = merge(var.common_tags, {
    Name = "khaleo-nat"
  })
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = merge(var.common_tags, {
    Name = "khaleo-public-rt"
  })
}

resource "aws_route_table_association" "public" {
  for_each = aws_subnet.public

  subnet_id      = each.value.id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main.id
  }

  tags = merge(var.common_tags, {
    Name = "khaleo-private-rt"
  })
}

resource "aws_route_table_association" "private" {
  for_each = aws_subnet.private

  subnet_id      = each.value.id
  route_table_id = aws_route_table.private.id
}

resource "aws_security_group" "alb" {
  name        = "khaleo-alb-sg"
  description = "Security group for public ALB"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "HTTPS from internet"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP from internet"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = var.common_tags
}

resource "aws_security_group" "backend" {
  name        = "khaleo-backend-sg"
  description = "Security group for backend EC2 instances"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "Backend traffic from ALB"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = var.common_tags
}

resource "aws_lb" "backend" {
  name               = "khaleo-backend-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = [for subnet in aws_subnet.public : subnet.id]

  tags = var.common_tags
}

resource "aws_lb_target_group" "backend" {
  name        = "khaleo-backend-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "instance"

  health_check {
    path                = var.backend_health_check_path
    protocol            = "HTTP"
    matcher             = "200-399"
    healthy_threshold   = 2
    unhealthy_threshold = 2
    timeout             = 5
    interval            = 30
  }

  tags = var.common_tags
}

resource "aws_lb_listener" "backend_https" {
  count = var.alb_certificate_arn != "" ? 1 : 0

  load_balancer_arn = aws_lb.backend.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = var.alb_certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }
}

resource "aws_lb_listener" "backend_http" {
  count = var.alb_certificate_arn == "" ? 1 : 0

  load_balancer_arn = aws_lb.backend.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.backend.arn
  }
}

resource "aws_launch_template" "backend" {
  name_prefix   = "khaleo-backend-"
  image_id      = var.backend_ami_id
  instance_type = var.backend_instance_type

  vpc_security_group_ids = [aws_security_group.backend.id]

  iam_instance_profile {
    name = aws_iam_instance_profile.backend_runtime.name
  }

  metadata_options {
    http_endpoint = "enabled"
    http_tokens   = "required"
  }

  user_data = base64encode(<<-EOT
    #!/bin/bash
    set -euxo pipefail
    dnf update -y
    dnf install -y java-17-amazon-corretto awscli
    mkdir -p /opt/khaleo/flashcard-backend
    
    # Tạo sẵn file service ngay khi EC2 boot lên
    cat <<EOF > /etc/systemd/system/flashcard-backend.service
    [Unit]
    Description=KhaLeo Backend
    After=network.target

    [Service]
    Type=simple
    WorkingDirectory=/opt/khaleo/flashcard-backend
    EnvironmentFile=-/opt/khaleo/flashcard-backend/runtime-secrets.env
    ExecStart=/usr/bin/java -jar /opt/khaleo/flashcard-backend/current.jar
    Restart=always
    RestartSec=5
    SuccessExitStatus=143

    [Install]
    WantedBy=multi-user.target
EOF
    systemctl daemon-reload
    systemctl enable flashcard-backend
  EOT
  )

  tag_specifications {
    resource_type = "instance"

    tags = merge(var.common_tags, {
      Name                           = "khaleo-backend"
      "${var.deploy_target_tag_key}" = var.deploy_target_tag_value
    })
  }

  tags = var.common_tags
}

resource "aws_autoscaling_group" "backend" {
  name                      = "khaleo-backend-asg"
  min_size                  = var.backend_min_size
  max_size                  = var.backend_max_size
  desired_capacity          = var.backend_desired_capacity
  health_check_type         = "ELB"
  health_check_grace_period = 300
  vpc_zone_identifier       = [for subnet in aws_subnet.private : subnet.id]
  target_group_arns         = [aws_lb_target_group.backend.arn]

  launch_template {
    id      = aws_launch_template.backend.id
    version = "$Latest"
  }

  tag {
    key                 = "Name"
    value               = "khaleo-backend"
    propagate_at_launch = true
  }

  tag {
    key                 = var.deploy_target_tag_key
    value               = var.deploy_target_tag_value
    propagate_at_launch = true
  }

  dynamic "tag" {
    for_each = var.common_tags
    content {
      key                 = tag.key
      value               = tag.value
      propagate_at_launch = true
    }
  }
}

resource "aws_wafv2_web_acl" "backend" {
  name  = "khaleo-backend-waf"
  scope = "REGIONAL"

  default_action {
    allow {}
  }

  rule {
    name     = "rate-limit"
    priority = 1

    action {
      block {}
    }

    statement {
      rate_based_statement {
        aggregate_key_type = "IP"
        limit              = var.waf_rate_limit
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "khaleoRateLimit"
      sampled_requests_enabled   = true
    }
  }

  visibility_config {
    cloudwatch_metrics_enabled = true
    metric_name                = "khaleoBackendWaf"
    sampled_requests_enabled   = true
  }

  tags = var.common_tags
}

resource "aws_wafv2_web_acl_association" "backend" {
  resource_arn = aws_lb.backend.arn
  web_acl_arn  = aws_wafv2_web_acl.backend.arn
}

resource "aws_cloudfront_origin_access_control" "frontend" {
  name                              = "khaleo-frontend-oac"
  description                       = "Origin access control for frontend bucket"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_distribution" "frontend" {
  enabled             = true
  is_ipv6_enabled     = true
  default_root_object = "index.html"
  aliases             = var.cloudfront_acm_certificate_arn != "" ? [var.root_domain_name] : []

  origin {
    domain_name              = aws_s3_bucket.frontend_bucket.bucket_regional_domain_name
    origin_id                = "frontend-s3-origin"
    origin_access_control_id = aws_cloudfront_origin_access_control.frontend.id
  }

  default_cache_behavior {
    allowed_methods        = ["GET", "HEAD", "OPTIONS"]
    cached_methods         = ["GET", "HEAD"]
    target_origin_id       = "frontend-s3-origin"
    viewer_protocol_policy = "redirect-to-https"
    compress               = true

    forwarded_values {
      query_string = false
      cookies {
        forward = "none"
      }
    }
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = var.cloudfront_acm_certificate_arn == ""
    acm_certificate_arn            = var.cloudfront_acm_certificate_arn != "" ? var.cloudfront_acm_certificate_arn : null
    ssl_support_method             = var.cloudfront_acm_certificate_arn != "" ? "sni-only" : null
    minimum_protocol_version       = var.cloudfront_acm_certificate_arn != "" ? "TLSv1.2_2021" : "TLSv1"
  }

  tags = var.common_tags
}

data "aws_iam_policy_document" "frontend_bucket_policy" {
  statement {
    sid    = "AllowCloudFrontRead"
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    actions = ["s3:GetObject"]

    resources = [
      "${aws_s3_bucket.frontend_bucket.arn}/*"
    ]

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.frontend.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "frontend" {
  bucket = aws_s3_bucket.frontend_bucket.id
  policy = data.aws_iam_policy_document.frontend_bucket_policy.json
}

resource "aws_route53_record" "frontend_alias" {
  count = var.route53_zone_id != "" ? 1 : 0

  zone_id = var.route53_zone_id
  name    = var.root_domain_name
  type    = "A"

  alias {
    name                   = aws_cloudfront_distribution.frontend.domain_name
    zone_id                = aws_cloudfront_distribution.frontend.hosted_zone_id
    evaluate_target_health = false
  }
}

resource "aws_route53_record" "api_alias" {
  count = var.route53_zone_id != "" ? 1 : 0

  zone_id = var.route53_zone_id
  name    = local.api_domain_name
  type    = "A"

  alias {
    name                   = aws_lb.backend.dns_name
    zone_id                = aws_lb.backend.zone_id
    evaluate_target_health = true
  }
}

data "aws_iam_policy_document" "github_actions_oidc_assume_role" {
  statement {
    sid     = "GitHubActionsAssumeRole"
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github_actions.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${local.repo_full_name}:environment:${var.github_environment_name}"]
    }
  }
}

data "aws_iam_policy_document" "backend_instance_assume_role" {
  statement {
    sid     = "Ec2AssumeRole"
    effect  = "Allow"
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_openid_connect_provider" "github_actions" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

resource "aws_iam_role" "github_actions_deploy" {
  name               = "github-actions-khaleo-prod-role"
  assume_role_policy = data.aws_iam_policy_document.github_actions_oidc_assume_role.json
}

resource "aws_iam_role" "backend_runtime" {
  name               = "khaleo-backend-runtime-role"
  assume_role_policy = data.aws_iam_policy_document.backend_instance_assume_role.json
}

resource "aws_iam_policy" "github_actions_deploy" {
  name = "GitHubActionsKhaLeoDeployPolicy"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "S3ArtifactBucketReadWrite"
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:AbortMultipartUpload",
          "s3:ListBucket"
        ]
        Resource = [
          "arn:aws:s3:::${var.artifact_bucket}",
          "arn:aws:s3:::${var.artifact_bucket}/*"
        ]
      },
      {
        Sid    = "S3FrontendBucketDeploy"
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:DeleteObject",
          "s3:AbortMultipartUpload",
          "s3:ListBucket"
        ]
        Resource = [
          "arn:aws:s3:::${var.frontend_bucket}",
          "arn:aws:s3:::${var.frontend_bucket}/*"
        ]
      },
      {
        Sid    = "CloudFrontInvalidate"
        Effect = "Allow"
        Action = [
          "cloudfront:CreateInvalidation",
          "cloudfront:GetInvalidation"
        ]
        Resource = "arn:aws:cloudfront::${data.aws_caller_identity.current.account_id}:distribution/${aws_cloudfront_distribution.frontend.id}"
      },
      {
        Sid    = "SsmDeployCommands"
        Effect = "Allow"
        Action = [
          "ssm:SendCommand",
          "ssm:ListCommandInvocations",
          "ssm:GetCommandInvocation",
          "ssm:ListCommands"
        ]
        Resource = [
          "arn:aws:ssm:${var.aws_region}:*:document/AWS-RunShellScript",
          "arn:aws:ec2:${var.aws_region}:*:instance/*",
          "arn:aws:ssm:${var.aws_region}:*:command/*"
        ]
      },
      {
        Sid    = "Ec2DescribeForTargetResolution"
        Effect = "Allow"
        Action = [
          "ec2:DescribeInstances",
          "ec2:DescribeTags",
          "elasticloadbalancing:DescribeTargetHealth"
        ]
        Resource = "*"
      }
    ]
  })
}

resource "aws_iam_policy" "backend_runtime" {
  name = "KhaLeoBackendRuntimePolicy"
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "ReadRuntimeSecrets"
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue"
        ]
        Resource = [
          aws_secretsmanager_secret.db_credentials.arn,
          aws_secretsmanager_secret.jwt_secret.arn,
          aws_secretsmanager_secret.ses_credentials.arn
        ]
      },
      {
        Sid    = "ReadBackendArtifacts"
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.artifact_bucket.arn,
          "${aws_s3_bucket.artifact_bucket.arn}/*"
        ]
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "github_actions_deploy" {
  role       = aws_iam_role.github_actions_deploy.name
  policy_arn = aws_iam_policy.github_actions_deploy.arn
}

resource "aws_iam_role_policy_attachment" "backend_runtime" {
  role       = aws_iam_role.backend_runtime.name
  policy_arn = aws_iam_policy.backend_runtime.arn
}

resource "aws_iam_role_policy_attachment" "backend_runtime_ssm" {
  role       = aws_iam_role.backend_runtime.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "backend_runtime" {
  name = "khaleo-backend-runtime-profile"
  role = aws_iam_role.backend_runtime.name
}

# Create Secrets Manager placeholders for runtime secrets (no secret string supplied)
resource "aws_secretsmanager_secret" "db_credentials" {
  name = var.db_secret_name
}

resource "aws_secretsmanager_secret" "jwt_secret" {
  name = var.jwt_secret_name
}

resource "aws_secretsmanager_secret" "ses_credentials" {
  name = var.ses_secret_name
}

### GitHub environment
resource "github_repository_environment" "production" {
  repository  = local.repo
  environment = var.github_environment_name
}

resource "github_actions_environment_secret" "deployment" {
  for_each = local.deployment_environment_secrets

  repository      = local.repo
  environment     = github_repository_environment.production.environment
  secret_name     = each.key
  plaintext_value = tostring(each.value)
}

resource "github_actions_environment_variable" "deployment" {
  for_each = local.deployment_environment_variables

  repository    = local.repo
  environment   = github_repository_environment.production.environment
  variable_name = each.key
  value         = tostring(each.value)
}

resource "github_actions_environment_secret" "manual_placeholders" {
  for_each = toset(var.github_manual_environment_secrets)

  repository      = local.repo
  environment     = github_repository_environment.production.environment
  secret_name     = each.value
  plaintext_value = var.placeholder_secret_value

  # Allow manual updates in GitHub UI without Terraform overwriting them.
  lifecycle {
    ignore_changes = [plaintext_value]
  }
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
