```groovy
def call() {
    dependencyCheck(
        odcInstallation: 'OWASP',
        additionalArguments: '--scan src --format HTML --format XML --out .'
    )

    dependencyCheckPublisher(
        pattern: 'dependency-check-report.xml',
        failedTotalHigh: 1,
        stopBuild: true
    )
}
```