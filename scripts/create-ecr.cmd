@echo off
REM Run once. Creates the ECR repository if it does not already exist.
aws ecr describe-repositories --repository-names simple-app --region eu-north-1 >nul 2>&1 || aws ecr create-repository --repository-name simple-app --region eu-north-1
