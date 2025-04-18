#!/bin/bash
set -e

# Build the Java application
./gradlew build

# Deploy with Terraform
cd terraform
terraform init -reconfigure
terraform apply -auto-approve

# Output the URL
echo "Deployed to: $(terraform output -raw eb_environment_url)" 