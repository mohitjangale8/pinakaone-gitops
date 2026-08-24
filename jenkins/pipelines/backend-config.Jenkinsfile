@Library('pinakaone-shared-lib') _

// Parameters are defined on the job itself (Manage via UI, not here):
//   APPLICATION - plain Choice Parameter, the chart under charts/ to edit
//   CONFIG_DATA - Active Choices Reactive Reference Parameter, pre-filled
//                 with the selected application's current values.yaml
// Deliberately a scripted pipeline, not `pipeline { parameters {} }`:
// declarative reconciles the job's parameter list against whatever's
// declared here on every run, which would delete the UI/config.xml-
// defined Active Choices parameter.
node {
    stage('Checkout gitops repo') {
        checkout scm
    }
    stage('Diff, validate, commit') {
        editApplicationValuesYaml(application: params.APPLICATION, content: params.CONFIG_DATA)
    }
}
