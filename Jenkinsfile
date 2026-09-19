pipeline {
  agent any

  parameters {
    string(name: 'GIT_URL',    defaultValue: 'https://github.com/wangyong-fy/k8s-fullstack-demo.git', description: '代码仓库地址')
    string(name: 'GIT_BRANCH', defaultValue: 'main', description: '分支')
    string(name: 'IMAGE_TAG',  defaultValue: '',     description: '镜像 tag，留空则使用 Jenkins 构建号')
  }

  environment {
    REGISTRY = 'wanyongdoker'
    APP_NS   = 'app'
    BUILD_NS = 'jenkins'
    KANIKO   = 'gcr.io/kaniko-project/executor:v1.23.2'
  }

  options { disableConcurrentBuilds() }

  stages {
    stage('Checkout') {
      steps {
        git branch: "${params.GIT_BRANCH}", url: "${params.GIT_URL}"
        script { env.TAG = params.IMAGE_TAG?.trim() ? params.IMAGE_TAG.trim() : env.BUILD_NUMBER }
        sh 'echo "本次构建 -> ${REGISTRY}/k8s-demo-backend:${TAG} 与 ${REGISTRY}/k8s-demo-frontend:${TAG}"'
      }
    }

    stage('Build & Push Backend') {
      steps {
        sh '''#!/bin/bash
set -e
CTX="git://${GIT_URL#https://}#refs/heads/${GIT_BRANCH}"
JOB="kaniko-backend-${BUILD_NUMBER}"
kubectl -n "$BUILD_NS" delete job "$JOB" --ignore-not-found
cat <<YAML | kubectl apply -f -
apiVersion: batch/v1
kind: Job
metadata:
  name: ${JOB}
  namespace: ${BUILD_NS}
  labels: {app: kaniko}
spec:
  backoffLimit: 0
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: kaniko
          image: ${KANIKO}
          args:
            - "--context=${CTX}"
            - "--context-sub-path=backend"
            - "--dockerfile=Dockerfile"
            - "--destination=${REGISTRY}/k8s-demo-backend:${TAG}"
            - "--snapshot-mode=redo"
          volumeMounts:
            - {name: docker-config, mountPath: /kaniko/.docker}
      volumes:
        - name: docker-config
          secret:
            secretName: dockerhub
            items: [{key: .dockerconfigjson, path: config.json}]
YAML
kubectl -n "$BUILD_NS" wait --for=condition=complete "job/$JOB" --timeout=900s
kubectl -n "$BUILD_NS" logs "job/$JOB" --tail=5
'''
      }
    }

    stage('Build & Push Frontend') {
      steps {
        sh '''#!/bin/bash
set -e
CTX="git://${GIT_URL#https://}#refs/heads/${GIT_BRANCH}"
JOB="kaniko-frontend-${BUILD_NUMBER}"
kubectl -n "$BUILD_NS" delete job "$JOB" --ignore-not-found
cat <<YAML | kubectl apply -f -
apiVersion: batch/v1
kind: Job
metadata:
  name: ${JOB}
  namespace: ${BUILD_NS}
  labels: {app: kaniko}
spec:
  backoffLimit: 0
  template:
    spec:
      restartPolicy: Never
      containers:
        - name: kaniko
          image: ${KANIKO}
          args:
            - "--context=${CTX}"
            - "--context-sub-path=frontend"
            - "--dockerfile=Dockerfile"
            - "--destination=${REGISTRY}/k8s-demo-frontend:${TAG}"
            - "--snapshot-mode=redo"
          volumeMounts:
            - {name: docker-config, mountPath: /kaniko/.docker}
      volumes:
        - name: docker-config
          secret:
            secretName: dockerhub
            items: [{key: .dockerconfigjson, path: config.json}]
YAML
kubectl -n "$BUILD_NS" wait --for=condition=complete "job/$JOB" --timeout=900s
kubectl -n "$BUILD_NS" logs "job/$JOB" --tail=5
'''
      }
    }

    stage('Deploy to K8s') {
      steps {
        sh '''#!/bin/bash
set -e
kubectl -n "$APP_NS" apply -f k8s/backend.yaml -f k8s/frontend.yaml -f k8s/ingress.yaml
kubectl -n "$APP_NS" set image deployment/backend  backend=${REGISTRY}/k8s-demo-backend:${TAG}
kubectl -n "$APP_NS" set image deployment/frontend frontend=${REGISTRY}/k8s-demo-frontend:${TAG}
kubectl -n "$APP_NS" rollout status deployment/backend  --timeout=300s
kubectl -n "$APP_NS" rollout status deployment/frontend --timeout=300s
kubectl -n "$APP_NS" get pods -l app
'''
      }
    }
  }

  post {
    always {
      sh '''#!/bin/bash
kubectl -n "$BUILD_NS" delete job -l app=kaniko --ignore-not-found || true
'''
    }
  }
}
