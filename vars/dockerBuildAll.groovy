
def call() {
    def services = env.SERVICES.readLines().findAll { it.trim() }

    withCredentials([
        usernamePassword(
            credentialsId: env.DOCKERHUB_CREDENTIALS,
            usernameVariable: 'DH_USER',
            passwordVariable: 'DH_TOKEN'
        )
    ]) {
        services.each { service ->
            def dockerfile = sh(
                script: """
                    find 'src/${service}' -type f -iname Dockerfile -print -quit
                """,
                returnStdout: true
            ).trim()

            if (!dockerfile) {
                error("Dockerfile not found for service: ${service}")
            }

            def image = "${env.DH_USER}/${service}:${env.IMAGE_TAG}"
            def context = "src/${service}"

            echo "Building image: ${image}"
            echo "Dockerfile: ${dockerfile}"
            echo "Build context: ${context}"

            sh """
                set -eu
                docker build \
                  -f '${dockerfile}' \
                  -t '${image}' \
                  '${context}'
            """
        }
    }
}
