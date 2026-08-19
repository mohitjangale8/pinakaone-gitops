// Shared library step: read charts/backend/values.yaml, show it in one
// editable multi-line box pre-filled with the current content (Jenkins'
// input step, `text` parameter - no extra plugin needed), then write back
// whatever was submitted, validate it's still valid YAML, and push.
def call() {
    def valuesFile = 'charts/backend/values.yaml'
    def current = readFile(valuesFile)

    def edited = input(
        message: 'Edit charts/backend/values.yaml, then Submit',
        parameters: [
            text(name: 'VALUES_YAML', defaultValue: current, description: 'Full file - edit and submit')
        ]
    )

    if (edited == current) {
        echo 'No changes made - nothing to commit.'
        return
    }

    writeFile file: valuesFile, text: edited

    // Fail loudly before pushing broken YAML - ArgoCD would otherwise
    // apply it as-is and take the backend down.
    sh "yq eval . ${valuesFile} > /dev/null"

    currentBuild.displayName = "#${env.BUILD_NUMBER} values.yaml edited"

    withCredentials([usernamePassword(credentialsId: 'github-credentials', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
        sh """
            git config user.email 'jenkins@pinakaone.local'
            git config user.name 'Jenkins'
            git add ${valuesFile}
            git commit -m 'backend: values.yaml edited via Jenkins' || echo 'nothing to commit'
            git push https://\$GIT_USER:\$GIT_TOKEN@github.com/mohitjangale8/pinakaone-gitops.git HEAD:main
        """
    }

    echo 'Pushed - ArgoCD will pick it up within ~30s.'
}
