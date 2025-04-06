pipeline {
    agent {
        label 'AGENT-1'
    }
     options{
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        //retry(1)
    }
    environment {
        DEBUG = 'true'
        region= 'us-east-1'
        account_id = '180294178330'
        project = 'expense'
        environment = 'dev'
        component = 'backend'
        APP_VERSION = ''  // this will become global, we can use across pipeline
    }
    parameters {
        booleanParam(name: 'deployment', defaultValue: false, description: 'Select Build if you want to deploy the code')
    }
    stages{
        stage('read the version') {
            steps {
                script {
                    sh 'git clone https://github.com/raghuatharva/jenkins-backend.git'
                    env.APP_VERSION = sh(script: 'git describe --tags --abbrev=0', returnStdout: true).trim()
                    echo "The latest version is ${env.APP_VERSION}"
                }
            }
        }

        stage('Sonarqube analysis') {   
            // ಇದು ಯಾವ ಸೋನಾರ್ environment ಅಂತ ಹೇಳುತ್ತೆ.  it may be sonar 6.0 versions , 7.0 or any version
            environment{
                SCANNER_HOME = tool 'sonar-anna' 
                // iam using sonar-scanner 7.9.1 under the name "sonar-anna"(manage jenkins > tools > sonar-scanner)
            }
            steps{
                withSonarQubeEnv('sonar-ellidiyappa'){ 
                   sh '${SCANNER_HOME}/bin/sonar-scanner'
                }
                // sonar-ellidiyappa is the name of the sonar server which is configured in manage jenkins > system > sonar servers
                // generic scanner, it automatically understands any scripting language(if the source code is java ,python etc.) and provide scan results
                // this shell command sh '${SCANNER_HOME}/bin/sonar-scanner' --> the sonar scanner pluggin we installed  reads the sonar-project.properties file and sends the data to the sonar server. what will sonar server do is, it will scan the code and provide the results
            }
        }

        stage('Sonar Quality gate'){
            // above stage will run the sonar scanner and send the data to the sonar server. we should create a quality gate in the sonar server. quality gate is a set of conditions that the code must meet in order to be considered "good" or "acceptable". if the code meets the conditions, then it is considered "good" or "acceptable". if the code does not meet the conditions, then it is considered "bad" or "unacceptable". if the code is unacceptable, then the pipeline will be aborted. if the code is acceptable, then the pipeline will continue to the next stage.
            // this stage will check whether the code is passed or failed
            steps{
                script {
                    timeout(time: 10, unit: 'MINUTES') {
                        waitForQualityGate abortPipeline: true
                    }
                }
            }
            // this will check the quality gate status. if it is passed, then it will continue the pipeline. if it is failed, then it will abort the pipeline
            // sonar quality gate is a set of conditions that the code must meet in order to be considered "good" or "acceptable". some of the conditions are:
            // 1. code coverage: the percentage of code that is covered by tests; the value should be greater than 80%
            // 2. code smells: the number of code smells in the code; the value should be less than 10
            // 3. bugs: the number of bugs in the code; the value should be less than 5
            // 4. vulnerabilities: the number of vulnerabilities in the code; the value should be less than 5
            // 5. duplications: the percentage of code that is duplicated; the value should be less than 3%
            // 6. security hotspots: the number of security hotspots in the code; the value should be less than 5
            // 7. maintainability rating: the rating of the code; the value should be A or B
            // 8. reliability rating: the rating of the code; the value should be A or B

            //                              -------MOST IMPORTANT -------

            // JUST WRITING THIS BLOCK IS NOT ENOUGH ; TO STOP THE PIPELINE , SONARQUBE SERVER SHOULD BE CONFIGURED WITH THE WEBHOOK; TO DO THAT GO TO SONAR SERVER --> ACCOUNT --> ADMIN --> CONFIGURATION --> WEBHOOKS and add this url ; < jenkins.rohanandlife.site:9000/sonarqube-webhook/ 

            // this will check the quality gate status. if it is passed, then it will continue the pipeline. if it is failed, then it will abort the pipeline
        }

        stage('building docker image'){
            steps{
                withAWS(region: 'us-east-1', credentials: 'aws-creds') {
                sh """
                aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${account_id}.dkr.ecr.us-east-1.amazonaws.com
                docker build -t ${account_id}.dkr.ecr.${region}.amazonaws.com/${project}/${environment}/${component}:${env.APP_VERSION} .
                docker push ${account_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${environment}/${component}:${env.APP_VERSION}
                """
                }
            }
        }
        stage('deployment'){
            when{
                expression { params.deployment }
                // since we havent mentioned true or false, it will ask the user to select true or false in the confirmation box of jenkins console 
            }
            steps{
                build job: 'backend-cd', parameters: [
                    string(name: 'version', value: ${env.APP_VERSION}),
                    string(name: 'ENVIRONMENT', value: 'dev'),
                ], wait: true
                // wait: true means CI waits for the CD pipeline to complete or to success. if fails, then CI will also fail
            }
        }
    }
    post{
        always{
            echo "this will run always"
            deleteDir()
        }
        success{
            echo "BACKEND IS DEPLOYED SUCCESSFULLY"
        }
        failure{
            echo " PIPELINE IS FAILED"
        }
    }
}