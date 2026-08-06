# Simple Application CI/CD Pipeline

Purpose: a small learning project for GitHub -> Jenkins -> Maven -> SonarQube -> Docker -> AWS ECR -> AWS Secrets Manager -> Kubernetes.

## Files
- `pom.xml` - Maven project, JUnit test, creates `target/simple-app.jar`.
- `src/main/.../App.java` - tiny HTTP Java app on port 8080.
- `src/test/.../AppTest.java` - simple Maven/JUnit test.
- `Dockerfile` - packages the JAR as a Docker image.
- `kubernetes/deployment.yaml` - runs 2 application pods and reads `APP_SECRET` from Kubernetes Secret.
- `kubernetes/service.yaml` - exposes the app with NodePort 30080.
- `Jenkinsfile` - complete learning CI/CD stages.
- `scripts/create-ecr.cmd` - one-time ECR repository creation helper.

## Prerequisites on the Jenkins Windows machine
Git, JDK 21, Maven, Jenkins, Docker Desktop, kubectl, AWS CLI. Kubernetes must be available to `kubectl` (for example Docker Desktop Kubernetes). SonarQube must be running and configured in Jenkins.

## One-time setup
1. AWS CLI: run `aws configure` and verify `aws sts get-caller-identity`.
2. ECR: run `scripts\\create-ecr.cmd`.
3. AWS Secrets Manager: create a secret named `simple-app-secret` with a simple test value. Do not put the secret in Git.
4. Jenkins credentials: create Secret text IDs `aws-access-key-id` and `aws-secret-access-key`.
5. SonarQube: run a SonarQube server (for learning, Docker is fine), create a token, install Jenkins SonarQube Scanner plugin, and configure a server named exactly `SonarQube` under Jenkins system configuration.
6. Kubernetes: verify `kubectl get nodes` succeeds from the same Windows account Jenkins uses.
7. Jenkinsfile: replace `REPLACE_AWS_ACCOUNT_ID`. Change region/repository/secret name only if yours differ.

## Local checks before Jenkins
From project root:
`mvn clean package`
`docker build -t simple-app:local .`
`kubectl get nodes`
`aws sts get-caller-identity`

## GitHub push
`git init`
`git add .`
`git commit -m "Initial simple application CI CD pipeline"`
`git branch -M main`
`git remote add origin YOUR_GITHUB_REPOSITORY_URL`
`git push -u origin main`

## Jenkins job
New Item -> Pipeline -> Pipeline script from SCM -> Git -> repository URL -> branch `*/main` -> Script Path `Jenkinsfile` -> Save -> Build Now.

## Pipeline stages
1. Checkout: downloads source from GitHub.
2. Maven Build & Test: compiles, tests and packages the JAR.
3. SonarQube Analysis: sends source analysis to SonarQube.
4. Docker Build: builds an image tagged with Jenkins BUILD_NUMBER.
5. Push Image to AWS ECR: logs in to ECR and pushes the versioned image.
6. Create Kubernetes Secret: retrieves the test secret from AWS Secrets Manager and creates/updates `simple-app-secret` in Kubernetes.
7. Deploy to Kubernetes: inserts the ECR image into the deployment, applies Kubernetes YAML, waits for rollout and displays pods/service.

## Important learning note
This is deliberately simple. The AWS Secrets Manager -> Jenkins -> Kubernetes Secret step can expose a secret to the Jenkins process environment. For a production system, prefer workload IAM and AWS Secrets Store CSI Driver / External Secrets rather than passing secret values through Jenkins.

## Verify after pipeline
`kubectl get pods`
`kubectl get svc simple-app-service`
`kubectl describe deployment simple-app`

For Docker Desktop Kubernetes, test NodePort according to your local Kubernetes networking. You can also use:
`kubectl port-forward service/simple-app-service 8080:8080`
Then open `http://localhost:8080`.
