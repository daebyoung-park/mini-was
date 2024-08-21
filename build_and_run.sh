#!/bin/bash

# Clean and package the project using Maven
mvn clean package

# Check if the JAR file was created
JAR_FILE="target/was.jar"
if [ -f "$JAR_FILE" ]; then
    echo "JAR file created successfully: $JAR_FILE"
    # Run the JAR file
    java -jar "$JAR_FILE"
else
    echo "Error: JAR file not found!"
fi

