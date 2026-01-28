#!/bin/bash
set -e

PROJECTS=(
    'bramble-api'
    'bramble-core'
    'bramble-android'
    'bramble-java'
    'anonchat-api'
    'briar-core'
    'anonomi-android'
    'briar-headless'
)

# clear witness files to prevent errors when upgrading dependencies
for project in ${PROJECTS[@]}
do
    echo "" > ${project}/witness.gradle
done

# calculating new checksums
for project in ${PROJECTS[@]}
do
    echo "Calculating new checksums for ${project} ..."
    ./gradlew -q --configure-on-demand ${project}:calculateChecksums \
        | grep -v '^\(Skipping\|Verifying\|Welcome to Gradle\)' \
        | sed "s/    /\t/g" \
        > ${project}/witness.gradle
done
