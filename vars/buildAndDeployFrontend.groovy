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

        sh """
            npm ci
            npm run build
            aws s3 sync dist/quickmart-frontend/browser s3://${bucket} --delete --region ${region}
        """
    }

    echo "Synced to s3://${bucket}. No CloudFront distribution yet, so this isn't publicly reachable until that's unblocked - see AWS Support case for account verification."
}
