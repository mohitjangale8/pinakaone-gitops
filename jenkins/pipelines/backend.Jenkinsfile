@Library('pinakaone-shared-lib') _

pipeline {
    agent any

    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: 'quickmart-backend branch to build')
    }

    // Manual trigger only for now - no polling/webhook until there's a
    // public endpoint (or SCM polling) wired up.
    triggers {}

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
