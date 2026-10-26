def call(String installation) {
    dependencyCheck(
        odcInstallation: installation,
        additionalArguments: '--scan . --format HTML,XML'
    )

    dependencyCheckPublisher(
        pattern: '**/dependency-check-report.xml'
    )
}
