#!/bin/bash
# Script to fix SSL certificate issues with Java 25

# Export the certificate from the failing site (Maven Central)
echo "Downloading Maven Central certificate..."
echo | openssl s_client -showcerts -servername repo.maven.apache.org -connect repo.maven.apache.org:443 2>/dev/null | openssl x509 -outform PEM > /tmp/maven-central.pem

# Import into Java 25 trust store
echo "Importing certificate into Java 25 trust store..."
JAVA_HOME=/Users/bhargavabasava/Apps/java/jdk-25.0.1.jdk/Contents/Home
sudo ${JAVA_HOME}/bin/keytool -import -trustcacerts -alias maven-central -file /tmp/maven-central.pem -keystore ${JAVA_HOME}/lib/security/cacerts -storepass changeit -noprompt

echo "Certificate imported successfully!"
