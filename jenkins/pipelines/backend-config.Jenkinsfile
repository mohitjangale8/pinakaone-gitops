@Library('pinakaone-shared-lib') _

// Updates charts/backend/values.yaml's config.* block (the backend's
// ConfigMap) without needing a code change/build - leave a parameter
// blank to keep its current value untouched.
pipeline {
    agent any

    parameters {
        string(name: 'CORS_ALLOWED_ORIGINS', defaultValue: '', description: 'config.corsAllowedOrigins - comma-separated origins')
        string(name: 'JWT_EXPIRATION_MS', defaultValue: '', description: 'config.jwtExpirationMs')
        choice(name: 'PAYMENT_MOCK', choices: ['', 'true', 'false'], description: 'config.paymentMock - leave blank to keep as-is')
        string(name: 'S3_BUCKET', defaultValue: '', description: 'config.s3Bucket')
        string(name: 'S3_REGION', defaultValue: '', description: 'config.s3Region')
        string(name: 'S3_PUBLIC_BASE', defaultValue: '', description: 'config.s3PublicBase')
        choice(name: 'TRACKING_ENABLED', choices: ['', 'true', 'false'], description: 'config.trackingEnabled - leave blank to keep as-is')
        string(name: 'TRACKING_CRON', defaultValue: '', description: 'config.trackingCron - quartz cron expression')
    }

    stages {
        stage('Checkout gitops repo') {
            steps {
                checkout scm
            }
        }

        stage('Update backend ConfigMap values') {
            steps {
                updateBackendConfigMap(
                    corsAllowedOrigins: params.CORS_ALLOWED_ORIGINS,
                    jwtExpirationMs: params.JWT_EXPIRATION_MS,
                    paymentMock: params.PAYMENT_MOCK,
                    s3Bucket: params.S3_BUCKET,
                    s3Region: params.S3_REGION,
                    s3PublicBase: params.S3_PUBLIC_BASE,
                    trackingEnabled: params.TRACKING_ENABLED,
                    trackingCron: params.TRACKING_CRON
                )
            }
        }
    }
}
