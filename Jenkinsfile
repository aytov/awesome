#!groovy

def version = ''

pipeline {
    agent any
    options {
        timestamps()
    }

    stages {
        stage('Checkout') {
        steps {
            deleteDir()
            checkout scmGit(
                branches: [[name: 'main']],
                extensions: scm.extensions + [[$class: 'CloneOption', noTags: false, reference: '', shallow: true]],
                userRemoteConfigs: [[credentialsId: '6ecb9821-0237-4fdb-9a08-18baaa9cc5f8', url: 'https://github.com/aytov/awesome.git']])

//             deleteDir()
//             withCredentials([gitUsernamePassword(credentialsId: '6ecb9821-0237-4fdb-9a08-18baaa9cc5f8')]) {
//                 sh("git clone -b main https://github.com/aytov/awesome.git")
//             }

            sh "ls -lat"
        }
    }

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
                        customImage = docker.build("aytovan/awesome:v1.0.2")
                        customImage.push()
                        customImage.push('latest'
                    }
                }
            }
        }
    }
}
