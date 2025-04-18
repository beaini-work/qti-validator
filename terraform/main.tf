# S3 bucket for application versions
resource "aws_s3_bucket" "app_versions" {
  bucket = "${var.application_name}-versions-${random_id.suffix.hex}"
}

resource "random_id" "suffix" {
  byte_length = 4
}

# Enable versioning on the S3 bucket
resource "aws_s3_bucket_versioning" "app_versions" {
  bucket = aws_s3_bucket.app_versions.id
  versioning_configuration {
    status = "Enabled"
  }
}

# Create the Elastic Beanstalk application
resource "aws_elastic_beanstalk_application" "app" {
  name        = var.application_name
  description = "QTI Validator Service"
}

# Create application version from JAR file
resource "aws_s3_object" "app_jar" {
  bucket = aws_s3_bucket.app_versions.id
  key    = "app-${formatdate("YYYYMMDDhhmmss", timestamp())}.jar"
  source = var.jar_path
  etag   = filemd5(var.jar_path)
}

# Upload Procfile
resource "aws_s3_object" "procfile" {
  bucket  = aws_s3_bucket.app_versions.id
  key     = "Procfile"
  content = "web: java -jar app.jar"
}

# Create Elastic Beanstalk application version
resource "aws_elastic_beanstalk_application_version" "app_version" {
  name        = "app-${formatdate("YYYYMMDDhhmmss", timestamp())}"
  application = aws_elastic_beanstalk_application.app.name
  description = "Application version created by Terraform"
  bucket      = aws_s3_bucket.app_versions.id
  key         = aws_s3_object.app_jar.id
}

# Create Elastic Beanstalk environment
resource "aws_elastic_beanstalk_environment" "environment" {
  name                = var.environment_name
  application         = aws_elastic_beanstalk_application.app.name
  solution_stack_name = var.solution_stack_name
  version_label       = aws_elastic_beanstalk_application_version.app_version.name
  
  # Use default AWS-managed service role
  setting {
    namespace = "aws:elasticbeanstalk:environment"
    name      = "ServiceRole"
    value     = "aws-elasticbeanstalk-service-role"
  }
  
  # Use default AWS-managed instance profile
  setting {
    namespace = "aws:autoscaling:launchconfiguration"
    name      = "IamInstanceProfile"
    value     = "aws-elasticbeanstalk-ec2-role"
  }
  
  # Application port
  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "SERVER_PORT"
    value     = "8080"
  }
  
  # Instance type
  setting {
    namespace = "aws:autoscaling:launchconfiguration"
    name      = "InstanceType"
    value     = "t2.micro"
  }
  
  # Auto-scaling
  setting {
    namespace = "aws:autoscaling:asg"
    name      = "MinSize"
    value     = "1"
  }
  
  setting {
    namespace = "aws:autoscaling:asg"
    name      = "MaxSize"
    value     = "2"
  }
  
  # Health check
  setting {
    namespace = "aws:elasticbeanstalk:environment"
    name      = "EnvironmentType"
    value     = "LoadBalanced"
  }
  
  # JVM options
  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "JAVA_OPTS"
    value     = "-Xmx256m"
  }
  
  # Deployment policy
  setting {
    namespace = "aws:elasticbeanstalk:command"
    name      = "DeploymentPolicy"
    value     = "Rolling"
  }
} 