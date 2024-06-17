# Apache Camel Upgrade Tool

### Introduction

The Apache Camel Karaf Upgrade Tool is meant to be used to upgrade the version of Apache Camel in the Apache Camel Karaf
project. Indeed, several tasks are required to upgrade the version of Apache Camel in the Apache Camel Karaf project, this
tool simply automates all of them to ease the migration and avoid mistakes.

### Usage

The tool is a simple executable jar that can be run anywhere as long as you have a Java runtime installed.
    
```shell
java -jar target/camel-upgrade-${camel-karaf-version}.jar
```
where `${camel-karaf-version}` is the version of the Apache Camel Karaf project.

The tool will ask you several questions to know what you want to do and what are the values you want to use. 
You can also provide the answers to the questions in a file named `camel-upgrade.properties` in the same directory as
where the tool is executed.

The file should contain the following keys:

```properties
# The version of the Apache Camel project to upgrade to
camel.version = 4.6.0
# The location of the root folder of the Apache Camel Karaf project to upgrade
camel.karaf.root = ../../
# The location of the root folder of the Apache Camel project
camel.root = ../../../camel
```

### Features

The tool provides the following features:

* Set the new version of Apache Camel Karaf according to the version of Apache Camel in all the pom files
* Set the version of Apache Camel in the parent pom file of the project
* Set the version of third-party libraries in the parent pom file of the project based on the parent pom file of the
  Apache Camel project
* Add a new wrapper and feature for all new components
* Remove the wrapper and feature for all components that have been removed in the new version of Apache Camel
* Load default input values from the `camel-upgrade.properties` file
* Automatically check-out the correct tag of the Apache Camel project in case the location of the Apache Camel root folder is not provided
