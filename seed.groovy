String provider = 'aws'
String product = 'microservices'
// display name on Jenkins
String serviceDisplayName = ''
String basePath = ''

// docelowo dać generyczny:
String serviceRepoAddress = ""

//path to jenkins files on serviceRepoAddress
String jenkinsFiles = "pipelines"
String serviceDockerRepoAddressDev = ""
String serviceDockerRepoAddressPrd = ""

//gatling settings
String gatlingSimulationName = "zone_processor_constant_load"
String gatlingEnvironment = "ZONE_PROCESSOR"
String gatlingSimulationClassName = "ZoneProcessorSimulation"
String gatlingTraceIdNumber = "ZONE_PROCESSOR_PERFORMANCE_TESTS"

def regions = [ 'eu-west-1', 'us-east-2' ]
def jobPath = "products/${product}"

def envConfigs = [
        'dev':[
                'slaveLabel': 'dev-gll-playground',
                'awsAccount': 'playground',
                'customers': [ 'shd' ],
                'dockerRepoName': "$serviceDockerRepoAddressDev"
        ],
        'int':[
                'slaveLabel': 'techops-staging-cbc',
                'awsAccount': 'staging',
                'customers': [ 'shd' ],
                'dockerRepoName': "$serviceDockerRepoAddressDev"
        ],
        'pft':[
                'slaveLabel': 'techops-staging-cbc',
                'awsAccount': 'staging',
                'customers': [ 'shd' ],
                'dockerRepoName': "$serviceDockerRepoAddressDev"
        ],
        'sbx':[
                'slaveLabel': 'techops-staging-cbc',
                'awsAccount': 'staging',
                'customers': [ 'shd' ],
                'dockerRepoName': "$serviceDockerRepoAddressPrd"
        ],
        'prd':[
                'slaveLabel': 'techops-production-cbc',
                'awsAccount': 'production',
                'customers': [ 'shd' ],
                'dockerRepoName': "$serviceDockerRepoAddressPrd"
        ]
]

folder(jobPath)

//Create sub folders
folder("${jobPath}/${serviceDisplayName}")

for (envConfig in envConfigs) {
    folder("${jobPath}/${serviceDisplayName}/${envConfig.key}"){
        displayName("${envConfig.key}")
        description("${envConfig.key} jobs")
    }

    for (region in regions) {
        folder("${jobPath}/${serviceDisplayName}/${envConfig.key}/${region}"){
            displayName("${region}")
            description("${envConfig.key} ${region} jobs")
        }

        for (customer in envConfig.value.customers){
            folder("${jobPath}/${serviceDisplayName}/${envConfig.key}/${region}/${customer}"){
                displayName("${customer}")
                description("${envConfig.key} ${region} ${customer} jobs")
            }
        }
    }
}

