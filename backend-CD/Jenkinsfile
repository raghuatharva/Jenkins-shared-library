// environment parameter and version parameter will be passed from CI pipeline. to use those parameters, we also needs to create same parameters in the pipeline.

// this pipeline will be triggered from the CI pipeline. in the CI pipeline, we will pass the environment and version as parameters. in this pipeline, we will use those parameters to deploy the code to the respective environment.

// this pipeline wull get deployed no matter what since there is no condition to deploy ; but for qa tests to run or to check jira or anything the params.ENVIRONMENT should be matching.

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
        region= 'us-east-1'
        account_id = '180294178330'
        project = 'expense'
        environment = ''
        component = 'backend'
        APP_VERSION = ''  // this will become global, we can use across pipeline
    }
    parameters {
        choice(name: 'ENVIRONMENT', choices: ['dev', 'qa', 'uat', 'pre-prod','prod'], description: 'Select the environment to deploy')
        string(name: 'version', defaultValue: '', description: 'Enter the version to deploy')
    }
    
    stages{
        stage('setting up the environment'){
            steps{
                script{
                    environment = params.ENVIRONMENT
                    APP_VERSION = params.version
                }
            }
        }

        stage('QA tests'){ // this is post deployment 
            when{
                expression {params.ENVIRONMENT == 'qa'}
            }
            steps{
                sh """
                echo "Running QA tests" 
                """
            }
        }
        
        stage ('Check jira ticket'){
            when{
                expression {params.ENVIRONMENT == 'prod'}
            }
            steps{
                script{
                    sh """
                    echo "Checking jira ticket status"
                    echo " check jira ticket window ; can i deploy now ? "
                    echo "fail if any of the conditions are not met"
                    """
                   // You typically check for JIRA status like "Approved", "Ready for Release", or "Ready for Deployment" (stake holders will put approved or not in jira ) ✅ And for Deployment Window, you check whether the current time falls within an allowed window — like Friday 6 PM to Sunday 12 AM. 
                }
            }
        }
        stage('deployment'){
            steps{
                withAWS(region: 'us-east-1', credentials: 'aws-creds') {
                    sh """
                    aws eks update-kubeconfig --region ${region} --name ${project}-${environment}
                    cd helm
                    sed -i s/IMAGE_VERSION/${env.APP_VERSION}/g values.yaml
                    helm upgrade --install ${component} -n ${project} .
                    """
                }
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