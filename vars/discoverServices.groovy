
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
    env.RUN_CI = 'false'

    // 3. Discover all microservices
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

    // 4. Get previous successful build
    def previousBuild = currentBuild.previousSuccessfulBuild

    // 5. Read baseline commit from previous build description
    def previousDescription = previousBuild?.description ?: ''

    def baselineLine = previousDescription.readLines().find {
        it.startsWith('CI_BASELINE_COMMIT=')
    }

    def previousCommit = baselineLine
        ? baselineLine.substring('CI_BASELINE_COMMIT='.length()).trim()
        : null

    // Fallback for older builds
    if (!previousCommit) {
        previousCommit = previousBuild
            ?.getBuildVariables()
            ?.get('GIT_COMMIT')
    }

    echo "Previous successful build: ${previousBuild?.number ?: 'None'}"
    echo "Previous successful commit: ${previousCommit ?: 'Not found'}"

    // 6. No baseline found
    if (!previousCommit) {

        echo 'No previous successful commit baseline found. Running full CI.'

        env.RUN_CI = 'true'
        selectedServices = allServices

    } else {

        // 7. Validate previous commit
        def commitExists = sh(
            script: "git cat-file -e '${previousCommit}^{commit}' 2>/dev/null",
            returnStatus: true
        ) == 0

        if (!commitExists) {
            error("Previous successful commit is unavailable: ${previousCommit}")
        }

        // 8. Detect changed files
        def changedFilesOutput = sh(
            script: "git diff --name-only '${previousCommit}' '${currentCommit}'",
            returnStdout: true
        ).trim()

        def changedFiles = changedFilesOutput
            ? changedFilesOutput.readLines()
            : []

        echo "Previous successful commit: ${previousCommit}"
        echo "Current commit: ${currentCommit}"
        echo "Changed files: ${changedFiles}"

        // 9. Detect source changes
        def sourceFiles = changedFiles.findAll {
            it.startsWith('src/')
        }

        if (sourceFiles) {

            env.RUN_CI = 'true'

            def changedServices = sourceFiles
                .collect { it.split('/')[1] }
                .unique()

            selectedServices = allServices.findAll {
                changedServices.contains(it)
            }

            echo "Changed microservices: ${changedServices}"

            if (selectedServices.isEmpty()) {
                echo 'Source changes detected, but no buildable microservice was identified.'
            }

        } else {

            echo 'No changes detected inside src/. Skipping CI pipeline.'
        }
    }

    // 10. Export selected services
    env.SERVICES = selectedServices.join('\n')

    // Save current commit in this build's description.
    // This becomes the baseline only if the build succeeds.
    currentBuild.description = "CI_BASELINE_COMMIT=${currentCommit}"

    echo "RUN_CI: ${env.RUN_CI}"
    echo "Image tag: ${env.IMAGE_TAG}"
    echo "Selected services: ${env.SERVICES ?: 'No services selected'}"
}
