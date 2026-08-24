// Shared library step: downloads gitleaks once (cached under $JENKINS_HOME,
// which is bind-mounted from host - see pinakaone-iac user_data.sh.tpl -
// so it survives container/instance replacement, not just this one build)
// and scans the given checked-out directory's CURRENT file contents
// (--no-git, not full git history). A full-history scan would permanently
// fail every future build on any repo that ever had a secret sitting in a
// past commit, even one long since fixed - this only cares about what's
// actually present in the workspace right now. Fails the build (gitleaks
// exits non-zero, sh step throws) if it finds anything not covered by that
// repo's own .gitleaks.toml allowlist, if it has one.
def call(String dir) {
    def version = '8.30.1'
    def binDir  = "${env.JENKINS_HOME}/tools/gitleaks-${version}"
    def bin     = "${binDir}/gitleaks"

    stage('Secret scan') {
        sh """
            if [ ! -x '${bin}' ]; then
                mkdir -p '${binDir}'
                curl -sL https://github.com/gitleaks/gitleaks/releases/download/v${version}/gitleaks_${version}_linux_x64.tar.gz \
                    | tar -xz -C '${binDir}' gitleaks
                chmod +x '${bin}'
            fi
        """

        def configFlag = fileExists("${dir}/.gitleaks.toml") ? "--config ${dir}/.gitleaks.toml" : ''
        sh "'${bin}' detect --no-git --source '${dir}' -v ${configFlag}"
    }
}
