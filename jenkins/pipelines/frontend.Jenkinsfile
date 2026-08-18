@Library('pinakaone-shared-lib') _

pipeline {
    agent any

    // Requires the "Git Parameter" plugin. Queries quickmart-frontend's
    // remote branches fresh on every "Build with Parameters" click, so new
    // branches show up without touching this file. No triggers - manual
    // build only, no polling/webhook until there's a public endpoint.
    parameters {
        gitParameter(
            name: 'BRANCH',
            type: 'PT_BRANCH',
            branchFilter: 'origin/(.*)',
            defaultValue: 'main',
            selectedValue: 'DEFAULT',
            sortMode: 'ASCENDING_SMART',
            quickFilterEnabled: true,
            useRepository: 'https://github.com/mohitjangale8/quickmart-frontend.git'
        )
    }

    stages {
        stage('Build and deploy frontend') {
            steps {
                buildAndDeployFrontend(branch: params.BRANCH, credentialsId: 'github-credentials')
            }
        }
    }
}
