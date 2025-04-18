#!/bin/bash
set -e

# Build the Java application with latest changes
./gradlew clean build

# Apply Terraform changes
cd terraform
terraform apply -auto-approve

echo "Application updated and deployed to: $(terraform output -raw eb_environment_url)" 