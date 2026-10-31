
def call(
    String credentialsId,
    String severities,
    String exitCode
) {

    def services = env.SERVICES?.readLines()?.findAll { it.trim() } ?: []

    if (services.isEmpty()) {
        echo 'No microservices available. Skipping image scan.'
        return
    }

    withCredentials([
        usernamePassword(
            credentialsId: credentialsId,
            usernameVariable: 'DH_USER',
            passwordVariable: 'DH_TOKEN'
        )
    ]) {

        def dockerHubUser = env.DH_USER

        services.each { service ->

            def image = "${dockerHubUser}/${service}:${env.IMAGE_TAG}"

            echo "Scanning image: ${image}"

            sh """
                set -eu
                trivy image \
                    --severity '${severities}' \
                    --exit-code '${exitCode}' \
                    --ignore-unfixed \
                    '${image}'
            """
        }
    }
}
