@Library('pinakaone-shared-lib') _

pipeline {
    agent any

    // No parameters, no triggers - plain "Build Now" against main. No
    // polling/webhook until there's a public endpoint wired up.

    stages {
        stage('Checkout gitops repo') {
            steps {
                checkout scm
            }
        }

        stage('Build and push backend') {
            steps {
                buildAndPushBackend(branch: 'main', credentialsId: 'github-credentials')
            }
        }
    }
}
