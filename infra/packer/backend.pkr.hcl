packer {
  required_plugins {
    amazon = {
      source  = "github.com/hashicorp/amazon"
      version = ">= 1.3.9"
    }
  }
}

variable "aws_region" {
  type = string
}

variable "subnet_id" {
  type = string
}

variable "app_jar_path" {
  type = string
}

variable "db_secret_id" {
  type = string
}

variable "jwt_secret_id" {
  type = string
}

variable "ses_secret_id" {
  type = string
}

variable "runtime_env_path" {
  type    = string
  default = "/opt/khaleo/flashcard-backend/runtime-secrets.env"
}

variable "app_sha" {
  type = string
}

locals {
  ami_name = "khaleo-backend-${formatdate("YYYYMMDDhhmmss", timestamp())}-${substr(var.app_sha, 0, 8)}"
}

source "amazon-ebs" "backend" {
  region        = var.aws_region
  subnet_id     = var.subnet_id
  instance_type = "t3.small"
  ssh_username  = "ec2-user"

  source_ami_filter {
    filters = {
      name                = "al2023-ami-2023.*-kernel-6.1-x86_64"
      root-device-type    = "ebs"
      virtualization-type = "hvm"
    }
    owners      = ["137112412989"]
    most_recent = true
  }

  ami_name = local.ami_name

  tags = {
    Name        = local.ami_name
    Application = "khaleo-backend"
    ManagedBy   = "github-actions"
    CommitSha   = var.app_sha
  }
}

build {
  name    = "khaleo-backend-immutable"
  sources = ["source.amazon-ebs.backend"]

  provisioner "file" {
    source      = var.app_jar_path
    destination = "/tmp/current.jar"
  }

  provisioner "shell" {
    script = "scripts/provision-backend-ami.sh"
    environment_vars = [
      "DB_SECRET_ID=${var.db_secret_id}",
      "JWT_SECRET_ID=${var.jwt_secret_id}",
      "SES_SECRET_ID=${var.ses_secret_id}",
      "RUNTIME_ENV_PATH=${var.runtime_env_path}",
      "APP_SHA=${var.app_sha}"
    ]
  }
}
