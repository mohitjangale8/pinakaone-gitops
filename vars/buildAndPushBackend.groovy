// Shared library step: checkout quickmart-backend at the given branch, build
// and push its Docker image to ECR, then bump the tag in this same repo's
// charts/backend/values.yaml and push - ArgoCD (polling every 30s) picks
// that up and redeploys with zero manual step beyond running this pipeline.
def call(Map config = [:]) {
    def branch        = config.branch ?: 'main'
    def credentialsId = config.credentialsId ?: 'github-credentials'
    def repoUrl       = 'https://github.com/mohitjangale8/quickmart-backend.git'
    def registry      = '931238251190.dkr.ecr.ap-south-1.amazonaws.com'
    def image         = 'quickmart-backend'
    def region        = 'ap-south-1'

    def imageTag
    dir('backend-src') {
        git branch: branch, url: repoUrl, credentialsId: credentialsId
        def sha = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
        imageTag = "${branch}-${sha}"

        sh """
            aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${registry}
            docker build -t ${registry}/${image}:${imageTag} .
            docker tag ${registry}/${image}:${imageTag} ${registry}/${image}:latest
            docker push ${registry}/${image}:${imageTag}
            docker push ${registry}/${image}:latest
        """
    }

    // Workspace root here is this same gitops repo, already checked out by
    // the pipeline's own "checkout scm" stage.
    sh "sed -i 's/^  tag: .*/  tag: ${imageTag}/' charts/backend/values.yaml"

    withCredentials([usernamePassword(credentialsId: credentialsId, usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
        sh """
            git config user.email 'jenkins@pinakaone.local'
            git config user.name 'Jenkins'
            git add charts/backend/values.yaml
            git commit -m "backend: deploy ${imageTag}" || echo 'nothing to commit'
            git push https://\$GIT_USER:\$GIT_TOKEN@github.com/mohitjangale8/pinakaone-gitops.git HEAD:dev
        """
    }

    echo "Pushed ${registry}/${image}:${imageTag} and updated the gitops chart - ArgoCD will pick it up within ~30s."
}
