// Shared library step: update one or more of charts/backend/values.yaml's
// config.* keys (the ones templates/configmap.yaml renders into the
// backend's ConfigMap) and push - ArgoCD (polling every 30s) picks it up
// and rolls the deployment, no manual kubectl step needed.
//
// Only keys actually passed in `values` get touched - anything omitted (or
// blank in the Jenkins parameter) is left exactly as it already is in git.
def call(Map values = [:]) {
    def valuesFile = 'charts/backend/values.yaml'
    def changed = []

    values.each { key, val ->
        if (val == null || val.toString().trim() == '') {
            return
        }
        // yq (not sed): these are nested under `config:`, and values can
        // contain characters (commas, slashes, cron expressions) that a
        // line-based sed replace would be too fragile to handle safely.
        sh "yq e '.config.${key} = \"${val}\"' -i ${valuesFile}"
        changed << "${key}=${val}"
    }

    if (changed.isEmpty()) {
        echo 'No config values provided - nothing to update.'
        return
    }

    currentBuild.displayName = "#${env.BUILD_NUMBER} config: ${changed.join(', ')}"

    withCredentials([usernamePassword(credentialsId: 'github-credentials', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
        sh """
            git config user.email 'jenkins@pinakaone.local'
            git config user.name 'Jenkins'
            git add ${valuesFile}
            git commit -m "backend config: ${changed.join(', ')}" || echo 'nothing to commit'
            git push https://\$GIT_USER:\$GIT_TOKEN@github.com/mohitjangale8/pinakaone-gitops.git HEAD:main
        """
    }

    echo "Updated ${changed.join(', ')} - ArgoCD will pick it up within ~30s."
}
