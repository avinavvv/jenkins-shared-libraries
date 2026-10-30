def call(String imageTagLength) {

    // Current commit
    def currentCommit = sh(
        script: 'git rev-parse HEAD',
        returnStdout: true
    ).trim()

    // Docker image tag
    env.IMAGE_TAG = "sha-${sh(
        script: "git rev-parse --short=${imageTagLength} HEAD",
        returnStdout: true
    ).trim()}"

    // Last successful Jenkins build ka commit
    def previousSuccessfulBuild = currentBuild.previousSuccessfulBuild
    def previousCommit = previousSuccessfulBuild
        ?.getBuildVariables()
        ?.get('GIT_COMMIT')

    // Available microservices discover karo
    def allServices = sh(
        script: '''
            set -eu
            find src -type f -name Dockerfile -print |
            sed -E 's#^src/([^/]+)(/.*)?/Dockerfile$#\\1#' |
            sort -u
        ''',
        returnStdout: true
    ).trim()

    if (!allServices) {
        error('No services found under src/*/Dockerfile')
    }

    def selectedServices

    if (!previousCommit) {
        // First run: all services
        echo 'First successful build not found. Selecting all services.'
        selectedServices = allServices.readLines()
    } else {
        // Last successful build se current commit tak changes
        def changedFiles = sh(
            script: "git diff --name-only ${previousCommit} ${currentCommit}",
            returnStdout: true
        ).trim()

        def changedServices = changedFiles
            ? changedFiles.readLines()
                .findAll { it.startsWith('src/') }
                .collect { it.split('/')[1] }
                .unique()
            : []

        selectedServices = allServices.readLines()
            .findAll { changedServices.contains(it) }

        echo "Changed services: ${selectedServices}"
    }

    env.SERVICES = selectedServices.join('\n')

    echo "Image tag: ${env.IMAGE_TAG}"
    echo "Selected services: ${env.SERVICES ?: 'No services changed'}"
}