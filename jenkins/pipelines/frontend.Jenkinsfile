@Library('pinakaone-shared-lib') _

pipeline {
    agent any

    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: 'quickmart-frontend branch to build')
    }

    // Manual trigger only for now - no polling/webhook until there's a
    // public endpoint (or SCM polling) wired up.
    triggers {}

    stages {
        stage('Build and deploy frontend') {
            steps {
                buildAndDeployFrontend(branch: params.BRANCH, credentialsId: 'github-credentials')
            }
        }
    }
}
