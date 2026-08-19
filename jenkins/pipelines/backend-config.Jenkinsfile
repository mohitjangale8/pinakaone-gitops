@Library('pinakaone-shared-lib') _

// CONFIG_DATA and DRY_RUN are defined on the job itself (Active Choices -
// CONFIG_DATA is pre-filled with the current charts/backend/values.yaml
// content right on the Build page, before you click Build). Deliberately
// a scripted pipeline, not `pipeline { parameters {} }`: declarative
// reconciles the job's parameter list against whatever's declared here on
// every run, which would wipe out the config.xml-defined Active Choices
// parameter.
node {
    stage('Checkout gitops repo') {
        checkout scm
    }
    stage('Diff / Apply') {
        editBackendValuesYamlFromParam(dryRun: params.DRY_RUN, content: params.CONFIG_DATA)
    }
}
