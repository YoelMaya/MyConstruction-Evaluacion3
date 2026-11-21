pipeline {
    agent any

    tools {
        jdk 'jdk17'
        maven 'maven3'
    }

    options {
        skipDefaultCheckout(true)
    }

    stages {
        stage('Descargar codigo GitHub') {
            steps {
                checkout scm
            }
        }

        stage('Verificar herramientas') {
            steps {
                bat 'java -version'
                bat 'mvn -version'
            }
        }

        stage('Compilar proyecto') {
            steps {
                bat 'mvn -B clean compile'
            }
        }

        stage('Pruebas automatizadas') {
            steps {
                bat 'mvn -B test'
            }
        }

        stage('Generar WAR') {
            steps {
                bat 'mvn -B package -DskipTests'
            }
        }

        stage('Validar WAR') {
            steps {
                bat 'if not exist target\\myconstruction-1.0.0.war exit /b 1'
            }
        }

        stage('Archivar WAR') {
            steps {
                archiveArtifacts artifacts: 'target/*.war',
                    fingerprint: true
            }
        }
    }

    post {
        always {
            junit testResults: 'target/surefire-reports/*.xml',
                allowEmptyResults: true
        }

        success {
            echo 'Compilacion WAR exitosa'
        }

        failure {
            echo 'Error durante la compilacion'
        }
    }
}