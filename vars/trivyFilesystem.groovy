def call(String severities, String exitCode) {
    sh """
        set -eu
        trivy fs \
          --scanners vuln,misconfig,secret \
          --severity '${severities}' \
          --exit-code '${exitCode}' \
          .
    """
}
