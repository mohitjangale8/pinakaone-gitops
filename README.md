# pinakaone-gitops

Generic GitOps repository. Shared Helm charts + per-project ArgoCD Applications (App-of-Apps pattern). Single source of truth for Kubernetes deployments.

## Structure

```
pinakaone-gitops/
 ├── bootstrap/
 │   └── app-of-apps.yaml      # root ArgoCD App -> watches apps/ recursively
 ├── charts/                   # shared Helm charts
 │   ├── backend/              # generic Spring Boot chart
 │   ├── frontend/             # generic SPA chart
 │   └── mysql/                # generic MySQL StatefulSet chart
 ├── apps/                     # ONE FOLDER PER PROJECT
 │   ├── quickmart/
 │   │   ├── values.yaml               # images, namespace, config
 │   │   ├── application-backend.yaml
 │   │   ├── application-frontend.yaml
 │   │   └── application-mysql.yaml
 │   └── <next-project>/
 └── docs/
     └── how-to-add-a-project.md
```

## Adding a New Project

Copy the previous project's app folder, change `values.yaml` (images, namespace). ArgoCD auto-discovers the new Applications on sync.

## Deployments Managed

- `quickmart`: Spring Boot backend + React/Angular frontend + MySQL, namespace `quickmart`

## License

Private repository - no license granted.