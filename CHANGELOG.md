# Release Notes

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased (6.1.0-SNAPSHOT)

## [6.0.0] - 2025-06-30
* First jakarta release
* Use more fluent approach for db api.
* Fix newly appearing stale-state exceptions - probably as a move to jpa compliancy
* Update db code to return merged/updated entity. Use that in synch engine to avoid stale-state exceptions (I hope).

## [5.0.13] - 2025-02-06
* Update library versions.
* Move response classes and ToString into bw-base module.
* Add start of openjpa implementation.
  Add new database exception and move db related exceptions into new package.
* Create a single instance of the service object for each URI and a single port from each service. Was getting intermittent failures on calls to getPort which appear to be memory related.
* Switch to use DbSession from bw-database.
* Convert the hql queries into valid jpql. No hibernate specific terms were required (I think).
* Switch to official jaxws-maven-plugin

## [5.0.12] - 2024-12-08
* Update library versions.
* Better error reporting
* Remove a lot of redundant throws clauses

## [5.0.11] - 2024-06-10
* Update library versions.
* Make SynchException unchecked
* Added a refresh operation to the synch engine and added associated code to the client side.
* Still has an issue - the subscription info at the synch engine side appears to be out of date after update. Get OptimisticLockExeption on retry of the refresh.
* Updating the subscription led to stale state exceptions. Just let the reschedule deal with it.

## [5.0.10] - 2024-06-06
* Update library versions.
* Fix needed to deal with util.hibernate bug relating to static sessionFactory variable.

## [5.0.9] - 2024-03-25
* Update library versions.

## [5.0.8] - 2024-03-24
* Excluded xrd also - reinstate.

## [5.0.7] - 2024-03-24
* Exclude calws from the generated artefact, or we get classloader errors.

## [5.0.6] - 2024-03-21
* Release 5.0.5 failed because of a missing name in the pom.

## [5.0.5] - 2024-03-21
* Release 5.0.4 failed because of a deploy issue. Release version with new dependencies.

## [5.0.4] - 2024-03-21
* Update library versions.
* Moved synch xml module into synch out of bw-xml

## [5.0.3] - 2024-02-23
*  Skip version - nexus plugin failed

## [5.0.2] - 2024-02-23
* Update library versions.
* Set User-Agent to identify Bedework

## [5.0.1] - 2023-12-07
* Update library versions.
* Remove enum user type from db. Use string value for persistence and provide option to get enum from that.
* Simplify the configuration utilities.
* Deploy synch service as a war. The ear version could not access hibernate mappings.

## [5.0.0] - 2022-02-12
* Update library versions.
* Use bedework-parent

## [4.0.14] - 2021-11-15
* Update library versions
* Add campusgroups support to synch engine.

## [4.0.13] - 2021-09-14
* Update library versions

## [4.0.12] - 2021-07-12
* Update library versions

## [4.0.11] - 2021-07-08
* Update library versions
* Remove unused fields and dependencies on hibernate.

## [4.0.10] - 2021-06-25
* Update library versions

## [4.0.9] - 2021-06-15
* Update library versions

## [4.0.8] - 2020-03-20
* bw-util refactor

## [4.0.7] - 2019-10-16
* Changes for latest http support

## [4.0.6] - 2019-10-16
* Switch to PooledHttpClient

## [4.0.5] - 2019-08-26
* Java 11: Add javax xml bind etc
* Fix dependencies

## [4.0.4] - 2019-06-27
* Use last-modified if etag not present

## [4.0.3] - 2019-04-15
* Update library versions.
* Fix jboss-app.xml

## [4.0.2] - 2018-12-13
* Update library versions.
* Logging changes

## [4.0.1] - 2018-11-28
* Update library versions.
* Reset subscription error count on reschedule
* Watch for null password in decrypt
* Add a reschedule now feature

## [4.0.0] - 2018-04-08
* First 4.x release based on 3.11 dev
* 3.12 onwards releases of bedework calendar engine now use this version.

