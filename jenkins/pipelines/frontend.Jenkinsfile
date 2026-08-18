@Library('pinakaone-shared-lib') _

pipeline {
    agent any

    parameters {
        string(name: 'BRANCH', defaultValue: 'main', description: 'quickmart-frontend branch to build')
    }

    // No triggers block at all = manual "Build with Parameters" only. No
    // polling/webhook until there's a public endpoint wired up.

    stages {
        stage('Build and deploy frontend') {
            steps {
                buildAndDeployFrontend(branch: params.BRANCH, credentialsId: 'github-credentials')
            }
        }
    }
}
