//Credits to Taman for helping to develop the code . 
//Coded by XxRagulxX
def SCAN_TYPE
def SCAN_TYPE_TEST
def TARGET
pipeline {
    agent any
    parameters{
        choice  choices: ["Baseline","FullScan"],
                 description: 'Type of scan that is going to perform inside the container',
                 name: 'SCAN_TYPE'
        choice  choices: ["Created", "NOT Created"],
                 description: "OWASP ZAP Already Exist or Not",
                 name: 'SCAN_TYPE_TEST'
         string defaultValue: "http://demo.testfire.net",
                 description: 'Target URL to scan',
                 name: 'TARGET'
 
         booleanParam defaultValue: true,
                 description: 'Parameter to know if wanna generate report.',
                 name: 'GENERATE_REPORT'
         booleanParam defaultValue: true,
                 description: 'Parameter to know if wanna Email the report',
                 name: 'Email'
        
    }

    stages {
        stage("Pipeline Formation"){
            steps {
                     script {
                         echo "<--Parameter Initialization-->"
                         echo """
                         The current parameters are:
                             Scan Type: ${params.SCAN_TYPE}
                             Target: ${params.TARGET}
                             Generate report: ${params.GENERATE_REPORT}
                             Email report: ${params.Email}
                         """
                     }
                 } 
        }
        stage("Docker Zap"){
            steps {
                script {
                    scan_type_test = "${params.SCAN_TYPE_TEST}"
                    echo "----> scan_type: $scan_type_test"
                    if(scan_type_test == "Created"){
                         sh 'sudo docker rm owasp'
                         echo "Starting container --> Start"
                         sh """
                          sudo docker run -dt --name owasp \
                          owasp/zap2docker-stable \
                          /bin/bash
                         """
                    }
                    else{
                         echo "Pulling up last OWASP ZAP container --> Start"
                         sh 'sudo docker pull owasp/zap2docker-stable'
                         echo "Pulling up last VMS container --> End"
                         echo "Starting container --> Start"
                         sh """
                          sudo docker run -dt --name owasp \
                          owasp/zap2docker-stable \
                          /bin/bash
                         """
                    }
                }
            }
        }
        stage('Directory') {
            when {
                        environment name : 'GENERATE_REPORT', value: 'true'
             }
             steps {
                 script {
                        sh """
                            sudo docker exec owasp \
                            mkdir /zap/wrk \
                

                        """
                    }
                }
        }
        stage('Scanning'){
            steps {
                script {
                    scan_type = "${params.SCAN_TYPE}"
                    echo "----> scan_type: $scan_type"
                    target = "${params.TARGET}"
                    if(scan_type == "Baseline"){
                         sh """
                             sudo docker exec owasp \
                             zap-baseline.py \
                             -t $target \
                             -r report.html \
                             -I
                         """
                     }
                     //-x report-$(date +%d-%b-%Y).xml
                    else{
                        sh """
                             sudo docker exec owasp \
                             zap-full-scan.py \
                             -t $target \
                             -r report.html \
                             -I
                         """
                    }
                }
            }
        }
        stage('Copy Report to Workspace'){
             steps {
                 script {
                    sh '''
                        sudo docker cp owasp:/zap/wrk/report.html ${WORKSPACE}/report.html
                    '''
                    
                }
            }
        }
        stage('Email') {
            when {
                    environment name : 'Email', value: 'true'
             }
             steps {
                 script {
                        
                         emailext attachmentsPattern: 'report.html ', body: 'Hello Bro', subject: 'Just Bug Testing', to: 'donragulsurya@gmail.com'
                    }
                }
        }

        stage('Stopping'){
            steps{
                script{
                    sh 'sudo aa-remove-unknown'
                    //sh 'kill 1'
                    sh 'sudo docker stop owasp'
                }
            }
        }
    }
                 
}

