def call(
    String installation,
    String projectKey,
    String projectName,
    String sources
) {
    withSonarQubeEnv(installation) {
        def scannerHome = tool installation

        sh """
            set -eu
            "\${scannerHome}/bin/sonar-scanner" \
              -Dsonar.projectKey="${projectKey}" \
              -Dsonar.projectName="${projectName}" \
              -Dsonar.sources="${sources}" \
              '-Dsonar.exclusions=**/*.java' \
              -Dsonar.sourceEncoding=UTF-8
        """
    }
}
