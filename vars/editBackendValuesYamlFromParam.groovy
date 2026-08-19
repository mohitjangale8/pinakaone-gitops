// Shared library step: takes the submitted CONFIG_DATA (the Active
// Choices box on the job's Build page, pre-filled with the current file)
// and either shows a diff only (dryRun=true) or writes+validates+pushes.
def call(Map config = [:]) {
    def valuesFile  = 'charts/backend/values.yaml'
    def dryRun      = config.dryRun
    def newContent  = config.content
    def current     = readFile(valuesFile)

    if (newContent == current) {
        echo 'No changes - nothing to do.'
        return
    }

    writeFile file: 'values.yaml.new', text: newContent
    // Fail fast on invalid YAML before it ever reaches git/ArgoCD.
    sh 'yq eval . values.yaml.new > /dev/null'

    sh "diff -u ${valuesFile} values.yaml.new || true"

    if (dryRun) {
        echo 'DRY_RUN is checked - diff shown above, nothing pushed. Uncheck DRY_RUN and re-run to apply.'
        sh 'rm -f values.yaml.new'
        return
    }

    sh "mv values.yaml.new ${valuesFile}"
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
