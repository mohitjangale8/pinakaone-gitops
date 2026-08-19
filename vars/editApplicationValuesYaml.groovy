// Shared library step: takes the submitted CONFIG_DATA (the Active
// Choices box on the job's Build page, pre-filled with the selected
// application's current values.yaml) and writes it back after checking
// the diff and validating it's still parseable YAML, then pushes -
// ArgoCD (polling every 30s) picks it up and rolls the deployment.
def call(Map config = [:]) {
    def app         = config.application
    def valuesFile  = "charts/${app}/values.yaml"
    def newContent  = config.content
    def current     = readFile(valuesFile)

    if (newContent == current) {
        echo 'No changes - nothing to do.'
        return
    }

    writeFile file: 'values.yaml.new', text: newContent

    // "Compile time" check - fail fast on invalid YAML before it ever
    // reaches git/ArgoCD, instead of ArgoCD failing to apply it later.
    sh 'yq eval . values.yaml.new > /dev/null'

    echo "--- diff: ${valuesFile} ---"
    sh "diff -u ${valuesFile} values.yaml.new || true"

    sh "mv values.yaml.new ${valuesFile}"
    currentBuild.displayName = "#${env.BUILD_NUMBER} ${app}/values.yaml edited"

    withCredentials([usernamePassword(credentialsId: 'github-credentials', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
        sh """
            git config user.email 'jenkins@pinakaone.local'
            git config user.name 'Jenkins'
            git add ${valuesFile}
            git commit -m "${app}: values.yaml edited via Jenkins" || echo 'nothing to commit'
            git push https://\$GIT_USER:\$GIT_TOKEN@github.com/mohitjangale8/pinakaone-gitops.git HEAD:main
        """
    }
    echo 'Pushed - ArgoCD will pick it up within ~30s.'
}
