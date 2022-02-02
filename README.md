# Password manager for Estonian ID card
## Environments used

* Windows 10
* Intellij IDEA

## Prerequisites

* Java version: 14+
* Apache Ant
* Maven
* Java Card with ARMIS environment

## Applet Description
TODO AID and applet name will be changed later
* CAP AID: 0123456789
* Installed CAP AID: 0123456789000006
* Name: PasswordStorageApplet.cap

# Resources

* Java Card SDK https://www.oracle.com/java/technologies/javacard-sdk-downloads.html
* Ant Java Card to create CAP https://github.com/martinpaljak/ant-javacard

# Commands
```
ant create-cap
```
```
mvn install:install-file -Dfile=api_classic-3.1.0.jar -DgroupId=oracle.javacard -DartifactId=api_classic -Dversion=3.1.0 -Dpackaging=jar
```