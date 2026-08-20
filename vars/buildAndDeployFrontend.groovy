// Shared library step: checkout quickmart-frontend at the given branch,
// build it, sync the static output to the frontend's S3 bucket, then
// invalidate CloudFront so the new build is actually visible (S3 sync alone
// doesn't clear CloudFront's edge caches). The distribution lives in a
// different AWS account than this instance's own role, so invalidation
// uses a separate, narrowly-scoped IAM user's keys pulled from Secrets
// Manager (same cross-account pattern as Caddy's DNS-01 credentials).
def call(Map config = [:]) {
    def branch        = config.branch ?: 'main'
    def credentialsId = config.credentialsId ?: 'github-credentials'
    def repoUrl       = 'https://github.com/mohitjangale8/quickmart-frontend.git'
    def bucket        = 'quickmart-frontend-site'
    def region        = 'ap-south-1'

    dir('frontend-src') {
        git branch: branch, url: repoUrl, credentialsId: credentialsId

        def shortSha = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
        def author   = sh(script: 'git log -1 --pretty=%an', returnStdout: true).trim()
        def message  = sh(script: 'git log -1 --pretty=%s', returnStdout: true).trim()
        currentBuild.displayName = "#${env.BUILD_NUMBER} quickmart-frontend -> ${branch}"
        currentBuild.description = "quickmart-frontend @ ${shortSha} ${author}\n${message}"

        // node_modules cache outside the workspace, keyed by a hash of
        // package.json + package-lock.json - same pattern used by BARC-UI's
        // pipeline (tran-ec2-deployment-config). Skips npm entirely when
        // dependencies haven't changed since the last build, which is most
        // builds. Copied in/out rather than symlinked: npm's own atomic
        // renames (@npmcli/*) don't reliably handle node_modules itself
        // being a symlink and fail partway through install with ENOTEMPTY.
        def cacheDir = "${env.HOME}/.quickmart-frontend-nm-cache"
        sh """
            mkdir -p '${cacheDir}'
            HASH=\$(cat package.json package-lock.json 2>/dev/null | md5sum | cut -d ' ' -f1)
            PREV=\$(cat '${cacheDir}/.dephash' 2>/dev/null || echo none)
            if [ "\$PREV" = "\$HASH" ] && [ -d '${cacheDir}/node_modules' ]; then
                echo "[deps] cache HIT (\$HASH) - restoring node_modules, skipping npm ci"
                rm -rf node_modules
                cp -a '${cacheDir}/node_modules' node_modules
            else
                echo "[deps] cache MISS - running npm ci"
                npm ci
                rm -rf '${cacheDir}/node_modules'
                cp -a node_modules '${cacheDir}/node_modules'
                echo "\$HASH" > '${cacheDir}/.dephash'
            fi
            npm run build
            aws s3 sync dist/quickmart-frontend/browser s3://${bucket} --delete --region ${region}
        """

        // SonarQube analysis - native sonar-scanner (installed in the
        // Jenkins container, see pinakaone-iac user_data.sh.tpl). Config
        // in the app repo's sonar-project.properties. Token fetched from
        // Secrets Manager at runtime (not stored in Jenkins).
        def sonarToken = sh(
            script: "aws secretsmanager get-secret-value --region ${region} --secret-id quickmart/sonarqube-token --query SecretString --output text 2>/dev/null || true",
            returnStdout: true
        ).trim()
        if (sonarToken) {
            withEnv(["SONAR_TOKEN=${sonarToken}"]) {
                sh 'sonar-scanner -Dsonar.host.url=https://sonarqube.pinakaone.in'
            }
            def sonarUrl = 'https://sonarqube.pinakaone.in/dashboard?id=quickmart-frontend'
            currentBuild.description += "<br/><a href=\"${sonarUrl}\" target=\"_blank\">SonarQube report</a>"
        } else {
            echo "WARNING: SonarQube token not found in Secrets Manager (quickmart/sonarqube-token) - skipping analysis"
        }
    }

    // Cross-account credentials (CloudFront is in the paid account, this
    // instance's own role is sandbox-account only) - fetched fresh each
    // run rather than stored as a Jenkins credential, so rotating the IAM
    // user's keys in Terraform needs no matching Jenkins-side change.
    //
    // `set +x` is required here: Jenkins' sh step traces every command to
    // the build log by default (the "+ ..." lines), which would otherwise
    // print the secret and both derived keys in plaintext to anyone who can
    // read the build console.
    sh """
        set +x
        SECRET=\$(aws secretsmanager get-secret-value --region ${region} --secret-id quickmart/frontend-invalidation --query SecretString --output text)
        export AWS_ACCESS_KEY_ID=\$(echo "\$SECRET" | python3 -c "import json,sys; print(json.load(sys.stdin)['access_key_id'])")
        export AWS_SECRET_ACCESS_KEY=\$(echo "\$SECRET" | python3 -c "import json,sys; print(json.load(sys.stdin)['secret_access_key'])")
        DISTRIBUTION_ID=\$(echo "\$SECRET" | python3 -c "import json,sys; print(json.load(sys.stdin)['distribution_id'])")
        unset SECRET
        set -x
        aws cloudfront create-invalidation --distribution-id "\$DISTRIBUTION_ID" --paths '/*'
    """

    echo "Synced to s3://${bucket} and invalidated CloudFront - live at https://quickmart.pinakaone.in"
}