//Create deployment job
for (envConfig in envConfigs) {
    for (region in regions) {
        for (customer in envConfig.value.customers) {
            pipelineJob("${jobPath}/${serviceDisplayName}/${envConfig.key}/${region}/${customer}/infrastructure") {
                displayName("Create infrastructure")
                description("Create infrastructure")

                logRotator {
                    numToKeep(10)
                }

                label(envConfig.value.slaveLabel)

                definition {
                    cpsScm {
                        scm {
                            git {
                                remote {
                                    name 'origin'
                                    url "$serviceRepoAddress"
                                    branch '$SERVICE_REPO_BRANCH_OR_COMMIT'
                                    credentials 'bitbucket-credentials'
                                }
                            }
                        }
                        scriptPath("$jenkinsFiles/createInfrastructure.jenkinsfile")
                    }
                    concurrentBuild(false)
                }

                environmentVariables {
                    env('ENV', envConfig.key)
                    env('REGION', region)
                    env('PRODUCT', product)
                    env('PROVIDER', provider)
                    env('region', region)
                    env('JENKINS_SLAVE_LABEL', envConfig.value.slaveLabel)
                    env('AWS_ACCOUNT', envConfig.value.awsAccount)
                    env('CUSTOMER', customer)
                    env('SERVICE_REPO_ADDRESS', serviceRepoAddress)
                    env('DOCKER_REPO_NAME', envConfig.value.dockerRepoName)
                }

                parameters {
                    stringParam('DOCKER_TAG', '', 'A docker image to be deployed')
                    stringParam('SERVICE_REPO_BRANCH_OR_COMMIT', '', 'Required name of the branch or identifier of the commit from which docker image has been built')
                    stringParam('TASK_COUNT', '', '[OPTIONAL] If provided then not calculate automaticly tasks')
                    choiceParam('Carrier', carriers, 'Carrier to deploy')

                    if("${envConfig.value.awsAccount}" == "playground" || "${envConfig.value.awsAccount}" == "staging"){
                        stringParam('BRANCH_ID','','Name of enviorement to set up. Optional.')
                        booleanParam('PUBLIC_ACCESS', false, 'Environment should be publicly accessible')
                    }
                }
            }

            pipelineJob("${jobPath}/${serviceDisplayName}/${envConfig.key}/${region}/${customer}/destroy-infrastructure") {
                displayName("Destroy infrastructure")
                description("Destroy infrastructure")

                logRotator {
                    numToKeep(10)
                }

                label(envConfig.value.slaveLabel)

                definition {
                    cpsScm {
                        scm {
                            git {
                                remote {
                                    name 'origin'
                                    url "$serviceRepoAddress"
                                    branch '$SERVICE_REPO_BRANCH_OR_COMMIT'
                                    credentials 'bitbucket-credentials'
                                }
                            }
                        }
                        scriptPath("$jenkinsFiles/destroyInfrastructure.jenkinsfile")
                    }
                    concurrentBuild(false)
                }

                environmentVariables {
                    env('ENV', envConfig.key)
                    env('REGION', region)
                    env('PRODUCT', product)
                    env('PROVIDER', provider)
                    env('region', region)
                    env('JENKINS_SLAVE_LABEL', envConfig.value.slaveLabel)
                    env('AWS_ACCOUNT', envConfig.value.awsAccount)
                    env('CUSTOMER', customer)
                    env('SERVICE_REPO_ADDRESS', serviceRepoAddress)
                }

                parameters {
                    stringParam('SERVICE_REPO_BRANCH_OR_COMMIT', '', 'Required name of the branch or identifier of the commit from which docker image has been built')
                    booleanParam('AUTO_APPROVE', false, 'Automatic approval of destroying an environment')
                    booleanParam('DESTROY_NEW_RELIC', false, 'Destroy new relic dashboard')
                    choiceParam('Carrier', carriers, 'Carrier to deploy')

                    if("${envConfig.value.awsAccount}" == "playground" || "${envConfig.value.awsAccount}" == "staging"){
                        stringParam('BRANCH_ID','','Name of enviorement to set up. Optional.')
                    }
                }
            }

            if (envConfig.key == "dev" || envConfig.key == "int")
            {
                pipelineJob("${jobPath}/${serviceDisplayName}/${envConfig.key}/${region}/${customer}/integration-tests") {
                    displayName("Run integration tests")
                    description("Run integration tests")

                    logRotator {
                        numToKeep(10)
                    }

                    label(envConfig.value.slaveLabel)

                    definition {
                        cpsScm {
                            scm {
                                git {
                                    remote {
                                        name 'origin'
                                        url "$serviceRepoAddress"
                                        branch '$SERVICE_REPO_BRANCH_OR_COMMIT'
                                        credentials 'bitbucket-credentials'
                                    }
                                }
                            }
                            scriptPath("$jenkinsFiles/integrationTests.jenkinsfile")
                        }
                        concurrentBuild(false)
                    }

                    environmentVariables {
                        env('ENV', envConfig.key)
                        env('REGION', region)
                        env('PRODUCT', product)
                        env('region', region)
                        env('AWS_ACCOUNT', envConfig.value.awsAccount)
                        env('CUSTOMER', customer)
                        env('JENKINS_SLAVE_LABEL', envConfig.value.slaveLabel)
                        env('SERVICE_REPO_ADDRESS', serviceRepoAddress)
                        env('SERVICE_DISPLAY_NAME', serviceDisplayName)
                    }

                    parameters {
                        stringParam('SERVICE_ENDPOINT', "http://api-${testsCarrierUuid}.zone.${region}.${envConfig.key}.aws.api.io", 'Address of the running service to be tested')
                        stringParam('SERVICE_REPO_BRANCH_OR_COMMIT', '', 'The name of the branch or identifier of the commit from which the test definition will be taken')
                        stringParam('REPORTING_DB_ENABLED', 'true', 'Enable/Disable save tests reports to the reporting database')
                        stringParam('REPORTING_DB_JOB_ID', '', 'Job ID for identify test in reporting database')
                        stringParam('REPORTING_DB_APPLICATION_NAME', "${serviceDisplayName}", 'Application name for reporting database')
                        stringParam('REPORTING_DB_APPLICATION_VERSION', '', 'Application version for reporting database')
                    }
                }
            }

            if (envConfig.key == "dev" || envConfig.key == "pft")
            {
                pipelineJob("${jobPath}/${serviceDisplayName}/${envConfig.key}/${region}/${customer}/performance-tests") {
                    displayName("Run performance tests")
                    description("Run performance tests")

                    logRotator {
                        numToKeep(10)
                    }

                    label(envConfig.value.slaveLabel)

                    definition {
                        cpsScm {
                            scm {
                                git {
                                    remote {
                                        name 'origin'
                                        url "$serviceRepoAddress"
                                        branch '$SERVICE_REPO_BRANCH_OR_COMMIT'
                                        credentials 'bitbucket-credentials'
                                    }
                                }
                            }
                            scriptPath("$jenkinsFiles/performanceTests.jenkinsfile")
                        }
                        concurrentBuild(false)
                    }

                    environmentVariables {
                        env('ENV', envConfig.key)
                        env('REGION', region)
                        env('PRODUCT', product)
                        env('region', region)
                        env('AWS_ACCOUNT', envConfig.value.awsAccount)
                        env('CUSTOMER', customer)
                        env('JENKINS_SLAVE_LABEL', envConfig.value.slaveLabel)
                        env('SERVICE_REPO_ADDRESS', serviceRepoAddress)
                    }

                    parameters {
                        def scenarios = """[
                                {
                                    "scenarioOverrides": "-DbucketName=${envConfig.key}-${region}-microservices -Dduration=1800 -Dusers=1 -DrequestsPerUserAtOnce=30 -Denvironment=${gatlingEnvironment} -Dgatling.core.directory.resources=/tmp/artifacts -Dgatling.core.directory.results=/tmp/artifacts -Dgatling.core.simulationClass=${gatlingSimulationClassName} -Dgatling.core.directory.simulations=/mpm-tests/tests/gatling/zone-processor/simulations/ -Dgatling.http.ahc.requestTimeout=5000 -DtraceIdNumber=${gatlingTraceIdNumber} -Dendpoint=http://prv.processor-${testsCarrierUuid}.zone.${region}.${envConfig.key}.aws.apick.io",
                                    "scenarioConfigFile": "/mpm-tests/tests/gatling/zone-processor/simulations/${gatlingSimulationName}.yml"
                                },
                                {
                                    "scenarioOverrides": "-DbucketName=${envConfig.key}-${region}-microservices -Dduration=1800 -Dusers=1 -DrequestsPerUserAtOnce=30 -Denvironment=${gatlingEnvironment} -Dgatling.core.directory.resources=/tmp/artifacts -Dgatling.core.directory.results=/tmp/artifacts -Dgatling.core.simulationClass=${gatlingSimulationClassName} -Dgatling.core.directory.simulations=/mpm-tests/tests/gatling/zone-processor/simulations/ -Dgatling.http.ahc.requestTimeout=5000 -DtraceIdNumber=${gatlingTraceIdNumber} -Dendpoint=http://prv.processor-${testsCarrierUuid}.zone.${region}.${envConfig.key}.aws.apick.io",
                                    "scenarioConfigFile": "/mpm-tests/tests/gatling/zone-processor/simulations/${gatlingSimulationName}.yml"
                                },
                                {
                                    "scenarioOverrides": "-DbucketName=${envConfig.key}-${region}-microservices -Dduration=1800 -Dusers=1 -DrequestsPerUserAtOnce=30 -Denvironment=${gatlingEnvironment} -Dgatling.core.directory.resources=/tmp/artifacts -Dgatling.core.directory.results=/tmp/artifacts -Dgatling.core.simulationClass=${gatlingSimulationClassName} -Dgatling.core.directory.simulations=/mpm-tests/tests/gatling/zone-processor/simulations/ -Dgatling.http.ahc.requestTimeout=5000 -DtraceIdNumber=${gatlingTraceIdNumber} -Dendpoint=http://prv.processor-${testsCarrierUuid}.zone.${region}.${envConfig.key}.aws.apick.io",
                                    "scenarioConfigFile": "/mpm-tests/tests/gatling/zone-processor/simulations/${gatlingSimulationName}.yml"
                                },
                                {
                                    "scenarioOverrides": "-DbucketName=${envConfig.key}-${region}-microservices -Dduration=1800 -Dusers=1 -DrequestsPerUserAtOnce=30 -Denvironment=${gatlingEnvironment} -Dgatling.core.directory.resources=/tmp/artifacts -Dgatling.core.directory.results=/tmp/artifacts -Dgatling.core.simulationClass=${gatlingSimulationClassName} -Dgatling.core.directory.simulations=/mpm-tests/tests/gatling/zone-processor/simulations/ -Dgatling.http.ahc.requestTimeout=5000 -DtraceIdNumber=${gatlingTraceIdNumber} -Dendpoint=http://prv.processor-${testsCarrierUuid}.zone.${region}.${envConfig.key}.aws.apick.io",
                                    "scenarioConfigFile": "/mpm-tests/tests/gatling/zone-processor/simulations/${gatlingSimulationName}.yml"
                                },
                                {
                                    "scenarioOverrides": "-DbucketName=${envConfig.key}-${region}-microservices -Dduration=1800 -Dusers=1 -DrequestsPerUserAtOnce=30 -Denvironment=${gatlingEnvironment} -Dgatling.core.directory.resources=/tmp/artifacts -Dgatling.core.directory.results=/tmp/artifacts -Dgatling.core.simulationClass=${gatlingSimulationClassName} -Dgatling.core.directory.simulations=/mpm-tests/tests/gatling/zone-processor/simulations/ -Dgatling.http.ahc.requestTimeout=5000 -DtraceIdNumber=${gatlingTraceIdNumber} -Dendpoint=http://prv.processor-${testsCarrierUuid}.zone.${region}.${envConfig.key}.aws.apick.io",
                                    "scenarioConfigFile": "/mpm-tests/tests/gatling/zone-processor/simulations/${gatlingSimulationName}.yml"
                                },
                                {
                                    "scenarioOverrides": "-DbucketName=${envConfig.key}-${region}-microservices -Dduration=1800 -Dusers=1 -DrequestsPerUserAtOnce=30 -Denvironment=${gatlingEnvironment} -Dgatling.core.directory.resources=/tmp/artifacts -Dgatling.core.directory.results=/tmp/artifacts -Dgatling.core.simulationClass=${gatlingSimulationClassName} -Dgatling.core.directory.simulations=/mpm-tests/tests/gatling/zone-processor/simulations/ -Dgatling.http.ahc.requestTimeout=5000 -DtraceIdNumber=${gatlingTraceIdNumber} -Dendpoint=http://prv.processor-${testsCarrierUuid}.zone.${region}.${envConfig.key}.aws.apick.io",
                                    "scenarioConfigFile": "/mpm-tests/tests/gatling/zone-processor/simulations/${gatlingSimulationName}.yml"
                                },
                                {
                                    "scenarioOverrides": "-DbucketName=${envConfig.key}-${region}-microservices -Dduration=1800 -Dusers=1 -DrequestsPerUserAtOnce=30 -Denvironment=${gatlingEnvironment} -Dgatling.core.directory.resources=/tmp/artifacts -Dgatling.core.directory.results=/tmp/artifacts -Dgatling.core.simulationClass=${gatlingSimulationClassName} -Dgatling.core.directory.simulations=/mpm-tests/tests/gatling/zone-processor/simulations/ -Dgatling.http.ahc.requestTimeout=5000 -DtraceIdNumber=${gatlingTraceIdNumber} -Dendpoint=http://prv.processor-${testsCarrierUuid}.zone.${region}.${envConfig.key}.aws.apick.io",
                                    "scenarioConfigFile": "/mpm-tests/tests/gatling/zone-processor/simulations/${gatlingSimulationName}.yml"
                                },
                                {
                                    "scenarioOverrides": "-DbucketName=${envConfig.key}-${region}-microservices -Dduration=1800 -Dusers=1 -DrequestsPerUserAtOnce=30 -Denvironment=${gatlingEnvironment} -Dgatling.core.directory.resources=/tmp/artifacts -Dgatling.core.directory.results=/tmp/artifacts -Dgatling.core.simulationClass=${gatlingSimulationClassName} -Dgatling.core.directory.simulations=/mpm-tests/tests/gatling/zone-processor/simulations/ -Dgatling.http.ahc.requestTimeout=5000 -DtraceIdNumber=${gatlingTraceIdNumber} -Dendpoint=http://prv.processor-${testsCarrierUuid}.zone.${region}.${envConfig.key}.aws.apick.io",
                                    "scenarioConfigFile": "/mpm-tests/tests/gatling/zone-processor/simulations/${gatlingSimulationName}.yml"
                                },
                                {
                                    "scenarioOverrides": "-DbucketName=${envConfig.key}-${region}-microservices -Dduration=1800 -Dusers=1 -DrequestsPerUserAtOnce=30 -Denvironment=${gatlingEnvironment} -Dgatling.core.directory.resources=/tmp/artifacts -Dgatling.core.directory.results=/tmp/artifacts -Dgatling.core.simulationClass=${gatlingSimulationClassName} -Dgatling.core.directory.simulations=/mpm-tests/tests/gatling/zone-processor/simulations/ -Dgatling.http.ahc.requestTimeout=5000 -DtraceIdNumber=${gatlingTraceIdNumber} -Dendpoint=http://prv.processor-${testsCarrierUuid}.zone.${region}.${envConfig.key}.aws.apick.io",
                                    "scenarioConfigFile": "/mpm-tests/tests/gatling/zone-processor/simulations/${gatlingSimulationName}.yml"
                                },
                                {
                                    "scenarioOverrides": "-DbucketName=${envConfig.key}-${region}-microservices -Dduration=1800 -Dusers=1 -DrequestsPerUserAtOnce=30 -Denvironment=${gatlingEnvironment} -Dgatling.core.directory.resources=/tmp/artifacts -Dgatling.core.directory.results=/tmp/artifacts -Dgatling.core.simulationClass=${gatlingSimulationClassName} -Dgatling.core.directory.simulations=/mpm-tests/tests/gatling/zone-processor/simulations/ -Dgatling.http.ahc.requestTimeout=5000 -DtraceIdNumber=${gatlingTraceIdNumber} -Dendpoint=http://prv.processor-${testsCarrierUuid}.zone.${region}.${envConfig.key}.aws.apick.io",
                                    "scenarioConfigFile": "/mpm-tests/tests/gatling/zone-processor/simulations/${gatlingSimulationName}.yml"
                                }
                            ]"""
                        if("${envConfig.value.awsAccount}" == "playground"){
                            stringParam('ENDPOINT', "http://prv.processor-${testsCarrierUuid}.zone.${region}.${envConfig.key}.aws.apick.io", 'Endpoint')
                            stringParam('SERVICE_REPO_BRANCH_OR_COMMIT', '', 'Required name of the branch or identifier of the commit from sequence service repository')
                            stringParam('EXECUTOR_TYPE', 'Gatling', 'Executor type')
                            stringParam('BRANCH', 'master', 'Branch')
                            textParam('SCENARIOS', scenarios, 'Scenarios')
                        } else if("${envConfig.value.awsAccount}" == "staging"){
                            stringParam('ENDPOINT', "http://prv.processor-${testsCarrierUuid}.zone.${region}.${envConfig.key}.aws.apick.io", 'Endpoint')
                            stringParam('SERVICE_REPO_BRANCH_OR_COMMIT', '', 'Required name of the branch or identifier of the commit from sequence service repository')
                            stringParam('EXECUTOR_TYPE', 'Gatling', 'Executor type')
                            stringParam('DOCKER_IMAGE_TAG', 'latest', 'Tag of the docker image for running the tests')
                            stringParam('TEST_DESCRIPTION', 'Performance tests', 'Describe why you are running these tests. Example: "Production data, NewRelic Off, 100 Users"')
                            textParam('SCENARIOS', scenarios, 'Scenarios')
                        }
                    }
                }
            }

            if (envConfig.key == 'prd')
            {
                pipelineJob("${jobPath}/${serviceDisplayName}/${envConfig.key}/${region}/${customer}/continuous-delivery") {
                    displayName("Production continuous delivery")
                    description("Runs integration, performance tests and creates docker image ready for deploy on production.")

                    logRotator {
                        numToKeep(10)
                    }

                    label(envConfig.value.slaveLabel)

                    definition {
                        cpsScm {
                            scm {
                                git {
                                    remote {
                                        name 'origin'
                                        url "$serviceRepoAddress"
                                        branch '$SERVICE_REPO_BRANCH_OR_COMMIT'
                                        credentials 'bitbucket-credentials'
                                    }
                                }
                            }
                            scriptPath("$jenkinsFiles/continuousDelivery.jenkinsfile")
                        }
                        concurrentBuild(false)
                    }

                    environmentVariables {
                        env('JENKINS_SLAVE_LABEL', envConfig.value.slaveLabel)
                        env('CUSTOMER', customer)
                        env('SERVICE_DISPLAY_NAME', serviceDisplayName)
                        env('SERVICE_REPO_ADDRESS', serviceRepoAddress)
                        env('SERVICE_DOCKER_REPO_ADDRESS_DEV', serviceDockerRepoAddressDev)
                        env('SERVICE_DOCKER_REPO_ADDRESS_PRD', serviceDockerRepoAddressPrd)
                        env('REGION', region)
                        env('CARRIER_UUID', testsCarrierUuid)
                    }

                    parameters {
                        stringParam('VERSION_MAJOR', '', 'Required major part of version used to resolve a docker image to be used')
                        stringParam('VERSION_MINOR', '', 'Required minor part of version used to resolve a docker image to be used')
                        stringParam('VERSION_PATCH', '', 'Required patch part of version used to resolve a docker image to be used')
                        stringParam('VERSION_BUILD', '', 'Required build number part of version used to resolve a docker image to be used')
                        stringParam('SERVICE_REPO_BRANCH_OR_COMMIT', '', 'Required name of the branch or identifier of the commit from which docker image has been built')
                        booleanParam('DESTROY_INT_ENV',false,'Destroy INT environment after build (Automatic destroy for this environments is triggered everyday at 18:00)')
                        booleanParam('DESTROY_PFT_ENV',false,'Destroy PFT environment after build (Automatic destroy for this environments is triggered everyday at 18:00)')
                    }
                }
            }
            if (envConfig.key == 'int' || envConfig.key == 'pft')
            {
                pipelineJob("${jobPath}/${serviceDisplayName}/${envConfig.key}/${region}/${customer}/automatic-destroy") {
                    displayName("Automatic destroy infrastructure trigger")
                    description("Automatic destroy infrastructure for on demand environments. Triggered everyday at 18:00")

                    logRotator {
                        numToKeep(10)
                    }

                    label(envConfig.value.slaveLabel)

                    triggers {
                        cron('H 4,16 * * *')
                    }

                    definition {
                        cpsScm {
                            scm {
                                git {
                                    remote {
                                        name 'origin'
                                        url "$serviceRepoAddress"
                                        branch 'master'
                                        credentials 'bitbucket-credentials'
                                    }
                                }
                            }
                            scriptPath("$jenkinsFiles/automaticDestroy.jenkinsfile")
                        }
                        concurrentBuild(true)
                    }

                    environmentVariables {
                        env('JENKINS_SLAVE_LABEL', envConfig.value.slaveLabel)
                        env('CUSTOMER', customer)
                        env('REGION', region)
                        env('PRODUCT', product)
                        env('ENV', envConfig.key)
                        env('SERVICE_DISPLAY_NAME', serviceDisplayName)
                        env('AWS_ACCOUNT', envConfig.value.awsAccount)
                        env('CARRIER_UUID', testsCarrierUuid)
                    }
                }
            }
        }
    }
}
