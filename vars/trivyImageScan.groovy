def call(
    String credentialsId,
    String severities,
    String exitCode
) {
    def services = env.SERVICES.readLines().findAll { it.trim() }

    withCredentials([
        usernamePassword(
            credentialsId: credentialsId,
            usernameVariable: 'DH_USER',
            passwordVariable: 'DH_TOKEN'
        )
    ]) {
        services.each { service ->
            def image = "${env.DH_USER}/${service}:${env.IMAGE_TAG}"

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
