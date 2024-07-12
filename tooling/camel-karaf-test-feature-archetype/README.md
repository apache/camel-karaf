# Apache Camel Karaf Integration Test Archetype

### Introduction

This archetype is to initialize a maven project to write an integration test for a Camel Karaf feature using the 
framework provided by `org.apache.camel.karaf:camel-integration-test`.

### Usage

This tool is using maven archetype:generate plugin, for instance, to 
initialize an integration test for the component camel-atom, go to `camel-karaf/tests/features/`, then run
    
```shell
mvn archetype:generate -DfeatureName=Atom -DarchetypeGroupId=org.apache.camel.karaf -DarchetypeArtifactId=camel-karaf-test-feature-archetype -DarchetypeVersion=4.7.0-SNAPSHOT 
```

Then rename the directory `camel-atom-test` to `camel-atom`, adapt the file `pom.xml`, and finally fill in the supplier class and integration test.
