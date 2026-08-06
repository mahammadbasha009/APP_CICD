// Learning pipeline: GitHub -> Maven -> SonarQube -> Docker -> AWS ECR -> AWS Secrets Manager -> Kubernetes
pipeline {
    agent any

    environment {
        // UPDATE these 4 values before running.
        AWS_REGION     = 'eu-north-1'
        AWS_ACCOUNT_ID = 'REPLACE_AWS_ACCOUNT_ID'
        ECR_REPOSITORY = 'simple-app'
        AWS_SECRET_ID  = 'simple-app-secret'

        // Jenkins credential IDs. Store these as Secret text credentials in Jenkins.
        AWS_ACCESS_KEY_ID     = credentials('aws-access-key-id')
        AWS_SECRET_ACCESS_KEY = credentials('aws-secret-access-key')
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Maven Build & Test') {
            steps { bat 'mvn clean package' }
        }

        stage('SonarQube Analysis') {
            steps {
                // Jenkins SonarQube server name must be "SonarQube".
                withSonarQubeEnv('SonarQube') {
                    bat 'mvn sonar:sonar'
                }
            }
        }

        stage('Docker Build') {
            steps { bat 'docker build -t %ECR_REPOSITORY%:%BUILD_NUMBER% .' }
        }

        stage('Push Image to AWS ECR') {
            steps {
                bat '''
                aws ecr get-login-password --region %AWS_REGION% | docker login --username AWS --password-stdin %AWS_ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com
                docker tag %ECR_REPOSITORY%:%BUILD_NUMBER% %AWS_ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com/%ECR_REPOSITORY%:%BUILD_NUMBER%
                docker push %AWS_ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com/%ECR_REPOSITORY%:%BUILD_NUMBER%
                '''
            }
        }

        stage('Create Kubernetes Secret') {
            steps {
                // Learning method: read one plain secret value from AWS Secrets Manager and create/update a K8s Secret.
                bat '''
                for /f "delims=" %%S in ('aws secretsmanager get-secret-value --secret-id %AWS_SECRET_ID% --region %AWS_REGION% --query SecretString --output text') do set "APP_SECRET=%%S"
                kubectl create secret generic simple-app-secret --from-literal=APP_SECRET="%APP_SECRET%" --dry-run=client -o yaml | kubectl apply -f -
                '''
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                bat '''
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
}
