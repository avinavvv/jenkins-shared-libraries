def call() {
    sh '''
        set -eu
        trivy fs \
          --scanners vuln,misconfig,secret \
          --severity HIGH,CRITICAL \
          --exit-code 0 \
          .
    '''
}