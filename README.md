# k8s-fullstack-demo

Kubernetes 全栈服务端演示项目（实习考核）。

## 架构

```
Browser → Ingress(k8s-demo.local)
            ├── /      → frontend Service → Nginx(Vue3 静态, 2副本)
            └── /api   → backend  Service → Spring Boot(2副本) → mysql Service → MySQL(StatefulSet + NFS PVC)
                                                              ↑ 密码来自 K8s Secret
CI/CD: GitHub → Jenkins → kaniko 构建镜像 → 推送 Docker Hub → kubectl 部署
```

## 目录

| 路径 | 说明 |
|---|---|
| `backend/` | Spring Boot 3 + JDK17 + Maven，提供 `/api/messages` 增查接口 |
| `frontend/` | Vue3 + Vite，Nginx 托管静态页 |
| `k8s/` | K8s 清单：backend / frontend Deployment+Service、Ingress |
| `Jenkinsfile` | 声明式流水线，用 kaniko 构建并部署 |

## 本地运行

```bash
# 后端（需 MySQL）
cd backend && mvn spring-boot:run
# 前端
cd frontend && npm install && npm run dev
```

## 镜像

- `wanyongdoker/k8s-demo-backend`
- `wanyongdoker/k8s-demo-frontend`
