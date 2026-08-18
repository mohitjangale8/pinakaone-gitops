@Library('pinakaone-shared-lib') _

pipeline {
    agent any

    // No parameters, no triggers - plain "Build Now" against main. No
    // polling/webhook until there's a public endpoint wired up.

    stages {
        stage('Build and deploy frontend') {
            steps {
                buildAndDeployFrontend(branch: 'main', credentialsId: 'github-credentials')
            }
        }
    }
}
