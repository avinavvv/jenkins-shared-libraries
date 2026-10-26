def call() {
    withCredentials([
        usernamePassword(
            credentialsId: env.DOCKERHUB_CREDENTIALS,
            usernameVariable: 'DH_USER',
            passwordVariable: 'DH_TOKEN'
        )
    ]) {
        def services = env.SERVICES.readLines().findAll { it.trim() }

        services.each { service ->
            def image = "${env.DH_USER}/${service}:${env.IMAGE_TAG}"

            echo "Building image: ${image}"

            sh """
                set -eu
                docker build \
                  -t '${image}' \
                  'src/${service}'
            """
        }
    }
}