// Shared library step: checkout quickmart-frontend at the given branch,
// build it, and sync the static output straight to the frontend's S3
// bucket. No image, no chart, no ArgoCD involvement - this is the whole
// deploy. CloudFront cache invalidation is skipped for now (distribution
// isn't live yet, pending AWS account verification) - once it exists, add
// an `aws cloudfront create-invalidation` call here.
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
    }

    echo "Synced to s3://${bucket}. No CloudFront distribution yet, so this isn't publicly reachable until that's unblocked - see AWS Support case for account verification."
}
