def call(String credentialsId) {
    def services = env.SERVICES.readLines().findAll { it.trim() }

    withCredentials([
        usernamePassword(
            credentialsId: credentialsId,
            usernameVariable: 'DH_USER',
            passwordVariable: 'DH_TOKEN'
        )
    ]) {
        services.each { service ->
            def dockerfile = sh(
                script: "find 'src/${service}' -type f -name Dockerfile -print -quit",
                returnStdout: true
            ).trim()

            if (!dockerfile) {
                error("Dockerfile not found for ${service}")
            }

            def context = sh(
                script: "dirname '${dockerfile}'",
                returnStdout: true
            ).trim()

            def image = "${env.DH_USER}/${service}:${env.IMAGE_TAG}"

            echo "Building image: ${image}"

            sh """
                set -eu
                docker buildx build \
                  --load \
                  -f '${dockerfile}' \
                  -t '${image}' \
                  '${context}'
            """
        }
    }
}
