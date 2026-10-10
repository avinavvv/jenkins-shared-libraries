def call(String Project, String ImageTag, String dockerhubuser) {
    withCredentials([usernamePassword(credentialsId: 'dockerhubCred', passwordVariable: 'dockerHubPass', usernameVariable: 'dockerHubUser')]) {
        sh 'echo $dockerHubPass | docker login -u $dockerHubUser --password-stdin'
    }
    sh "docker push ${dockerhubuser}/${Project}:${ImageTag}"
}
