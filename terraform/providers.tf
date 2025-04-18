terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
  
  # Optional: Configure backend for state storage
  # backend "s3" {
  #   bucket = "terraform-state-bucket"
  #   key    = "qti-validator/terraform.tfstate"
  #   region = "us-east-2"
  # }
}

provider "aws" {
  region = var.aws_region
} 