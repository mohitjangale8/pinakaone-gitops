// Shared library step: takes the submitted CONFIG_DATA (the Active
// Choices box on the job's Build page, pre-filled with just the selected
// application's config.* block - not the whole values.yaml, since that
// also has image.tag/replicas/service etc. owned by other pipelines/HPA)
// and merges it back into the full file, then pushes - ArgoCD (polling
// every 30s) picks it up and rolls the deployment.
def call(Map config = [:]) {
    def app            = config.application
    def valuesFile     = "charts/${app}/values.yaml"
    def newConfigBlock = config.content
    def current        = readFile(valuesFile)

    writeFile file: 'config-fragment.yaml', text: newConfigBlock
    // Fail fast if the edited fragment itself isn't valid YAML, before it
    // ever reaches git/ArgoCD.
    sh 'yq eval . config-fragment.yaml > /dev/null'

    sh "yq eval '.config = load(\"config-fragment.yaml\")' ${valuesFile} > values.yaml.new"

    if (readFile('values.yaml.new') == current) {
        echo 'No changes - nothing to do.'
        sh 'rm -f config-fragment.yaml values.yaml.new'
        return
    }

    echo "--- diff: ${valuesFile} ---"
    sh "diff -u ${valuesFile} values.yaml.new || true"

    sh "mv values.yaml.new ${valuesFile}"
    sh 'rm -f config-fragment.yaml'
    currentBuild.displayName = "#${env.BUILD_NUMBER} ${app} config updated"

    withCredentials([usernamePassword(credentialsId: 'github-credentials', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
        sh """
            git config user.email 'jenkins@pinakaone.local'
            git config user.name 'Jenkins'
            git add ${valuesFile}
            git commit -m "${app}: config updated via Jenkins" || echo 'nothing to commit'
            git push https://\$GIT_USER:\$GIT_TOKEN@github.com/mohitjangale8/pinakaone-gitops.git HEAD:main
        """
    }
    echo 'Pushed - ArgoCD will pick it up within ~30s.'
}
