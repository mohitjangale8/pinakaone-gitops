@Library('pinakaone-shared-lib') _

pipeline {
    agent any

    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: 'quickmart-backend branch to build')
    }

    // No triggers block at all = manual "Build with Parameters" only. No
    // polling/webhook until there's a public endpoint wired up.

    stages {
        stage('Checkout gitops repo') {
            steps {
                checkout scm
            }
        }

        stage('Build and push backend') {
            steps {
                buildAndPushBackend(branch: params.BRANCH, credentialsId: 'github-credentials')
            }
        }
    }
}
