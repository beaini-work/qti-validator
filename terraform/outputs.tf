output "eb_environment_url" {
  description = "URL of the Elastic Beanstalk environment"
  value       = aws_elastic_beanstalk_environment.environment.endpoint_url
}

output "app_version" {
  description = "Application version deployed"
  value       = aws_elastic_beanstalk_application_version.app_version.name
} 