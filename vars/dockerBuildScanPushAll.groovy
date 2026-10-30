def call(
    String credentialsId,
    String severities,
    String exitCode
) {
    def services = env.SERVICES?.readLines()?.findAll { it.trim() } ?: []

    if (services.isEmpty()) {
        echo 'No changed microservices. Skipping.'
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

                try {
                    echo "Building: ${image}"

                    sh """
                        set -eu
                        docker buildx build \
                            --load \
                            -f '${dockerfile}' \
                            -t '${image}' \
                            '${context}'
                    """

                    echo "Scanning: ${image}"

                    sh """
                        set -eu
                        trivy image \
                            --severity '${severities}' \
                            --exit-code '${exitCode}' \
                            --ignore-unfixed \
                            '${image}'
                    """

                    echo "Pushing: ${image}"
                    sh "docker push '${image}'"

                } finally {
                    echo "Cleaning local image: ${image}"

                    sh """
                        docker image rm -f '${image}' || true
                        docker buildx prune -af || true
                    """
                }
            }

        } finally {
            sh 'docker logout >/dev/null 2>&1 || true'
        }
    }
}