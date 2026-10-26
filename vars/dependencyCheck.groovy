def call(String installation) {
    sh """
        dependency-check.sh \
          --scan . \
          --format HTML \
          --format XML \
          --out .
    """

    dependencyCheckPublisher(
        pattern: '**/dependency-check-report.xml'
    )
}
