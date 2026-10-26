def call() {
    dependencyCheck(
        odcInstallation: 'OWASP-Dependency-Check',
        additionalArguments: '''
            --scan src
            --format HTML
            --format XML
            --out .
            --failOnCVSS 7
        '''.trim().replaceAll('\\s+', ' ')
    )

    dependencyCheckPublisher(
        pattern: 'dependency-check-report.xml',
        failedTotalHigh: 1,
        stopBuild: true
    )
}