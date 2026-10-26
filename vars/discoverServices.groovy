def call(String imageTagLength) {
    env.IMAGE_TAG = sh(
        script: "git rev-parse --short=${imageTagLength} HEAD",
        returnStdout: true
    ).trim()

    env.SERVICES = sh(
        script: '''
            set -eu
            find src -type f -name Dockerfile -print |
            sed -E 's#^src/([^/]+)(/.*)?/Dockerfile$#\\1#' |
            sort -u
        ''',
        returnStdout: true
    ).trim()

    if (!env.SERVICES?.trim()) {
        error('No services found under src/*/Dockerfile')
    }

    echo "Image tag: ${env.IMAGE_TAG}"
    echo "Services: ${env.SERVICES}"
}
