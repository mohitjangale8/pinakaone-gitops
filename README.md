# pinakaone-gitops

GitOps repository for the "pinakaone" platform: Helm charts, ArgoCD Application manifests (app-of-apps pattern), and the Jenkins pipeline definitions + shared library that build and deploy into them. QuickMart is the only project deployed through this repo so far.

For the full narrative on how the pipelines/deployment model actually work end to end and why - see the project handbook (`PinakaOne_QuickMart_Handbook.html`). This README only describes repo layout.

## Structure

```
pinakaone-gitops/
 ├── bootstrap/
 │   ├── app-of-apps.yaml          # the root ArgoCD Application. Applied ONCE, by hand,
 │   │                             # from pinakaone-iac's user_data.sh.tpl (ArgoCD can't
 │   │                             # discover this file via Git-watching before it exists
 │   │                             # to begin the watching in the first place). Watches
 │   │                             # apps/ recursively - everything under it is picked up
 │   │                             # automatically from then on.
 │   └── cluster-secret-store.yaml # tells External Secrets Operator to pull from AWS
 │                                 # Secrets Manager. Same one-time, applied-by-hand
 │                                 # bootstrap as app-of-apps.yaml, for the same reason.
 ├── apps/quickmart/               # ArgoCD Application objects - WHERE each chart's
 │   │                             # rendered manifests should be applied. Each one just
 │   │                             # points at repoURL + path (a charts/ subfolder) +
 │   │                             # destination namespace. These change rarely - almost
 │   │                             # never touched after being written once.
 │   ├── application-backend.yaml
 │   ├── application-mysql.yaml
 │   ├── application-sonarqube.yaml
 │   └── application-sonarqube-postgres.yaml
 ├── charts/                       # the actual application content - WHAT gets deployed.
 │   │                             # values.yaml here changes on every single deploy
 │   │                             # (Jenkins bumps image.tag on every build).
 │   ├── backend/                  # Spring Boot API - Deployment, Service (NodePort),
 │   │                             # HPA, PDB, ConfigMap, ExternalSecret
 │   ├── mysql/                    # StatefulSet + NodePort Service + ExternalSecret
 │   ├── sonarqube/                # Deployment (Recreate strategy) + statically-pinned PVC
 │   └── sonarqube-postgres/       # StatefulSet backing store for SonarQube
 ├── jenkins/
 │   ├── pipelines/                # backend.Jenkinsfile, frontend.Jenkinsfile,
 │   │                             # backend-config.Jenkinsfile - thin entry points that
 │   │                             # mostly just call into vars/
 │   ├── jobs/backend-config/      # raw Jenkins job XML (not a Jenkinsfile) - needed
 │   │   └── config.xml            # because this job's Active Choices UI parameter would
 │   │                             # get wiped by a declarative pipeline's own parameters{}
 │   │                             # block on every run. Fetched live by user_data.sh.tpl's
 │   │                             # Jenkins init script to seed a fresh Jenkins home.
 │   └── install-jenkins-tools.sh  # worth checking whether this is still actually invoked
 │                                 # anywhere - the toolchain it likely installs (Maven,
 │                                 # Node, yq, sonar-scanner) is now baked directly into
 │                                 # the jenkins-quickmart:lts image at AMI-build time.
 └── vars/                         # the shared library - the real pipeline logic
     ├── buildAndPushBackend.groovy       # checkout -> secret scan -> SonarQube ->
     │                                    # build+push to ECR -> bump charts/backend/values.yaml
     ├── buildAndDeployFrontend.groovy    # checkout -> secret scan -> npm build -> S3 sync ->
     │                                    # SonarQube -> CloudFront invalidation (cross-account,
     │                                    # narrowly-scoped credentials, set +x-guarded)
     ├── scanForSecrets.groovy            # gitleaks --no-git, shared by both build pipelines
     └── editApplicationValuesYaml.groovy # backend-config job's values.yaml config: merge logic
```

## The two different kinds of YAML under `apps/` and `charts/` - don't confuse them

`apps/quickmart/application-backend.yaml` is a pointer: it says *where* to find the backend's chart and *where* to deploy it. It almost never changes.

`charts/backend/values.yaml` is the actual configuration: image tag, resource limits, env vars. It changes on every deploy - this is the exact file the Jenkins pipeline patches via `sed` after every successful build.

## How a deploy actually happens

1. A human clicks "Build with Parameters" on the relevant Jenkins job (there are no automatic triggers - no public webhook endpoint, and the box sleeps 16 hours a day).
2. The pipeline (via `vars/`) builds the app, scans it for secrets, runs SonarQube (non-fatally), and for backend/frontend, either pushes an image to ECR or syncs to S3.
3. For the backend: the pipeline bumps `charts/backend/values.yaml`'s `image.tag` and pushes to this repo's `main` branch.
4. ArgoCD polls this repo (every ~30s - the default 3-minute interval was shortened via an `argocd-cm` patch) and reconciles: renders the chart via its built-in Helm support, diffs against live cluster state, and applies the difference.
5. The frontend never goes through ArgoCD/Kubernetes at all - its pipeline finishes at "S3 sync + CloudFront invalidation."

## Frontend has no chart or Application here, on purpose

The frontend is a static Angular build served from S3 + CloudFront (see `pinakaone-iac`'s `cloudfront-spa` module and the `pinakaone-domain` project) - it was never deployed into Kubernetes, so there's no `charts/frontend/` and no frontend `Application` manifest. Any documentation implying otherwise is stale.

## Adding a new app

Add a new `charts/<app>/` (Chart.yaml + values.yaml + templates/) and a matching `apps/quickmart/application-<app>.yaml` pointing at it. `app-of-apps.yaml`'s recursive watch on `apps/` picks it up automatically on its next reconcile - no other file needs to change.

## License

Private repository - no license granted.
