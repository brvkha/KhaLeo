terraform {
  backend "s3" {
    bucket       = "khaleo-tf-state-backend"
    key          = "flashcard-app/terraform.tfstate"
    region       = "ap-southeast-1"
    use_lockfile = true
    encrypt      = true
  }
  required_version = ">= 1.2.0"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    github = {
      source  = "integrations/github"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

provider "aws" {
  alias  = "us_east_1"
  region = "us-east-1"
}

# The GitHub provider will use the environment variable GITHUB_TOKEN or GH_TOKEN
provider "github" {
  owner = var.github_owner
}
