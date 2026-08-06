// ============================================================================
// APP-CICD Learning Pipeline
//
// GitHub
//    ↓
// Maven Build & Unit Test
//    ↓
// SonarQube Analysis
//    ↓
// Docker Build
//    ↓
// AWS ECR
//    ↓
// Verify Kubernetes
//    ↓
// AWS Secrets Manager
//    ↓
// Kubernetes Secret
//    ↓
// Kubernetes Deployment
// ============================================================================

pipeline {

    agent any


    // ========================================================================
    // JENKINS TOOLS
    // ========================================================================
    //
    // Jenkins -> Manage Jenkins -> Tools
    //
    // Maven installation Name must be exactly:
    //
    // Maven
    //
    // ========================================================================

    tools {
        maven 'Maven'
    }


    // ========================================================================
    // ENVIRONMENT VARIABLES
    // ========================================================================

    environment {

        // --------------------------------------------------------------------
        // AWS CONFIGURATION
        // --------------------------------------------------------------------

        AWS_REGION = 'eu-north-1'

        AWS_ACCOUNT_ID = '746491202703'

        ECR_REPOSITORY = 'simple-app'

        AWS_SECRET_ID = 'simple-app-secret'


        // --------------------------------------------------------------------
        // KUBERNETES CONFIGURATION
        // --------------------------------------------------------------------
        //
        // Jenkins Windows Service does not automatically use:
        //
        // C:\Users\smdba\.kube\config
        //
        // Therefore kubeconfig was copied to Jenkins home:
        //
        // C:\ProgramData\Jenkins\.jenkins\.kube\config
        //
        // --------------------------------------------------------------------

        KUBECONFIG = 'C:\\ProgramData\\Jenkins\\.jenkins\\.kube\\config'


        // --------------------------------------------------------------------
        // JENKINS AWS CREDENTIALS
        // --------------------------------------------------------------------
        //
        // Jenkins:
        //
        // Manage Jenkins
        //      ↓
        // Credentials
        //      ↓
        // System
        //      ↓
        // Global credentials
        //
        // --------------------------------------------------------------------

        AWS_ACCESS_KEY_ID =
            credentials('aws-access-key-id')

        AWS_SECRET_ACCESS_KEY =
            credentials('aws-secret-access-key')
    }


    stages {


        // ====================================================================
        // STAGE 1
        // CHECKOUT SOURCE CODE FROM GITHUB
        // ====================================================================

        stage('Checkout') {

            steps {

                echo '========================================'
                echo 'STAGE 1: Checkout Source Code'
                echo '========================================'

                checkout scm
            }
        }



        // ====================================================================
        // STAGE 2
        // VERIFY MAVEN
        // ====================================================================

        stage('Verify Maven') {

            steps {

                echo '========================================'
                echo 'STAGE 2: Verify Maven'
                echo '========================================'

                bat '''
                mvn --version
                '''
            }
        }



        // ====================================================================
        // STAGE 3
        // MAVEN BUILD + UNIT TEST
        // ====================================================================

        stage('Maven Build & Test') {

            steps {

                echo '========================================'
                echo 'STAGE 3: Maven Build and Unit Tests'
                echo '========================================'

                bat '''
                mvn clean verify
                '''
            }
        }



        // ====================================================================
        // STAGE 4
        // SONARQUBE CODE ANALYSIS
        // ====================================================================

        stage('SonarQube Analysis') {

            steps {

                echo '========================================'
                echo 'STAGE 4: SonarQube Code Analysis'
                echo '========================================'

                // Jenkins:
                //
                // Manage Jenkins
                //      ↓
                // System
                //      ↓
                // SonarQube installations
                //
                // Name must be:
                //
                // SonarQube

                withSonarQubeEnv('SonarQube') {

                    bat '''
                    mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar ^
                    -Dsonar.projectKey=APP-CICD
                    '''
                }
            }
        }



        // ====================================================================
        // STAGE 5
        // BUILD DOCKER IMAGE
        // ====================================================================

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



        // ====================================================================
        // STAGE 6
        // PUSH DOCKER IMAGE TO AWS ECR
        // ====================================================================

        stage('Push Image to AWS ECR') {

            steps {

                echo '========================================'
                echo 'STAGE 6: Push Docker Image to AWS ECR'
                echo '========================================'

                bat '''

                aws --version

                echo Logging into AWS ECR...

                aws ecr get-login-password --region %AWS_REGION% | docker login --username AWS --password-stdin %AWS_ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com


                echo Tagging Docker image...

                docker tag %ECR_REPOSITORY%:%BUILD_NUMBER% %AWS_ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com/%ECR_REPOSITORY%:%BUILD_NUMBER%


                echo Pushing Docker image to ECR...

                docker push %AWS_ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com/%ECR_REPOSITORY%:%BUILD_NUMBER%

                '''
            }
        }



        // ====================================================================
        // STAGE 7
        // VERIFY JENKINS -> KUBERNETES CONNECTION
        // ====================================================================

        stage('Verify Kubernetes') {

            steps {

                echo '========================================'
                echo 'STAGE 7: Verify Kubernetes Connection'
                echo '========================================'

                bat '''

                echo Checking kubectl...

                kubectl version --client


                echo.
                echo Current Kubernetes Context:
                echo.

                kubectl config current-context


                echo.
                echo Kubernetes Cluster Nodes:
                echo.

                kubectl get nodes

                '''
            }
        }



        // ====================================================================
        // STAGE 8
        // AWS SECRETS MANAGER -> KUBERNETES SECRET
        // ====================================================================

        stage('Create Kubernetes Secret') {

            steps {

                echo '========================================'
                echo 'STAGE 8: AWS Secrets Manager'
                echo 'Create Kubernetes Secret'
                echo '========================================'

                powershell '''

                    Write-Host "Retrieving application secret from AWS Secrets Manager..."


                    $secret = aws secretsmanager get-secret-value `
                        --secret-id $env:AWS_SECRET_ID `
                        --region $env:AWS_REGION `
                        --query SecretString `
                        --output text


                    if (-not $secret) {

                        throw "Unable to retrieve secret from AWS Secrets Manager"

                    }


                    Write-Host "Secret retrieved successfully."

                    Write-Host "Creating/updating Kubernetes Secret..."


                    kubectl create secret generic simple-app-secret `
                        --from-literal=APP_SECRET="$secret" `
                        --dry-run=client `
                        -o yaml |
                        kubectl apply -f -


                    if ($LASTEXITCODE -ne 0) {

                        throw "Failed to create Kubernetes Secret"

                    }


                    Write-Host "Kubernetes Secret created/updated successfully."

                '''
            }
        }



        // ====================================================================
        // STAGE 9
        // DEPLOY APPLICATION TO KUBERNETES
        // ====================================================================

        stage('Deploy to Kubernetes') {

            steps {

                echo '========================================'
                echo 'STAGE 9: Deploy Application to Kubernetes'
                echo '========================================'


                // ----------------------------------------------------------------
                // Replace ECR_IMAGE_PLACEHOLDER inside deployment.yaml
                //
                // Example generated image:
                //
                // 746491202703.dkr.ecr.eu-north-1.amazonaws.com/simple-app:5
                //
                // ----------------------------------------------------------------

                bat '''

                echo -----------------------------------------
                echo Creating Kubernetes Deployment YAML
                echo -----------------------------------------


                powershell -NoProfile -Command "(Get-Content kubernetes/deployment.yaml) -replace 'ECR_IMAGE_PLACEHOLDER','%AWS_ACCOUNT_ID%.dkr.ecr.%AWS_REGION%.amazonaws.com/%ECR_REPOSITORY%:%BUILD_NUMBER%' | Set-Content kubernetes/deployment-generated.yaml"


                echo.
                echo -----------------------------------------
                echo Applying Kubernetes Deployment
                echo -----------------------------------------


                kubectl apply -f kubernetes/deployment-generated.yaml


                echo.
                echo -----------------------------------------
                echo Applying Kubernetes Service
                echo -----------------------------------------


                kubectl apply -f kubernetes/service.yaml


                echo.
                echo -----------------------------------------
                echo Waiting for Deployment
                echo -----------------------------------------


                kubectl rollout status deployment/simple-app --timeout=120s


                echo.
                echo -----------------------------------------
                echo Kubernetes Pods
                echo -----------------------------------------


                kubectl get pods


                echo.
                echo -----------------------------------------
                echo Kubernetes Service
                echo -----------------------------------------


                kubectl get svc simple-app-service

                '''
            }
        }
    }



    // ========================================================================
    // PIPELINE RESULT
    // ========================================================================

    post {


        success {

            echo '========================================'
            echo 'CI/CD PIPELINE COMPLETED SUCCESSFULLY'
            echo '========================================'

            echo 'GitHub          : SUCCESS'
            echo 'Maven           : SUCCESS'
            echo 'Unit Tests      : SUCCESS'
            echo 'SonarQube       : SUCCESS'
            echo 'Docker Build    : SUCCESS'
            echo 'AWS ECR         : SUCCESS'
            echo 'Kubernetes      : SUCCESS'

        }


        failure {

            echo '========================================'
            echo 'CI/CD PIPELINE FAILED'
            echo '========================================'

            echo 'Check the Jenkins Console Output.'
            echo 'Find the first failed stage above.'

        }


        always {

            echo '========================================'
            echo "Build Number: ${BUILD_NUMBER}"
            echo '========================================'

        }
    }
}