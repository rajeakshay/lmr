#!/bin/bash
#
# Builds blitz.jar and makes it available in 'dist' directory as well as
# installs blitz.jar in local Maven repository.
#
# Pre-requisites:
# ===============
# Java 7 SE
# Maven     (https://maven.apache.org/install.html)
#
# Step 0:
# =======
# Run the following to make this script executable -> chmod +x build.sh
#
echo "============================================================================"
echo "  BUILDING blitz.jar..."
echo "============================================================================"
mkdir -p dist
mvn clean install && cp target/blitz.jar dist/blitz.jar
if [[ $? != 0 ]];
then
	echo "============================================================================"
    echo "  EXITING BUILD SCRIPT DUE TO AN ERROR."
    echo "============================================================================"
	exit 1
fi
echo "============================================================================"
echo "1. Latest build of blitz.jar is available in dist folder."
echo "2. Latest build of blitz.jar is also installed in local Maven repository."
echo "   Add a project dependency as follows: "
echo "   <dependency>"
echo "      <groupId>blitz</groupId>"
echo "      <artifactId>blitz</artifactId>"
echo "      <version>1.0</version>"
echo "   </dependency>"
echo "============================================================================"
echo "  BUILD SCRIPT SUCCESSFUL"
echo "============================================================================"