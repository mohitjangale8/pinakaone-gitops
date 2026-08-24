#!/bin/bash
# Installs the extra tooling the Jenkins pipelines need on top of the
# jenkins/jenkins:lts image: Node.js (frontend build), yq (backend-config
# YAML edits), JDK 21 + Maven (backend SonarQube analysis), and SonarScanner
# CLI (frontend SonarQube analysis).
#
# Fetched and run by the EC2 boot script (pinakaone-iac user_data.sh.tpl)
# inside the Jenkins container as root. Kept as a separate repo file so
# user-data itself stays under EC2's 16KB limit - the boot script just
# curls this and pipes it to bash.
set -euxo pipefail

# No Node.js - needed for the frontend pipeline.
curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
apt-get install -y nodejs

# yq - for backend-config's YAML edits.
curl -fsSL https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64 -o /usr/local/bin/yq
chmod +x /usr/local/bin/yq

# JDK 21 + Maven - needed for the backend pipeline's SonarQube analysis
# (mvn sonar:sonar). JDK 21 matches the app's pom.xml java.version.
apt-get update
apt-get install -y openjdk-21-jdk maven

# SonarScanner CLI - needed for the frontend pipeline's SonarQube
# analysis (sonar-scanner reads sonar-project.properties). Version pinned
# to a known-good release; bump deliberately, not by accident.
apt-get install -y unzip
curl -fsSL https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-8.1.0.6389-linux-x64.zip -o /tmp/sonar-scanner.zip
unzip -q /tmp/sonar-scanner.zip -d /opt
ln -sf /opt/sonar-scanner-8.1.0.6389-linux-x64/bin/sonar-scanner /usr/local/bin/sonar-scanner
rm -f /tmp/sonar-scanner.zip