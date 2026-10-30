def call(String credentialsId) {

    def services = env.SERVICES?.readLines()?.findAll { it.trim() } ?: []

    if (services.isEmpty()) {
        echo 'No changed microservices. Skipping Docker push.'
        return
    }

    withCredentials([
        usernamePassword(
            credentialsId: credentialsId,
            usernameVariable: 'DH_USER',
            passwordVariable: 'DH_TOKEN'
        )
    ]) {
        try {
            sh '''
                set -eu
                echo "$DH_TOKEN" | docker login \
                  --username "$DH_USER" \
                  --password-stdin
            '''

            services.each { service ->
                def image = "${env.DH_USER}/${service}:${env.IMAGE_TAG}"

                echo "Pushing image: ${image}"
                sh "docker push '${image}'"
            }
        } finally {
            sh 'docker logout >/dev/null 2>&1 || true'
        }
    }
}
