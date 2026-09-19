pipeline {
  agent any

  parameters {
    string(name: 'REPO_URL',    defaultValue: 'git://172.19.52.97/k8s-demo-app.git', description: '代码仓库地址')
    string(name: 'REPO_BRANCH', defaultValue: 'main', description: '分支')
    string(name: 'IMAGE_TAG',   defaultValue: '',     description: '镜像 tag，留空则使用构建号')
  }

  environment {
    REGISTRY = 'wanyongdoker'
    APP_NS   = 'app'
    BUILD_NS = 'jenkins'
    KANIKO   = 'gcr.io/kaniko-project/executor:v1.23.2'
    CACHE    = 'wanyongdoker/kaniko-cache'
  }

  options { disableConcurrentBuilds() }

  stages {
    stage('Checkout') {
      steps {
        git branch: "${params.REPO_BRANCH}", url: "${params.REPO_URL}"
        script { env.TAG = params.IMAGE_TAG?.trim() ? params.IMAGE_TAG.trim() : env.BUILD_NUMBER }
        sh 'echo "本次构建 -> ${REGISTRY}/k8s-demo-backend:${TAG} 与 ${REGISTRY}/k8s-demo-frontend:${TAG}"'
      }
    }

    stage('Build & Push Backend') {
      steps {
        sh '''#!/bin/bash
set -e
HOSTPATH="${REPO_URL#*://}"
CTX="git://${HOSTPATH}#refs/heads/${REPO_BRANCH}"
JOB="kaniko-backend-${BUILD_NUMBER}"
echo "context = $CTX"
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
            - "--cache=true"
            - "--cache-repo=${CACHE}"
          volumeMounts:
            - {name: docker-config, mountPath: /kaniko/.docker}
      volumes:
        - name: docker-config
          secret:
            secretName: dockerhub
            items: [{key: .dockerconfigjson, path: config.json}]
YAML
for i in $(seq 1 240); do
  s=$(kubectl -n "$BUILD_NS" get job "$JOB" -o jsonpath='{.status.succeeded}' 2>/dev/null)
  f=$(kubectl -n "$BUILD_NS" get job "$JOB" -o jsonpath='{.status.failed}' 2>/dev/null)
  [ "$s" = "1" ] && { echo "backend 镜像 OK"; break; }
  [ -n "$f" ] && { echo "backend 构建失败"; kubectl -n "$BUILD_NS" logs "job/$JOB" --tail=60; exit 1; }
  sleep 5
done
[ "$s" = "1" ] || { echo "backend 构建超时"; exit 1; }
kubectl -n "$BUILD_NS" logs "job/$JOB" --tail=4
'''
      }
    }

    stage('Build & Push Frontend') {
      steps {
        sh '''#!/bin/bash
set -e
HOSTPATH="${REPO_URL#*://}"
CTX="git://${HOSTPATH}#refs/heads/${REPO_BRANCH}"
JOB="kaniko-frontend-${BUILD_NUMBER}"
echo "context = $CTX"
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
            - "--cache=true"
            - "--cache-repo=${CACHE}"
          volumeMounts:
            - {name: docker-config, mountPath: /kaniko/.docker}
      volumes:
        - name: docker-config
          secret:
            secretName: dockerhub
            items: [{key: .dockerconfigjson, path: config.json}]
YAML
for i in $(seq 1 240); do
  s=$(kubectl -n "$BUILD_NS" get job "$JOB" -o jsonpath='{.status.succeeded}' 2>/dev/null)
  f=$(kubectl -n "$BUILD_NS" get job "$JOB" -o jsonpath='{.status.failed}' 2>/dev/null)
  [ "$s" = "1" ] && { echo "frontend 镜像 OK"; break; }
  [ -n "$f" ] && { echo "frontend 构建失败"; kubectl -n "$BUILD_NS" logs "job/$JOB" --tail=60; exit 1; }
  sleep 5
done
[ "$s" = "1" ] || { echo "frontend 构建超时"; exit 1; }
kubectl -n "$BUILD_NS" logs "job/$JOB" --tail=4
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
