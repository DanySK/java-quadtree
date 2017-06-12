#!/bin/bash
set -e
openssl aes-256-cbc -K $encrypted_ff8e67061518_key -iv $encrypted_ff8e67061518_iv -in prepare_environment.sh.enc -out prepare_environment.sh -d
bash prepare_environment.sh
./gradlew
./gradlew uploadArchives
