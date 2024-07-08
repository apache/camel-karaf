# Apache Camel Karaf Archetype

### Introduction

This archetype is to initialize a maven project to test a camel-feature inside karaf using the 
org.apache.camel.karaf:camel-integration-test

### Usage

This tool is using maven archetype:generate plugin, for instance, to 
initialize a test for camel-Atom , go to camel-karaf/tests/features/ , run
    
```shell
mvn archetype:generate -DfeatureName=Atom -DarchetypeGroupId=org.apache.camel.karaf -DarchetypeArtifactId=camel-karaf-test-feature-archetype -DarchetypeVersion=4.6.0-SNAPSHOT 
```

Then rename the directory camel-atom-test to camel-atom and adapt pom.xml, fill the supplier class and integration test.
