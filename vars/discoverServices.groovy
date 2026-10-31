
def call(String imageTagLength) {

    // 1. Current commit
    def currentCommit = sh(
        script: 'git rev-parse HEAD',
        returnStdout: true
    ).trim()

    // 2. Docker image tag
    def shortCommit = sh(
        script: "git rev-parse --short=${imageTagLength} HEAD",
        returnStdout: true
    ).trim()

    env.IMAGE_TAG = "sha-${shortCommit}"

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

    // 5. First build or missing baseline: build all services
    if (!previousCommit) {

        echo 'No previous successful commit found. Selecting all services.'
        selectedServices = allServices

    } else {

        // 6. Validate previous commit exists in checkout
        def commitExists = sh(
            script: "git cat-file -e '${previousCommit}^{commit}' 2>/dev/null",
            returnStatus: true
        ) == 0

        if (!commitExists) {

            echo 'Previous commit is unavailable. Selecting all services.'
            selectedServices = allServices

        } else {

            // 7. Find changed files
            def changedFilesOutput = sh(
                script: "git diff --name-only '${previousCommit}' '${currentCommit}'",
                returnStdout: true
            ).trim()

            def changedFiles = changedFilesOutput
                ? changedFilesOutput.readLines()
                : []

            echo "Changed files: ${changedFiles}"

            // 8. Select changed microservices
            def changedServices = changedFiles
                .findAll { it.startsWith('src/') }
                .collect { it.split('/')[1] }
                .unique()

            // 9. Shared/root build configuration changes
            def sharedChanges = changedFiles.any { file ->
                !file.startsWith('src/') &&
                (
                    file == 'Jenkinsfile' ||
                    file == '.dockerignore' ||
                    file == 'Dockerfile' ||
                    file == 'docker-compose.yml'
                )
            }

            // Changes inside src but outside discovered service folders
            def unknownSrcChanges = changedFiles.any { file ->
                if (!file.startsWith('src/')) {
                    return false
                }

                def parts = file.split('/')
                return parts.length < 2 ||
                    !allServices.contains(parts[1])
            }

            if (sharedChanges || unknownSrcChanges) {

                echo 'Shared or unknown source changes detected. Selecting all services.'
                selectedServices = allServices

            } else {

                selectedServices = allServices.findAll {
                    changedServices.contains(it)
                }
            }
        }
    }

    // 10. Export selected services
    env.SERVICES = selectedServices.join('\n')

    echo "Image tag: ${env.IMAGE_TAG}"
    echo "Selected services: ${env.SERVICES ?: 'No services changed'}"
}
