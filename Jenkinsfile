// myapp pipeline: build -> test -> publish -> Chef deploys it to dev -> uat -> prod.
// Your diagram, stage for stage. Jenkins polls GitHub every ~2 minutes,
// so a `git push` to main starts this automatically.
pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '20'))
    timeout(time: 45, unit: 'MINUTES')
  }

  triggers { pollSCM('H/2 * * * *') }

  environment {
    VERSION         = "1.0.${env.BUILD_NUMBER}"
    ARTIFACT_DIR    = '/var/www/artifacts/myapp'                 // served by nginx on :8081
    ARTIFACT_URL    = 'http://jenkins.lab.internal:8081/myapp'
    // CHANGE THIS to your own chef-infra repository:
    INFRA_REPO      = 'https://github.com/VishakArudhra/chef-infra.git'
    CHEF_USER       = 'jenkins'
    CHEF_SERVER_URL = 'https://chef-server.lab.internal/organizations/lab'
  }

  stages {
    stage('1. Checkout') {
      steps {
        checkout scm
        // The deploy tooling (knife config, ci/ scripts, InSpec profile) lives in chef-infra.
        dir('chef-infra') {
          git url: env.INFRA_REPO, branch: 'main', credentialsId: 'github-pat'
        }
      }
    }

    stage('2. Build') {
      steps { sh 'mvn -B -ntp clean package -DskipTests -Drevision=$VERSION' }
    }

    stage('3. Unit tests') {
      steps { sh 'mvn -B -ntp test -Drevision=$VERSION' }
      post { always { junit 'target/surefire-reports/*.xml' } }
    }

    stage('4. Publish artifact') {
      steps {
        sh '''
          JAR=target/myapp-$VERSION.jar
          cp "$JAR" "$ARTIFACT_DIR/"
          sha256sum "$JAR" | cut -d' ' -f1 > "$ARTIFACT_DIR/myapp-$VERSION.jar.sha256"
          echo "Published $ARTIFACT_URL/myapp-$VERSION.jar"
        '''
        archiveArtifacts artifacts: 'target/myapp-*.jar', fingerprint: true
        script {
          env.SHA256 = sh(script: 'sha256sum target/myapp-$VERSION.jar | cut -d" " -f1', returnStdout: true).trim()
        }
      }
    }

    stage('5. Chef validation') {
      // Before releasing, check every target group runs approved cookbook versions.
      steps {
        dir('chef-infra') {
          withCredentials([file(credentialsId: 'chef-jenkins-key', variable: 'CHEF_CLIENT_KEY')]) {
            sh '''
              knife ssl fetch -c .chef/config.rb >/dev/null 2>&1 || true
              for g in dev uat prod; do
                python3 ci/check_versions.py --lock policyfiles/myapp.lock.json \
                  --approved approved-versions.json --group $g --knife-config .chef/config.rb --fail-on-drift
              done
            '''
          }
        }
      }
    }

    stage('6+7. Deploy dev') {
      steps { script { release('dev') } }
    }

    stage('Deploy uat') {
      steps { script { release('uat') } }
    }

    stage('Approve prod') {
      steps {
        timeout(time: 30, unit: 'MINUTES') {
          input message: "Release myapp ${env.VERSION} to PROD?", ok: 'Release'
        }
      }
    }

    stage('Deploy prod') {
      steps { script { release('prod') } }
    }
  }

  post {
    always {
      junit allowEmptyResults: true, testResults: 'chef-infra/reports/*.xml'
    }
  }
}

// Tell Chef which version this group should run (data bag), then trigger
// cinc-client on those nodes and verify with InSpec.
def release(String group) {
  dir('chef-infra') {
    withCredentials([
      file(credentialsId: 'chef-jenkins-key', variable: 'CHEF_CLIENT_KEY'),
      sshUserPrivateKey(credentialsId: 'deploy-ssh-key', keyFileVariable: 'DEPLOY_KEY', usernameVariable: 'DEPLOY_USER')
    ]) {
      sh "ci/release_app.sh ${group} \"\$VERSION\" \"\$SHA256\" \"\$ARTIFACT_URL\""
    }
  }
}
