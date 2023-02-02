#!groovy

pipelineJob('product-job') {
    definition {
        cps {
            script(readFileFromWorkspace('env/prod.jenkinsfile'))
            sandbox()
        }
    }
}
