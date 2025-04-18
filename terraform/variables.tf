variable "aws_region" {
  description = "AWS region to deploy resources"
  type        = string
  default     = "us-east-2"
}

variable "application_name" {
  description = "Name of the Elastic Beanstalk application"
  type        = string
  default     = "qti-validator"
}

variable "environment_name" {
  description = "Name of the Elastic Beanstalk environment"
  type        = string
  default     = "qti-validator-prod"
}

variable "solution_stack_name" {
  description = "Elastic Beanstalk solution stack name"
  type        = string
  default     = "64bit Amazon Linux 2023 v4.5.0 running Corretto 21"
}

variable "jar_path" {
  description = "Path to the JAR file to deploy"
  type        = string
  default     = "../build/libs/qti-validator.jar"
} 