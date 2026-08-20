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

    // Jenkins' own build number, not branch+sha - gives a clean, consistent
    // v1, v2, v3... sequence with zero extra state to track. The git sha is
    // still recoverable from the commit this build number's Jenkins run
    // checked out, if ever needed.
    def imageTag = "v${env.BUILD_NUMBER}"
    dir('backend-src') {
        git branch: branch, url: repoUrl, credentialsId: credentialsId

        def shortSha  = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
        def author    = sh(script: 'git log -1 --pretty=%an', returnStdout: true).trim()
        def message   = sh(script: 'git log -1 --pretty=%s', returnStdout: true).trim()
        currentBuild.displayName = "#${env.BUILD_NUMBER} quickmart-backend -> ${branch}"
        currentBuild.description = "quickmart-backend @ ${shortSha} ${author}\n${message}"

        // SonarQube analysis - native mvn (Maven+JDK21 installed in the
        // Jenkins container, see pinakaone-iac user_data.sh.tpl). Token
        // fetched from Secrets Manager at runtime (not stored in Jenkins).
        // Uses the sonar.* properties declared in the app's pom.xml.
        def sonarToken = sh(
            script: "aws secretsmanager get-secret-value --region ${region} --secret-id quickmart/sonarqube-token --query SecretString --output text 2>/dev/null || true",
            returnStdout: true
        ).trim()
        if (sonarToken) {
            withEnv(["SONAR_TOKEN=${sonarToken}"]) {
                sh 'mvn -B -DskipTests compile sonar:sonar'
            }
            def sonarUrl = 'https://sonarqube.pinakaone.in/dashboard?id=quickmart-backend'
            currentBuild.description += "<br/><a href=\"${sonarUrl}\" target=\"_blank\">SonarQube report</a>"
        } else {
            echo "WARNING: SonarQube token not found in Secrets Manager (quickmart/sonarqube-token) - skipping analysis"
        }

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
            git push https://\$GIT_USER:\$GIT_TOKEN@github.com/mohitjangale8/pinakaone-gitops.git HEAD:main
        """
    }

    echo "Pushed ${registry}/${image}:${imageTag} and updated the gitops chart - ArgoCD will pick it up within ~30s."
}
