#!/bin/bash
#
# Authors: Akshay Raje
#
# Builds mr-blitz.jar and makes it available in 'dist' directory as well as
# installs mr-blitz.jar in local Maven repository.
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
echo "  BUILDING mr-blitz.jar..."
echo "============================================================================"
mkdir -p dist
mvn clean install && cp target/mr-blitz.jar dist/mr-blitz.jar
if [[ $? != 0 ]];
then
	echo "============================================================================"
    echo "  EXITING BUILD SCRIPT DUE TO AN ERROR."
    echo "============================================================================"
	exit 1
fi
echo "============================================================================"
echo "1. Latest build of mr-blitz.jar is available in dist folder."
echo "2. Latest build of mr-blitz.jar is also installed in local Maven repository."
echo "   Add a project dependency as follows: "
echo "   <dependency>"
echo "      <groupId>mr-blitz</groupId>"
echo "      <artifactId>mr-blitz</artifactId>"
echo "      <version>1.0</version>"
echo "   </dependency>"
echo "============================================================================"
echo "  BUILD SCRIPT SUCCESSFUL"
echo "============================================================================"