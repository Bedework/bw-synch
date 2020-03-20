# bw-synch [![Build Status](https://travis-ci.org/Bedework/bw-synch.svg)](https://travis-ci.org/Bedework/bw-synch)

This project provides a synch engine for external subscriptions for
[Bedework](https://www.apereo.org/projects/bedework).

It could also be used for 1 way synchronization between any source and server. 

## Requirements

1. JDK 8
2. Maven 3

## Building Locally

> mvn clean install

## Releasing

Releases of this fork are published to Maven Central via Sonatype.

To create a release, you must have:

1. Permissions to publish to the `org.bedework` groupId.
2. `gpg` installed with a published key (release artifacts are signed).

To perform a new release:

> mvn -P bedework-dev release:clean release:prepare

When prompted, select the desired version; accept the defaults for scm tag and next development version.
When the build completes, and the changes are committed and pushed successfully, execute:

> mvn -P bedework-dev release:perform

For full details, see [Sonatype's documentation for using Maven to publish releases](http://central.sonatype.org/pages/apache-maven.html).

## Release Notes
### 4.0.0
  * First 4.x release based on 3.11 dev 
  * 3.12 onwards releases of bedework calendar engine now use this version.

### 4.0.1
    * Reset subscription error count on reschedule
    * Watch for null password in decrypt
    * Add a reschedule now feature

### 4.0.2
    * Logging changes

### 4.0.3
    * Fix jboss-app.xml

### 4.0.4
    * Use last-modified if etag not present

### 4.0.5
    * Java 11: Add javax xml bind etc
    * Fix dependencies

### 4.0.6
    * Switch to PooledHttpClient

### 4.0.7
    * Changes for latest http support

### 4.0.8
    * bw-util refactor
    
    