
def call(String imageTagLength) {

    // 1. Get current commit
    def currentCommit = sh(
        script: 'git rev-parse HEAD',
        returnStdout: true
    ).trim()

    // 2. Generate Docker image tag
    def shortCommit = sh(
        script: "git rev-parse --short=${imageTagLength} HEAD",
        returnStdout: true
    ).trim()

    env.IMAGE_TAG = "sha-${shortCommit}"

    // Default: CI will be skipped
    env.RUN_CI = 'false'

    // 3. Discover all available microservices
    def allServicesOutput = sh(
        script: '''
            set -eu
            find src -type f -name Dockerfile -print |
            sed -E 's#^src/([^/]+)(/.*)?/Dockerfile$#\\1#' |
            sort -u
        ''',
        returnStdout: true
    ).trim()

    if (!allServicesOutput) {
        error('No services found under src/*/Dockerfile')
    }

    def allServices = allServicesOutput.readLines()
    def selectedServices = []

    // 4. Find previous successful build commit
    def previousSuccessfulBuild = currentBuild.previousSuccessfulBuild

    def previousCommit = previousSuccessfulBuild
        ?.getBuildVariables()
        ?.get('GIT_COMMIT')

    // 5. First build: select all services
    if (!previousCommit) {

        echo 'First build detected. Running complete CI pipeline.'

        env.RUN_CI = 'true'
        selectedServices = allServices

    } else {

        // 6. Validate previous commit
        def commitExists = sh(
            script: "git cat-file -e '${previousCommit}^{commit}' 2>/dev/null",
            returnStatus: true
        ) == 0

        if (!commitExists) {
            error("Previous successful commit is unavailable: ${previousCommit}")
        }

        // 7. Detect changed files
        def changedFilesOutput = sh(
            script: "git diff --name-only '${previousCommit}' '${currentCommit}'",
            returnStdout: true
        ).trim()

        def changedFiles = changedFilesOutput
            ? changedFilesOutput.readLines()
            : []

        echo "Changed files: ${changedFiles}"

        // 8. Check whether anything changed inside src/
        def sourceChanged = changedFiles.any {
            it.startsWith('src/')
        }

        if (sourceChanged) {

            env.RUN_CI = 'true'

            echo 'Changes detected inside src/. Running CI pipeline.'

            // 9. Identify changed microservices
            def changedServices = changedFiles
                .findAll { it.startsWith('src/') }
                .collect { it.split('/')[1] }
                .unique()

            // 10. Select only changed microservices
            selectedServices = allServices.findAll {
                changedServices.contains(it)
            }

            if (selectedServices.isEmpty()) {
                echo 'Source files changed, but no individual microservice Dockerfile directory was identified.'
            }

        } else {

            env.RUN_CI = 'false'

            echo 'No changes detected inside src/. Skipping CI pipeline.'
        }
    }

    // 11. Export selected services
    env.SERVICES = selectedServices.join('\n')

    echo "RUN_CI: ${env.RUN_CI}"
    echo "Image tag: ${env.IMAGE_TAG}"
    echo "Selected services: ${env.SERVICES ?: 'No services selected'}"
}
