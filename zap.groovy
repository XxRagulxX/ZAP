//Credits to Taman for helping to develop the code . 
//Coded by XxRagulxX
def SCAN_TYPE
def SCAN_TYPE_TEST
def TARGET
def REPORT
pipeline {
    agent any
    parameters{
        choice  choices: ["Baseline","FullScan"],
                 description: 'Type of scan that is going to perform inside the container',
                 name: 'SCAN_TYPE'
        choice  choices: ["Created", "NOT Created"],
                 description: "OWASP ZAP Already Exist or Not",
                 name: 'SCAN_TYPE_TEST'
        choice  choices: ["HTML","XML","JSON","WIKI"],
                 description: 'Report Output file type',
                 name: 'REPORT'
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
                             Report_Test: ${params.REPORT}
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
                    report = "${params.REPORT}"
                    if(scan_type == "Baseline"){
                        if(report == "HTML"){
                         sh """
                             sudo docker exec owasp \
                             zap-baseline.py \
                             -t $target \
                             -r report.html \
                             -I
                         """
                        }
                        if(report == "XML"){
                         sh """
                             sudo docker exec owasp \
                             zap-baseline.py \
                             -t $target \
                             -x report.xml \
                             -I
                         """
                        }
                        if(report == "JSON"){
                         sh """
                             sudo docker exec owasp \
                             zap-baseline.py \
                             -t $target \
                             -J report.json \
                             -I
                         """
                        }
                        else{
                            if(report == "WIKI"){
                         sh """
                             sudo docker exec owasp \
                             zap-baseline.py \
                             -t $target \
                             -w report.md \
                             -I
                         """
                        }
                     }
                    }
                     //-x report-$(date +%d-%b-%Y).xml
                    else{
                        if(report == "HTML"){
                         sh """
                             sudo docker exec owasp \
                             zap-full-scan.py \
                             -t $target \
                             -r report.html \
                             -I
                         """
                        }
                        if(report == "XML"){
                         sh """
                             sudo docker exec owasp \
                             zap-full-scan.py \
                             -t $target \
                             -r report.xml \
                             -I
                         """
                        }
                        if(report == "JSON"){
                        sh """
                             sudo docker exec owasp \
                             zap-full-scan.py \
                             -t $target \
                             -r report.md \
                             -I
                         """
                        }
                        else{
                            if(report == "WIKI"){
                        sh """
                             sudo docker exec owasp \
                             zap-full-scan.py \
                             -t $target \
                             -r report.md \
                             -I
                         """
                            } 
                        }
                    }
                }
           }
        }
        stage('Copy Report to Workspace'){
             steps {
                script{
                    if(report == "HTML"){
                        sh '''
                          sudo docker cp owasp:/zap/wrk/report.html ${WORKSPACE}/report.html
                        '''
                    }
                    if(report == "XML"){
                        sh '''
                          sudo docker cp owasp:/zap/wrk/report.xml ${WORKSPACE}/report.xml
                        '''
                    }
                    if(report == "JSON"){
                        sh '''
                          sudo docker cp owasp:/zap/wrk/report.json ${WORKSPACE}/report.json
                        '''
                    }
                    else{
                        if(report == "WIKI"){
                        sh '''
                          sudo docker cp owasp:/zap/wrk/report.md ${WORKSPACE}/report.md
                        '''
                        }
                   }
                }
            }
        }
        stage('Email') {
            when {
                    environment name : 'Email', value: 'true'
             }
             steps {
                 script {
                    if(report == "HTML"){
                        emailext attachmentsPattern: 'report.html', body: 'ZAP Scanning Report . Report is attached below .', subject: 'OWASP ZAP REPORT', to: 'donragulsurya@gmail.com'
                    }
                    if(report == "XML"){
                        emailext attachmentsPattern: 'report.xml', body: 'ZAP Scanning Report . Report is attached below .', subject: 'OWASP ZAP REPORT', to: 'donragulsurya@gmail.com'
                    }
                    if(report == "JSON"){
                        emailext attachmentsPattern: 'report.json', body: 'ZAP Scanning Report . Report is attached below .', subject: 'OWASP ZAP REPORT', to: 'donragulsurya@gmail.com'
                    }
                    else{
                        if(report == "WIKI"){
                            emailext attachmentsPattern: 'report.md', body: 'ZAP Scanning Report . Report is attached below .', subject: 'OWASP ZAP REPORT', to: 'donragulsurya@gmail.com'
                        }
                    }
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

