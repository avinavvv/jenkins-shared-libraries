def call(String projectKey, String projectName, String sourceDir) {
    withSonarQubeEnv('Sonar') {
        def scannerHome = tool 'Sonar'

        sh """
            set -eu
            "${scannerHome}/bin/sonar-scanner" \
              -Dsonar.projectKey="${projectKey}" \
              -Dsonar.projectName="${projectName}" \
              -Dsonar.sources="${sourceDir}" \
              -Dsonar.sourceEncoding=UTF-8
        """
    }
}