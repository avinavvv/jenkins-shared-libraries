
def call() {
    return sh(
        script: '''
            set -eu
            find src -type f -iname 'Dockerfile' -print |
            sed -E 's#^src/([^/]+)(/.*)?/Dockerfile$#\\1#' |
            sort -u
        ''',
        returnStdout: true
    ).trim()
}
