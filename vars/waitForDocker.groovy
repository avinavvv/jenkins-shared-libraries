def call() {
    timeout(time: 5, unit: 'MINUTES') {
        waitUntil {
            def ready = sh(
                script: 'docker info >/dev/null 2>&1',
                returnStatus: true
            ) == 0

            if (!ready) {
                echo 'Waiting for Docker daemon...'
            }

            return ready
        }
    }

    echo 'Docker daemon is ready.'
    sh 'docker info'
}