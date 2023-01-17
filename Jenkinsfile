#!groovy

def version = ''

pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                script {
                    version = sh(returnStdout: true, script: "git describe --tags").trim()
                }
                sh "npm ci"
                sh 'npm version | grep "$(git describe --tags | sed "s/-.*//")" && echo "package version in sync with git tags" || (echo "[ERROR] FAILURE: Package version is _OUT_OF_SYNC_ with git tags" && exit 99)'

                sh "npm prune --production"
                stash includes: '**/*', name: 'node_package'
            }
        }

        stage('Docker publish') {
            steps {
                unstash 'node_package'
                script {
                    def customImage
                    docker.withRegistry('https://registry.hub.docker.com', '96a9e243-0ce6-4a78-b18d-b846ca3939e3') {
                        echo "Building custom image"
                        customImage = docker.build("aytovan/awesome:${version}")
                        customImage.push()
                        customImage.push('latest')
                    }
                }
            }
        }
    }
}
