def call(String installation) {
    dependencyCheckAnalyze(
        odcInstallation: installation,
        additionalArguments: '--scan . --format HTML,XML'
    )

    dependencyCheckPublisher(
        pattern: '**/dependency-check-report.xml'
    )
}

