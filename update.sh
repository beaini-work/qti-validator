#!/bin/bash
set -e

# Build the Java application with latest changes
gradle clean build

# Apply Terraform changes
cd terraform
terraform apply -auto-approve

echo "Application updated and deployed to: $(terraform output -raw eb_environment_url)" 