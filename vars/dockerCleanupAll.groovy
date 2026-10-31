
def call(String credentialsId) {

    def services = env.SERVICES?.readLines()?.findAll { it.trim() } ?: []

    withCredentials([
        usernamePassword(
            credentialsId: credentialsId,
            usernameVariable: 'DH_USER',
            passwordVariable: 'DH_TOKEN'
        )
    ]) {

        def dockerHubUser = env.DH_USER

        if (services.isEmpty()) {
            echo 'No images to clean.'
        } else {

            services.each { service ->

                def image = "${dockerHubUser}/${service}:${env.IMAGE_TAG}"

                echo "Removing local image: ${image}"

                sh "docker image rm -f '${image}' || true"
            }
        }

        echo 'Cleaning Docker Buildx cache.'

        sh 'docker buildx prune -af || true'

        echo 'Docker cleanup completed.'
    }
}
