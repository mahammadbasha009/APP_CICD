// Learning pipeline:
// GitHub -> Maven -> SonarQube -> Docker -> AWS ECR -> AWS Secrets Manager -> Kubernetes

pipeline {
    agent any

    // Jenkins -> Manage Jenkins -> Tools
    // Maven installation Name must be exactly "Maven"
    tools {
        maven 'Maven'
    }

    environment {

        // =========================================================
        // AWS CONFIGURATION
        // =========================================================

        AWS_REGION     = 'eu-north-1'

        // IMPORTANT:
        // Replace this with your actual 12-digit AWS Account ID
        AWS_ACCOUNT_ID = 'REPLACE_AWS_ACCOUNT_ID'

        ECR_REPOSITORY = 'simple-app'

        AWS_SECRET_ID  = 'simple-app-secret'


        // =========================================================
        // JENKINS CREDENTIALS
        // =========================================================

        AWS_ACCESS_KEY_ID =
            credentials('aws-access-key-id')

        AWS_SECRET_ACCESS_KEY =
            credentials('aws-secret-access-key')
    }


    stages {

        // =========================================================
        // STAGE 1 - CHECKOUT
        // =========================================================

        stage('Checkout') {
            steps {

                echo '========================================'
                echo 'STAGE 1: Checkout source code'
                echo '========================================'

                checkout scm
            }
        }


        // =========================================================
        // STAGE 2 - VERIFY MAVEN
        // =========================================================

        stage('Verify Maven') {
            steps {

                echo '========================================'
                echo 'STAGE 2: Verify Maven'
                echo '========================================'

                bat 'mvn --version'
            }
        }


        // =========================================================
        // STAGE 3 - MAVEN BUILD AND TEST
        // =========================================================

        stage('Maven Build & Test') {
            steps {

                echo '========================================'
                echo 'STAGE 3: Maven Build and Unit Tests'
                echo '========================================'

                bat 'mvn clean verify'
            }
        }


        // =========================================================
        // STAGE 4 - SONARQUBE
        // =========================================================

        stage('SonarQube Analysis') {
            steps {

                echo '========================================'
                echo 'STAGE 4: SonarQube Code Analysis'
                echo '========================================'

                // Jenkins:
                // Manage Jenkins -> System -> SonarQube installations
                //
                // Name must be:
                // SonarQube

                withSonarQubeEnv('SonarQube') {

                    bat '''
                    mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar ^
                    -Dsonar.projectKey=APP-CICD
                    '''
                }
            }
        }


        // =========================================================
        // STAGE 5 - DOCKER BUILD
        // =========================================================

        stage('Docker Build') {
            steps {

                echo '========================================'
                echo 'STAGE 5: Build Docker Image'
                echo '========================================'

                bat '''
                docker --version
                docker build -t %ECR_REPOSITORY%:%BUILD_NUMBER% .
                docker images
                '''
            }
        }


        // =========================================================
        // STAGE 6 - PUSH IMAGE TO AWS ECR
        // =========================================================

        stage('Push Image to AWS ECR') {
            steps {

                echo '========================================'
                echo 'STAGE 6: Push Docker Image to AWS ECR'
                echo '========================================'

                bat '''
                aws --version

                aws ecr get-login-password --region %AWS_REGION% | docker login --username AWS --password-stdin %AWS_ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com

                docker tag %ECR_REPOSITORY%:%BUILD_NUMBER% %AWS_ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com/%ECR_REPOSITORY%:%BUILD_NUMBER%

                docker push %AWS_ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com/%ECR_REPOSITORY%:%BUILD_NUMBER%
                '''
            }
        }


        // =========================================================
        // STAGE 7 - AWS SECRETS MANAGER -> KUBERNETES
        // =========================================================

        stage('Create Kubernetes Secret') {
            steps {

                echo '========================================'
                echo 'STAGE 7: Get Secret from AWS'
                echo '========================================'

                bat '''
                for /f "delims=" %%S in ('aws secretsmanager get-secret-value --secret-id %AWS_SECRET_ID% --region %AWS_REGION% --query SecretString --output text') do set "APP_SECRET=%%S"

                kubectl create secret generic simple-app-secret --from-literal=APP_SECRET="%APP_SECRET%" --dry-run=client -o yaml | kubectl apply -f -
                '''
            }
        }


        // =========================================================
        // STAGE 8 - KUBERNETES DEPLOYMENT
        // =========================================================

        stage('Deploy to Kubernetes') {
            steps {

                echo '========================================'
                echo 'STAGE 8: Deploy Application to Kubernetes'
                echo '========================================'

                bat '''
                kubectl version --client

                kubectl get nodes

                powershell -NoProfile -Command "(Get-Content kubernetes/deployment.yaml) -replace 'ECR_IMAGE_PLACEHOLDER','%AWS_ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com/%ECR_REPOSITORY%:%BUILD_NUMBER%' | Set-Content kubernetes/deployment-generated.yaml"

                kubectl apply -f kubernetes/deployment-generated.yaml

                kubectl apply -f kubernetes/service.yaml

                kubectl rollout status deployment/simple-app --timeout=120s

                kubectl get pods

                kubectl get svc simple-app-service
                '''
            }
        }
    }


    // =============================================================
    // PIPELINE RESULT
    // =============================================================

    post {

        success {
            echo '========================================'
            echo 'CI/CD PIPELINE COMPLETED SUCCESSFULLY'
            echo '========================================'
        }

        failure {
            echo '========================================'
            echo 'CI/CD PIPELINE FAILED'
            echo 'Check the failed stage above.'
            echo '========================================'
        }

        always {
            echo "Build Number: ${BUILD_NUMBER}"
        }
    }
}