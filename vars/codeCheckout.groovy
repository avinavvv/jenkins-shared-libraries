def call(String repositoryUrl, String branch) {
    git branch: branch, url: repositoryUrl
}