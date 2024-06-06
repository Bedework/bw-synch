# bw-synch [![Build Status](https://travis-ci.org/Bedework/bw-synch.svg)](https://travis-ci.org/Bedework/bw-synch)

This project provides a synch engine for external subscriptions for
[Bedework](https://www.apereo.org/projects/bedework).

It could also be used for 1 way synchronization between any source and server. 

## Requirements

1. JDK 17
2. Maven 3

## Building Locally

> mvn clean install

## Releasing

Releases of this fork are published to Maven Central via Sonatype.

To create a release, you must have:

1. Permissions to publish to the `org.bedework` groupId.
2. `gpg` installed with a published key (release artifacts are signed).

To perform a new release use the release script:

> ./bedework/build/quickstart/linux/util-scripts/release.sh <module-name> "<release-version>" "<new-version>-SNAPSHOT"

When prompted, indicate all updates are committed

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

### 4.0.9
* Update library versions

### 4.0.10
* Update library versions

### 4.0.11
* Update library versions
* Remove unused fields and dependencies on hibernate.

### 4.0.12
* Update library versions

### 4.0.13
* Update library versions

### 4.0.14
* Update library versions
* Add campusgroups support to synch engine.

#### 5.0.0
* Use bedework-parent for builds
*  Upgrade library versions

#### 5.0.1
*  Upgrade library versions
* Remove enum user type from db. Use string value for persistence and provide option to get enum from that.
* Simplify the configuration utilities.
* Deploy synch service as a war. The ear version could not access hibernate mappings.

#### 5.0.2
*  Upgrade library versions
* Set User-Agent to identify Bedework

#### 5.0.3
*  Skip version - nexus plugin failed

#### 5.0.4
*  Upgrade library versions
* Moved synch xml module into synch out of bw-xml

#### 5.0.5
* Release 5.0.4 failed because of a deploy issue. Release version with new dependencies.

#### 5.0.6
* Release 5.0.5 failed because of a missing name in the pom.

#### 5.0.7
* Exclude calws from the generated artefact, or we get classloader errors.

#### 5.0.8
* Excluded xrd also - reinstate.

#### 5.0.8
* calws-soap source in different artefact.

#### 5.0.9
*  Upgrade library versions

#### 5.0.10
* Upgrade library versions
* Fix needed to deal with util.hibernate bug relating to static sessionFactory variable.
   