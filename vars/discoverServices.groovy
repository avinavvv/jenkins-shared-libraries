def call() {
    return sh(
        script: '''
            set -eu
            find src -mindepth 2 -maxdepth 2 \
                -type f -name Dockerfile \
                -print |
            sed 's#^src/##; s#/Dockerfile$##' |
            sort -u
        ''',
        returnStdout: true
    ).trim()
}