# Apache Camel Karaf

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.camel.karaf/karaf/badge.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/org.apache.camel.karaf/karaf)

[Apache Camel](http://camel.apache.org/) is a powerful open source integration framework based on known
Enterprise Integration Patterns with powerful bean integration.

This project provides Apache Karaf support for Apache Camel.

## Build

To build camel-karaf, simple do:

```
mvn clean install
```

If you want to skip the tests, you can do:

```
mvn clean install -DskipTests
``` 

## Upgrade

If you want to upgrade camel-karaf to a new camel version, here's the process:

1. camel repository has to be cloned in the same folder as camel-karaf repository:

```
git clone https://github.com/apache/camel
```

2. in the camel repository, you have to checkout on the target version tag:

```
cd camel
git checkout camel-x.y.z
```

3. once you have built camel-karaf, you can find the update tool in `tooling/camel-upgrade` folder. Go in this folder:

```
cd tooling/camel-upgrade
```

4. you can now run the upgrade tool:

```
java -jar target/camel-upgrade-*.jar
```

5. When done, you can review the changes (with `git diff` for instance) and create a Pull Request.

## Release

Here's the process to do a camel-karaf release:

1. Create the release tag:

```
mvn release:prepare
```

2. Push the release:

```
mvn release:perform
```

3. Close the Maven staging repository on repository.apache.org

4. The source distribution is available the `target` folder: `camel-karaf-x.y.z-source.release.zip.*`. You have to stage the source distribution files to `svn://dist.apache.org/repos/dist/dev/camel`.

5. When both Maven Staging Repository (closed) and dist.apache.org are up to date with the release files, you can start the release vote.
