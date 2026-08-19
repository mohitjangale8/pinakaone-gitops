@Library('pinakaone-shared-lib') _

// Fetches the current charts/backend/values.yaml and shows it in one
// editable box (pre-filled, not a blank form) - edit and submit, and it
// gets pushed straight to pinakaone-gitops. ArgoCD (polling every 30s)
// picks it up from there, no manual kubectl step needed.
pipeline {
    agent any

    stages {
        stage('Checkout gitops repo') {
            steps {
                checkout scm
            }
        }

        stage('Edit values.yaml') {
            steps {
                editBackendValuesYaml()
            }
        }
    }
}
